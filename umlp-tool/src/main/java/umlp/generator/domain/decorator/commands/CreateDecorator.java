/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.DeSerDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.List;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class CreateDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_create";

  public CreateDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getConcreteDomainClasses(origCD).forEach(c -> {
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

    ASTCDAttribute attr = addDomainAttr(ast, classname);

    addConstructor(ast, classname, packageName);
    List<ASTCDAssocSide> bidirectionalMaps = GenService.getBidirectionalMaps(domainClass);
    getCD4C().addMethod(ast, getDoRunTemplate(), classname, getCorrectManager(domainClass), domainPackage, hasOCL,
        bidirectionalMaps);
    if (!bidirectionalMaps.isEmpty()) {
      // the create command updates the other side of these maps (create/DoRun.ftl)
      getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandManager"));
      getCD4C().addImport(ast, "java.util.Optional");
      getCD4C().addImport(ast, "java.util.Collections");
      for (ASTCDAssocSide map : bidirectionalMaps) {
        addCorrectMangerImportForTargetType(origCD.getCDDefinition(), ast, map.getSymbol().getType());
        addCorrectMangerImportForTargetType(origCD.getCDDefinition(), ast,
            GenService.getOtherSideOfMap(map).get().getSymbol().getType());
        // the value and the owner type
        for (ASTCDAssocSide side : List.of(map, GenService.getOtherSideOfMap(map).get())) {
          ASTCDType type = (ASTCDType) side.getSymbol().getType().getTypeInfo().getAstNode();
          getCD4C().addImport(ast, getPackageName(UMLPSymbolTables.getDiagramFor(type), DEFAULT_PACKAGE) + "."
              + type.getName());
        }
      }
    }
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainClass.getName(), "Create");
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList(StringTransformations.uncapitalize(classname) + ".getGemId()"));
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
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + classname);

    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ getCorrectManager(domainClass));

    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE)+"."+ classname + "AccessPolicy");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("ValidationMessage"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("ValidationStatus"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ValidationResult"));
    getCD4C().addImport(ast, "java.util.stream.Collectors;");
  }

  protected String extendedClass() {
    return "Command<Void>";
  }

  protected ASTCDAttribute addDomainAttr(ASTCDClass ast, String classname) {
    return getCD4C().addAttribute(ast, "protected final "
        + classname + " " + StringTransformations.uncapitalize(classname));
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, packageName, SUFFIX);
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.create.DoRun";
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.Constructor";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, ASTCDClass domainClass) {
    String cmdName = domainClass.getName() + SUFFIX;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
            .setName(cmdName+DE_SER)
            .build();

    getCD4C().addMethod(ast, getFromJsonObjectTemplate(), domainClass.getName(), cmdName);
    getCD4C().addMethod(ast, getBuildJsonTemplate(), domainClass.getName(), cmdName);

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

  protected String getBuildJsonTemplate() {
    return "umlp.commands.deser.BuildJson";
  }

  protected String getFromJsonObjectTemplate() {
    return "umlp.commands.deser.FromJsonObject";
  }
}
