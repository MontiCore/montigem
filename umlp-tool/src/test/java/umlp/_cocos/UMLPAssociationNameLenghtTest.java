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
    checker.addCoCo(new UMLPAssociationNameLength());
    ASTCDCompilationUnit ast = parse("cocoinvalid/CDAssociationNameLengthToLongCase.umlp");
    runAfterParseTrafo(ast);
    runDefaultTrafos(ast);
    createSymTab(ast);
    checker.checkAll(ast);

    assertTrue(Log.getFindings().getFirst().getMsg().startsWith(UMLPAssociationNameLength.ERROR_CODE));
  }
}
