/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetProblem;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.ExpressionTree;
import standalone.com.sun.source.tree.LambdaExpressionTree;
import standalone.com.sun.source.tree.MemberSelectTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.ParenthesizedTree;
import standalone.com.sun.source.tree.Tree;
import standalone.com.sun.source.tree.TypeCastTree;
import standalone.com.sun.source.util.TreeScanner;
import umlp.jsweet.extension.AbstractAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;

/**
 * This adapter is responsible to enable the usage of some additional {@link java.util} classes.
 */
public class JavaBasicsAdapter extends AbstractAdapter {

  public JavaBasicsAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    LinkedHashMap<String,String> defaultTypeMappings = new LinkedHashMap<>();
    defaultTypeMappings.put(Spliterator.class.getName(), "any");
    addTypeMappings(defaultTypeMappings);
  }

  /**
   * Can be used to adapt method invocations like {@code Objects.requireNonNull(...)}
   */
  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }
    String targetType = invocation.getTargetType().toString();
    switch (targetType) {
      case "java.util.Objects":
        if (substituteMethodInvocation_JavaUtilObjects(invocation)) {
          return true;
        }
        break;
      case "java.util.Arrays":
        if (substituteMethodInvocation_JavaUtilArrays(invocation)) {
          getPrinter().useModule(new ModuleImportDescriptor("Arrays", getLibraryLocation()));
          return true;
        }
        break;
      case "java.lang.StringBuilder":
      case "java.lang.StringBuffer":
        if (substituteAsyncStringBuilderAppend(invocation)) {
          return true;
        }
        break;
      case "java.lang.String":
        if (substituteMethodInvocation_JavaLangString(invocation)) {
          return true;
        }
        // TODO this is pretty hidden to make java.lang.String#join async because it is need when it receives a List
        else if(invocation.getMethodName().equals("join")) {
          getPrinter().print("(await ");
          boolean substitution = super.substituteMethodInvocation(invocation);
          getPrinter().println(")");
          return substitution;
        }
        break;
      default:
        if (isJavaUtilFunctionSupplier(invocation, targetType)
            && substituteMethodInvocation_JavaUtilFunctionSupplier(invocation)) {
          return true;
        }
        if (isAsyncFunctionalInterface(targetType)
            && substituteMethodInvocation_AsyncFunctionalInvocation(invocation)) {
          return true;
        }
        break;
    }

    String targetClassName = invocation.getMethod().getEnclosingElement().toString();
    ExtendedElement targetExpression = invocation.getTargetExpression();
    if (targetExpression != null && targetExpression.getTypeAsElement() != null) {
      targetClassName = targetExpression.getTypeAsElement().toString();
    }
    if (targetClassName != null
        && (targetExpression != null || invocation.getMethod().getModifiers().contains(
        Modifier.STATIC))) {
    }

    return super.substituteMethodInvocation(invocation);
  }

  private boolean isJavaUtilFunctionSupplier(MethodInvocationElement invocation, String targetType) {
    return targetType.startsWith("java.util.function.Supplier")
        || invocation.getMethod().getEnclosingElement().toString()
        .startsWith("java.util.function.Supplier");
  }

  public boolean substituteMethodInvocation_JavaUtilFunctionSupplier(MethodInvocationElement invocation) {
    switch (invocation.getMethodName()) {
      case "get":
        if (canUseAwaitInCurrentExpression()) {
          MethodInvocationTree methodInvocationTree = ExtendedElementFactory.toTree(invocation);
          LambdaExpressionTree supplierLambda = getSupplierLambda(methodInvocationTree);
          if (supplierLambda != null) {
            this.print("(await (");
            if (!context.isAsyncLambda(supplierLambda)) {
              this.print("async ");
            }
            this.print(supplierLambda);
            this.print(")())");
          }
          else {
            this.print("(await ");
            getPrinter().printDefaultMethodInvocation(methodInvocationTree);
            this.print(")");
          }
          return true;
        }
        break;
    }
    return false;
  }

  private boolean canUseAwaitInCurrentExpression() {
    LambdaExpressionTree lambda = getPrinter().getParent(LambdaExpressionTree.class);
    return isInAsyncMethod() && (lambda == null || context.isAsyncLambda(lambda) || isSupplierLambda(lambda));
  }

  private LambdaExpressionTree getSupplierLambda(MethodInvocationTree invocation) {
    if (!(invocation.getMethodSelect() instanceof MemberSelectTree)) {
      return null;
    }
    ExpressionTree target = ((MemberSelectTree) invocation.getMethodSelect()).getExpression();
    Tree unwrapped = unwrapSupplierTarget(target);
    return unwrapped instanceof LambdaExpressionTree ? (LambdaExpressionTree) unwrapped : null;
  }

  private Tree unwrapSupplierTarget(Tree tree) {
    while (tree instanceof ParenthesizedTree || tree instanceof TypeCastTree) {
      if (tree instanceof ParenthesizedTree) {
        tree = ((ParenthesizedTree) tree).getExpression();
      }
      else {
        tree = ((TypeCastTree) tree).getExpression();
      }
    }
    return tree;
  }

  private boolean isSupplierLambda(LambdaExpressionTree lambda) {
    TypeMirror type = Util.getType(lambda);
    return type != null && type.toString().startsWith("java.util.function.Supplier");
  }

  public boolean substituteMethodInvocation_JavaUtilObjects(MethodInvocationElement invocation) {
    switch (invocation.getMethodName()) {
      case "requireNonNull":
        String msg = invocation.getArgumentCount() > 1 ? invocation.getArgument(1).toString() : "";
        if(types().isSameType(
          types().erasure(invocation.getArgument(0).getType()),
          types().erasure(getContext().elements.getTypeElement(Optional.class.getName()).asType()))) {
          // TODO Optional Case is wonky
          this.print("/* requireNonNull with Opt as arg*/")
              .print(String.format(
                  "(obj => {if (obj === undefined) { throw Object.defineProperty(new Error(%s), '__classes', { configurable: true, value: ['java.lang.Throwable','java.lang.Object','java.lang.RuntimeException','java.lang.NullPointerException','java.lang.Exception'] }) } else { return obj}}) (",
                  msg))
              .print(invocation.getArgument(0))
              .print(")");
          return true;
        }
        this.print("/* requireNonNull */")
            .print(String.format(
                "(obj => {if (obj === undefined || obj === null) { throw Object.defineProperty(new Error(%s), '__classes', { configurable: true, value: ['java.lang.Throwable','java.lang.Object','java.lang.RuntimeException','java.lang.NullPointerException','java.lang.Exception'] }) } else { return obj}}) (",
                msg))
            .print(invocation.getArgument(0))
            .print(")");
        return true;
      case "equals":
        String dateType = null;
        for (ExtendedElement argument : invocation.getArguments()) {
          switch (argument.getType().toString()) {
            case "java.time.LocalDateTime":
            case "java.time.LocalDate":
            case "java.util.Date":
              dateType = argument.getType().toString();
          }
        }
        if (dateType != null) {
          // Same comparison as JavaTimeAdapter: LocalDate by its date fields, LocalDateTime by its
          // wall-clock fields (both read with the local getters), java.util.Date by its instant.
          String keyA;
          String keyB;
          switch (dateType) {
            case "java.time.LocalDate":
              keyA = "Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())";
              break;
            case "java.time.LocalDateTime":
              keyA = "Date.UTC(a.getFullYear(), a.getMonth(), a.getDate(), a.getHours(),"
                  + " a.getMinutes(), a.getSeconds(), a.getMilliseconds())";
              break;
            default:
              keyA = "a.getTime()";
          }
          keyB = keyA.replace("a.", "b.");
          this.print("/* equals */")
              .print("((a, b) => { return a == null || b == null ? a == b : "
                  + keyA + " === " + keyB + "; }) (")
              .printArgList(invocation.getArguments())
              .print(")");
        }
        else {
          getPrinter().useModule(new ModuleImportDescriptor("JavaUtilUtils", getLibraryLocation()));
          boolean inAsyncMethod = isInAsyncMethod();
          this.print("/* equals */");
          if (inAsyncMethod) {
            this.print("await ");
          }
          this.print(inAsyncMethod ? "JavaUtilUtils.equals(" : "JavaUtilUtils.equalsSync(")
              .printArgList(invocation.getArguments())
              .print(")");
        }
        return true;
    }
    return false;
  }

  protected boolean isInAsyncMethod() {
    MethodTree methodTree = getPrinter().getParent(MethodTree.class);
    if (methodTree == null) {
      return false;
    }
    Element methodElement = Util.getElement(methodTree);
    return methodElement != null && context.hasAnnotationType(methodElement, JSweetConfig.ANNOTATION_ASYNC);
  }

  public boolean substituteMethodInvocation_JavaUtilArrays(MethodInvocationElement invocation) {
    switch (invocation.getMethodName()) {
      case "asList": {
        if(invocation.getArgumentCount() == 1 && invocation.getArgument(0).getType().getKind() == TypeKind.ARRAY) {
          // emulate Java varargs
          this.print("Arrays.asList(...").print(invocation.getArgument(0)).print(")");
          return true;
        } else {
          this.print("Arrays.asList(").printArgList(invocation.getArguments()).print(")");
          return true;
        }
      }
      case "copyOf": {
        if (invocation.getArgumentCount() == 2) {
          this.print("Arrays.copyOf(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else {
          report(invocation, JSweetProblem.USER_ERROR, "Signature not supported, use copyOf(T[], int) instead.");
          return false;
        }
      }
      case "copyOfRange": {
        if (invocation.getArgumentCount() == 3) {
          this.print("Arrays.copyOfRange(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else {
          report(invocation, JSweetProblem.USER_ERROR, "Signature not supported, use copyOfRange(T[], int, int) instead.");
          return false;
        }
      }
      case "fill": {
        if (invocation.getArgumentCount() == 2) {
          this.print("Arrays.fill(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else if (invocation.getArgumentCount() == 4) {
          this.print("Arrays.fillRange(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else {
          report(invocation, JSweetProblem.USER_ERROR, "Parameter count mismatch.");
          return false;
        }
      }
      case "sort": {
        if (invocation.getArgumentCount() == 1) {
          this.print("Arrays.sort(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else {
          report(invocation, JSweetProblem.USER_ERROR, "Signature not supported, use sort(T[])");
          return false;
        }
      }
      case "stream": {
        if (invocation.getArgumentCount() == 1) {
          this.print("Arrays.stream(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else if (invocation.getArgumentCount() == 3) {
          this.print("Arrays.streamRange(").printArgList(invocation.getArguments()).print(")");
          return true;
        } else {
          report(invocation, JSweetProblem.USER_ERROR, "Parameter count mismatch.");
          return false;
        }
      }
      case "toString": {
        this.print("Arrays.toString(").printArgList(invocation.getArguments()).print(")");
        return true;
      }
      default: {
        report(invocation, JSweetProblem.USER_ERROR, "Unsupported method.");
        return false;
      }
    }
  }

  public boolean substituteMethodInvocation_JavaLangString(MethodInvocationElement invocation) {
    switch (invocation.getMethodName()) {
      case "format":
        this.print("StringHelper.format(").printArgList(invocation.getArguments()).print(")");
        return true;
      case "isBlank": {
        print("(");
        print(invocation.getTargetExpression());
        print(".trim().length === 0)");
        return true;
      }
      case "split": {
        print("StringHelper.split(");
        print(invocation.getTargetExpression());
        print(", ");
        printArgList(invocation.getArguments());
        print(")");
        return true;
      }
    }
    return false;
  }

  // An awaited append argument must not land in the core adapter's synchronous append lambda.
  private boolean substituteAsyncStringBuilderAppend(MethodInvocationElement invocation) {
    if (!invocation.getMethodName().equals("append") || invocation.getArgumentCount() != 1) {
      return false;
    }
    if (!printsAwait(ExtendedElementFactory.toTree(invocation.getArgument(0)))) {
      return false;
    }
    print("(/* async append */ await (async sb => { sb.str += <any>")
        .print(invocation.getArgument(0)).print("; return sb; })(")
        .print(invocation.getTargetExpression()).print("))");
    return true;
  }

  // True if printing this expression emits a top-level await (not inside a nested lambda).
  private boolean printsAwait(Tree tree) {
    if (tree == null) {
      return false;
    }
    Boolean found = tree.accept(new TreeScanner<Boolean, Void>() {
      @Override
      public Boolean visitMethodInvocation(MethodInvocationTree inv, Void unused) {
        if (context.isAwaitInvocation(inv) || isStringJoin(inv)) {
          return true;
        }
        return super.visitMethodInvocation(inv, unused);
      }

      @Override
      public Boolean visitLambdaExpression(LambdaExpressionTree lambda, Void unused) {
        // awaits inside a nested lambda stay inside that lambda
        return false;
      }

      @Override
      public Boolean reduce(Boolean r1, Boolean r2) {
        return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
      }
    }, null);
    return Boolean.TRUE.equals(found);
  }

  private boolean isStringJoin(MethodInvocationTree invocation) {
    Element method = Util.getElement(invocation.getMethodSelect());
    return method != null && method.getSimpleName().contentEquals("join")
        && method.getEnclosingElement() != null
        && method.getEnclosingElement().toString().equals("java.lang.String");
  }

  private boolean isAsyncFunctionalInterface(String targetType) {
    // Supplier has dedicated handling above; everything else in java.util.function.* or Runnable/Callable
    return (targetType.startsWith("java.util.function.")
            && !targetType.startsWith("java.util.function.Supplier"))
        || targetType.equals("java.lang.Runnable")
        || targetType.equals("java.util.concurrent.Callable");
  }

  private boolean substituteMethodInvocation_AsyncFunctionalInvocation(MethodInvocationElement invocation) {
    ExtendedElement target = invocation.getTargetExpression();
    if (target == null) return false;
    List<ExtendedElement> arguments = invocation.getArguments();
    if (!targetOrArgsAreAsync(target, arguments)) return false;

    print("await (async target => (typeof target === 'function') ? target(")
        .printArgList(arguments)
        .print(") : (<any>target).").print(invocation.getMethodName()).print("(")
        .printArgList(arguments).print("))(")
        .print(target).print(")");
    return true;
  }

  private boolean targetOrArgsAreAsync(ExtendedElement target, List<ExtendedElement> arguments) {
    if (containsAsyncLambda(ExtendedElementFactory.toTree(target))) return true;
    for (ExtendedElement arg : arguments) {
      Tree tree = ExtendedElementFactory.toTree(arg);
      if (tree instanceof MethodInvocationTree && context.isAwaitInvocation((MethodInvocationTree) tree)) return true;
      if (containsAsyncLambda(tree)) return true;
    }
    return false;
  }

  private boolean containsAsyncLambda(Tree tree) {
    if (tree == null) return false;
    if (tree instanceof LambdaExpressionTree)
      return context.isAsyncLambda((LambdaExpressionTree) tree);
    // Unwrap casts and parentheses (reuses existing helper)
    Tree unwrapped = unwrapSupplierTarget(tree);
    if (unwrapped != tree) return containsAsyncLambda(unwrapped);
    // Recurse into member-select target (e.g. asyncLambda.apply)
    if (tree instanceof MemberSelectTree)
      return containsAsyncLambda(((MemberSelectTree) tree).getExpression());
    return false;
  }

  @Override
  public TypeMirror mapType(TypeMirror type) {
    return null;
  }

  @Override
  protected void initStaticLibrary() {
    addToStaticLibrary("java.util.concurrent.atomic.AtomicReference");
    addToStaticLibrary("java.util.Random");
    putToStaticLibrary("java.lang.String", "StringHelper");
  }
}
