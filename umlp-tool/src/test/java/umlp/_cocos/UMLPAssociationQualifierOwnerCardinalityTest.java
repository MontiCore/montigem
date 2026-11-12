/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierOwnerCardinalityTest extends AbstractCoCoTest<UMLPAssociationQualifierOwnerCardinality> {

  UMLPAssociationQualifierOwnerCardinalityTest() {
    super(new UMLPAssociationQualifierOwnerCardinality(), UMLPCoCoChecker::addCoCo);
  }

  /**
   * bidirectional without the cardinality [1], [0..1] or [*] of the qualified class, attribute and type qualified,
   * with the qualifier on either side
   */
  @Test
  public void testInvalidOwnerCardinality() {
    parseInvalid("UMLPAssocQualifierNavigability");

    assertFindingAtLine(0, 18, "[[name]]", "B");
    assertFindingAtLine(1, 22, "[[number]]", "A");
    assertFindingAtLine(2, 26, "[String]", "B");
    assertFindingAtLine(3, 30, "[int]", "A");
    assertFindingAtLine(4, 32, "[[name]]", "B");
    assertFindingsCount(5);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
