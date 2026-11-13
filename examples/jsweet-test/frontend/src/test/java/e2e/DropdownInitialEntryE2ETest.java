package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression test: a dropdown with a preset entry has to show that entry on init. */
public class DropdownInitialEntryE2ETest extends AbstractJsweettestE2ETest {

  @BeforeEach
  void init(Page page) {
    navAndWait(page, "/gui/test/DropdownInitialEntry");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  @Test
  void showsPresetEntryOnInit(Page page) {
    assertThat(page.locator("gem-dropdown-input .gem-dropdown-text")).hasText("Second");
  }

  @Test
  void selectingAnotherOptionUpdatesDisplay(Page page) {
    page.locator("gem-dropdown-input .gem-dropdown-input").click();
    page.locator(".gem-dropdown-options").getByText("Third").click();
    assertThat(page.locator("gem-dropdown-input .gem-dropdown-text")).hasText("Third");
  }
}
