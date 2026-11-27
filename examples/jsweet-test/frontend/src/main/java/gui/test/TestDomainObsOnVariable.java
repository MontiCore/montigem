/* (c) https://github.com/MontiCore/monticore */
package gui.test;

import def.dom.HTMLInputElement;
import jsweettest.A;
import jsweettest.JsweettestManager;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

import static def.dom.Globals.document;

@Component
public class TestDomainObsOnVariable extends TestDomainObsOnVariableTOP {

  /**
   * The A is assigned to a variable at the beginning of the method (instead of being a parameter), so the text is only
   * updated on changes of the A if the domain observer is registered on that variable (__regObsToDomainObj).
   */
  @Override
  public String valueOfA() {
    A a = JsweettestManager.getASure(getAId());
    return "A: " + a.getValue();
  }

  @Override
  public Consumer<Void> showA() {
    return Void -> {
      HTMLInputElement input = (HTMLInputElement) document.getElementById("domainObsOnVariableAIdInput");
      setAId(Long.parseLong(input.value.trim()));
    };
  }

  /** updates the A in the GUI */
  @Override
  public Consumer<Void> renameA() {
    return Void -> {
      A a = JsweettestManager.getASure(getAId());
      a.setValue("renamed in the GUI");
    };
  }
}
