package aas;

import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.DataSpecificationIec61360;
import org.eclipse.digitaltwin.aas4j.v3.model.EmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ValueReferencePair;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultDataSpecificationIec61360;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultValueList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultValueReferencePair;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import aas.decorator.utils.DataSpecificationResolver;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataSpecificationResolver.
 * Tests resolveEnumValues() with mocked AAS model elements.
 */
class DataSpecificationResolverTest {

  @Test
  void resolveEnumValues_withNullSemanticId() {
    List<ConceptDescription> concepts = List.of();
    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues(null, concepts);
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withNullConceptDescriptions() {
    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("anyId", null);
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withEmptyConceptDescriptions() {
    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("anyId", List.of());
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withNoMatchOnSemanticId() {
    ConceptDescription desc = new DefaultConceptDescription.Builder()
        .id("differentId")
        .build();

    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("mySemanticId", List.of(desc));
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withMatchingConceptButNoEmbeddedDataSpec_withKeys() {
    Key key = new DefaultKey.Builder()
        .type(KeyTypes.CONCEPT_DESCRIPTION)
        .value("http://example.com/someKey")
        .build();

    EmbeddedDataSpecification eds = new DefaultEmbeddedDataSpecification.Builder()
        .dataSpecification(new DefaultReference.Builder()
            .keys(List.of(key))
            .build())
        .build();

    ConceptDescription desc = new DefaultConceptDescription.Builder()
        .id("semanticId")
        .embeddedDataSpecifications(List.of(eds))
        .build();

    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("semanticId", List.of(desc));
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withNonIEC61360DataSpec() {
    Key key1 = new DefaultKey.Builder()
        .type(KeyTypes.CONCEPT_DESCRIPTION)
        .value("http://example.com/wrong")
        .build();
    Key key2 = new DefaultKey.Builder()
        .type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
        .value("http://example.com/another")
        .build();

    EmbeddedDataSpecification eds = new DefaultEmbeddedDataSpecification.Builder()
        .dataSpecification(new DefaultReference.Builder()
            .keys(List.of(key1, key2))
            .build())
        .build();

    ConceptDescription desc = new DefaultConceptDescription.Builder()
        .id("semanticId")
        .embeddedDataSpecifications(List.of(eds))
        .build();

    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("semanticId", List.of(desc));
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withIEC61360ButEmptyValueReferencePairs() {
    Key iecKey = new DefaultKey.Builder()
        .type(KeyTypes.GLOBAL_REFERENCE)
        .value("https://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/3/0")
        .build();

    EmbeddedDataSpecification eds = new DefaultEmbeddedDataSpecification.Builder()
        .dataSpecification(new DefaultReference.Builder()
            .keys(List.of(iecKey))
            .build())
        .dataSpecificationContent(new DefaultDataSpecificationIec61360.Builder()
            .valueList(new DefaultValueList.Builder()
                .valueReferencePairs(List.of())
                .build())
            .build())
        .build();

    ConceptDescription desc = new DefaultConceptDescription.Builder()
        .id("semanticId")
        .embeddedDataSpecifications(List.of(eds))
        .build();

    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("semanticId", List.of(desc));
    System.out.println(result);
    assertFalse(result.isPresent());
  }

  @Test
  void resolveEnumValues_withValidIEC61360AndValues() {
    DataSpecificationIec61360 iecSpec = new DefaultDataSpecificationIec61360.Builder()
        .valueList(new DefaultValueList.Builder()
            .valueReferencePairs(List.of(
                new DefaultValueReferencePair.Builder().value("READABLE").build(),
                new DefaultValueReferencePair.Builder().value("WRITEABLE").build(),
                new DefaultValueReferencePair.Builder().value("EXECUTABLE").build()
            ))
            .build())
        .build();

    Key iecKey = new DefaultKey.Builder()
        .type(KeyTypes.GLOBAL_REFERENCE)
        .value("https://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/3/0")
        .build();

    EmbeddedDataSpecification eds = new DefaultEmbeddedDataSpecification.Builder()
        .dataSpecification(new DefaultReference.Builder()
            .keys(List.of(iecKey))
            .build())
        .dataSpecificationContent(iecSpec)
        .build();

    ConceptDescription desc = new DefaultConceptDescription.Builder()
        .id("semanticId")
        .embeddedDataSpecifications(List.of(eds))
        .build();

    Optional<List<String>> result = DataSpecificationResolver.resolveEnumValues("semanticId", List.of(desc));

    assertTrue(result.isPresent());
    assertEquals(3, result.get().size());
    assertEquals("READABLE", result.get().get(0));
    assertEquals("WRITEABLE", result.get().get(1));
    assertEquals("EXECUTABLE", result.get().get(2));
  }

}
