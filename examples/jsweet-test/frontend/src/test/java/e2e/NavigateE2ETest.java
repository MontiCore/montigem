package e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

public class NavigateE2ETest extends AbstractJsweettestE2ETest {
  
  Page page;

  @BeforeEach
  void init(Browser browser) {
    page = browser.newPage();
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/test/NavigateSource"); // go to page we want to test
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }
  
  @AfterEach
  void close() {
    // an open page keeps its websocket, then the backend clean-up waits 30 s for it
    page.close();
  }
  
  @Test
  void testSimpleNavigation() {
    locator(page, ".navTest0", GemButton).click();
    assertThat(page).hasURL(Pattern.compile(".*/gui/test/Test0$"));
  }
  
  @Test
  void testSimpleStringNavigation() {
    locator(page, ".navTestString", GemButton).click();
    assertThat(page).hasURL(Pattern.compile(".*/gui/test/Test0$"));
  }
  
  @Test
  void testNavigationWithList() {
    locator(page, ".navTest1", GemButton).click();
    assertThat(page).hasURL(Pattern.compile(".*/gui/test/Test1$"));
  }
  
  @Test
  void testNavigationWithOneRoutingId() {
    locator(page, ".navTestE", GemButton).nth(0).click();
    assertThat(page).hasURL(Pattern.compile(".*/gui/test/NavigateTargetWithId/\\d+$"));
  }
  
  @Test
  void testNavigationWithTwoRoutingIds() {
    locator(page, ".navTestEA", GemButton).nth(0).click();
    assertThat(page).hasURL(Pattern.compile(".*/gui/test/NavigateTargetWithIds/\\d+/\\d+$"));
  }

}
