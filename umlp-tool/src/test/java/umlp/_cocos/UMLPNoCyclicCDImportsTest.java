/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UMLPNoCyclicCDImportsTest extends AbstractUMLPTest {

  @Test
  public void testValid() {
    List<ASTCDCompilationUnit> asts = parseWithSymTab("Base", "Other", "Extension");

    check(asts);

    assertNoErrors();
  }

  @Test
  public void testInvalid() {
    List<ASTCDCompilationUnit> asts = parseWithSymTab("CycleA", "CycleB", "CycleC", "Base");

    check(asts);

    assertEquals(3, Log.getErrorCount());
    assertEquals(UMLPNoCyclicCDImports.ERROR_CODE + " " + String.format(UMLPNoCyclicCDImports.ERROR_MSG_FORMAT,
        "CycleA", "CycleA -> CycleB -> CycleC -> CycleA"),
        Log.getFindings().stream().filter(Finding::isError).findFirst().orElseThrow().getMsg());
  }

  protected List<ASTCDCompilationUnit> parseWithSymTab(String... models) {
    List<ASTCDCompilationUnit> asts = List.of(models).stream().map(m -> parse("cdimports/" + m + ".umlp")).toList();
    asts.forEach(this::createSymTab);
    return asts;
  }

  protected void check(List<ASTCDCompilationUnit> asts) {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPNoCyclicCDImports());
    asts.forEach(checker::checkAll);
  }
}
