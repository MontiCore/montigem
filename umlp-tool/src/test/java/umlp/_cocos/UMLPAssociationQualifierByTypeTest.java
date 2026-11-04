/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByTypeTest extends AbstractCoCoTest<UMLPAssociationQualifierByType> {

  UMLPAssociationQualifierByTypeTest() {
    super(new UMLPAssociationQualifierByType(), UMLPCoCoChecker::addCoCo);
  }

  /** boolean and enum keys are rejected; domain class, String and numeric keys are valid */
  @Test
  public void testInvalid() {
    parseInvalid("UMLPAssocQualifierComplexType");
    assertFindingAtLine(0, 15, "boolean");
    assertFindingAtLine(1, 16, "E");
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
