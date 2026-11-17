/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genjava;

import de.monticore.cd.codegen.CDGenerator;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;

import java.util.Arrays;

public class GUI2JavaGenerator extends CDGenerator {

  public GUI2JavaGenerator(GeneratorSetup genSetup) {
    super(genSetup);
    genSetup.getGlex().setGlobalValue("cdPrinter", new CdUtilsPrinter());
  }

  public void generate(ASTGUICompilationUnit gui, String configTemplate) {
    this.setup.setTracing(true);
    try {
      Reporting.addConfigTemplate(configTemplate);
      // create an empty classdiagram
      ASTCDCompilationUnit cd = GUI2CDTrafo.createCDCompilationUnit(gui);

      TemplateController tc = setup.getNewTemplateController(configTemplate);
      TemplateHookPoint hpp = new TemplateHookPoint(configTemplate);

      // translate the GUI-AST into Java
      hpp.processValue(tc, gui,
          Arrays.asList(cd, setup.getGlex(), setup.getHandcodedPath(), setup.getOutputDirectory().toPath(), this));
    }finally {
      Reporting.clearConfigTemplates();
    }
  }

}
