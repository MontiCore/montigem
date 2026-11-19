package ma2fustrumo;

import de.monticore.symboltable.ISymbol;
import de.monticore.tagging.SimpleSymbolTagger;
import de.monticore.tagging.TagRepository;
import de.monticore.tagging.tags._ast.ASTTag;
import de.monticore.tagging.tags._ast.ASTTagUnit;
import de.monticore.tagging.tags._ast.ASTValuedTag;
import de.se_rwth.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Looks up valued tags across all tag units loaded in the global repository.
 *
 * <p>A separate tagger is used for each tag unit. This avoids depending on the
 * MontiCore multi-unit lookup behavior and makes tags from every loaded file
 * visible.</p>
 */
public class TagLookup {
  public Optional<String> getValuedTag(ISymbol symbol, String name) {
    Set<String> values = new LinkedHashSet<>();
    for (ASTTagUnit tagUnit : TagRepository.getLoadedTagUnits()) {
      SimpleSymbolTagger tagger = new SimpleSymbolTagger(() -> Collections.singleton(tagUnit));
      for (ASTTag tag : tagger.getTags(symbol)) {
        if (tag instanceof ASTValuedTag && name.equals(((ASTValuedTag) tag).getName())) {
          values.add(((ASTValuedTag) tag).getValue());
        }
      }
    }

    if (values.size() > 1) {
      List<String> sortedValues = new ArrayList<>(values);
      Collections.sort(sortedValues);
      Log.error("0xDA201 Conflicting " +
                name +
                " tags on " +
                symbol.getFullName() +
                ": " +
                sortedValues);
    }
    return values.stream().findFirst();
  }
}
