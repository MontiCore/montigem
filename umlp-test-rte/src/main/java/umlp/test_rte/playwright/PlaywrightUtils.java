package umlp.test_rte.playwright;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import de.se_rwth.commons.logging.Log;

import java.util.Collection;
import java.util.List;

public interface PlaywrightUtils {
  default void waitForAngular(Page page) {
    //@formatter:off
    page.evaluate("async () => {" +
                  "  if (window.getAllAngularTestabilities) {\n" +
                  "    // @ts-expect-error\n" +
                  "    await Promise.all(window.getAllAngularTestabilities().map(whenStable));\n" +
                  "    \n" +
                  "    // @ts-expect-error\n" +
                  "    async function whenStable(testability) {\n" +
                  "      return new Promise(res => testability.whenStable(res));\n" +
                  "    }\n" +
                  "  }\n" +
                  "}");
    //@formatter:on
  }

  /**
   * Waits until the web socket of the application has connected.
   * <p>
   * <strong>ATTENTION: </strong>
   * <p>Calling this method requires the {@code app.component.ts} to
   * have the following line of code at the end of the constructor:
   * <pre>{@code
   * (<any>window).websocket_connected = true;
   * }</pre>
   * Otherwise, this method can potentially never return!
   *
   * @param page The current {@link Page}
   */
  default void waitForWebsocket(Page page) {
    long start = System.currentTimeMillis();

    Locator.EvaluateOptions options = new Locator.EvaluateOptions();
    options.setTimeout(30_000);
    page.locator("body").evaluate(
        //@formatter:off
        "new Promise(resolve => {\n" +
        "  const checkReady = () => {\n" +
        "    if (window.websocket_connected != undefined) {\n" +
        "      resolve(window.websocket_connected);\n" +
        "    } else {\n" +
        "      setTimeout(checkReady, 100);\n" +
        "    }\n" +
        "  };\n" +
        "  checkReady();\n" +
        "});"
        //@formatter:on
        , null, options);

    Log.info("Waiting for websocket took " + (System.currentTimeMillis() - start) + "ms", PlaywrightUtils.class.getName());
  }

  default boolean waitInit(Page page, String pageName) {
    String pageComponentName = pageName.endsWith("Component") ? pageName : pageName + "Component";
    //@formatter:off
    String command =
        "async () => { \n" +
            "    const startTime = new Date().getTime(); \n" +
            "    while (true) { \n" +
            "        if (window.allVarsInited2 && window.allVarsInited2[\"" + pageComponentName + "\"]) { \n" +
            "            console.log(\"all variables are initialized\"); \n" +
            "            return true; \n" +
            "        } \n" +
            "        if (new Date().getTime() - startTime > 10000) { \n" +
            "            console.log('Timed out waiting for allVarsInited'); \n"
            + "          return false; \n" +
            "        } \n" +
            "        await new Promise(resolve => setTimeout(resolve, 100)); \n" +
            "    } \n" +
            "} \n";
    return Boolean.parseBoolean(page.evaluate(command).toString());
    //@formatter:on
  }

  /**
   * Waits until the page is fully loaded. Has the same requirements as {@link #waitForWebsocket(Page)}
   * Caution for some reason the ordering matters, waitForAngular at the end of this method leads in
   * some cases to long waits that result in a timeout exception inside a playwright test.
   *
   * @param page The {@link Page}
   */
  default void waitRdy(Page page) {
    waitForAngular(page);
    waitForWebsocket(page);
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  /**
   * Name of the global variable in which the frontend ({@code @umlp/commonj2ts},
   * {@code PendingCommands}) publishes the number of commands it is busy with: commands sent to
   * the backend whose result has not arrived yet, and commands received from the backend whose
   * processing has not finished.
   */
  String PENDING_COMMANDS_VARIABLE = "umlp_pending_commands";

  /**
   * Waits until the frontend in the given page has no command in progress.
   * See {@link #waitForBackend(Collection)}.
   *
   * @param page The {@link Page}
   */
  default void waitForBackend(Page page) {
    waitForBackend(List.of(page));
  }

  /**
   * Waits until the frontends in the given pages have no command in progress: every command they
   * sent has been answered by the backend, and every command the backend pushed to them has been
   * processed. The pages are polled every {@link #waitForBackendPollMillis()} ms and must be idle
   * in two consecutive polls, so that a command sent right after the result of a previous one
   * (e.g. {@code GetFreeGemIdRequest} followed by a create command) is waited for as well.
   * <p>
   * Pages that do not show the application (e.g. {@code about:blank}) and closed pages are
   * ignored. A page that does not publish {@link #PENDING_COMMANDS_VARIABLE} yet has no command
   * manager and counts as idle. If the pages are still busy after
   * {@link #waitForBackendTimeoutMillis()} ms, a warning is logged and the method returns.
   *
   * @param pages The {@link Page}s
   */
  default void waitForBackend(Collection<Page> pages) {
    long start = System.currentTimeMillis();
    long deadline = start + waitForBackendTimeoutMillis();
    int idlePolls = 0;
    while (true) {
      boolean idle = true;
      String busy = null;
      for (Page page : pages) {
        if (page.isClosed() || !page.url().startsWith(getUrlBase())) {
          continue;
        }
        Object pending;
        try {
          pending = page.evaluate("() => window." + PENDING_COMMANDS_VARIABLE);
        } catch (PlaywrightException e) {
          if (page.isClosed()) {
            continue;
          }
          // e.g. the page is navigating
          idle = false;
          busy = page.url() + " (" + e.getMessage() + ")";
          continue;
        }
        if (pending instanceof Number count && count.intValue() > 0) {
          idle = false;
          busy = page.url() + " (" + count.intValue() + " commands pending)";
        }
      }
      idlePolls = idle ? idlePolls + 1 : 0;
      if (idlePolls >= 2) {
        break;
      }
      if (System.currentTimeMillis() > deadline) {
        Log.warn("Waiting for the backend timed out after " + (System.currentTimeMillis() - start)
            + "ms, still busy: " + busy);
        break;
      }
      sleepForBackend(waitForBackendPollMillis());
    }
    Log.info("Waiting for the backend took " + (System.currentTimeMillis() - start) + "ms",
        PlaywrightUtils.class.getName());
  }

  /**
   * @return The maximum time in ms {@link #waitForBackend(Collection)} waits for busy pages
   */
  default long waitForBackendTimeoutMillis() {
    return 10_000;
  }

  /**
   * @return The time in ms between two polls of {@link #waitForBackend(Collection)}
   */
  default long waitForBackendPollMillis() {
    return 50;
  }

  private static void sleepForBackend(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to wait for backend", e);
    }
  }

  /**
   * Waits for a {@link Dialog} to occur on the provided {@link Page} while the trigger is run
   *
   * @param page    {@link Page} where the {@link Dialog} should occur
   * @param trigger {@link Runnable} which should trigger the {@link Dialog}
   * @return A {@link AwaitDialog} instance
   */
  default AwaitDialog awaitDialog(Page page, Runnable trigger) {
    return new AwaitDialog(page, trigger);
  }

  String getUrlBase();

  default String getSaveUrlBase() {
    return getUrlBase().endsWith("/") ? getUrlBase() : getUrlBase() + "/";
  }

  /**
   * Navigates to a new route and waits for the website to reload.
   * <p>
   * Has the same requirements as {@link #waitRdy(Page)}!
   * <p>
   * <strong>Example</strong>
   * <pre>{@code
   * navAndWait(page, "/person/Overview");
   * }</pre>
   *
   * @param page  The {@link Page}
   * @param route Where to navigate
   */
  default void navAndWait(Page page, String route) {
    page.navigate(getSaveUrlBase() + route.replaceAll("^/*", ""));
    waitRdy(page);
  }

  /**
   * Gets the first {@link Locator} that matches the given selector from a {@link Page} and from that gets the
   * underlying html element of the respective GuiDSL component.
   *
   * <p>Should be preferred over statements like {@code page.locator(".foo > button")} since the locator of the
   * underlying html element may change
   *
   * <p><strong>Example</strong>
   * <pre>{@code
   * locator(page, ".foo", ComponentKind.GemButton).click();
   * }</pre>
   *
   * @param page     The {@link Page} to get the {@link Locator} from
   * @param selector Selector for the {@link Locator} to get
   * @param kind     What kind of component the selector denounces
   * @return The respective {@link Locator}
   */
  default Locator locator(Page page, String selector, ComponentKindLike kind) {
    return locator(new PageAdapter(page), selector, kind);
  }

  /**
   * Gets the first {@link Locator} that matches the given selector inside another {@link Locator} and from that gets the
   * underlying html element of the respective GuiDSL component.
   *
   * <p>Should be preferred over statements like {@code locator.locator(".foo > button")} since the locator of the
   * underlying html element may change
   *
   * <p><strong>Example</strong>
   * <pre>{@code
   * locator(locator, ".foo", ComponentKind.GemButton).click();
   * }</pre>
   *
   * @param locator  The {@link Locator} to get the next {@link Locator} from
   * @param selector Selector for the {@link Locator} to get
   * @param kind     What kind of component the selector denounces
   * @return The respective {@link Locator}
   */
  default Locator locator(Locator locator, String selector, ComponentKindLike kind) {
    return locator(new LocatorAdapter(locator), selector, kind);
  }

  default Locator locator(LocatorHolder locatorHolder, String selector, ComponentKindLike kind) {
    return locatorHolder.locator(selector).first().locator(kind.selector());
  }

  //Page and Locator each define the locator function themselves
  public interface LocatorHolder {
    Locator locator(String selector);
  }

  public static class PageAdapter implements LocatorHolder {

    private final Page page;

    public PageAdapter(Page page) {
      this.page = page;
    }

    @Override
    public Locator locator(String selector) {
      return page.locator(selector);
    }
  }

  public static class LocatorAdapter implements LocatorHolder {
    private final Locator locator;

    public LocatorAdapter(Locator locator) {
      this.locator = locator;
    }

    @Override
    public Locator locator(String selector) {
      return locator.locator(selector);
    }
  }
}
