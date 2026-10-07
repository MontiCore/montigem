package umlp.jsweet.extension.cocos;

import de.se_rwth.commons.logging.Log;
import jsweet.lang.Erased;
import jsweet.lang.Replace;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.TranspilationHandler;
import org.jsweet.transpiler.util.AbstractTreeScanner;
import standalone.com.sun.source.tree.ClassTree;
import standalone.com.sun.source.tree.CompilationUnitTree;
import standalone.com.sun.source.tree.ExpressionTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.NewClassTree;
import standalone.com.sun.source.tree.Tree;
import standalone.com.sun.source.tree.VariableTree;
import standalone.com.sun.source.util.TreePath;
import standalone.com.sun.source.util.Trees;
import standalone.com.sun.tools.javac.code.Symbol;
import de.se_rwth.commons.SourcePosition;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import static umlp.jsweet.extension.common.FutureAdapter.isFutureAdaptedTypes;

/**
 * This class does not adapt anything but throws warnings when finding Java Concepts that we cannot
 * transpile.
 */
public class JSweetCoCoChecker extends AbstractTreeScanner {

  public JSweetCoCoChecker(JSweetContext context) {
    super(null, context, null);
  }

  /**
   * @param compilationUnit the scanned unit, JSweet needs it to report the source position of an internal error
   */
  public JSweetCoCoChecker(JSweetContext context, CompilationUnitTree compilationUnit) {
    super(null, context, compilationUnit);
  }

  protected JSweetCoCoChecker(TranspilationHandler logHandler, JSweetContext context, CompilationUnitTree compilationUnit) {
    super(logHandler, context, compilationUnit);
  }

  @Override
  public Void visitClass(ClassTree classTree, Trees trees) {
    Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) trees.getElement(getCurrentPath());

    boolean classIsBackendOnlyOrErased = classSymbol.getAnnotationMirrors().stream()
        .anyMatch(a -> a.getAnnotationType().toString()
            .equals("umlp.jsweet.extension.annotation.BackendOnly")
            || a.getAnnotationType().toString()
            .equals(Erased.class.getCanonicalName()));
    if(classIsBackendOnlyOrErased) {
      // Stop visitation here
      return null;
    }
    super.visitClass(classTree, trees);
    return null;
  }

  @Override
  public Void visitMethod(MethodTree methodTree, Trees trees) {
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) trees.getElement(getCurrentPath());

    boolean methodOrAttrIsBackendOnlyOrErased = methodSymbol.getAnnotationMirrors().stream()
        .anyMatch(a -> a.getAnnotationType().toString()
            .equals("umlp.jsweet.extension.annotation.BackendOnly")
            || a.getAnnotationType().toString()
            .equals(Erased.class.getCanonicalName())
            || a.getAnnotationType().toString().equals(Replace.class.getCanonicalName()));
    if(methodOrAttrIsBackendOnlyOrErased) {
      // Stop visitation here
      return null;
    }
    super.visitMethod(methodTree, trees);
    return null;
  }

  @Override
  public Void visitVariable(VariableTree variableTree, Trees trees) {
    Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) trees.getElement(getCurrentPath());
    if(varSymbol.enclClass() == null || !(varSymbol.owner instanceof Symbol.ClassSymbol)) {
      super.visitVariable(variableTree, trees);
      return null;
    }
    boolean methodOrAttrIsBackendOnlyOrErased = varSymbol.getAnnotationMirrors().stream()
        .anyMatch(a -> a.getAnnotationType().toString()
            .equals("umlp.jsweet.extension.annotation.BackendOnly")
            || a.getAnnotationType().toString()
            .equals(Erased.class.getCanonicalName()));
    if(methodOrAttrIsBackendOnlyOrErased) {
      // Stop visitation here
      return null;
    }

    Element element = trees.getElement(getCurrentPath());

    if (element != null && element.getKind() == ElementKind.FIELD) {
      ExpressionTree initializer = variableTree.getInitializer();
      // Determine if initializer is Async
      if (initializer != null) {
        if(element.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
          //Log.error("0xEE010: Field initialization in Java Declaration assignment now allowed. "+ getStartSourcePosition(getCurrent()));
        }
      }
    }

    super.visitVariable(variableTree, trees);
    return null;
  }

  /**
   * Delegates method invocations to the unsupported APIs checker.
   *
   * @param invocation the method invocation tree
   * @param trees the trees instance
   * @return null
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree invocation, Trees trees) {
    // a path from the current one, searching the compilation unit for each tree is costly
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) trees.getElement(
        new TreePath(getCurrentPath(), invocation.getMethodSelect()));
    checkUnsupportedApis(invocation, methodSymbol);
    super.visitMethodInvocation(invocation, trees);
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, Trees trees) {
      TreePath path = getCurrentPath();
      Symbol.MethodSymbol methodSymbol = null;

      // Attempt 1: Direct path
      if (path != null && path.getLeaf() == newClassTree) {
        Object element = trees.getElement(path);
        // Safe type check replaces the error-prone cast
        if (element instanceof Symbol.MethodSymbol) {
          methodSymbol = (Symbol.MethodSymbol) element;
        }
      }

      // Attempt 2: Fallback to searching from root
      if (methodSymbol == null) {
        TreePath fallbackPath = trees.getPath(getCurrentPath().getCompilationUnit(), newClassTree);
        // Null check prevents exceptions in getElement()
        if (fallbackPath != null) {
          Object element = trees.getElement(fallbackPath);
          if (element instanceof Symbol.MethodSymbol) {
            methodSymbol = (Symbol.MethodSymbol) element;
          }
        }
      }

      checkUnsupportedApis(newClassTree, methodSymbol);

    super.visitNewClass(newClassTree, trees);
    return null;
  }

  /**
   * Checks the use of Java APIs that are not or only partially supported according to the User Guide
   * and issues corresponding warnings.
   */
  protected void checkUnsupportedApis(Tree tree, Symbol.MethodSymbol methodSymbol) {

    if (methodSymbol == null) {
      return;
    }

    String methodName = methodSymbol.getSimpleName().toString();
    String ownerFqn = methodSymbol.owner != null
        ? methodSymbol.owner.getQualifiedName().toString()
        : "";

    // 1) Java 9+ factory methods like List.of / Map.of / Set.of
    if (methodName.equals("of")
        && ( "java.util.Map".equals(ownerFqn)
        || "java.util.Set".equals(ownerFqn))) {
      Log.warn("0xEE005: " + ownerFqn + ".of(...) is not supported by our transpilation. "
              + "Use Collections.* helpers (e.g., Collections.emptyList/singletonList) "
              + "or Guava's Lists.newArrayList instead.",
          getStartSourcePosition(getContext(),getCurrentPath().getCompilationUnit(),tree));
      return;
    }

    if ("java.util.Collections".equals(ownerFqn)) {
      switch (methodName) {
        case "emptySet":
        case "emptyMap":
        case "singletonMap":
        case "unmodifiableMap":
        case "unmodifiableSet":
        case "unmodifiableSortedSet":
        case "sort":
        case "reverse":
        case "shuffle":
        case "binarySearch":
        case "copy":
        case "fill":
        case "rotate":
        case "swap":
        case "min":
        case "max":
        case "checkedCollection":
        case "checkedList":
        case "checkedMap":
        case "checkedSet":
        case "checkedSortedSet":
        case "synchronizedCollection":
        case "synchronizedList":
        case "synchronizedMap":
        case "synchronizedSet":
        case "synchronizedSortedSet":
          Log.warn("0xEE006: java.util.Collections." + methodName + "(...) is not supported by our transpilation. "
                  + "Use simple list/map operations or custom helpers instead.",
              getStartSourcePosition(getContext(),getCurrentPath().getCompilationUnit(),tree));
          return;
        default:
      }
    }

    if ("java.util.Objects".equals(ownerFqn)) {
      switch (methodName) {
        case "hash":
        case "hashCode":
        case "deepEquals":
        case "compare":
        case "compareUnsigned":
        case "requireNonNullElse":
        case "requireNonNullElseGet":
          Log.warn("0xEE008: java.util.Objects." + methodName + "(...) is not explicitly supported "
                  + "and may not behave as expected in transpiled code. "
                  + "Prefer requireNonNull/equals or implement comparison/hash logic explicitly.",
              getStartSourcePosition(getContext(),getCurrentPath().getCompilationUnit(),tree));
          return;
        default:
      }
    }

    if (ownerFqn.startsWith("java.time.")
        && !ownerFqn.equals("java.time.LocalDateTime")
        && !ownerFqn.equals("java.time.LocalDate")) {
      Log.warn("0xEE009: Calls to " + ownerFqn + "." + methodName + "(...) are not explicitly supported. "
              + "Only java.time.LocalDateTime, java.time.LocalDate and java.util.Date "
              + "with their standard methods are supported in transpiled code.",
          getStartSourcePosition(getContext(),getCurrentPath().getCompilationUnit(),tree));
      return;
    }

    if (ownerFqn.startsWith("java.io.")
        || ownerFqn.startsWith("java.net.")
        || ownerFqn.startsWith("java.sql.")
        || ownerFqn.startsWith("java.security.")
        || ownerFqn.startsWith("java.lang.reflect.")) {
      if(ownerFqn.endsWith("PrintStream")) {
        // System.out and System.err are transpiled correctly no need to throw a Warning
        return;
      }
      Log.warn("0xEE003: Calls to " + ownerFqn + "." + methodName + " are not supported in transpiled code. "
              + "Move this logic to backend-only code or mark it with @BackendOnly/@Erased.",
          getStartSourcePosition(getContext(),getCurrentPath().getCompilationUnit(),tree));
      return;
    }

    if (ownerFqn.startsWith("java.util.concurrent.")) {
      if (! (ownerFqn.equals("java.util.concurrent.atomic.AtomicReference") || isFutureAdaptedTypes(ownerFqn)) ) {
        Log.warn("0xEE004: java.util.concurrent APIs are not supported in transpiled code. "
                + "Use async/await and @Synchronized instead of Java threads or executors.",
            getStartSourcePosition(getContext(), getCurrentPath().getCompilationUnit(),tree));
      }
    }
  }

  public static SourcePosition getStartSourcePosition(JSweetContext context, CompilationUnitTree cu,
                                                      standalone.com.sun.source.tree.Tree tree) {
    long startSP = context.trees.getSourcePositions().getStartPosition(cu, tree);

    if(cu != null &&
        cu.getLineMap() != null &&
        cu.getSourceFile() != null) {

      String fileName = cu.getSourceFile().toUri().getPath();
      int line = (int) cu.getLineMap().getLineNumber(startSP);
      int column = (int) cu.getLineMap().getColumnNumber(startSP);

      return new SourcePosition(line, column, fileName);
    }

    // Return a default position if the exact source cannot be determined
    return SourcePosition.getDefaultSourcePosition();
  }


}
