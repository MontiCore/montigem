/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.decorator.commands.EventSendingObserverDecorator;
import umlp.generator.test_infrastructure.domain.decorator.MonitorDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GemListAssocDecorator.getName;

public class ManagerImplDecorator extends AbstractDecorator {

  public static final String SUFFIX = ManagerDecorator.SUFFIX + "Impl";

  public ManagerImplDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  public final void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
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
    MonitorDecorator.Stereotype.USE_MONITOR_BUILDERS.addTo(managerClass);
    MonitorDecorator.Stereotype.MANAGER.addTo(managerClass);

    // Add @jsweet.lang.Erased annotation
    glex.replaceTemplate("cd2java.Annotations", managerClass,
        new StringHookPoint("@jsweet.lang.Erased"));

    // Build manager adaper/delegators
    List<String> superDelegateManagerNames = new ArrayList<>();
    for (var ex : this.getExtendedDefinitions(origCD)) {
      List<String> exFQN = new ArrayList<>();
      exFQN.add(astcdPackage);
      exFQN.add(ex.getCDDefinition().getName() + "4" + origCD.getCDDefinition().getName() +ManagerDecorator.SUFFIX + "Impl");
      String superManager = String.join(".", exFQN);
      superDelegateManagerNames.add(superManager);
      getCD4C().addImport(managerClass, superManager);
    }

    // methods
    getCD4C().addMethod(managerClass, "umlp.managerimpl.Constructor", name + SUFFIX, name);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.Init", name, name + ManagerDecorator.SUFFIX + "Impl", superDelegateManagerNames);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.InitNeo4J", name, name + ManagerDecorator.SUFFIX + "Impl", superDelegateManagerNames);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetFreeGemId");
    getCD4C().addMethod(managerClass, "umlp.managerimpl.ErrorNotificationScope");

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), name + EventSendingObserverDecorator.SUFFIX));

    List<ASTCDClass> concreteDomainClasses = getConcreteDomainClasses(origCD)
        .stream()
        .filter(c -> !TagLookUp.isSingleton(c))
        .collect(Collectors.toList());
    getCD4C().addMethod(managerClass, "umlp.managerimpl.ClearCache", concreteDomainClasses);

    origCD.getCDDefinition().getCDClassesList()
        .forEach(c -> addClassElements(origCD, c, managerClass, name));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> addInterfaceElements(origCD, i, managerClass));

    // rekeying the values of the map roles in the maps of the loaded owners
    GemMapKeyRole.declaredIn(origCD).forEach(r -> getCD4C().addMethod(managerClass, "umlp.managerimpl.DoRekey",
        r.getRekeyName(), r.getRole(), r.getKeyType(), r.getOwners()));

    // Imports
    addImports(managerClass);
    return managerClass;
  }

  /**
   * Gets the classes for which a manager will provide _doFooBuilderFromId methods.
   *
   * @param origCD The origCD for which the manager is made
   * @return The classes of the provided builders
   */
  public static Stream<ASTCDClass> classesOfProvidedBuilders(ASTCDCompilationUnit origCD) {
    return origCD.getCDDefinition().getCDClassesList().stream().filter(clazz -> {
      var modifier = clazz.getModifier();
      return !modifier.isDerived() && !modifier.isAbstract();
    });
  }

  protected void addClassElements(ASTCDCompilationUnit origCD, ASTCDClass domainClass,
                                  ASTCDClass managerClass, String managerName) {
    String name = domainClass.getName();
    if (domainClass.getModifier().isDerived()) {
      getCD4C().addMethod(managerClass, "umlp.managerimpl.GetDerived", name);
      return;
    }
    boolean isSingleton = TagLookUp.isSingleton(domainClass);
    if (!domainClass.getModifier().isAbstract()) {
      managerClass.addCDMember(getDBObserverAttribute(domainClass));
      managerClass.addCDMember(getCommandSendingObserverAttribute(origCD, managerClass, domainClass));
      managerClass.addAllCDMembers(getKeyObserverAttributes(origCD, managerClass, domainClass));
      if (isSingleton) {
        managerClass.addCDMember(getSingletonAttribute(domainClass));
      } else {
        managerClass.addCDMember(getCacheAttribute(domainClass));
      }
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoBuilderParam", name);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoDeSerBuilder", name);
      if (!isSingleton) {
        List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
            .filter(GenService::isBidirectional)
            .filter(Predicate.not(GenService::isSymbolDerived))
            .collect(Collectors.toList());
        Map<String, String> sideToCorrectManager = new LinkedHashMap<>();
        assocs.forEach(a -> {
          ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) a.getSymbol().getType()
                  .getTypeInfo().getAstNode());
          String printedType = a.getSymbol().getType().print();
          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

          String correctManager = getCorrectManager(a.getSymbol().getAssoc().getAssociation().getAstNode());
          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." +correctManager);

          sideToCorrectManager.put(a.getName(), correctManager);
        });
        getCD4C().addMethod(managerClass, "umlp.managerimpl.DoAdd", name, managerName, assocs, sideToCorrectManager);
        getCD4C().addMethod(managerClass, "umlp.managerimpl.DoDelete",origCD.getCDDefinition().getName(), name, managerName,
            GemMapOwnerRole.holding(origCD, domainClass), GemMapOwnerRole.keyedBy(origCD, domainClass),
            GenService.getBidirectionalMaps(domainClass).stream().map(ASTCDAssocSide::getName).collect(Collectors.toList()),
            GemMapOwnerRole.typeQualifiedKeyedBy(origCD, domainClass).stream()
                .filter(assoc -> GenService.getTypeQualifiedMapSide(assoc).flatMap(GenService::getOtherSideOfMap)
                    .isPresent())
                .collect(Collectors.toList()));
        getCD4C().addMethod(managerClass, "umlp.managerimpl.DoRegister", name);
      }
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoAddRequiredObserver", origCD.getCDDefinition().getName(), name,
          GemMapKeyRole.keyObserverAttributes(origCD, domainClass));
    }
    if (isSingleton) {
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetSingletonElement", origCD.getCDDefinition().getName(), name, managerName);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoOptSingletonElement", name);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetListSingletonElement", name);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetFilteredListSingletonElement", name);
    }else {
      Set<ASTCDType> collectConcreteSubclassesInclSelf = UMLPSymbolTables.collectConcreteSubclassesInclSelf(origCD, domainClass);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoOpt", origCD.getCDDefinition().getName(), name, collectConcreteSubclassesInclSelf);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetSure", name);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetList", origCD.getCDDefinition().getName(), name, collectConcreteSubclassesInclSelf);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetFilteredList", origCD.getCDDefinition().getName(), name, collectConcreteSubclassesInclSelf);
      getCD4C().addMethod(managerClass, "umlp.managerimpl.IsLoaded", name, collectConcreteSubclassesInclSelf);
    }
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoLoadAllWithIDs", name);

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), domainClass.getName() + "GemList"));
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

  protected void addInterfaceElements(ASTCDCompilationUnit origCD, ASTCDInterface domainInterface,
                                      ASTCDClass managerClass) {
    String name = domainInterface.getName();

    Set<ASTCDType> concreteSubclassesInclSelf = UMLPSymbolTables.collectConcreteSubclassesInclSelf(origCD, domainInterface);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoOpt", origCD.getCDDefinition().getName(),name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetSure", name);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetList", origCD.getCDDefinition().getName(), name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoGetFilteredList", origCD.getCDDefinition().getName(), name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.IsLoaded", name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managerimpl.DoLoadAllWithIDs", name);

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), name + "GemList"));

    // Associations
    CDSymbolTables.getAssociations(domainInterface).stream()
        .filter(GenService::isList)
        .filter(Predicate.not(GenService::isSymbolDerived))
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

  protected ASTCDAttribute getCacheAttribute(ASTCDType domainClass) {
    ASTCDAttribute domainClassDaoAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createBasicGenericTypeOf("Map", "Long", domainClass.getName()),
        StringTransformations.uncapitalize("known" + domainClass.getName() + "s"));
    getGlex().replaceTemplate("cd2java.Value", domainClassDaoAttr, new StringHookPoint(" = new SoftValueHashMap<>()"));
    return domainClassDaoAttr;
  }

  protected ASTCDAttribute getDBObserverAttribute(ASTCDClass domainClass) {
    String type = domainClass.getName() + "DbObserver";
    ASTCDAttribute domainClassDaoAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createQualifiedType(type), "the" + type);
    getGlex().replaceTemplate("cd2java.Value", domainClassDaoAttr, new StringHookPoint(" = new " + type + "()"));
    return domainClassDaoAttr;
  }

  protected ASTCDAttribute getCommandSendingObserverAttribute(ASTCDCompilationUnit origCD, ASTCDClass managerClass, ASTCDClass domainClass) {
    String type = domainClass.getName() + "CommandSendingObserver";
    ASTCDAttribute domainClassDaoAttr = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getMCTypeFacade().createQualifiedType(type), "the" + type);
    getGlex().replaceTemplate("cd2java.Value", domainClassDaoAttr, new StringHookPoint(" = new " + type + "()"));
    getCD4C().addImport(managerClass, getPackageName(origCD, CLIENT_PACKAGE) +"." + type);
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
        getMCTypeFacade().createQualifiedType( domainClass.getName()),
        StringTransformations.uncapitalize(domainClass.getName() + "Instance"));
    return singletonAttr;
  }

  protected void addImports(ASTCDClass clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("DatabaseAccessManager"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemList"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemListBuilder"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("SoftValueHashMap"));
    getCD4C().addImport(clazz, "java.util.HashMap");
    getCD4C().addImport(clazz, "java.util.Map");
  }

}
