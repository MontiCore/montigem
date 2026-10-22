/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateHookPoint;
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
 * Generates the GemMap classes of attribute qualified associations ({@code Owner [[attr]] -> Target}),
 * see {@link GenService#isMap}: a GemValueMapBuilder keyed by the value of attr, or a GemEntryMapBuilder
 * if attr references a domain object, see {@link QualifierKey}.
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
    return retList;
  }

  protected ASTCDClass createGemMapClass(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType domainClass) {
    String className = getName(domainClass, assoc);
    CDTypeSymbol targetSymbol = (CDTypeSymbol) assoc.getMCQualifiedType().getDefiningSymbol().get();
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

    // one observer per type of values, as subclasses notify their own observers only
    String declaringType = key.getDeclaringType().getName();
    List<CDTypeSymbol> observedTypes = getObservedTypes(origCD, targetSymbol);
    for (CDTypeSymbol observedType : observedTypes) {
      ASTCDAttribute keyObserver = getCD4C().addAttribute(ast,
          "protected " + observedType.getName() + "Observer keyObserver" + observedType.getName());
      glex.replaceTemplate("cd2java.Annotations", keyObserver, new StringHookPoint("transient"));
      glex.replaceTemplate("cd2java.Value", keyObserver, new TemplateHookPoint("umlp.gemmap.KeyObserver",
          className, observedType.getName(), declaringType, key.getAttrName(), key.getNotifyType()));
      addDomainImports(ast, observedType.getAstNode());
      getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(UMLPSymbolTables.getDiagramFor(observedType.getAstNode()), DEFAULT_PACKAGE),
          observedType.getName() + "Observer"));
    }

    // constructor
    getCD4C().addConstructor(ast, "umlp.gemmap.Constructor", className);

    // methods
    getCD4C().addMethod(ast, "umlp.gemmap.LoadObjects", "loadValueObjects", targetName,
        getCorrectManager(targetSymbol.getAstNode()));
    if (key.isEntryKey()) {
      CDTypeSymbol keySymbol = getKeySymbol(targetSymbol, key);
      getCD4C().addMethod(ast, "umlp.gemmap.LoadObjects", "loadKeyObjects", keySymbol.getName(),
          getCorrectManager(keySymbol.getAstNode()));
    }
    getCD4C().addMethod(ast, "umlp.gemmap.DeriveKey", key.getKeyType(), targetName,
        key.getKeyExpression("value"), key.isEntryKey());
    // modifications through the map itself are synchronized via the owner, like GemList
    String managerName = origCD.getCDDefinition().getName() + ManagerDecorator.SUFFIX;
    getCD4C().addMethod(ast, "umlp.gemlist.assocs.GetOwner", domainClass.getName(), managerName);
    getCD4C().addMethod(ast, "umlp.gemmap.IsKeyDerived");
    getCD4C().addMethod(ast, "umlp.gemmap.PutValueAndSync", targetName, assoc.getName());
    getCD4C().addMethod(ast, "umlp.gemmap.RemoveValueIDAndSync", assoc.getName());
    getCD4C().addMethod(ast, "umlp.gemmap.RemoveAndSync", key.getKeyType(), key.isEntryKey());
    addDomainImports(ast, domainClass);

    getCD4C().addMethod(ast, "umlp.gemmap.OnValueKeyed", targetName,
        observedTypes.stream().map(CDTypeSymbol::getName).collect(Collectors.toList()));
    getCD4C().addMethod(ast, "umlp.gemmap.OnValueRemoved", targetName,
        getCorrectManager(targetSymbol.getAstNode()),
        observedTypes.stream().map(CDTypeSymbol::getName).collect(Collectors.toList()));

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

  protected CDTypeSymbol getKeySymbol(CDTypeSymbol targetSymbol, QualifierKey key) {
    return (CDTypeSymbol) CDSymbolTables.getAssociationsInHierarchy(targetSymbol.getAstNode()).stream()
        .filter(a -> a.getName().equals(key.getAttrName())).findFirst().get()
        .getSymbol().getType().getTypeInfo();
  }

  protected void addImports(ASTCDCompilationUnit origCD, CDTypeSymbol targetSymbol, QualifierKey key, ASTCDClass ast) {
    addDomainImports(ast, targetSymbol.getAstNode());
    addDomainImports(ast, key.getDeclaringType().getAstNode());
    if (key.isEntryKey()) {
      addDomainImports(ast, getKeySymbol(targetSymbol, key).getAstNode());
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
}
