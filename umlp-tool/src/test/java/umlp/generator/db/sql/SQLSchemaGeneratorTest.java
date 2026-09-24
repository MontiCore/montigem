/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;
import umlp.generator.domain.UMLSetDefaultsTrafo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLSchemaGeneratorTest extends AbstractUMLPTest {

  protected static final String ASSOC_A = "componentWithAVeryLongNameForTestingIdentifiers_componentWithAVeryLongNameForTestingIdentifiers_targetA_targetA";
  protected static final String ASSOC_B = "componentWithAVeryLongNameForTestingIdentifiers_componentWithAVeryLongNameForTestingIdentifiers_targetB_targetB";

  @Test
  public void testTruncateIdentifier() {
    assertEquals("short", SQLSchemaGenerator.truncateIdentifier("short"));
    assertEquals("a".repeat(63), SQLSchemaGenerator.truncateIdentifier("a".repeat(63)));
    assertEquals("a".repeat(63), SQLSchemaGenerator.truncateIdentifier("a".repeat(64)));
    assertEquals("a".repeat(63), SQLSchemaGenerator.truncateIdentifier("a".repeat(100)));
    assertEquals("a".repeat(61) + "ä", SQLSchemaGenerator.truncateIdentifier("a".repeat(61) + "ä"));
    assertEquals("a".repeat(62), SQLSchemaGenerator.truncateIdentifier("a".repeat(62) + "ä"));
    assertEquals("a".repeat(61), SQLSchemaGenerator.truncateIdentifier("a".repeat(61) + "€"));
    assertEquals("a".repeat(60), SQLSchemaGenerator.truncateIdentifier("a".repeat(60) + "😀"));
    assertEquals("ä".repeat(31), SQLSchemaGenerator.truncateIdentifier("ä".repeat(40)));
  }

  @Test
  public void testLongIdentifiers() {
    String content = generate("SQLIdentifiers.umlp");
    assertNoErrors();

    assertTrue(content.contains("CREATE TABLE \"component_with_a_very_long_name_for_testing_identifiers\"("));
    assertTrue(content.contains("CONSTRAINT pk_component_with_a_very_long_name_for_testing_identifiers PRIMARY KEY (id)"));
    assertTrue(content.contains("\"measurement_value_with_a_very_long_name_for_testing_identifier_\" VARCHAR NOT NULL"));
    assertTrue(content.contains("CREATE TABLE component_with_a_very_long_name_for_testing_identifiers$tags_wi("));
    assertTrue(content.contains("CONSTRAINT pk_component_with_a_very_long_name_for_testing_identifiers$tags PRIMARY KEY"));
    assertTrue(content.contains("CREATE TABLE assoc_component_with_a_very_long_name_for_testing_identifiers_c("));
    assertTrue(content.contains("CONSTRAINT pk_assoc_component_with_a_very_long_name_for_testing_identifier PRIMARY KEY"));

    assertEquals("\"measurement_value_with_a_very_long_name_for_testing_identifier_\"",
        SQLSchemaGenerator.convertAttributeName("measurementValueWithAVeryLongNameForTestingIdentifierLimitA", SQLSchemaGenerator.AttributeType.QUOTED));
    assertEquals("component_with_a_very_long_name_for_testing_identifiers$tags_wi",
        SQLSchemaGenerator.convertListAttributeTableName("ComponentWithAVeryLongNameForTestingIdentifiers", "tagsWithAVeryLongNameForTestingTheIdentifierLimit"));
  }

  @Test
  public void testIdentifierCollisions() {
    String content = generate("SQLIdentifierCollisions.umlp");
    assertNoErrors();

    List<String> warnings = Log.getFindings().stream().filter(Finding::isWarning).map(Finding::getMsg).collect(Collectors.toList());
    assertEquals(3, warnings.size(), warnings.toString());
    assertTrue(warnings.stream().allMatch(w -> w.startsWith("0xdf10f")), warnings.toString());
    assertTrue(warnings.get(0).contains("attribute SQLIdentifierCollisions.ComponentWithAVeryLongNameForTestingIdentifiers.measurementValueWithAVeryLongNameForTestingIdentifierLimitA")
        && warnings.get(0).contains("attribute SQLIdentifierCollisions.ComponentWithAVeryLongNameForTestingIdentifiers.measurementValueWithAVeryLongNameForTestingIdentifierLimitB"), warnings.get(0));
    for (String warning : warnings.subList(1, 3)) {
      assertTrue(warning.contains("association SQLIdentifierCollisions." + ASSOC_A) && warning.contains("association SQLIdentifierCollisions." + ASSOC_B), warning);
    }

    assertTrue(content.contains("CREATE TABLE \"component_with_a_very_long_name_for_testing_identifiers\"("));
    assertTrue(content.contains("CONSTRAINT pk_component_with_a_very_long_name_for_testing_identifiers PRIMARY KEY (id)"));

    String columnA = SQLSchemaGenerator.convertAttributeName("measurementValueWithAVeryLongNameForTestingIdentifierLimitA", SQLSchemaGenerator.AttributeType.QUOTED);
    String columnB = SQLSchemaGenerator.convertAttributeName("measurementValueWithAVeryLongNameForTestingIdentifierLimitB", SQLSchemaGenerator.AttributeType.QUOTED);
    assertHashed("\"measurement_value_with_a_very_long_name_for_testing_id_", columnA, columnB);
    assertTrue(content.contains(columnA + " VARCHAR NOT NULL") && content.contains(columnB + " VARCHAR NOT NULL"), content);

    String tableA = SQLSchemaGenerator.convertAssocName(ASSOC_A);
    String tableB = SQLSchemaGenerator.convertAssocName(ASSOC_B);
    assertHashed("assoc_component_with_a_very_long_name_for_testing_iden_", tableA, tableB);
    assertTrue(content.contains("CREATE TABLE " + tableA + "(") && content.contains("CREATE TABLE " + tableB + "("), content);

    Matcher pks = Pattern.compile("CONSTRAINT (pk_assoc_\\S+) PRIMARY KEY").matcher(content);
    assertTrue(pks.find());
    String pkA = pks.group(1);
    assertTrue(pks.find());
    assertHashed("pk_assoc_component_with_a_very_long_name_for_testing_i_", pkA, pks.group(1));
  }

  protected void assertHashed(String prefix, String a, String b) {
    assertNotEquals(a, b);
    for (String identifier : List.of(a, b)) {
      assertTrue(identifier.matches(Pattern.quote(prefix) + "[0-9a-f]{8}\"?"), identifier);
      assertEquals(SQLSchemaGenerator.MAX_IDENTIFIER_BYTES, identifier.replace("\"", "").length(), identifier);
    }
  }

  protected String generate(String model) {
    ASTCDCompilationUnit ast = parse("src/test/resources/sql", model);
    runAfterParseTrafo(ast);
    new UMLSetDefaultsTrafo(CD4CodeMill.inheritanceTraverser()).transform(ast);
    createSymTab(ast);
    return new SQLSchemaGenerator(getGenSetup()).generateContent(ast);
  }
}
