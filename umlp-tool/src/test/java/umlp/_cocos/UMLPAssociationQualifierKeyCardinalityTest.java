/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierKeyCardinalityTest extends AbstractCoCoTest<UMLPAssociationQualifierKeyCardinality> {

  UMLPAssociationQualifierKeyCardinalityTest() {
    super(new UMLPAssociationQualifierKeyCardinality(), UMLPCoCoChecker::addCoCo);
  }

  /** optional and multi-valued association roles as qualifiers, on both sides */
  @Test
  public void testInvalidKeyCardinality() {
    parseInvalid("UMLPAssocQualifierKeyCardinality");

    assertFindingAtLine(0, 12, "optC", "A");
    assertFindingAtLine(1, 13, "manyC", "A");
    assertFindingAtLine(2, 14, "someC", "A");
    assertFindingAtLine(3, 15, "manyC", "A");
    assertFindingsCount(4);
  }

  /** single valued qualifiers: attributes (no cardinality) and an association role with cardinality [1] */
  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
