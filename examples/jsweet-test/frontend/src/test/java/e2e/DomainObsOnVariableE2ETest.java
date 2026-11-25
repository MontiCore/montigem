/* (c) https://github.com/MontiCore/monticore */
package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.A;
import jsweettest.JsweettestManager;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

/**
 * TestDomainObsOnVariable#valueOfA assigns the A to a variable at the beginning of the method
 * (A a = JsweettestManager.getASure(...)). The text must be updated whenever that A changes, which requires the domain
 * observer to be registered on the variable.
 */
public class DomainObsOnVariableE2ETest extends AbstractJsweettestE2ETest {

  private static final String A_ID_INPUT_SELECTOR = "#domainObsOnVariableAIdInput";

  // the class of a GemText is on the gem-text element itself
  private static final String A_VALUE_SELECTOR = "gem-text.aValue";

  private A a;

  @BeforeEach
  void init(Page page) {
    a = JsweettestManager.aBuilder().value("initial " + System.nanoTime()).build().get();

    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/test/TestDomainObsOnVariable");
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // show the A
    waitRdy(page);
    page.fill(A_ID_INPUT_SELECTOR, String.valueOf(a.getGemId()));
    locator(page, ".show", GemButton).click();
    assertThat(page.locator(A_VALUE_SELECTOR)).hasText("A: " + a.getValue());
  }

  @Test
  void testUpdateInBackendUpdatesGUI(Page page) {
    String value = "renamed in the backend " + System.nanoTime();
    a.setValue(value);

    assertThat(page.locator(A_VALUE_SELECTOR)).hasText("A: " + value);
  }

  @Test
  void testUpdateInGUIUpdatesGUI(Page page) {
    locator(page, ".rename", GemButton).click();

    // TestDomainObsOnVariable#renameA
    assertThat(page.locator(A_VALUE_SELECTOR)).hasText("A: renamed in the GUI");
  }
}
