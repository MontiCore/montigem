/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
  Companion of the ExampleNotes: splits the text of the info card into the first
  hand-written paragraph, which is always visible, and the rest behind "More":
  the library documentation of the demonstrated components (common.ComponentDocs)
  and the other hand-written notes of the example.

  <p>The paragraph lists are built once and cached: method bindings in a .gui are
  re-evaluated on every change-detection cycle, and the @iterate of the info
  card should see the same list instance every time.
*/
@Component
public class ExampleNotes extends ExampleNotesTOP {

  private List<String> cachedParagraphs;
  private List<String> cachedMoreParagraphs;

  /** The first hand-written `about` paragraph. */
  public List<String> paragraphs(List<String> about) {
    if (cachedParagraphs == null) {
      List<String> result = new ArrayList<>();
      if (about != null && !about.isEmpty()) {
        result.add(about.get(0));
      }
      cachedParagraphs = result;
    }
    return cachedParagraphs;
  }

  /** Documentation paragraphs of the components ("GemBarChart|A bar chart ...")
      followed by the other hand-written `about` paragraphs. */
  public List<String> moreParagraphs(List<String> docComponents, List<String> about) {
    if (cachedMoreParagraphs == null) {
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
        for (int i = 1; i < about.size(); i++) {
          result.add(about.get(i));
        }
      }
      cachedMoreParagraphs = result;
    }
    return cachedMoreParagraphs;
  }

  public Boolean hasDocComponents(List<String> docComponents) {
    return docComponents != null && !docComponents.isEmpty();
  }

  public Boolean hasSteps(List<String> tryIt) {
    return tryIt != null && !tryIt.isEmpty();
  }
}
