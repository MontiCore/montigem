package dashboard.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import dashboard.test_infrastructure.AbstractDashboardE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static umlp.test_rte.playwright.ComponentKind.GemButton;

public class E2ETest extends AbstractDashboardE2ETest{
//-------------------------------------------------------------------------------------------------
//Tests for synchronization for cells and views of the config object with the frontend element
//Current version here cannot be verified if it actually works because of local machine problems
//with tests initialization, but the template/idea should be correct.
//-------------------------------------------------------------------------------------------------
//  @BeforeEach
//  void init(Page page) {
//    page.onConsoleMessage(cm -> System.out.println("[BROWSER1] " + cm.text()));
//    navAndWait(page, "/home/Testing"); //go to page we want to test
//  }
//
//  @Test
//  void testDeleteCell(Page page) {
//    waitRdy(page);
//    locator(page,".editButton", GemButton).click();
//    waitRdy(page);
//    Locator cell = page.locator("app-cell").first();
//    String cellId = cell.getAttribute("id");
//    locator(cell,".deleteButton", GemButton).click();
//    waitForBackend();
//    page.waitForLoadState();
//    assertThat(page.locator("#" + cellId)).hasCount(0);
//
//  }
//
//  @Test
//  void testCreateCell(Page page) {
//    waitRdy(page);
//    int before = page.locator("app-cell").count();
//    locator(page,".createButton", GemButton).click();
//    waitRdy(page);
//    locator(page, ".endButton1", GemButton).click();
//    waitRdy(page);
//    locator(page, ".finishButton2", GemButton).click();
//    waitForBackend();
//    int after = page.locator("app-cell").count();
//    assertEquals(before + 1, after);
//
//  }
//
//  @Test
//  void testDeleteView(Page page) {
//    waitRdy(page);
//    int before = page.locator("view").count();
//    locator(page,".closeButton2", GemButton).click();
//    waitRdy(page);
//    int after = page.locator("view").count();
//    assertEquals(before - 1, after);
//
//  }
//
//  @Test
//  void testCreateView(Page page) {
//    waitRdy(page);
//    int before = page.locator("view").count();
//    locator(page,".addView2", GemButton).click();
//    waitRdy(page);
//    int after = page.locator("app-cell").count();
//    assertEquals(before + 1, after);
//
//  }
}
