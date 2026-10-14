package example_advanced;

import umlp.guiutil.HTMLUtils;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component
public class GemToggleExample extends GemToggleExampleTOP {

  @Override
  public Consumer<Boolean> showState() {
    return state -> HTMLUtils.alert(state ? "Toggle is on" : "Toggle is off");
  }

  @Override
  public Consumer<Void> switchFromCode() {
    return $ -> this.setCodeSwitchedOn(!this.isCodeSwitchedOn());
  }
}
