/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPMapAttributeValueTypeTest extends AbstractCoCoTest<UMLPMapAttributeValueType> {

  UMLPMapAttributeValueTypeTest() {
    super(new UMLPMapAttributeValueType(), UMLPCoCoChecker::addCoCo);
  }

  /** Map attributes whose values are neither domain class objects nor simple types: a list, a map, a char */
  @Test
  public void testInvalidValueType() {
    parseInvalid("UMLPMapAttribute");

    assertFindingAtLine(0, 5, "lists", "List<B>");
    assertFindingAtLine(1, 6, "maps", "Map<String,Integer>");
    assertFindingAtLine(2, 24, "initials", "Character");
    assertFindingAtLine(3, 25, "letters", "char");
    assertFindingsCount(4);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
