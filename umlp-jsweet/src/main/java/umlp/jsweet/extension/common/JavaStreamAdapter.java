/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.LambdaExpressionTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.Tree;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import de.se_rwth.commons.logging.Log;


public class JavaStreamAdapter extends AbstractAdapter {

  public JavaStreamAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeJavaStreamTypeMapping());
  }

  public Function<TypeMirror, String> computeJavaStreamTypeMapping() {
    return (TypeMirror type) -> {
      if (isPrimitiveStream(type)) {
        return "Array<number>";
      }
      TypeMirror mappedType = mapStreamType(type);
      if (mappedType != null) {
        String typeName = this.getMappedType(mappedType);
        if ("any".equals(typeName)) {
          return "Array<any>";
        }
        return "Array<" + typeName + ">";
      }
      if (isStream(type)) {
        return "Array<any>";
      }
      return null;
    };
  }

  public TypeMirror mapStreamType(TypeMirror type) {
    if (isStream(type) && type instanceof DeclaredType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
      if (typeArgs.size() > 0) {
        return typeArgs.get(0);
      }
    }
    return null;
  }

  protected static final Set<String> SUPPORTED_STREAM_METHODS = new LinkedHashSet<>(Arrays.asList(
      "filter", "map", "flatMap", "distinct", "sorted", "peek", "limit", "skip",
      "forEach", "forEachOrdered", "toArray", "reduce", "collect",
      "min", "max", "count", "anyMatch", "allMatch", "noneMatch",
      "findFirst", "findAny", "empty", "of", "range", "rangeClosed",
      "toList"
  ));

  protected static final Set<String> NUMERICAL_MAP_METHODS = new LinkedHashSet<>(Arrays.asList(
      "mapToInt", "mapToLong", "mapToDouble",
      "flatMapToInt", "flatMapToLong", "flatMapToDouble"
  ));

  @Override
  public TypeMirror mapType(TypeMirror type) {
    // Only change the emitted TypeScript type string. JSweet's Java model should still see Stream<T>.
    return null;
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();

    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }
    String targetTypeFqn = getFqn(invocation.getTargetType());


    // 1. collection.stream() -> array conversion without the Stream runtime.
    if (methodName.equals("stream") && isCollection(invocation.getTargetType())) {
      if (requiresArrayConversionViaRuntime(invocation)) {
        if (isInAsyncMethod()) {
          print("(await GemListUtil.toArraySync(");
        }
        else {
          print("GemListUtil.toArray(");
        }
        printTargetOrThis(invocation);
        print(isInAsyncMethod() ? "))" : ")");
        getPrinter().useModule(new ModuleImportDescriptor("GemListUtil", getLibraryLocation()));
      } else {
        print("Array.from(");
        printTargetOrThis(invocation);
        print(")");
      }
      return true;
    }

    // 2. Stream methods
    if (isStream(invocation.getTargetType())) {
      // Check for supported methods
      if (!SUPPORTED_STREAM_METHODS.contains(methodName) && !NUMERICAL_MAP_METHODS.contains(methodName)) {
        Log.error("0xEE010: Stream method '" + methodName + "' is currently not supported by our transpilation.");
      }

      if (substituteStaticStreamSource(invocation)) {
        return true;
      }

      if (NUMERICAL_MAP_METHODS.contains(methodName)) {
        return substituteStreamOperation(invocation, methodName.startsWith("flatMap") ? "flatMap" : "map");
      }

      if (substituteStreamOperation(invocation, methodName)) {
        return true;
      }
    }

    if (methodName.equals("stream") && targetTypeFqn.equals("java.util.Arrays")) {
      return substituteArraysStream(invocation);
    }

    // 3. Collectors...
    if (targetTypeFqn.equals("java.util.stream.Collectors")) {
      switch (methodName) {
        case "toList" -> {
          print("({ name: 'toList' })");
          return true;
        }
        case "toSet" -> {
          print("({ name: 'toSet' })");
          return true;
        }
        case "joining" -> {
          print("({ name: 'joining', delimiter: ");
          if (invocation.getArgumentCount() == 0) {
            print("''");
          } else {
            print(invocation.getArgument(0));
          }
          print(" })");
          return true;
        }
        case "counting" -> {
          print("({ name: 'counting' })");
          return true;
        }
      }
    }

    return super.substituteMethodInvocation(invocation);
  }

  @Override
  public boolean substituteInstanceof(String exprStr, ExtendedElement expr, TypeMirror type) {
    if (type != null && isStream(type)) {
      print("(").print(exprStr).print(" != null)");
      return true;
    }
    return super.substituteInstanceof(exprStr, expr, type);
  }

  private boolean substituteStaticStreamSource(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    switch (methodName) {
      case "of" -> {
        print("((...__args) => __args.length === 1 && Array.isArray(__args[0]) ? __args[0] : __args)(")
            .printArgList(invocation.getArguments())
            .print(")");
        return true;
      }
      case "empty" -> {
        print("[]");
        return true;
      }
      case "range" -> {
        printRange(invocation, false);
        return true;
      }
      case "rangeClosed" -> {
        printRange(invocation, true);
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private boolean substituteArraysStream(MethodInvocationElement invocation) {
    if (invocation.getArgumentCount() == 1) {
      print("Array.from(").print(invocation.getArgument(0)).print(")");
      return true;
    }
    if (invocation.getArgumentCount() == 3) {
      print("Array.from(").print(invocation.getArgument(0)).print(").slice(")
          .print(invocation.getArgument(1)).print(", ").print(invocation.getArgument(2)).print(")");
      return true;
    }
    return false;
  }

  private boolean substituteStreamOperation(MethodInvocationElement invocation, String methodName) {
    switch (methodName) {
      case "map" -> {
        printMap(invocation);
        return true;
      }
      case "filter" -> {
        printFilter(invocation);
        return true;
      }
      case "flatMap" -> {
        printFlatMap(invocation);
        return true;
      }
      case "distinct" -> {
        print("Array.from(new Set(").print(invocation.getTargetExpression()).print("))");
        return true;
      }
      case "sorted" -> {
        print("Array.from(").print(invocation.getTargetExpression()).print(").sort(");
        if (invocation.getArgumentCount() > 0) {
          printArgList(invocation.getArguments());
        }
        print(")");
        return true;
      }
      case "peek" -> {
        print("(").print(invocation.getTargetExpression()).print(").map((__item) => { (")
            .print(invocation.getArgument(0)).print(")(__item); return __item; })");
        return true;
      }
      case "limit" -> {
        print("(").print(invocation.getTargetExpression()).print(").slice(0, ").print(invocation.getArgument(0)).print(")");
        return true;
      }
      case "skip" -> {
        print("(").print(invocation.getTargetExpression()).print(").slice(").print(invocation.getArgument(0)).print(")");
        return true;
      }
      case "forEach", "forEachOrdered" -> {
        print("(").print(invocation.getTargetExpression()).print(").forEach(").printArgList(invocation.getArguments()).print(")");
        return true;
      }
      case "toArray" -> {
        print("Array.from(").print(invocation.getTargetExpression()).print(")");
        return true;
      }
      case "toList" -> {
        if(isInAsyncMethod()) {
          print("(await ");
        }
        print("new "+CollectionsAdapter.CustomAsyncListImplementationConstructor+"(").print(invocation.getTargetExpression()).print(")");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName, getLibraryLocation()));
        if(isInAsyncMethod()) {
          print(")");
        }
        return true;
      }
      case "reduce" -> {
        printReduce(invocation);
        return true;
      }
      case "collect" -> {
        printCollect(invocation);
        return true;
      }
      case "min" -> {
        printMinMax(invocation, true);
        return true;
      }
      case "max" -> {
        printMinMax(invocation, false);
        return true;
      }
      case "count" -> {
        print("Array.from(").print(invocation.getTargetExpression()).print(").length");
        return true;
      }
      case "anyMatch" -> {
        printAnyMatch(invocation);
        return true;
      }
      case "allMatch" -> {
        printAllMatch(invocation);
        return true;
      }
      case "noneMatch" -> {
        printNoneMatch(invocation);
        return true;
      }
      case "findFirst", "findAny" -> {
        print("((__stream) => __stream.length > 0 ? __stream[0] : null)(")
            .print(invocation.getTargetExpression()).print(")");
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private void printMap(MethodInvocationElement invocation) {
    ExtendedElement mapper = invocation.getArgument(0);
    if (isAsyncLikeArg(mapper)) {
      print("(await Promise.all((").print(invocation.getTargetExpression()).print(").map(");
      printAsyncArg(mapper);
      print(")))");
    } else {
      print("(").print(invocation.getTargetExpression()).print(").map(").print(mapper).print(")");
    }
  }

  private void printFilter(MethodInvocationElement invocation) {
    ExtendedElement predicate = invocation.getArgument(0);
    if (isAsyncLikeArg(predicate)) {
      print("(await (async (__stream, __predicate) => { const __flags = await Promise.all(__stream.map(__predicate)); return __stream.filter((_, __i) => __flags[__i]); })(")
          .print(invocation.getTargetExpression()).print(", ");
      printAsyncArg(predicate);
      print("))");
    } else {
      print("(").print(invocation.getTargetExpression()).print(").filter(").print(predicate).print(")");
    }
  }

  private void printFlatMap(MethodInvocationElement invocation) {
    ExtendedElement mapper = invocation.getArgument(0);
    if (isAsyncLikeArg(mapper)) {
      print("(await (async (__stream: Array<any>, __mapper: (__item: any) => any) => (await Promise.all(__stream.map(__mapper))).flatMap((__mapped: any) => __mapped == null ? [] : Array.isArray(__mapped) ? __mapped : [__mapped]))(")
          .print(invocation.getTargetExpression()).print(", ");
      printAsyncArg(mapper);
      print("))");
    } else {
      print("((__stream: Array<any>, __mapper: (__item: any) => any) => __stream.flatMap((__item: any) => { const __mapped = __mapper(__item); return __mapped == null ? [] : Array.isArray(__mapped) ? __mapped : [__mapped]; }))(")
          .print(invocation.getTargetExpression()).print(", ").print(mapper).print(")");
    }
  }

  private void printReduce(MethodInvocationElement invocation) {
    print("((__stream) => ");
    if (invocation.getArgumentCount() == 1) {
      print("__stream.length === 0 ? null : __stream.slice(1).reduce(")
          .print(invocation.getArgument(0)).print(", __stream[0])");
    } else {
      print("__stream.reduce(").print(invocation.getArgument(1)).print(", ")
          .print(invocation.getArgument(0)).print(")");
    }
    print(")(").print(invocation.getTargetExpression()).print(")");
  }

  private void printCollect(MethodInvocationElement invocation) {
    String collector = getCollectorName(invocation);
    switch (collector) {
      case "toList" -> {
        if(isInAsyncMethod()) {
          print("(await ");
        }
        print("new AsyncList().__AsyncListConstructor(").print(invocation.getTargetExpression()).print(")");
        if(isInAsyncMethod()) {
          print(")");
        }
      }
      case "toSet" -> {
        if(isInAsyncMethod()) {
          print("(await ");
        }
        print("new AsyncList().__AsyncListConstructor(Array.from(new Set(").print(invocation.getTargetExpression()).print(")))");
        if(isInAsyncMethod()) {
          print(")");
        }
      }
      case "joining" -> {
        print("Array.from(").print(invocation.getTargetExpression()).print(").join(");
        printJoiningDelimiter(invocation);
        print(")");
      }
      case "counting" -> print("Array.from(").print(invocation.getTargetExpression()).print(").length");
      default -> print("Array.from(").print(invocation.getTargetExpression()).print(")");
    }
  }

  private void printMinMax(MethodInvocationElement invocation, boolean min) {
    print("((__stream, __comparator) => __stream.length === 0 ? null : __stream.reduce((__best, __item) => __comparator(__item, __best) ")
        .print(min ? "<" : ">")
        .print(" 0 ? __item : __best))(")
        .print(invocation.getTargetExpression()).print(", ").print(invocation.getArgument(0)).print(")");
  }

  private void printAnyMatch(MethodInvocationElement invocation) {
    ExtendedElement predicate = invocation.getArgument(0);
    if (isAsyncLikeArg(predicate)) {
      print("(await (async (__stream, __predicate) => { for (const __item of __stream) { if (await __predicate(__item)) return true; } return false; })(")
          .print(invocation.getTargetExpression()).print(", ");
      printAsyncArg(predicate);
      print("))");
    } else {
      print("(").print(invocation.getTargetExpression()).print(").some(").printArgList(invocation.getArguments()).print(")");
    }
  }

  private void printAllMatch(MethodInvocationElement invocation) {
    ExtendedElement predicate = invocation.getArgument(0);
    if (isAsyncLikeArg(predicate)) {
      print("(await (async (__stream, __predicate) => { for (const __item of __stream) { if (!(await __predicate(__item))) return false; } return true; })(")
          .print(invocation.getTargetExpression()).print(", ");
      printAsyncArg(predicate);
      print("))");
    } else {
      print("(").print(invocation.getTargetExpression()).print(").every(").printArgList(invocation.getArguments()).print(")");
    }
  }

  private void printNoneMatch(MethodInvocationElement invocation) {
    ExtendedElement predicate = invocation.getArgument(0);
    if (isAsyncLikeArg(predicate)) {
      print("(await (async (__stream, __predicate) => { for (const __item of __stream) { if (await __predicate(__item)) return false; } return true; })(")
          .print(invocation.getTargetExpression()).print(", ");
      printAsyncArg(predicate);
      print("))");
    } else {
      print("!(").print(invocation.getTargetExpression()).print(").some(").printArgList(invocation.getArguments()).print(")");
    }
  }

  private void printRange(MethodInvocationElement invocation, boolean closed) {
    print("((__start, __end) => Array.from({ length: Math.max(0, __end - __start")
        .print(closed ? " + 1" : "")
        .print(") }, (_, __i) => __start + __i))(")
        .printArgList(invocation.getArguments())
        .print(")");
  }

  private String getCollectorName(MethodInvocationElement invocation) {
    if (invocation.getArgumentCount() != 1 || !(invocation.getArgument(0) instanceof MethodInvocationElement)) {
      return "";
    }
    MethodInvocationElement collector = (MethodInvocationElement) invocation.getArgument(0);
    if (collector.getTargetType() != null && getFqn(collector.getTargetType()).equals("java.util.stream.Collectors")) {
      return collector.getMethodName();
    }
    return "";
  }

  private void printJoiningDelimiter(MethodInvocationElement invocation) {
    MethodInvocationElement collector = (MethodInvocationElement) invocation.getArgument(0);
    if (collector.getArgumentCount() == 0) {
      print("''");
    } else {
      print(collector.getArgument(0));
    }
  }

  private void printTargetOrThis(MethodInvocationElement invocation) {
    if (invocation.getTargetExpression() != null) {
      print(invocation.getTargetExpression());
    } else {
      print("this");
    }
  }

  private boolean isAsyncArg(ExtendedElement arg) {
    Tree tree = ExtendedElementFactory.toTree(arg);
    return tree instanceof LambdaExpressionTree && context.isAsyncLambda((LambdaExpressionTree) tree);
  }

  private boolean isAsyncLikeArg(ExtendedElement arg) {
    return isAsyncArg(arg);
  }

  protected boolean isInAsyncMethod() {
    MethodTree methodTree = getPrinter().getParent(MethodTree.class);
    if (methodTree == null) {
      return false;
    }
    Element methodElement = Util.getElement(methodTree);
    return methodElement != null && context.hasAnnotationType(methodElement, JSweetConfig.ANNOTATION_ASYNC);
  }

  private JavaStreamAdapter printAsyncArg(ExtendedElement arg) {
    if (!isAsyncArg(arg) && containsGeneratedListStream(arg)) {
      print("async ");
    }
    print(arg);
    return this;
  }

  private String getFqn(TypeMirror type) {
    return types().erasure(type).toString();
  }

  private boolean isAsyncCollection(TypeMirror type) {
    String fqn = getFqn(type);
    return fqn.contains("GemList") || fqn.contains("IUMLPList");
  }

  // TODO this should be handled in one place
  private boolean requiresArrayConversionViaRuntime(MethodInvocationElement invocation) {
    TypeMirror type = invocation.getTargetType();
    String fqn = getFqn(type);
    return isAsyncCollection(type) ||
        fqn.contains("java.util.List") ||
        fqn.contains("java.util.Set") ||
        fqn.contains("java.util.AbstractList") ||
        fqn.contains("java.util.LinkedList") ||
        fqn.contains("java.util.ArrayList") ||
        fqn.contains("java.util.concurrent.CopyOnWriteArrayList") ||
        fqn.contains("java.util.Stack") ||
        fqn.contains("java.util.Vector");
  }

  private boolean containsGeneratedListStream(ExtendedElement arg) {
    return arg != null && arg.toString().contains(".stream") && containsGeneratedListGetter(arg.toString());
  }

  private boolean containsGeneratedListGetter(String source) {
    return source != null && source.contains(".get") && source.contains("List");
  }

  private boolean isStream(TypeMirror type) {
    if (type == null) {
      return false;
    }
    String fqn = getFqn(type);
    return fqn.contains("stream.Stream") ||
        fqn.contains("stream.IntStream") ||
        fqn.contains("stream.LongStream") ||
        fqn.contains("stream.DoubleStream");
  }

  private boolean isPrimitiveStream(TypeMirror type) {
    if (type == null) {
      return false;
    }
    String fqn = getFqn(type);
    return fqn.contains("stream.IntStream") ||
        fqn.contains("stream.LongStream") ||
        fqn.contains("stream.DoubleStream");
  }

  private boolean isCollection(TypeMirror type) {
    String fqn = getFqn(type);
    return fqn.contains("java.util.List") ||
        fqn.contains("java.util.Collection") ||
        fqn.contains("java.util.ArrayList") ||
        fqn.contains("java.util.Set") ||
        fqn.contains("java.util.HashSet") ||
        fqn.contains("java.util.LinkedHashSet") ||
        fqn.contains("GemList") ||
        fqn.contains("IUMLPList");
  }
}
