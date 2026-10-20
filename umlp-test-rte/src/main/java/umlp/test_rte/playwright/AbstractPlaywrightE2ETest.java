package umlp.test_rte.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

/**
 * An abstract base for end-to-end tests which use playwright. When using this class it is
 * important that at the end of your constructor in {@code app.component.ts} there is the line
 * <pre>{@code (<any>window).websocket_connected = true;}</pre>
 * <p>
 * <strong>Debugging</strong>
 * <p>Playwright tests can be debugged by modifying the run configuration of the test to use the
 * environment variable {@code PWDEBUG=1}
 */
@UsePlaywright
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Timeout(120)
// restart backend for every method
public abstract class AbstractPlaywrightE2ETest implements PlaywrightUtils {

  static final int frontendServerPort = getFreePort();
  static final int backendServerPort = getFreePort();

  protected FrontendServer4Test frontendServer;

  /**
   * The browser of the current test, used by {@link #waitForBackend()}
   */
  private Browser currentBrowser;

  @Override
  public String getUrlBase() {
    return "http://localhost:" + getFrontendServerPort();
  }

  public static int backendServerPort() {
    return backendServerPort;
  }

  /**
   * Gets an unbound port from the os
   *
   * @return The free port
   * @throws RuntimeException If no free port was found for whatever reason
   */
  protected static int getFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected int getFrontendServerPort() {
    return frontendServerPort;
  }

  protected int getBackendServerPort() {
    return backendServerPort;
  }

  /**
   * Gets the directory to which the frontend app has been build to.
   *
   * <p>By default this directory is assumed to be {@code build/app/dist/client}
   *
   * @return The {@link File} denoting the app directory
   */
  protected File getFrontendDistDirectory() {
    return new File("build/app/dist/client/browser");
  }

  /**
   * Starts the frontendServer with the {@link FrontendServer4Test}
   *
   * @throws IOException When an {@link IOException} occurs while starting the frontend
   */
  @BeforeAll
  protected void startFrontend() throws IOException {
    frontendServer = new FrontendServer4Test(
        getFrontendDistDirectory(),
        "http://localhost:" + getBackendServerPort(),
        getFrontendServerPort()
    );
    Log.info("Starting the FrontEnd Server", this.getClass().getName());
    frontendServer.start();
  }

  /**
   * Stops the {@link #frontendServer} if it is present
   */
  @AfterAll
  protected void stopFrontend() {
    if(frontendServer != null) {
      Log.info("Stopping the FrontEnd Server", this.getClass().getName());
      frontendServer.stop();
    }
  }

  /**
   * Remembers the browser of the current test, so that {@link #waitForBackend()} can wait for
   * all of its pages. This is the browser that also provides the {@link Page} parameters.
   *
   * @param browser The {@link Browser} of the current test
   */
  @BeforeEach
  protected void rememberPlaywrightBrowser(Browser browser) {
    this.currentBrowser = browser;
  }

  /**
   * Waits until no page of the current test has a command in progress, i.e. until all commands
   * have reached the backend, have been processed and their results have arrived.
   * See {@link #waitForBackend(java.util.Collection)}.
   */
  protected void waitForBackend() {
    List<Page> pages = currentBrowser == null ? List.of() : currentBrowser.contexts().stream()
        .flatMap(context -> context.pages().stream())
        .toList();
    waitForBackend(pages);
  }

  // After Each is generated

}
