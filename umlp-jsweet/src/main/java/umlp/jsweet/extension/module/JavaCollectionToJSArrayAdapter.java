/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.module;

import de.se_rwth.commons.logging.Log;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ForeachLoopElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.NewClassElement;
import umlp.jsweet.extension.common.AbstractAdapter;
import umlp.jsweet.extension.util.RTEPathUtil;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JavaCollectionToJSArrayAdapter extends AbstractAdapter {

  public JavaCollectionToJSArrayAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
  }

  @Override
  public void beforeTypeBody(TypeElement type) {
    super.beforeTypeBody(type);
    if(!isAsyncList) {
      getTypeMirrorMappings().add(0, computeMapTypeMapping());

      addTypeMapping(RTEPathUtil.getBackendRTECommonPath("GemList"), "Array");
    }
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

  private String getMappedTypeArgument(TypeMirror type) {
    for (Function<TypeMirror, String> mapping : getTypeMirrorMappings()) {
      String mapped = mapping.apply(type);
      if (mapped != null) {
        return mapped;
      }
    }
    return getMappedType(type);
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
        case "java.util.Collections":
          if (substituteJavaCollectionsInvocations(invocation, targetMethodName)) {
            return true;
          }
        case "com.google.common.collect.Lists":
          if (substituteCommonListsInvocations(invocation, targetMethodName)) {
            return true;
          }
        case "java.util.Arrays":
          if (substituteUtilArraysInvocations(invocation, targetMethodName)) {
            return true;
          }
        case "com.google.common.collect.Sets":
          if (substituteSetsInvocations(invocation, targetMethodName)) {
            return true;
          }
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
          else {
            return false;
          }
        case "java.util.Map.Entry":
          if (substituteMapEntryInvocations(invocation, targetMethodName)) {
            return true;
          }
      }
    }
    return super.substituteMethodInvocation(invocation);
  }

  private boolean substituteJavaCollectionsInvocations(MethodInvocationElement invocation,
      String targetMethodName) {
    switch (targetMethodName) {
      case "emptyList":
        printMacroName(targetMethodName);
        print("[]");
        return true;
      case "unmodifiableList":
      case "unmodifiableCollection":
        //        case "unmodifiableSet":
        //        case "unmodifiableSortedSet":
        printMacroName(targetMethodName);
        print("[").printArgList(invocation.getArguments()).print("]");
        return true;
      case "singletonList":
        printMacroName(targetMethodName);
        print("[").print(invocation.getArgument(0)).print("]");
        return true;
    }
    return false;
  }

  private boolean substituteCommonListsInvocations(MethodInvocationElement invocation,
      String targetMethodName) {
    switch (targetMethodName) {
      case "newArrayList":
        if (invocation.getArgumentCount() > 0) {
          print("[").printArgList(invocation.getArguments()).print("]");
        }
        else {
          print("[]");
        }
        return true;
    }
    return false;
  }

  private boolean substituteUtilArraysInvocations(MethodInvocationElement invocation,
      String targetMethodName) {
    switch (targetMethodName) {
      case "asList": {
        printMacroName(targetMethodName);
        print("[").printArgList(invocation.getArguments()).print("]");
        return true;
      }
    }
    return false;
  }

  private boolean substituteSetsInvocations(MethodInvocationElement invocation,
      String targetMethodName) {
    switch (targetMethodName) {
      case "newHashSet":
        print("[").printArgList(invocation.getArguments()).print("]");
        return true;
    }
    return false;
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
        print("[...").print(invocation.getTargetExpression()).print(".keys()").print("]");
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
        print("([...").print(invocation.getTargetExpression()).print(".entries()").print("])");
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

  protected JavaCollectionToJSArrayAdapter printTargetForParameter(ExtendedElement expression) {
    if (expression != null && expression.toString().equals("super")) {
      getPrinter().print("this");
      return this;
    }
    print(expression);
    return this;
  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    switch (newClass.getTypeAsElement().toString()) {
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
}
