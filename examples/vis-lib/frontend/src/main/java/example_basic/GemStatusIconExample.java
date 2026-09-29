/* (c) https://github.com/MontiCore/monticore */
package example_basic;

import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component
public class GemStatusIconExample extends GemStatusIconExampleTOP {

  /** Current status of the traffic light, e.g. the name of an enum constant. */
  @Override
  public String currentStatus() {
    return this.getTrafficLight();
  }

  /** Switches the traffic light to the next status: OK -> WARNING -> ERROR -> OK. */
  @Override
  public Consumer<Void> nextStatus() {
    return $ -> {
      String current = this.getTrafficLight();
      if ("OK".equals(current)) {
        this.setTrafficLight("WARNING");
      } else if ("WARNING".equals(current)) {
        this.setTrafficLight("ERROR");
      } else {
        this.setTrafficLight("OK");
      }
    };
  }
}
