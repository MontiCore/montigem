/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genhtml;

import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import umlp.generator.gui.GUIGeneratorUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GUI2HtmlGenerator {

  protected static final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();

  protected final GeneratorEngine generatorEngine;
  protected GeneratorSetup setup;

  public GUI2HtmlGenerator(GeneratorSetup generatorSetup) {
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    this.setup = generatorSetup;
  }

  public void generate(ASTGUICompilationUnit ast, String configTemplate) {
    try {
      Reporting.addConfigTemplate(configTemplate);
      TemplateController tc = setup.getNewTemplateController(configTemplate);
      TemplateHookPoint hpp = new TemplateHookPoint(configTemplate);

      setup.getGlex().setGlobalValue("htmlHelper", new HTMLHelper(ast));

      // translate the GUI-AST into HTML
      hpp.processValue(tc, ast,
          Arrays.asList(ast, setup.getGlex(), setup.getHandcodedPath(), this));
    }finally {
      Reporting.clearConfigTemplates();
    }
  }

  public void generate(ASTGUICompilationUnit decoratedAst) {
    this.setup.setTracing(false);
    setup.getGlex().setGlobalValue("htmlHelper",  new HTMLHelper(decoratedAst));

    String componentName = decoratedAst.getComponentName();
    String pathName = util.getGenFileFolder(decoratedAst.getMCPackageDeclaration());
    Path filePath = Paths.get(pathName, componentName + ".html");

    GeneratorEngine e = new GeneratorEngine(setup);
    e.generate("umlp.gui2html.Page", filePath, decoratedAst);
  }

}
