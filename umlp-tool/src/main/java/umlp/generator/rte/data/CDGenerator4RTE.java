package umlp.generator.rte.data;

import de.monticore.cd.codegen.CDGenerator;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.generating.templateengine.TemplateHookPoint;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.domain.decorator.GenService;

import java.util.Arrays;

/**
 * configures and employs generators of individual UML/P languages to generate
 * code. Currently, it uses only the CDGenerator.
 *
 * Later do: make this customizable as Groovy script
 */
public class CDGenerator4RTE {

  protected GeneratorSetup genSetup;
  protected UMLPGeneratorConfig config;

  protected CDGenerator cdGenerator;

  public CDGenerator4RTE(GeneratorSetup genSetup, UMLPGeneratorConfig config) {
    this.genSetup = genSetup;
    this.config = config;

    genSetup.getGlex().setGlobalValue("cdPrinter", new CdUtilsPrinter());
    genSetup.getGlex().setGlobalValue("genService", new GenService());
    cdGenerator = new CDGenerator(genSetup);
  }

  public void generate(ASTCDCompilationUnit cdAST, String configTemplate) {
    // create an empty classdiagram
    ASTCDCompilationUnit decoratedAST = createCompilationUnit(cdAST);

    genSetup.getGlex().setGlobalValue("UMLPGeneratorConfig", config);
    genSetup.getGlex().addAfterTemplate("ClassContent:Annotations",
        new StringHookPoint("@umlp.jsweet.extension.annotation.CD2Java"));
    genSetup.getGlex().addAfterTemplate("EnumContent:Annotations",
        new StringHookPoint("@umlp.jsweet.extension.annotation.CD2Java"));
    genSetup.getGlex().addAfterTemplate("InterfaceContent:Annotations",
        new StringHookPoint("@umlp.jsweet.extension.annotation.CD2Java"));

    TemplateController tc = genSetup.getNewTemplateController(configTemplate);
    TemplateHookPoint hpp = new TemplateHookPoint(configTemplate);
    // translate the CD-AST into Java with the CD Generator
    hpp.processValue(tc, cdAST, Arrays.asList(decoratedAST, genSetup.getGlex(), genSetup.getHandcodedPath(), cdGenerator));
  }

  public ASTCDCompilationUnit createCompilationUnit(ASTCDCompilationUnit cd) {
    // create an empty CD definition
    ASTCDDefinition cdDef = CD4CodeMill.cDDefinitionBuilder()
        .setName(cd.getCDDefinition().getName())
        .setModifier(CDModifier.PUBLIC.build())
        .build();

    // ... and add it to a new CD compilation unit
    ASTCDCompilationUnit newCD = CD4CodeMill.cDCompilationUnitBuilder()
        .setCDDefinition(cdDef)
        .build();

    // set the package
    if (cd.isPresentMCPackageDeclaration()) {
      newCD.setMCPackageDeclaration(cd.getMCPackageDeclaration());
    }

    return newCD;
  }
}

