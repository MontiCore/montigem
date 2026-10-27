/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierKeyCardinalityTest extends AbstractCoCoTest<UMLPAssociationQualifierKeyCardinality> {

  UMLPAssociationQualifierKeyCardinalityTest() {
    super(new UMLPAssociationQualifierKeyCardinality(), UMLPCoCoChecker::addCoCo);
  }

  /** optional and multi-valued qualifiers, as attributes and as association roles, on both sides */
  @Test
  public void testInvalidKeyCardinality() {
    parseInvalid("UMLPAssocQualifierKeyCardinality");

    assertFindingAtLine(0, 13, "optName", "A");
    assertFindingAtLine(1, 14, "names", "A");
    assertFindingAtLine(2, 15, "optC", "A");
    assertFindingAtLine(3, 16, "manyC", "A");
    assertFindingAtLine(4, 17, "someC", "A");
    assertFindingAtLine(5, 18, "manyC", "A");
    assertFindingsCount(6);
  }

  /** single valued qualifiers: attributes and an association role */
  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
