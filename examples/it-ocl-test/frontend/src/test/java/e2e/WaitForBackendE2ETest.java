package e2e;

import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import mydomain.test_infrastructure.AbstractMydomainE2ETest;
import org.junit.jupiter.api.Test;
import umlp.test_rte.playwright.ComponentKind;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * {@link #waitForBackend()} must wait until all commands of an action have been answered, also
 * if the browser is slow. The button below sends 8 commands one after another
 * (4 x GetFreeGemIdRequest, 3 x LinkedAccessTarget_create, LinkedAccessRoot_create).
 * <p>
 * The browser is slowed down with Chromium's CPU throttling, and the page is closed right after
 * {@link #waitForBackend()}, so that commands which have not been sent by then are lost. With a
 * fixed wait of 200 ms, the root object does not reach the backend.
 */
public class WaitForBackendE2ETest extends AbstractMydomainE2ETest {

  private static final double CPU_THROTTLING_RATE = 10;

  @Test
  void waitsForAllCommandsOfASlowBrowser(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/Start");

    CDPSession cdp = page.context().newCDPSession(page);
    JsonObject params = new JsonObject();
    params.addProperty("rate", CPU_THROTTLING_RATE);
    cdp.send("Emulation.setCPUThrottlingRate", params);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
        .setName("Add Linked Access Root With Too Many Targets")
        .setExact(true)).click();
    waitForBackend();
    page.close();

    Page check = page.context().newPage();
    navAndWait(check, "/ocl2java/errormessages/ErrorMsgs1Page");
    waitForAngular(check);

    // invariant 3: at most 2 targets, violated by the root (gem id 0)
    locator(check, ".errorMsgs1Inv3", ComponentKind.GemButton).click();
    waitForAngular(check);

    assertThat(check.locator(".resultGemText")).hasText("0");
  }
}
