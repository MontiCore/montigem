/* (c) https://github.com/MontiCore/monticore */
package e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static umlp.test_rte.playwright.ComponentKind.GemText;

/**
 * Instant, Duration, LocalTime, Optional<Duration> and List<Instant> attributes in the browser: the transpiled code
 * computes with the values like Java, and the values survive the way to the server and back (a reload shows what the
 * server stored)
 */
public class TimeValuesE2ETest extends AbstractJsweettestE2ETest {

  /** after "Set A", see TestTimeValues.setA */
  private static final String VALUES_A = "start=2026-11-17T08:00:00.123456Z end=2026-11-17T09:30:00.123461Z"
      + " length=PT1H30M0.000005S minutes=90 berlin=2026-11-17T09:00:00.123456 reminder=09:15 pause=none"
      + " history=2026-11-17T08:00:00.123456Z";

  /** after "Set B", see TestTimeValues.setB */
  private static final String VALUES_B = "start=1969-12-31T23:59:59.500Z end=1969-12-31T23:58:29.750Z"
      + " length=PT-1M-29.75S minutes=-1 berlin=1970-01-01T00:59:59.500 reminder=23:59:59.999999 pause=PT48H"
      + " history=1969-12-31T23:59:59.500Z 2026-11-17T09:30:15.000123Z";

  @Test
  void testValuesSurviveTheServer(Page page) {
    open(page);
    page.locator(".setA").click();
    assertThat(summary(page)).hasText(VALUES_A);
    open(page);
    assertThat(summary(page)).hasText(VALUES_A);

    page.locator(".setB").click();
    assertThat(summary(page)).hasText(VALUES_B);
    open(page);
    assertThat(summary(page)).hasText(VALUES_B);
  }

  private void open(Page page) {
    navAndWait(page, "/gui/test/TestTimeValues");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    waitRdy(page);
  }

  private Locator summary(Page page) {
    return page.locator(GemText.selector()).filter(new Locator.FilterOptions().setHasText("start="));
  }
}
