/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierNavigabilityTest extends AbstractCoCoTest<UMLPAssociationQualifierNavigability> {

  UMLPAssociationQualifierNavigabilityTest() {
    super(new UMLPAssociationQualifierNavigability(), UMLPCoCoChecker::addCoCo);
  }

  /** not navigable from the qualified class, attribute and type qualified */
  @Test
  public void testInvalidDirections() {
    parseInvalid("UMLPAssocQualifierNavigability");

    assertFindingAtLine(0, 20, "[[name]]", "B");
    assertFindingAtLine(1, 28, "[A]", "B");
    assertFindingsCount(2);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
