/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.OverloadScanner;
import org.jsweet.transpiler.util.AbstractTreeScanner;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.ClassTree;
import standalone.com.sun.source.tree.CompilationUnitTree;
import standalone.com.sun.source.tree.EnhancedForLoopTree;
import standalone.com.sun.source.tree.ExpressionTree;
import standalone.com.sun.source.tree.LambdaExpressionTree;
import standalone.com.sun.source.tree.MemberSelectTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.NewClassTree;
import standalone.com.sun.source.tree.ParenthesizedTree;
import standalone.com.sun.source.tree.Tree;
import standalone.com.sun.source.tree.TypeCastTree;
import standalone.com.sun.source.util.TreePath;
import standalone.com.sun.source.util.Trees;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Marks methods and lambdas as async and awaits the invocations of async methods.
 * Combines the rule of JSweet's AsyncAwaitPropagationScanner, the UMLP rules (async RTE calls,
 * GemList accessors, List iterations, constructors) and the propagation along the class
 * hierarchy, and scans all compilation units again until no rule adds an async method or lambda.
 */
public class UMLPAsyncAwaitPropagationScanner extends AbstractTreeScanner {

  private final boolean transpileToAsync;

  private final GUIAdapterUtils adapterUtils;

  private boolean stillWorking = false;

  // The rules do not cover the same trees: the UMLP rules skip @Erased classes and methods, the
  // arguments of this(...) and super(...), and invocations of unknown methods. The JSweet rule
  // skips the candy packages and invocations of unknown methods.
  private int umlpRulesOff = 0;

  private int jsweetRuleOff = 0;

  // the class hierarchy does not change during the scan, so its lookups are cached
  private final Map<TypeElement, Map<String, List<ExecutableElement>>> declaredMethods = new HashMap<>();

  private final Map<TypeElement, Map<String, List<ExecutableElement>>> methodsByName = new HashMap<>();

  private final Map<ExecutableElement, List<ExecutableElement>> matchingMethods = new HashMap<>();

  private final Map<ExecutableElement, Set<ExecutableElement>> matchingSuperMethods = new HashMap<>();

  private final Map<MethodInvocationTree, Optional<TypeElement>> receiverTypes = new IdentityHashMap<>();

  private final Map<TypeElement, Boolean> hwcComponentsOrDirectives = new HashMap<>();

  private final Map<TypeElement, Boolean> lists = new HashMap<>();

  public UMLPAsyncAwaitPropagationScanner(JSweetContext context, boolean transpileToAsync) {
    super(null, context, null);
    this.adapterUtils = new GUIAdapterUtils(types(), context, util());
    this.transpileToAsync = transpileToAsync;
  }

  public void process(List<CompilationUnitTree> compilationUnits, Trees trees) {
    do {
      stillWorking = false;
      for (CompilationUnitTree compilationUnit : compilationUnits) {
        scan(compilationUnit, trees);
      }
    } while (stillWorking);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree topLevel, Trees trees) {
    this.compilationUnit = topLevel;
    boolean candy = util().getPackageFullNameForCompilationUnit(topLevel)
        .startsWith(JSweetConfig.LIBS_PACKAGE + ".");
    return scanWithout(candy, false, () -> super.visitCompilationUnit(topLevel, trees));
  }

  @Override
  public Void visitClass(ClassTree node, Trees trees) {
    // Do not inspect any @Erased classes (and possible mark interfaces as async)
    boolean erased = context.hasAnnotationType(Util.getElement(node), JSweetConfig.ANNOTATION_ERASED);
    return scanWithout(false, erased, () -> super.visitClass(node, trees));
  }

  @Override
  public Void visitMethod(MethodTree node, Trees trees) {
    ExecutableElement method = Util.getElement(node);
    propagateAlongHierarchy(method);
    // Do not check erased methods further -> this leads to very weird js code
    boolean erased = context.hasAnnotationType(method, JSweetConfig.ANNOTATION_ERASED);
    if (umlpRulesOff == 0 && !erased) {
      addAsyncIfOverridingAsync(node, method);
    }
    return scanWithout(false, erased, () -> super.visitMethod(node, trees));
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree invocation, Trees trees) {
    ExecutableElement method = Util.getElement(invocation.getMethodSelect());
    if (method == null) {
      return scanWithout(true, true, () -> super.visitMethodInvocation(invocation, trees));
    }
    if (jsweetRuleOff == 0) {
      awaitAsyncInvocation(invocation, method);
    }
    boolean constructor = method.getKind() == ElementKind.CONSTRUCTOR;
    if (umlpRulesOff == 0 && !constructor) {
      awaitUMLPInvocation(invocation, method, trees);
    }
    return scanWithout(false, constructor, () -> super.visitMethodInvocation(invocation, trees));
  }

  @Override
  public Void visitNewClass(NewClassTree invocation, Trees trees) {
    if (umlpRulesOff == 0 && transpileToAsync) {
      Element e = Util.getElement(((JCTree.JCNewClass) invocation).getTree());
      if (e instanceof ExecutableElement) {
        TypeElement type = (TypeElement) e.getEnclosingElement();
        if (isList(type) || !UMLPAdapterUtils.isJdkType(type)) {
          addAsyncKeywordToMethod();
          addAsyncKeywordToLambda();
        }
      }
    }
    return super.visitNewClass(invocation, trees);
  }

  // For UMLP all List iterations are asynchronous, making methods using for each loops also asynchronous
  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, Trees trees) {
    if (umlpRulesOff == 0) {
      TypeMirror expressionType = Util.getType(node.getExpression());
      if (types().isSubtype(types().erasure(expressionType), types().erasure(util().getType(List.class)))) {
        addAsyncKeywordToMethod();
        addAsyncKeywordToLambda();
      }
    }
    return super.visitEnhancedForLoop(node, trees);
  }

  protected Void scanWithout(boolean jsweetRule, boolean umlpRules, Supplier<Void> scan) {
    jsweetRuleOff += jsweetRule ? 1 : 0;
    umlpRulesOff += umlpRules ? 1 : 0;
    try {
      return scan.get();
    } finally {
      jsweetRuleOff -= jsweetRule ? 1 : 0;
      umlpRulesOff -= umlpRules ? 1 : 0;
    }
  }

  /**
   * An async method makes the methods it overrides async, and a method overriding an async
   * method becomes async.
   */
  protected void propagateAlongHierarchy(ExecutableElement method) {
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      return;
    }
    boolean inObject = ((TypeElement) method.getEnclosingElement()).getQualifiedName()
        .contentEquals("java.lang.Object");
    if (isAsync(method)) {
      addAsyncWithOverloads(method);
      if (!inObject) {
        matchingMethods(method).forEach(this::addAsyncWithOverloads);
      }
    }
    else if (!inObject && matchingMethods(method).stream().anyMatch(this::isAsync)) {
      addAsyncWithOverloads(method);
    }
  }

  protected void addAsyncIfOverridingAsync(MethodTree node, ExecutableElement method) {
    // Constructors CAN NOT be async
    // CBu: now they can ;)
    if (method.getKind() == ElementKind.CONSTRUCTOR
        && method.getEnclosingElement().getKind() != ElementKind.ENUM && transpileToAsync) {
      addAsyncKeywordToMethod(node);
    }
    Element parent = Util.getElement(getParent());
    if (!(parent instanceof TypeElement)) {
      return;
    }
    // Methods overriding async methods must be async again
    if (methodsByName((TypeElement) parent, node.getName().toString()).stream().anyMatch(this::isAsync)) {
      addAsyncKeywordToMethod(node);
    }
    // Also methods which are void and overriding any async methods must be async again
    if ("void".equals(method.getReturnType().toString())
        && matchingSuperMethods((TypeElement) parent, method).stream().anyMatch(this::isAsync)) {
      addAsyncKeywordToMethod(node);
    }
  }

  /** the rule of JSweet's AsyncAwaitPropagationScanner */
  protected void awaitAsyncInvocation(MethodInvocationTree invocation, ExecutableElement method) {
    try {
      if (isAsync(method) && !"void".equals(method.getReturnType().toString())) {
        addAsyncKeywordToMethod();
        addAsyncKeywordToLambda();
        Tree directParent = getParent();
        if (!(context.isAwaitInvocation(invocation) || (directParent instanceof MethodInvocationTree
            && (directParent.toString().equals("await") || directParent.toString().endsWith(".await"))))) {
          context.addAwaitInvocation(invocation);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void awaitUMLPInvocation(MethodInvocationTree invocation, ExecutableElement method, Trees trees) {
    try {
      // Methods overriding async methods from the RTE must be async again
      boolean isAsyncFromSuper = receiverType(invocation, method, trees)
          .map(receiver -> methodsByName(receiver, method.getSimpleName().toString()).stream()
              .anyMatch(this::isAsync))
          .orElse(false);

      if (isAsyncFromSuper || isAsync(method)) {
        context.addAwaitInvocation(invocation);
        addAsyncKeywordToMethod();
        addAsyncKeywordToLambda();
      }
      else if (isHWCComponentOrDirective(Util.getElement(getParent(ClassTree.class)))
          && isInvocatedGemListAccessor(invocation)) {
        context.addAwaitInvocation(invocation);
        addAsyncKeywordToMethod();
        addAsyncKeywordToLambda();
      }
      else if (transpileToAsync && isAsyncFunctionalInterfaceInvocation(method)
          && targetIsAsyncLambda(invocation)) {
        // A java.util.function.* invocation (e.g. Function.apply) whose target is an already-async
        // lambda will generate "await (async target => ...)" in JavaBasicsAdapter. Propagate async
        // upward so the containing lambda is also marked async.
        context.addAwaitInvocation(invocation);
        addAsyncKeywordToMethod();
        addAsyncKeywordToLambda();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected Optional<TypeElement> receiverType(MethodInvocationTree invocation, ExecutableElement method,
      Trees trees) {
    return receiverTypes.computeIfAbsent(invocation, i -> {
      Element receiverType = null;
      ExpressionTree select = invocation.getMethodSelect();
      if (select instanceof MemberSelectTree mst) {
        TypeMirror typeMirror =
            trees.getTypeMirror(new TreePath(getCurrentPath(), mst.getExpression()));
        if (typeMirror instanceof DeclaredType declaredType) {
          receiverType = declaredType.asElement();
        }
      } else {
        // implicit receiver (this)
        receiverType = method.getEnclosingElement();
      }
      return receiverType instanceof TypeElement type ? Optional.of(type) : Optional.empty();
    });
  }

  private boolean isAsyncFunctionalInterfaceInvocation(ExecutableElement method) {
    String enclosingClassName = method.getEnclosingElement().toString();
    return (enclosingClassName.startsWith("java.util.function.")
        && !enclosingClassName.startsWith("java.util.function.Supplier"))
        || enclosingClassName.equals("java.lang.Runnable")
        || enclosingClassName.equals("java.util.concurrent.Callable");
  }

  private boolean targetIsAsyncLambda(MethodInvocationTree invocation) {
    if (!(invocation.getMethodSelect() instanceof MemberSelectTree)) return false;
    Tree target = ((MemberSelectTree) invocation.getMethodSelect()).getExpression();
    target = unwrapParensAndCasts(target);
    return target instanceof LambdaExpressionTree && context.isAsyncLambda((LambdaExpressionTree) target);
  }

  private Tree unwrapParensAndCasts(Tree tree) {
    while (tree instanceof ParenthesizedTree || tree instanceof TypeCastTree) {
      if (tree instanceof ParenthesizedTree) {
        tree = ((ParenthesizedTree) tree).getExpression();
      } else {
        tree = ((TypeCastTree) tree).getExpression();
      }
    }
    return tree;
  }

  private boolean isInvocatedGemListAccessor(MethodInvocationTree invocation) {
    Element invocatedType = types().asElement(Util.getType(invocation));
    return invocatedType instanceof TypeElement && isList((TypeElement) invocatedType);
  }

  protected boolean isList(TypeElement type) {
    return lists.computeIfAbsent(type, t -> adapterUtils.isSubTypeOf(t, "java.util.List"));
  }

  protected boolean isHWCComponentOrDirective(TypeElement type) {
    return hwcComponentsOrDirectives.computeIfAbsent(type, adapterUtils::isHWCComponentOrDirective);
  }

  protected boolean isAsync(Element element) {
    return context.hasAnnotationType(element, JSweetConfig.ANNOTATION_ASYNC);
  }

  private void addAsyncKeywordToMethod() {
    addAsyncKeywordToMethod(getParent(MethodTree.class));
  }

  private void addAsyncKeywordToLambda() {
    LambdaExpressionTree lambdaParent = getParent(LambdaExpressionTree.class);
    if (lambdaParent != null && !context.isAsyncLambda(lambdaParent)) {
      context.addAsyncLambda(lambdaParent);
      stillWorking = true;
    }
  }

  // Adds the asynchronous keyword to the method, overloading methods, and other implementing classes
  // Copied from JSweet Core
  private void addAsyncKeywordToMethod(MethodTree methodTree) {
    if (methodTree != null) {
      ExecutableElement methodElement = Util.getElement(methodTree);
      if (!isAsync(methodElement)) {
        context.addExtraAnnotationType(methodElement, JSweetConfig.ANNOTATION_ASYNC);
        addAsyncToOverloads(methodElement);
        for (ExecutableElement candidate : matchingMethods(methodElement)) {
          context.addExtraAnnotationType(candidate, JSweetConfig.ANNOTATION_ASYNC);
        }
        stillWorking = true;
      }
    }
  }

  private void addAsyncWithOverloads(ExecutableElement method) {
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      return;
    }
    if (!isAsync(method)) {
      context.addExtraAnnotationType(method, JSweetConfig.ANNOTATION_ASYNC);
      stillWorking = true;
    }
    addAsyncToOverloads(method);
  }

  private void addAsyncToOverloads(ExecutableElement method) {
    if (context.isInvalidOverload(method)) {
      OverloadScanner.Overload overload = context.getOverload(
          (TypeElement) method.getEnclosingElement(), method);
      for (OverloadScanner.OverloadMethodEntry overloadEntry : overload.getEntries()) {
        if (!isAsync(overloadEntry.methodElement)) {
          context.addExtraAnnotationType(overloadEntry.methodElement, JSweetConfig.ANNOTATION_ASYNC);
          stillWorking = true;
        }
      }
    }
  }

  /** the method itself and the methods it overrides, in its super classes and interfaces */
  protected List<ExecutableElement> matchingMethods(ExecutableElement method) {
    return matchingMethods.computeIfAbsent(method, m -> {
      List<ExecutableElement> candidates = new ArrayList<>();
      collectMatchingMethods((TypeElement) m.getEnclosingElement(), m.getSimpleName().toString(),
          (ExecutableType) m.asType(), candidates);
      return candidates;
    });
  }

  // Util.collectMatchingMethodDeclarationsInType with overrides, but on the declared methods by name
  private void collectMatchingMethods(TypeElement type, String name, ExecutableType methodType,
      List<ExecutableElement> result) {
    if (type == null) {
      return;
    }
    for (ExecutableElement candidate : declaredMethods(type, name)) {
      if (util().isInvocable((ExecutableType) candidate.asType(), methodType)) {
        result.add(candidate);
      }
    }
    if (type.getSuperclass() != null && !Object.class.getName().equals(type.getSuperclass().toString())) {
      collectMatchingMethods((TypeElement) types().asElement(type.getSuperclass()), name, methodType, result);
    }
    for (TypeElement anInterface : util().getInterfaces(type)) {
      collectMatchingMethods(anInterface, name, methodType, result);
    }
  }

  protected Set<ExecutableElement> matchingSuperMethods(TypeElement type, ExecutableElement method) {
    return matchingSuperMethods.computeIfAbsent(method, m -> adapterUtils.listMatchingSuperMethods(type, m));
  }

  /** the methods with this name in the type, its super classes and the interfaces of all of them */
  protected List<ExecutableElement> methodsByName(TypeElement type, String name) {
    return methodsByName.computeIfAbsent(type, t -> new HashMap<>()).computeIfAbsent(name, n -> {
      List<ExecutableElement> candidates = new ArrayList<>();
      // like Util.findMethodDeclarationsInType, which does not look into interfaces
      for (TypeElement t = type; t != null; t = util().getSuperclass(t)) {
        candidates.addAll(declaredMethods(t, n));
      }
      findMethodsInInterfaces(type, n, candidates, new HashSet<>());
      return candidates;
    });
  }

  /** the methods and constructors declared in the type with this name */
  protected List<ExecutableElement> declaredMethods(TypeElement type, String name) {
    return declaredMethods.computeIfAbsent(type, t -> {
      Map<String, List<ExecutableElement>> byName = new HashMap<>();
      for (Element element : t.getEnclosedElements()) {
        if (element instanceof ExecutableElement method) {
          byName.computeIfAbsent(method.getSimpleName().toString(), k -> new ArrayList<>()).add(method);
        }
      }
      return byName;
    }).getOrDefault(name, Collections.emptyList());
  }

  private void findMethodsInInterfaces(
      TypeElement typeElement,
      String methodName,
      List<ExecutableElement> result,
      Set<TypeElement> visited) {

    if (!visited.add(typeElement)) {
      return;
    } // Direct interfaces
    for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
      Element interfaceElement = types().asElement(interfaceMirror);
      if (interfaceElement instanceof TypeElement) {
        TypeElement interfaceTypeElement = (TypeElement) interfaceElement;
        result.addAll(declaredMethods(interfaceTypeElement, methodName));
        findMethodsInInterfaces(interfaceTypeElement, methodName, result, new HashSet<>());
      }
    }

    // Superclass chain
    TypeMirror superclass = typeElement.getSuperclass();
    if (superclass.getKind() != TypeKind.NONE) {
      Element superElement = types().asElement(superclass);

      if (superElement instanceof TypeElement superTypeElement) {
        findMethodsInInterfaces(
            superTypeElement,
            methodName,
            result,
            visited);
      }
    }
  }
}
