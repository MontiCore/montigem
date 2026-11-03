/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;/* (c) https://github.com/MontiCore/monticore */

import com.google.common.collect.Lists;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.se_rwth.commons.Joiners;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;

import java.util.Collection;
import java.util.List;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.ManagerDecorator.SUFFIX;

public class GemListDomainDecorator extends AbstractDecorator {

  public GemListDomainDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, GEMLIST_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> astcdPackage.addAllCDElements(withOrigin(
        createClasses(origCD, c))));

    getNonDerivedDomainInterfaces(origCD).forEach(i -> astcdPackage.addAllCDElements(withOrigin(createClasses(origCD,
                                                                                                              i))));
  }

  protected Collection<ASTCDClass> createClasses(ASTCDCompilationUnit origCD, ASTCDType clazz) {
    List<ASTCDClass> retList = Lists.newArrayList();
    retList.add(createGemListClass(origCD, clazz));
    return retList;
  }

  protected ASTCDClass createGemListClass(ASTCDCompilationUnit origCD, ASTCDType domainClass) {
    String sourceName = domainClass.getName();
    String classname = sourceName
        + "GemList";
    String managerName = origCD.getCDDefinition().getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
      .setModifier(PUBLIC.build())
      .setName(classname)
      .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("GemListBuilder<" + sourceName + ">"))
      .build();

    // Imports
    addImports(origCD, domainClass.getName(), ast);

    // constructor
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname);
    getCD4C().addConstructor(ast, getConstructorDefaultTemplate(), classname);

    // methods
    getCD4C().addMethod(ast, getLoadObjectsTemplate(), sourceName, getCorrectManager(domainClass));
    ASTCDMethodSignature m = getCD4C().addMethod(ast, getDeepEqualsTemplate(), sourceName);
    markDeepEqualsBackendOnly(ast, m);
    getCD4C().addMethod(ast, getInternalAddTemplate());
    getCD4C().addMethod(ast, getInternalSetTemplate());
    getCD4C().addMethod(ast, getInternalRemoveTemplate());

    return ast;
  }

  protected void markDeepEqualsBackendOnly(ASTCDType targetType, ASTCDMethodSignature m) {
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint("@BackendOnly(BackendOnlyModes.THROW_ERROR)"));
    getCD4C().addImport(targetType, "umlp.jsweet.extension.annotation.BackendOnly");
    getCD4C().addImport(targetType, "umlp.jsweet.extension.annotation.BackendOnlyModes");
  }

  protected String getConstructorDefaultTemplate() {
    return "umlp.gemlist.ConstructorDefault";
  }

  protected String getConstructorTemplate() {
    return "umlp.gemlist.Constructor";
  }

  protected String getLoadObjectsTemplate() {
    return "umlp.gemlist.LoadObjects";
  }

  protected String getDeepEqualsTemplate(){
    return "umlp.gemlist.DeepEquals";
  }

  protected String getInternalRemoveTemplate() {
    return "umlp.gemlist.AllObjectGemListInternalRemove";
  }

  protected String getInternalSetTemplate() {
    return "umlp.gemlist.AllObjectGemListInternalSet";
  }

  protected String getInternalAddTemplate() {
    return "umlp.gemlist.AllObjectGemListInternalAdd";
  }

  protected void addImports(ASTCDCompilationUnit origCD, String domainName, ASTCDClass ast) {
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), domainName));
    getCD4C().addImport(ast, Joiners.DOT.join(getPackageName(origCD, DEFAULT_PACKAGE), origCD.getCDDefinition().getName()+SUFFIX));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GemListBuilder"));
    getCD4C().addImport(ast, "umlp.backendrte.common.LoadResult");
    getCD4C().addImport(ast, "umlp.backendrte.common.LoadingState");
    getCD4C().addImport(ast, "java.util.HashMap");
    getCD4C().addImport(ast, "java.util.Map");
  }

}
