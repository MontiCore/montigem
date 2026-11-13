/* (c) https://github.com/MontiCore/monticore */
package umlp;

import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import umlp.generator.domain.UMLAttrToAssocTrafo;
import umlp.generator.domain.UMLImportTrafo;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLAttrToAssocTrafoTest extends AbstractUMLPTest {

  private static Stream<String> transformAttributes(){
    return Stream.of("trafo/CardOne.umlp", "trafo/CardOpt.umlp", "trafo/CardMult.umlp");
  }

  @ParameterizedTest
  @MethodSource("transformAttributes")
  public void testCard(String modelName) {
    ASTCDCompilationUnit ast = parse("src/test/resources", modelName);

    UMLImportTrafo importTrafo = new UMLImportTrafo(CD4CodeMill.inheritanceTraverser());
    importTrafo.transform(ast);

    createSymTab(ast);

    // Before
    assertAll(
        () -> assertEquals(0, ast.getCDDefinition().getCDAssociationsList().size())
    );

    UMLAttrToAssocTrafo trafo = new UMLAttrToAssocTrafo(CD4CodeMill.inheritanceTraverser());
    trafo.transform(ast);

    // After
    assertAll(
        () -> assertEquals(1, ast.getCDDefinition().getCDAssociationsList().size()),
        () -> ast.getCDDefinition().getCDClassesList().forEach(
            c -> assertTrue(c.getCDAttributeList().isEmpty()))
    );
  }


  /** Map&lt;K, V&gt; with a domain class value becomes the type qualified association Owner [K] -> (attr) V [1] */
  @Test
  public void testMap() {
    ASTCDCompilationUnit ast = parse("src/test/resources", "trafo/CardMap.umlp");

    UMLImportTrafo importTrafo = new UMLImportTrafo(CD4CodeMill.inheritanceTraverser());
    importTrafo.transform(ast);

    createSymTab(ast);

    UMLAttrToAssocTrafo trafo = new UMLAttrToAssocTrafo(CD4CodeMill.inheritanceTraverser());
    trafo.transform(ast);

    var assocs = ast.getCDDefinition().getCDAssociationsList();
    assertEquals(2, assocs.size());
    for (var assoc : assocs) {
      // qualified on the owner side, navigable from the owner, one value per key
      assertEquals("Foo", assoc.getLeft().getMCQualifiedType().getMCQualifiedName().getQName());
      assertTrue(assoc.getLeft().isPresentCDQualifier() && assoc.getLeft().getCDQualifier().isPresentByType());
      assertFalse(assoc.getLeft().isPresentCDCardinality());
      assertTrue(assoc.getCDAssocDir().isDefinitiveNavigableRight());
      assertFalse(assoc.getCDAssocDir().isDefinitiveNavigableLeft());
      assertTrue(assoc.getRight().getCDCardinality().isOne());
      assertTrue(assoc.getRight().getMCQualifiedType().getMCQualifiedName().getQName().endsWith("Bar"));
    }
    assertEquals("byName", assocs.get(0).getRight().getCDRole().getName());
    assertEquals("String", assocs.get(0).getLeft().getCDQualifier().getByType().printType());
    assertEquals("byBaz", assocs.get(1).getRight().getCDRole().getName());
    assertEquals("Baz", assocs.get(1).getLeft().getCDQualifier().getByType().printType());

    // the map without a domain class value stays an attribute
    var foo = ast.getCDDefinition().getCDClassesList().stream().filter(c -> c.getName().equals("Foo")).findFirst()
        .get();
    assertEquals(1, foo.getCDAttributeList().size());
    assertEquals("counts", foo.getCDAttributeList().get(0).getName());
  }

  @Test
  public void testNoTransformation() {
    ASTCDCompilationUnit ast = parse("src/test/resources", "trafo/NoTransformation.umlp");

    UMLImportTrafo importTrafo = new UMLImportTrafo(CD4CodeMill.inheritanceTraverser());
    importTrafo.transform(ast);

    createSymTab(ast);

    UMLAttrToAssocTrafo trafo = new UMLAttrToAssocTrafo(CD4CodeMill.inheritanceTraverser());
    trafo.transform(ast);

    assertEquals(0, ast.getCDDefinition().getCDAssociationsList().size());
    ast.getCDDefinition().getCDClassesList().forEach(
            c -> assertEquals(4, c.getCDAttributeList().size()));
  }

  @Test
  public void testMultipleUsesOfDomainClass() {
    ASTCDCompilationUnit ast = parse("src/test/resources", "trafo/MultipleUsesOfDomainClass.umlp");

    UMLImportTrafo importTrafo = new UMLImportTrafo(CD4CodeMill.inheritanceTraverser());
    importTrafo.transform(ast);

    createSymTab(ast);

    UMLAttrToAssocTrafo trafo = new UMLAttrToAssocTrafo(CD4CodeMill.inheritanceTraverser());
    trafo.transform(ast);

    assertEquals(2, ast.getCDDefinition().getCDAssociationsList().size());
  }


}
