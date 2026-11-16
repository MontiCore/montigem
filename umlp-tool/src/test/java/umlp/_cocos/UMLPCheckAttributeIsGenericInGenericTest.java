package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLPCheckAttributeIsGenericInGenericTest extends AbstractUMLPTest {

  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPAttributeGenericArgumentInGeneric());

    ASTCDCompilationUnit ast = parse("cocoinvalid/UMLPAttributeGenericInGeneric.umlp");

    runAfterParseTrafo(ast);
    createSymTab(ast);

    checker.checkAll(ast);

    assertTrue(Log.getFindings().getFirst().getMsg().startsWith(UMLPAttributeGenericArgumentInGeneric.ERROR_CODE));
  }

  @Test
  public void testValid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    checker.addCoCo(new UMLPAttributeGenericArgumentInGeneric());

    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAttributeGenericNormalCase.umlp");

    runAfterParseTrafo(ast);
    createSymTab(ast);

    checker.checkAll(ast);

    assertNoErrors();
  }
}
