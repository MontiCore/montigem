package gui.kitchen;

import gui.admin.LoginServiceFront;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component
public class KitchenPage extends KitchenPageTOP {

  @Override
  public void init() {
    super.init();
    LoginServiceFront.setLoggedIn(false);
  }

  /**
   * Receives the letter picked in the search bar. Writing it to the page parameter re-renders the
   * panels; because it is page state and not stored, it is gone as soon as the page is left.
   */
  @Override
  public Consumer<String> selectLetter() {
    return letter -> setSearchLetter(letter);
  }
}
