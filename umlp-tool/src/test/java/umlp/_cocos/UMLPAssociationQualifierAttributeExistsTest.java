/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierAttributeExistsTest extends AbstractCoCoTest<UMLPAssociationQualifierAttributeExists> {

  UMLPAssociationQualifierAttributeExistsTest() {
    super(new UMLPAssociationQualifierAttributeExists(), UMLPCoCoChecker::addCoCo);
  }

  /** the qualifier names neither an attribute of the value class nor a role navigable from it */
  @Test
  void testInvalidNoAttr() {
    parseInvalid("UMLPAssocQualifierNoAttr");
    assertFindingAtLine(0,
                        getExpectedErrorMessage(UMLPAssociationQualifierAttributeExists.ERROR_CODE,
                                                UMLPAssociationQualifierAttributeExists.ERROR_MSG_FORMAT,
                                                "noMatchingAttr",
                                                "B"),
                        13);
    assertFindingsCount(1);
  }

  /** qualifiers that are attributes of the value class or roles of associations navigable from it */
  @Test
  public void testValidKeys() {
    ASTCDCompilationUnit ast = parse("cocovalid/UMLPAssocQualifierKeys.umlp");
    createSymTab(ast);
    checker.checkAll(ast);
    assertNoErrors();
  }
}
