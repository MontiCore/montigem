/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GemListAssocDecorator.getName;

public class ManagerMockDecorator extends AbstractDecorator {

  public static final String SUFFIX = "Mock";

  public ManagerMockDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    astcdPackage.addCDElement(withOrigin(createClass(origCD)));
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD) {
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


    // attributes
    getCD4C().addAttribute(managerClass, "static protected long freeGemId = 1001L");
    getCD4C().addAttribute(managerClass, "protected static " + name + "DummyProvider dummyProvider");

    // methods
    getCD4C().addMethod(managerClass, "umlp.managermock.MockConstructor", name);
    getCD4C().addMethod(managerClass, "umlp.managermock.Init", name);
    getCD4C().addMethod(managerClass, "umlp.managermock.DoGetFreeGemId");
    getCD4C().addMethod(managerClass, "umlp.managermock.ErrorNotificationScope");
    getCD4C().addMethod(managerClass, "umlp.managermock.SecretlyAdaptNextGemId");

    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, COMMAND_PACKAGE), name + EventSendingObserverDecorator.SUFFIX));

    List<ASTCDClass> concreteDomainClasses = getConcreteDomainClasses(origCD);
    Function<ASTCDClass, Boolean> isSingleton = TagLookUp::isSingleton;
    getCD4C().addMethod(managerClass, "umlp.managermock.ClearCache",
            concreteDomainClasses.stream().filter(x -> !isSingleton.apply(x)).collect(Collectors.toList()),
            concreteDomainClasses.stream().filter(isSingleton::apply).collect(Collectors.toList()));


    origCD.getCDDefinition().getCDClassesList()
        .forEach(c -> addClassElements(origCD, c, managerClass, name));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> addInterfaceElements(origCD, i, managerClass));

    // rekeying the values of the map roles in the maps of the owners
    GemMapKeyRole.declaredIn(origCD).forEach(r -> getCD4C().addMethod(managerClass, "umlp.managermock.DoRekey",
        r.getRekeyName(), r.getRole(), r.getKeyType(), r.getOwners()));

    // Imports
    addImports(managerClass);

    return managerClass;
  }

  /**
   * the key observers of the map roles that may hold objects of the concrete class domainClass, one per role (see
   * GemMapKeyRole)
   */
  protected void addKeyObserverAttributes(ASTCDCompilationUnit origCD, ASTCDClass domainClass,
                                          ASTCDClass managerClass) {
    for (GemMapKeyRole role : GemMapKeyRole.observing(origCD, domainClass)) {
      String type = role.getKeyObserverClass(domainClass.getName());
      addAttribute(managerClass, "protected " + type + " " + role.getKeyObserverAttribute(domainClass.getName()),
          "new " + type + "()");
      getCD4C().addImport(managerClass, getPackageName(origCD, GEMMAP_PACKAGE) + "." + type);
    }
  }

  protected void addClassElements(ASTCDCompilationUnit origCD, ASTCDClass domainClass,
                                  ASTCDClass managerClass, String managerName) {
    String name = domainClass.getName();
    if (domainClass.getModifier().isDerived()) {
      // Same as the backend/managerimpl
      getCD4C().addMethod(managerClass, "umlp.managerimpl.GetDerived", name);
      return;
    }
    String lName = StringTransformations.uncapitalize(name);
    boolean isSingleton = TagLookUp.isSingleton(domainClass);

    List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociations(domainClass);
    Set<ASTCDType> concreteSubclassesInclSelf = UMLPSymbolTables.collectConcreteSubclassesInclSelf(origCD, domainClass);
    List<String> keyObservers = GemMapKeyRole.keyObserverAttributes(origCD, domainClass);
    if (isSingleton) {
      getCD4C().addMethod(managerClass, "umlp.managermock.DoOptSingletonElement", name);
    } else {
      getCD4C().addMethod(managerClass, "umlp.managermock.DoOpt", name, concreteSubclassesInclSelf,
          GemMapKeyRole.keyObserverAttributes(origCD, concreteSubclassesInclSelf));
    }
    getCD4C().addMethod(managerClass, "umlp.managermock.DoGetSure", name);
    if (!isSingleton) {
      getCD4C().addMethod(managerClass, "umlp.managermock.IsLoaded", name, concreteSubclassesInclSelf);
    }
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), domainClass.getName() + "GemList"));

    if (isSingleton) {
      getCD4C().addMethod(managerClass, "umlp.managermock.DoGetSingletonElement", name, keyObservers);
    }



    if (!domainClass.getModifier().isAbstract()) {
      if (isSingleton) {
        getCD4C().addMethod(managerClass, "umlp.managermock.DoGetListSingletonElement", name);
        getCD4C().addMethod(managerClass, "umlp.managermock.DoGetFilteredListSingletonElement", name);
      } else {
        getCD4C().addMethod(managerClass, "umlp.managermock.DoGetList", name, concreteSubclassesInclSelf);
        getCD4C().addMethod(managerClass, "umlp.managermock.DoGetFilteredList", name, concreteSubclassesInclSelf);
        getCD4C().addMethod(managerClass, "umlp.managermock.DoRegister", name, keyObservers);
      }
      
      if (isSingleton) {
        addAttribute(managerClass, "protected Optional<" + name + "> " + lName + "Instance", "Optional.empty()");
      }else {
        addAttribute(managerClass,
                     "protected HashMapWithOrderedKeys<Long, " + name + "> " + lName + "s",
                     "new HashMapWithOrderedKeys<>()");
      }
      addAttribute(managerClass, "protected GemList<" + name + "> " + lName + "GemList", "new " + name + "GemList().build().get()");
      addAttribute(managerClass, "protected " + name + "CommandSendingObserver the" + name + "CommandObserver", "new " + name + "CommandSendingObserver()");
      addKeyObserverAttributes(origCD, domainClass, managerClass);

      getCD4C().addMethod(managerClass, "umlp.managermock.DoBuilderParam", name);
      getCD4C().addMethod(managerClass, "umlp.managermock.DoDeSerBuilder", name);
      if (!isSingleton) {
        getCD4C().addMethod(managerClass, "umlp.managermock.DoAdd", name, managerName,
            assocs.stream()
                .filter(GenService::isBidirectional)
                .filter(Predicate.not(GenService::isSymbolDerived))
                .collect(Collectors.toList()),
            keyObservers);
        getCD4C().addMethod(managerClass, "umlp.managermock.DoDelete", name, managerName,
            GemMapOwnerRole.holding(origCD, domainClass), GemMapOwnerRole.keyedBy(origCD, domainClass),
            GenService.getBidirectionalMaps(domainClass).stream().map(ASTCDAssocSide::getName).collect(Collectors.toList()));
      }
      getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, CLIENT_PACKAGE), domainClass.getName() + CommandSendingObserverDecorator.SUFFIX));
    } else {
      getCD4C().addMethod(managerClass, "umlp.managermock.DoGetListAbstract", name);
      getCD4C().addMethod(managerClass, "umlp.managermock.DoGetFilteredListAbstract", name);
    }
    getCD4C().addMethod(managerClass, "umlp.managermock.DoLoadAllWithIDs", name);

    // Associations
    assocs.stream()
        .filter(GenService::isList)
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> {
          SymTypeExpression type = a.getSymbol().getType();
          String printedType = type.print();
          ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

          getCD4C().addMethod(managerClass, "umlp.managermock.DoCreateAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addMethod(managerClass, "umlp.managermock.DoBuilderAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), getName(domainClass, a)));
        });
  }

  protected void addInterfaceElements(ASTCDCompilationUnit origCD, ASTCDInterface domainClass,
                                      ASTCDClass managerClass) {
    String name = domainClass.getName();
    String lName = StringTransformations.uncapitalize(name);

    addAttribute(managerClass, "protected HashMapWithOrderedKeys<Long, " + name + "> " + lName + "s", "new HashMapWithOrderedKeys<>()");
    addAttribute(managerClass, "protected GemList<" + name + "> " + lName + "GemList", "new " + name + "GemList().build().get()");

    List<ASTCDAssocSide> assocs = CDSymbolTables.getAssociations(domainClass);
    Set<ASTCDType> concreteSubclassesInclSelf = UMLPSymbolTables.collectConcreteSubclassesInclSelf(origCD, domainClass);
    getCD4C().addMethod(managerClass, "umlp.managermock.DoOpt", name, concreteSubclassesInclSelf,
        GemMapKeyRole.keyObserverAttributes(origCD, concreteSubclassesInclSelf));
    getCD4C().addMethod(managerClass, "umlp.managermock.DoGetSure", name);
    getCD4C().addMethod(managerClass, "umlp.managermock.DoGetList", name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managermock.DoGetFilteredList", name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managermock.IsLoaded", name, concreteSubclassesInclSelf);
    getCD4C().addMethod(managerClass, "umlp.managermock.DoLoadAllWithIDs", name);

    // Imports
    getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), domainClass.getName() + "GemList"));

    // Associations
    assocs.stream()
        .filter(GenService::isList)
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> {
          SymTypeExpression type = a.getSymbol().getType();
          String printedType = type.print();
          ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

          getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

          getCD4C().addMethod(managerClass, "umlp.managermock.DoCreateAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addMethod(managerClass, "umlp.managermock.DoBuilderAssoc", getAssocName(domainClass, a), printedType);
          getCD4C().addImport(managerClass, Joiners.DOT.join(getPackageName(origCD, GEMLIST_PACKAGE), getName(domainClass, a)));
        });
  }

  protected void addImports(ASTCDClass clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemList"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("HashMapWithOrderedKeys"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("NotificationScope"));
  }
}
