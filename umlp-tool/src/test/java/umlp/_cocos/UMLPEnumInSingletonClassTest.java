package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UMLPEnumInSingletonClassTest extends AbstractUMLPTest {
  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPCheckParameterOfCDEnum());
    ASTCDCompilationUnit ast = parse("cocoinvalid/CDEnumAsParameterFromSingletonClass.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    checker.checkAll(ast);

    assertEquals(true, Log.getFindings().stream().anyMatch(f -> f.getMsg().startsWith(UMLPCheckParameterOfCDEnum.ERROR_CODE)));
  }
}
