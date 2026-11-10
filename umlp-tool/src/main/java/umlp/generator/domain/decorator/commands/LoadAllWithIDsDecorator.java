/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.UMLPMill;
import umlp.generator.util.PathUtil;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class LoadAllWithIDsDecorator extends AbstractDeSerDecorator {

  public static final String PREFIX = "LoadAll";
  public static final String SUFFIX = "WithIDs";

  public LoadAllWithIDsDecorator(GlobalExtensionManagement glex) {
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
    String cmdName = PREFIX + classname + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("Request<" + classname + ">"))
        .setName(cmdName)
        .build();

    ASTCDAttribute attr = addGemIds(ast);

    addConstructor(ast, cmdName, domainPackage);

    getCD4C().addMethod(ast, "umlp.commands.GeneralCheckAccessPolicy", classname, "Read");
    getCD4C().addMethod(ast, "umlp.commands.loadallwithids.DoRun", classname, domainPackage, getCorrectManager(domainType));
    getCD4C().addMethod(ast, "umlp.commands.loadallwithids.GetNewlySubscribedGemIds");

    AccessorDecorator accessor = new AccessorDecorator(glex);
    ast.addAllCDMembers(accessor.decorate(attr));

    addImports(origCD, domainType, ast, classname);

    return ast;
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDType domainType, ASTCDClass ast, String classname) {
    getCD4C().addImport(ast, PathUtil.getBackendRTERequestPath("Request"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ListResult"));
    getCD4C().addImport(ast, "java.util.List");
    getCD4C().addImport(ast, "java.util.Collection");
    getCD4C().addImport(ast, "java.util.Iterator");
    getCD4C().addImport(ast, "java.util.ListIterator");
    getCD4C().addImport(ast, "java.util.Spliterator");
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + classname);
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + classname + "AccessPolicy");
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + getCorrectManager(domainType));
  }

  protected ASTCDAttribute addGemIds(ASTCDClass ast) {
    return getCD4C().addAttribute(ast, "private final List<Long> gemIDs");
  }

  protected void addConstructor(ASTCDClass ast, String cmdName, String packageName) {
    getCD4C().addConstructor(ast, "umlp.commands.loadallwithids.Constructor", cmdName, packageName);
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDType domainType) {
    String cmdName = PREFIX + domainType.getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
        .setName(cmdName + "DeSer")
        .build();

    getCD4C().addMethod(ast, "umlp.commands.loadallwithids.deser.Get_FromJsonObject", cmdName);
    getCD4C().addMethod(ast, "umlp.commands.loadallwithids.deser.Get_BuildJson", cmdName);

    addDeSerImport(origCD, ast, cmdName);

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDCompilationUnit origCD, ASTCDClass ast, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonElement");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, "java.util.ArrayList");
    getCD4C().addImport(ast, "java.util.List");
    getCD4C().addImport(ast, "java.util.Optional");
  }
}
