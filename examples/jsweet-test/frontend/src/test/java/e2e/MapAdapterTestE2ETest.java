package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * java.util.Map in the browser: the page gui.test.MapAdapterTest runs the scenarios on maps transpiled to AsyncMap,
 * the test checks what the page shows
 */
public class MapAdapterTestE2ETest extends AbstractJsweettestE2ETest {

  private static final String RESULT_INPUT_SELECTOR = "#mapResultIdInput";
  private static final String DATA_INPUT_SELECTOR = "#mapInputTestFieldIdInput";

  @BeforeEach
  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/gui/test/MapAdapterTest");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    setInput(page, "a=1, b=2, c=3");
  }

  private void setInput(Page page, String value) {
    waitRdy(page);
    assertThat(page.locator(DATA_INPUT_SELECTOR)).isVisible();
    page.fill(DATA_INPUT_SELECTOR, value);
  }

  private void checkActionExact(Page page, String buttonClass, String expectedExactValue) {
    page.locator("." + buttonClass).click();
    waitRdy(page);
    assertThat(page.locator(RESULT_INPUT_SELECTOR)).hasValue(expectedExactValue);
  }

  @Test
  void testPutAndGet(Page page) {
    checkActionExact(page, "putGet", "put(): added=null, replaced=1, get(a)=10, get(z)=null, size=4");
  }

  @Test
  void testInsertionOrder(Page page) {
    checkActionExact(page, "order", "order: a=10, b=2, c=3, d=4");
  }

  @Test
  void testContainsAndRemove(Page page) {
    checkActionExact(page, "containsRemove",
        "containsKey(b)=true, containsValue(3)=true, remove(b)=2, remove(z)=null, map=a=1, c=3");
  }

  @Test
  void testIterate(Page page) {
    checkActionExact(page, "iterate", "keys=abc, sum=6, entries=a=1, b=2, c=3");
  }

  @Test
  void testDefaultMethods(Page page) {
    checkActionExact(page, "defaults", "getOrDefault=-1, map=a=1, b=12, c=300, e=5, f=6, groups=2/1");
  }

  @Test
  void testLiveViews(Page page) {
    checkActionExact(page, "views", "views: map=b=4, c=6, x=7, keySet sees put=true");
  }

  @Test
  void testEqualityAndCopy(Page page) {
    checkActionExact(page, "equality", "equals(copy)=true, afterChange=false, original a=1");
  }

  /** the field keeps its entries across the clicks */
  @Test
  void testMapField(Page page) {
    checkActionExact(page, "field", "field clicks=1");
    checkActionExact(page, "field", "field clicks=2");
  }

  @Test
  void testTimeKeys(Page page) {
    checkActionExact(page, "timeKeys", "date keys: size=1, visits=2, containsKey=true");
  }

  @Test
  void testIsEmptyAndClear(Page page) {
    checkActionExact(page, "isEmptyClear", "isEmptyClear: before=true, afterPutAll=false, afterClear=true, size=0");
  }

  /** the scenarios work on the entries entered on the page */
  @Test
  void testOtherInput(Page page) {
    setInput(page, "x=5, y=7");
    checkActionExact(page, "iterate", "keys=xy, sum=12, entries=x=5, y=7");
  }
}
