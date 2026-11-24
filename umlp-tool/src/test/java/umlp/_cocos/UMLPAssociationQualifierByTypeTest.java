/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByTypeTest extends AbstractCoCoTest<UMLPAssociationQualifierByType> {

  UMLPAssociationQualifierByTypeTest() {
    super(new UMLPAssociationQualifierByType(), UMLPCoCoChecker::addCoCo);
  }

  /**
   * boolean, enum, floating point, char, Optional and collection keys are rejected; domain class, String and integral
   * keys are valid
   */
  @Test
  public void testInvalid() {
    parseInvalid("UMLPAssocQualifierComplexType");
    assertFindingAtLine(0, 16, "boolean");
    assertFindingAtLine(1, 17, "E");
    assertFindingAtLine(2, 19, "double");
    assertFindingAtLine(3, 20, "Float");
    assertFindingAtLine(4, 21, "char");
    assertFindingAtLine(5, 22, "Character");
    assertFindingAtLine(6, 24, "Optional");
    assertFindingAtLine(7, 25, "List");
    assertFindingsCount(8);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }

}
