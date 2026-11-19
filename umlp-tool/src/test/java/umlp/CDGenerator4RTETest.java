package umlp;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import org.junit.jupiter.api.Test;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.rte.data.CDGenerator4RTE;
import umlp.generator.util.TagLookUp;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CDGenerator4RTETest extends AbstractUMLPTest {

  @Test
  public void generatesPojoAssociationApisForAllCardinalities() throws Exception {
    ASTCDCompilationUnit ast = parse("rte/PojoAssociations.cd");
    runAfterParseTrafo(ast);
    var scope = createSymTab(ast);
    TagRelated.doStoreTagStereos(ast, new UMLPTool(), scope,
        new DefaultParser().parse(new Options(), new String[0]));
    try {
      runDefaultTrafos(ast);
      recreateSymbolTable(ast);
      new CDGenerator4RTE(getGenSetup(), UMLPGeneratorConfig.getInstance()).generate(ast, "umlp.RTE2Java");
      Path generated = Path.of(BUILD_FOLDER, "generated-test-sources", "rte", "pojoassociations");
      String node = Files.readString(generated.resolve("Node.java"));
      String builder = Files.readString(generated.resolve("NodeBuilder.java"));
      for (String method : new String[]{"addChildren", "setOwner", "addPorts", "addSources", "addTargets", "setPrevious", "setNext"}) {
        assertTrue(node.contains(method), method);
      }
      assertTrue(builder.contains("this.setChildrenList(children)"));
      assertTrue(Files.readString(generated.resolve("PortBuilder.java")).contains("this.setChannel(channel)"));
      assertNoErrors();
    } finally {
      TagLookUp.cleanUp();
    }
  }

  @Test
  public void testGenerateMethod() {
    ASTCDCompilationUnit ast = parse("rte/ExampleData.cd");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    runDefaultTrafos(ast);
    recreateSymbolTable(ast);

    CDGenerator4RTE gen = new CDGenerator4RTE(getGenSetup(), UMLPGeneratorConfig.getInstance());
    gen.generate(ast, "umlp.RTE2Java");

    assertNoErrors();
  }
}
