/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;
import umlp.UMLPMill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The key of a GemMap given as a domain typed attribute of the target and as the role of a to-one association of the
 * target are the same to the generator: the attribute becomes such an association (UMLAttrToAssocTrafo)
 */
public class QualifierKeyTest extends AbstractUMLPTest {

  @Test
  public void testKeyViaAttributeEqualsKeyViaAssociation() {
    ASTCDCompilationUnit ast = parse("gemmap/QualifierKeyForms.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    runDefaultTrafos(ast);
    // like UMLPTool#recreateSymbolTable after the trafos
    UMLPMill.globalScope().removeSubScope(ast.getEnclosingScope());
    createSymTab(ast);

    QualifierKey byAttribute = QualifierKey.of(targetSide(ast, "byStudent"));
    QualifierKey byRole = QualifierKey.of(targetSide(ast, "byTutor"));

    assertTrue(byAttribute.isEntryKey());
    assertEquals(byAttribute.isEntryKey(), byRole.isEntryKey());
    assertEquals("GemEntryMap", byRole.getMapType());
    assertEquals("Student", byAttribute.getKeyType());
    assertEquals(byAttribute.getKeyType(), byRole.getKeyType());
    assertEquals(byAttribute.getStoredKeyType(), byRole.getStoredKeyType());
    assertEquals(byAttribute.getNotifyType(), byRole.getNotifyType());
    assertEquals(byAttribute.getStoredKeyJson(), byRole.getStoredKeyJson());
    assertEquals("Enrollment", byAttribute.getDeclaringType().getName());
    assertEquals(byAttribute.getDeclaringType(), byRole.getDeclaringType());
    assertEquals("e.getStudentGem().getGemId()", byAttribute.getKeyExpression("e"));
    assertEquals("e.getTutorGem().getGemId()", byRole.getKeyExpression("e"));
    assertNoErrors();
  }

  /**
   * @return the side of Course's association with the role
   */
  protected ASTCDAssocSide targetSide(ASTCDCompilationUnit ast, String role) {
    ASTCDClass course = ast.getCDDefinition().getCDClassesList().stream()
        .filter(c -> c.getName().equals("Course")).findFirst().get();
    return CDSymbolTables.getAssociations(course).stream()
        .filter(side -> side.getName().equals(role)).findFirst().get();
  }
}
