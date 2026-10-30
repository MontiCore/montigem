/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  Companion of the ParameterList: looks the parameters of a component up in the
  generated common.ComponentDocs and splits its "name|type|default|doc" entries
  for the template.

  <p>Method bindings in a .gui are re-evaluated on every change-detection
  cycle, so the parameter lists are cached per component - the same list
  instance is returned every time, and the @iterate over it does not churn.
*/
@Component
public class ParameterList extends ParameterListTOP {

  private final Map<String, List<String>> cache = new HashMap<>();

  /** The "name|type|default|doc" entries of the component, cached. */
  public List<String> parametersOf(String component) {
    List<String> cached = cache.get(component);
    if (cached == null) {
      cached = ComponentDocs.parameters(component);
      cache.put(component, cached);
    }
    return cached;
  }

  public Boolean hasNoParameters(String component) {
    return parametersOf(component).isEmpty();
  }

  public String packageOf(String component) {
    return ComponentDocs.packageOf(component);
  }

  public String docUrl(String component) {
    return ComponentDocs.docUrl(component);
  }

  public String paramName(String entry) {
    return field(entry, 0);
  }

  /** Type and, if the parameter has one, its default value: `String = "auto"`. */
  public String paramTypeAndDefault(String entry) {
    String type = field(entry, 1);
    String def = field(entry, 2);
    if (def.isEmpty()) {
      return type;
    }
    return type + " = " + def;
  }

  public String paramDoc(String entry) {
    String doc = field(entry, 3);
    return doc.isEmpty() ? "(no description)" : doc;
  }

  /** The i-th "|"-separated field of an entry; the doc (last field) may itself
      contain no "|" because the generator replaces it. */
  private String field(String entry, int index) {
    if (entry == null) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    int start = 0;
    while (true) {
      int bar = entry.indexOf("|", start);
      if (bar < 0) {
        parts.add(entry.substring(start));
        break;
      }
      parts.add(entry.substring(start, bar));
      start = bar + 1;
    }
    return index < parts.size() ? parts.get(index) : "";
  }
}
