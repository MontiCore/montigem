/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierUnidirectionalTest extends AbstractCoCoTest<UMLPAssociationQualifierUnidirectional> {

  UMLPAssociationQualifierUnidirectionalTest() {
    super(new UMLPAssociationQualifierUnidirectional(), UMLPCoCoChecker::addCoCo);
  }

  /** bidirectional or not navigable from the qualified class, with the qualifier on either side */
  @Test
  public void testInvalidDirections() {
    parseInvalid("UMLPAssocQualifierNavigability");

    assertFindingAtLine(0, 19, "name", "B");
    assertFindingAtLine(1, 21, "name", "B");
    assertFindingAtLine(2, 23, "number", "A");
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
