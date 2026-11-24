/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPMapAttributeValueTypeTest extends AbstractCoCoTest<UMLPMapAttributeValueType> {

  UMLPMapAttributeValueTypeTest() {
    super(new UMLPMapAttributeValueType(), UMLPCoCoChecker::addCoCo);
  }

  /** Map attributes whose values are no domain class objects: a number, an enum, a list */
  @Test
  public void testInvalidValueType() {
    parseInvalid("UMLPMapAttribute");

    assertFindingAtLine(0, 5, "counts", "Integer");
    assertFindingAtLine(1, 6, "kinds", "E");
    assertFindingAtLine(2, 7, "lists", "List<B>");
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
