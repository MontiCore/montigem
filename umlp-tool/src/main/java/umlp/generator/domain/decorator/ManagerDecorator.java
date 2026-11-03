/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import com.google.common.collect.Sets;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;
import umlp.generator.util.UMLPSymbolTables;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

import static de.monticore.cd.facade.CDModifier.PROTECTED;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;

public class ManagerDecorator extends AbstractDecorator {

  public static final String SUFFIX = "Manager";

  public ManagerDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    astcdPackage.addCDElement(withOrigin(createClass(origCD)));
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD) {
    String name = origCD.getCDDefinition().getName();

    ASTCDClass managerClass = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC_ABSTRACT.build())
        .setName(getClassName(name)).build();

    // Attribute observerList
    ASTCDAttribute attrObserver = getCDAttributeFacade().createAttribute(PROTECTED.build(),
        getObserversType(name), StringTransformations.uncapitalize(name) + "Observers");
    glex.replaceTemplate("cd2java.Value", attrObserver, new StringHookPoint(defaultListValue()));
    managerClass.addCDMember(attrObserver);

    addSingletonElements(name, managerClass);
    getCD4C().addMethod(managerClass, getErrorNotificationScopeTemplate());

    origCD.getCDDefinition().getCDClassesList()
        .forEach(c -> addClassElements(c, managerClass));

    origCD.getCDDefinition().getCDInterfacesList()
        .forEach(i -> addInterfaceElements(i, managerClass));

    // the key observers of the map roles rekey through the manager, see GemMapKeyRole
    GemMapKeyRole.declaredIn(origCD).forEach(r -> {
      getCD4C().addMethod(managerClass, "umlp.manager.Rekey", r.getRekeyName(), r.getKeyType(), true);
      getCD4C().addMethod(managerClass, "umlp.manager.Rekey", r.getRekeyName(), r.getKeyType(), false);
    });

    addImports(managerClass);

    return managerClass;
  }

  public static String getClassName(String name) {
    return name + SUFFIX;
  }

  protected String getObserversType(String name) {
    return "List<" + name + "Observer>";
  }

  protected String defaultListValue() {
    return "= new ArrayList<>()";
  }

  protected String getErrorNotificationScopeTemplate() {
    return "umlp.manager.ErrorNotificationScope";
  }

  protected void addSingletonElements(String simpleName, ASTCDClass managerClass) {
    String name = managerClass.getName();
    // add attribute
    getCD4C().addAttribute(managerClass, "protected static " + name + " instance");

    // add methods
    getCD4C().addMethod(managerClass, "umlp.manager.InitMe", name);
    getCD4C().addMethod(managerClass, "umlp.manager.GetSingleton", name);
    getCD4C().addMethod(managerClass, "umlp.manager.SetInstance", simpleName);

    getCD4C().addMethod(managerClass, "umlp.manager.GetFreeGemId", true);
    getCD4C().addMethod(managerClass, "umlp.manager.GetFreeGemId", false);

    getCD4C().addMethod(managerClass, "umlp.manager.ClearCache", true);
    getCD4C().addMethod(managerClass, "umlp.manager.ClearCache", false);

    getCD4C().addMethod(managerClass, "umlp.manager.AddManagerObserver", simpleName, true);
    getCD4C().addMethod(managerClass, "umlp.manager.AddManagerObserver", simpleName, false);
    getCD4C().addMethod(managerClass, "umlp.manager.RemoveManagerObserver", simpleName, true);
    getCD4C().addMethod(managerClass, "umlp.manager.RemoveManagerObserver", simpleName, false);
  }

  protected void addClassElements(ASTCDClass domainClass, ASTCDClass managerClass) {
    String name = domainClass.getName();

    ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor(domainClass);
    getCD4C().addImport(managerClass, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + domainClass.getName());

    if (domainClass.getModifier().isDerived()) {
      getCD4C().addMethod(managerClass, "umlp.manager.GetDerived", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetDerived", name, false);
    } else {
      boolean isSingleton = TagLookUp.isSingleton(domainClass);
      if (isSingleton) {
        getCD4C().addMethod(managerClass, "umlp.manager.GetSingletonElement", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.GetSingletonElement", name, false);

        // also generate some of the methods
        getCD4C().addMethod(managerClass, "umlp.manager.GetSingletonElementSure", name);
      }else {
        getCD4C().addMethod(managerClass, "umlp.manager.CreateGem", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.CreateGem", name, false);
        getCD4C().addMethod(managerClass, "umlp.manager.GetSure", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.GetSure", name, false);
        getCD4C().addMethod(managerClass, "umlp.manager.IsLoaded", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.IsLoaded", name, false);
      }
      if (!isSingleton && !domainClass.getModifier().isAbstract()) {
        // Registration as a first-class manager operation; the object dispatches
        // itself here via _registerMe() (monticore#5062)
        getCD4C().addMethod(managerClass, "umlp.manager.Register", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.Register", name, false);
      }
      getCD4C().addMethod(managerClass, "umlp.manager.Opt", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.Opt", name, false);

      getCD4C().addMethod(managerClass, "umlp.manager.GetList", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetList", name, false);

      getCD4C().addMethod(managerClass, "umlp.manager.GetFilteredList", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetFilteredList", name, false);

      getCD4C().addMethod(managerClass, "umlp.manager.LoadAllWithIDs", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.LoadAllWithIDs", name, false);


      if (!domainClass.getModifier().isAbstract()) {
        getCD4C().addMethod(managerClass, "umlp.manager.BuilderParam", name, true);
        getCD4C().addMethod(managerClass, "umlp.manager.BuilderParam", name, false);
        if (isSingleton) {
          // methods that throw errors when invoked
          getCD4C().addMethod(managerClass, "umlp.manager.AddWithScopeSingletonElement", name);
          getCD4C().addMethod(managerClass, "umlp.manager.DeleteWithScopeSingletonElement", name);
        } else {
          getCD4C().addMethod(managerClass, "umlp.manager.Builder", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.Builder", name, false);

          getCD4C().addMethod(managerClass, "umlp.manager.DeSerBuilder", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.DeSerBuilder", name, false);
          getCD4C().addMethod(managerClass, "umlp.manager.Delete", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.Delete", name, false);
          getCD4C().addMethod(managerClass, "umlp.manager.DeleteWithScope", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.DeleteWithScope", name, false);
          getCD4C().addMethod(managerClass, "umlp.manager.Add", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.Add", name, false);
          getCD4C().addMethod(managerClass, "umlp.manager.AddWithScope", name, true);
          getCD4C().addMethod(managerClass, "umlp.manager.AddWithScope", name, false);
        }
      }

      // Associations
      Collection<String> types = new LinkedHashSet<>();
      CDSymbolTables.getAssociations(domainClass).stream()
          .filter(a -> !types.contains(a.getName()))
          .filter(Predicate.not(GenService::isSymbolDerived))
          .filter(GenService::isList)
          .forEach(a -> {
            types.add(a.getName());
            SymTypeExpression type = a.getSymbol().getType();
            String printedType = type.print();
            ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

            getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);

            getCD4C().addMethod(managerClass, "umlp.manager.CreateAssoc", getAssocName(domainClass, a), printedType, true);
            getCD4C().addMethod(managerClass, "umlp.manager.CreateAssoc", getAssocName(domainClass, a), printedType, false);
            getCD4C().addMethod(managerClass, "umlp.manager.BuilderAssoc", getAssocName(domainClass, a), printedType, true);
            getCD4C().addMethod(managerClass, "umlp.manager.BuilderAssoc", getAssocName(domainClass, a), printedType, false);
          });
    }

  }

  protected void addInterfaceElements(ASTCDInterface domainInterface, ASTCDClass managerClass) {
    String name = domainInterface.getName();

    ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor(domainInterface);
    getCD4C().addImport(managerClass, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + domainInterface.getName());

    if (domainInterface.getModifier().isDerived()) {
      getCD4C().addMethod(managerClass, "umlp.manager.GetDerived", name);
    } else {
      getCD4C().addMethod(managerClass, "umlp.manager.CreateGem", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.CreateGem", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.Opt", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.Opt", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.GetSure", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetSure", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.GetList", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetList", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.GetFilteredList", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.GetFilteredList", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.LoadAllWithIDs", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.LoadAllWithIDs", name, false);
      getCD4C().addMethod(managerClass, "umlp.manager.IsLoaded", name, true);
      getCD4C().addMethod(managerClass, "umlp.manager.IsLoaded", name, false);

      // Associations
      Collection<String> types = new LinkedHashSet<>();
      CDSymbolTables.getAssociations(domainInterface).stream()
          .filter(GenService::isList)
          .filter(a -> !types.contains(a.getName()))
          .filter(Predicate.not(GenService::isSymbolDerived))
          .forEach(a -> {
            types.add(a.getName());
            SymTypeExpression type = a.getSymbol().getType();
            String printedType = type.print();
            ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());

            getCD4C().addImport(managerClass, getPackageName(cd, DEFAULT_PACKAGE) + "." + printedType);
            getCD4C().addMethod(managerClass, "umlp.manager.CreateAssoc", getAssocName(domainInterface, a), printedType, true);
            getCD4C().addMethod(managerClass, "umlp.manager.CreateAssoc", getAssocName(domainInterface, a), printedType, false);
            getCD4C().addMethod(managerClass, "umlp.manager.BuilderAssoc", getAssocName(domainInterface, a), printedType, true);
            getCD4C().addMethod(managerClass, "umlp.manager.BuilderAssoc", getAssocName(domainInterface, a), printedType, false);
          });
    }
  }

  protected void addImports(ASTCDType clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("LoadResult"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("LoadingState"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("GemList"));
  }
}
