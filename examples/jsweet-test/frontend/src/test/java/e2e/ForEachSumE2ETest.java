/* (c) https://github.com/MontiCore/monticore */
package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.C1;
import jsweettest.C2;
import jsweettest.JsweettestManager;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

/**
 * TestForEachSum#sumsOfC2s sums the attributes of the C2s of a C1 in a for each loop. The text must be updated
 * whenever an attribute of one of these C2s changes, which requires the domain observer to be registered on the loop
 * variable.
 */
public class ForEachSumE2ETest extends AbstractJsweettestE2ETest {

  private static final String C1_ID_INPUT_SELECTOR = "#forEachSumC1IdInput";

  // the class of a GemText is on the gem-text element itself
  private static final String SUMS_SELECTOR = "gem-text.sums";

  private C2 first;

  private C2 second;

  private C2 third;

  @BeforeEach
  void init(Page page) {
    String marker = "for-each-sum-" + System.nanoTime();
    first = newC2(marker + ":first", 1, 10);
    second = newC2(marker + ":second", 2, 20);
    third = newC2(marker + ":third", 3, 30);
    C1 c1 = JsweettestManager.c1Builder()
        .string(marker)
        .int1(0)
        .int2(0)
        .hwGemId(-1L)
        .c2Req(first)
        .build().get();
    c1.addC2mult(first);
    c1.addC2mult(second);
    c1.addC2mult(third);

    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/test/TestForEachSum");
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // show the sums of the C2s of the C1
    waitRdy(page);
    page.fill(C1_ID_INPUT_SELECTOR, String.valueOf(c1.getGemId()));
    locator(page, ".show", GemButton).click();
    assertThat(page.locator(SUMS_SELECTOR)).hasText("int1: 6, int2: 60");
  }

  private C2 newC2(String string, int int1, int int2) {
    return JsweettestManager.c2Builder()
        .string(string)
        .int1(int1)
        .int2(int2)
        .build().get();
  }

  @Test
  void testAttributeChangeOfLoopElementUpdatesGUI(Page page) {
    second.setInt1(12);

    assertThat(page.locator(SUMS_SELECTOR)).hasText("int1: 16, int2: 60");
  }

  @Test
  void testAttributeChangesOfSeveralLoopElementsUpdateGUI(Page page) {
    first.setInt2(100);
    third.setInt2(300);

    assertThat(page.locator(SUMS_SELECTOR)).hasText("int1: 6, int2: 420");
  }
}
