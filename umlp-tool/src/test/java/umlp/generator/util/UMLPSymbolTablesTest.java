/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.util;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.symbols.basicsymbols._symboltable.DiagramSymbol;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UMLPSymbolTablesTest extends AbstractUMLPTest {

  @Test
  public void testExtendedCDsFromStarAndTypeImports() {
    createSymTab(parse("cdimports/Base.umlp"));
    createSymTab(parse("cdimports/Other.umlp"));
    ASTCDCompilationUnit extension = parse("cdimports/Extension.umlp");
    createSymTab(extension);

    List<String> extendedCDs = UMLPSymbolTables.getExtendedCDs(extension).stream()
        .map(DiagramSymbol::getFullName).toList();

    assertEquals(List.of("Base", "Other"), extendedCDs);
    assertNoErrors();
  }
}
