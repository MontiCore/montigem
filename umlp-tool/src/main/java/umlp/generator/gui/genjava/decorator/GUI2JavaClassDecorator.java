/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genjava.decorator;

import de.monticore.ast.ASTNode;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTLiteralExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.*;
import de.monticore.guidsl._symboltable.BuiltInTypes;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;
import de.monticore.literals.mccommonliterals._ast.ASTStringLiteral;
import de.monticore.ocl.setexpressions._ast.ASTSetCollectionItem;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsVisitor2;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symbols.oosymbols._symboltable.OOTypeSymbol;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types3.SymTypeRelations;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.Splitters;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.domain.decorator.BuilderDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.ObserverDecorator;
import umlp.generator.gui.GUI2ClassDecorator;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.genjava.prettyprinter.Expression2AccessorPrinter;
import umlp.generator.gui.genjava.prettyprinter.Expression2MutatorPrinter;
import umlp.generator.gui.genjava.prettyprinter.Expression2NullCheckPrinter;
import umlp.generator.gui.genjava.prettyprinter.Expression2SetterPrinter;
import umlp.generator.gui.genjava.prettyprinter.ExpressionPrinterConfig;
import umlp.generator.gui.genjava.prettyprinter.IterationItemsFinder;
import umlp.generator.gui.gentypescript.printer.TypeScriptTypes;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;

import java.util.*;
import java.util.function.Function;

/**
 * Turn a GUI AST into a CD AST containing one class. The class manages component state and
 * implements some logic, such as navigation to pages, creation of objects, etc.
 */
public class GUI2JavaClassDecorator extends GUI2ClassDecorator implements GUIDSLVisitor2,
    CommonExpressionsVisitor2, SetExpressionsVisitor2 {

  public static final String GUARD_ARG = "condition";
  public static final String ITERATE_ARG = "iterable";

  protected GUIDSLTraverser traverser;

  protected List<Method> addedMethods = new ArrayList<>();
  protected boolean extendedDomainObserver = false;
  boolean getScopeAdded = false;
  boolean isRTELibrary = false;
  protected Set<String> implementedAssocObservers = new LinkedHashSet<>();
  protected Set<String> subscribedManagerEvents = new LinkedHashSet<>();
  protected Map<String, NotifyMethodTemplate> notifyDeleted = new LinkedHashMap<>();
  protected Map<String, NotifyMethodTemplate> notifyAdded = new LinkedHashMap<>();
  protected Map<String, NotifyMethodTemplate> notifyAssocAdded = new LinkedHashMap<>();

  public GUI2JavaClassDecorator(final GlobalExtensionManagement glex) {
    super(glex);
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
    traverser.add4CommonExpressions(this);
    traverser.add4SetExpressions(this);

    if(glex.hasGlobalVar("UMLPGeneratorConfig")){
      isRTELibrary = UMLPGeneratorConfig.getInstance().is4TSLib() &&
          !UMLPGeneratorConfig.getInstance().is4Domain();
    }
  }

  protected void resetHooks() {
    glex.setAfterTemplate(getInitBodyHookpoint(), new ArrayList<>());
    glex.setAfterTemplate(getDestroyBodyHookpoint(), new ArrayList<>());
  }

  /**
   * Main function that transforms GUI AST into CD AST. Transformation stages:
   * 1. create empty class
   * 2. add directive annotation
   * 3. traverse GUI AST and add class members in a reverse order
   * 4. add initialization methods
   * 5. add constructor
   * Adding members includes adding initialization functions for the attributes. These functions
   * must be called in a specific order in the class initialization method, namely in reverse.
   *
   * @param gui input GUI AST
   * @param cd  output CD AST
   */
  public void transform(ASTGUICompilationUnit gui, ASTCDCompilationUnit cd) {
    this.resetHooks();
    super.transform(gui, cd);

    addRouter();

    ASTGUIComponentDeclaration declaration = gui.getGUIComponentDeclaration();
    if (declaration.isPresentTypeParameters()) {
      clazz.setTypeParameters(declaration.getTypeParameters().deepClone());
    }

    StaticObjectsCollector.getInstance().init(declaration);
    InitDependencyTree.getInstance().init(declaration);
    gui.accept(traverser);

    addInit();
    addDestroy();

    addImport("java.util.ArrayList");
    if(!isRTELibrary) {
      addImport("umlp.guiutil.GemListUtil");
      addImport("umlp.jsweet.extension.annotation.AngularTrackedUpdates");
      addImport("umlp.jsweet.extension.annotation.ObserverRegistrationParams");
      addImport("umlp.jsweet.extension.annotation.NotifyAboutFieldUpdate");
    }

    // remove
    for (NotifyMethodTemplate notifyDeleted : this.notifyDeleted.values()) {
      getCD4C().addMethod(clazz, notifyDeleted.getTemplate(), notifyDeleted.getArguments().toArray());
    }

    // add
    for (NotifyMethodTemplate notifyAdded : this.notifyAdded.values()) {
      getCD4C().addMethod(clazz, notifyAdded.getTemplate(), notifyAdded.getArguments().toArray());
    }

    for (NotifyMethodTemplate notifyAssocAdded : this.notifyAssocAdded.values()) {
      getCD4C().addMethod(clazz, notifyAssocAdded.getTemplate(), notifyAssocAdded.getArguments().toArray());
    }

    addConstructor();
  }

  /**
   * Replace CD2Java generator templates with GUI2Java templates. The templates to be replaced here
   * are independent of a specific AST node. Node-dependent templates have getters of form
   * "public String get<template_name>Template()"
   */
  protected void replaceTemplates() {
    glex.replaceTemplate("cd2java.Imports", new StringHookPoint(""));
  }

  /**
   * Each input parameter (with one exception) is transformed into
   * - attribute
   * - getter
   * - another getter for Usage in Angular Templates
   * - setter
   * - init function
   * The init function does not require an input parameter and sets a default value of a parameter.
   * Input parameters representing domain objects receive a value loaded from a server.
   * `GUIViewElement` and its collections are not transformed, since they do not represent the state
   * of a component, but rather its appearance (useless in the context of HWC/testing)
   *
   * @param node input parameter of a component
   */
  @Override
  public void endVisit(ASTGUIParam node) {
    // calc type
    if (!node.isPresentSymbol()) {
      Log.error("0x0000E Unable to find symbol for '" + node.getName()
          + "' at " + node.get_SourcePositionStart().toString()
          + " - " + node.get_SourcePositionEnd().toString()
      );
    }
    SymTypeExpression typeSTE = node.getSymbol().getType();
    // exclude GUIViewElement
    if (util.isViewElement(typeSTE)
        || util.isViewElementsCollection(typeSTE)) {
      return;
    }
    ASTMCType type = getSimpleType(createTypeAndAddImports(typeSTE));

    // calc name
    String name = "_" + node.getName();

    // add attr
    ASTCDAttribute attr = getCDAttributeFacade().createAttribute(
        CDModifier.PRIVATE.build(), type, name);
    clazz.addCDMember(attr);

    // add getter
    String getterName = "get" + StringTransformations.capitalize(node.getName());
    if (SymTypeRelations.isBoolean(typeSTE)) {
      getterName = "is" + StringTransformations.capitalize(node.getName());
    }
    ASTCDMethodSignature getter = getCD4C().addMethod(clazz, getParamGetterTemplate(), type, name, getterName);

    // add Angular getter
    Optional<ASTCDMethodSignature> angularGetter = Optional.empty();
    if(!isRTELibrary) {
      glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

      ASTCDMethodSignature method = getCD4C().addMethod(clazz, getParamAngularGetterTemplate(),type, name, getterName, false);
      glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
      angularGetter = Optional.of(method);
    }

    // add set externally check attr
    String setExternName = name + "SetExternally";
    getCD4C().addAttribute(clazz, "protected boolean " + setExternName + " = false");

    // add wait external value attr
    String externInitName = addExternInitAttr(name);

    // observer notification (will be populated by JSweet)
    String notifyObserversName = "notify" + StringTransformations.capitalize(node.getName()) + "Observers";
    ASTCDMethodSignature notifyObservers = getCD4C().addMethod(clazz, getNotifyObserversTemplate(), notifyObserversName);
    if(!isRTELibrary) {
      glex.replaceTemplate("cd2java.Annotations",notifyObservers, new StringHookPoint(
          "@NotifyAboutFieldUpdate(observedField= \""
              +attr.getName()+"\", methodRequiringUpdate=\""
              +angularGetter.get().getName()+"\")"));
    }

    // add setter
    String setterName = "set" + StringTransformations.capitalize(node.getName());
    getCD4C().addMethod(clazz, getParamSetterTemplate(), type, name, setterName,
        setExternName, notifyObserversName);

    String setterInternalName = "set" + StringTransformations.capitalize(node.getName()) + "Internal";
    getCD4C().addMethod(clazz, getParamSetterInternalTemplate(), type, name, setterInternalName);

    String initName = "init" + StringTransformations.capitalize(node.getName());

    // case 1: component Foo(App app)
    if (util.isDomainType(typeSTE) && typeSTE.isObjectType() && TagLookUp.isSingleton(typeSTE.getTypeInfo())) {
      // Use the manager corresponding (when importing from extended CDs)
      String manager = getAndImportManager(typeSTE.asObjectType());

      // add init
      Optional<InitDependency> dep = InitDependencyTree.getInstance().getDependency(node);
      if (dep.isPresent()) {
        dep.get().setInitMethod(new MethodTemplate(
            getInitParamSingletonTemplate(), type, name, manager,
            initName, setExternName, externInitName, setterInternalName
        ));
      }
    }
    // case 2: component Foo(Person p)
    else if (util.isDomainType(typeSTE) && !util.isEnumType(typeSTE)) {
      String manager = getAndImportManager(typeSTE.asObjectType());
      if (typeSTE.hasTypeInfo() && TagLookUp.isDerived(typeSTE.getTypeInfo())) {
        // add derived class init
        Optional<InitDependency> dep = InitDependencyTree.getInstance().getDependency(node);
        if (dep.isPresent()) {
          dep.get().setInitMethod(new MethodTemplate(
              getInitDerivedDomainParamTemplate(), type, name, manager,
              initName, setExternName, externInitName, setterInternalName
          ));
        }
      }
      else {
        String id = util.getRoutingId(node);
        getCD4C().addAttribute(clazz, "public Integer " + id);

        // add builder init
        Optional<InitDependency> dep = InitDependencyTree.getInstance().getDependency(node);
        if (dep.isPresent()) {
          dep.get().setInitMethod(new MethodTemplate(
              getInitDomainParamTemplate(), type, name, manager, id,
              initName, setExternName, externInitName, util.isAbstractDomainType(typeSTE), setterInternalName
          ));
        }
      }
    }
    // case 3: component Foo(List<Person> p)
    else if (util.getDomainListInnerType(typeSTE).isPresent()
        && !util.isEnumType(util.getDomainListInnerType(typeSTE).get())) {
      ASTMCType innerType = createTypeAndAddImports(util.getDomainListInnerType(typeSTE).get());
      ASTMCType simpleInnerType = getSimpleType(innerType);
      String manager = getAndImportManager(util.getDomainListInnerType(typeSTE).get().asObjectType());

      // add list item observer
      if (gui.getGUIComponentDeclaration().isPage()) {
        addDomainObserverMethods();
        if (!subscribedManagerEvents.contains(simpleInnerType.printType())) {
          subscribedManagerEvents.add(simpleInnerType.printType());
          addImport(PathUtil.getBackendRTECommandPath("CommandManager"));
          /*
            CommandManager.executeCommand(new Person_subscribeManagerEvents());
           */
          if (((OOTypeSymbol) util.getDomainListInnerType(typeSTE).get().getTypeInfo()).isIsInterface()) {
            Log.warn("0xTODO: " + util.getDomainListInnerType(typeSTE).get().printFullName() + " is an interface, no _subscribeManagerEvents is generated. This MIGHT be a bug.");
            // TODO: We do not have the SubscribeManagerEvent generated for interfaces - do we need them?
            // Right now, we just fix the compilation errors
          }
          else {
            importCommand(innerType);
            glex.addAfterTemplate(getInitBodyHookpoint(), new TemplateHookPoint(getSubscribeManagerEventsTemplate(), simpleInnerType));
          }
        }

        // TODO: simple name clash, e.g., foo.Person and bar.Person
        // add notifyDeleted method
        if (!notifyDeleted.containsKey(simpleInnerType.printType())) {
          notifyDeleted.put(simpleInnerType.printType(),
              new NotifyMethodTemplate(getNotifyDeletedTemplate(), simpleInnerType));
        }
        notifyDeleted.get(simpleInnerType.printType()).addCalledMethod(notifyObservers.getName());

        // add notifyAdded method
        if (!notifyAdded.containsKey(simpleInnerType.printType())) {
          notifyAdded.put(simpleInnerType.printType(),
              new NotifyMethodTemplate(getNotifyAddedTemplate(), simpleInnerType));
        }
        notifyAdded.get(simpleInnerType.printType()).addCalledMethod(notifyObservers.getName());
      }

      // add attr init
      Optional<InitDependency> dep = InitDependencyTree.getInstance()
          .getDependency(node);
      if (dep.isPresent()) {
        // add init
        dep.get().setInitMethod(new MethodTemplate(
            getInitDomainListTemplate(), getSimpleType(innerType), name, manager,
            initName, setExternName, externInitName, setterInternalName
        ));
      }
    }
    // case 4: component Foo(?String s, MyDomainEnum e)
    else {
      Optional<String> value = Optional.empty();
      if (node.isPresentDefault()) {
        value = Optional.of(util.print(node.getDefault()));
      }
      // add attr init
      Optional<InitDependency> dep = InitDependencyTree.getInstance().getDependency(node);
      if (dep.isPresent()) {
        dep.get().setInitMethod(new MethodTemplate(
            getInitDefaultParamTemplate(), name, value,
            initName, setExternName, externInitName, getterName, setterName
        ));
      }
    }
  }

  protected String addExternInitAttr(String attrName) {
    return "";
  }

  /**
   * An argument is transformed into a getter.
   * If the argument holds a reference, i.e., argument corresponds to a parameter like `?int value`,
   * it is also transformed into a setter (used for input fields or forms).
   * If the argument is an `Event` it is transformed into a call instead of a getter, e.g.,
   * `foo@GemButton(leftClick = action())` is transformed into `fooLeftClick` method that calls
   * whatever is on the right side of the assignment.
   * `GUIViewElement` and its collections are not transformed, since they do not represent the state
   * of a component, but rather its appearance (useless in the context of HWC/testing)
   *
   * @param node argument passed to a component
   */
  @Override
  public void endVisit(ASTGUIComponent node) {
    Map<ASTGUINamedArg, SymTypeExpression> argumentTypeMap = node.getArgumentTypes();
    for (ASTGUINamedArg arg : node.getGUINamedArgs().getGUINamedArgList()) {
      // calc type
      if (!arg.isPresentNameSymbol()) {
        Log.error("0x0000E Unable to find symbol for '" + arg.getName()
            + "' at " + arg.get_SourcePositionStart().toString()
            + " - " + arg.get_SourcePositionEnd().toString()
        );
      }

      SymTypeExpression typeSTE = argumentTypeMap.get(arg);
      // exclude GUIViewElement
      if (util.isViewElement(typeSTE)
          || util.isViewElementsCollection(typeSTE)) {
        continue;
      }
      ASTMCType type = getSimpleType(createTypeAndAddImports(typeSTE));

      // calc name
      String name = calcBaseArgName(node, arg);

      // add getter
      IterationItemsFinder finder = new IterationItemsFinder();
      Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
      Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

      Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
          new IndentPrinter(), false, iterateItems, nonClassMembers
      );
      Expression2SetterPrinter setterPrinter = new Expression2MutatorPrinter(
          new IndentPrinter(), false, iterateItems, nonClassMembers
      );
      Expression2NullCheckPrinter nullCheckPrinter = new Expression2NullCheckPrinter(getterPrinter);

      String nullCheck = nullCheckPrinter.prettyprint(arg.getExpression());

      ASTExpression argExpr = arg.getExpression();
      if (GUIDSLMill.typeDispatcher().isCommonExpressionsASTCallExpression(argExpr)) {
        ASTCallExpression methodCall = GUIDSLMill.typeDispatcher().asCommonExpressionsASTCallExpression(argExpr);
        if (methodCall.getArguments().sizeExpressions() > 0) {
          boolean hasViewElementArg = false;
          List<ASTExpression> expressions = methodCall.getArguments().getExpressionList();
          for(ASTExpression expr : expressions) {
            if (GUIDSLMill.typeDispatcher().isGUIDSLASTGUIComponent(expr)) {
              hasViewElementArg = true;
              break;
            }
          }
          if (hasViewElementArg) {
            continue;
          }
        }
      }
      String getterValue = getterPrinter.prettyprint(argExpr);

      List<ASTCDParameter> params = getIterationItemParameters(arg);

      if (typeSTE.printFullName().startsWith(BuiltInTypes.EVENT.getQName())) {
        ASTMCType innerType;
        if (typeSTE.isGenericType() && typeSTE.asGenericType().sizeArguments() == 1) {
          innerType = getSimpleType(createTypeAndAddImports(typeSTE.asGenericType().getArgument(0)));
        }
        else {
          if (typeSTE.isObjectType()) {
            Log.error("0x0001F Found Event -typed argument without type parameters"
                + " (underspecified Event type)."
                + " Event type expects exactly a single type parameter, e.g. Event<Integer>."
                + " At " + arg.get_SourcePositionStart().toString()
                + " - " + arg.get_SourcePositionEnd().toString()
            );
          }
          else if (typeSTE.isGenericType() && typeSTE.asGenericType().sizeArguments() != 1) {
            Log.error("0x0001F Found Event -typed argument with multiple type parameters."
                + " Event type expects exactly a single type parameter, e.g. Event<Integer>."
                + " At " + arg.get_SourcePositionStart().toString()
                + " - " + arg.get_SourcePositionEnd().toString()
            );
          }
          else {
            Log.error("0x00020 Unknown error while processing an Event -typed argument "
                + " at " + arg.get_SourcePositionStart().toString()
                + " - " + arg.get_SourcePositionEnd().toString()
            );
          }
          return;
        }

        // add event caller
        getCD4C().addMethod(
            clazz, getEventCallerTemplate(),
            innerType, name, getterValue, params
        );
      }
      else {
        String returnValue = Expression2SetterPrinter.SETTER_PARAM;
        String conv;
        // example: `String val = date2String(this.getPerson().getBirthday())`
        conv = getConvertedValueAssignment(
            printType(getSimpleType(createTypeAndAddImports(typeSTE))),
            returnValue,
            getConversion(typeSTE, util.getType(arg.getExpression()))
                .apply(getterValue)
        );

        ASTCDMethodSignature getter = getCD4C().addMethod(
            clazz, getArgGetterWithConversionTemplate(),
            type, name, nullCheck, returnValue, conv, params
        );

        if(!isRTELibrary) {
          glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

          ASTCDMethodSignature method = getCD4C().addMethod(
              clazz, getArgGetterWithConversionAngularTemplate(),
              type, name, nullCheck, returnValue, conv, params, false
          );
          glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
        }
      }

      if (arg.getNameSymbol().isIsReference()) {
        // add setter
        String target = "conv_" + Expression2SetterPrinter.SETTER_PARAM;
        String value = Expression2SetterPrinter.SETTER_PARAM;
        Optional<String> setterValue = Optional.ofNullable(setterPrinter.prettyprint(arg.getExpression()));
        if (this.getClass().getSimpleName().equals("GUI2JavaClassDecorator") && (setterValue.isEmpty() || "null".equals(setterValue.get()))) {
          setterValue = Optional.of(value + " = " + value);
        }
        SymTypeExpression left = util.getType(arg.getExpression());
        String convType;
        if (left.isNullType()) {
          convType = "Object";
        }
        else {
          convType = printType(getSimpleType(createTypeAndAddImports(left)));
        }
        // example: `Date val_$ = string2Date(val)`
        ASTCDMethodSignature convert = getCD4C().addMethod(
            clazz, getArgConversionTemplate(),
            convType, name, type, target, getConversion(left, typeSTE).apply(target)
        );

        getCD4C().addMethod(
            clazz, getArgSetterWithConversionTemplate(),
            type, name, nullCheck, setterValue.orElse(null),
            convType, value, convert.getName(), params
        );
      }
    }

    addAutomaticDomainValidityGetter(node);
  }

  protected void addAutomaticDomainValidityGetter(ASTGUIComponent node) {
    Optional<GUIGeneratorUtil.AutomaticDomainValidityBinding> bindingOpt = util.getAutomaticDomainValidityBinding(node);
    if (bindingOpt.isEmpty()) {
      return;
    }

    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
    Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);
    Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
        new IndentPrinter(), false, iterateItems, nonClassMembers
    );

    GUIGeneratorUtil.AutomaticDomainValidityBinding binding = bindingOpt.get();
    ASTFieldAccessExpression fieldAccess = binding.getEntryExpression();
    String name = node.getId().getName() + StringTransformations.capitalize(binding.getTargetArgName());
    String attrName = fieldAccess.getName();
    String domainObjGetter = getterPrinter.prettyprint(fieldAccess.getExpression());
    String targetType = printType(getSimpleType(createTypeAndAddImports(binding.getTargetType())));
    List<ASTCDParameter> params = getIterationItemParameters(node);
    List<AdditionalValidationSource> additionalValidationSources =
        getAdditionalValidationSources(node, fieldAccess, getterPrinter);

    ASTCDMethodSignature getter = getCD4C().addMethod(clazz, "umlp.gui2java.GetValidityFromDomain",
        name, domainObjGetter, attrName, targetType, params, additionalValidationSources);

    if(!isRTELibrary) {
      glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

      ASTCDMethodSignature method = getCD4C().addMethod(clazz, "umlp.gui2java.GetValidityFromDomainForAngular",
          name, domainObjGetter, attrName, targetType, params, additionalValidationSources);

      glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
    }

    addImport("mc.fenix.input.geminputbasetypes.ValidationError");
    addImport("java.util.Optional");
  }

  // TODO: use type check instead
  protected List<AdditionalValidationSource> getAdditionalValidationSources(
      ASTGUIComponent node,
      ASTFieldAccessExpression fieldAccess,
      Expression2AccessorPrinter getterPrinter
  ) {
    if (!(fieldAccess.getExpression() instanceof ASTNameExpression)) {
      return Collections.emptyList();
    }

    ASTNameExpression item = (ASTNameExpression) fieldAccess.getExpression();
    Optional<VariableSymbol> itemSymbol =
        util.findIterationItemSymbols(item.getName(), node.getEnclosingScope());
    if (itemSymbol.isEmpty()
        || !(itemSymbol.get().getEnclosingScope().getAstNode() instanceof ASTGUIIterate)) {
      return Collections.emptyList();
    }

    ASTGUIIterate iterate =
        (ASTGUIIterate) itemSymbol.get().getEnclosingScope().getAstNode();
    if (!(iterate.getIterateControl() instanceof ASTGUIShortIterateControl)) {
      return Collections.emptyList();
    }

    ASTExpression iterable =
        ((ASTGUIShortIterateControl) iterate.getIterateControl()).getIterable();
    if (!(iterable instanceof ASTFieldAccessExpression)) {
      return Collections.emptyList();
    }

    ASTFieldAccessExpression associationAccess = (ASTFieldAccessExpression) iterable;
    return List.of(new AdditionalValidationSource(
        getterPrinter.prettyprint(associationAccess.getExpression()),
        associationAccess.getName()
    ));
  }

  public static class AdditionalValidationSource {
    protected final String domainObjGetter;
    protected final String attrName;

    public AdditionalValidationSource(String domainObjGetter, String attrName) {
      this.domainObjGetter = domainObjGetter;
      this.attrName = attrName;
    }

    public String getDomainObjGetter() {
      return domainObjGetter;
    }

    public String getAttrName() {
      return attrName;
    }
  }

  /**
   * A guard condition is transformed into a getter.
   *
   * @param node guard AST node
   */
  @Override
  public void endVisit(ASTGUIGuard node) {
    // calc type
    ASTMCType type;
    // if the condition is a custom method
    if (StaticObjectsCollector.getInstance().getNode2Method().containsKey(node.getCondition())) {
      Method m = StaticObjectsCollector.getInstance().getNode2Method().get(node.getCondition());
      type = getSimpleType(createTypeAndAddImports(m.getReturnType()));
    }
    else {
      SymTypeExpression typeSTE = util.getType(node.getCondition());
      type = getSimpleType(createTypeAndAddImports(typeSTE));
    }

    // calc name
    String name = node.getId().getName() + StringTransformations.capitalize(GUARD_ARG);

    // add getter
    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
    Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

    Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
        new IndentPrinter(), false, iterateItems, nonClassMembers
    );
    Expression2NullCheckPrinter nullCheckPrinter = new Expression2NullCheckPrinter(getterPrinter);

    String nullCheck = nullCheckPrinter.prettyprint(node.getCondition());
    String getterValue = getterPrinter.prettyprint(node.getCondition());

    List<ASTCDParameter> params = getIterationItemParameters(node);

    // add getter
    ASTCDMethodSignature getter = getCD4C().addMethod(
        clazz, getArgGetterTemplate(),
        type, name, nullCheck, getterValue, params
    );

    if(!isRTELibrary) {
      glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

      // add Angular getter
      ASTCDMethodSignature method = getCD4C().addMethod(
          clazz, getArgAngularGetterTemplate(),
          type, name, nullCheck, getterValue, params, false // Assume always boolean
      );
      glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
    }
  }

  /**
   * An iterable of the iterate statement is transformed into a getter.
   * Currently only iterate statement of form `@iterate(Person p : persons)` is supported
   *
   * @param node iterate AST node
   */
  @Override
  public void endVisit(ASTGUIIterate node) {
    if (!(node.getIterateControl() instanceof ASTGUIShortIterateControl)) {
      Log.error("0x00004 iterate construct " + node.getId().getName()
          + " was not processed: only short version, such as '(Type item : list)',"
          + " is supported so far");
    }
    ASTGUIShortIterateControl control = (ASTGUIShortIterateControl) node.getIterateControl();
    // calc type
    ASTMCType type;
    SymTypeExpression typeSTE;
    // if the iterable is a custom method
    if (StaticObjectsCollector.getInstance().getNode2Method().containsKey(control.getIterable())) {
      Method m = StaticObjectsCollector.getInstance().getNode2Method().get(control.getIterable());
      if (util.isViewElement(m.getReturnType())
          || util.isViewElementsCollection(m.getReturnType())) {
        Log.warn("Detected method " + m.getName() + " returning GUIViewElement object(s)."
            + " Such methods are uncommon to define, consider adjusting the model.");
      }
      typeSTE = m.getReturnType();
      type = getSimpleType(createTypeAndAddImports(typeSTE));
    }
    else {
      typeSTE = util.getType(control.getIterable());
      // exclude GUIViewElement
      if (util.isViewElement(typeSTE)
          || util.isViewElementsCollection(typeSTE)) {
        return;
      }
      type = getSimpleType(createTypeAndAddImports(typeSTE));
    }

    // calc name
    String name = node.getId().getName() + StringTransformations.capitalize(ITERATE_ARG);

    // add getter
    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
    Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

    Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
        new IndentPrinter(), false, iterateItems, nonClassMembers
    );
    Expression2NullCheckPrinter nullCheckPrinter = new Expression2NullCheckPrinter(getterPrinter);

    String nullCheck = nullCheckPrinter.prettyprint(control.getIterable());
    String getterValue = getterPrinter.prettyprint(control.getIterable());

    List<ASTCDParameter> params = getIterationItemParameters(node);

    ASTCDMethodSignature getter = getCD4C().addMethod(
        clazz, getArgGetterTemplate(),
        type, name, nullCheck, getterValue, params
    );

    if(!isRTELibrary) {
      glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

      ASTMCType typeToPrint = getSimpleType(createAngularType(typeSTE));
      ASTCDMethodSignature method = getCD4C().addMethod(
          clazz, getArgAngularGetterTemplate(),
          typeToPrint, name, nullCheck, getterValue, params, typeToPrint.printType().startsWith("def.js.Array")
      );
      glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
    }

    // add init
    if (!control.getItem().isPresentSymbol()) {
      Log.error("0x0000E Unable to find symbol for '" + control.getItem().getName()
          + "' at " + control.getItem().get_SourcePositionStart().toString()
          + " - " + control.getItem().get_SourcePositionEnd().toString()
      );
    }
    SymTypeExpression itemTypeSTE = control.getItem().getSymbol().getType();
    ASTMCType itemType = getSimpleType(createTypeAndAddImports(itemTypeSTE));
    String itemName = control.getItem().getSymbol().getName();

    Optional<InitDependency> dep = InitDependencyTree.getInstance().getDependency(control);
    if (dep.isPresent()) {
      // add init
      dep.get().setInitMethod(new MethodTemplate(
          getIterateInitTemplate(), itemType, name, itemName, getterValue, params
      ));
    }
  }

  /**
   * A GUIList is transformed into an attribute, getter, and an init method.
   * A list of form `[1, 2, 3]` in a GUI model represents an immutable list that is initialized once
   * and is a static part of the component's state. For example,
   * `@PersonList(persons = [p1, p2, p3])` produces a `getPersons()` method that always returns the
   * same `List` object, which is stored as a class attribute.
   *
   * @param node GUIList AST node
   */
  @Override
  public void endVisit(ASTSetEnumeration node) {
    // a GUIList requires preprocessing and extra information for code generation
    Map<ASTNode, Attribute> node2Attr = StaticObjectsCollector
        .getInstance().getNode2Attr();
    if (node2Attr.containsKey(node)) {
      // add attr
      Attribute attrWrapper = node2Attr.get(node);
      ASTCDAttribute attr = attrWrapper.getCdAttr();
      for (String qName : attrWrapper.getImportList()) {
        addImport(qName);
      }
      clazz.addCDMember(attr);

      // calc iterate items as additional args
      List<ASTCDParameter> params = attrWrapper.getParams();

      ASTMCType type = attrWrapper.getBaseType();
      String name = attr.getName();

      if (params.isEmpty()) {
        // add getter
        ASTCDMethodSignature getter = getCD4C().addMethod(clazz, getGetterTemplate(), type, name);

        if(!isRTELibrary) {
          glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

          ASTCDMethodSignature method = getCD4C().addMethod(clazz, getAngularGetterTemplate(), type, name, false);
          glex.replaceTemplate("cd2java.Annotations", method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
        }

        // add attr init
        Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
            new IndentPrinter(), false, new ArrayList<>(), new ArrayList<>()
        );
        List<String> listItems = new ArrayList<>();
        for (ASTSetCollectionItem val : node.getSetCollectionItemList()) {
          listItems.add(getterPrinter.prettyprint(val));
        }

        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          if (TypeScriptTypes.ARRAY_LIST.getJavaQName().isPresent()) {
            addImport(TypeScriptTypes.ARRAY_LIST.getJavaQName().get());
          }
          // add init
          dep.get().setInitMethod(new MethodTemplate(
              getListInitTemplate(), type, name, listItems
          ));
        }
      }
      else {
        // add getter
        ASTCDMethodSignature getter = getCD4C().addMethod(clazz, getStaticObjectGetterTemplate(),
            type, name, params);
        if(!isRTELibrary) {
          glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));
        }

        // add attr init
        IterationItemsFinder finder = new IterationItemsFinder();
        Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
        Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

        Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
            new IndentPrinter(), false, iterateItems, nonClassMembers
        );
        List<String> listItems = new ArrayList<>();
        for (ASTSetCollectionItem val : node.getSetCollectionItemList()) {
          listItems.add(getterPrinter.prettyprint(val));
        }

        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          if (TypeScriptTypes.HASH_MAP.getJavaQName().isPresent()) {
            addImport(TypeScriptTypes.HASH_MAP.getJavaQName().get());
          }
          // add init
          dep.get().setInitMethod(new MethodTemplate(
              getStaticListInitTemplate(), type, name, listItems, params
          ));
        }
      }
    }
  }

  /**
   * A Call expression is transformed into either an attribute, getter, and an init method for a
   * static object, or into a function.
   * Calls of form `Person()` represent a constructor call that creates an object. The object is a
   * static part of the component's state. For example,
   * `@PersonForm(person = Person())` produces a `getPerson()` method that always returns the same
   * `Person` object, which is stored as a class attribute.
   * Calls of form `doSomething()`, `alert("my message")`, `findPerson(persons)` represent a
   * function that has to be defined by hand. The result of generation are abstract functions
   * `doSomething()`, `alert(String val)`, `findPerson(List<Person> p)`, whose return type is
   * derived from the left side of the assignment.
   * Complex calls, such as `doSomething()()`, `alert(alert())`, `persons.get(calcIndex())` are
   * forbidden. Such calls must be simplified to an accepted format.
   *
   * @param node GUIList AST node
   */
  @Override
  public void endVisit(ASTCallExpression node) {
    Map<ASTNode, Attribute> node2Attr = StaticObjectsCollector.getInstance().getNode2Attr();
    Map<ASTNode, Method> node2Method = StaticObjectsCollector.getInstance().getNode2Method();
    // a constructor call, e.g., @PersonForm(person = Person())
    if (node2Attr.containsKey(node)) {
      if (!node.getArguments().isEmptyExpressions()) {
        SymTypeExpression ste = util.getType(node);
        if (!(ste.isObjectType() || ste.isGenericType())) {
          Log.error("0x00016 Constructor with arguments '" + util.print(node)
              + "' at " + node.getArguments().get_SourcePositionStart().toString()
              + " - " + node.getArguments().get_SourcePositionEnd().toString()
              + ". Only constructors explicitly declared with matching parameters are supported."
              + " Please replace the call via a custom or a predefined function call,"
              + " e.g. get" + util.print(node)
          );
        }
      }

      // add attr
      Attribute attrWrapper = node2Attr.get(node);
      for (String qName : attrWrapper.getImportList()) {
        addImport(qName);
      }
      ASTCDAttribute attr = attrWrapper.getCdAttr();
      clazz.addCDMember(attr);

      // calc iterate items as additional args
      List<ASTCDParameter> params = attrWrapper.getParams();

      ASTMCType type = attrWrapper.getBaseType();
      SymTypeExpression ste = attrWrapper.getSourceType();
      String name = attr.getName();

      IterationItemsFinder finder = new IterationItemsFinder();
      Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
      Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

      Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
          new IndentPrinter(), false, iterateItems, nonClassMembers
      );
      List<String> constructorArgs = new ArrayList<>();
      for (ASTExpression val : node.getArguments().getExpressionList()) {
        constructorArgs.add(getterPrinter.prettyprint(val));
      }

      if (params.isEmpty()) {
        // add getter
        ASTCDMethodSignature getter = getCD4C().addMethod(clazz, getGetterTemplate(), type, name);

        if(!isRTELibrary) {
          glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));

          ASTCDMethodSignature method = getCD4C().addMethod(clazz, getAngularGetterTemplate(), type, name, false);
          glex.replaceTemplate("cd2java.Annotations",method, new StringHookPoint("@AngularTrackedUpdates @ObserverRegistrationParams"));
        }

        // add builder init
        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          if (constructorArgs.isEmpty()) {
            // a call that maps to a domain builder, e.g., @PersonForm(person = Person())
            if (util.isDomainType(ste)) {
              String manager = getAndImportManager();

              dep.get().setInitMethod(new MethodTemplate(
                  getBuilderInitTemplate(), type, name, manager
              ));
            }
            // a call that maps to a non-domain builder, e.g., @GemDropdown(options = [Option()])
            else {
              addImport(BuilderDecorator.getClassName(ste.printFullName()));
              dep.get().setInitMethod(new MethodTemplate(
                  getBuilderViaConstructorInitTemplate(), type, name
              ));
            }
          }
          // a call that maps to a constructor, e.g., @GemDropdown(options = [Option("foo")])
          else {
            dep.get().setInitMethod(new MethodTemplate(
                getConstructorInitTemplate(), type, name, constructorArgs
            ));
          }
        }
      }
      else {
        // add getter
        ASTCDMethodSignature getter = getCD4C().addMethod(clazz, getStaticObjectGetterTemplate(),
            type, name, params);
        if(!isRTELibrary) {
          glex.replaceTemplate("cd2java.Annotations", getter, new StringHookPoint("@ObserverRegistrationParams"));
        }

        // add builder init
        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          if (TypeScriptTypes.HASH_MAP.getJavaQName().isPresent()) {
            addImport(TypeScriptTypes.HASH_MAP.getJavaQName().get());
          }
          if (constructorArgs.isEmpty()) {
            // a call that maps to a domain builder, e.g., @PersonForm(person = Person())
            if (util.isDomainType(ste)) {
              String manager = getAndImportManager();

              dep.get().setInitMethod(new MethodTemplate(
                  getStaticBuilderInitTemplate(), type, name, manager, params
              ));
            }
            // a call that maps to a non-domain builder, e.g., @GemDropdown(options = [Option()])
            else {
              addImport(BuilderDecorator.getClassName(ste.printFullName()));
              dep.get().setInitMethod(new MethodTemplate(
                  getStaticBuilderViaConstructorInitTemplate(), type, name
              ));
            }
          }
          else {
            dep.get().setInitMethod(new MethodTemplate(
                getStaticConstructorInitTemplate(), type, name, constructorArgs, params
            ));
          }
        }
      }
    }
    // a custom function or a predefined one, e.g.,
    // @PersonForm(person = makePerson()) or @PersonForm(buttonClick = create(person))
    else if (node2Method.containsKey(node)) {
      Method method = node2Method.get(node);
      String name = method.getName();
      if (!addedMethods.contains(method)) {
        addedMethods.add(method);
        if (util.isCommandCall(this.gui, node)) {
          getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("CommandResult"));
          getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("CommandManager"));
          SymTypeExpression symTypeExpression = method.getReturnType();
          String commandResultTypeParameter = symTypeExpression.print();
          if (GenService.isList(symTypeExpression)) {
            SymTypeExpression innerType = symTypeExpression.asGenericType().getArgument(0);
            commandResultTypeParameter = innerType.print();
          }
          getSimpleType(createTypeAndAddImports(method.getReturnType()));
          String cmdQPath = Joiners.DOT.join(util.getMainDomainModelSimpleName().get().toLowerCase(),
              "commands", StringTransformations.capitalize(method.getName()));
          getCD4C().addImport(clazz, cmdQPath);

          LinkedHashMap<ASTCDParameter, SymTypeExpression> argumentParams = new LinkedHashMap<>();
          for (int i = 0; i < method.getParamTypes().size(); ++i) {
            SymTypeExpression argSTE = method.getParamTypes().get(i);
            argumentParams.put(CDParameterFacade.getInstance().createParameter(
                    getSimpleType(createTypeAndAddImports(argSTE)), Expression2SetterPrinter.SETTER_PARAM + "_$" + i),
                argSTE);
          }
          getCD4C().addMethod(clazz, getCommandExecutionTemplate(), method.getReturnType(), commandResultTypeParameter, name, argumentParams);
        }
        // predefined function: @PersonForm(buttonClick = create(person))
        else if (PredefinedFunction.getMember(name).isPresent()) {
          // TODO AGe: check type correctness
          PredefinedFunction fun = PredefinedFunction.getMember(name).get();
          String manager = getAndImportManager();
          switch (fun) {
            case CREATE:
              if (method.getParamTypes().size() != 1
                  || !util.isDomainType(method.getParamTypes().get(0))) {
                Log.error("0xD001A Function 'create' expects a single domain type argument",
                    node.get_SourcePositionStart(),
                    node.get_SourcePositionEnd());
              }
              if (TypeScriptTypes.CONSUMER.getJavaQName().isPresent()) {
                addImport(TypeScriptTypes.CONSUMER.getJavaQName().get());
              }
              getCD4C().addMethod(clazz, getCreateTemplate(),
                  method.getAlias(), getSimpleType(createTypeAndAddImports(method.getParamTypes().get(0))));
              break;
            case DELETE:
              if (method.getParamTypes().size() != 1
                  || !util.isDomainType(method.getParamTypes().get(0))) {
                Log.error("0xD001B Function 'delete' expects a single domain type argument",
                    node.get_SourcePositionStart(),
                    node.get_SourcePositionEnd());
              }
              if (TypeScriptTypes.CONSUMER.getJavaQName().isPresent()) {
                addImport(TypeScriptTypes.CONSUMER.getJavaQName().get());
              }
              getCD4C().addMethod(clazz, getDeleteTemplate(),
                  method.getAlias(), getSimpleType(createTypeAndAddImports(method.getParamTypes().get(0))), manager);
              break;
            case ASSOC_ADD: {
              addAssocListMethod(method, node, getAssocAddTemplate());
              break;
            }
            case ASSOC_REMOVE: {
              addAssocListMethod(method, node, getAssocRemoveTemplate());
              break;
            }
            case ASSOC_SET: {
              addAssocListMethod(method, node, getAssocSetTemplate());
              break;
            }
            case ASSOC_SET_ABSENT: {
              addAssocMethod(method, node, getAssocSetAbsentTemplate());
              break;
            }
            case NAVIGATE: {
              if ((method.getParamTypes().size() != 1 && node.getArguments().sizeExpressions() != 1) ||
                  (!util.isViewElement(method.getParamTypes().getFirst()) &&
                      !util.isStringLiteral(node.getArguments().getExpression(0)))) {
                Log.error("0xD001C Function 'navigate' expects one argument that is either of type GUIViewElement, "
                    + "e.g. '@MyPage(param = val)', or an URL as String",
                    node.get_SourcePositionStart(),
                    node.get_SourcePositionEnd());
              }
              ASTExpression navArgExpr = node.getArguments().getExpression(0);

              if (TypeScriptTypes.CONSUMER.getJavaQName().isPresent()) {
                addImport(TypeScriptTypes.CONSUMER.getJavaQName().get());
              }
              if (TypeScriptTypes.APP_ROUTER.getJavaQName().isPresent()) {
                addImport(TypeScriptTypes.APP_ROUTER.getJavaQName().get());
              }
              if (TypeScriptTypes.ROUTER.getJavaQName().isPresent()) {
                addImport(TypeScriptTypes.ROUTER.getJavaQName().get());
              }

              if (util.isStringLiteral(navArgExpr)) {
                ASTLiteralExpression routingUrlExpr = GUIDSLMill.typeDispatcher().asExpressionsBasisASTLiteralExpression(navArgExpr);
                ASTStringLiteral routingUrl = GUIDSLMill.typeDispatcher().asMCCommonLiteralsASTStringLiteral(routingUrlExpr.getLiteral());
                getCD4C().addMethod(clazz, getNavigateStringTemplate(),
                    method.getAlias(), routingUrl.getValue());
              }
              else if (util.isViewElement(method.getParamTypes().getFirst())) {
                ASTGUIComponent guiComponent = GUIDSLMill.typeDispatcher().asGUIDSLASTGUIComponent(navArgExpr);
                printNavigateMethod(method, node, guiComponent);
                prepareNavigateEventCaller(node, guiComponent);
              }
              break;
            }
          }
        }
        // custom function: @PersonForm(person = makePerson())
        else {
          ASTMCType type = getSimpleType(createTypeAndAddImports(method.getReturnType()));
          List<ASTCDParameter> params = new ArrayList<>();
          for (int i = 0; i < method.getParamTypes().size(); ++i) {
            SymTypeExpression argSTE = method.getParamTypes().get(i);
            params.add(CDParameterFacade.getInstance().createParameter(
                getSimpleType(createTypeAndAddImports(argSTE)),
                Expression2SetterPrinter.SETTER_PARAM + "_$" + i
            ));
          }

          ASTCDMethodSignature methodAST = getCD4C().addMethod(clazz, getEmptyMethodTemplate(), type, name, params);
          addImport("umlp.jsweet.extension.annotation.ObserverRegistrationParams");
          glex.replaceTemplate("cd2java.Annotations", methodAST, new StringHookPoint("@ObserverRegistrationParams"));
        }
      }
    }
  }

  @Override
  public void endVisit(ASTFieldAccessExpression node) {
    Map<ASTNode, Attribute> node2Attr = StaticObjectsCollector.getInstance().getNode2Attr();
    // an assoc access, e.g., @AddressForm(person = person.address)
    if (node2Attr.containsKey(node)) {
      // get init method info
      Attribute attrWrapper = node2Attr.get(node);
      for (String qName : attrWrapper.getImportList()) {
        addImport(qName);
      }
      ASTCDAttribute attr = attrWrapper.getCdAttr();

      // calc iterate items as additional args
      List<ASTCDParameter> params = attrWrapper.getParams();

      String name = attr.getName();

      SymTypeExpression accessType = util.getType(node);
      SymTypeExpression ownerType = util.getType(node.getExpression());
      // a domain list, e.g., @FriendsOverview(persons = person.friends)
      if (util.isDomainListType(accessType)) {
        // add assoc init
        if (ownerType.hasTypeInfo() && TagLookUp.isDerived(ownerType.getTypeInfo())) {
          ExpressionPrinterConfig.getInstance().customConversions.put(node,
              n -> "calc" + StringTransformations.capitalize(node.getName()) + "()");
        }
        else {
          String tmpName = ExpressionPrinterConfig.attr2Getter(node);
          if (tmpName.endsWith("List")) {
            tmpName = tmpName.substring(0, tmpName.length() - 4) + "GemList";
          }
          final String exprName = tmpName;
          ExpressionPrinterConfig.getInstance().customConversions.put(node,
              n -> "_internal_" + exprName + "().loadAll()");

          // extends PersonObserverMixin
          addAssocObserverMethods(ownerType, node);
        }

        IterationItemsFinder finder = new IterationItemsFinder();
        Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
        Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

        Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
            new IndentPrinter(), false, iterateItems, nonClassMembers
        );
        String getter = getterPrinter.prettyprint(node);
        ExpressionPrinterConfig.getInstance().customConversions.remove(node);
        String ownerGetter = getterPrinter.prettyprint(node.getExpression());

        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          dep.get().setInitMethod(new MethodTemplate(
              getListAssocInitTemplate(), name, getter, ownerGetter, params
          ));
        }
      }
      // a domain object, e.g., @AddressForm(person = person.address)
      else if (util.isDomainType(accessType)) {
        // add assoc init
        IterationItemsFinder finder = new IterationItemsFinder();

        if (ownerType.hasTypeInfo() && TagLookUp.isDerived(ownerType.getTypeInfo())) {
          ExpressionPrinterConfig.getInstance().customConversions.put(node,
              n -> "calc" + StringTransformations.capitalize(node.getName()) + "()");
        }
        else {
          ExpressionPrinterConfig.getInstance().customConversions.put(node,
              n -> ExpressionPrinterConfig.attr2Getter(node) + "Gem().load()");
        }
        Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
        Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

        Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
            new IndentPrinter(), false, iterateItems, nonClassMembers
        );
        String getter = getterPrinter.prettyprint(node);
        ExpressionPrinterConfig.getInstance().customConversions.remove(node);

        Optional<InitDependency> dep = InitDependencyTree.getInstance()
            .getDependency(node);
        if (dep.isPresent()) {
          dep.get().setInitMethod(new MethodTemplate(
              getObjectAssocInitTemplate(), name, getter, params
          ));
        }
      }
    }
    else if (util.isEnumMember(node)) {
      getSimpleType(createTypeAndAddImports(util.getType(node)));
    }
  }

  /**
   * Add directive to a Java class for further JSweet translation
   */
  protected void addClassAnnotation() {
    String domainModel = util.getMainDomainModelQName().orElse("");
    boolean isEmpty = gui.getGUIComponentDeclaration().getGUIParamList().isEmpty();

    addImport("umlp.jsweet.extension.annotation.Directive");
    glex.replaceTemplate("cd2java.Annotations", clazz,
        new TemplateHookPoint("umlp.gui2java.GUI2JavaDirectiveAnnotation", domainModel, isEmpty));
  }

  protected void addConstructor() {
    getCD4C().addConstructor(clazz, "umlp.gui2java.GUI2JavaConstructor");
  }

  /**
   * An init method is a slightly more flexible alternative to a constructor.
   *
   * @return class initialization method
   */
  protected void addInit() {
    Optional<InitDependency> root = InitDependencyTree.getInstance()
        .getDependency(gui.getGUIComponentDeclaration());
    if (root.isPresent()) {
      Collection<ASTCDMethodSignature> paramInit = addInitMethods(root.get());
      for (ASTCDMethodSignature init : paramInit) {
        glex.addAfterTemplate(
            getInitBodyHookpoint(),
            new TemplateHookPoint(getInitCallTemplate(), init)
        );
      }
    }

    getCD4C().addMethod(clazz, getInitTemplate());
  }

  @Override
  protected void addDestroy() {
    getCD4C().addMethod(clazz, getDestroyTemplate());
  }

  protected Collection<ASTCDMethodSignature> addInitMethods(InitDependency ds) {
    Collection<ASTCDMethodSignature> res = new ArrayList<>();
    if (ds.getInitMethod().isEmpty()) {
      for (InitDependency sub : ds.getDependentList()) {
        res.addAll(addInitMethods(sub));
      }
    }
    else {
      MethodTemplate initMethod = ds.getInitMethod().get();

      Collection<ASTCDMethodSignature> subInitList = new ArrayList<>();
      for (InitDependency sub : ds.getDependentList()) {
        subInitList.addAll(addInitMethods(sub));
      }
      List<Object> args = initMethod.getArguments();
      args.add(subInitList);
      res.add(getCD4C().addMethod(clazz, initMethod.getTemplate(), args.toArray()));
    }
    return res;
  }

  protected String getAssocNotifyKey(SymTypeExpression ownerType, String assocName) {
    return ownerType.printFullName() + "#" + assocName;
  }

  protected void extendDomainObserver() {
    // extend observer
    if (!clazz.isPresentCDExtendUsage()) {
      clazz.setCDExtendUsage(CD4CodeMill.cDExtendUsageBuilder()
          .setSuperclassList(new ArrayList<>()).build());
    }
    clazz.getSuperclassList().add(MCTypeFacade.getInstance()
        .createQualifiedType(getAndImportDomainObserver()));
  }

  protected void addDomainObserverMethods() {
    if (!extendedDomainObserver) {
      extendDomainObserver();
      extendedDomainObserver = true;
      // add observer init
      glex.addAfterTemplate(
          getInitBodyHookpoint(),
          new TemplateHookPoint(getAddObserverTemplate(), getAndImportManager())
      );

      // add observer destroy
      glex.addAfterTemplate(
          getDestroyBodyHookpoint(),
          new TemplateHookPoint(getRemoveObserverTemplate(), getAndImportManager())
      );

      // implement getScope method of observer
      addGetScope();
    }
  }

  protected void implementAssocObserver(SymTypeExpression ownerType, ASTFieldAccessExpression assoc) {
    // the CD Generator produces `hardy.domain.Person`, instead of `hardy.Domain.Person`, calculate
    String qType = ownerType.printFullName();
    String type = ownerType.print();
    List<String> parts = new ArrayList<>(Splitters.DOT.splitToList(qType));
    if (parts.size() > 1) {
      String domainPackage = parts.get(parts.size() - 2).toLowerCase();
      parts.set(parts.size() - 2, domainPackage);
      qType = Joiners.DOT.join(parts);
    }

    String qObserver = qType + ObserverDecorator.SUFFIX;
    String observer = type + ObserverDecorator.SUFFIX;
    addImport(qObserver);
    // implement observer
    if (!clazz.isPresentCDInterfaceUsage()) {
      clazz.setCDInterfaceUsage(CD4CodeMill.cDInterfaceUsageBuilder()
          .setInterfaceList(new ArrayList<>()).build());
    }
    clazz.getInterfaceList().add(MCTypeFacade.getInstance()
        .createQualifiedType(observer));
  }

  protected void addAssocObserverMethods(SymTypeExpression ownerType, ASTFieldAccessExpression assoc) {
    if (!implementedAssocObservers.contains(ownerType.printFullName())) {
      implementAssocObserver(ownerType, assoc);
      implementedAssocObservers.add(ownerType.printFullName());

      // implement getScope method of observer
      addGetScope();
    }

    String key = getAssocNotifyKey(ownerType, assoc.getName());
    if (!notifyAssocAdded.containsKey(key)) {
      ASTMCType type = getSimpleType(createTypeAndAddImports(ownerType));
      notifyAssocAdded.put(key, new NotifyMethodTemplate(getNotifyAssocAddedTemplate(), type, assoc.getName()));
    }
  }

  /**
   * Both domain observer & assoc observer require a getScope method
   */
  protected void addGetScope() {
    if (getScopeAdded) return;
    getScopeAdded = true;
    // implement getScope method of observer
    addImport(PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addMethod(clazz, getScopeTemplate());
  }

  protected void addAssocListMethod(Method method, ASTCallExpression node, String template) {
    // TODO AGe: better "first arg is an assoc" check
    if (method.getParamTypes().size() != 2
        || (!util.isDomainListType(method.getParamTypes().get(0)) && !util.isDomainType(method.getParamTypes().get(0)))
        || node.getArguments().sizeExpressions() != 2
        || !(node.getArguments().getExpression(0) instanceof ASTFieldAccessExpression)
        || !util.isDomainType(method.getParamTypes().get(1))
    ) {
      Log.error("0x0001A Functions 'assoc_add' and 'assoc_remove' expect 2 arguments:"
          + "an association and a domain object"
          + " at " + node.get_SourcePositionStart().toString()
          + " - " + node.get_SourcePositionEnd().toString());
    }
    ASTMCType type = getSimpleType(createTypeAndAddImports(method.getParamTypes().get(1)));

    ASTExpression holder = ((ASTFieldAccessExpression) node.getArguments()
        .getExpression(0)).getExpression();

    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
    Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

    Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
        new IndentPrinter(), false, iterateItems, nonClassMembers
    );
    String getterValue = getterPrinter.prettyprint(holder);
    boolean isList = util.isDomainListType(method.getParamTypes().get(0));

    String assoc = ((ASTFieldAccessExpression) node.getArguments()
        .getExpression(0)).getName();

    if (TypeScriptTypes.CONSUMER.getJavaQName().isPresent()) {
      addImport(TypeScriptTypes.CONSUMER.getJavaQName().get());
    }
    getCD4C().addMethod(clazz, template,
        method.getAlias(), type, getterValue, isList, assoc);
  }

  protected void addAssocMethod(Method method, ASTCallExpression node, String template) {
    if (method.getParamTypes().size() != 1
        || node.getArguments().sizeExpressions() != 1
        || !(node.getArguments().getExpression(0) instanceof ASTFieldAccessExpression)
        || !util.isDomainType(method.getParamTypes().get(0))
    ) {
      Log.error("0x0001A Functions 'assoc_add' and 'assoc_remove' expect 2 arguments:"
          + "an association and a domain object"
          + " at " + node.get_SourcePositionStart().toString()
          + " - " + node.get_SourcePositionEnd().toString());
    }
    ASTMCType type = getSimpleType(createTypeAndAddImports(method.getParamTypes().get(0)));

    ASTExpression holder = ((ASTFieldAccessExpression) node.getArguments()
        .getExpression(0)).getExpression();

    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterateItems = new LinkedHashSet<>(finder.getItems(gui));
    Collection<ASTExpression> nonClassMembers = new LinkedHashSet<>(iterateItems);

    Expression2AccessorPrinter getterPrinter = new Expression2AccessorPrinter(
        new IndentPrinter(), false, iterateItems, nonClassMembers
    );
    String getterValue = getterPrinter.prettyprint(holder);
    boolean isList = false;

    String assoc = ((ASTFieldAccessExpression) node.getArguments()
        .getExpression(0)).getName();

    if (TypeScriptTypes.CONSUMER.getJavaQName().isPresent()) {
      addImport(TypeScriptTypes.CONSUMER.getJavaQName().get());
    }
    getCD4C().addMethod(clazz, template,
        method.getAlias(), type, getterValue, isList, assoc);
  }

  protected String getConvertedValueAssignment(String type, String varName, String value) {
    return type + " " + varName + " = " + value;
  }

  private void printNavigateMethod(Method method, ASTCallExpression node, ASTGUIComponent guiComponent) {
    String pageName = guiComponent.getName();
    String path = Names.getPathFromQualifiedName(guiComponent.getNameSymbol().getFullName());
    ASTGUINamedArgs guiCompArgs = guiComponent.getGUINamedArgs();
    if (guiCompArgs.sizeGUINamedArgs() > 0 && guiCompArgs.getGUINamedArgList().stream().anyMatch(arg -> {
      // Check if arguments have at least one domain type argument
      SymTypeExpression typeSTE = guiComponent.getArgumentTypes().get(arg);
      return util.isDomainType(typeSTE);
    })) {
      // @FooPage(Foo foo, int i, Bar bar) -> navigate_0(Foo foo, /*ignore primitive type int,*/ Bar bar)
      List<ASTMCType> navArgTypes = guiCompArgs.getGUINamedArgList().stream()
          .map(arg -> guiComponent.getArgumentTypes().get(arg))
          .filter(util::isDomainType)
          .map(domainTypeSTE -> getSimpleType(createTypeAndAddImports(domainTypeSTE)))
          .toList();
      getCD4C().addMethod(clazz, getNavigateWithIdTemplate(), method.getAlias(), path, pageName, navArgTypes);
    }
    else {
      // @FooPage(int i) -> navigate_1()
      getCD4C().addMethod(clazz, getNavigateTemplate(), method.getAlias(), path, pageName);
    }
  }

  private void prepareNavigateEventCaller(ASTCallExpression node, ASTGUIComponent guiComponent) {
    // remove GUIViewElement argument
    if (node.getArguments().sizeExpressions() == 1) {
      node.getArguments().removeExpression(0);
    }

    if (guiComponent.getGUINamedArgs().sizeGUINamedArgs() > 0) {
      for (ASTGUINamedArg namedArg : guiComponent.getGUINamedArgs().getGUINamedArgList()) {
        SymTypeExpression typeSTE = guiComponent.getArgumentTypes().get(namedArg);
        if (util.isDomainType(typeSTE)) {
          // add actual navigation arguments
          node.getArguments().addExpression(namedArg.getExpression());
        }
      }
    }
  }

  // TODO AGe: replace with RTE functions and specific components
  public Function<String, String> getConversion(
      SymTypeExpression left, SymTypeExpression right
  ) {
    boolean leftIsString = SymTypeRelations.isString(left);

    boolean rightIsOptional = util.isOptional(right);
    if (rightIsOptional) {
      return optional2XConversion(getConversion(left, util.getOptionalTypeArgument(right)));
    }

    boolean rightIsDate = right.printFullName()
        .equals(TypeScriptTypes.LOCAL_DATE.getJavaQName().get());
    if (leftIsString && rightIsDate) {
      return date2StringConversion();
    }

    boolean rightIsDateTime = right.printFullName()
        .equals(TypeScriptTypes.LOCAL_DATE_TIME.getJavaQName().get());
    if (leftIsString && rightIsDateTime) {
      return dateTime2StringConversion();
    }

    boolean rightIsNumber = TypeScriptTypes.isNumber(right);
    if (leftIsString && rightIsNumber) {
      return number2StringConversion();
    }

    boolean rightIsEnum = util.isEnumType(right);
    if (leftIsString && rightIsEnum) {
      String type = getSimpleType(createTypeAndAddImports(right)).printType();
      return enum2StringConversion(type);
    }

    boolean rightIsString = SymTypeRelations.isString(right);

    boolean leftIsOptional = util.isOptional(left);
    if (leftIsOptional) {
      return x2OptionalConversion(getConversion(util.getOptionalTypeArgument(left), right));
    }

    boolean leftIsDate = left.printFullName()
        .equals(TypeScriptTypes.LOCAL_DATE.getJavaQName().get());
    if (rightIsString && leftIsDate) {
      getSimpleType(createTypeAndAddImports(left));
      return string2DateConversion();
    }

    boolean leftIsDateTime = left.printFullName()
        .equals(TypeScriptTypes.LOCAL_DATE_TIME.getJavaQName().get());
    if (rightIsString && leftIsDateTime) {
      getSimpleType(createTypeAndAddImports(left));
      return string2DateTimeConversion();
    }

    boolean leftIsNumber = TypeScriptTypes.isNumber(left);
    if (rightIsString && leftIsNumber) {
      String type = getSimpleType(createTypeAndAddImports(left)).printType();
      return string2NumberConversion(type);
    }

    boolean leftIsEnum = util.isEnumType(left);
    if (rightIsString && leftIsEnum) {
      String type = getSimpleType(createTypeAndAddImports(left)).printType();
      return string2EnumConversion(type);
    }

    if (leftIsNumber && rightIsNumber) {
      String leftType = getSimpleType(createTypeAndAddImports(left)).printType();
      String rightType = getSimpleType(createTypeAndAddImports(right)).printType();
      if (!util.getBoxedType(leftType).equals(util.getBoxedType(rightType))) {
        return number2NumberConversion(leftType);
      }
    }

    return s -> s;
  }

  protected Function<String, String> date2StringConversion() {
    return s -> s + ".toString()";
  }

  protected Function<String, String> string2DateConversion() {
    return s -> "LocalDate.parse(" + s + ")";
  }

  protected Function<String, String> dateTime2StringConversion() {
    return s -> s + ".toString()";
  }

  protected Function<String, String> string2DateTimeConversion() {
    return s -> "LocalDateTime.parse(" + s + ")";
  }

  protected Function<String, String> number2StringConversion() {
    return s -> "String.valueOf(" + s + ")";
  }

  protected Function<String, String> string2NumberConversion(String type) {
    String finalType = util.getBoxedType(type);
    return s -> finalType + ".valueOf(" + s + ")";
  }

  protected Function<String, String> number2NumberConversion(String type) {
    String finalType = util.getBoxedType(type);
    String accessor = numberAccessorMethod(finalType);
    return s -> finalType + ".valueOf(" + s + "." + accessor + "())";
  }

  protected String numberAccessorMethod(String boxedType) {
    switch (boxedType) {
      case "Integer":
        return "intValue";
      case "Long":
        return "longValue";
      case "Float":
        return "floatValue";
      case "Double":
        return "doubleValue";
      default:
        return "doubleValue";
    }
  }

  protected Function<String, String> enum2StringConversion(String type) {
    return s -> s + ".toString()";
  }

  protected Function<String, String> string2EnumConversion(String type) {
    return s -> type + ".valueOf(" + s + ")";
  }

  /*
  protected Function<String, String> optionalNumber2StringConversion() {
    return s -> s + ".isPresent() ? " + "String.valueOf(" + s + ".get()" + ")" + " : \"\"";
  }

  protected Function<String, String> string2OptionalNumberConversion(String type) {
    String boxedType = util.getBoxedType(type);
    return s -> "Optional.of(" + boxedType + ".valueOf(" + s + ")" + ")";
  }

  protected Function<String, String> optionalEnum2StringConversion(String type) {
    return s -> s + ".isPresent() ? " + s + ".get().toString() : \"\"";
  }

  protected Function<String, String> string2OptionalEnumConversion(String type) {
    return s -> "Optional.of(" + type + ".valueOf(" + s + ")" + ")";
  }
  /**/

  protected Function<String, String> optional2XConversion(Function<String, String> conv) {
    return s -> s + ".isPresent() ? " + conv.apply(s + ".get()") + " : null";
  }

  protected Function<String, String> x2OptionalConversion(Function<String, String> conv) {
    return s -> "Optional.of(" + conv.apply(s) + ")";
  }

  protected String printType(ASTMCType type) {
    return new CdUtilsPrinter().printType(type);
  }

  protected String getInitDefaultParamTemplate() {
    return "umlp.gui2java.GUI2JavaInitDefaultParam";
  }

  protected String getInitDomainListTemplate() {
    return "umlp.gui2java.GUI2JavaInitDomainList";
  }

  protected String getNotifyObserversTemplate() {
    return "umlp.gui2java.GUI2JavaNotifyObservers";
  }

  protected String getNotifyDeletedTemplate() {
    return "umlp.gui2java.GUI2JavaNotifyDeleted";
  }

  protected String getNotifyAddedTemplate() {
    return "umlp.gui2java.GUI2JavaNotifyAdded";
  }

  protected String getNotifyAssocAddedTemplate() {
    return "umlp.gui2java.GUI2JavaNotifyAssocAdded";
  }

  protected String getInitParamSingletonTemplate() {
    return "umlp.gui2java.GUI2JavaInitParamSingleton";
  }

  protected String getInitDerivedDomainParamTemplate() {
    return "umlp.gui2java.GUI2JavaInitDerivedDomainParam";
  }

  protected String getInitDomainParamTemplate() {
    return "umlp.gui2java.GUI2JavaInitDomainParam";
  }

  protected String getInitCallTemplate() {
    return "umlp.gui2java.GUI2JavaInitCall";
  }

  protected String getInitTemplate() {
    return "umlp.gui2java.GUI2JavaInit";
  }

  protected String getInitBodyHookpoint() {
    return "umlp.gui2java.GUI2JavaInitBody";
  }

  protected String getDestroyTemplate() {
    return "umlp.gui2java.GUI2JavaDestroy";
  }

  protected String getDestroyBodyHookpoint() {
    return "umlp.gui2java.GUI2JavaDestroyBody";
  }

  protected String getSubscribeManagerEventsTemplate() {
    return "umlp.gui2java.GUI2JavaSubscribeManagerEvents";
  }

  protected String getAddObserverTemplate() {
    return "umlp.gui2java.GUI2JavaAddObserver";
  }

  protected String getRemoveObserverTemplate() {
    return "umlp.gui2java.GUI2JavaRemoveObserver";
  }

  protected String getParamGetterTemplate() {
    return "umlp.gui2java.GUI2JavaParamGetter";
  }

  protected String getParamAngularGetterTemplate() {
    return "umlp.gui2java.GUI2JavaParamAngularGetter";
  }

  protected String getParamSetterTemplate() {
    return "umlp.gui2java.GUI2JavaParamSetter";
  }

  protected String getParamSetterInternalTemplate() {
    return "umlp.gui2java.GUI2JavaParamSetterInternal";
  }

  protected String getArgGetterTemplate() {
    return "umlp.gui2java.GUI2JavaArgGetter";
  }

  protected String getArgAngularGetterTemplate() {
    return "umlp.gui2java.GUI2JavaArgAngularGetter";
  }

  protected String getArgSetterTemplate() {
    return "umlp.gui2java.GUI2JavaArgSetter";
  }

  protected String getArgGetterWithConversionTemplate() {
    return "umlp.gui2java.GUI2JavaArgGetterWithConversion";
  }

  protected String getArgGetterWithConversionAngularTemplate() {
    return "umlp.gui2java.GUI2JavaArgAngularGetterWithConversion";
  }

  protected String getArgSetterWithConversionTemplate() {
    return "umlp.gui2java.GUI2JavaArgSetterWithConversion";
  }

  protected String getArgConversionTemplate() {
    return "umlp.gui2java.GUI2JavaArgConversion";
  }

  protected String getListInitTemplate() {
    return "umlp.gui2java.GUI2JavaListInit";
  }

  protected String getIterateInitTemplate() {
    return "umlp.gui2java.GUI2JavaIterateInit";
  }

  protected String getGetterTemplate() {
    return "umlp.gui2java.GUI2JavaGetter";
  }

  protected String getAngularGetterTemplate() {
    return "umlp.gui2java.GUI2JavaAngularGetter";
  }

  protected String getStaticObjectGetterTemplate() {
    return "umlp.gui2java.GUI2JavaStaticObjectGetter";
  }

  protected String getStaticListInitTemplate() {
    return "umlp.gui2java.GUI2JavaStaticListInit";
  }

  protected String getBuilderInitTemplate() {
    return "umlp.gui2java.GUI2JavaBuilderInit";
  }

  protected String getBuilderViaConstructorInitTemplate() {
    return "umlp.gui2java.GUI2JavaBuilderViaConstructorInit";
  }

  protected String getConstructorInitTemplate() {
    return "umlp.gui2java.GUI2JavaConstructorInit";
  }

  protected String getStaticBuilderInitTemplate() {
    return "umlp.gui2java.GUI2JavaStaticBuilderInit";
  }

  protected String getStaticBuilderViaConstructorInitTemplate() {
    return "umlp.gui2java.GUI2JavaStaticBuilderViaConstructorInit";
  }

  protected String getStaticConstructorInitTemplate() {
    return "umlp.gui2java.GUI2JavaStaticConstructorInit";
  }

  protected String getListAssocInitTemplate() {
    return "umlp.gui2java.GUI2JavaListAssocInit";
  }

  protected String getObjectAssocInitTemplate() {
    return "umlp.gui2java.GUI2JavaObjectAssocInit";
  }

  protected String getEmptyMethodTemplate() {
    return "umlp.gui2java.GUI2JavaEmptyMethod";
  }

  protected String getEventCallerTemplate() {
    return "umlp.gui2java.GUI2JavaEventCaller";
  }

  protected String getCreateTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaCreate";
  }

  protected String getDeleteTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaDelete";
  }

  protected String getAssocAddTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaAssocAdd";
  }

  protected String getAssocRemoveTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaAssocRemove";
  }

  protected String getAssocSetTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaAssocAdd";
  }

  protected String getAssocSetAbsentTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaAssocRemove";
  }

  protected String getScopeTemplate() {
    return "umlp.gui2java.GUI2JavaScope";
  }

  protected String getCommandExecutionTemplate() {
    return "umlp.gui2java.GUI2JavaExecuteCommandMethod";
  }

  protected String getNavigateTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaNavigate";
  }

  protected String getNavigateStringTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaNavigateString";
  }

  protected String getNavigateWithIdTemplate() {
    return "umlp.gui2java.predefined.GUI2JavaNavigateWithId";
  }
}
