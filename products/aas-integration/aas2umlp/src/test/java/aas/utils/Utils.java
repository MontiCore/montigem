package aas.utils;

import aas.decorator.AAS2UMLPDecorator;
import de.se_rwth.commons.logging.Log;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.AASXDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.xml.XmlDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Entity;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Qualifier;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementList;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class Utils {

  private Utils() {}

  public record AASElement(String submodelType, String fullId, List<AASElement> children, String cardinality, String semanticId) {}

  public static List<AASElement> flattenAAS(Submodel submodel) {

    List<AASElement> allElements = new ArrayList<>();

    if (submodel.getId() != null && !submodel.getId().isEmpty() ) {
      List<AASElement> elements = new ArrayList<>();
      String fullIdSm = submodel.getId();

      for (SubmodelElement child : submodel.getSubmodelElements()) {
        String fullIdChild = fullIdSm + "/" + child.getIdShort();
        elements.add(new AASElement(
            child.getClass().getSimpleName().replace("Default", ""),
            fullIdChild,
            directChildrenOfAsAASElement(child, fullIdChild),
            determineCardinality(child),
            isSemanticIdPresent(child) ? getSemIdFromSE(child) : "noSemId"));
      }
      allElements.addAll(elements);

      for (SubmodelElement child : submodel.getSubmodelElements()) {
        _flattenAAS(allElements, child, submodel, fullIdSm);
      }
    } else {
      Log.error("Submodel " + submodel.getIdShort() + " has no id or id is empty!");
    }
    return allElements;
  }

  private static void _flattenAAS(List<AASElement> allElements, SubmodelElement submodelElement, Referable parentElement, String fullIdParent) {
    if (submodelElement.getIdShort() == null) {
      submodelElement.setIdShort(parentElement.getIdShort() + "_Element");
    }
    switch (submodelElement.getClass().getSimpleName()) {
      case "DefaultSubmodelElementCollection" -> {
        String fullIdSme = fullIdParent + "/" + submodelElement.getIdShort();

        for (SubmodelElement child : ((DefaultSubmodelElementCollection) submodelElement).getValue()) {
          String fullIdChild = fullIdSme + "/" + child.getIdShort();
          allElements.add(new AASElement(
              child.getClass().getSimpleName().replace("Default", ""),
              fullIdChild,
              directChildrenOfAsAASElement(child, fullIdChild),
              determineCardinality(child),
              isSemanticIdPresent(child) ? getSemIdFromSE(child) : "noSemId"
          ));
        }

        for (SubmodelElement child : ((DefaultSubmodelElementCollection) submodelElement).getValue()) {
          _flattenAAS(allElements, child, submodelElement, fullIdSme);
        }
      }
      case "DefaultSubmodelElementList" -> {
        String fullIdSme = fullIdParent + "/" + submodelElement.getIdShort();

        for (SubmodelElement child : ((DefaultSubmodelElementList) submodelElement).getValue()) {
          if (child.getIdShort() == null) {
            child.setIdShort(submodelElement.getIdShort() + "_Element");
          }
          String fullIdChild = fullIdSme + "/" + child.getIdShort();
          allElements.add(new AASElement(
              child.getClass().getSimpleName().replace("Default", ""),
              fullIdChild,
              directChildrenOfAsAASElement(child, fullIdChild),
              determineCardinality(child),
              isSemanticIdPresent(child) ? getSemIdFromSE(child) : "noSemId"
          ));
        }

        for (SubmodelElement child : ((DefaultSubmodelElementList) submodelElement).getValue()) {
          _flattenAAS(allElements, child, submodelElement, fullIdSme);
        }
      }
      default -> {}
    }
  }


  private static String getSemIdFromSE(SubmodelElement element) {
    if (element.getSemanticId() == null ) {
      Log.error("Submodel element " + element.getIdShort() + " has no reference to a semantic id.");
      return "";
    }

    if (element.getSemanticId().getKeys().isEmpty()) {
      Log.error("Submodel element " + element.getIdShort() + " is missing keys in the reference" +
          " to the semantic id.");
      return "";
    }

    StringBuilder id = new StringBuilder();
    for (Key key : element.getSemanticId().getKeys()) {
      id.append(key.getValue());
    }

    return id.toString();
  }

  private static boolean isSemanticIdPresent(SubmodelElement element) {
    return element.getSemanticId() != null && !element.getSemanticId().getKeys().isEmpty();
  }

  private static String determineCardinality(SubmodelElement element) {

    if (!element.getQualifiers().isEmpty()) {
      List<Qualifier> tmp = element.getQualifiers().stream().filter(qualifier -> qualifier.getType().contains("Cardinality") || qualifier.getType().contains("Multiplicity")).toList();
      switch (tmp.getFirst().getValue()) {
        case "OneToMany" -> {
          return "OneToMany";
        }
        case "ZeroToOne" -> {
          return "ZeroToOne";
        }
        case "One" -> {
          return "One";
        }
        default -> {
          return "ZeroToMany";
        }
      }
    }
    return "ZeroToMany";
  }

  private static List<AASElement> directChildrenOfAsAASElement(SubmodelElement element, String fullElementId) {

    List<AASElement> children = new ArrayList<>();

    switch (element) {
      case  DefaultSubmodelElementCollection collection -> collection.getValue().forEach(submodelElement ->
              children.add(new AASElement(
                  submodelElement.getClass().getSimpleName().replace("Default", ""),
                  fullElementId + "/" + submodelElement.getIdShort(),
                  null,
                  determineCardinality(submodelElement),
                  isSemanticIdPresent(submodelElement) ? getSemIdFromSE(submodelElement) : "noSemId"
                  ))
          );
      case DefaultSubmodelElementList list -> list.getValue().forEach(submodelElement ->
          {
            if (submodelElement.getIdShort() == null) {
              submodelElement.setIdShort(element.getIdShort() + "_Element");
            }
            children.add(new AASElement(
                submodelElement.getClass().getSimpleName().replace("Default", ""),
                fullElementId + "/" + submodelElement.getIdShort(),
                null,
                determineCardinality(submodelElement),
                isSemanticIdPresent(submodelElement) ? getSemIdFromSE(submodelElement) : "noSemId"
            ));
          }
      );
      default -> {}
    }
    return children;
  }

  public static Environment getEnvironment(String file) {
    Environment env = null;
    try {
      InputStream input = null;
      input = new FileInputStream(file);

      switch (AASFormat.valueOf(FilenameUtils.getExtension(file.toUpperCase()))) {
        case AASX: {
          AASXDeserializer deserializer = new AASXDeserializer(input);
          env = deserializer.read();
          break;
        }
        case JSON: {
          JsonDeserializer deserializer = new JsonDeserializer();
          env = deserializer.read(input, StandardCharsets.UTF_8, Environment.class);
          break;
        }
        case XML: {
          XmlDeserializer deserializer = new XmlDeserializer();
          env = deserializer.read(input, StandardCharsets.UTF_8);
          break;
        }
      }
    } catch (Exception e) {
      Log.error(e.getMessage());
    }
    return env;
  }

  public record Match(Submodel submodel, SubmodelElement element) {}

  public static List<Match> findAllByIdShort(Submodel submodel, String idShort) {
    return findAllByIdShort(List.of(submodel), idShort);
  }

  public static List<Match> findAllByIdShort(List<Submodel> submodels, String idShort) {

    List<Match> hits = new ArrayList<>();

    for (Submodel sm : submodels) {
      if (sm == null || sm.getSubmodelElements() == null) continue;

      Deque<SubmodelElement> stack = new ArrayDeque<>(sm.getSubmodelElements());

      while (!stack.isEmpty()) {
        SubmodelElement sme = stack.pop();

        if (idShort.equals(sme.getIdShort())) {
          hits.add(new Match(sm, sme));
        }

        stack.addAll(childrenOf(sme));
      }
    }

    return hits;
  }

  private static List<SubmodelElement> childrenOf(SubmodelElement sme) {
    return switch (sme) {
      case SubmodelElementCollection c -> nullSafeList(c.getValue());
      case SubmodelElementList l -> nullSafeList(l.getValue());
      case Entity e -> nullSafeList(e.getStatements());
      case null, default -> List.of();
    };

  }

  private static <T> List<T> nullSafeList(List<T> list) {
    return list == null ? List.of() : list;
  }

}
