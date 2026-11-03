/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierUnidirectionalTest extends AbstractCoCoTest<UMLPAssociationQualifierUnidirectional> {

  UMLPAssociationQualifierUnidirectionalTest() {
    super(new UMLPAssociationQualifierUnidirectional(), UMLPCoCoChecker::addCoCo);
  }

  @Test
  public void testInvalidDirections() {
    parseInvalid("UMLPAssocQualifierBidirectional");

    assertFindingAtLine(0, 12, "name", "B");
    assertFindingAtLine(1, 13, "name", "B");
    assertFindingAtLine(2, 14, "number", "A");
    assertFindingsCount(3);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
