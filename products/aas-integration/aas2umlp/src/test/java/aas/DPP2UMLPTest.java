package aas;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdmerge.CDMerge;
import de.monticore.cdmerge.config.MergeParameter;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;
import aas.decorator.utils.StringUtils;
import aas.utils.Utils;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DPP2UMLPTest extends AbstractTest {

  AAS2UMLPGenerator generator_IDSHORTONLY = new AAS2UMLPGenerator(AAS2UMLPModes.IDSHORT_ONLY);
  AAS2UMLPGenerator generator_HYBRID = new AAS2UMLPGenerator(AAS2UMLPModes.HYBRID);
  AAS2UMLPGenerator generator_SEMANTICIDONLY = new AAS2UMLPGenerator(AAS2UMLPModes.SEMANTICID_ONLY);
  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path idShortDir;
  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path semIdDir;
  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path hybridDir;

  @Test
  public void TechnicalDatTest() throws IOException {

    callGenerators("src/test/resources/aas/DPP/IDTA_02003-1-2_Template_TechnicalData.aasx");

    assertFileExists(idShortDir + "/TechnicalDataAAS.umlp");
    assertFileExists(hybridDir + "/TechnicalDataAAS.umlp");
    assertFileExists(semIdDir + "/TechnicalDataAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/TechnicalDataAAS.umlp");
    createSymTab(ast1);
    ASTCDCompilationUnit ast2 = parse(hybridDir.toString() + "/TechnicalDataAAS.umlp");
    createSymTab(ast2);
    ASTCDCompilationUnit ast3 = parse(semIdDir.toString() + "/TechnicalDataAAS.umlp");
    createSymTab(ast3);

    List<Utils.AASElement> elements = Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02003-1-2_Template_TechnicalData.aasx")
        .getSubmodels().getFirst());

    Map<String, String> map = getElementMap(idShortDir.toString() + "/TechnicalDataAAS.yml");
    Map<String, String> map2 = getElementMap(hybridDir.toString() + "/TechnicalDataAAS_semanticIdToClassTransformation.yml");

    checkPresenceId(ast1,elements, map);
    checkPresenceAssocs(ast1,elements, map);
    checkPresenceSemId(ast2, elements, map2);
    checkPresenceSemId(ast3, elements, map2);
  }



  @Test
  public void HandoverDocumentationTest() throws IOException {

    callGenerators("src/test/resources/aas/DPP/IDTA_02004-2-0_Template_HandoverDocumentation.aasx");

    assertFileExists(idShortDir + "/HandoverDocumentationAAS.umlp");
    assertFileExists(hybridDir + "/HandoverDocumentationAAS.umlp");
    assertFileExists(semIdDir + "/HandoverDocumentationAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/HandoverDocumentationAAS.umlp");
    createSymTab(ast1);
    ASTCDCompilationUnit ast2 = parse(hybridDir.toString() + "/HandoverDocumentationAAS.umlp");
    createSymTab(ast2);
    ASTCDCompilationUnit ast3 = parse(semIdDir.toString() + "/HandoverDocumentationAAS.umlp");
    createSymTab(ast3);

    List<Utils.AASElement> elements = Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02004-2-0_Template_HandoverDocumentation.aasx")
        .getSubmodels().getFirst());

    Map<String, String> map = getElementMap(idShortDir.toString() + "/HandoverDocumentationAAS.yml");
    Map<String, String> map2 = getElementMap(hybridDir.toString() + "/HandoverDocumentationAAS_semanticIdToClassTransformation.yml");

    checkPresenceId(ast1,elements, map);
    checkPresenceAssocs(ast1,elements, map);
    checkPresenceSemId(ast2, elements, map2);
    checkPresenceSemId(ast3, elements, map2);
  }

  @Test
  public void DigitalNameplateTest() throws IOException {

    callGenerators("src/test/resources/aas/DPP/IDTA_02006-3-0-1_Template_DigitalNameplate.aasx");

    assertFileExists(idShortDir + "/DigitalNameplateAAS.umlp");
    assertFileExists(hybridDir + "/DigitalNameplateAAS.umlp");
    assertFileExists(semIdDir + "/DigitalNameplateAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/DigitalNameplateAAS.umlp");
    createSymTab(ast1);
    ASTCDCompilationUnit ast2 = parse(hybridDir.toString() + "/DigitalNameplateAAS.umlp");
    createSymTab(ast2);
    ASTCDCompilationUnit ast3 = parse(semIdDir.toString() + "/DigitalNameplateAAS.umlp");
    createSymTab(ast3);

    List<Utils.AASElement> elements = Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02006-3-0-1_Template_DigitalNameplate.aasx")
        .getSubmodels().getFirst());

    Map<String, String> map = getElementMap(idShortDir.toString() + "/DigitalNameplateAAS.yml");
    Map<String, String> map2 = getElementMap(hybridDir.toString() + "/DigitalNameplateAAS_semanticIdToClassTransformation.yml");

    checkPresenceId(ast1,elements, map);
    checkPresenceAssocs(ast1,elements, map);
    checkPresenceSemId(ast2, elements, map2);
    checkPresenceSemId(ast3, elements, map2);
  }

  @Test
  public void MaintenanceInstructionsTest() throws IOException { //uses the .json because the .aasx is broken (14.05.26)
    callGenerators("src/test/resources/aas/DPP/IDTA_02018_Template_MaintenanceInstructions.json");

    assertFileExists(idShortDir + "/MaintenanceInstructionsAAS.umlp");
    assertFileExists(hybridDir + "/MaintenanceInstructionsAAS.umlp");
    assertFileExists(semIdDir + "/MaintenanceInstructionsAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/MaintenanceInstructionsAAS.umlp");
    createSymTab(ast1);
    ASTCDCompilationUnit ast2 = parse(hybridDir.toString() + "/MaintenanceInstructionsAAS.umlp");
    createSymTab(ast2);
    ASTCDCompilationUnit ast3 = parse(semIdDir.toString() + "/MaintenanceInstructionsAAS.umlp");
    createSymTab(ast3);

    List<Utils.AASElement> elements = Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02018_Template_MaintenanceInstructions.json")
        .getSubmodels().getFirst());

    Map<String, String> map = getElementMap(idShortDir.toString() + "/MaintenanceInstructionsAAS.yml");
    Map<String, String> map2 = getElementMap(hybridDir.toString() + "/MaintenanceInstructionsAAS_semanticIdToClassTransformation.yml");

    checkPresenceId(ast1,elements, map);
    checkPresenceAssocs(ast1,elements, map);
    checkPresenceSemId(ast2, elements, map2);
    checkPresenceSemId(ast3, elements, map2);

  }

  @Test
  public void CarbonFootprintTest() throws IOException {
    callGenerators("src/test/resources/aas/DPP/IDTA_02023 _Template_CarbonFootprint.aasx");

    assertFileExists(idShortDir + "/CarbonFootprintAAS.umlp");
    assertFileExists(hybridDir + "/CarbonFootprintAAS.umlp");
    assertFileExists(semIdDir + "/CarbonFootprintAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/CarbonFootprintAAS.umlp");
    createSymTab(ast1);
    ASTCDCompilationUnit ast2 = parse(hybridDir.toString() + "/CarbonFootprintAAS.umlp");
    createSymTab(ast2);
    ASTCDCompilationUnit ast3 = parse(semIdDir.toString() + "/CarbonFootprintAAS.umlp");
    createSymTab(ast3);

    List<Utils.AASElement> elements = Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02023 _Template_CarbonFootprint.aasx")
        .getSubmodels().getFirst());

    Map<String, String> map1 = getElementMap(idShortDir.toString() + "/CarbonFootprintAAS.yml");
    Map<String, String> map2 = getElementMap(hybridDir.toString() + "/CarbonFootprintAAS_semanticIdToClassTransformation.yml");

    checkPresenceId(ast1,elements, map1);
    checkPresenceAssocs(ast1,elements, map1);
    checkPresenceSemId(ast2, elements, map2);
    checkPresenceSemId(ast3, elements, map2);

  }

  @Test
  public void MergeTest() throws IOException {
    List<ASTCDCompilationUnit> mergeSet = new ArrayList<>();

    generator_IDSHORTONLY.parseAASAndGenerateUmlp("src/test/resources/aas/DPP/IDTA_02023 _Template_CarbonFootprint.aasx",
        idShortDir.toString(),
        idShortDir.toString());

    assertFileExists(idShortDir + "/CarbonFootprintAAS.umlp");

    ASTCDCompilationUnit ast0 = parse(idShortDir.toString() + "/CarbonFootprintAAS.umlp");
    createSymTab(ast0);

    mergeSet.add(ast0);

    generator_IDSHORTONLY.parseAASAndGenerateUmlp("src/test/resources/aas/DPP/IDTA_02018_Template_MaintenanceInstructions.json",
        idShortDir.toString() ,
        idShortDir.toString() );

    assertFileExists(idShortDir + "/MaintenanceInstructionsAAS.umlp");

    ASTCDCompilationUnit ast1 = parse(idShortDir.toString() + "/MaintenanceInstructionsAAS.umlp");
    createSymTab(ast1);

    mergeSet.add(ast1);

    generator_IDSHORTONLY.parseAASAndGenerateUmlp("src/test/resources/aas/DPP/IDTA_02006-3-0-1_Template_DigitalNameplate.aasx",
        idShortDir.toString() ,
        idShortDir.toString() );

    assertFileExists(idShortDir + "/DigitalNameplateAAS.umlp");

    ASTCDCompilationUnit ast2 = parse(idShortDir.toString() + "/DigitalNameplateAAS.umlp");
    createSymTab(ast2);

    mergeSet.add(ast2);

    generator_IDSHORTONLY.parseAASAndGenerateUmlp("src/test/resources/aas/DPP/IDTA_02004-2-0_Template_HandoverDocumentation.aasx",
        idShortDir.toString() ,
        idShortDir.toString() );

    assertFileExists(idShortDir + "/HandoverDocumentationAAS.umlp");

    ASTCDCompilationUnit ast3 = parse(idShortDir.toString() + "/HandoverDocumentationAAS.umlp");
    createSymTab(ast3);

    mergeSet.add(ast3);

    generator_IDSHORTONLY.parseAASAndGenerateUmlp("src/test/resources/aas/DPP/IDTA_02003-1-2_Template_TechnicalData.aasx",
        idShortDir.toString() ,
        idShortDir.toString() );

    assertFileExists(idShortDir + "/TechnicalDataAAS.umlp");

    ASTCDCompilationUnit ast4 = parse(idShortDir.toString() + "/TechnicalDataAAS.umlp");
    createSymTab(ast4);

    mergeSet.add(ast4);

    ASTCDCompilationUnit mergedAst = CDMerge.merge(mergeSet, "Merge", Set.of(MergeParameter.FAIL_AMBIGUOUS));

    assertNotNull(mergedAst);

    List<Utils.AASElement> elements = new ArrayList<>(Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02003-1-2_Template_TechnicalData.aasx")
        .getSubmodels().getFirst()));
    elements.addAll(Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02004-2-0_Template_HandoverDocumentation.aasx")
        .getSubmodels().getFirst()));
    elements.addAll(Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02006-3-0-1_Template_DigitalNameplate.aasx")
        .getSubmodels().getFirst()));
    elements.addAll(Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02018_Template_MaintenanceInstructions.json")
        .getSubmodels().getFirst()));
    elements.addAll(Utils.flattenAAS(Utils.getEnvironment("src/test/resources/aas/DPP/IDTA_02023 _Template_CarbonFootprint.aasx")
        .getSubmodels().getFirst()));

    Yaml yaml = new Yaml();
    FileInputStream in1 = new FileInputStream(idShortDir.toString() + "/TechnicalDataAAS.yml");
    Map<String, String> map1 = yaml.load(in1);
    in1.close();

    FileInputStream in2 = new FileInputStream(idShortDir.toString() + "/HandoverDocumentationAAS.yml");
    Map<String, String> map2 = yaml.load(in2);
    in2.close();

    FileInputStream in3 = new FileInputStream(idShortDir.toString() + "/DigitalNameplateAAS.yml");
    Map<String, String> map3 = yaml.load(in3);
    in3.close();

    FileInputStream in4 = new FileInputStream(idShortDir.toString() + "/MaintenanceInstructionsAAS.yml");
    Map<String, String> map4 = yaml.load(in4);
    in4.close();

    FileInputStream in5 = new FileInputStream(idShortDir.toString() + "/CarbonFootprintAAS.yml");
    Map<String, String> map5 = yaml.load(in5);
    in5.close();

    Map<String,String> map = new HashMap<String,String>();
    map.putAll(map1);
    map.putAll(map2);
    map.putAll(map3);
    map.putAll(map4);
    map.putAll(map5);

    checkPresenceId(mergedAst,elements, map);
    checkPresenceAssocs(mergedAst,elements, map);
  }

  private void checkPresenceId(ASTCDCompilationUnit astCD, List<Utils.AASElement> elements, Map<String, String> nameMap) {

    List<ASTCDClass> classes = astCD.getCDDefinition().getCDClassesList();
    List<ASTCDAssociation> associations = astCD.getCDDefinition().getCDAssociationsList();

    elements.forEach(element -> {
      boolean found = false;
      switch (element.submodelType()) {
        case "SubmodelElementCollection", "SubmodelElementList", "Range", "Property" -> {
          if (classes.stream().anyMatch(classElement ->
              classElement.getName().equals(StringUtils.capitalize(nameMap.get(element.fullId()))))
          ) {
            found = true;
          }
        }
        default -> {
          if (associations.stream().anyMatch(association -> checkPresenceNameAssoc(association, nameMap.get(element.fullId())))
          ) {
            found = true;
          }
        }

      }

      assertTrue(found, element.fullId());
    });
  }

  private boolean checkPresenceNameAssoc(ASTCDAssociation assoc, String name) {

    String right = "";
    String left = "";

    if (assoc.getRight().isPresentCDRole()) {
      right = StringUtils.uncapitalize(assoc.getRight().getCDRole().getName());
    }
    if (assoc.getLeft().isPresentCDRole()) {
      left = StringUtils.uncapitalize(assoc.getLeft().getCDRole().getName());
    }

    return right.equals(StringUtils.uncapitalize(name)) || left.equals(StringUtils.uncapitalize(name));
  }

  private void checkPresenceAssocs(ASTCDCompilationUnit astCD, List<Utils.AASElement> elements, Map<String, String> nameMap) {

    List<ASTCDAssociation> associations = astCD.getCDDefinition().getCDAssociationsList();
    List<ASTCDClass> classes = astCD.getCDDefinition().getCDClassesList();

    elements.forEach(element -> {
      switch (element.submodelType()) {
        case "SubmodelElementCollection" -> {
          List<ASTCDAssociation> filteredAssociations = associations.stream()
              .filter(association ->
                  association.getLeft().getMCQualifiedType().getMCQualifiedName().getParts(0)
                  .equals(StringUtils.capitalize(nameMap.get(element.fullId()))))
              .toList();

          assertEquals(numberOfAssocsForChildren(element.children()), filteredAssociations.size(), element.fullId());
          List< Utils.AASElement > list = element.children().stream().filter(child -> !child.submodelType().equals("Property")).toList();
          list.forEach(child -> {
            assertTrue(filteredAssociations.stream().anyMatch(
                association -> association.getRight().getCDRole().getName().equals(StringUtils.uncapitalize(nameMap.get(child.fullId())))),
                nameMap.get(child.fullId()) + " association role is incorrect.");
            assertTrue(filteredAssociations.stream().anyMatch(association -> checkCardinality(association, child.cardinality())));
          });
        }

        case "SubmodelElementList" -> {
          List<ASTCDAssociation> filteredAssociations = associations.stream()
              .filter(association ->
                  association.getLeft().getMCQualifiedType().getMCQualifiedName().getParts(0)
                      .equals(StringUtils.capitalize(nameMap.get(element.fullId()))))
              .toList();
          assertEquals(1, filteredAssociations.size(), element.fullId());
          if (element.children().getFirst().submodelType().equals("SubmodelElementCollection")
              || element.children().getFirst().submodelType().equals("SubmodelElementList")
              || element.children().getFirst().submodelType().equals("Property")
              || element.children().getFirst().submodelType().equals("Range"))
          {
            if (element.children().getFirst().fullId() == null) {
              assertEquals((StringUtils.capitalize(nameMap.get(element.fullId())) + "_Element"),
                  filteredAssociations.getFirst().getRight().getMCQualifiedType().getMCQualifiedName().getParts(0));
            } else {
              assertEquals(StringUtils.capitalize(nameMap.get(element.children().getFirst().fullId())),
                  filteredAssociations.getFirst().getRight().getMCQualifiedType().getMCQualifiedName().getParts(0));
            }

          }

        }
      }
    });
  }

  private boolean checkCardinality(ASTCDAssociation association, String cardinality) {
    switch (cardinality) {
      case "One" -> {
        return association.getRight().getCDCardinality().isOne();
      }
      case "OneToMany" -> {
        return association.getRight().getCDCardinality().isAtLeastOne();
      }
      case "ZeroToOne" -> {
        return association.getRight().getCDCardinality().isOpt();
      }
      case "ZeroToMany" -> {
        return association.getRight().getCDCardinality().isMult();
      }
      default -> {
        Log.error("Cardinality " + cardinality + " is not supported");
        return false;
      }
    }
  }

  private int numberOfAssocsForChildren(List<Utils.AASElement> elements) {

    List<String> seen = new ArrayList<>();

    int count = 0;
    for(Utils.AASElement element : elements) {
      if (!seen.contains(element.fullId())) {
        count++;
        seen.add(element.fullId());
      }
    }

    return count;
  }

  private Map<String, String> getElementMap(String path) throws IOException {
    Yaml yaml = new Yaml();
    FileInputStream in = new FileInputStream(path);
    Map<String, String> map = yaml.load(in);
    in.close();
    return map;
  }

  private void callGenerators(String pathToAas) {
    generator_IDSHORTONLY.parseAASAndGenerateUmlp(pathToAas,
        idShortDir.toString(),
        idShortDir.toString());

    generator_HYBRID.parseAASAndGenerateUmlp(pathToAas,
        hybridDir.toString(),
        hybridDir.toString());

    generator_SEMANTICIDONLY.parseAASAndGenerateUmlp(pathToAas,
        semIdDir.toString(),
        semIdDir.toString());
  }

  private void assertFileExists(String path) {
    File file = new File(path);
    assertTrue(file.exists());
  }

  private void checkPresenceSemId(ASTCDCompilationUnit astCD, List<Utils.AASElement> elements, Map<String, String> semIdMap){

    List<ASTCDClass> classes = astCD.getCDDefinition().getCDClassesList();
    List<ASTCDAssociation> associations = astCD.getCDDefinition().getCDAssociationsList();

    elements.forEach(element -> {
      boolean found = false;
      switch (element.submodelType()) {
        case "SubmodelElementCollection", "SubmodelElementList", "Range", "Property" -> {
          if (classes.stream().anyMatch(classElement ->
              classElement.getName().equals(StringUtils.capitalize(semIdMap.get(element.semanticId()))))
          ) {
            found = true;
          }
        }
        default -> {
          if (associations.stream().anyMatch(association -> checkPresenceNameAssoc(association, semIdMap.get(element.semanticId())))
          ) {
            found = true;
          }
        }

      }

      assertTrue(found, element.fullId());
    });
  }
}
