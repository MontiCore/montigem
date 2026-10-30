/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
  Companion of the InfoCard: the collapse toggle of the optional details slot
  and the split of opted-in "<lead>|<body>" paragraphs into their bold lead
  line and body text (see the header comment in InfoCard.gui).
*/
@Component
public class InfoCard extends InfoCardTOP {

  private List<String> cachedAllParagraphs;

  public Consumer<Void> toggleDetails() {
    return $ -> setDetailsExpanded(!isDetailsExpanded());
  }

  /** The paragraphs to render. The combined list is cached, because the @iterate
      should see the same list instance on every change-detection cycle. */
  public List<String> shownParagraphs(List<String> paragraphs, List<String> moreParagraphs, Boolean expanded) {
    if (!expanded || moreParagraphs == null) {
      return paragraphs;
    }
    if (cachedAllParagraphs == null) {
      List<String> result = new ArrayList<>();
      for (String p : paragraphs) {
        result.add(p);
      }
      for (String p : moreParagraphs) {
        result.add(p);
      }
      cachedAllParagraphs = result;
    }
    return cachedAllParagraphs;
  }

  /** True iff the paragraph opts into the "<lead>|<body>" convention: it has a
      "|" with a non-empty lead in front of it. A leading "|" does not opt in,
      so no empty lead line is ever rendered. */
  public Boolean hasLead(String p) {
    return p != null && p.indexOf("|") > 0;
  }

  /** Lead part (before the first "|") of an opted-in paragraph, "" otherwise. */
  public String paragraphLead(String p) {
    return hasLead(p) ? p.substring(0, p.indexOf("|")) : "";
  }

  /** Body part (after the first "|") of an opted-in paragraph, the whole
      string otherwise. */
  public String paragraphBody(String p) {
    return hasLead(p) ? p.substring(p.indexOf("|") + 1) : p;
  }
}
