/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._symboltable.CDRoleSymbol;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.Joiners;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class DbObserverDecorator extends AbstractDecorator {

  public static final String SUFFIX = "DbObserver";

  private String cdName;

  public DbObserverDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    cdName = origCD.getCDDefinition().getName();
    getConcreteDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(c))));
  }

  protected ASTCDClass createClass(ASTCDClass domainClass) {
    String className = domainClass.getName();

    ASTCDClass observerClass = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage(className + "Observer"))
        .setName(className + SUFFIX)
        .build();

    var getScope = getCD4C().addMethod(observerClass, "umlp.common_observer.GetScopeLocal");
    glex.replaceTemplate("cd2java.Annotations", getScope, new StringHookPoint("@Override"));
    getCD4C().addImport(observerClass, PathUtil.getBackendRTECommonPath("NotificationScope"));


    UMLPSymbolTables.getAttributesInHierarchyWithDefiningType(domainClass).entrySet().stream()
        .filter(e -> !e.getKey().getModifier().isDerived())
        .forEach(e -> {
          ASTCDAttribute a = e.getKey();
          ASTCDType definingType = e.getValue();
          if(GenService.isList(a)) {
            String innerType = GenService.getInnerType(a.getMCType());
            getCD4C().addMethod(observerClass, "umlp.dbobserver.listattrs.AddNotify",
                UMLPSymbolTables.getDiagramFor(domainClass).getCDDefinition().getName(), a.getName(), definingType.getName(), domainClass.getName(), innerType);
            getCD4C().addMethod(observerClass, "umlp.dbobserver.listattrs.RemoveNotify",
                UMLPSymbolTables.getDiagramFor(domainClass).getCDDefinition().getName(), a.getName(), definingType.getName(), domainClass.getName(), innerType);
          }

          ASTCDMethodSignature set = getCD4C().addMethod(observerClass, "umlp.dbobserver.Notify",
              cdName, className, a.getName(), definingType.getName(), GenService.getType(a.getSymbol().getType()));
          glex.replaceTemplate("cd2java.Annotations", set, new StringHookPoint("@Override"));

          ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor(definingType);
          getCD4C().addImport(observerClass, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + definingType.getName());
        });

    for (Map.Entry<ASTCDAssocSide, ASTCDAssociation> e : UMLPSymbolTables.getOppositeSideToAssociationMap(domainClass).entrySet()) {
      if (!e.getValue().getModifier().isDerived()) {
        CDRoleSymbol cdRollSymbol = e.getKey().getSymbol();

        SymTypeExpression type = cdRollSymbol.getType();

        SymTypeExpression otherType = cdRollSymbol.getOtherSide().getType();
        String otherTypeName = otherType.print();

        ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
        ASTCDCompilationUnit otherTypeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) otherType.getTypeInfo().getAstNode());
        getCD4C().addImport(observerClass, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print());
        getCD4C().addImport(observerClass, getPackageName(otherTypeCD, DEFAULT_PACKAGE) + "." + otherType.print());

        ASTCDMethodSignature add = null;
        ASTCDMethodSignature remove = null;

        String cdDefinitionOfAssoc = UMLPSymbolTables.getDiagramFor(e.getValue()).getCDDefinition().getName();
        if (GenService.isMap(e.getKey())) {
          // maps: values are put under a key, there are no positions (no set at index);
          // keys are not stored, they are derived when loading, so rekeying changes nothing
          String keyType = QualifierKey.of(e.getKey()).getStoredKeyType();
          glex.replaceTemplate("cd2java.Annotations", getCD4C().addMethod(observerClass,
              "umlp.dbobserver.assocs.PutNotify", cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(),
              otherTypeName, keyType), new StringHookPoint("@Override"));
          glex.replaceTemplate("cd2java.Annotations", getCD4C().addMethod(observerClass,
              "umlp.dbobserver.assocs.RemoveNotify", cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(),
              otherTypeName), new StringHookPoint("@Override"));
          continue;
        }
        if (GenService.isMultiValued(e.getKey())) {
          add = getCD4C().addMethod(observerClass, "umlp.dbobserver.assocs.AddNotify",
              cdDefinitionOfAssoc , className, e.getValue().getName(), e.getKey(), otherTypeName);
          remove = getCD4C().addMethod(observerClass, "umlp.dbobserver.assocs.RemoveNotify",
              cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(), otherTypeName);
        }
        ASTCDMethodSignature set = getCD4C().addMethod(observerClass, "umlp.dbobserver.assocs.SetNotify",
            cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(), otherTypeName);

        if (add != null && remove != null) {
          glex.replaceTemplate("cd2java.Annotations", add, new StringHookPoint("@Override"));
          glex.replaceTemplate("cd2java.Annotations", remove, new StringHookPoint("@Override"));
        }
        glex.replaceTemplate("cd2java.Annotations", set, new StringHookPoint("@Override"));
      }
    }

    addImports(domainClass, observerClass);
    super.addImports(domainClass, observerClass);

    for (ASTCDMethod method : observerClass.getCDMethodList()) {
      glex.replaceTemplate("cd2java.Annotations", method, new StringHookPoint("@Override"));
    }

    // Add @jsweet.lang.Erased annotation
    glex.replaceTemplate("cd2java.Annotations", observerClass,
        new StringHookPoint("@jsweet.lang.Erased"));


    return observerClass;
  }

  protected void addImports(ASTCDClass domainClass, ASTCDType observerClass) {
    getCD4C().addImport(observerClass, PathUtil.getBackendRTECommonPath("DatabaseAccessManager"));

    Set<String> daoFactoriesFromExtendedCDs = new LinkedHashSet<>();
    List<ASTCDAssocSide> assocSides = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(GenService::isVisible).collect(Collectors.toList());
    assocSides.stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .map(assocSide -> assocSide.getSymbol().getAssoc().getAssociation())
        .forEach(associationSymbol -> {
          daoFactoriesFromExtendedCDs.add(
              Joiners.DOT.join(associationSymbol.getPackageName().toLowerCase(),
                  UMLPSymbolTables.getDiagramFor(associationSymbol.getAstNode()).getCDDefinition().getName()+"DAOFactory"));
        });
    daoFactoriesFromExtendedCDs.forEach(i -> getCD4C().addImport(observerClass, i));
  }
}
