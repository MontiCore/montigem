/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;/* (c) https://github.com/MontiCore/monticore */

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.UMLPMill;
import umlp.generator.util.PathUtil;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class GetFilteredDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_get_filtered";

  public GetFilteredDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getNonDerivedNonPojoDomainClasses(origCD).forEach(c -> {
      astcdPackage.addCDElement(withOrigin(createClass(origCD, c, astcdPackage.getName())));
      astcdPackage.addCDElement(withOrigin(createDeSer(origCD, c)));
    });

    getNonDerivedDomainInterfaces(origCD).forEach(c -> {
          astcdPackage.addCDElement(withOrigin(createClass(origCD, c, astcdPackage.getName())));
          astcdPackage.addCDElement(withOrigin(createDeSer(origCD, c)));
        });
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, ASTCDType domainType, String packageName) {
    String classname = domainType.getName();
    String domainPackage = getPackageName(origCD, DEFAULT_PACKAGE);

    ASTCDClass ast = UMLPMill.cDClassBuilder()
      .setModifier(PUBLIC.build())
      .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
      .setName(classname+SUFFIX)
      .build();

    addConstructor(ast, classname, packageName);
    getCD4C().addMethod(ast, getDoRunTemplate(), classname,  getCorrectManager(domainType), domainPackage);
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainType.getName(), "Read");
    addClassImports(ast, origCD, domainType);

    return ast;
  }

  protected void addClassImports(ASTCDClass ast, ASTCDCompilationUnit origCD, ASTCDType domainType) {
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+ "." + domainType.getName() + "AccessPolicy");
    getCD4C().addImport(ast, PathUtil.getBackendRTERequestPath("Request"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ListResult"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ getCorrectManager(domainType));
  }

  protected String getGeneralCheckAccessPolicyTemplate() {
    return "umlp.commands.GeneralCheckAccessPolicy";
  }

  protected String extendedClass() {
    return "Request<Long>";
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, "", SUFFIX, packageName);
    getCD4C().addConstructor(ast, getConstructorWithFilterTemplate(), classname, "", SUFFIX, packageName);
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.get_filtered.DoRun";
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.get_filtered.Constructor";
  }

  protected String getConstructorWithFilterTemplate() {
    return "umlp.commands.get_filtered.ConstructorWithFilter";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDType domainType) {
    String cmdName = domainType.getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
            .setName(cmdName+DE_SER)
            .build();

    getCD4C().addMethod(ast, getGetFiltered_FromJsonObjectTemplate(), cmdName);
    getCD4C().addMethod(ast, getGetFiltered_BuildJsonTemplate(),cmdName);

    addDeSerImport(origCD, ast, cmdName);

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDCompilationUnit origCD, ASTCDClass ast, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
  }

  protected String getGetFiltered_BuildJsonTemplate() {
    return "umlp.commands.deser.GetFiltered_BuildJson";
  }

  protected String getGetFiltered_FromJsonObjectTemplate() {
    return "umlp.commands.deser.GetFiltered_FromJsonObject";
  }
}
