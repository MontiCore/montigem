/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.MethodInvocationElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.function.Function;

public class FutureAdapter extends AbstractAdapter {

  public FutureAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeJavaFutureTypeMapping());
  }

  public Function<TypeMirror, String> computeJavaFutureTypeMapping() {
    return (TypeMirror type) -> {
      if (isFuture(type)) {
        TypeMirror mappedType = mapFutureType(type);
        if (mappedType != null) {
          String typeName = this.getMappedType(mappedType);
          return "Promise<" + typeName + ">";
        }
        return "Promise<any>";
      }
      return null;
    };
  }

  public TypeMirror mapFutureType(TypeMirror type) {
    if (isFuture(type) && type instanceof DeclaredType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
      if (typeArgs.size() > 0) {
        return typeArgs.get(0);
      }
    }
    return null;
  }

  private boolean isFuture(TypeMirror type) {
    if (type == null) {
      return false;
    }
    String fqn = types().erasure(type).toString();
    return isFutureAdaptedTypes(fqn);
  }

  public static boolean isFutureAdaptedTypes(String fqn){
    return fqn.startsWith("java.util.concurrent.CompletableFuture") || fqn.startsWith("java.util.concurrent.Future") || fqn.startsWith("java.util.concurrent.CompletionStage");
  }

  @Override
  public TypeMirror mapType(TypeMirror type) {
    if (isFuture(type)) {
      return null;
    }
    return super.mapType(type);
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (isFutureInvocation(invocation)) {
      if (substituteFutureMethods(invocation)) {
        return true;
      }
    }

    return super.substituteMethodInvocation(invocation);
  }

  private boolean isFutureInvocation(MethodInvocationElement invocation) {
    if (invocation.getMethod() != null && invocation.getMethod().getEnclosingElement() != null) {
      String enclosing = invocation.getMethod().getEnclosingElement().toString();
      if (enclosing.startsWith("java.util.concurrent.CompletableFuture") || enclosing.startsWith("java.util.concurrent.Future") || enclosing.startsWith("java.util.concurrent.CompletionStage")) {
        return true;
      }
    }
    return isFuture(invocation.getTargetType());
  }

  private boolean substituteFutureMethods(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    ExtendedElement target = invocation.getTargetExpression();
    List<ExtendedElement> args = invocation.getArguments();

    switch (methodName) {
      case "supplyAsync":
        printMacroName(methodName);
        print("new Promise((resolve) => resolve((").print(args.get(0)).print(")()))");
        return true;
      case "runAsync":
        printMacroName(methodName);
        print("new Promise<void>((resolve) => { (").print(args.get(0)).print(")(); resolve(); })");
        return true;
      case "completedFuture":
      case "completedStage":
        printMacroName(methodName);
        print("Promise.resolve(").printArgList(args).print(")");
        return true;
      case "failedFuture":
      case "failedStage":
        printMacroName(methodName);
        print("Promise.reject(").printArgList(args).print(")");
        return true;
      case "thenApply":
      case "thenApplyAsync":
      case "thenAccept":
      case "thenAcceptAsync":
      case "thenCompose":
      case "thenComposeAsync":
        printMacroName(methodName);
        print("(").print(target).print(").then(").print(args.get(0)).print(")");
        return true;
      case "thenRun":
      case "thenRunAsync":
        printMacroName(methodName);
        print("(").print(target).print(").then(() => (").print(args.get(0)).print(")())");
        return true;
      case "exceptionally":
      case "exceptionallyCompose":
      case "exceptionallyComposeAsync":
        printMacroName(methodName);
        print("(").print(target).print(").catch(").print(args.get(0)).print(")");
        return true;
      case "whenComplete":
      case "whenCompleteAsync":
        printMacroName(methodName);
        print("(").print(target).print(").then((res) => { (").print(args.get(0)).print(")(res, null); return res; }, (err) => { (").print(args.get(0)).print(")(null, err); throw err; })");
        return true;
      case "handle":
      case "handleAsync":
        printMacroName(methodName);
        print("(").print(target).print(").then((res) => (").print(args.get(0)).print(")(res, null), (err) => (").print(args.get(0)).print(")(null, err))");
        return true;
      case "get":
      case "join":
        printMacroName(methodName);
        print("(await (").print(target).print("))");
        return true;
      case "allOf":
        printMacroName(methodName);
        print("Promise.all([").printArgList(args).print("]).then(() => {})");
        return true;
      case "anyOf":
        printMacroName(methodName);
        print("Promise.race([").printArgList(args).print("])");
        return true;
    }
    return false;
  }
}
