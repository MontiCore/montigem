/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;/* (c) https://github.com/MontiCore/monticore */

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
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

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.ManagerDecorator.SUFFIX;

public class GemListAssocDecorator extends AbstractDecorator {

  public GemListAssocDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, GEMLIST_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> CDSymbolTables.getAssociations(c)
                                                                  .stream()
                                                                  .filter(Predicate.not(GenService::isSymbolDerived))
                                                                  .forEach(a -> astcdPackage.addAllCDElements(
                                                                      withOrigin(createClasses(
                                                                          origCD,
                                                                          a,
                                                                          c)))));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> CDSymbolTables.getAssociations(i)
        .stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .forEach(a -> astcdPackage.addAllCDElements(withOrigin(createClasses(origCD, a, i)))));
  }

  protected Collection<ASTCDClass> createClasses(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType clazz) {
    List<ASTCDClass> retList = Lists.newArrayList();
    if(GenService.isList(assoc)) {
      retList.add(createGemListClass(origCD, assoc, clazz));
    }
    return retList;
  }

  protected ASTCDClass createGemListClass(ASTCDCompilationUnit origCD, ASTCDAssocSide assoc, ASTCDType domainClass) {
    String sourceName = domainClass.getName();
    String assocName = assoc.getName();
    String className = getName(domainClass, assoc);
    CDTypeSymbol targetSymbol = (CDTypeSymbol) assoc.getMCQualifiedType().getDefiningSymbol().get();
    String targetName = targetSymbol.getName();
    String managerName = origCD.getCDDefinition().getName() + ManagerDecorator.SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setName(className)
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("GemListBuilder<" + targetName + ">"))
            .build();

    // Imports
    ASTCDType targetType = targetSymbol.getAstNode();
    ASTCDCompilationUnit targetCD = UMLPSymbolTables.getDiagramFor(targetType);
    addImports(origCD, sourceName, targetCD, targetName, ast);

    // attributes and getter
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected long ownerGemID");
    ast.addAllCDMembers(accessor.decorate(attr));

    // constructor
    addConstructors(ast, className);
    // methods
    getCD4C().addMethod(ast, getLoadObjectsTemplate(), targetName, getCorrectManager(targetSymbol.getAstNode()));
    getCD4C().addMethod(ast, getGetOwnerTemplate(), sourceName, managerName);
    getCD4C().addMethod(ast, getInternalAddTemplate(), assocName);
    getCD4C().addMethod(ast, getInternalSetTemplate(), assocName);
    getCD4C().addMethod(ast, getInternalRemoveTemplate(), assocName);
    ASTCDMethodSignature m = getCD4C().addMethod(ast, getDeepEqualsTemplate(), targetName);
    markDeepEqualsBackendOnly(ast, m);

    return ast;
  }

  protected void markDeepEqualsBackendOnly(ASTCDType targetType, ASTCDMethodSignature m) {
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint("@BackendOnly(BackendOnlyModes.THROW_ERROR)"));
    getCD4C().addImport(targetType, "umlp.jsweet.extension.annotation.BackendOnly");
    getCD4C().addImport(targetType, "umlp.jsweet.extension.annotation.BackendOnlyModes");
  }

  protected void addConstructors(ASTCDClass ast, String className) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), className);
//    getCD4C().addConstructor(ast, getConstructorEmptyTemplate(), className);
  }

  protected String getConstructorEmptyTemplate() {
    return "umlp.gemlist.assocs.ConstructorEmpty";
  }

  protected String getConstructorTemplate() {
    return "umlp.gemlist.assocs.Constructor";
  }

  protected String getInternalRemoveTemplate() {
    return "umlp.gemlist.assocs.InternalRemove";
  }

  protected String getInternalSetTemplate() {
    return "umlp.gemlist.assocs.InternalSet";
  }

  protected String getInternalAddTemplate() {
    return "umlp.gemlist.assocs.InternalAdd";
  }

  protected String getGetOwnerTemplate() {
    return "umlp.gemlist.assocs.GetOwner";
  }

  protected String getLoadObjectsTemplate() {
    return "umlp.gemlist.LoadObjects";
  }

  protected String getDeepEqualsTemplate(){
    return "umlp.gemlist.DeepEquals";
  }

  protected void addImports(ASTCDCompilationUnit origCD, String domainName,
                            ASTCDCompilationUnit targetCD, String targetName, ASTCDClass ast) {
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), domainName));
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(targetCD, DEFAULT_PACKAGE), targetName));
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(targetCD, DEFAULT_PACKAGE), targetCD.getCDDefinition().getName()+ SUFFIX));
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), origCD.getCDDefinition().getName() + SUFFIX));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GemListBuilder"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, "umlp.backendrte.common.LoadResult");
    getCD4C().addImport(ast, "umlp.backendrte.common.LoadingState");
    getCD4C().addImport(ast, "java.util.HashMap");
    getCD4C().addImport(ast, "java.util.Map");
  }

  public static String getName(ASTCDType type, ASTCDAssocSide assoc) {
    return type.getName()
            + "_"
            + StringTransformations.capitalize(assoc.getName())
            + "GemList";
  }

}
