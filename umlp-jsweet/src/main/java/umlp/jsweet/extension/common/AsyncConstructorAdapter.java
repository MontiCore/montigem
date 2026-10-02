/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import de.se_rwth.commons.logging.Log;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.extension.AnnotationManager;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.NewClassElement;
import org.jsweet.transpiler.model.support.MethodInvocationElementSupport;
import org.jsweet.transpiler.model.support.NewClassElementSupport;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.CompilationUnitTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.StatementTree;
import standalone.com.sun.source.tree.Tree;
import standalone.com.sun.source.tree.VariableTree;
import standalone.com.sun.source.util.TreeScanner;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.AbstractAdapter;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static umlp.jsweet.extension.cocos.JSweetCoCoChecker.getStartSourcePosition;
import static umlp.jsweet.extension.util.GUIAdapterUtils.COMPONENT_ANNOTATION;
import static umlp.jsweet.extension.util.GUIAdapterUtils.DIRECTIVE_ANNOTATION;

public class AsyncConstructorAdapter extends AbstractAdapter {

  UMLPAdapterUtils adapterUtils;

  List<JCTree.JCVariableDecl> requiredFieldInitializations = new ArrayList<>();
  List<TypeMirror> requiredFieldInitializationsTypeMirrors = new ArrayList<>();
  boolean collectFieldInitializationsMode = true;

  // the variable declarations of one compilation unit by their initializer, so that a lookup does not
  // search the whole compilation unit
  private CompilationUnitTree declarationsOf;
  private final Map<Tree, JCTree.JCVariableDecl> declarationsByInitializer = new IdentityHashMap<>();

  public AsyncConstructorAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    this.adapterUtils = new GUIAdapterUtils(this, types(), context, util());
    addAnnotationManager(getEraseAndMarkAsnycConstructorsAnnotationManager());
    addAnnotationManager(getMarkComponentInitAsAsyncAnnotationManager());
  }

  protected AnnotationManager getMarkComponentInitAsAsyncAnnotationManager() {
    return new AnnotationManager() {
      @Override
      public Action manageAnnotation(Element element, String annotationType) {
        if (JSweetConfig.ANNOTATION_ASYNC.equals(annotationType)
            && element.getKind() == ElementKind.METHOD
            && element.getSimpleName().contentEquals("init")
        ) {
          TypeElement enclosingType = (TypeElement) element.getEnclosingElement();
          if (getContext().hasAnnotationType(enclosingType, COMPONENT_ANNOTATION) ||
              getContext().hasAnnotationType(enclosingType, DIRECTIVE_ANNOTATION)) {
            return Action.ADD;
          }
        }
        return Action.VOID;
      }
    };
  }

  protected AnnotationManager getEraseAndMarkAsnycConstructorsAnnotationManager() {
    return new AnnotationManager() {
      @Override
      public Action manageAnnotation(Element element, String annotationType) {
        if ((JSweetConfig.ANNOTATION_ERASED.equals(annotationType) || JSweetConfig.ANNOTATION_ASYNC.equals(annotationType) )
            && element.getKind() == ElementKind.CONSTRUCTOR) {
          TypeElement enclosingType = (TypeElement) element.getEnclosingElement();
          if (shouldTransformConstructors(enclosingType)) {
            return Action.ADD;
          }
        }
        return Action.VOID;
      }
    };
  }

  /**
   * This method is necessary to prevent JSweet from printing the await async constructor calls in
   * a new default constructor causing async await in constructor behavior
   * @param type               the type the expression is being assigned to
   * @param assignedExpression the assigned expression
   * @return
   */
  @Override
  public boolean substituteAssignedExpression(TypeMirror type, ExtendedElement assignedExpression) {
    if(collectFieldInitializationsMode) {
      return substituteAssignedExpressionInCollectionMode(type, assignedExpression);
    }
    return super.substituteAssignedExpression(type,assignedExpression);
  }

  protected boolean substituteAssignedExpressionInCollectionMode(TypeMirror type, ExtendedElement assignedExpression) {

    // Every Constructor is assumed to be async. Otherwise an incomplete set of async methods is
    // contained in asyncjre.json and more methods are async by propagation.
    if(assignedExpression instanceof NewClassElementSupport newClassAssignedExpression) {
      JCTree.JCVariableDecl varDecl = getDeclarationInitializedWith(newClassAssignedExpression.getTree());
      if(varDecl != null) {
        if(varDecl.sym.getKind() == ElementKind.FIELD) {
          if(varDecl.sym.isStatic()) {
            Log.error("0xEE010: Static async field declaration initialization not allowed. " +
                "Use dependency injection or static delegate instead."+
                getStartSourcePosition(getContext(), getCompilationUnitTree(), getPrinter().getCurrent()));
          }

          requiredFieldInitializations.add(varDecl);
          requiredFieldInitializationsTypeMirrors.add(type);
          // Print undefined because we set it inside the async constructor
          getPrinter().print("undefined");
          return true;
        }
      }
    } else if (assignedExpression instanceof MethodInvocationElementSupport methodInvocationElementSupport) {
      JCTree.JCMethodInvocation invocation =
          (JCTree.JCMethodInvocation) methodInvocationElementSupport.getTree();

      JCTree.JCVariableDecl varDecl = getDeclarationInitializedWith(invocation);

      if (varDecl != null) {
        if (varDecl.sym.getKind() == ElementKind.FIELD) {
          if (getContext().hasAnnotationType(methodInvocationElementSupport.getMethod(),  JSweetConfig.ANNOTATION_ASYNC)) {
            if(varDecl.sym.isStatic()) {
              Log.error("0xEE010: Static async field declaration initialization not allowed. " +
                  "Use dependency injection or static delegate instead."+
                  getStartSourcePosition(getContext(), getCompilationUnitTree(), getPrinter().getCurrent()));
            }
            requiredFieldInitializations.add(varDecl);
            requiredFieldInitializationsTypeMirrors.add(type);

            // Print undefined because we set it inside the async constructor
            getPrinter().print("undefined");
            return true;
          }
        }
      }
    }
    return super.substituteAssignedExpression(type, assignedExpression);
  }

  /**
   * @return the variable declaration whose initializer is the given expression, or null if the
   * expression is not the direct initializer of a variable
   */
  protected JCTree.JCVariableDecl getDeclarationInitializedWith(Tree expression) {
    CompilationUnitTree compilationUnit = getCompilationUnitTree();
    if (declarationsOf != compilationUnit) {
      declarationsByInitializer.clear();
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitVariable(VariableTree variable, Void unused) {
          if (variable.getInitializer() != null) {
            declarationsByInitializer.put(variable.getInitializer(), (JCTree.JCVariableDecl) variable);
          }
          return super.visitVariable(variable, unused);
        }
      }.scan(compilationUnit, null);
      declarationsOf = compilationUnit;
    }
    return declarationsByInitializer.get(expression);
  }

  /**
   * Replace every Constructor Call to allow async Constructors
   * @param newClass the new class expression
   * @return
   */
  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().getSimpleName().toString();

    if(!UMLPAdapterUtils.isJdkType((TypeElement) newClass.getTypeAsElement()) &&
        newClass.getTypeAsElement().getKind() != ElementKind.ENUM) {
      if(context.hasAnnotationType(newClass.getConstructor(), JSweetConfig.ANNOTATION_ASYNC) ||
          UMLPAdapterUtils.isRTEIncluded(newClass.getConstructor().getReturnType().toString())) {
        getPrinter().print("(<any> await ");
      }
      // We need to invoke the default Behavior of the Java2TypeScriptTranslator to handle implicit constructor args of inner classes
      // Explicit constructors args are handled by our method and suppress their printing at this point
      super.substituteNewClass(newClass);
      getPrinter().print(".__"+className+"Constructor(");
      // TODO: if (!typeApply.getTypeArguments().isEmpty()) {
      //                        print("<").printTypeArgList(typeApply.getTypeArguments()).print(">");
      //                    }
      printArgList(newClass.getArguments());
      getPrinter().print(")");
      if(context.hasAnnotationType(newClass.getConstructor(), JSweetConfig.ANNOTATION_ASYNC) ||
          UMLPAdapterUtils.isRTEIncluded(newClass.getConstructor().getReturnType().toString())) {
        getPrinter().print(")");
      }
      return true;
    }
    return super.substituteNewClass(newClass);
  }

  /**
   * Here we have to tell JSweet that we substituted the Constructor to make sure JSweet does not
   * print a default constructor that may contain async initialization logic from field initializations.
   * Furthermore, if the currently transpiled class is an Angular Component we have to add the
   * await AsyncConstructor to the init() method, because Angular calls constructors directly
   * @param parentTypeElement parent class
   * @param method            method's symbol
   * @return
   */
  @Override
  public boolean substituteMethodBody(TypeElement parentTypeElement, ExecutableElement method) {
    if(shouldTransformConstructors(parentTypeElement)) {
      if(method.getKind() == ElementKind.CONSTRUCTOR) {
        return true;
      }
      if(getContext().hasAnnotationType(parentTypeElement, COMPONENT_ANNOTATION)) {
        if(method.getKind() == ElementKind.METHOD && method.getSimpleName().toString().equals("init")) {
          getPrinter().print("await this.__").print(parentTypeElement.getSimpleName().toString()).print("Constructor();");
          getPrinter().println();
        }
      }
    }
    return super.substituteMethodBody(parentTypeElement, method);
  }

  @Override
  public void afterTypeBody(TypeElement type) {
    if (shouldTransformConstructors(type)) {
      List<ExecutableElement> constructors = new ArrayList<>();
      for (Element enclosed : type.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
          constructors.add((ExecutableElement) enclosed);
        }
      }
      if (!constructors.isEmpty()) {
        printMergedAsyncConstructors(constructors);
        printDefaultAsyncInitInComponent(type);
      } else {
        throw new RuntimeException("No constructor: now it might get shit");
      }
    }
    super.afterTypeBody(type);
  }

  protected boolean shouldTransformConstructors(TypeElement type) {
    return !getContext().isInterface(type)
        && type.getKind() != ElementKind.ENUM
        && !UMLPAdapterUtils.isJdkType(type);
  }

  protected boolean isExplicitConstructorInvocation(StatementTree stmt) {
    if (!(stmt instanceof JCTree.JCExpressionStatement)) {
      return false;
    }
    JCTree.JCExpression expr = ((JCTree.JCExpressionStatement) stmt).expr;
    if (!(expr instanceof JCTree.JCMethodInvocation)) {
      return false;
    }
    String meth = ((JCTree.JCMethodInvocation) expr).meth.toString();
    return meth.equals("super") || meth.equals("this");
  }

  protected void printDefaultAsyncInitInComponent(TypeElement type) {
    // If there is no Init and we are inside a Component -> create an Async init()
    if(getContext().hasAnnotationType(type, COMPONENT_ANNOTATION) &&
        !ElementFilter.methodsIn(type.getEnclosedElements()).stream().anyMatch(m -> m.getSimpleName().toString().equals("init"))) {
      getPrinter().print("public async init() : Promise<void> {");
      getPrinter().println();
      getPrinter().print("await this.__").print(type.getSimpleName().toString()).print("Constructor();");
      getPrinter().print("await super.init();");
      getPrinter().print("}");
    }
  }

  protected void printMergedAsyncConstructors(List<ExecutableElement> constructors) {
    TypeElement clazz = (TypeElement) constructors.get(0).getEnclosingElement();
    String className = clazz.getSimpleName().toString();
    String superClassName = ((DeclaredType) types().directSupertypes(clazz.asType()).get(0))
        .asElement().getSimpleName().toString();

    // Core constructor (most params) provides the merged signature — matches JSweet's core overload
    ExecutableElement coreConstructor = constructors.get(0);
    for (ExecutableElement c : constructors) {
      if (c.getParameters().size() > coreConstructor.getParameters().size()) {
        coreConstructor = c;
      }
    }
    List<? extends VariableElement> coreParams = coreConstructor.getParameters();

    // Signature: core param names, types erased to any (matches inCoreWrongOverload behavior)
    println().printIndent()
        .print("public async __").print(className).print("Constructor(");
    for (int i = 0; i < coreParams.size(); i++) {
      if (i > 0) print(", ");
      print(adapterUtils.getJSweetParameterName(coreParams.get(i)) + "?: any");
    }
    if(clazz.getSimpleName().isEmpty() || className.isBlank()) {
      className = getAnonymousSuperType(clazz).getSimpleName().toString();
    }
    print("): Promise<"+className+"> {").println().startIndent();

    for(int element = 0; element< requiredFieldInitializations.size(); element++) {
      this.collectFieldInitializationsMode = false;
      getPrinter().printIndent().println("// Field Constructor initialization");

      printFieldInitialization(requiredFieldInitializations.get(element));
    }
    this.collectFieldInitializationsMode = true;
    this.requiredFieldInitializations = new ArrayList<>();
    this.requiredFieldInitializationsTypeMirrors = new ArrayList<>();

    boolean multipleOverloads = constructors.size() > 1;
    for (int ci = 0; ci < constructors.size(); ci++) {
      ExecutableElement constructor = constructors.get(ci);
      List<? extends VariableElement> params = constructor.getParameters();

      if (multipleOverloads) {
        // Type-based dispatch: matches printMethodParamsTest
        if (ci == 0) {
          printIndent().print("if (");
        } else {
          printIndent().print("} else if (");
        }
        printConstructorParamsTest(params, coreParams);
        print(") {").println();
        startIndent();
      }

      // Parameter name remapping: if overload param j has a different name than core param j,
      // declare a local alias — matches printInlinedMethod's remapping logic
      for (int j = 0; j < params.size(); j++) {
        String overloadParamName = adapterUtils.getJSweetParameterName(params.get(j));
        String coreParamName = adapterUtils.getJSweetParameterName(coreParams.get(j));
        if (!overloadParamName.equals(coreParamName)) {
          printIndent().print("let " + overloadParamName + ": any = " + coreParamName + ";").println();
        }
      }

      MethodTree methodTree = trees().getTree(constructor);
      if (methodTree != null && methodTree.getBody() != null) {
        for (StatementTree stmt : methodTree.getBody().getStatements()) {

          // Print call of super Constructors
          if (isExplicitConstructorInvocation(stmt)) {
            JCTree.JCMethodInvocation call =
                (JCTree.JCMethodInvocation) ((JCTree.JCExpressionStatement) stmt).expr;
            if (call.meth.toString().equals("this")) {
              printIndent().print("await this.__" + className + "Constructor(");
            } else if(!superClassName.equals("Object")) {
              printIndent().print("await super.__" + superClassName + "Constructor(");
            }
            boolean first = true;
            for (JCTree.JCExpression arg : call.args) {
              if (!first) print(", ");
              print(arg);
              first = false;
            }
            if(!superClassName.equals("Object")) {
              print(");").println();
            }
          } else {
            super.print(stmt);
          }
          printIndent().println();
        }
      }

      if (multipleOverloads) {
        endIndent();
      }
    }
    if (multipleOverloads) {
      printIndent().print("}").println();
    }
    printIndent().print("return this;");
    endIndent().println().printIndent().print("}");
  }

  protected TypeElement getAnonymousSuperType(TypeElement type) {
    TypeMirror superType = null;
    List<? extends TypeMirror> interfaces = type.getInterfaces();

    if (!interfaces.isEmpty()) {
      superType = interfaces.get(0);
    }

    TypeMirror superclass = type.getSuperclass();

    if (superclass.getKind() != TypeKind.NONE) {
      superType = superclass;
    }
    if(superType != null && superType.getKind() == TypeKind.DECLARED) {
      return ((TypeElement) ((DeclaredType) superType).asElement());
    }
    return null;
  }

  // Copied from Java2TypeScriptTranslator
  protected void printFieldInitialization(VariableTree field) {
    VariableElement fieldElement = Util.getElement(field);
    if (context.hasAnnotationType(fieldElement, JSweetConfig.ANNOTATION_ERASED)) {
      return;
    }
    String name = getContext().getActualName(fieldElement);
    if (context.getFieldNameMapping(fieldElement) != null) {
      name = context.getFieldNameMapping(fieldElement);
    }
    printIndent().print("this.").print(name).print(" = ");
    if (!substituteAssignedExpression(fieldElement.asType(), ExtendedElementFactory.INSTANCE.create((field.getInitializer())))) {
      print(field.getInitializer());
    }
    print(";").println();
  }

  // Matches printMethodParamsTest: instanceof/typeof checks for present params,
  // === undefined for absent core params
  protected void printConstructorParamsTest(List<? extends VariableElement> overloadParams,
      List<? extends VariableElement> coreParams) {
    boolean hasCondition = false;
    for (int i = 0; i < overloadParams.size(); i++) {
      if (hasCondition) print(" && ");
      hasCondition = true;
      printParamTypeTest(
          adapterUtils.getJSweetParameterName(coreParams.get(i)),
          overloadParams.get(i).asType());
    }
    for (int i = overloadParams.size(); i < coreParams.size(); i++) {
      if (hasCondition) print(" && ");
      hasCondition = true;
      print(adapterUtils.getJSweetParameterName(coreParams.get(i)) + " === undefined");
    }
  }

  // Matches printInstanceOf + null check from printMethodParamsTest
  protected void printParamTypeTest(String coreParamName, TypeMirror paramType) {
    String mappedType = getMappedType(paramType);

    TypeMirror unwrappedOptional = paramType;
    if (types().erasure(paramType).toString().equals("java.util.Optional")
        && paramType instanceof DeclaredType) {
      List<? extends TypeMirror> args = ((DeclaredType) paramType).getTypeArguments();
      if (!args.isEmpty()) {
        unwrappedOptional = args.get(0);
      }
      mappedType = getMappedType(unwrappedOptional);
    }

    // Function types: Consumer<T>, Supplier<T>, etc. map to (p0: T) => void in TypeScript
    // TODO can we put Consumer<T> and Supplier<T> into typeMappings?
    if (mappedType.startsWith("(") && mappedType.contains("=>") || mappedType.startsWith("Consumer")) {
      print("typeof " + coreParamName + " === 'function'");
      return;
    }

    switch (mappedType) {
      case "string":
        print("(typeof " + coreParamName + " === 'string' || " + coreParamName + " === null)");
        return;
      case "number":
        print("typeof " + coreParamName + " === 'number'");
        return;
      case "boolean":
        print("typeof " + coreParamName + " === 'boolean'");
        return;
      default:
        break;
    }
    // Delegate to the adapter chain (handles List, custom domain types, etc.)
    // Use the erased mapped type so MyType<Foo> becomes instanceof MyType, not instanceof Foo.
    String erasedType = getMappedType(types().erasure(unwrappedOptional));
    if (!substituteInstanceof(coreParamName, null, unwrappedOptional)) {
      if(unwrappedOptional.getKind() == TypeKind.DECLARED
          && ((DeclaredType) unwrappedOptional).asElement().getKind() == ElementKind.ENUM ) {
        print("( /* TODO ENUM instanceOf */ true"+ " || " + coreParamName + " === null)");
      } else {
        print("(" + coreParamName + " instanceof " + erasedType + " || " + coreParamName + " === null)");
      }
    }
  }


  /**
   * JSweet's PrinterAdapter was specifically extended, so we could write this method which allows us
   * to do the AsyncConstructor stuff also for inner classes. It is very ugly since it copies a lot
   * of code from Java2JavaScriptTranslator, but until we find a better solution we have to live with it,
   * I guess. - CBu
   * @param args Arguments of the Constructor
   * @return
   */
  @Override
  public boolean substituteExplicitConstructorArgs(List<? extends Tree> args) {
    // Do not print anything since we will handle explicit constructor in the __Constructor Method
    return true;
  }

  @Override
  public boolean substituteExplicitConstructorArgsDecl(List<? extends Tree> args) {
    // Do not print anything since we will handle explicit constructor in the __Constructor Method
    return true;
  }


  @Override
  public Void beforeInnerNewClass(NewClassElement newClass) {
    if (context.hasAnnotationType(newClass.getConstructor(), JSweetConfig.ANNOTATION_ASYNC)) {
      getPrinter().print("(/* inner class constructor subst */ <any> await ");
    }
    return null;
  }

  @Override
  public Void afterInnerNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().getSimpleName().toString();

    // Class name is empty for anonymous classes
    getPrinter().print(".__"+className+"Constructor(");

    printArgList(newClass.getArguments());

    getPrinter().print(")");

    if(context.hasAnnotationType(newClass.getConstructor(), JSweetConfig.ANNOTATION_ASYNC)) {
      getPrinter().print(")");
    }
    return null;
  }


  @Override
  public Java2TypeScriptTranslator getPrinter() {
    if (super.getPrinter() instanceof Java2TypeScriptTranslator) {
      return (Java2TypeScriptTranslator) super.getPrinter();
    } else {
      Log.error("0JS0001 Java2TypeScriptTranslater must be core Adapter");
      return null;
    }
  }
}
