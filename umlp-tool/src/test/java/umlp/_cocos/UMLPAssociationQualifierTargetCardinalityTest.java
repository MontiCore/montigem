/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierTargetCardinalityTest extends AbstractCoCoTest<UMLPAssociationQualifierTargetCardinality> {

  UMLPAssociationQualifierTargetCardinalityTest() {
    super(new UMLPAssociationQualifierTargetCardinality(), UMLPCoCoChecker::addCoCo);
  }

  /** the value side has another cardinality than [1] or none, on both sides of the association */
  @Test
  public void testInvalidTargetCardinality() {
    parseInvalid("UMLPAssocQualifierTargetCardinality");

    assertFindingAtLine(0, 10, "A", "name");
    assertFindingAtLine(1, 11, "A", "name");
    assertFindingAtLine(2, 12, "A", "name");
    assertFindingAtLine(3, 13, "A", "name");
    assertFindingAtLine(4, 14, "A", "name");
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
