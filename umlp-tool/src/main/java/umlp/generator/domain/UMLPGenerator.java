/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain;

import de.monticore.cd.codegen.CDGenerator;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.se_rwth.commons.logging.Log;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import umlp.generator.db.sql.SQLSchemaGenerator;
import umlp.generator.domain.decorator.GenService;

import java.util.Arrays;
import java.util.List;

/**
 * configures and employs generators of individual UML/P languages to generate
 * code. Currently, it uses only the CDGenerator.
 *
 * Later do: make this customizable as Groovy script
 */
public class UMLPGenerator {

  protected GeneratorSetup genSetup;
  protected UMLPGeneratorConfig config;

  protected CDGenerator cdGenerator;
  protected SQLSchemaGenerator schemaGenerator;

  public UMLPGenerator(GeneratorSetup genSetup, UMLPGeneratorConfig config) {
    this.genSetup = genSetup;
    this.config = config;

    genSetup.getGlex().setGlobalValue("cdPrinter", new CdUtilsPrinter());
    genSetup.getGlex().setGlobalValue("genService", new GenService());
    cdGenerator = new CDGenerator(genSetup);
    schemaGenerator = new SQLSchemaGenerator(genSetup);
  }

  public void generate(ASTCDCompilationUnit cdAST, String configTemplate,
                       List<ASTOCLCompilationUnit> oclCompilationUnits) {
    try {
      Reporting.addConfigTemplate(configTemplate);

      // create an empty classdiagram
      ASTCDCompilationUnit decoratedAST = createCompilationUnit(cdAST);

      genSetup.getGlex().setGlobalValue("UMLPGeneratorConfig", config);

      TemplateController tc = genSetup.getNewTemplateController(configTemplate);
      TemplateHookPoint hpp = new TemplateHookPoint(configTemplate);
      // translate the CD-AST into Java with the CD Generator
      hpp.processValue(tc, cdAST, Arrays.asList(decoratedAST,
          genSetup.getGlex(),
          genSetup.getHandcodedPath(),
          cdGenerator,
          schemaGenerator,
          oclCompilationUnits));
    }finally {
      Reporting.clearConfigTemplates();
    }
  }

  protected static void logTrafoExec(String trafo, ASTCDCompilationUnit ast){
    Log.info("========= CD after '"+trafo+"': =========", UMLPGenerator.class.getName());
  }

  public ASTCDCompilationUnit createCompilationUnit(ASTCDCompilationUnit cd) {
    return createNewCompilationUnit(cd);
  }

  public static ASTCDCompilationUnit createNewCompilationUnit(ASTCDCompilationUnit cd) {
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
