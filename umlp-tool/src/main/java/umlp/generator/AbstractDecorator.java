/* (c) https://github.com/MontiCore/monticore */
package umlp.generator;

import com.google.common.collect.Iterables;
import de.monticore.ast.ASTNode;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.facade.CDAttributeFacade;
import de.monticore.cd.facade.CDConstructorFacade;
import de.monticore.cd.facade.CDExtendUsageFacade;
import de.monticore.cd.facade.CDInterfaceUsageFacade;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.facade.CDStereotypeFacade;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._prettyprint.CD4CodeFullPrettyPrinter;
import de.monticore.cd4codebasis._ast.ASTCDConstructor;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.HookPoint;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symbols.basicsymbols._symboltable.DiagramSymbol;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.TypeCheck;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedType;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.Splitters;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO refactor this class
public abstract class AbstractDecorator {

  // TODO: Why are these static?
  protected static Map<ASTNode, String> origins = new LinkedHashMap<>();

  protected static HookPoint originHookPoint = new OriginHookPoint(origins);

  public static HookPoint getOriginHookPoint() {
    return originHookPoint;
  }

  public static void cleanUp() {
    // the tool class loader is reused for the next project, which may have no OCL files
    hasOCL = false;
    origins.clear();
    origins = new LinkedHashMap<>();
    originHookPoint = new OriginHookPoint(origins);
  }

  public AbstractDecorator(GlobalExtensionManagement glex) {
    this.glex = glex;
  }

  public static final String DEFAULT_PACKAGE = "";

  public static final String COMMAND_PACKAGE = DEFAULT_PACKAGE + "commands";

  public static final String GEMLIST_PACKAGE = DEFAULT_PACKAGE + "gemlists";

  public static final String GEMMAP_PACKAGE = DEFAULT_PACKAGE + "gemmaps";

  public static final String CLIENT_PACKAGE = DEFAULT_PACKAGE + "client";

  public static final String SHELLAPI_PACKAGE = DEFAULT_PACKAGE + "shellapi";

  public static boolean hasOCL = false;

  protected final GlobalExtensionManagement glex;

  protected final CD4CodeFullPrettyPrinter printer = new CD4CodeFullPrettyPrinter(new IndentPrinter(), false);

  protected TypeCheck typeCheck;

  /**
   * A {@link Map} <strong>possibly</strong> containing other decorators, which have been used
   * during the last {@link #transform(ASTCDCompilationUnit, ASTCDCompilationUnit)} call.
   *
   * <p>There is no guarantee that decorators which have been used are actually added to this map,
   * since there is currently no use for that. However, it can be used to pass meta information
   * out of a {@link #transform(ASTCDCompilationUnit, ASTCDCompilationUnit)} call.
   *
   * <p><strong>Adding decorators</strong>
   * <p>You should probably never directly add a decorator to this map, but use
   * {@link #remember(AbstractDecorator)} instead.
   *
   * <p><strong>Retrieving decorators</strong>
   * <p>In order to retrieve a used decorator by type, it is recommended to use
   * {@link #getUsedDecorator(Class)}.
   *
   * <p>It is highly recommended to clear this map on the start of a new
   * {@link #transform(ASTCDCompilationUnit, ASTCDCompilationUnit)} call
   *
   * @see #getUsedDecorator(Class)
   * @see #remember(AbstractDecorator)
   * @see Map#clear()
   */
  protected final Map<Class<?>, AbstractDecorator> usedDecorators = new HashMap<>();

  /**
   * Adds an {@link AbstractDecorator} to {@link #usedDecorators} and returns it again for further
   * usage.
   *
   * <p><strong>Example</strong>
   * <pre>{@code
   * ObserverDecorator observerDecorator = remember(new ObserverDecorator(glex));
   * observerDecorator.transform(cdAST, decoratedAST);
   * }</pre>
   *
   * @param decorator The decorator to add
   * @see #usedDecorators
   * @see #getUsedDecorator(Class)
   */
  protected <T extends AbstractDecorator> T remember(T decorator) {
    usedDecorators.put(decorator.getClass(), decorator);
    return decorator;
  }

  /**
   * Attempt to retrieve another decorator which was used during the latest
   * {@link #transform(ASTCDCompilationUnit, ASTCDCompilationUnit)} call.
   *
   * <p>Decorators sometimes store additional information about the types they generate and this
   * method provides a way to access that information.
   *
   * @param decoratorType The {@link Class} of the decorator to get
   * @return The instance of the decorator, if it was used and stored in the last
   * {@link #transform(ASTCDCompilationUnit, ASTCDCompilationUnit)} call
   * @param <T> The type of the decorator to retrieve
   */
  public <T extends AbstractDecorator> Optional<T> getUsedDecorator(Class<T> decoratorType) {
    return Optional.ofNullable(usedDecorators.get(decoratorType))
        .map(decorator -> {
          //noinspection unchecked
          return (T) decorator;
        });
  }

  protected GlobalExtensionManagement getGlex() {
    return glex;
  }

  protected MCTypeFacade getMCTypeFacade() {
    return MCTypeFacade.getInstance();
  }

  protected CDAttributeFacade getCDAttributeFacade() {
    return CDAttributeFacade.getInstance();
  }

  protected CDConstructorFacade getCDConstructorFacade() {
    return CDConstructorFacade.getInstance();
  }

  protected CDMethodFacade getCDMethodFacade() {
    return CDMethodFacade.getInstance();
  }

  protected CDParameterFacade getCDParameterFacade() {
    return CDParameterFacade.getInstance();
  }

  protected CDExtendUsageFacade getExtendUsageFacade() {
    return CDExtendUsageFacade.getInstance();
  }

  protected CDInterfaceUsageFacade getInterfaceUsageFacade() {
    return CDInterfaceUsageFacade.getInstance();
  }

  protected CDStereotypeFacade getStereotypeFacade() {
    return CDStereotypeFacade.getInstance();
  }

  protected static CD4C getCD4C() {
    return CD4C.getInstance();
  }

  protected CD4CodeFullPrettyPrinter getCDPrinter() {
    return printer;
  }

  public TypeCheck getTypeCheck() {
    return typeCheck;
  }

  public void setTypeCheck(TypeCheck typeCheck) {
    this.typeCheck = typeCheck;
  }

  /**
   * Adds an attribute to the given {@link ASTCDType} without adding getters, setters or any initial value.
   *
   * @param target      Where to define the attribute
   * @param declaration The declaration code of the attribute
   * @return The added {@link ASTCDAttribute}
   * @see #addAttribute(ASTCDType, String, String)
   */
  protected ASTCDAttribute addAttribute(ASTCDType target, String declaration) {
    return addAttribute(target,
                        false,
                        false,
                        declaration,
                        Optional.empty());
  }

  /**
   * Adds an attribute with an initial value to the given {@link ASTCDType} without adding getters nor setters.
   *
   * @param target       Where to define the attribute
   * @param declaration  The declaration code of the attribute
   * @param initialValue The initial value code of the attribute. If blank no initial value will be added
   * @return The added {@link ASTCDAttribute}
   * @see #addAttribute(ASTCDType, boolean, boolean, String, String)
   */
  protected ASTCDAttribute addAttribute(ASTCDType target, String declaration, String initialValue) {
    return addAttribute(target, false, false, declaration, initialValue);
  }

  /**
   * Adds an attribute with an initial value to the given {@link ASTCDType}.
   *
   * @param target       Where to define the attribute
   * @param addGetter    Whether to add a getter
   * @param addSetter    Whether to add a setter
   * @param declaration  The declaration code of the attribute
   * @param initialValue The initial value code of the attribute. If blank no initial value will be
   *                     added. May start with an equals sign, but is not required to.
   * @return The added {@link ASTCDAttribute}
   * @see CD4C#addAttribute(ASTCDType, boolean, boolean, String)
   */
  protected ASTCDAttribute addAttribute(ASTCDType target, boolean addGetter, boolean addSetter, String declaration, String initialValue) {
    return addAttribute(target,
                        addGetter,
                        addSetter,
                        declaration,
                        Optional.of(initialValue)
                                .filter(s -> !s.isBlank())
                                .map(s -> (s.trim().startsWith("=") ? "" : " = ") + s)
                                .map(StringHookPoint::new));
  }

  /**
   * Adds an attribute with an initial value to the given {@link ASTCDType}.
   *
   * @param target                Where to define the attribute
   * @param addGetter             Whether to add a getter
   * @param addSetter             Whether to add a setter
   * @param declaration           The declaration code of the attribute
   * @param initialValueHookPoint An optional {@link HookPoint} with the initial value of the
   *                              attribute. The value must start with an equals sign!
   *                              (evaluate to an assign expression)
   * @return The added {@link ASTCDAttribute}
   * @see CD4C#addAttribute(ASTCDType, boolean, boolean, String)
   */
  protected ASTCDAttribute addAttribute(ASTCDType target, boolean addGetter, boolean addSetter, String declaration, Optional<HookPoint> initialValueHookPoint) {
    ASTCDAttribute attribute = getCD4C().addAttribute(target, addGetter, addSetter, declaration);

    initialValueHookPoint.ifPresent(hookPoint -> getGlex().replaceTemplate("cd2java.Value",
                                                                           attribute,
                                                                           hookPoint));

    return attribute;
  }

  /**
   * Set the annotations for an {@link ASTNode} via {@link HookPoint}
   * <p>
   * <strong>Example</strong>
   * <pre>{@code
   * setAnnotations(method, new StringHookPoint("@AfterEach"));
   * }</pre>
   *
   * @param node      The {@link ASTNode} to set the annotations of
   * @param hookPoint The {@link HookPoint} generating the annotations
   */
  protected void setAnnotations(ASTNode node, HookPoint hookPoint) {
    glex.replaceTemplate("cd2java.Annotations", node, hookPoint);
  }

  /**
   * Adds constructors which delegate to {@code super()} for every constructor in the parent class. Logs an
   * error when the target doesn't extend the parent class
   *
   * @param target      A class to add the super constructors to
   * @param parentClass The parent class of the target class
   * @return All added constructors
   * @see Log#error(String)
   */
  @SuppressWarnings("UnusedReturnValue") //pretty sure someone will find this useful someday
  protected Collection<ASTCDConstructor> addSuperConstructors(ASTCDClass target, ASTCDClass parentClass) {
    //quick check that the target actually extends the parent class
    String parentClassName = parentClass.getName();
    String errorMessage = "Can't add super constructors: " + target.getName() + " doesn't extend " + parentClassName;
    List<ASTMCObjectType> superclassList = target.getCDExtendUsage().getSuperclassList();
    if (superclassList.isEmpty()) {
      Log.error(errorMessage);
      return List.of();
    }
    ASTMCObjectType superType = superclassList.get(0);
    if (superType instanceof ASTMCQualifiedType) {
      ASTMCQualifiedType qualifiedType = (ASTMCQualifiedType) superType;
      String superTypeName = qualifiedType.getMCQualifiedName().getBaseName();
      if (!superTypeName.equals(parentClassName)) {
        Log.error(errorMessage);
        return List.of();
      }
    } else {
      Log.warn("Failed to ensure that " + target.getName() + " really extends " + parentClassName);
    }

    //fetch the constructors
    List<ASTCDConstructor> constructors = parentClass.getCDMethodSignatureList()
        .stream()
        .filter(signature -> signature instanceof ASTCDConstructor)
        .map(signature -> (ASTCDConstructor) signature)
        .collect(Collectors.toList());

    CD4C cd4C = getCD4C();
    String typeName = target.getName();
    Collection<ASTCDConstructor> addedConstructors = new ArrayList<>();
    for (ASTCDConstructor constructor : constructors) {
      ASTCDConstructor addedConstructor = (ASTCDConstructor) cd4C.addConstructor(target,
                                                           "umlp.common.SuperConstructor",
                                                           typeName,
                                                           constructor.getCDParameterList());
      addedConstructors.add(addedConstructor);
    }
    return addedConstructors;
  }

  /**
   * Creates a deep clone of the provided {@link ASTCDMethodSignature}, replaces its body with a
   * {@link HookPoint} and adds the created signature to the target {@link ASTCDClass}
   *
   * @param original         The original {@link ASTCDMethodSignature}
   * @param newBodyHookPoint A {@link HookPoint} to replace the body
   * @param target           A {@link ASTCDClass} to add the new method to
   * @return The new {@link ASTCDMethodSignature} with the new body
   * @see #replaceMethodBody(ASTCDMethodSignature, HookPoint)
   */
  protected ASTCDMethodSignature replaceMethodBody(ASTCDMethodSignature original, HookPoint newBodyHookPoint, ASTCDClass target) {
    ASTCDMethodSignature copy = replaceMethodBody(original, newBodyHookPoint);
    target.addCDMember(copy);
    return copy;
  }

  /**
   * Creates a deep clone of the provided {@link ASTCDMethodSignature} and replaces its body with a
   * {@link HookPoint}
   *
   * @param original         The original {@link ASTCDMethodSignature}
   * @param newBodyHookPoint A {@link HookPoint} to replace the body
   * @return The new {@link ASTCDMethodSignature} with the new body
   */
  protected ASTCDMethodSignature replaceMethodBody(ASTCDMethodSignature original, HookPoint newBodyHookPoint) {
    ASTCDMethodSignature copy = original.deepClone();
    getGlex().replaceTemplate(getCD4C().getEmptyBodyTemplate(), copy, newBodyHookPoint);
    return copy;
  }

  /**
   * Gets all {@link ASTCDClass}es of a class diagramm which are not derived nor abstract
   *
   * @param cd A class diagramm
   * @return A list of all concrete domain classes
   * @see #getNonDerivedDomainClasses(ASTCDCompilationUnit)
   */
  protected List<ASTCDClass> getConcreteDomainClasses(ASTCDCompilationUnit cd) {
    return getDomainClasses(cd, Filter.DERIVED.or(Filter.ABSTRACT).negate());
  }

  /**
   * Gets all {@link ASTCDClass}es of a class diagramm which are derived
   *
   * @param cd A class diagramm
   * @return A list of all derived domain classes
   * @see #getNonDerivedDomainClasses(ASTCDCompilationUnit)
   */
  protected List<ASTCDClass> getDerivedDomainClasses(ASTCDCompilationUnit cd) {
    return getDomainClasses(cd, Filter.DERIVED);
  }

  /**
   * Gets all {@link ASTCDClass}es of a class diagramm which are not derived (but may be abstract!)
   *
   * @param cd A class diagramm
   * @return A list of all non-derived domain classes
   * @see #getConcreteDomainClasses(ASTCDCompilationUnit)
   */
  protected List<ASTCDClass> getNonDerivedDomainClasses(ASTCDCompilationUnit cd) {
    return getDomainClasses(cd, Filter.DERIVED.negate());
  }

  /**
   * Gets all classes from the specified class diagramm which match the given filter. Best used in
   * combination with {@link Filter}
   *
   * @param cd     A class diagramm
   * @param filter A filter
   * @return A list with all matching classes
   * @see Filter
   */
  protected List<ASTCDClass> getDomainClasses(ASTCDCompilationUnit cd, Predicate<Filter.Wrapper> filter) {
    return cd.getCDDefinition()
        .getCDClassesList()
        .stream()
        .filter(c -> filter.test(Filter.Wrapper.wrap(c)))
        .collect(Collectors.toList());
  }

  /**
   * Gets all {@link ASTCDInterface}s of a class diagramm which are not derived
   *
   * @param cd A class diagramm
   * @return A list of all non-derived domain interfaces
   */
  protected List<ASTCDInterface> getNonDerivedDomainInterfaces(ASTCDCompilationUnit cd) {
    return getDomainInterfaces(cd, Filter.DERIVED.negate());
  }

  /**
   * Gets all interfaces from the specified class diagramm which match the given filter. Best used in
   * combination with {@link Filter}
   *
   * @param cd     A class diagramm
   * @param filter A filter
   * @return A list with all matching interfaces
   * @see Filter
   */
  protected List<ASTCDInterface> getDomainInterfaces(ASTCDCompilationUnit cd, Predicate<Filter.Wrapper> filter) {
    return cd.getCDDefinition()
        .getCDInterfacesList()
        .stream()
        .filter(c -> filter.test(Filter.Wrapper.wrap(c)))
        .collect(Collectors.toList());
  }

  protected ASTCDPackage getPackage(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD, String packageName) {
    String completeName = getPackageName(origCD, packageName);
    Optional<ASTCDPackage> astCDPackage = decoratedCD.getCDDefinition()
        .getCDPackagesList()
        .stream()
        .filter(p -> p.getName().equals(completeName))
        .findFirst();
    if (astCDPackage.isPresent()) {
      return astCDPackage.get();
    } else {
      ASTCDPackage p = CD4CodeMill.cDPackageBuilder()
          .setMCQualifiedName(getMCTypeFacade().createQualifiedName(completeName)).build();
      decoratedCD.getCDDefinition().addCDElement(p);
      return p;
    }
  }

  public static String getPackageName(ASTCDCompilationUnit origCD, String subPackage) {
    String origPackage = Names.constructQualifiedName(origCD.getCDPackageList(), origCD.getCDDefinition().getName());
    return (subPackage.isEmpty() ? origPackage : Joiners.DOT.join(origPackage, subPackage)).toLowerCase();
  }

  protected String getPackageName(DiagramSymbol cD, String subPackage) {
    String origPackage = cD.getPackageName().isEmpty()? cD.getName().toLowerCase() :
        Joiners.DOT.join(cD.getPackageName(), cD.getName().toLowerCase());
    return (subPackage.isEmpty() ? origPackage : Joiners.DOT.join(origPackage, subPackage)).toLowerCase();
  }

  protected Optional<String> getPackageFromFQNString(String fQN) {
    String[] subNames = fQN.split("\\.");
    if (subNames.length > 1) {
      String[] packageNamesOnly = Arrays.copyOf(subNames, subNames.length - 1);
      return Optional.of(Joiners.DOT.join(packageNamesOnly));
    } else {
      return Optional.empty();
    }
  }

  protected String getTypeFromFQNString(String fQN) {
    String[] subNames = fQN.split("\\.");
    return subNames[subNames.length - 1];
  }

  protected List<ASTCDAttribute> getOptionalAttributes(ASTCDClass domainClass) {
    return CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && GenService.isOptional(a)).collect(Collectors.toList());
  }

  protected List<ASTCDAttribute> getListAttributes(ASTCDClass domainClass) {
    return CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && GenService.isList(a)).collect(Collectors.toList());
  }

  protected List<ASTCDAttribute> getNormalAttributes(ASTCDClass domainClass) {
    return CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() &&
            !GenService.isList(a) && !GenService.isOptional(a)).collect(Collectors.toList());
  }

  protected String getAssocName(ASTCDType type, ASTCDAssocSide assoc) {
    return type.getName()
        + "_"
        + StringTransformations.capitalize(assoc.getName());
  }

  protected void addImports(ASTCDType domainType, ASTCDType cdType) {
    CDSymbolTables.getAttributesInHierarchy(domainType).forEach(a -> addJavaImport(cdType, a.getSymbol().getType()));
    CDSymbolTables.getAssociationsInHierarchy(domainType).forEach(assocSide -> {
      getPackageFromFQNString(assocSide.getSymbol().getType().printFullName())
          .ifPresent(cdPackage ->
              getCD4C().addImport(cdType, Joiners.DOT.join(cdPackage.toLowerCase(),
                  getTypeFromFQNString(assocSide.getSymbol().getType().printFullName()))));
    });
    domainType.getInterfaceList().forEach(cdInterface ->
        UMLPSymbolTables.getTypeSymbol(cdInterface).ifPresent(definingSymbol ->
            getCD4C().addImport(cdType, Joiners.DOT.join(definingSymbol.getPackageName().toLowerCase(),
                definingSymbol.getName()))));
    domainType.getSuperclassList().forEach(cdExtension ->
        UMLPSymbolTables.getTypeSymbol(cdExtension).ifPresent(definingSymbol ->
            getCD4C().addImport(cdType, Joiners.DOT.join(definingSymbol.getPackageName().toLowerCase(),
                definingSymbol.getName()))));
  }

  protected void addJavaImport(ASTCDType domainType, SymTypeExpression type) {
    if (type.isGenericType()) {
      if(!(type.printFullName().contains("List")
          || type.printFullName().contains("Set")
          || type.printFullName().contains("Map")
          || type.printFullName().contains("Optional")
      )) {
        getCD4C().addImport(domainType, type.asGenericType().getTypeConstructorFullName());
      }

      if (GenService.isNotADomainType(type.asGenericType().getArgument(0))) {
        String imp = GenService.getCorrectJavaFQN(type.asGenericType().getArgument(0).printFullName());
        getCD4C().addImport(domainType, imp);
      } else if(GenService.isCDEnum(type.asGenericType().getArgument(0))) {
        String imp =
            Joiners.DOT.join(type.asGenericType().getArgument(0).getTypeInfo()
                    .getPackageName().toLowerCase(),
                type.asGenericType().getArgument(0).getTypeInfo().getName());
        getCD4C().addImport(domainType, imp);
      }
    } else if (GenService.isNotADomainType(type)) {
      getCD4C().addImport(domainType, GenService.getCorrectJavaFQN(type.printFullName()));
    } else if(GenService.isCDEnum(type)) {
      String imp =
          Joiners.DOT.join(type.getTypeInfo().getPackageName().toLowerCase(),type.getTypeInfo().getName());
      getCD4C().addImport(domainType, imp);
    }
  }

  protected void addDecoratorComment(ASTCDElement... nodes) {
    addDecoratorComment(Arrays.stream(nodes).collect(Collectors.toList()));
  }

  protected void addDecoratorComment(Collection<? extends ASTCDElement> nodes) {
    for (ASTCDElement node : nodes) {
      origins.put(node, "/* Created by decorator " + getClass().getName() + " */\n");
    }
  }

  protected ASTCDElement withOrigin(ASTCDElement element) {
    addDecoratorComment(element);
    return element;
  }

  protected Collection<? extends ASTCDElement> withOrigin(Collection<? extends ASTCDElement> elements) {
    addDecoratorComment(elements);
    return elements;
  }

  abstract public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD);


  public static String getCorrectManager(ASTCDAssociation assoc) {
    return UMLPSymbolTables.getDiagramFor(assoc).getCDDefinition().getName() + ManagerDecorator.SUFFIX;
  }

  public static String getCorrectManager(ASTCDType type) {
    return UMLPSymbolTables.getDiagramFor(type).getCDDefinition().getName() + ManagerDecorator.SUFFIX;
  }

  public static String getCorrectManager(ASTCDAssocSide assocSide) {
    return getClassDiagramName(assocSide) + ManagerDecorator.SUFFIX;
  }

  public static String getClassDiagramName(ASTCDAssocSide assocSide) {
    return UMLPSymbolTables.getDiagramFor(assocSide.getSymbol().getType().getTypeInfo().getAstNode()).getCDDefinition().getName();
  }

  public static String getCorrectManagerOfTargetType(ASTCDAssocSide assocSideTarget) {
    String qName = getManagerOfTargetTypeQName(assocSideTarget);
    return Iterables.getLast(Splitters.DOT.splitToList(qName));
  }

  public static String getManagerOfTargetTypeQName(ASTCDAssocSide assocSideTarget) {
    SymTypeExpression type = assocSideTarget.getSymbol().getType();
    List<String> fullName = Splitters.DOT.splitToList(type.printFullName());
    if (fullName.size() <= 1) {
      Log.error("0x0002C Manager name cannot be calculated. SymTypeExpression: '"
          + type + "'. Does not belong to a package defined by a Class Diagram.");
    }
    List<String> qName = new ArrayList<>(fullName.subList(0, Math.max(0, fullName.size() - 2)));
    qName.add(fullName.get(fullName.size() - 2).toLowerCase());
    String simpleName = fullName.get(fullName.size() - 2) + ManagerDecorator.SUFFIX;
    qName.add(simpleName);
    return Joiners.DOT.join(qName);
  }

  public static String getCorrectManager(ASTCDAttribute attribute) {
    return attribute.getSymbol().getPackageName() + ManagerDecorator.SUFFIX;
  }

  public static void addCorrectMangerImportForTargetType(ASTCDDefinition origCD, ASTCDType ast, SymTypeExpression type) {
    addImportForCorrectedPackage(origCD, ast, type, ManagerDecorator.SUFFIX);
  }

  public static void addImportForCorrectedPackage(ASTCDDefinition origCD, ASTCDType ast, SymTypeExpression type, String suffix){
    String[] fullName = type.printFullName().split("\\.");
    if (fullName.length <= 1) {
      return;
    }
    String potentialExtendedCDName = fullName[fullName.length - 2];
    String potentialExtendedCDFullName = Joiners.DOT.join(Arrays.copyOfRange(fullName, 0, fullName.length - 1));
    // This checks a CD 'extends' another one, i.e. a UMLP Project extends/ is composed of another one
    if (UMLPSymbolTables.getExtendedCDs(UMLPSymbolTables.getDiagramFor(origCD)).stream()
        .anyMatch(cd -> cd.getFullName().equals(potentialExtendedCDFullName))) {
      getCD4C().addImport(ast, Joiners.DOT.join(potentialExtendedCDFullName.toLowerCase(), potentialExtendedCDName) + suffix);
    }
  }

  protected List<ASTCDCompilationUnit> getExtendedDefinitions(ASTCDCompilationUnit origCD){
    return UMLPSymbolTables.getExtendedCDs(origCD).stream()
        .map(cd -> UMLPSymbolTables.getDiagramFor((ASTCDDefinition) cd.getAstNode()))
        .collect(Collectors.toList());
  }
}
