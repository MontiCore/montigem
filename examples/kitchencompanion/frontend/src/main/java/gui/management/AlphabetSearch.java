package gui.management;

import kitchencompanion.UserFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AlphabetSearch extends AlphabetSearchTOP {
  private static final String[] ALPHABET = {
      "A", "B", "C", "D", "E", "F", "G",
      "H", "I", "J", "K", "L", "M", "N",
      "O", "P", "Q", "R", "S", "T", "U",
      "V", "W", "X", "Y", "Z"
  };
  private List<String> list;

  @Override
  public List<String> getAlphabet() {
    if (list == null) {
      list = new ArrayList<>();
      for (String s : ALPHABET) {
        list.add(s);
      }
    }
    return list;
  }
  
  @Override
  public Consumer<Void> select(String str) {
    return $ -> report(str);
  }

  @Override
  public Consumer<Void> clearSelection() {
    return $ -> report("");
  }

  private void report(String letter) {
    if (getOnSelect() != null) {
      getOnSelect().accept(letter);
    }
  }


  @Override
  public Boolean getDisabled(String str) {
    if (str.equals(getSelectedLetter())) {
      return true;
    }
    if (isUserOrOffer()) {
      return UserFilter.noUserFor(str);
    }
    return UserFilter.noOfferFor(str);
  }
  
  @Override
  public Boolean isDisabled() {
    return getSelectedLetter() == null || getSelectedLetter().isEmpty();
  }
}
