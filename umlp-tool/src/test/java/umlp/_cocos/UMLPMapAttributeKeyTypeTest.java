/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPMapAttributeKeyTypeTest extends AbstractCoCoTest<UMLPMapAttributeKeyType> {

  UMLPMapAttributeKeyTypeTest() {
    super(new UMLPMapAttributeKeyType(), UMLPCoCoChecker::addCoCo);
  }

  /** Map attributes with keys a type qualified association does not support: floating point, boolean, enum */
  @Test
  public void testInvalidKeyType() {
    parseInvalid("UMLPMapAttribute");

    assertFindingAtLine(0, 9, "Double", "byWeight");
    assertFindingAtLine(1, 10, "Boolean", "byFlag");
    assertFindingAtLine(2, 11, "E", "byKind");
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
