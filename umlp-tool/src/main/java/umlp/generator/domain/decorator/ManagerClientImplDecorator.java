/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mcfullgenerictypes.MCFullGenericTypesMill;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.Filter;
import umlp.generator.domain.decorator.commands.GetFreeGemIdRequestDecorator;
import umlp.generator.test_infrastructure.domain.decorator.MonitorDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GemListAssocDecorator.getName;

public class ManagerClientImplDecorator extends AbstractDecorator {

  public static final String SUFFIX = ManagerDecorator.SUFFIX + "ClientImpl";

  public ManagerClientImplDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    astcdPackage.addCDElement(withOrigin(createClass(origCD, astcdPackage.getName())));
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, String astcdPackage) {
    String name = origCD.getCDDefinition().getName();

    ASTCDClass managerClass = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(name + SUFFIX)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(name + ManagerDecorator.SUFFIX))
        .build();

    MonitorDecorator.Stereotype.NEEDS_MONITOR.addTo(managerClass);

    List<String> superDelegateManagerNames = new ArrayList<>();
    for (var ex : this.getExtendedDefinitions(origCD)) {
      List<String> exFQN = new ArrayList<>();
      exFQN.add(astcdPackage);
      exFQN.add(ex.getCDDefinition().getName() + "4" + origCD.getCDDefinition().getName() +ManagerDecorator.SUFFIX + "Client" + "Impl");
      superDelegateManagerNames.add(ex.getCDDefinition().getName() + "4" + origCD.getCDDefinition().getName() +ManagerDecorator.SUFFIX + "Client" + "Impl");
      getCD4C().addImport(managerClass, String.join(".", exFQN));
    }

    // methods
    getCD4C().addMethod(managerClass, getInitTemplate(), name, name + SUFFIX, superDelegateManagerNames);
    getCD4C().addMethod(managerClass, getConstructorTemplate(), name + SUFFIX);
    getCD4C().addMethod(managerClass, getDoGetFreeGemIdTemplate());
    getCD4C().addMethod(managerClass, getErrorNotificationScopeTemplate());

    List<ASTCDClass> concreteDomainClasses = getDomainClasses(origCD, Filter.ABSTRACT.or(Filter.DERIVED).or(Filter.SINGLETON).negate());
    getCD4C().addMethod(managerClass, getClearCacheTemplate(), concreteDomainClasses);


    origCD.getCDDefinition().getCDClassesList()
        .forEach(c -> addClassElements(origCD, c, managerClass, name));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> addInterfaceElements(origCD, i, managerClass));

    // rekeying the values of the map roles locally in the maps of the loaded owners
    GemMapKeyRole.declaredIn(origCD).forEach(r -> getCD4C().addMethod(managerClass, getDoRekeyTemplate(),
        r.getRekeyName(), r.getRole(), r.getKeyType(), r.getOwners()));

    // Imports
    addImports(origCD, managerClass);
    return managerClass;
  }

  protected String getClearCacheTemplate() {
    return "umlp.managerclientimpl.ClearCache";
  }

  protected String getInitTemplate() {
    return "umlp.managerclientimpl.Init";
  }

  protected String getErrorNotificationScopeTemplate() {
    return "umlp.managerclientimpl.ErrorNotificationScope";
  }

  protected String getDoGetFreeGemIdTemplate() {
    return "umlp.managerclientimpl.DoGetFreeGemId";
  }

  protected String getConstructorTemplate() {
    return "umlp.managerclientimpl.Constructor";
  }


  protected void addClassElements(ASTCDCompilationUnit origCD, ASTCDClass domainClass,
                                  ASTCDClass managerClass, String managerName) {
    String name = domainClass.getName();

    if (domainClass.getModifier().isDerived()) {
      getCD4C().addMethod(managerClass, "umlp.managerclientimpl.GetDerived", name);
      return;
    }

    boolean isSingleton = TagLookUp.isSingleton(domainClass);

    if (!domainClass.getModifier().isAbstract()) {
      if (isSingleton) {
        managerClass.addCDMember(getSingletonAttribute(domainClass));
      }else {
        managerClass.addCDMember(getCacheAttribute(domainClass));
        managerClass.addCDMember(getLoadedAttribute(domainClass));
      }
      managerClass.addCDMember(getCommandObserverAttribute(domainClass));
      managerClass.addAllCDMembers(getKeyObserverAttributes(origCD, managerClass, domainClass));
    }
    List<String> keyObservers = GemMapKeyRole.keyObserverAttributes(origCD, domainClass);

    List<ASTCDType> concreteSubclassesOrderedByDistance = concreteSubclassesOrderedByDistance(origCD, domainClass);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetSure", name);
    ASTCDMethodSignature m = getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoGetList", name, isSingleton, domainClass.getModifier().isAbstract());
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint("@umlp.jsweet.extension.annotation.Synchronized"));
    ASTCDMethodSignature mf = getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoGetFilteredList", name, isSingleton, domainClass.getModifier().isAbstract());
    glex.replaceTemplate("cd2java.Annotations", mf, new StringHookPoint("@umlp.jsweet.extension.annotation.Synchronized"));

    if (isSingleton) {
      getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoOptSingletonElement", name);
      getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoGetSingletonElement", name, keyObservers);
    } else {
      getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoOpt", name, concreteSubclassesOrderedByDistance);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.IsLoaded", name, concreteSubclassesOrderedByDistance);
      if (!domainClass.getModifier().isAbstract()) {
        // Implements the abstract _doRegister<X> manager operation; objects
        // dispatch themselves here via _registerMe() (monticore#5062)
        getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoRegister", name, keyObservers);
      }
    }
    getCD4C().addMethod(managerClass, "umlp.managerclientimpl.LoadAllWithIDs", name, getPackageName(origCD, DEFAULT_PACKAGE), isSingleton, domainClass.getModifier().isAbstract());

    if (!domainClass.getModifier().isAbstract()) {
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoBuilderParam", name);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoDeSerBuilder", name);
      if (!isSingleton) {
        getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoAdd", name, managerName, keyObservers);
        getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoDelete", name, managerName);
      }
    }

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), domainClass.getName() + "GemList"));
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainClass.getName() + "_get"));
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainClass.getName() + "_get_filtered"));
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), "LoadAll" + name + "WithIDs"));

    if (!domainClass.getModifier().isAbstract()) {
      getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, CLIENT_PACKAGE), domainClass.getName() + "CommandObserver"));
      getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainClass.getName() + "_create"));
      getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainClass.getName() + "_delete"));
    }

    // Associations
    CDSymbolTables.getAssociations(domainClass).stream()
        .filter(GenService::isList)
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> {
          SymTypeExpression type = a.getSymbol().getType();
          String printedType = type.print();
          ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

          getCD4C().addMethod(managerClass, "umlp.managerimpl.DoCreateAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addMethod(managerClass, "umlp.managerimpl.DoBuilderAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), getName(domainClass, a)));
        });
  }

  protected static List<ASTCDType> concreteSubclassesOrderedByDistance(ASTCDCompilationUnit origCD, ASTCDType domainType) {
    return UMLPSymbolTables.collectConcreteSubclassesInclSelfWithDist(origCD, domainType)
        .entrySet().stream()
        .sorted((o1, o2) -> o2.getValue() - o1.getValue())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  protected void addInterfaceElements(ASTCDCompilationUnit origCD, ASTCDInterface domainInterface,
                                      ASTCDClass managerClass) {
    String name = domainInterface.getName();

    List<ASTCDType> concreteSubclassesOrderedByDistance = concreteSubclassesOrderedByDistance(origCD, domainInterface);
    getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoOpt", name, concreteSubclassesOrderedByDistance);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetSure", name);
    getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoGetList", name, true, true);
    getCD4C().addMethod(managerClass, "umlp.managerclientimpl.DoGetFilteredList", name, true, true);
    getCD4C().addMethod(managerClass, "umlp.managerclientimpl.LoadAllWithIDs", name, getPackageName(origCD, DEFAULT_PACKAGE), true, true);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.IsLoaded", name, concreteSubclassesOrderedByDistance);
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainInterface.getName() + "_get"));
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), domainInterface.getName() + "_get_filtered"));
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), "LoadAll" + name + "WithIDs"));

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), name + "GemList"));

    // Associations
    CDSymbolTables.getAssociations(domainInterface).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .filter(GenService::isList)
        .forEach(a -> {
          SymTypeExpression type = a.getSymbol().getType();
          String printedType = type.print();
          ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

          getCD4C().addMethod(managerClass, "umlp.managerimpl.DoCreateAssoc", getAssocName(domainInterface, a), printedType);
          getCD4C().addMethod(managerClass, "umlp.managerimpl.DoBuilderAssoc", getAssocName(domainInterface, a), printedType);
          getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), getName(domainInterface, a)));
        });
  }

  protected String getDoRekeyTemplate() {
    return "umlp.managerclientimpl.DoRekey";
  }

  protected ASTCDAttribute getCacheAttribute(ASTCDType domainClass) {
    ASTCDAttribute domainClassDaoAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createBasicGenericTypeOf("Map", "Long", domainClass.getName()),
        StringTransformations.uncapitalize("known" + domainClass.getName() + "s"));
    getGlex().replaceTemplate("cd2java.Value", domainClassDaoAttr, new StringHookPoint(defaultMapValue()));
    return domainClassDaoAttr;
  }

  protected ASTCDAttribute getLoadedAttribute(ASTCDType domainClass) {
    ASTCDAttribute attr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createBasicGenericTypeOf("Optional",
            MCFullGenericTypesMill.mCCustomTypeArgumentBuilder().setMCType(
                getMCTypeFacade().createBasicGenericTypeOf("GemList", domainClass.getName())
            ).build()
        ),
        StringTransformations.uncapitalize("loaded" + domainClass.getName() + "List"));
    getGlex().replaceTemplate("cd2java.Value", attr, new StringHookPoint(" = Optional.empty()"));
    return attr;
  }

  protected String defaultMapValue() {
    return " = new HashMap<>()";
  }

  protected ASTCDAttribute getCommandObserverAttribute(ASTCDClass domainClass) {
    String type = domainClass.getName() + "CommandObserver";
    ASTCDAttribute domainClassDaoAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createQualifiedType(type), "the" + type);
    getGlex().replaceTemplate("cd2java.Value", domainClassDaoAttr, new StringHookPoint(" = new " + type + "()"));
    return domainClassDaoAttr;
  }

  /**
   * the key observers of the map roles that may hold objects of domainClass, one per role (see GemMapKeyRole)
   */
  protected List<ASTCDAttribute> getKeyObserverAttributes(ASTCDCompilationUnit origCD, ASTCDClass managerClass,
                                                          ASTCDClass domainClass) {
    List<ASTCDAttribute> result = new ArrayList<>();
    for (GemMapKeyRole role : GemMapKeyRole.observing(origCD, domainClass)) {
      String type = role.getKeyObserverClass(domainClass.getName());
      ASTCDAttribute attr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
          getMCTypeFacade().createQualifiedType(type), role.getKeyObserverAttribute(domainClass.getName()));
      getGlex().replaceTemplate("cd2java.Value", attr, new StringHookPoint(" = new " + type + "()"));
      getCD4C().addImport(managerClass, getPackageName(origCD, GEMMAP_PACKAGE) + "." + type);
      result.add(attr);
    }
    return result;
  }

  protected ASTCDAttribute getSingletonAttribute(ASTCDType domainClass) {
    ASTCDAttribute singletonAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createOptionalTypeOf(domainClass.getName()),
        StringTransformations.uncapitalize(domainClass.getName() + "Instance"));
    getGlex().replaceTemplate("cd2java.Value", singletonAttr, new StringHookPoint(defaultEmptyOptional()));
    return singletonAttr;
  }

  protected String defaultEmptyOptional() {
    return " = Optional.empty()";
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDClass clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("CommandManager"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("SimpleResult"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("ListResult"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("CommandResult"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(clazz, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), GetFreeGemIdRequestDecorator.CLASS_NAME));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemList"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemListBuilder"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTEUtilPath("LogUtil"));
    getCD4C().addImport(clazz, "java.util.HashMap");
    getCD4C().addImport(clazz, "java.util.Map");
  }

}
