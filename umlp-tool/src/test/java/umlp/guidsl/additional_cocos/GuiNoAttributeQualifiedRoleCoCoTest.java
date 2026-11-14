/* (c) https://github.com/MontiCore/monticore */
package umlp.guidsl.additional_cocos;

import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.generating.GeneratorSetup;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._cocos.GUIDSLCoCoChecker;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.coco.GuiNoAttributeQualifiedRoleCoCo;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiNoAttributeQualifiedRoleCoCoTest extends DomainGUITest {

  @BeforeEach
  public void genSetup() {
    GeneratorSetup genSetup = new GeneratorSetup();
    genSetup.setOutputDirectory(new File(BUILD_FOLDER + "/guigentests"));
    genSetup.setGlex(createGlex());
    genSetup.getGlex().setGlobalValue("cdPrinter", new CdUtilsPrinter());
    CD4C.init(genSetup);
  }

  protected List<Finding> check(String model) {
    ASTGUICompilationUnit gui = processGUIModel(model, "src/test/resources/gui", "packaged/QualifiedDomain.umlp");
    GUIGeneratorUtil.getInstance().init(List.of("packaged.QualifiedDomain"), Collections.emptyList());
    Log.clearFindings();

    GUIDSLCoCoChecker checker = new GUIDSLCoCoChecker();
    checker.addCoCo(new GuiNoAttributeQualifiedRoleCoCo());
    checker.checkAll(gui);
    return Log.getFindings().stream().filter(Finding::isError).collect(Collectors.toList());
  }

  @Test
  public void testUnqualifiedRole() {
    assertTrue(check("gui/coco/GuiNoAttributeQualifiedRoleCoCo1.gui").isEmpty());
  }

  @Test
  public void testOptionalMapRole() {
    List<Finding> errors = check("gui/coco/GuiNoAttributeQualifiedRoleCoCo2.gui");
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).getMsg().startsWith(GuiNoAttributeQualifiedRoleCoCo.ERROR_CODE));
    assertTrue(errors.get(0).getMsg().contains("'byNumber'"));
    assertTrue(errors.get(0).getMsg().contains("getByNumberMap()"));
  }

  @Test
  public void testMultiMapRole() {
    List<Finding> errors = check("gui/coco/GuiNoAttributeQualifiedRoleCoCo3.gui");
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).getMsg().contains("'byLogin'"));
  }
}
