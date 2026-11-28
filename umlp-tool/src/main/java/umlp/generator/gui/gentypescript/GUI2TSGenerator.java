/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.io.paths.MCPath;
import umlp.generator.gui.genjava.GUI2CDTrafo;
import umlp.generator.gui.genjava.GUI2JavaGenerator;
import umlp.generator.gui.gentypescript.printer.TSUtilsPrinter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GUI2TSGenerator extends GUI2JavaGenerator {

  protected static final String TS_EXTENSION = ".ts";
  protected final MCPath importsPath;

  public GUI2TSGenerator(GeneratorSetup genSetup, MCPath importsPath) {
    super(genSetup);
    genSetup.getGlex().setGlobalValue("cdPrinter", new TSUtilsPrinter());
    this.importsPath = importsPath;
  }

  protected Path getAsPath(String packageAsPath, String name) {
    return Paths.get(packageAsPath, name + TS_EXTENSION);
  }

  @Override
  public void generate(ASTGUICompilationUnit gui, String configTemplate) {
    this.setup.setTracing(true);
    try {
      Reporting.addConfigTemplate(configTemplate);
      // create an empty classdiagram
      ASTCDCompilationUnit cd = GUI2CDTrafo.createCDCompilationUnit(gui);

      TemplateController tc = setup.getNewTemplateController(configTemplate);
      TemplateHookPoint hpp = new TemplateHookPoint(configTemplate);

      // translate the GUI-AST into TypeScript
      hpp.processValue(tc, gui, Arrays.asList(
          cd, setup.getGlex(), setup.getHandcodedPath(), setup.getOutputDirectory().toPath(), importsPath, this));
    }finally {
      Reporting.clearConfigTemplates();
    }
  }

}
