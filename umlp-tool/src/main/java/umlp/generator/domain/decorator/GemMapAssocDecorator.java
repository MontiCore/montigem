/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import de.monticore.types.check.SymTypeExpression;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.ManagerDecorator.SUFFIX;

/**
 * Generates the GemMap classes of qualified associations, see {@link GenService#isMap}: a GemValueMapBuilder keyed
 * by values, or a GemEntryMapBuilder keyed by domain objects, see {@link QualifierKey}. Attribute qualified
 * associations ({@code Owner [[attr]] -> Target}) derive the key of a value from attr and get key observers that
 * rekey the values; type qualified associations ({@code Owner [Type] -> Target}) take the key on put.
 */
public class GemMapAssocDecorator extends AbstractDecorator {

  public GemMapAssocDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, GEMMAP_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> CDSymbolTables.getAssociations(c)
        .stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> astcdPackage.addAllCDElements(withOrigin(createClasses(origCD, a, c)))));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> CDSymbolTables.getAssociations(i)
        .stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> astcdPackage.addAllCDElements(withOrigin(createClasses(origCD, a, i)))));
  }

  protected Collection<ASTCDClass> createClasses(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType clazz) {
    List<ASTCDClass> retList = Lists.newArrayList();
    if (GenService.isMap(assoc)) {
      retList.add(createGemMapClass(origCD, assoc, clazz));
    }
    if (GenService.isMap(assoc) && GenService.isKeyDerived(assoc)) {
      // one key observer per type of values, as subclasses notify their own observers only;
      // the managers attach them to the values, see GemMapKeyRole
      CDTypeSymbol targetSymbol = (CDTypeSymbol) UMLPSymbolTables.getTypeSymbol(assoc.getMCQualifiedType()).get();
      for (CDTypeSymbol observedType : getObservedTypes(origCD, targetSymbol)) {
        retList.add(createKeyObserverClass(origCD, assoc, clazz, targetSymbol, observedType));
      }
    }
    return retList;
  }

  protected ASTCDClass createGemMapClass(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType domainClass) {
    String className = getName(domainClass, assoc);
    CDTypeSymbol targetSymbol = (CDTypeSymbol) UMLPSymbolTables.getTypeSymbol(assoc.getMCQualifiedType()).get();
    String targetName = targetSymbol.getName();
    QualifierKey key = QualifierKey.of(assoc);

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(className)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(
            key.getMapType() + "Builder<" + key.getKeyType() + ", " + targetName + ">"))
        .build();

    addImports(origCD, targetSymbol, key, ast);

    // attributes and getter
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected long ownerGemID");
    ast.addAllCDMembers(accessor.decorate(attr));

    // constructor
    getCD4C().addConstructor(ast, "umlp.gemmap.Constructor", className);

    // methods
    getCD4C().addMethod(ast, "umlp.gemmap.LoadObjects", "loadValueObjects", targetName,
        getCorrectManager(targetSymbol.getAstNode()));
    if (key.isEntryKey()) {
      CDTypeSymbol keySymbol = key.getKeySymbol();
      getCD4C().addMethod(ast, "umlp.gemmap.LoadObjects", "loadKeyObjects", keySymbol.getName(),
          getCorrectManager(keySymbol.getAstNode()));
    }
    // modifications through the map itself are synchronized via the owner, like GemList
    String managerName = origCD.getCDDefinition().getName() + ManagerDecorator.SUFFIX;
    getCD4C().addMethod(ast, "umlp.gemlist.assocs.GetOwner", domainClass.getName(), managerName);
    if (key.isDerived()) {
      getCD4C().addMethod(ast, "umlp.gemmap.DeriveKey", key.getKeyType(), targetName,
          key.getKeyExpression("value"), key.isEntryKey());
      getCD4C().addMethod(ast, "umlp.gemmap.IsKeyDerived");
      getCD4C().addMethod(ast, "umlp.gemmap.PutValueAndSync", targetName, assoc.getName());
    } else {
      // the key is chosen on put (RTE: not derived by default)
      getCD4C().addMethod(ast, "umlp.gemmap.PutAndSync", key.getKeyType(), targetName, assoc.getName());
    }
    getCD4C().addMethod(ast, "umlp.gemmap.RemoveValueIDAndSync", assoc.getName());
    getCD4C().addMethod(ast, "umlp.gemmap.RemoveAndSync", key.getKeyType(), key.isEntryKey());
    addDomainImports(ast, domainClass);

    return ast;
  }

  /**
   * The observer that rekeys the values of type observedType of a GemMap role when their qualifying attribute
   * changes: the manager of observedType attaches it to every object it takes up or loads (like its other observers),
   * the manager of the owner rekeys the maps of the loaded owners, see {@link GemMapKeyRole}.
   * A class of its own, like the other generated observers, so that it can be extended by hand-written code (TOP
   * mechanism).
   */
  protected ASTCDClass createKeyObserverClass(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType domainClass,
                                              CDTypeSymbol targetSymbol, CDTypeSymbol observedType) {
    QualifierKey key = QualifierKey.of(assoc);
    String className = getKeyObserverName(domainClass, assoc, observedType.getName());

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(className)
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage(observedType.getName() + "Observer"))
        .build();

    addDomainImports(ast, targetSymbol.getAstNode());
    addDomainImports(ast, observedType.getAstNode());
    addDomainImports(ast, key.getDeclaringType().getAstNode());
    if (key.isEntryKey()) {
      addDomainImports(ast, key.getKeySymbol().getAstNode());
    }
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(UMLPSymbolTables.getDiagramFor(observedType.getAstNode()), DEFAULT_PACKAGE),
        observedType.getName() + "Observer"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    // the manager of the owner's class diagram rekeys the maps of its loaded owners
    String managerName = origCD.getCDDefinition().getName() + ManagerDecorator.SUFFIX;
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), managerName));

    // rekeys locally and on changes by other participants
    ASTCDMethodSignature getScope = getCD4C().addMethod(ast, "umlp.common_observer.GetScopeLocal");
    glex.replaceTemplate("cd2java.Annotations", getScope, new StringHookPoint("@Override"));
    ASTCDMethodSignature notify = getCD4C().addMethod(ast, "umlp.gemmap.KeyObserverNotify",
        key.getDeclaringType().getName(), key.getAttrName(), key.getNotifyType(), managerName,
        domainClass.getName() + StringTransformations.capitalize(assoc.getName()));
    glex.replaceTemplate("cd2java.Annotations", notify, new StringHookPoint("@Override"));
    return ast;
  }

  /**
   * @return the target and its subtypes known in the class diagram, most specific first
   */
  protected List<CDTypeSymbol> getObservedTypes(ASTCDCompilationUnit origCD, CDTypeSymbol targetSymbol) {
    List<CDTypeSymbol> types = Lists.newArrayList(targetSymbol);
    getNonDerivedDomainClasses(origCD).stream().map(ASTCDType::getSymbol)
        .filter(t -> depthBelow(t, targetSymbol) > 0)
        .forEach(types::add);
    types.sort(Comparator.comparingInt((CDTypeSymbol t) -> depthBelow(t, targetSymbol)).reversed());
    return types;
  }

  /**
   * @return the number of inheritance steps from type up to superType, 0 if equal, -1 if not a subtype
   */
  protected int depthBelow(CDTypeSymbol type, CDTypeSymbol superType) {
    if (type.getFullName().equals(superType.getFullName())) {
      return 0;
    }
    int best = -1;
    for (SymTypeExpression direct : type.getSuperTypesList()) {
      if (direct.getTypeInfo() instanceof CDTypeSymbol) {
        int depth = depthBelow((CDTypeSymbol) direct.getTypeInfo(), superType);
        if (depth >= 0 && (best < 0 || depth + 1 < best)) {
          best = depth + 1;
        }
      }
    }
    return best;
  }

  protected void addImports(ASTCDCompilationUnit origCD, CDTypeSymbol targetSymbol, QualifierKey key, ASTCDClass ast) {
    addDomainImports(ast, targetSymbol.getAstNode());
    if (key.isDerived()) {
      addDomainImports(ast, key.getDeclaringType().getAstNode());
    }
    if (key.isEntryKey()) {
      addDomainImports(ast, key.getKeySymbol().getAstNode());
    }
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath(key.getMapType() + "Builder"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("LoadResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("LoadingState"));
    getCD4C().addImport(ast, "java.util.HashMap");
    getCD4C().addImport(ast, "java.util.Map");
  }

  /**
   * imports a domain type and the manager of its class diagram
   */
  protected void addDomainImports(ASTCDClass ast, ASTCDType type) {
    ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor(type);
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(cd, DEFAULT_PACKAGE), type.getName()));
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(cd, DEFAULT_PACKAGE), cd.getCDDefinition().getName() + SUFFIX));
  }

  public static String getName(ASTCDType type, ASTCDAssocSide assoc) {
    return type.getName()
        + "_"
        + StringTransformations.capitalize(assoc.getName())
        + "GemMap";
  }

  /**
   * e.g. Faculty_CourseByTitleCourseKeyObserver: the key observer of the GemMap Faculty_CourseByTitleGemMap
   * for values of type Course
   */
  public static String getKeyObserverName(ASTCDType type, ASTCDAssocSide assoc, String observedType) {
    return getKeyObserverName(type.getName(), assoc.getName(), observedType);
  }

  public static String getKeyObserverName(String declaringType, String role, String observedType) {
    return declaringType + "_" + StringTransformations.capitalize(role) + observedType + "KeyObserver";
  }
}
