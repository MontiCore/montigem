/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
  Companion of the ExampleNotes: assembles the paragraphs of the info card from
  the library documentation of the demonstrated components (common.ComponentDocs)
  and the hand-written notes of the example.

  <p>The paragraph list is built once and cached: method bindings in a .gui are
  re-evaluated on every change-detection cycle, and the @iterate of the info
  card should see the same list instance every time.
*/
@Component
public class ExampleNotes extends ExampleNotesTOP {

  private List<String> cachedParagraphs;

  /** Documentation paragraphs of the components ("GemBarChart|A bar chart ...")
      followed by the hand-written `about` paragraphs. */
  public List<String> paragraphs(List<String> docComponents, List<String> about) {
    if (cachedParagraphs == null) {
      List<String> result = new ArrayList<>();
      if (docComponents != null) {
        for (String c : docComponents) {
          String description = ComponentDocs.description(c);
          if (!description.isEmpty()) {
            result.add(c + "|" + description);
          }
        }
      }
      if (about != null) {
        for (String a : about) {
          result.add(a);
        }
      }
      cachedParagraphs = result;
    }
    return cachedParagraphs;
  }

  public Boolean hasDocComponents(List<String> docComponents) {
    return docComponents != null && !docComponents.isEmpty();
  }

  public Boolean hasSteps(List<String> tryIt) {
    return tryIt != null && !tryIt.isEmpty();
  }
}
