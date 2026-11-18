/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByAttributeNameTest extends AbstractCoCoTest<UMLPAssociationQualifierByAttributeName> {

  UMLPAssociationQualifierByAttributeNameTest() {
    super(new UMLPAssociationQualifierByAttributeName(), UMLPCoCoChecker::addCoCo);
  }

  /**
   * List, Optional and Set attributes (no key or several keys per value object), an enum attribute and float, double
   * and char attributes
   */
  @Test
  public void testInvalidQAttrType() {
    parseInvalid("UMLPAssocQualifierComplexAttrType");

    assertFindingAtLine(0, 18, "names");
    assertFindingAtLine(1, 19, "optName");
    assertFindingAtLine(2, 20, "ids");
    assertFindingAtLine(3, 21, "e");
    // Java and the browser would disagree on floating point and char keys
    assertFindingAtLine(4, 22, "weight");
    assertFindingAtLine(5, 23, "ratio");
    assertFindingAtLine(6, 24, "initial");
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
