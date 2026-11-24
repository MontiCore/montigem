package loadingtests.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

import static loadingtests.e2e.E2EUtil.createDummyData;
import static loadingtests.e2e.E2EUtil.isLoadedPersonFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

@EnabledIfEnvironmentVariable(named = "WITH_JSWEET", matches = "TRUE")
public class TestForNoErrorMessages extends AbstractLoadingTestsE2ETest {

  private List<Long> personGemIds;
  private List<Long> animalGemIds;
  private List<Long> foodGemIds;

  private static int wait = 0;

  @BeforeEach
  void init(Page page) {
    this.personGemIds= new ArrayList<>();
    this.animalGemIds= new ArrayList<>();
    this.foodGemIds= new ArrayList<>();
    createDummyData(this.personGemIds, this.animalGemIds, this.foodGemIds);
    page.onConsoleMessage(cm ->{
      if(cm.type().equalsIgnoreCase("error")) {
        fail("JavaScript error thrown in Browser: "+ cm.text());
      }
      System.out.println("[BROWSER] " + cm.text());
      });
  }

  /**
   * Given a page that can get an AnimalBuilder as input and displays its food association.
   * We expect there to be no thrown Errors when we navigate on the page and away from the page.
   * @param page
   */
  @Test
  void testUpdateWithBuilder(Page page) {
    navAndWait(page, "/test_no_error/BuilderAndRouterNavigation"); //go to page we want to test
    waitForAngular(page);

    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Navigate to another page
    page.locator(GemButton.selector()).first().click();

    page.waitForURL("**/Persons");
    assertTrue(page.url().endsWith("test_update_after_build/Persons"));

    waitRdy(page);

    // Assert all persons were loaded
    // isLoadedPerson is async in the browser: page.evaluate awaits its result
    personGemIds.forEach(id -> waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedPersonFunction, id.intValue())),
        "person " + id + " in the browser"));
    waitRdy(page);
  }

}
