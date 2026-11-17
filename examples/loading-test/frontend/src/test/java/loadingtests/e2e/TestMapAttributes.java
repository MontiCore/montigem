/* (c) https://github.com/MontiCore/monticore */
package loadingtests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import loadingtests.Gradebook;
import loadingtests.LoadingTestsManager;
import loadingtests.MyEnum;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

/**
 * Map attributes with simple values in the browser: the page test_mapattrs/TestMapAttributes runs the scenarios in
 * the transpiled domain classes, the test checks the backend after synchronization and what the page read
 */
public class TestMapAttributes extends AbstractLoadingTestsE2ETest {

  Gradebook gradebook;

  @BeforeEach
  void init(Page page) {
    this.gradebook = LoadingTestsManager.gradebookBuilder().gradesPut("math", 1).build().get();
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }

  void open(Page page) {
    navAndWait(page, "/test_mapattrs/TestMapAttributes/" + gradebook.getGemId());
    waitRdy(page);
  }

  void click(Page page, String label) {
    page.locator(GemButton.selector() + ":text-is(\"" + label + "\")").click();
    waitRdy(page);
  }

  Locator result(Page page) {
    return page.locator(GemText.selector()).first();
  }

  /**
   * clicks the button until the result contains the text: a command pushed by the backend reaches the browser
   * asynchronously
   */
  void clickUntil(Page page, String label, String text) {
    waitUntil(() -> {
      click(page, label);
      return result(page).textContent().contains(text);
    }, "\"" + label + "\" never showed " + text);
    assertThat(result(page)).hasText(text);
  }

  @Test
  public void testLoadedInOrder(Page page) {
    gradebook.putGrades("art", 2);
    open(page);
    clickUntil(page, "Read Grades", "math=1;art=2");
  }

  @Test
  public void testPutReplaceRemove(Page page) {
    open(page);
    click(page, "Put Grade");
    click(page, "Replace Grade");
    waitUntil(() -> gradebook.sizeGradess() == 2 && gradebook.getGrades("math") == 3,
        "backend: math=3 and art=2");
    assertEquals(List.of("math", "art"), List.copyOf(gradebook.getGradesMap().keySet()));
    click(page, "Remove Grade");
    waitUntil(() -> gradebook.getGradesMap().equals(Map.of("art", 2)), "backend: art=2 only");
  }

  @Test
  public void testBackendChangeReachesBrowser(Page page) {
    open(page);
    gradebook.putGrades("art", 2);
    clickUntil(page, "Read Grades", "math=1;art=2");
  }

  @Test
  public void testLongKeyAndEnumValue(Page page) {
    open(page);
    click(page, "Put State");
    waitUntil(() -> gradebook.containsKeyStates(9007199254740991L), "backend: state under the max key");
    assertEquals(MyEnum.B, gradebook.getStates(9007199254740991L));
    clickUntil(page, "Read States", "size=1;max=B");
  }

  @Test
  public void testDateValue(Page page) {
    gradebook.putDueDates("math", LocalDate.of(2026, 11, 17));
    open(page);
    click(page, "Copy Due Date");
    waitUntil(() -> gradebook.containsKeyDueDates("copy"), "backend: copied due date");
    assertEquals(LocalDate.of(2026, 11, 17), gradebook.getDueDates("copy"));
  }
}
