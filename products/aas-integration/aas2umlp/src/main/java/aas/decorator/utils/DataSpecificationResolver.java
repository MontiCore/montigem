package aas.decorator.utils;

import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.DataSpecificationContent;
import org.eclipse.digitaltwin.aas4j.v3.model.DataSpecificationIec61360;
import org.eclipse.digitaltwin.aas4j.v3.model.EmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.ValueReferencePair;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultDataSpecificationIec61360;

import java.util.*;

public class DataSpecificationResolver {

  private static final String IEC61360_URI =
      "https://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/3/0";

  public static Optional<List<String>> resolveEnumValues(
      String semanticId,
      List<ConceptDescription> conceptDescriptions) {

    // Guard: null or empty inputs
    if (semanticId == null || conceptDescriptions == null || conceptDescriptions.isEmpty()) {
      return Optional.empty();
    }

    Optional<ConceptDescription> matchingConcept = conceptDescriptions.stream()
        .filter(cdesc -> cdesc.getId().equals(semanticId))
        .findFirst();

    if (matchingConcept.isEmpty()) {
      return Optional.empty();
    }

    Optional<DataSpecificationIec61360> matchingDataSpec = matchingConcept.get().getEmbeddedDataSpecifications().stream()
        .filter(eDataSpec -> eDataSpec.getDataSpecification().getKeys().stream().anyMatch(key -> key.getValue().equals(IEC61360_URI)))
        .map(EmbeddedDataSpecification::getDataSpecificationContent)
        .map(dataSpecificationContent -> ((DataSpecificationIec61360) dataSpecificationContent))
        .findFirst();

    if (matchingDataSpec.isEmpty()
        || matchingDataSpec.get().getValueList() == null
        || matchingDataSpec.get().getValueList().getValueReferencePairs() == null) {
      return Optional.empty();
    }

    List<String> valueList = matchingDataSpec.get().getValueList().getValueReferencePairs().stream()
        .map(ValueReferencePair::getValue)
        .toList();
    if (valueList.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(valueList);

  }
}
