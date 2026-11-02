package aas;

import aas.decorator.SemanticIdToClassTransformationDecorator;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.logging.Log;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import aas.decorator.AAS2UMLPDecorator;
import aas.decorator.NameTransformationDecorator;
import aas.decorator.utils.AASParsingUtil;
import aas.decorator.utils.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;


public class AAS2UMLPGenerator {

  public static final String NAMETRANSFORMATIONS = "nameTransformations";
  public static final String SEMANTICIDTOCLASS = "semanticIdToClass";

  private final AAS2UMLPModes mode;

  public AAS2UMLPGenerator(AAS2UMLPModes mode) {
    this.mode = mode;
  }

  /**
   * Parses an AAS model and generates UMLP/CD and YAML output files.
   *
   * <p>The given paths for output files must be directories.
   * This method will overwrite any existing files at the specified output paths.
   * The AAS path must point to a valid AAS file (.aasx, .json, or .xml) or a directory containing such files. The first valid file found will be parsed.
   *</p>
   *
   *
   * @param aasPath the path to the input AAS model (file or directory)
   * @param outputPathCd the output path for the generated UMLP/CD file (must be a directory)
   * @param outputPathYAML the output path for the generated YAML file (must be a directory)
   *
   * @throws RuntimeException if an error occurs during parsing or file writing
   */
  public void parseAASAndGenerateUmlp(String aasPath, String outputPathCd, String outputPathYAML) {
    Environment result = AASParsingUtil.parseAAS(Path.of(aasPath));

    if (result == null) {
      Log.error("Failed to parse AAS");
      return;
    }

    String aasName = result.getAssetAdministrationShells().getFirst().getIdShort().replace("-", "_");

    generateCDAndYAML(result.getSubmodels(),
        result.getConceptDescriptions(),
        StringUtils.capitalize(aasName), outputPathCd , outputPathYAML);

  }

  public void generateCDAndYAML(List<Submodel> submodels, List<ConceptDescription> conceptDescriptions, String aasName, String outputPathCd, String outputPathYAML) {


    ASTCDDefinition cdDef = CD4CodeMill.cDDefinitionBuilder()
        .setName(aasName)
        .setModifier(CDModifier.PUBLIC.build())
        .build();

    ASTCDCompilationUnit decoratedCd = CD4CodeMill.cDCompilationUnitBuilder()
        .setCDDefinition(cdDef)
        .build();

    GlobalExtensionManagement glex = new GlobalExtensionManagement();
    glex.defineGlobalVar(NAMETRANSFORMATIONS, new LinkedHashMap<String, String>());
    glex.defineGlobalVar(SEMANTICIDTOCLASS, new LinkedHashMap<String, String>());

    AAS2UMLPDecorator cDdecorator = new AAS2UMLPDecorator(glex, mode);
    cDdecorator.decorate(aasName, submodels, conceptDescriptions, decoratedCd);

    String printedYAML = "";

    if (mode == AAS2UMLPModes.IDSHORT_ONLY) {
      NameTransformationDecorator yAMLDecorator = new NameTransformationDecorator(glex);
      printedYAML = yAMLDecorator.decorate();
      writeToPath(Path.of(outputPathYAML + "/" + StringUtils.capitalize(aasName) + ".yml"), printedYAML);

    } else if (mode == AAS2UMLPModes.HYBRID) {
      NameTransformationDecorator nameTransformationDecorator = new NameTransformationDecorator(glex);
      printedYAML = nameTransformationDecorator.decorate();
      writeToPath(Path.of(outputPathYAML + "/" + StringUtils.capitalize(aasName) + "_nameTransformation.yml"), printedYAML);
      SemanticIdToClassTransformationDecorator semanticIdToClassTransformationDecorator = new SemanticIdToClassTransformationDecorator(glex);
      printedYAML = semanticIdToClassTransformationDecorator.decorate();
      writeToPath(Path.of(outputPathYAML + "/" + StringUtils.capitalize(aasName) + "_semanticIdToClassTransformation.yml"), printedYAML);

    } else if (mode == AAS2UMLPModes.SEMANTICID_ONLY) {
      SemanticIdToClassTransformationDecorator yAMLDecorator = new SemanticIdToClassTransformationDecorator(glex);
      printedYAML = yAMLDecorator.decorate();
      writeToPath(Path.of(outputPathYAML + "/" + StringUtils.capitalize(aasName) + ".yml"), printedYAML);
    }

    String printedCd = CD4CodeMill.prettyPrint(decoratedCd, true);

    writeToPath(Path.of(outputPathCd + "/" + StringUtils.capitalize(aasName) + ".umlp"), printedCd);
  }

  private static void writeToPath(Path outputPath, String printed) {
    try {
      Files.writeString(outputPath,
          printed,
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      Log.error("Failed to write to " + outputPath, e);
    }
  }
}
