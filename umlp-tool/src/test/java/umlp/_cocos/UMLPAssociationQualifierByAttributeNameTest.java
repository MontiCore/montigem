/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByAttributeNameTest extends AbstractCoCoTest<UMLPAssociationQualifierByAttributeName> {

  UMLPAssociationQualifierByAttributeNameTest() {
    super(new UMLPAssociationQualifierByAttributeName(), UMLPCoCoChecker::addCoCo);
  }

  @Test
  public void testInvalidQAttrType() {
    parseInvalid("UMLPAssocQualifierComplexAttrType");

    // the List names (line 16) is multi-valued, see UMLPAssociationQualifierKeyCardinalityTest
    assertFindingAtLine(0, 17, "e");
    // Java and the browser would disagree on floating point and char keys
    assertFindingAtLine(1, 18, "weight");
    assertFindingAtLine(2, 19, "ratio");
    assertFindingAtLine(3, 20, "initial");
    assertFindingsCount(4);
  }

  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }

  @Test
  void testInvalidNoAttr() {
    parseInvalid("UMLPAssocQualifierNoAttr");
    assertFindingAtLine(0,
                        getExpectedErrorMessage(UMLPAssociationQualifierByAttributeName.NO_ATTR_ERROR_CODE,
                                                UMLPAssociationQualifierByAttributeName.NO_ATTR_ERROR_MSG_FORMAT,
                                                "noMatchingAttr",
                                                "B"),
                        13);
    assertFindingsCount(1);
  }
}
