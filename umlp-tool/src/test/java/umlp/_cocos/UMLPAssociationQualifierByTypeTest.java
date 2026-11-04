/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByTypeTest extends AbstractCoCoTest<UMLPAssociationQualifierByType> {

  UMLPAssociationQualifierByTypeTest() {
    super(new UMLPAssociationQualifierByType(), UMLPCoCoChecker::addCoCo);
  }

  /** boolean, enum, floating point and char keys are rejected; domain class, String and integral keys are valid */
  @Test
  public void testInvalid() {
    parseInvalid("UMLPAssocQualifierComplexType");
    assertFindingAtLine(0, 15, "boolean");
    assertFindingAtLine(1, 16, "E");
    assertFindingAtLine(2, 17, "double");
    assertFindingAtLine(3, 18, "Float");
    assertFindingAtLine(4, 19, "char");
    assertFindingAtLine(5, 20, "Character");
    assertFindingsCount(6);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }

}
