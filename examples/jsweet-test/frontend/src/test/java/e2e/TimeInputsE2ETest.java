/* (c) https://github.com/MontiCore/monticore */
package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * GemTextInput with the input types date, time and datetime-local bound to a LocalDate, LocalTime and LocalDateTime:
 * the input emits the format of the bound type, so the generated parse takes it, like in Java
 */
public class TimeInputsE2ETest extends AbstractJsweettestE2ETest {

  @Test
  void testInputsSetTheBoundValues(Page page) {
    navAndWait(page, "/gui/test/TestTimeInputs");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    waitRdy(page);

    page.locator(".dateInput input").fill("2024-05-07");
    page.locator(".timeInput input").fill("10:30");
    page.locator(".dateTimeInput input").fill("2024-05-07T10:30");

    assertThat(page.locator(".summary")).hasText("date=2024-05-07 time=10:30 dateTime=2024-05-07T10:30");
  }
}
