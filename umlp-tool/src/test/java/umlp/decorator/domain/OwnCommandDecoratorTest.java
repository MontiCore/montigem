package umlp.decorator.domain;

import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.tagging.SimpleSymbolTagger;
import de.monticore.tagging.TagRepository;
import de.monticore.tagging.tags.TagsMill;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import org.junit.jupiter.api.Test;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.decorator.commands.OwnCommandDecorator;
import umlp.UMLPMill;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class OwnCommandDecoratorTest extends DecoratorTest {
  @Override
  protected String getModel() {
    return "de/monticore/E.umlp";
  }

  @Override
  protected AbstractDecorator getDecorator(GlobalExtensionManagement glex) {
    Set<String> supportedDeSers = Set.of();
    return new OwnCommandDecorator(glex, supportedDeSers);
  }

  @Test
  public void checkImports() {
    ASTCDClass clazz = getClassByName("Baz");
    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);
    assertAll(
        // check that java types are imported correctly
        () -> assertTrue(imports.stream().anyMatch(
            statement -> statement.getQName().equals("java.time.LocalDateTime")))
    );
  }

  @Test
  public void checkAttributes() {
    ASTCDClass clazz = getClassByName("Baz");
    assertAll(
        // domain type params should generate a ...gemId attribute
        () -> existsAttribute(clazz, "domainParamGemId"),
        () -> existsAttribute(clazz, "enumParamGemId"),

        // java type param
        () -> existsAttribute(clazz, "javaParam")

    );
  }

  @Test
  public void importsDependencyWithoutAst() {
    CDTypeSymbol symbol = UMLPMill.cDTypeSymbolBuilder().setName("Function4Display").build();
    symbol.setFullName("mc.fenix.functiondisplay.FunctionStructure4Fenix.Function4Display");
    assertFalse(symbol.isPresentAstNode());
    ASTCDClass command = getClassByName("Baz");
    new ImportDecorator().importType(SymTypeExpressionFactory.createTypeObject(symbol), command);
    assertTrue(CD4C.getInstance().getImportList(command).stream().anyMatch(statement ->
        statement.getQName().equals("mc.fenix.functiondisplay.functionstructure4fenix.Function4Display")));
    assertFalse(CD4C.getInstance().getImportList(command).stream().anyMatch(statement ->
        statement.getQName().equals("mc.fenix.functiondisplay.functionstructure4fenix.Function4DisplayAccessPolicy")));
  }

  @Test
  public void importsPojoOutsideFenixWithoutAccessPolicy() {
    TagsMill.init();
    TagRepository.loadTempTagModel("CommandImportTest");
    CDTypeSymbol symbol = UMLPMill.cDTypeSymbolBuilder().setName("Display").build();
    symbol.setFullName("example.CustomGUI.Display");
    new SimpleSymbolTagger().addTag(symbol, TagsMill.simpleTagBuilder().setName("pojo").build());
    ASTCDClass command = getClassByName("Baz");
    new ImportDecorator().importType(SymTypeExpressionFactory.createTypeObject(symbol), command);
    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(command);
    assertTrue(imports.stream().anyMatch(statement -> statement.getQName().equals("example.customgui.Display")));
    assertFalse(imports.stream().anyMatch(statement -> statement.getQName().equals("example.customgui.DisplayAccessPolicy")));
  }

  private static class ImportDecorator extends OwnCommandDecorator {
    ImportDecorator() {
      super(new GlobalExtensionManagement(), Set.of());
    }

    void importType(SymTypeExpression type, ASTCDClass command) {
      addImport(type, command);
    }
  }

}
