package e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

public class TestGeneralErrorCollectorE2ETest extends AbstractJsweettestE2ETest {

  Page page;

  @BeforeEach
  void init(Browser browser) {
    page = browser.newPage();
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/test/TestGeneralErrorCollector"); // go to page we want to test
    page.waitForLoadState(LoadState.NETWORKIDLE);
    page.getByText("Reset Data").click();
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  @AfterEach
  void close() {
    // an open page keeps its websocket, then the backend clean-up waits 30 s for it
    page.close();
  }

  @Test
  void testAddOnSyncError() {

    List<String> syncErrors = new ArrayList<>();
    page.onConsoleMessage(consoleMessage -> {
      if (consoleMessage.text().startsWith("notifySyncError")) {
        syncErrors.add(consoleMessage.text());
      }
    });

    final int default_c2s = 5;

    assertThat(page.getByText("Got a C2")).hasCount(default_c2s + 0);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("add")).click();
    assertThat(page.getByText("Got a C2")).hasCount(default_c2s + 1);
    assertEquals(0, syncErrors.size());
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("sync error")).click();
    assertThat(page.getByText("Got a C2")).hasCount(default_c2s + 1);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("add")).click();
    assertThat(page.getByText("Got a C2")).hasCount(default_c2s + 2);
    assertEquals(1, syncErrors.size());
    assertEquals("notifySyncErrorSync error!", syncErrors.getFirst());
  }

}
