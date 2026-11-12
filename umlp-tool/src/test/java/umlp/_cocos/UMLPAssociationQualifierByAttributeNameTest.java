/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;

public class UMLPAssociationQualifierByAttributeNameTest extends AbstractCoCoTest<UMLPAssociationQualifierByAttributeName> {

  UMLPAssociationQualifierByAttributeNameTest() {
    super(new UMLPAssociationQualifierByAttributeName(), UMLPCoCoChecker::addCoCo);
  }

  /** List, Optional and Set attributes (no key or several keys per value object) and an enum attribute */
  @Test
  public void testInvalidQAttrType() {
    parseInvalid("UMLPAssocQualifierComplexAttrType");

    assertFindingAtLine(0, 15, "names");
    assertFindingAtLine(1, 16, "optName");
    assertFindingAtLine(2, 17, "ids");
    assertFindingAtLine(3, 18, "e");
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
