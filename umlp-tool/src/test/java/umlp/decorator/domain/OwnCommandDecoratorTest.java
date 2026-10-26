package umlp.decorator.domain;

import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import org.junit.jupiter.api.Test;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.decorator.commands.OwnCommandDecorator;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

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
        // gem-identifiable domain type params should generate a ...gemId attribute
        () -> existsAttribute(clazz, "domainParamGemId"),
        // enum domain type params should generate an attribute
        () -> existsAttribute(clazz, "enumParam"),

        // java type param
        () -> existsAttribute(clazz, "javaParam")

    );
  }
}
