/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPMapAttributeKeyTypeTest extends AbstractCoCoTest<UMLPMapAttributeKeyType> {

  UMLPMapAttributeKeyTypeTest() {
    super(new UMLPMapAttributeKeyType(), UMLPCoCoChecker::addCoCo);
  }

  /**
   * Map attributes with keys a type qualified association does not support (floating point, boolean, enum) and a
   * domain class key with simple values
   */
  @Test
  public void testInvalidKeyType() {
    parseInvalid("UMLPMapAttribute");

    assertFindingAtLine(0, 9, "Double", "byWeight");
    assertFindingAtLine(1, 10, "Boolean", "byFlag");
    assertFindingAtLine(2, 11, "E", "byKind");
    assertFindingAtLine(3, 12, "Double", "countByWeight");
    assertFindingAtLine(4, 13, "C", "countByC");
    assertFindingAtLine(5, 14, "Boolean", "countByFlag");
    assertFindingAtLine(6, 15, "E", "countByKind");
    assertFindingsCount(7);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
