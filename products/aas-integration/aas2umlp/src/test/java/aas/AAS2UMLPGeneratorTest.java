package aas;

import aas.utils.AssociationFinder;
import aas.utils.ClassFinder;
import aas.utils.NameFinder;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdmerge.CDMerge;
import de.monticore.cdmerge.config.MergeParameter;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AAS2UMLPGeneratorTest extends AbstractTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path outputDir;

  @ParameterizedTest
  @MethodSource("generate")
  void testGenerate(AAS2UMLPModes mode, int expectedCount) {

    AAS2UMLPGenerator generator = new AAS2UMLPGenerator(mode);

    File cd = new File(outputDir + "/TestingAAS.umlp");

    generator.parseAASAndGenerateUmlp("src/test/resources/aas/aas.aasx",
        outputDir.toString(),
        outputDir.toString());

    ASTCDCompilationUnit ast = parse(outputDir.toString() + "/TestingAAS.umlp");

    assertTrue(cd.exists());

    long count = 0;
    try (Stream<Path> stream = Files.list(outputDir)) {
      count = stream.filter(Files::isRegularFile).count();
    } catch (Exception e) {
      fail(e.getMessage());
    }
    assertEquals(expectedCount, count);
  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  public void testMergeFull(AAS2UMLPModes mode) {

    AAS2UMLPGenerator generator = new AAS2UMLPGenerator(mode);

    List<ASTCDCompilationUnit> mergeSet = new ArrayList<>();

    generator.parseAASAndGenerateUmlp("src/test/resources/aas/aas.aasx",
        outputDir.toString(),
        outputDir.toString());

    ASTCDCompilationUnit ast = parse(outputDir.toString() + "/TestingAAS.umlp");

    createSymTab(ast);
    mergeSet.add(ast);

    generator.parseAASAndGenerateUmlp("src/test/resources/aas/aasmerge.aasx",
        outputDir.toString(),
        outputDir.toString());

    ASTCDCompilationUnit ast2 = parse(outputDir.toString() + "/AASMerge.umlp");
    createSymTab(ast2);
    mergeSet.add(ast2);

    ASTCDCompilationUnit mergedAst = CDMerge.merge(mergeSet, "Merge", Set.of(MergeParameter.FAIL_AMBIGUOUS));

    assertNotNull(mergedAst);

  }

  @ParameterizedTest
  @EnumSource(AAS2UMLPModes.class)
  public void testMergeSpecific(AAS2UMLPModes mode) {

    AAS2UMLPGenerator generator = new AAS2UMLPGenerator(mode);

    List<ASTCDCompilationUnit> mergeSet = new ArrayList<>();

    generator.parseAASAndGenerateUmlp("src/test/resources/aas/aas.aasx",
        outputDir.toString(),
        outputDir.toString());

    ASTCDCompilationUnit ast = parse(outputDir.toString() + "/TestingAAS.umlp");

    createSymTab(ast);
    mergeSet.add(ast);

    generator.parseAASAndGenerateUmlp("src/test/resources/aas/aasmerge.aasx",
        outputDir.toString(),
        outputDir.toString());

    ASTCDCompilationUnit ast2 = parse(outputDir.toString() + "/AASMerge.umlp");
    createSymTab(ast2);
    mergeSet.add(ast2);

    ASTCDCompilationUnit mergedAst = CDMerge.merge(mergeSet, "Merge", Set.of(MergeParameter.FAIL_AMBIGUOUS));

    CD4CodeTraverser t = CD4CodeMill.inheritanceTraverser();
    AssociationFinder finder = new AssociationFinder("Submodel1");
    ClassFinder finder2 = new ClassFinder("Mergesubmodel");
    t.add4CDAssociation(finder);
    t.add4CDBasis(finder2);
    mergedAst.accept(t);

    assertAll(
        () -> assertEquals(11, finder.getAssociations().size()),
        () -> assertNotNull(finder2.getFoundClass())
    );
  }

  public static Stream<Arguments> generate() {
    return Stream.of(
        Arguments.of(AAS2UMLPModes.IDSHORT_ONLY, 2),
        Arguments.of(AAS2UMLPModes.HYBRID, 3),
        Arguments.of(AAS2UMLPModes.SEMANTICID_ONLY, 2)
    );
  }
}
