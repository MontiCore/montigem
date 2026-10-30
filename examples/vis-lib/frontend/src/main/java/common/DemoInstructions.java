/* (c) https://github.com/MontiCore/monticore */
package common;

import umlp.jsweet.extension.annotation.Component;

/**
  Companion of the DemoInstructions card: only the guard of the optional intro
  line. The card itself does not collapse - it always sits inside the
  collapsible common.DemoBox.
*/
@Component
public class DemoInstructions extends DemoInstructionsTOP {

  /** True iff an intro line was configured for this usage. */
  public Boolean hasIntro(String intro) {
    return intro != null && !intro.isEmpty();
  }
}
