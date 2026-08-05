/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import de.se_rwth.commons.logging.Log;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.ForeachLoopElement;
import org.jsweet.transpiler.model.LiteralElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.NewClassElement;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.EnhancedForLoopTree;
import standalone.com.sun.source.tree.ExpressionTree;
import standalone.com.sun.source.tree.MemberSelectTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.AbstractAdapter;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.RTEPathUtil;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

public class CollectionsAdapter extends AbstractAdapter {
  public static final String CustomUMLPListInterfaceName = "IUMLPList";

  public static final String CustomCollectionInterfaceName = "Collection";

  public static final String CustomAsyncListImplementationName = "AsyncList";
  public static final String CustomAsyncListImplementationConstructor = "AsyncList<any>().__AsyncListConstructor";

  public static final String CustomAsyncSetImplementationName = "AsyncSet";
  public static final String CustomAsyncSetImplementationConstructor = "AsyncSet<any>().__AsyncSetConstructor";

  public static final String COLLECTORS = "Collectors";
  public static final String STREAM = "AsyncStream";

  UMLPAdapterUtils adapterUtils;

  public CollectionsAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    super.addTypeMappings(getStaticLibrary());
    getTypeMirrorMappings().add(0, computeUMLPGuiArrayTypeMapping());
    getTypeMirrorMappings().add(0, computeMapTypeMapping());
    getTypeMirrorMappings().add(0, computeMapEntryTypeMapping());
    this.adapterUtils = new GUIAdapterUtils(this, types(), context, util());
  }

  public Function<TypeMirror, String> computeUMLPGuiArrayTypeMapping() {
    return (TypeMirror type) -> {
      if (type.toString().equals("umlp.guiutil.UMLPGuiArray")) {
        return "Array";
      }
      return null;
    };
  }

  @Override
  public TypeMirror mapType(TypeMirror type) {
    if (this.types().isSubtype(this.types().erasure(type), this.types().erasure(
        this.util().getType(Map.class)))
        && type instanceof DeclaredType) {
      return type;
    }
    return super.mapType(type);
  }

  public Function<TypeMirror, String> computeMapTypeMapping() {
    return (TypeMirror type) -> {
      if (this.types().isSubtype(this.types().erasure(type), this.types().erasure(
          this.util().getType(Map.class)))) {
        StringBuilder result = new StringBuilder();
        result.append("Map<");
        List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
        for (int i = 0; i < typeArgs.size(); i++) {
          String qualifiedTypeName = getMappedTypeArgument(typeArgs.get(i));
          result.append(qualifiedTypeName);
          if (i < typeArgs.size() - 1) {
            result.append(", ");
          }
        }
        result.append(">");
        return result.toString();
      }
      return null;
    };
  }

  public Function<TypeMirror, String> computeMapEntryTypeMapping() {
    return (TypeMirror type) -> {
      if (type instanceof DeclaredType
          && "java.util.Map.Entry".equals(this.types().erasure(type).toString())) {
        List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
        if (typeArgs.size() == 2) {
          return "[" + getMappedTypeArgument(typeArgs.get(0)) + ", "
              + getMappedTypeArgument(typeArgs.get(1)) + "]";
        }
      }
      return null;
    };
  }

  private String getMappedTypeArgument(TypeMirror type) {
    for (Function<TypeMirror, String> mapping : getTypeMirrorMappings()) {
      String mapped = mapping.apply(type);
      if (mapped != null) {
        return mapped;
      }
    }
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      String mappedType = getMappedType(type);
      String simpleName = declaredType.asElement().getSimpleName().toString();
      String qualifiedName = getPrinter().getQualifiedTypeName(
          declaredType.asElement(), false, false);
      if (!simpleName.equals(qualifiedName) && mappedType.startsWith(simpleName)) {
        return qualifiedName + mappedType.substring(simpleName.length());
      }
      return mappedType;
    }
    return getMappedType(type);
  }

  @Override
  protected void initStaticLibrary() {
    putToStaticLibrary(List.class.getName(), CustomUMLPListInterfaceName);
    putToStaticLibrary(AbstractList.class.getName(), CustomUMLPListInterfaceName);
    putToStaticLibrary(ArrayList.class.getName(), CustomAsyncListImplementationName);
    putToStaticLibrary(CopyOnWriteArrayList.class.getName(), CustomAsyncListImplementationName);
    putToStaticLibrary(LinkedList.class.getName(), CustomAsyncListImplementationName);
    putToStaticLibrary(Stack.class.getName(), CustomAsyncListImplementationName);
    putToStaticLibrary(Vector.class.getName(), CustomAsyncListImplementationName);

    // Does this work for now?
    putToStaticLibrary(Set.class.getName(), CustomAsyncSetImplementationName);
    putToStaticLibrary(Stream.class.getName(), STREAM);
    putToStaticLibrary(HashSet.class.getName(), CustomAsyncSetImplementationName);
    putToStaticLibrary(LinkedHashSet.class.getName(), CustomAsyncSetImplementationName);
    putToStaticLibrary(Collection.class.getName(), CustomCollectionInterfaceName);
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    String targetMethodName = invocation.getMethodName();
    String targetClassName = invocation.getMethod().getEnclosingElement().toString();
    ExtendedElement targetExpression = invocation.getTargetExpression();
    if (targetExpression != null && targetExpression.getTypeAsElement() != null) {
      targetClassName = targetExpression.getTypeAsElement().toString();
    }
    TypeMirror jdkSuperclass = getContext().getJdkSuperclass(targetClassName, new LinkedHashSet<>());
    boolean delegate = jdkSuperclass != null;
    if (delegate) {
      targetClassName = jdkSuperclass.toString();
    }

    if (targetClassName != null
        && (targetExpression != null || invocation.getMethod().getModifiers().contains(
        Modifier.STATIC))) {
      switch (targetClassName) {
        case "java.util.stream.Collectors":
          getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.COLLECTORS, getLibraryLocation()));
          break;
        case "java.util.stream.Stream":
          getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.STREAM, getLibraryLocation()));
          if(targetMethodName.equals("of")) {
            getPrinter().print("AsyncStream.of(");
            printArgList(invocation.getArguments());
            print(")");
            return true;
          }
          break;
        case "java.util.List":
          if(targetMethodName.equals("of")) {
            print("(await new " + CustomAsyncListImplementationConstructor + "(")
                .printArgList(invocation.getArguments()).print("))");
            getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncListImplementationName, getLibraryLocation()));
            return true;
          }
        case "java.util.AbstractList":
        case "java.util.LinkedList":
        case "java.util.ArrayList":
        case "java.util.concurrent.CopyOnWriteArrayList":
        case "java.util.Stack":
        case "java.util.Vector":
        case "umlp.backendrte.common.GemList":
          // Handle standard List methods + stream()
          if (substituteCommonListAndSetInvocations(invocation, targetMethodName)) {
            return true;
          }
          break;
        case "java.util.Set":
        case "java.util.HashSet":
        case "java.util.LinkedHashSet":
          if (substituteCommonListAndSetInvocations(invocation, targetMethodName)) {
            return true;
          }
          break;
        case "java.util.Collections":
          if (substituteJavaCollectionsInvocations(invocation, targetMethodName))
            return true;
        case "com.google.common.collect.Lists":
          if (substituteCommonListsInvocations(invocation, targetMethodName))
            return true;
        case "java.util.Arrays":
          if (substituteUtilArraysInvocations(invocation, targetMethodName))
            return true;
        case "com.google.common.collect.Sets":
          if (substituteSetsInvocations(invocation, targetMethodName))
            return true;
        case "umlp.guiutil.UMLPGuiArray":
          if (substituteUMLPGuiArrayInvocations(invocation, targetMethodName))
            return true;
        case "java.util.Map":
        case "java.util.AbstractMap":
        case "java.util.HashMap":
        case "java.util.concurrent.ConcurrentHashMap":
        case "java.util.NavigableMap":
        case "java.util.TreeMap":
        case "java.util.Hashtable":
        case "java.util.WeakHashMap":
        case "java.util.LinkedHashMap":
          if (substituteMapInvocations(invocation, targetMethodName)) {
            return true;
          }
        case "java.util.Map.Entry":
          if (substituteMapEntryInvocations(invocation, targetMethodName)) {
            return true;
          }
      }
    }

    return super.substituteMethodInvocation(invocation);
  }

  private boolean substituteUMLPGuiArrayInvocations(MethodInvocationElement invocation,
                                                    String targetMethodName) {
    switch (targetMethodName) {
      case "set":
        print(invocation.getTargetExpression()).print("[").print(invocation.getArgument(0)).print("] = ").print(invocation.getArgument(1));
        return true;
      case "get":
        print(invocation.getTargetExpression()).print("[").print(invocation.getArgument(0)).print("]");
        return true;
    }
    return false;
  }

  private boolean substituteSetsInvocations(MethodInvocationElement invocation, String targetMethodName) {
    switch (targetMethodName) {
      case "newHashSet":
        printMacroName(targetMethodName);
        print("(await "+CollectionsAdapter.CustomAsyncSetImplementationName+".of(").printArgList(invocation.getArguments()).print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncSetImplementationName, getLibraryLocation()));
        return true;
    }
    return false;
  }

  private boolean substituteUtilArraysInvocations(MethodInvocationElement invocation, String targetMethodName) {
    switch (targetMethodName) {
      case "asList": {
        printMacroName(targetMethodName);
        print("(await new ").print(CollectionsAdapter.CustomAsyncListImplementationConstructor).print("([")
            .printArgList(invocation.getArguments()).print("]))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        return true;
      }
    }
    return false;
  }

  private boolean substituteCommonListsInvocations(MethodInvocationElement invocation, String targetMethodName) {
    switch (targetMethodName) {
      case "newArrayList":
        print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor);
        if (invocation.getArgumentCount() > 1) {
          print("(").printArgList(invocation.getArguments()).print("))");
        }
        else if (invocation.getArgumentCount() > 0) {
          print("([").printArgList(invocation.getArguments()).print("]))");
        }
        else {
          print("()))");
        }
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        return true;
    }
    return false;
  }

  private boolean substituteJavaCollectionsInvocations(MethodInvocationElement invocation, String targetMethodName) {
    switch (targetMethodName) {
      case "emptyList":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor + "())");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        return true;
      //        case "emptySet":
      //          printMacroName(adapter, targetMethodName);
      //          adapter.print("new ArrayList()");
      //          return true;
      case "unmodifiableList":
      case "unmodifiableCollection":
        //        case "unmodifiableSet":
        //        case "unmodifiableSortedSet":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor + "(")
            .printArgList(invocation.getArguments()).print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        return true;
      case "singleton":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncSetImplementationConstructor + "([")
            .print(invocation.getArgument(0)).print("]))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncSetImplementationName, getLibraryLocation()));
        return true;
      case "singletonList":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor + "([")
            .print(invocation.getArgument(0)).print("]))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        return true;
      case "nCopies":
        printMacroName(targetMethodName);
        print("Array(").print(invocation.getArgument(0)).print(").fill(").print(invocation.getArgument(1)).print(")");
        return true;
    }
    return false;
  }

  private boolean substituteCommonListAndSetInvocations(MethodInvocationElement invocation, String targetMethodName) {
    // 1. Handle stream() here to prevent erasure.
    // Result: nums.stream() -> nums

    switch (targetMethodName) {
      case "remove":
        substituteRemoveMethod(invocation);
        return true;
      default:
        // 2. Default fallback: Print as a standard method call (e.g., .add(), .get()).
        // This ensures compatibility with IUMLPList (avoids mapping to .push()).
        getPrinter().printDefaultMethodInvocation(ExtendedElementFactory.toTree(invocation));
        return true;
    }
  }

  private void substituteRemoveMethod(MethodInvocationElement invocation) {
    MethodInvocationTree methodInvocationTree = ExtendedElementFactory.toTree(invocation);
    if (methodInvocationTree.getMethodSelect() instanceof MemberSelectTree) {
      ExecutableType type = Util.getType(
          methodInvocationTree.getMethodSelect()) instanceof ExecutableType
          ? Util.getType(methodInvocationTree.getMethodSelect())
          : null;
      ExpressionTree selected = ((MemberSelectTree) methodInvocationTree.getMethodSelect()).getExpression();
      ExecutableElement methSym = util().findMethodDeclarationInType(
          Util.getTypeElement(selected), "remove", type);

      if (type.getReturnType().toString().equals("boolean")) {
        getContext().addMethodNameMapping(methSym, "removeElement");
      }
      else {
        getContext().addMethodNameMapping(methSym, "removeIndex");
      }
    }
    getPrinter().printDefaultMethodInvocation(methodInvocationTree);
  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    switch (newClass.getTypeAsElement().toString()) {
      case "java.util.List":
      case "java.util.AbstractList":
      case "java.util.LinkedList":
      case "java.util.ArrayList":
      case "java.util.concurrent.CopyOnWriteArrayList":
      case "java.util.Stack":
      case "java.util.Vector":
      case "java.util.Set":
      case "java.util.HashSet":
      case "java.util.LinkedHashSet":
        if (newClass.getArgumentCount() == 0) {
          if (isSetType(newClass)) {
            print("(await new " + CustomAsyncSetImplementationConstructor + "())");
            getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncSetImplementationName, getLibraryLocation()));
          }
          else {
            print("(await new " + CustomAsyncListImplementationConstructor + "())");
            getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncListImplementationName, getLibraryLocation()));
          }
        }
        else {
          if (util().isNumber(newClass.getArgument(0).getType())
              || (newClass.getArgument(0) instanceof LiteralElement)) {
            if (isSetType(newClass)) {
              // TODO this is not relevant for Sets??
              print("(await new " + CustomAsyncSetImplementationConstructor + "())");
              getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncSetImplementationName, getLibraryLocation()));
            }
            else {
              print("(await new " + CustomAsyncListImplementationConstructor + "())");
              getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncListImplementationName, getLibraryLocation()));
            }
          }
          else {
            if (isSetType(newClass)) {
              print("(await new " + CustomAsyncSetImplementationConstructor + "(")
                  .print(newClass.getArgument(0)).print("))");
              getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncSetImplementationName, getLibraryLocation()));
            }
            else {
              print("(await new " + CustomAsyncListImplementationConstructor + "(")
                  .print(newClass.getArgument(0)).print("))");
              getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncListImplementationName, getLibraryLocation()));
            }
          }
        }
        return true;
      case "umlp.guiutil.UMLPGuiArray":
        if (newClass.getArgumentCount() == 0) {
          print("[]");
        }
        else if (newClass.getArgumentCount() == 1) {
          printArgList(newClass.getArguments()).print(".toArray()");
        }
        else {
          Log.warn("Did not implement new UMLPGuiArray with more than one parameters");
        }
        return true;
      case "java.util.Map":
      case "java.util.AbstractMap":
      case "java.util.HashMap":
      case "java.util.concurrent.ConcurrentHashMap":
      case "java.util.NavigableMap":
      case "java.util.TreeMap":
      case "java.util.Hashtable":
      case "java.util.WeakHashMap":
      case "java.util.LinkedHashMap":
        print("new Map()");
        return true;
    }

    return super.substituteNewClass(newClass);
  }

  private boolean isSetType(NewClassElement newClass) {
    String type = newClass.getTypeAsElement().toString();
    return "java.util.Set".equals(type)
        || "java.util.HashSet".equals(type)
        || "java.util.LinkedHashSet".equals(type);
  }

  @Override
  public boolean substituteForEachLoop(ForeachLoopElement foreachLoop, boolean targetHasLength, String indexVarName) {
    EnhancedForLoopTree loop = ExtendedElementFactory.toTree(foreachLoop);
    TypeMirror expressionType = Util.getType(loop.getExpression());
    if (types().isSubtype(expressionType, types().erasure(util().getType(List.class)))
        // TODO how to catch every Collection that we want here?
    || types().isSubtype(expressionType, types().erasure(util().getType(Set.class)))
    || types().isSubtype(expressionType, types().erasure(util().getType(Collection.class)))
    ) {
      printAwaitForEachLoop(loop, indexVarName);
      return true;
    }
    return super.substituteForEachLoop(foreachLoop, targetHasLength, indexVarName);
  }

  protected void printAwaitForEachLoop(EnhancedForLoopTree loop, String indexVarName) {
    print("for await(" + Java2TypeScriptTranslator.VAR_DECL_KEYWORD + " ")
        // Rename JS keywords (e.g. a variable named "function") like the body does
        .print(adapterUtils.getJSweetVariableName(loop.getVariable().getName().toString()) + " of <AsyncIterable<");
    TypeMirror variableType = ((JCTree.JCVariableDecl) loop.getVariable()).sym.asType();
    // The inferred type may not occur anywhere in the Java source (for example with var), so
    // explicitly register every type named by the generated AsyncIterable cast.
    ensureTypeModulesUsed(variableType);
    print(getMappedTypeArgument(variableType)).print(">>(");
    print(loop.getExpression()).print(" ?? (async function* () {})())");
    getPrinter().print(") {").println().startIndent().printIndent()
        .printIndent().print(loop.getStatement())
        .endIndent().println().printIndent().print("}");
  }

  private void ensureTypeModulesUsed(TypeMirror type) {
    if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      getPrinter().ensureModuleIsUsed(declaredType.asElement());
      for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
        ensureTypeModulesUsed(typeArgument);
      }
    }
    else if (type instanceof ArrayType) {
      ensureTypeModulesUsed(((ArrayType) type).getComponentType());
    }
  }

  @Override
  public Java2TypeScriptTranslator getPrinter() {
    if (super.getPrinter() instanceof Java2TypeScriptTranslator) {
      return (Java2TypeScriptTranslator) super.getPrinter();
    }
    else {
      Log.error("0JS0001 Java2TypeScriptTranslater must be core Adapter");
      return null;
    }
  }

  @Override
  public boolean substituteInstanceof(String exprStr, ExtendedElement expr, TypeMirror type) {
    String typeString = types().erasure(type).toString();
    if (containedInStaticLibrary(typeString)) {
      print("(");
      print(exprStr, expr);
      print(" != null && ");
      print("(");
      print(exprStr, expr);
      print(" instanceof "+ CustomAsyncListImplementationName +" || ");
      print(exprStr, expr);
      print(" instanceof GemList)");
      print(")");

      getPrinter().useModule(new ModuleImportDescriptor("GemList", getLibraryLocation()));
      getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncListImplementationName, getLibraryLocation()));
      return true;
    }
    return super.substituteInstanceof(exprStr, expr, type);
  }

  private boolean substituteMapInvocations(MethodInvocationElement invocation,
                                           String targetMethodName) {
    switch (targetMethodName) {
      case "put":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".set(")
            .printArgList(invocation.getArguments())
            .print(")");
        return true;
      case "get":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".get(")
            .printArgList(invocation.getArguments())
            .print(")");
        return true;
      case "containsKey":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".has(")
            .printArgList(invocation.getArguments())
            .print(")");
        return true;
      case "keySet":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncSetImplementationConstructor + "(");
        print("[...").print(invocation.getTargetExpression()).print(".keys()").print("]");
        print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncSetImplementationName, getLibraryLocation()));
        return true;
      case "values":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".values()");
        return true;
      case "size":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".size");
        return true;
      case "isEmpty":
        printMacroName(targetMethodName);
        print("(").print(invocation.getTargetExpression()).print(".size === 0)");
        return true;
      case "remove":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".delete(")
            .printArgList(invocation.getArguments())
            .print(")");
        return true;
      case "clear":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print(".clear()");
        return true;
      case "entrySet":
        printMacroName(targetMethodName);
        print("(await new " + CollectionsAdapter.CustomAsyncSetImplementationConstructor + "(");
        print("[...").print(invocation.getTargetExpression()).print(".entries()").print("]");
        print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncSetImplementationName, getLibraryLocation()));
        return true;
      case "clone":
        printMacroName(targetMethodName);
        print("(o => { let c = {}; for (let k in Object.keys(o)){ c[k] = o[k] } return c; })(");
        printTargetForParameter(invocation.getTargetExpression()).print(")");
        return true;
    }

    return false;
  }

  private boolean substituteMapEntryInvocations(MethodInvocationElement invocation,
                                                String targetMethodName) {
    switch (targetMethodName) {
      case "getValue":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print("[1]");
        return true;
      case "getKey":
        printMacroName(targetMethodName);
        print(invocation.getTargetExpression()).print("[0]");
        return true;
    }
    return false;
  }

  protected CollectionsAdapter printTargetForParameter(ExtendedElement expression) {
    if (expression != null && expression.toString().equals("super")) {
      getPrinter().print("this");
      return this;
    }
    print(expression);
    return this;
  }
}
