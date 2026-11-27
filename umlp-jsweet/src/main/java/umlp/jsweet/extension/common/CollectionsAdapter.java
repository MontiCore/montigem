/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import de.se_rwth.commons.logging.Log;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetContext;
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
import standalone.com.sun.source.tree.TypeCastTree;
import standalone.com.sun.tools.javac.code.Symbol;
import standalone.com.sun.tools.javac.code.Type;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
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

  public static final String CustomUMLPMapInterfaceName = "IUMLPMap";

  public static final String CustomAsyncMapBaseName = "AbstractAsyncMap";

  public static final String CustomAsyncMapImplementationName = "AsyncMap";
  public static final String CustomAsyncMapImplementationConstructor = "AsyncMap<any, any>().__AsyncMapConstructor";

  public static final String MapEntryHelperName = "MapEntryHelper";

  public static final String COLLECTORS = "Collectors";
  public static final String STREAM = "AsyncStream";

  UMLPAdapterUtils adapterUtils;

  public CollectionsAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    super.addTypeMappings(getStaticLibrary());
    getTypeMirrorMappings().add(0, computeUMLPGuiArrayTypeMapping());
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

    // maps work like lists: the interfaces become IUMLPMap, the classes AsyncMap
    // (insertion ordered, keys compared like ===)
    putToStaticLibrary(Map.class.getName(), CustomUMLPMapInterfaceName);
    putToStaticLibrary(AbstractMap.class.getName(), CustomUMLPMapInterfaceName);
    putToStaticLibrary(SortedMap.class.getName(), CustomUMLPMapInterfaceName);
    putToStaticLibrary(NavigableMap.class.getName(), CustomUMLPMapInterfaceName);
    putToStaticLibrary(ConcurrentMap.class.getName(), CustomUMLPMapInterfaceName);
    putToStaticLibrary(HashMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(LinkedHashMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(TreeMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(Hashtable.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(WeakHashMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(IdentityHashMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(ConcurrentHashMap.class.getName(), CustomAsyncMapImplementationName);
    putToStaticLibrary(ConcurrentSkipListMap.class.getName(), CustomAsyncMapImplementationName);
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
          break;
        case "java.util.Map":
        case "java.util.AbstractMap":
        case "java.util.SortedMap":
        case "java.util.NavigableMap":
        case "java.util.concurrent.ConcurrentMap":
        case "java.util.HashMap":
        case "java.util.LinkedHashMap":
        case "java.util.TreeMap":
        case "java.util.Hashtable":
        case "java.util.WeakHashMap":
        case "java.util.IdentityHashMap":
        case "java.util.concurrent.ConcurrentHashMap":
        case "java.util.concurrent.ConcurrentSkipListMap":
        case "umlp.backendrte.common.GemValueMap":
        case "umlp.backendrte.common.GemEntryMap":
          // Handle standard Map methods
          if (substituteMapInvocations(invocation, targetMethodName)) {
            return true;
          }
          break;
        case "java.util.Map.Entry":
          if (substituteMapEntryInvocations(invocation, targetMethodName)) {
            return true;
          }
          break;
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

  /**
   * the JDK list and set classes whose constructor calls become an awaited async RTE constructor (AsyncList, AsyncSet),
   * see {@link #substituteNewClass}
   */
  public static final Set<String> ASYNC_LIST_AND_SET_CLASSES = Set.of(
      "java.util.List",
      "java.util.AbstractList",
      "java.util.LinkedList",
      "java.util.ArrayList",
      "java.util.concurrent.CopyOnWriteArrayList",
      "java.util.Stack",
      "java.util.Vector",
      "java.util.Set",
      "java.util.HashSet",
      "java.util.LinkedHashSet");

  /**
   * the JDK map classes whose constructor calls become an awaited async RTE constructor (AsyncMap), see
   * {@link #substituteNewClass}
   */
  public static final Set<String> ASYNC_MAP_CLASSES = Set.of(
      "java.util.HashMap",
      "java.util.LinkedHashMap",
      "java.util.TreeMap",
      "java.util.Hashtable",
      "java.util.WeakHashMap",
      "java.util.IdentityHashMap",
      "java.util.concurrent.ConcurrentHashMap",
      "java.util.concurrent.ConcurrentSkipListMap");

  /**
   * @return whether constructing the class (qualified name) awaits an async RTE constructor, so the enclosing method
   * must be async (UMLPAsyncAwaitPropagationScanner)
   */
  public static boolean isAsyncCollectionClass(String qualifiedName) {
    return ASYNC_LIST_AND_SET_CLASSES.contains(qualifiedName) || ASYNC_MAP_CLASSES.contains(qualifiedName);
  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().toString();
    if (ASYNC_LIST_AND_SET_CLASSES.contains(className)) {
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
    }
    if (ASYNC_MAP_CLASSES.contains(className)) {
      // no arguments, a capacity (+ load factor), a comparator (ignored) or a map to copy
      print("(await new " + CustomAsyncMapImplementationConstructor + "(")
          .printArgList(newClass.getArguments()).print("))");
      getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncMapImplementationName, getLibraryLocation()));
      return true;
    }
    if ("umlp.guiutil.UMLPGuiArray".equals(className)) {
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
  public boolean substituteForEachLoop(ForeachLoopElement foreachLoop, boolean targetHasLength, String
      indexVarName) {
    if (!targetHasLength) {
      printForEachLoop(ExtendedElementFactory.toTree(foreachLoop), indexVarName);
      return true;
    }
    return super.substituteForEachLoop(foreachLoop, targetHasLength, indexVarName);
  }

  protected void printForEachLoop(EnhancedForLoopTree loop, String indexVarName) {
    getPrinter().print("for(" + Java2TypeScriptTranslator.VAR_DECL_KEYWORD + " " + indexVarName + "=");
    if (loop.getExpression() instanceof TypeCastTree || loop.getExpression() instanceof MethodInvocationTree) {
      print("(");
    }
    getPrinter().print(loop.getExpression());
    if (loop.getExpression() instanceof TypeCastTree || loop.getExpression() instanceof MethodInvocationTree) {
      print(")");
    }
    // hasNext is awaited if asyncjre.json makes it async (the loop's method is then
    // async as well, see UMLPAsyncAwaitPropagationScanner#visitEnhancedForLoop)
    String awaitHasNext = isAsyncIteratorMethod(getContext(), util(), "hasNext") ? "await " : "";
    print(".iterator();" + awaitHasNext + indexVarName + ".hasNext();) {").println().startIndent().printIndent();

    Symbol.VarSymbol variable = ((JCTree.JCVariableDecl) loop.getVariable()).sym;
    // The inferred type may not occur anywhere in the Java source (for example with var), so
    // explicitly register every type named by the printed variable type.
    ensureTypeModulesUsed(variable.asType());
    // the value of the loop variable is the invocation iterator.next(), printed by the printer, so the adapters can
    // substitute it (substituteMethodInvocation, e.g. GUIComponentObserverPatternAdapter)
    getPrinter().print(loop.getVariable())
        .print(" = ").print(createIteratorNextInvocation(loop, variable, indexVarName)).print(";").println();
    getPrinter().printIndent().print(loop.getStatement());
    endIndent().println().printIndent().print("}");
  }

  /**
   * Creates the invocation indexVarName.next() of the iterator loop printed for the for-each loop (javac's Lower would
   * create it when desugaring the loop, but JSweet transpiles the trees before that). It is awaited if
   * java.util.Iterator#next is async, like UMLPAsyncAwaitPropagationScanner would register it, which does not see
   * this tree.
   *
   * @param variable the loop variable, which owns the type of the elements
   */
  protected JCTree.JCMethodInvocation createIteratorNextInvocation(EnhancedForLoopTree loop, Symbol.VarSymbol variable,
                                                                   String indexVarName) {
    TypeElement iteratorElement = util().getTypeElementByName(getContext(), "java.util.Iterator");
    Symbol.MethodSymbol next = (Symbol.MethodSymbol) util().findFirstMethodDeclarationInType(iteratorElement, "next");
    // Iterator<E> with the (boxed) type of the loop variable as E, so next returns the type of the loop variable
    TypeMirror elementType = variable.asType().getKind().isPrimitive()
        ? types().boxedClass((PrimitiveType) variable.asType()).asType()
        : variable.asType();
    DeclaredType iteratorType = types().getDeclaredType(iteratorElement, elementType);
    JCTree.JCMethodInvocation invocation = ((GUIAdapterUtils) adapterUtils).getNewIteratorNextInvocation(
        variable.name.table.fromString(indexVarName), variable.owner, (Type) iteratorType, next,
        (Type) types().asMemberOf(iteratorType, next), (Type) elementType, ((JCTree) loop).pos);
    if (getContext().hasAnnotationType(next, JSweetConfig.ANNOTATION_ASYNC)) {
      getContext().addAwaitInvocation(invocation);
    }
    return invocation;
  }

  /**
   * @return whether the method symbol java.util.Iterator#methodName (hasNext or next) is annotated @Async (e.g. by
   * asyncjre.json), so the invocations in the iterator loops printed for for-each loops must be awaited
   */
  public static boolean isAsyncIteratorMethod(JSweetContext context, Util util, String methodName) {
    ExecutableElement method = util.findFirstMethodDeclarationInType(
        util.getTypeElementByName(context, "java.util.Iterator"), methodName);
    return context.hasAnnotationType(method, JSweetConfig.ANNOTATION_ASYNC);
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
    String replacement = getStaticLibrary().get(typeString);
    if (CustomUMLPMapInterfaceName.equals(replacement) || CustomAsyncMapImplementationName.equals(replacement)) {
      // AsyncMap and the GemMaps extend AbstractAsyncMap
      print("(");
      print(exprStr, expr);
      print(" != null && ");
      print(exprStr, expr);
      print(" instanceof " + CustomAsyncMapBaseName + ")");
      getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncMapBaseName, getLibraryLocation()));
      return true;
    }
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
    if (invocation.getMethod().getModifiers().contains(Modifier.STATIC)) {
      // Map.of, Map.entry, ...: not supported, see JSweetCoCoChecker
      return false;
    }
    switch (targetMethodName) {
      case "clone":
        // java.util.HashMap#clone() → a shallow copy
        printMacroName(targetMethodName);
        print("(await new " + CustomAsyncMapImplementationConstructor + "(");
        printTargetForParameter(invocation.getTargetExpression()).print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CustomAsyncMapImplementationName, getLibraryLocation()));
        return true;
      default:
        // IUMLPMap has the methods of java.util.Map, so print them as standard method calls
        getPrinter().printDefaultMethodInvocation(ExtendedElementFactory.toTree(invocation));
        return true;
    }
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
      case "setValue":
        // entries are tuples, the entries of an AsyncMap write through to their map
        printMacroName(targetMethodName);
        print("(await " + MapEntryHelperName + ".setValue(").print(invocation.getTargetExpression())
            .print(", ").print(invocation.getArgument(0)).print("))");
        getPrinter().useModule(new ModuleImportDescriptor(MapEntryHelperName, getLibraryLocation()));
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
