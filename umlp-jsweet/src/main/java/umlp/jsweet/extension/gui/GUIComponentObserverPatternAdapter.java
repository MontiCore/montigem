/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import de.se_rwth.commons.logging.Log;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.OverloadScanner;
import org.jsweet.transpiler.extension.AnnotationManager;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.ForeachLoopElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.VariableAccessElement;
import org.jsweet.transpiler.model.support.MethodInvocationElementSupport;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.ClassTree;
import standalone.com.sun.source.tree.EnhancedForLoopTree;
import standalone.com.sun.source.tree.ExpressionTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.Tree;
import standalone.com.sun.source.tree.TypeCastTree;
import standalone.com.sun.source.tree.VariableTree;
import standalone.com.sun.tools.javac.code.Symbol;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.RTEPathUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GUIComponentObserverPatternAdapter extends PrinterAdapter {

  public static final String DOMAIN_OBSERVER_FUNCTION_NAME = "__regObsToDomainObj";

  private static final String DOMAIN_OBSERVER_DEFAULT_FUNCTION = "__regObsToDomainObjDefault";

  public static final String STATEVAR_OBSERVER_FUNCTION_NAME = "__regObsToStateVar";

  private static final String STATEVAR_OBSERVER_DEFAULT_FUNCTION = "__regObsToStateVarDefault";

  private static final String CREATE_DOMAIN_OBS = "createDomainObserverRegisterFunction";

  private static final String CREATE_STATEVAR_OBS = "createStateVarObserverRegisterFunction";

  private GUIAdapterUtils adapterUtils;

  public GUIComponentObserverPatternAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    addAnnotationManager(getSynchronizedAnnotationManager());
    addAnnotationManager(addComponentManagerListenerToSetAnnotationManager());
    this.adapterUtils = new GUIAdapterUtils(this, types(), context, util());
  }

  @Override
  public void onTranspilationStarted() {
    new GUIObserverRegistrationParamsPropagationScanner(context)
        .process(getContext().compilationUnits, getContext().trees);
    new GUISerializedBindingPropagationScanner(context)
        .process(getContext().compilationUnits, getContext().trees);
    super.onTranspilationStarted();
  }

  @Override
  public void beforeTypeBody(TypeElement type) {
    if (adapterUtils.hasDirectiveAnnotation(type)) {
      printUniqueIds(type);
      printHashes(type);
      printGetObs(type);
    }
    if (adapterUtils.isHWCComponent(type) || adapterUtils.hasDirectiveAnnotation(type) && !adapterUtils.isTOPOfComponent(type)) {
      printNgOnDestroy(type);
    }
    adapterUtils.importFromWithModule("GemListUtil", GUIAdapterUtils.UMLP_COMMON_POINTER);
    super.beforeTypeBody(type);
  }

  @Override
  public boolean substituteImplements(TypeElement type) {
    boolean res = super.substituteImplements(type);

    if ((adapterUtils.isHWCComponent(type))) {
      adapterUtils.importFromWithModule("OnDestroy", "@angular/core");
      if (util().getInterfaces(type).isEmpty()) {
        print(" implements OnDestroy");
      }
      else {
        print(", OnDestroy");
      }
    }

    return res;
  }

  private void printUniqueIds(TypeElement type) {
    String guiComponentName = type.getSimpleName().toString();
    printIndent().print("private static nextId: number = 0;").println()
        .printIndent().print("protected uniqueIdentifier:number = " + guiComponentName + ".nextId++;")
        .println().println();
  }

  private void printHashes(TypeElement type) {
    adapterUtils.importFromWithoutModule("ManyKeysMap", "many-keys-map");
    String guiComponentName = type.getSimpleName().toString();
    printIndent().print("private static nextHash: number = 0;").println();
    printIndent().print("protected hashes: ManyKeysMap<any, number> = new ManyKeysMap();").println();
    printIndent().print("protected computeHash(objs: any[]): number {").println();
    startIndent().printIndent().print("let key = [];").println();
    printIndent().print("for (const obj of objs) {").println();
    startIndent().printIndent().print("if (typeof obj.getGemId === 'function') {").println();
    startIndent().printIndent().print("key.push(obj.getGemId());").println();
    endIndent().printIndent().print("} else {").println();
    startIndent().printIndent().print("key.push(obj);").println();
    endIndent().printIndent().print("}").println();
    endIndent().printIndent().print("}").println();
    printIndent().print("if (this.hashes.has(key)) {").println();
    startIndent().printIndent().print("return this.hashes.get(key);").println();
    endIndent().printIndent().print("} else {").println();
    startIndent().printIndent().print("let hash: number = " + guiComponentName + ".nextHash++;").println();
    printIndent().print("this.hashes.set(key, hash);").println();
    printIndent().print("return hash;").println();
    endIndent().printIndent().print("}").println();
    endIndent().printIndent().print("}").println().println();
  }

  private void printGetObs(TypeElement type) {
    String guiComponentName = "\"" + type.getQualifiedName().toString() + "\"";
    if (guiComponentName.endsWith("TOP\"") && adapterUtils.hasDirectiveAnnotation(type)) {
      guiComponentName = guiComponentName.substring(0, guiComponentName.length() - 4) + "\"";
    }
    guiComponentName = guiComponentName + " + this.uniqueIdentifier";
    printIndent().print("protected " + CREATE_DOMAIN_OBS
            + "<T extends GemIdentifiable>(updateName: string, update: (() => void)):")
        .println().startIndent().printIndent().print("(obj: T) => T {")
        .println().printIndent()
        .print("return (obj: T) => {").println().startIndent().printIndent()
        .print("if (obj != undefined && obj != null) {").println().startIndent().printIndent()
        .print("return ComponentObserverManager.addDomainObjectObserver(obj, update.bind(this), updateName, "
            + guiComponentName + ");")
        .println().endIndent().printIndent().print("}")
        .print(" else {").println().startIndent().printIndent()
        .print("return null;")
        .println().endIndent().printIndent().print("}")
        .println().endIndent().printIndent().print("}")
        .println().endIndent().printIndent().print("}").println().println();

    printIndent().print("protected " + CREATE_STATEVAR_OBS
            + "<T>(updateName: string, update: (() => void)):")
        .println().startIndent().printIndent().print("(obj: T, variableName: string) => T {")
        .println().printIndent()
        .print("return (obj: T, variableName: string) => {").println().startIndent().printIndent()
        .print(
            "return ComponentObserverManager.addStateVariableObserver(obj, variableName, update.bind(this), updateName, "
                + guiComponentName + ");")
        .println().endIndent().printIndent().print("}")
        .println().endIndent().printIndent().print("}").println().println();

    adapterUtils.importFromWithModule("GemIdentifiable", GUIAdapterUtils.UMLP_COMMON_POINTER);
  }

  private void printNgOnDestroy(TypeElement typeElement) {
    String guiComponentName = "\"" + typeElement.getQualifiedName().toString() + "\"";
    if (guiComponentName.endsWith("TOP\"") && adapterUtils.hasDirectiveAnnotation(typeElement)) {
      guiComponentName = guiComponentName.substring(0, guiComponentName.length() - 4) + "\"";
    }
    guiComponentName = guiComponentName + " + this.uniqueIdentifier";
    printIndent().print("public ngOnDestroy(): void {").println().startIndent().printIndent();
    if (adapterUtils.isHWCComponent(typeElement)) {
      print("super.ngOnDestroy();").println().printIndent();
    }
    print("ComponentObserverManager.removeObservers(" + guiComponentName + ");")
        .println().endIndent().printIndent().print("}")
        .println().println();
  }

  @Override
  public boolean substituteVariable(VariableElement variable) {
    if (variable.getSimpleName().toString().equals(DOMAIN_OBSERVER_FUNCTION_NAME)) {
      println().startIndent().startIndent().printIndent().endIndent().endIndent()
          .print(DOMAIN_OBSERVER_FUNCTION_NAME)
          .print(": <T extends GemIdentifiable> (__domainObj: T) => T")
          .print(" = __domainObj => __domainObj");
      adapterUtils.importFromWithModule("GemIdentifiable", GUIAdapterUtils.UMLP_COMMON_POINTER);
      return true;
    }

    if (variable.getSimpleName().toString().equals(STATEVAR_OBSERVER_FUNCTION_NAME)) {
      println().startIndent().startIndent().printIndent().endIndent().endIndent()
          .print(STATEVAR_OBSERVER_FUNCTION_NAME)
          .print(": <T> (__stateVar: T, varName: string) => T")
          .print(" = (__stateVar, _) => __stateVar");
      return true;
    }

    return super.substituteVariable(variable);
  }

  @Override
  public boolean substituteExecutable(ExecutableElement executable) {
    TypeElement classElement = getParentElement(TypeElement.class);
    if (adapterUtils.isAngularTrackedUpdatesMethod(executable, this) &&
        !context.hasAnnotationType(executable, Override.class.getName()))
      printSynchronizerMethods(executable, false);
    if (adapterUtils.isObserverRegistrationParamsMethod(executable, this)) {
      addObserversToParameters(executable, classElement);
    }
    if (adapterUtils.isInitMethod(executable, classElement)) {
      print("protected allVarsInited: boolean = false;").println().printIndent();
    }
    return super.substituteExecutable(executable);
  }

  private void addObserversToParameters(ExecutableElement executable, TypeElement classElement) {
    MethodTree tree = trees().getTree(executable);
    JCTree.JCVariableDecl domainObserver = adapterUtils.getNewParameter(DOMAIN_OBSERVER_FUNCTION_NAME);
    JCTree.JCVariableDecl statevarObserver = adapterUtils.getNewParameter(STATEVAR_OBSERVER_FUNCTION_NAME);
    List<? extends VariableTree> parameters = tree.getParameters();
    if (parameters.size() > 0 && parameters.get(0) instanceof JCTree.JCVariableDecl
        && tree instanceof JCTree.JCMethodDecl) {
      ((JCTree.JCMethodDecl) tree).params =
          ((standalone.com.sun.tools.javac.util.List<JCTree.JCVariableDecl>) parameters)
              .append(domainObserver).append(statevarObserver);
    }
    else if (parameters.size() == 0 && tree instanceof JCTree.JCMethodDecl) {
      ((JCTree.JCMethodDecl) tree).params = standalone.com.sun.tools.javac.util.List.of(
          domainObserver, statevarObserver);
    }
    else if (parameters.size() == 0) {
      Log.error("0JS0002 unexpected error while transpiling class " + classElement.getQualifiedName().toString());
    }
    else if (parameters.size() > 0) {
      Log.error("0JS0003 unexpected error while transpiling class " + classElement.getQualifiedName().toString());
    }
  }

  private void addObserversToArguments(MethodInvocationElement invocation,
      String domainObserverFunctionName, String statevarObserverFunctionName) {
    MethodInvocationTree tree = ((MethodInvocationElementSupport) invocation).getTree();
    JCTree.JCIdent domainObserver = adapterUtils.getNewIdent(domainObserverFunctionName);
    JCTree.JCIdent statevarObserver = adapterUtils.getNewIdent(statevarObserverFunctionName);
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() >= 2 &&
        arguments.get(arguments.size() - 2) instanceof JCTree.JCIdent &&
        arguments.get(arguments.size() - 1) instanceof JCTree.JCIdent &&
        ((JCTree.JCIdent) arguments.get(arguments.size() - 2)).getName().toString().equals(
            domainObserverFunctionName) &&
        ((JCTree.JCIdent) arguments.get(arguments.size() - 1)).getName().toString().equals(
            statevarObserverFunctionName)
    ) {
      return;
    }
    if (arguments.size() > 0) {
      ((JCTree.JCMethodInvocation) tree).args =
          ((standalone.com.sun.tools.javac.util.List) arguments)
              .append(domainObserver).append(statevarObserver);
    }
    else if (arguments.size() == 0) {
      ((JCTree.JCMethodInvocation) tree).args =
          ((JCTree.JCMethodInvocation) tree).args.append(domainObserver).append(statevarObserver);
    }
  }

  private void addTriggerObserverLogicToMethodBody(ExecutableElement executable, TypeElement classElement) {
    MethodTree tree = trees().getTree(executable);

  }

  private Map<String, Set<OverloadScanner.Overload>> overloads = new LinkedHashMap<>();

  @Override
  public boolean substituteOverloadMethodBody(TypeElement parentTypeElement,
      OverloadScanner.Overload overload) {
    if (adapterUtils.isObserverRegistrationParamsAnnotated(overload.getCoreMethodElement())) {
      Set<OverloadScanner.Overload> classOverloads = this.overloads.computeIfAbsent(
          parentTypeElement.getQualifiedName().toString(),
          a -> new LinkedHashSet<>());
      classOverloads.add(overload);
    }
    return super.substituteOverloadMethodBody(parentTypeElement, overload);
  }

  private void printSynchronizerMethods(ExecutableElement executable, boolean overloadMethod) {
    if (executable.getReturnType().toString().equals("void")) {
      return;
    }

    TypeElement classElement = getParentElement(TypeElement.class);
    TypeElement superType = (TypeElement) types().asElement(classElement.getSuperclass());
    if (util().getAllMembers(superType).stream().anyMatch(e -> e.getSimpleName().equals(executable.getSimpleName()))) {
      // only for the base method, when overloads are present
      return;
    }

    // MethodName
    String methodName;
    if (!overloadMethod && context.isInvalidOverload(executable) && !util().hasTypeParameters(
        executable)) {
      methodName = adapterUtils.getOverloadMethodName(executable);
    }
    else {
      methodName = adapterUtils.getJSweetExecutableName(executable);
    }

    Tree returnType = trees().getTree(executable).getReturnType();

    boolean serialized = context.hasAnnotationType(executable, GUIAdapterUtils.SERIALIZED_ANNOTATION);

    printResultCache(executable, overloadMethod, methodName, returnType, serialized);

    printSynchronizerMethod(executable, overloadMethod, methodName, returnType);

    printUpdateMethod(executable, overloadMethod, methodName, serialized);
  }

  private void printUpdateMethod(ExecutableElement executable, boolean overloadMethod, String methodName,
      boolean serialized) {
    print("public " + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "(");
    printMethodParameters(executable, overloadMethod, GUIAdapterUtils.UPDATE_SUFFIX);
    print("): void {").println().startIndent().printIndent()
//        .print("console.log(\"" + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "\");")
        .println().printIndent();
    if (serialized) {
      String chainField = methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Chain";
      String pendingField = methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Pending";
      print("if (this." + pendingField + ") {").println().startIndent().printIndent()
          .print("return;").println().endIndent().printIndent().print("}").println().printIndent()
          .print("this." + pendingField + " = true;").println().printIndent()
          .print("this." + chainField + " = this." + chainField + ".then(() => {")
          .println().startIndent().printIndent()
          .print("this." + pendingField + " = false;").println().printIndent()
          .print("return Promise.resolve(this." + methodName + "(");
    }
    else {
      print("Promise.resolve(this." + methodName + "(");
    }
    printArguments(executable);
    if (executable.getParameters().size() > 0) {
      print(",");
    }
    println().startIndent().printIndent();

    // domain observer register function
    String updateName = executable.getSimpleName().toString() + adapterUtils.UPDATE_SUFFIX;
    print("this." + CREATE_DOMAIN_OBS + "('" + updateName + "'")
        .print(" + this.computeHash([");
    printArguments(executable);
    print("]), () => this." + updateName + "(");
    printArguments(executable);
    print(")),").println().printIndent();

    // state variable observer register function
    print("this." + CREATE_STATEVAR_OBS + "('" + updateName + "'")
        .print(" + this.computeHash([");
    printArguments(executable);
    print("]), () => this." + updateName + "(");
    printArguments(executable);
    print("))").println().endIndent().printIndent();

    String failuresField = methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Failures";
    if (serialized) {
      // The final .catch below resolves the chain, so a rejected run cannot break it.
      print("));").println().endIndent().printIndent().print("})");
    }
    else {
      print("))");
    }
    print(".then(__v => {").println().startIndent().printIndent()
        .print("this." + failuresField + " = 0;").println().printIndent()
        .print("this." + methodName + adapterUtils.RESULT_CACHE_SUFFIX);
    String arg = "__v === undefined ? null : __v";
    printResultCacheSetter(executable, arg);
    // Without a catch a single rejection (e.g. an accessor that is still null while
    // its async load runs) would leave the cache at null forever. Resetting to
    // undefined lets the next change-detection tick retry — but only a few times,
    // or a persistent failure turns into an endless retry storm.
    print(";").println().endIndent().printIndent()
        .print("}).catch(__e => {").println().startIndent().printIndent()
        .print("if (++this." + failuresField + " > 3) {").println().startIndent().printIndent()
        .print("if (this." + failuresField + " === 4) {").println().startIndent().printIndent()
        .print("console.warn('" + updateName + " keeps failing, giving up until inputs change', __e);")
        .println().endIndent().printIndent().print("}").println().printIndent()
        .print("return;").println().endIndent().printIndent().print("}").println().printIndent()
        .print("console.debug('" + updateName + " failed, will retry on next change detection', __e);")
        .println().printIndent()
        .print("this." + methodName + adapterUtils.RESULT_CACHE_SUFFIX);
    printResultCacheSetter(executable, "undefined");
    print(";").println().endIndent().printIndent().print("});")
        .println().endIndent().printIndent().print("}").println().println().printIndent();
  }

  private void printSynchronizerMethod(ExecutableElement executable, boolean overloadMethod, String methodName,
      Tree returnType) {
    print("public " + methodName + adapterUtils.SYNCHRONIZED_SUFFIX + "(");
    printMethodParameters(executable, overloadMethod, adapterUtils.SYNCHRONIZED_SUFFIX);
    print("): ");
    printType(returnType, overloadMethod)
        .print(" {").println().startIndent().printIndent()
        .print("if (!this.allVarsInited) {").println().startIndent().printIndent()
        .print("return undefined;").println().endIndent().printIndent().print("}").println().printIndent();
    printPendingArgumentGuard(executable);
    print("if (this." + methodName + adapterUtils.RESULT_CACHE_SUFFIX);
    printResultCacheAccessor(executable);
    print(" === undefined) {").println().startIndent().printIndent()
        .print("this." + methodName + adapterUtils.RESULT_CACHE_SUFFIX);
    printResultCacheSetter(executable, "null");
    print(";").println().printIndent()
        .print("this." + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "(");
    printArguments(executable);
    print(");").println().endIndent().printIndent().print("}").println().printIndent()
        .print("return this." + methodName + adapterUtils.RESULT_CACHE_SUFFIX);
    printResultCacheAccessor(executable);
    print(";").println().endIndent().printIndent().print("}").println().println().printIndent();
  }

  /**
   * Domain-object arguments coming from other async bindings are null until their
   * update promise resolves. Invoking the handwritten method (or getGemId() for the
   * cache key) with such a transient null crashes; skip this change-detection tick
   * instead — the template re-evaluates once the upstream binding resolves.
   */
  private void printPendingArgumentGuard(ExecutableElement executable) {
    List<String> gemParams = new ArrayList<>();
    for (VariableElement parameter : executable.getParameters()) {
      Element paramType = types().asElement(parameter.asType());
      if (paramType instanceof TypeElement && adapterUtils.isSubTypeOf((TypeElement) paramType,
          RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))) {
        gemParams.add(adapterUtils.getJSweetParameterName(parameter));
      }
    }
    if (gemParams.isEmpty()) {
      return;
    }
    print("if (" + String.join(" == null || ", gemParams) + " == null) {")
        .println().startIndent().printIndent()
        .print("return null;").println().endIndent().printIndent().print("}").println().printIndent();
  }

  private GUIComponentObserverPatternAdapter printArguments(ExecutableElement executable) {
    if (executable.getParameters().size() >= 1 && executable.getParameters()
        .get(0) instanceof ExtendedElement) {
      throw new IllegalStateException("TODO");
    }
    else if (executable.getParameters().size() > 0) {
      for (VariableElement parameter : executable.getParameters()) {
        String paramName = adapterUtils.getJSweetParameterName(parameter);
        print(paramName);
        print(", ");
      }
      getPrinter().removeLastChars(2);
    }
    return this;
  }

  private void printResultCacheAccessor(ExecutableElement executable) {
    if (!executable.getParameters().isEmpty()) {
      print(".get([");
      for (VariableElement parameter : executable.getParameters()) {
        print(adapterUtils.getJSweetParameterName(parameter));
        if (adapterUtils.isSubTypeOf((TypeElement) types().asElement(parameter.asType()),
            RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))) {
          print(".getGemId()");
        }
        print(", ");
      }
      getPrinter().removeLastChars(2);
      print("])");
    }
  }

  private void printResultCacheSetter(ExecutableElement executable, String value) {
    if (!executable.getParameters().isEmpty()) {
      print(".set([");
      for (VariableElement parameter : executable.getParameters()) {
        print(adapterUtils.getJSweetParameterName(parameter));
        if (adapterUtils.isSubTypeOf((TypeElement) types().asElement(parameter.asType()),
            RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))) {
          print(".getGemId()");
        }
        print(", ");
      }
      getPrinter().removeLastChars(2);
      print("], " + value + ")");
    }
    else {
      print(" = " + value);
    }
  }

  private void printResultCache(ExecutableElement executable, boolean overloadMethod, String methodName,
      Tree returnType, boolean serialized) {
    // Print result cache
    print("private ");
    print(methodName + adapterUtils.RESULT_CACHE_SUFFIX + ": ");
    if (executable.getParameters().isEmpty()) {
      printType(returnType, overloadMethod)
          .print(" = undefined;");
    }
    else {
      adapterUtils.importFromWithoutModule("ManyKeysMap", "many-keys-map");
      print("ManyKeysMap<any, ");
      printType(returnType, overloadMethod);
      print(">");
      print(" = new ManyKeysMap()");
    }
    println().printIndent();
    print("private " + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Failures: number = 0;");
    println().printIndent();
    if (serialized) {
      // deliberately conservative: one chain per method, also for parameterized (ManyKeysMap) bindings
      print("private " + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Chain: Promise<any> = Promise.resolve();");
      println().printIndent();
      print("private " + methodName + GUIAdapterUtils.UPDATE_SUFFIX + "Pending: boolean = false;");
      println().printIndent();
    }
  }

  @Override
  public boolean substituteMethodBody(TypeElement parentTypeElement, ExecutableElement method) {
    if (adapterUtils.isObserverRegistrationParamsAnnotated(method)) {
      for (VariableElement parameter : method.getParameters()) {
        if (adapterUtils.isSubTypeOf(Util.getElement(parameter.asType()),
            RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))) {
          String actualName = adapterUtils.getJSweetParameterName(parameter);
          printIndent().print(
              DOMAIN_OBSERVER_FUNCTION_NAME + "(" + actualName + ");").println();
        }
      }
    }
    return super.substituteMethodBody(parentTypeElement, method);
  }

  // Add Observers to Domain Objects and state variables
  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    // Calls of GUIUtils DOMAIN_OBSERVER_FUNCTION_NAME and STATEVAR_OBSERVER_FUNCTION_NAME are handled in GUIUtilAdapter.java

    boolean shouldPrintDomainObs = false;
    boolean shouldPrintStatevarObs = false;
    boolean shouldAddObservers = false;
    ExecutableElement surroundingMethod = Util.getElement(getPrinter().getParent(MethodTree.class));
    boolean outerMethodHasObsRegistrationParams = surroundingMethod != null
        && adapterUtils.isObserverRegistrationParamsAnnotated(surroundingMethod);
    boolean isExcludedByManualObservations = isIsExcludedByManualObservations(surroundingMethod);

    Element invocatedType = types().asElement(invocation.getType());

    if (outerMethodHasObsRegistrationParams) {
      // targets invocated Element p.getBestFriend()...
      if (invocatedType != null && invocatedType instanceof TypeElement &&
          adapterUtils.isSubTypeOf((TypeElement) invocatedType,
              RTEPathUtil.getBackendRTECommonPath("GemIdentifiable")) &&
          adapterUtils.isSubTypeOf((TypeElement) invocatedType,
              RTEPathUtil.getBackendRTECommonPath("GemBuildable")) &&
          !isExcludedByManualObservations) {
        shouldPrintDomainObs = true;
      }
    }

    Element returnType = types().asElement(invocation.getMethod().getReturnType());
    if (outerMethodHasObsRegistrationParams && adapterUtils.isObserverRegistrationParamsAnnotated(invocation.getMethod()) &&
        // This is necessary because the generated angular component (ts only) does not know about obsevers. Only for Callbacks
        (!(returnType instanceof TypeElement) ||
            !adapterUtils.isSubTypeOf((TypeElement) returnType, "java.util.function.Consumer"))) {
      shouldAddObservers = true;
    }

    ClassTree parent = getPrinter().getParent(ClassTree.class);
    Element parentElement = Util.getElement(parent);
    if (outerMethodHasObsRegistrationParams && adapterUtils.isStateVarGetter(invocation, parentElement, this)
        && !isExcludedByManualObservations) {
      shouldPrintStatevarObs = true;
    }


    if (shouldPrintDomainObs) {
      print(DOMAIN_OBSERVER_FUNCTION_NAME + "(");
    }
    if (shouldPrintStatevarObs) {
      print(STATEVAR_OBSERVER_FUNCTION_NAME + "(");
    }

    if (shouldAddObservers && !isExcludedByManualObservations) {
      addObserversToArguments(invocation, DOMAIN_OBSERVER_FUNCTION_NAME,
          STATEVAR_OBSERVER_FUNCTION_NAME);
    }

    boolean res = super.substituteMethodInvocation(invocation);

    if (shouldPrintStatevarObs) {
      print(", \"" + GUIAdapterUtils.STATEVAR_PREFIX + GUIAdapterUtils.getMethodNameWithoutGetIs(invocation) + "\")");
    }
    if (shouldPrintDomainObs) {
      print(")");
    }
    return res;
  }

  private boolean isIsExcludedByManualObservations(ExecutableElement surroundingMethod) {
    boolean isExcludedByManualObservations = surroundingMethod != null && context.hasAnnotationType(
        surroundingMethod, "umlp.jsweet.extension.annotation.ManualObservers");
    if (!isExcludedByManualObservations) {
      return isExcludedByManualObservations;
    }

    Stack<Tree> stack = getPrinter().getStack();
    for (int i = stack.size() - 1; i >= 0; i--) {
      Tree treeAtIndex = stack.get(i);
      Element element = trees().getElement(trees().getPath(getCompilationUnitTree(), treeAtIndex));
      if (element != null && element instanceof Symbol.MethodSymbol &&
          ((Symbol.MethodSymbol) element).owner.getQualifiedName().toString().equals("umlp.guiutil.GUIUtils") &&
          (((Symbol.MethodSymbol) element).getSimpleName().toString().equals("registerObserverToDomainObject") ||
              (((Symbol.MethodSymbol) element).getSimpleName().toString().equals("registerObserverToDomainObject")))
      ) {
        isExcludedByManualObservations = false;
        break;
      }
      if (element != null && element instanceof Symbol.MethodSymbol &&
          ((Symbol.MethodSymbol) element).owner.getQualifiedName().toString().equals("umlp.guiutil.GUIUtils") &&
          (((Symbol.MethodSymbol) element).getSimpleName().toString().equals("ignoreUpdatesOnDomainObject") ||
              (((Symbol.MethodSymbol) element).getSimpleName().toString().equals("ignoreUpdatesOnStateVar")))
      ) {
        isExcludedByManualObservations = true;
        break;
      }
    }
    return isExcludedByManualObservations;
  }

  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    if (variableAccess.getVariable().getSimpleName().toString().equals(DOMAIN_OBSERVER_FUNCTION_NAME)) {
      print(DOMAIN_OBSERVER_FUNCTION_NAME);
      return true;
    }
    else if (variableAccess.getVariable().getSimpleName().toString().equals(STATEVAR_OBSERVER_FUNCTION_NAME)) {
      print(STATEVAR_OBSERVER_FUNCTION_NAME);
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }

  @Override
  public boolean substituteAssignedExpression(TypeMirror type, ExtendedElement assignedExpression) {
    if (assignedExpression.toString().equals("\"" + DOMAIN_OBSERVER_FUNCTION_NAME + "\"")) {
      print(DOMAIN_OBSERVER_FUNCTION_NAME);
      return true;
    }
    else if (assignedExpression.toString().equals("\"" + STATEVAR_OBSERVER_FUNCTION_NAME + "\"")) {
      print(STATEVAR_OBSERVER_FUNCTION_NAME);
      return true;
    }
    return super.substituteAssignedExpression(type, assignedExpression);
  }

  private void printMethodParameters(ExecutableElement executable, boolean overloadMethod,
      String methodSuffix) {
    if (executable.getParameters().size() >= 1 && executable.getParameters()
        .get(0) instanceof ExtendedElement) {
      printArgList((List<? extends ExtendedElement>) executable.getParameters());
    }
    else if (executable.getParameters().size() > 0) {
      for (VariableElement parameter : executable.getParameters()) {
        Tree tree = trees().getTree(parameter);
        String paramName = adapterUtils.getJSweetParameterName(parameter);
        print(paramName).print(overloadMethod ? "?" : "").print(": ");
        getPrinter().substituteAndPrintType(((JCTree.JCVariableDecl) tree).getType()).print(", ");
      }
      getPrinter().removeLastChars(2);
    }
  }

  private GUIComponentObserverPatternAdapter printType(Tree returnType, boolean overloadMethod) {
    if (overloadMethod) {
      print("any");
      return this;
    }
    else if (returnType == null) {
      print("any");
      return this;
    }
    else {
      getPrinter().substituteAndPrintType(returnType);
      return this;
    }
  }

  /**
   * This AnnotationManager adds the @GUIAdapterUtils.NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION
   * to methods. It is called e.g. in {@link JSweetContext#hasAnnotationType}
   * @return
   */
  private AnnotationManager getSynchronizedAnnotationManager() {
    return new AnnotationManager() {
      @Override
      public Action manageAnnotation(Element element, String annotationType) {
        return GUIAdapterUtils.NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION.equals(annotationType) &&
            element.getKind().toString().equals("METHOD") &&
            isHWCMethod(element) ?
            Action.ADD :
            Action.VOID;
      }

      // Each HWC Method needs to have observer registration parameters so they register the parameters
      // When called for Angular Change Detection
      private boolean isHWCMethod(Element element) {
        return !((Symbol.MethodSymbol) element).getReturnType().getKind().equals(TypeKind.VOID) &&
            adapterUtils.isMethodOfHWCComponent(element);
      }
    };
  }

  /**
   * This Annotation manager adds a @Replace annotation to each method that is annotated with
   * {@link GUIAdapterUtils#NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION}
   * @return
   */
  private AnnotationManager addComponentManagerListenerToSetAnnotationManager() {
    return new AnnotationManager() {
      @Override
      public Action manageAnnotation(Element element, String annotationType) {
        return JSweetConfig.ANNOTATION_REPLACE.equals(annotationType)
        && hasTriggerUpdateMethodAnnotation(element) ?
            Action.ADD :
            Action.VOID;
      }

      @Override
      public <T> T getAnnotationValue(Element element,
          String annotationType, String propertyName,
          Class<T> propertyClass, T defaultValue) {
        if (JSweetConfig.ANNOTATION_REPLACE.equals(annotationType)
            && hasTriggerUpdateMethodAnnotation(element)) {
          String fullyQualifiedNameOfClass =
              getCurrentClassNameWithoutTOP(getParentElement(Symbol.TypeSymbol.class));

          String valueOfReplaceAnnotation = "{{body}}\n{{baseIndent}}ComponentObserverManager.callStateVariableObserver(\""
              + getObservedField(element) + "\", \"" + fullyQualifiedNameOfClass + "\" + this.uniqueIdentifier);\n";

          String methodToUpdate = getMethodRequiringUpdate(element);
          if(!methodToUpdate.isBlank()) {
            valueOfReplaceAnnotation+= "{{baseIndent}}this." + methodToUpdate + GUIAdapterUtils.UPDATE_SUFFIX + "();";
          }
          return propertyClass.cast(valueOfReplaceAnnotation);
        }
        else {
          return null;
        }
      }

      private String getCurrentClassNameWithoutTOP(Symbol.TypeSymbol clazz) {
        String fqn = clazz.getQualifiedName().toString();
        if (fqn.endsWith("TOP")) {
          fqn = fqn.substring(0, fqn.length() - 3);
        }
        return fqn;
      }

      private boolean hasTriggerUpdateMethodAnnotation(Element element) {
        if (getPrinter() == null) {
          return false;
        }
        return getContext().elementHasAnnotationType(element,
            GUIAdapterUtils.NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION);
      }

      private String getObservedField(Element element) {
        return getContext().getAnnotationValue(element,
            GUIAdapterUtils.NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION,
            GUIAdapterUtils.NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION_OBSERVED_FIELD, String.class, "");
      }

      private String getMethodRequiringUpdate(Element element) {
        return getContext().getAnnotationValue(element,
            GUIAdapterUtils.NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION,
            GUIAdapterUtils.NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION_METHOD, String.class, "");
      }
    };
  }

  @Override
  public boolean substituteForEachLoop(ForeachLoopElement foreachLoop, boolean targetHasLength, String indexVarName) {
    EnhancedForLoopTree loop = ExtendedElementFactory.toTree(foreachLoop);
    TypeMirror expressionType = Util.getType(loop.getExpression());

    // Must be subtype of List
    if (!types().isSubtype(expressionType, types().erasure(util().getType(List.class)))) {
      return false;
    }

    // Must be a declared generic type
    if (expressionType.getKind() != TypeKind.DECLARED) {
      return false;
    }

    List<? extends TypeMirror> typeArguments = ((DeclaredType) expressionType).getTypeArguments();
    if (typeArguments.size() != 1) {
      return false;
    }

    TypeMirror innerType = typeArguments.get(0);

    // Must be Domain type = subtype of GemIdentifiable
    if (!adapterUtils.isSubTypeOf(Util.getElement(innerType),
        RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))) {
      return false;
    }

    // Check for Domain observer param availability
    MethodTree surroundingMethodTree = getPrinter().getParent(MethodTree.class);
    if (surroundingMethodTree == null) {
      return false;
    }
    boolean hasDomainObserverParam = surroundingMethodTree.getParameters().stream()
        .anyMatch(param -> param.getName().toString().equals(DOMAIN_OBSERVER_FUNCTION_NAME));
    if (!hasDomainObserverParam) {
      return false;
    }

    // Print with additional sync logic
    printForEachLoopWithObserver(loop);
    return true;
  }

  /**
   * Prints the for each loop and adds line __regObsToDomainObj(${loopVar}); before the actual statement.
   * This activates the jsweet -> angular sync if on of the iterated elements changes
   * @param loop
   */
  protected void printForEachLoopWithObserver(EnhancedForLoopTree loop){
    String loopVarName = avoidJSKeyword(loop.getVariable().getName().toString());
    print("for (" + Java2TypeScriptTranslator.VAR_DECL_KEYWORD + " ")
        .print(loopVarName + " of ");
    boolean parentheses = loop.getExpression() instanceof TypeCastTree
        || loop.getExpression() instanceof MethodInvocationTree;
    if (parentheses) {
      print("(");
    }
    print(loop.getExpression());
    if (parentheses) {
      print(")");
    }
    getPrinter().print(") {").println().startIndent()
        .printIndent().println(DOMAIN_OBSERVER_FUNCTION_NAME + "(" + loopVarName + ");")
        .printIndent().print(loop.getStatement())
        .endIndent().println().printIndent().print("}");
  }

  private String avoidJSKeyword(String name) {
    if (JSweetConfig.JS_KEYWORDS.contains(name)) {
      name = JSweetConfig.JS_KEYWORD_PREFIX + name;
    }
    return name;
  }
}
