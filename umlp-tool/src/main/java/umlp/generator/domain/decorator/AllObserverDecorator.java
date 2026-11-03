/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

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
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.Map;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.GenService.createType;

public class AllObserverDecorator extends AbstractDecorator {

  public static final String SUFFIX = "AllObserver";

  private String cdName;

  public AllObserverDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    cdName = origCD.getCDDefinition().getName();
    getNonDerivedDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(c))));
  }

  protected ASTCDClass createClass(ASTCDClass domainClass) {
    String className = domainClass.getName();

    ASTCDClass observerClass = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage(className + "Observer"))
        .setName(className + SUFFIX)
        .build();

    getCD4C().addImport(observerClass, "java.util.function.Consumer");

    getCD4C().addAttribute(observerClass, "  private Consumer<Void> consumer");

    getCD4C().addConstructor(observerClass, "umlp.allobserver.AllObserverConstructor", className + SUFFIX);

    var getScope = getCD4C().addMethod(observerClass, "umlp.common_observer.GetScopeLocal");
    glex.replaceTemplate("cd2java.Annotations", getScope, new StringHookPoint("@Override"));
    getCD4C().addImport(observerClass, PathUtil.getBackendRTECommonPath("NotificationScope"));


    UMLPSymbolTables.getAttributesInHierarchyWithDefiningType(domainClass).entrySet().stream()
        .filter(e -> !e.getKey().getModifier().isDerived())
        .forEach(e -> {
          ASTCDAttribute a = e.getKey();
          ASTCDType definingType = e.getValue();
          String fullType = getCDPrinter().prettyprint(createType(a));
          if(GenService.isList(a)) {
            String innerType = GenService.getInnerType(a.getMCType());
            getCD4C().addMethod(observerClass, "umlp.allobserver.listattrs.AllObserverAddNotify",
                UMLPSymbolTables.getDiagramFor(a).getCDDefinition().getName(), a.getName(), definingType.getName(), domainClass.getName(), innerType);
            getCD4C().addMethod(observerClass, "umlp.allobserver.listattrs.AllObserverRemoveNotify",
                UMLPSymbolTables.getDiagramFor(a).getCDDefinition().getName(), a.getName(), definingType.getName(), domainClass.getName(), innerType);
          }

          ASTCDMethodSignature set = getCD4C().addMethod(observerClass, "umlp.allobserver.AllObserverNotify",
              cdName, className, a.getName(), definingType.getName(), fullType);
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
          // maps: values are put under a key and rekeyed, there are no positions (no set at index)
          String keyType = QualifierKey.of(e.getKey()).getStoredKeyType();
          glex.replaceTemplate("cd2java.Annotations", getCD4C().addMethod(observerClass,
              "umlp.allobserver.assocs.AllObserverPutNotify", cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(),
              otherTypeName, keyType), new StringHookPoint("@Override"));
          glex.replaceTemplate("cd2java.Annotations", getCD4C().addMethod(observerClass,
              "umlp.allobserver.assocs.AllObserverRekeyNotify", cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(),
              otherTypeName, keyType), new StringHookPoint("@Override"));
          glex.replaceTemplate("cd2java.Annotations", getCD4C().addMethod(observerClass,
              "umlp.allobserver.assocs.AllObserverRemoveNotify", cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(),
              otherTypeName), new StringHookPoint("@Override"));
          continue;
        }
        if (GenService.isMultiValued(e.getKey())) {
          add = getCD4C().addMethod(observerClass, "umlp.allobserver.assocs.AllObserverAddNotify",
              cdDefinitionOfAssoc , className, e.getValue().getName(), e.getKey(), otherTypeName);
          remove = getCD4C().addMethod(observerClass, "umlp.allobserver.assocs.AllObserverRemoveNotify",
              cdDefinitionOfAssoc, className, e.getValue().getName(), e.getKey(), otherTypeName);
        }
        ASTCDMethodSignature set = getCD4C().addMethod(observerClass, "umlp.allobserver.assocs.AllObserverSetNotify",
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


    return observerClass;
  }
}
