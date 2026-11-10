/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.DeSerDecorator;
import umlp.generator.util.PathUtil;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class DeleteDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_delete";

  public DeleteDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getConcreteNonPojoDomainClasses(origCD).forEach(c -> {
      astcdPackage.addCDElement(withOrigin(createClass(origCD, c, astcdPackage.getName())));
      astcdPackage.addCDElement(withOrigin(createDeSer(origCD, c)));
    });
  }

  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, ASTCDClass domainClass, String packageName) {
    String classname = domainClass.getName();
    String domainPackage = getPackageName(origCD, DEFAULT_PACKAGE);

    ASTCDClass ast = UMLPMill.cDClassBuilder()
      .setModifier(PUBLIC.build())
      .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
      .setName(classname+SUFFIX)
      .build();

    ASTCDAttribute attr = addDomainAttr(ast);

    addConstructor(ast, classname, packageName);
    getCD4C().addMethod(ast, getDoRunTemplate(), classname, getCorrectManager(domainClass), domainPackage);
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainClass.getName(), "Delete");
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("gemId"));
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ast.addAllCDMembers(accessor.decorate(attr));

    addImports(origCD, domainClass, ast, classname);

    return ast;
  }

  protected String getGeneralCheckAccessPolicyTemplate() {
    return "umlp.commands.GeneralCheckAccessPolicy";
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDClass domainClass, ASTCDClass ast,
      String classname) {
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("Command"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ classname);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+ "." + classname + "AccessPolicy");
    getCD4C().addImport(
        ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+getCorrectManager(domainClass));
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
  }

  protected ASTCDAttribute addDomainAttr(ASTCDClass ast) {
    return getCD4C().addAttribute(ast, "private final long gemId");
  }

  protected String extendedClass() {
    return "Command<Void>";
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, "", SUFFIX, packageName);
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.delete.DoRun";
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.get.Constructor";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDClass domainClass) {
    String cmdName = domainClass.getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
            .setName(cmdName+DE_SER)
            .build();

    getCD4C().addMethod(ast, getGet_FromJsonObjectTemplate(), cmdName);
    getCD4C().addMethod(ast, getGet_BuildJsonTemplate(), cmdName);

    addDeSerImport(ast, origCD, domainClass, cmdName);

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDClass ast, ASTCDCompilationUnit origCD, ASTCDClass domainClass, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+domainClass.getName());
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+domainClass.getName()+ DeSerDecorator.SUFFIX);
  }

  protected String getGet_BuildJsonTemplate() {
    return "umlp.commands.deser.Get_BuildJson";
  }

  protected String getGet_FromJsonObjectTemplate() {
    return "umlp.commands.deser.Get_FromJsonObject";
  }
}
