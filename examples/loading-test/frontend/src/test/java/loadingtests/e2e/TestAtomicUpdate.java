package loadingtests.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import loadingtests.Food;
import loadingtests.LoadingTestsManager;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static loadingtests.e2e.E2EUtil.createDummyData;
import static loadingtests.e2e.E2EUtil.isLoadedAnimalFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

public class TestAtomicUpdate extends AbstractLoadingTestsE2ETest {

  private List<Long> personGemIds;
  private List<Long> animalGemIds;
  private List<Long> foodGemIds;

  @BeforeEach
  void init(Page page) {
    this.personGemIds = new ArrayList<>();
    this.animalGemIds = new ArrayList<>();
    this.foodGemIds = new ArrayList<>();
    createDummyData(this.personGemIds, this.animalGemIds, this.foodGemIds);
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }

  /**
   * Test if the backend (or another client) can add & remove an element from the same list
   * See <a href="https://git.rwth-aachen.de/monticore/monticore/-/issues/4378">#4378</a>
   * <p/>
   * [BROWSER] Exec command 3 loadingtests.commands.ReplaceFoodItem from 1 Client 1
   * [BROWSER] {"id":3,"commandType":"loadingtests.commands.ReplaceFoodItem","participantId":1,"aGemId":15} Client 1
   * Received:	{"id":3,"commandType":"loadingtests.commands.ReplaceFoodItem","participantId":1,"aGemId":15}
   * [BROWSER] Add command to queue: 4 R {waitingTicket: 4, command: R}
   * [BROWSER] Clearing Command Buffer [Object]
   * [BROWSER] Sending command from buffer R
   * [BROWSER] Sending message to server: {"id":3,"commandType":"loadingtests.commands.ReplaceFoodItem","participantId":1,"aGemId":15} _
   * Sending command 23 loadingtests.commands.Animal_addFood to 1
   * Sending command 24 loadingtests.commands.Animal_removeFood to 1
   * Responding: {"commandId":3,"typeParameter":"java.lang.Void","statusResult":true,"simpleResult":false,"listResult":false,"validationResult":false,"successful":true}
   * StandardWebSocketSession[id=0b68f346-a308-ab29-2200-4a655c4e0eee, uri=ws://localhost:53738/umlp/api/command]
   * [BROWSER] Exec command 5 loadingtests.commands.Food_get from 1 Client 1
   * [BROWSER] {"id":5,"commandType":"loadingtests.commands.Food_get","participantId":1,"gemId":17} Client 1
   * Received:	{"id":5,"commandType":"loadingtests.commands.Food_get","participantId":1,"gemId":17}
   * [BROWSER] Add command to queue: 5 $ {waitingTicket: 5, command: $}
   * Responding: {"commandId":5,"typeParameter":"loadingtests.Food","statusResult":false,"simpleResult":true,"listResult":false,"validationResult":false,"successful":true,"value":{"gemId":17,"name":"a#"}}
   * StandardWebSocketSession[id=0b68f346-a308-ab29-2200-4a655c4e0eee, uri=ws://localhost:53738/umlp/api/command]
   * [BROWSER] Clearing Command Buffer [Object]
   * [BROWSER] Sending command from buffer $
   * [BROWSER] Sending message to server: {"id":5,"commandType":"loadingtests.commands.Food_get","participantId":1,"gemId":17} _
   * [BROWSER] Received result of command 4
   * [BROWSER] Received result of command 5
   * [BROWSER] Adding known food 17 Client 1
   * [BROWSER] 0x50003 idx must be at most owns.size
   * [BROWSER] 0xAC727 GemList::_internal_add index out of bounds, size: 0, idx: 1
   * [BROWSER] ERROR Error: IndexOutOfBoundsException
   * at Tr.logIndexError (http://localhost:53738/main.fe0dd5204b568c0b.js:1:47052)
   * at Tr._internal_add (http://localhost:53738/main.fe0dd5204b568c0b.js:1:46168)
   * at Tr._internal_add (http://localhost:53738/main.fe0dd5204b568c0b.js:1:49602)
   * at _n._internal_addFood (http://localhost:53738/main.fe0dd5204b568c0b.js:1:74648)
   * at _n._internal_addFood2 (http://localhost:53738/main.fe0dd5204b568c0b.js:1:75905)
   * at _n.addFood1 (http://localhost:53738/main.fe0dd5204b568c0b.js:1:74101)
   * at http://localhost:53738/main.fe0dd5204b568c0b.js:1:29396
   * at u.invoke (http://localhost:53738/polyfills.5692fea145d372a2.js:1:19140)
   * at Object.onInvoke (http://localhost:53738/main.fe0dd5204b568c0b.js:1:283999)
   * at u.invoke (http://localhost:53738/polyfills.5692fea145d372a2.js:1:19080)
   */
  @Test
  void testAtomicUpdate(Page page) throws Exception {
    navAndWait(page, "/test_update_list/TestAtomicUpdate"); //go to page we want to test

    waitForAngular(page);

    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // PlayWright cannot handle long values as args
    // Also give the page some time to catch up
    assertEqualsRetry(Boolean.TRUE, () -> page.evaluate(isLoadedAnimalFunction, (int) LoadingTestsManager.getUpdateListHelper().getTestAnimal().getGemId()),
        "Failed to load animal " + LoadingTestsManager.getUpdateListHelper().getTestAnimal().getGemId());


    waitForAngular(page);
    // Assert that one food is displayed
    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertEquals("a", page.locator(GemText.selector()).textContent().trim());

    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertThat(page.locator(GemText.selector())).hasText("a");

    // Click "replace first food item"
    page.locator(GemButton.selector()).click();


    // Let frontend communicate with backend and wait for response
    page.waitForLoadState(LoadState.NETWORKIDLE);


    Thread.sleep(200);
    // Assert, that a different food is displayed
    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertThat(page.locator(GemText.selector()).last()).hasText("a#");

    // Do the entire thing again, to test if the next command works / lock is freed
    // Click "replace first food item"
    page.locator(GemButton.selector()).click();
    // Let frontend communicate with backend and wait for response
    page.waitForLoadState(LoadState.NETWORKIDLE);
    Thread.sleep(200);
    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertThat(page.locator(GemText.selector()).last()).hasText("a##");


  }

  @Test
  void testOrder(Page page) throws Exception {
    navAndWait(page, "/test_update_list/TestAtomicUpdate"); //go to page we want to test
    waitForAngular(page);
    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // PlayWright cannot handle long values as args
    // Also give the page some time to catch up
    assertEqualsRetry(Boolean.TRUE, () -> page.evaluate(isLoadedAnimalFunction, (int) LoadingTestsManager.getUpdateListHelper().getTestAnimal().getGemId()),
        "Failed to load animal " + LoadingTestsManager.getUpdateListHelper().getTestAnimal().getGemId());

    waitForAngular(page);

    assertEquals(1, LoadingTestsManager.getUpdateListHelper().getTestAnimal().getFoodList().size());
    // Assert that one food is displayed
    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("a");

    Food b = LoadingTestsManager.foodBuilder().name("b").build().get();
    LoadingTestsManager.getUpdateListHelper().getTestAnimal().addFood(b);

    // ensure the frontend displays the correct data
    assertThat(page.locator(GemText.selector())).hasCount(2);
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("a");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("b");

    // just to make sure: the backend has the same order
    assertEquals(2, LoadingTestsManager.getUpdateListHelper().getTestAnimal().getFoodList().size());
    assertEquals("a", LoadingTestsManager.getUpdateListHelper().getTestAnimal().getFood(0).getName());
    assertEquals("b", LoadingTestsManager.getUpdateListHelper().getTestAnimal().getFood(1).getName());

    // and ensure something simple like a reload does not break our data (cough monticore#4975)
    page.reload();
    assertThat(page.locator(GemText.selector())).hasCount(2);
    //  WTF does a reload break the ordering? -> because GemListBuilder
    //  and we still don't have the time/priority to properly test the basics
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("a");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("b");

    // Finally, test that a remove operation works (as this was the initial reason for fixing the order bug)
    LoadingTestsManager.getUpdateListHelper().getTestAnimal().removeFood(b);

    assertEquals(1, LoadingTestsManager.getUpdateListHelper().getTestAnimal().getFoodList().size());
    assertThat(page.locator(GemText.selector())).hasCount(1);
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("a");
    // (if the order is incorrect, this line leads to a general sync bug/inconsistent state warning)
  }

  /** evaluates again until the result is the expected one, see {@link #waitUntil} */
  void assertEqualsRetry(Object expected, Supplier<Object> evaluate, String message) {
    waitUntil(() -> Objects.equals(expected, evaluate.get()), message);
  }

}
