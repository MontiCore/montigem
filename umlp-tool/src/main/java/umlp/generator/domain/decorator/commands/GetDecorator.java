/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;/* (c) https://github.com/MontiCore/monticore */

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import org.jetbrains.annotations.NotNull;
import umlp.UMLPMill;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class GetDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_get";

  public GetDecorator(GlobalExtensionManagement glex) {
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
      .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("Request<"+classname+">"))
      .setName(classname+SUFFIX)
      .build();
    ASTCDAttribute attr = addGemId(ast);

    addConstructor(ast, classname, packageName);
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainType.getName(), "Read");
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList());

    if(TagLookUp.isSingleton(domainType.getSymbol())){
      getCD4C().addAttribute(ast, "private long actualGemId = -1");
      getCD4C().addMethod(ast, getSingletonDoRunTemplate(), classname, domainPackage, getCorrectManager(domainType));
      getCD4C().addMethod(ast, getSingletonGetNewlySubscribedGemIdsTemplate());
    } else {
      getCD4C().addMethod(ast, getDoRunTemplate(), classname, domainPackage, getCorrectManager(domainType));
      getCD4C().addMethod(ast, getGetNewlySubscribedGemIdsTemplate());
    }

    AccessorDecorator accessor = new AccessorDecorator(glex);
    ast.addAllCDMembers(accessor.decorate(attr));

    addImports(origCD, domainType, ast, classname);

    return ast;
  }

  protected @NotNull String getSingletonGetNewlySubscribedGemIdsTemplate() {
    return "umlp.commands.get.SingletonGetNewlySubscribedGemIds";
  }

  protected @NotNull String getSingletonDoRunTemplate() {
    return "umlp.commands.get.SingletonDoRun";
  }

  protected String getGeneralCheckAccessPolicyTemplate() {
    return "umlp.commands.GeneralCheckAccessPolicy";
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDType domainType, ASTCDClass ast,
      String classname) {
    getCD4C().addImport(ast, PathUtil.getBackendRTERequestPath("Request"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("SimpleResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("LoadingState"));
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ classname);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+ "." + classname + "AccessPolicy");
    getCD4C().addImport(
        ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+getCorrectManager(domainType));
  }

  protected ASTCDAttribute addGemId(ASTCDClass ast) {
    return getCD4C().addAttribute(ast, "private final long gemId");
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname,"", SUFFIX, packageName);
  }

  protected String getGetNewlySubscribedGemIdsTemplate() {
    return "umlp.commands.get.GetNewlySubscribedGemIds";
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.get.DoRun";
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.get.Constructor";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDType domainType) {
    String cmdName = domainType.getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
            .setName(cmdName+DE_SER)
            .build();

    getCD4C().addMethod(ast, getGet_FromJsonObjectTemplate(), cmdName);
    getCD4C().addMethod(ast, getGet_BuildJsonTemplate(),cmdName);

    addDeSerImport(origCD, ast, cmdName);

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDCompilationUnit origCD, ASTCDClass ast, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
  }

  protected String getGet_BuildJsonTemplate() {
    return "umlp.commands.deser.Get_BuildJson";
  }

  protected String getGet_FromJsonObjectTemplate() {
    return "umlp.commands.deser.Get_FromJsonObject";
  }
}
