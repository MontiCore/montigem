/* (c) https://github.com/MontiCore/monticore */
package navigation;

import umlp.jsweet.extension.annotation.Component;
import umlp.guiutil.Router;
import java.util.function.Consumer;

@Component
public class Navigation extends NavigationTOP {

  @Override
  public void init() {
    super.init();
  }

  public Consumer<Void> setCategoryBasic() {
    return v -> setActiveCategory("Basic");
  }

  public Consumer<Void> setCategoryInput() {
    return v -> setActiveCategory("Input");
  }

  public Consumer<Void> setCategoryArrange() {
    return v -> setActiveCategory("Arrange");
  }

  public Consumer<Void> setCategoryCharts() {
    return v -> setActiveCategory("Charts");
  }
  public Consumer<Void> setCategoryTest() {
    return v -> setActiveCategory("Test");
  }

  public Consumer<Void> setCategoryOther() {
    return v -> setActiveCategory("Other");
  }
}
