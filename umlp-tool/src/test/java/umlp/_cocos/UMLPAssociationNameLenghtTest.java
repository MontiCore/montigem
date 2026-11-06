package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLPAssociationNameLenghtTest extends AbstractUMLPTest {
  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new PostgresTableNameLengthCoCo());
    ASTCDCompilationUnit ast = parse("cocoinvalid/CDNameLengthToLongCase.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    runDefaultTrafos(ast);
    recreateSymbolTable(ast);

    checker.checkAll(ast);

    System.out.println(Log.getFindings());

    assertTrue(Log.getFindings().getFirst().getMsg().startsWith(PostgresTableNameLengthCoCo.ERROR_CODE));
  }
}
