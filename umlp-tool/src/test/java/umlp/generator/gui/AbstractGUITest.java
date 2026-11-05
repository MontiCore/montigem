/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui;

import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.guidsl.GUIDSLTool;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._parser.GUIDSLParser;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.BeforeEach;
import umlp.generator.AbstractDecorator;
import umlp.AbstractUMLPTest;

import java.util.Optional;

public abstract class AbstractGUITest extends AbstractUMLPTest {

  @BeforeEach
  public void guiSetup() {
    LogStub.init();
    Log.enableFailQuick(false);
    Log.clearFindings();
    GUIDSLTool tool = new GUIDSLTool();
    tool.resetGSAndMill();
  }

  protected static GlobalExtensionManagement createGlex() {
    GlobalExtensionManagement glex = new GlobalExtensionManagement();
    glex.setBeforeTemplate("cd2java.Class", AbstractDecorator.getOriginHookPoint());
    glex.setBeforeTemplate("cd2java.Enum", AbstractDecorator.getOriginHookPoint());
    glex.setBeforeTemplate("cd2java.Interface", AbstractDecorator.getOriginHookPoint());
    glex.setGlobalValue("genHelper", GUIGeneratorUtil.getInstance());
    return glex;
  }

  public static ASTGUICompilationUnit parseGUI(String model) {
    return parseGUI("src/test/resources/", model);
  }

  public static ASTGUICompilationUnit parseGUI(String resources, String model) {
    try {
      GUIDSLParser parser = new GUIDSLParser();
      Optional<ASTGUICompilationUnit> optAst = parser.parse(resources + model);

      if (!parser.hasErrors() && optAst.isPresent()) {
        return optAst.get();
      }
      Log.error("0xA1050x72114 Model could not be parsed.");
    } catch (NullPointerException e) {
      Log.error("0xA1051x37376 Failed to parse " + model, e);
    }
    // should never be reached (unless failquick is off)
    return null;
  }

}
