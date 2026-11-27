/* (c) https://github.com/MontiCore/monticore */
package gui.test;

import def.dom.HTMLInputElement;
import jsweettest.C1;
import jsweettest.C2;
import jsweettest.JsweettestManager;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

import static def.dom.Globals.document;

@Component
public class TestForEachSum extends TestForEachSumTOP {

  /**
   * Sums the attributes of the C2s of the C1 in a for each loop. The C2s are only accessed via the loop variable, so
   * the text is only updated on changes of a C2 if the domain observer is registered on the loop variable.
   */
  @Override
  public String sumsOfC2s() {
    C1 c1 = JsweettestManager.getC1Sure(getC1Id());
    int sumInt1 = 0;
    int sumInt2 = 0;
    for (C2 c2 : c1.getC2multList()) {
      sumInt1 += c2.getInt1();
      sumInt2 += c2.getInt2();
    }
    return "int1: " + sumInt1 + ", int2: " + sumInt2;
  }

  @Override
  public Consumer<Void> showC1() {
    return Void -> {
      HTMLInputElement input = (HTMLInputElement) document.getElementById("forEachSumC1IdInput");
      setC1Id(Long.parseLong(input.value.trim()));
    };
  }
}
