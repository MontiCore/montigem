package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLPSingletonClassParametersTest extends AbstractUMLPTest {
  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPCheckParameterOfCommand());
    ASTCDCompilationUnit ast = parse("cocoinvalid/UMLPSingletonClassParameters.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    checker.checkAll(ast);

    assertEquals(3, Log.getErrorCount());
    assertTrue(Log.getFindings().stream().anyMatch(f -> f.getMsg().startsWith(UMLPCheckParameterOfCommand.ERROR_CODE)));
  }
}
