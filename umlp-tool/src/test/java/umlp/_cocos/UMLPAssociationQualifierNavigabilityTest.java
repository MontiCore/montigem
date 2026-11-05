/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierNavigabilityTest extends AbstractCoCoTest<UMLPAssociationQualifierNavigability> {

  UMLPAssociationQualifierNavigabilityTest() {
    super(new UMLPAssociationQualifierNavigability(), UMLPCoCoChecker::addCoCo);
  }

  /**
   * not navigable from the qualified class (0xCDC6A), or bidirectional without the cardinality [1], [0..1] or [*] of
   * the qualified class (0xCDC6D)
   */
  @Test
  public void testInvalidDirections() {
    parseInvalid("UMLPAssocQualifierBidirectional");

    assertOwnerCardinalityFindingAtLine(0, 12, "[[name]]", "B");
    assertFindingAtLine(1, 13, "[[name]]", "B");
    assertOwnerCardinalityFindingAtLine(2, 14, "[[number]]", "A");
    assertOwnerCardinalityFindingAtLine(3, 16, "[String]", "B");
    assertFindingAtLine(4, 17, "[A]", "B");
    assertOwnerCardinalityFindingAtLine(5, 18, "[int]", "A");
    assertOwnerCardinalityFindingAtLine(6, 19, "[[name]]", "B");
    assertFindingsCount(7);
  }

  protected void assertOwnerCardinalityFindingAtLine(int findingIndex, int atLine, Object... args) {
    assertFindingAtLine(findingIndex, getExpectedErrorMessage(
        UMLPAssociationQualifierNavigability.OWNER_CARDINALITY_ERROR_CODE,
        UMLPAssociationQualifierNavigability.OWNER_CARDINALITY_ERROR_MSG_FORMAT, args), atLine);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
