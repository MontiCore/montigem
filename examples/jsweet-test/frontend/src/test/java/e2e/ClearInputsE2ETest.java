/* (c) https://github.com/MontiCore/monticore */
package e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jsweettest.ClearableInputs;
import jsweettest.EnumTestEnum;
import jsweettest.JsweettestDAOFactory;
import jsweettest.JsweettestManager;
import jsweettest.test_infrastructure.AbstractJsweettestE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clears text inputs bound to optional attributes that the generated GUI code converts from the
 * input string: a cleared input empties the attribute in the frontend and in the backend.
 */
public class ClearInputsE2ETest extends AbstractJsweettestE2ETest {

  protected final List<String> browserErrors = Collections.synchronizedList(new ArrayList<>());

  protected final List<String> failedResponses = Collections.synchronizedList(new ArrayList<>());

  protected final List<String> sentCommands = Collections.synchronizedList(new ArrayList<>());

  protected ClearableInputs inputs;

  @BeforeEach
  void init(Page page) {
    inputs = JsweettestManager.clearableInputsBuilder()
        .birthday(Optional.of(LocalDate.of(2026, 10, 18)))
        .appointment(Optional.of(LocalDateTime.of(2026, 10, 18, 8, 30)))
        .amount(Optional.of(7))
        .letter(Optional.of(EnumTestEnum.B))
        .build().get();

    page.onConsoleMessage(cm -> {
      System.out.println("[BROWSER] " + cm.text());
      if ("error".equals(cm.type())) {
        browserErrors.add(cm.text());
      }
    });
    page.onPageError(browserErrors::add);
    page.onWebSocket(ws -> {
      ws.onFrameSent(f -> sentCommands.add(f.text()));
      ws.onFrameReceived(f -> {
        if (f.text().contains("\"successful\":false")) {
          failedResponses.add(f.text());
        }
      });
    });
    navAndWait(page, "/gui/test/ClearInputs");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    waitRdy(page);
  }

  @Test
  void testClearOptionalDate(Page page) {
    testClear(page, "birthday", ClearableInputs::getBirthday);
  }

  @Test
  void testClearOptionalDateTime(Page page) {
    testClear(page, "appointment", ClearableInputs::getAppointment);
  }

  @Test
  void testClearOptionalNumber(Page page) {
    testClear(page, "amount", ClearableInputs::getAmount);
  }

  @Test
  void testClearOptionalEnum(Page page) {
    testClear(page, "letter", ClearableInputs::getLetter);
  }

  protected void testClear(Page page, String id, Function<ClearableInputs, Optional<?>> getter) {
    assertTrue(getter.apply(inputs).isPresent());
    String setCommand = "\"jsweettest.commands.ClearableInputs_set"
        + id.substring(0, 1).toUpperCase() + id.substring(1) + "\"";

    page.fill("#" + id + "Input", "");

    awaitBackend(() -> getter.apply(inputs).isEmpty());
    // the frontend sends the attribute as an empty optional, i.e., without a value
    List<String> sent = sentCommands.stream().filter(c -> c.contains(setCommand)).toList();
    assertEquals(1, sent.size(), "sent set commands: " + sentCommands);
    assertFalse(sent.get(0).contains("\"" + id + "\""), sent.get(0));
    assertTrue(getter.apply(inputs).isEmpty(), "attribute of the backend object");
    ClearableInputs stored = JsweettestDAOFactory.getClearableInputsDAO()
        .selectClearableInputs(inputs.getGemId()).get();
    assertTrue(getter.apply(stored).isEmpty(), "stored attribute");
    assertEquals(List.of(), failedResponses, "failed commands");
    assertEquals(List.of(), browserErrors, "errors in the browser");
  }

  /**
   * Waits up to 5 seconds until the condition holds in the backend.
   */
  protected void awaitBackend(BooleanSupplier condition) {
    long end = System.currentTimeMillis() + 5000;
    while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
      waitForBackend();
    }
  }
}
