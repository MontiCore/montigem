/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.MethodInvocationElement;
import standalone.com.sun.source.tree.LambdaExpressionTree;
import standalone.com.sun.source.tree.Tree;
import umlp.jsweet.extension.AbstractAdapter;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.function.Function;

public class OptionalAdapter extends AbstractAdapter {

  public OptionalAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeJavaOptionalTypeMapping());
  }

  public Function<TypeMirror, String> computeJavaOptionalTypeMapping() {
    return (TypeMirror type) -> {
      TypeMirror mappedType = mapOptionalType(type);
      if (mappedType != null) {
        String typeName = this.getMappedType(mappedType);
        if ("any".equals(typeName)) {
           // If we can't resolve the inner type, return any
           return "any";
        }
        return "(" + typeName + " | null)";
      }
      return null;
    };
  }

  public TypeMirror mapOptionalType(TypeMirror type) {
    if (isOptional(type) && type instanceof DeclaredType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
      if (typeArgs.size() > 0) {
        return typeArgs.get(0);
      }
    }
    return null;
  }

  private boolean isOptional(TypeMirror type) {
    if (type == null) {
      return false;
    }
    String fqn = types().erasure(type).toString();
    return fqn.equals("java.util.Optional");
  }

  @Override
  public TypeMirror mapType(TypeMirror type) {
    // We return null here to NOT transform the TypeMirror itself in JSweet's internal model,
    // but we use computeJavaOptionalTypeMapping to change the TS output string.
    return null;
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (isOptionalInvocation(invocation)) {
      if (substituteOptionalMethods(invocation)) {
        return true;
      }
    }

    return super.substituteMethodInvocation(invocation);
  }

  private boolean isOptionalInvocation(MethodInvocationElement invocation) {
    if (invocation.getMethod() != null && invocation.getMethod().getEnclosingElement() != null) {
      String enclosing = invocation.getMethod().getEnclosingElement().toString();
      if (enclosing.equals("java.util.Optional")) {
        return true;
      }
    }
    return isOptional(invocation.getTargetType());
  }

  private boolean substituteOptionalMethods(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    ExtendedElement target = invocation.getTargetExpression();
    List<ExtendedElement> args = invocation.getArguments();

    switch (methodName) {
      case "of":
        print("((v) => { if (v == null) throw new Error('NullPointerException'); return v; })(").printArgList(args).print(")");
        return true;
      case "ofNullable":
        printArgList(args);
        return true;
      case "empty":
        print("null");
        return true;
      case "isPresent":
        print("(").print(target).print(" != null)");
        return true;
      case "isEmpty":
        print("(").print(target).print(" == null)");
        return true;
      case "get":
        print("((v) => { if (v == null) throw new Error('NoSuchElementException'); return v; })(").print(target).print(")");
        return true;
      case "orElse":
        print("(").print(target).print(" != null ? ").print(target).print(" : ").print(args.get(0)).print(")");
        return true;
      case "orElseGet":
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("(").print(target).print(" != null ? ").print(target).print(" : (").print(args.get(0)).print(")())");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "map":
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("(").print(target).print(" != null ? (").print(args.get(0)).print(")(").print(target).print(") : null)");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "flatMap":
        // Since we map Optional<T> to T | null, flatMap is same as map here if the mapper returns T | null
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("(").print(target).print(" != null ? (").print(args.get(0)).print(")(").print(target).print(") : null)");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "filter":
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("(").print(target).print(" != null && (").print(args.get(0)).print(")(").print(target).print(") ? ").print(target).print(" : null)");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "ifPresent":
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("(v => { if (v != null) (").print(args.get(0)).print(")(v); })(").print(target).print(")");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "ifPresentOrElse":
        printMacroName(methodName);
        boolean isAsync = isAsyncArg(args.get(0)) || isAsyncArg(args.get(1));
        if (isAsync) {
          print("(await ");
        }
        print("((v, action, emptyAction) => { if (v != null) (action)(v); else (emptyAction)(); })(").print(target).print(", ").print(args.get(0)).print(", ").print(args.get(1)).print(")");
        if (isAsync) {
          print(")");
        }
        return true;
      case "or":
        printMacroName(methodName);
        if (isAsyncArg(args.get(0))) {
          print("(await ");
        }
        print("((v, get) => v == null ? (get)() : v)(").print(target).print(", ").print(args.get(0)).print(")");
        if (isAsyncArg(args.get(0))) {
          print(")");
        }
        return true;
      case "stream":
        printMacroName(methodName);
        print("AsyncStream.ofOptional(").print(target).print(")");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.STREAM, getLibraryLocation()));
        return true;
    }
    return false;
  }

  private boolean isAsyncArg(ExtendedElement arg) {
    Tree tree = ExtendedElementFactory.toTree(arg);
    return tree instanceof LambdaExpressionTree && context.isAsyncLambda((LambdaExpressionTree) tree);
  }
}
