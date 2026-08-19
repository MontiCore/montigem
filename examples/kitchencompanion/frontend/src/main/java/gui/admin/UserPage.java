package gui.admin;

import java.util.function.Consumer;

public class UserPage extends UserPageTOP {
  
  @Override
  public Consumer<String> selectLetter() {
    return letter -> setSearchLetter(letter);
  }
  
}
