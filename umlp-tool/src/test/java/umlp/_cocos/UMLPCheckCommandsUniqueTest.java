/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLPCheckCommandsUniqueTest extends AbstractUMLPTest {

  @Test
  public void testValid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPCheckCommandUnique());
    ASTCDCompilationUnit ast = parse("cocovalid/Valid.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);

    checker.checkAll(ast);

    assertTrue(Log.getFindings().isEmpty());
  }

  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPCheckCommandUnique());
    ASTCDCompilationUnit ast = parse("cocoinvalid/UMLPCheckDuplicateCommands.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    checker.checkAll(ast);

    assertEquals(2, Log.getFindingsCount());
    Log.getFindings().forEach(f -> assertTrue(f.getMsg().startsWith(UMLPCheckCommandUnique.ERROR_CODE)));
  }

}
