package loadingtests.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import de.se_rwth.commons.logging.Log;
import loadingtests.Animal;
import loadingtests.LoadingTestsManager;
import loadingtests.Person;
import loadingtests.commands.Animal_create;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static loadingtests.e2e.E2EUtil.createDummyData;
import static loadingtests.e2e.E2EUtil.isLoadedAnimalFunction;
import static loadingtests.e2e.E2EUtil.isLoadedFoodFunction;
import static loadingtests.e2e.E2EUtil.isLoadedPersonFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

public class TestLoading extends AbstractLoadingTestsE2ETest {

  private List<Long> personGemIds;
  private List<Long> animalGemIds;
  private List<Long> foodGemIds;

  @BeforeEach
  void init(Page page) {
    this.personGemIds= new ArrayList<>();
    this.animalGemIds= new ArrayList<>();
    this.foodGemIds= new ArrayList<>();
    createDummyData(this.personGemIds, this.animalGemIds, this.foodGemIds);
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }


  // Test fails currently as our infrastructure does not lazy load
  @Disabled("Investigate: Works locally, fails in pipeline")
  @Test
  void testLoadingAllPersonsAndNothingElse(Page page) {
    navAndWait(page, "/test_load_all/LoadOnlyPersons"); //go to page we want to test
    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    page.evaluate("console.warn(window.managerLoadedFoodFunction)");
    page.evaluate("console.warn(window)");


    // PlayWright cannot handle long values as args
    personGemIds.forEach(id -> waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedPersonFunction, id.intValue())),
        "person " + id + " in the browser"));

    animalGemIds.forEach(id ->
        assertEquals(Boolean.FALSE,page.evaluate(isLoadedAnimalFunction,id.intValue()))
    );

    foodGemIds.forEach(id ->
        assertEquals(Boolean.FALSE,page.evaluate(isLoadedFoodFunction,id.intValue()))
    );
  }

  @Test
  void testLoadOnePersonAndAllButOneAnimals(Page page) {
    Long gemIdOfPersonWithAnimals = this.personGemIds.get(4);

    navAndWait(page, "/test_load_all/LoadOnePersonAndAllAnimals/"+gemIdOfPersonWithAnimals); //go to page we want to test
    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Assert person was loaded (over the websocket, which NETWORKIDLE does not wait for)
    waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedPersonFunction, gemIdOfPersonWithAnimals.intValue())),
        "person " + gemIdOfPersonWithAnimals + " in the browser");

    // Assert no other person was loaded
    // Fails because pet <-> owner is bidirectional
    personGemIds.forEach(id -> {
      if(!id.equals(gemIdOfPersonWithAnimals)) {
        assertEquals(Boolean.FALSE,page.evaluate(isLoadedPersonFunction,id.intValue()));
      }
    });

    List<Long> attachedGemIds = LoadingTestsManager.getPersonSure(gemIdOfPersonWithAnimals).getPetList().stream().map(Animal::getGemId).toList();
    animalGemIds.forEach(id -> {
      if (attachedGemIds.contains(id)) {
        waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedAnimalFunction, id.intValue())),
            "Animal gemId=" + id + " was not loaded, " + attachedGemIds);
      }
      else {
        assertEquals(Boolean.FALSE, page.evaluate(isLoadedAnimalFunction, id.intValue()), "Animal gemId=" + id + " was loaded despite not being attached to " + attachedGemIds);
      }
    });

  }


  @Test
  void testUpdateAllAnimalsPageAfterNewAnimalWasCreated(Page page) {
    Long gemIdOfPersonWithNoAnimals = this.personGemIds.get(1);
    navAndWait(page, "/test_load_all/DisplayAnimals");
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Assert animals were loaded
    animalGemIds.forEach(id -> waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedAnimalFunction, id.intValue())),
        "animal " + id + " in the browser"));

    // Assert that animals are displayed
    assertThat(page.locator(GemText.selector())).hasCount(animalGemIds.size());
    for (int i=0;i<animalGemIds.size();i++) {
      assertThat(page.locator(GemText.selector()).nth(i)).hasText(LoadingTestsManager.getAnimalSure(animalGemIds.get(i)).getName());
    }

    Log.info("Building new animal now.", this.getClass().getName());
    Long gemId = LoadingTestsManager.animalBuilder().name("animalName").legs(0).food(Set.of()).build().get().getGemId();

    // Assert that new animal was synchronized: the backend pushes it over the websocket, which NETWORKIDLE does not
    // wait for
    waitUntil(() -> Boolean.TRUE.equals(page.evaluate(isLoadedAnimalFunction, gemId.intValue())),
        "animal " + gemId + " in the browser");

    // and is also displayed
    assertThat(page.locator(GemText.selector())).hasCount(animalGemIds.size() + 1);
    assertThat(page.locator(GemText.selector()).nth(animalGemIds.size())).hasText("animalName");
  }

  @Test // for https://git.rwth-aachen.de/monticore/monticore/-/issues/4805
  void testRemoveWithReload(Page page) {
    Person person = LoadingTestsManager.personBuilder().age(0).name("P").build().get();
    person.addPet(LoadingTestsManager.animalBuilder().name("aA").legs(4).build().get());
    person.addPet(LoadingTestsManager.animalBuilder().name("aB").legs(4).build().get());
    person.addPet(LoadingTestsManager.animalBuilder().name("aC").legs(4).build().get());
    person.setFavouritePet(person.getPet(0));
    navAndWait(page, "/test_load_all/AnimalsRemove/"+person.getGemId());
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // The list is, apparently, inverted
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aA");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aB");
    assertThat(page.locator(GemText.selector()).nth(2)).hasText("aC");
    page.reload();
    // Is the page the same after a reload?
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aA");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aB");
    assertThat(page.locator(GemText.selector()).nth(2)).hasText("aC");
    // Ensure the first/favourite pet is the one we expect
    Assertions.assertEquals("aA", person.getFavouritePet().getName());
    Assertions.assertEquals("aA", person.getPet(0).getName());
    Assertions.assertEquals("aB", person.getPet(1).getName());
    // Remove the first/favourite pet, which should be aA
    page.locator(GemButton.selector()).click();
    // And assert, that we have removed the correct element
    Assertions.assertEquals("aB", person.getPet(0).getName());
    // TODO: No guarantee for the tests after this point
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aB");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aC");
    // Prepare next removal
    person.setFavouritePet(person.getPet(0));
    Assertions.assertEquals("aB", person.getFavouritePet().getName()); // ensure it really is correct
    // and add new element
    person.addPet(LoadingTestsManager.animalBuilder().name("aD").legs(4).build().get());

    // Assert, that the new element is in the correct order
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aB");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aC");
    assertThat(page.locator(GemText.selector()).nth(2)).hasText("aD"); // check that the insert appears at the correct position: https://git.rwth-aachen.de/monticore/monticore/-/issues/4976
    page.locator(GemButton.selector()).click(); // remove aB
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aC");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aD");
    // final check, because page-reloads apparently are not supported by MontiGem
    page.reload();
    assertThat(page.locator(GemText.selector()).nth(0)).hasText("aC");
    assertThat(page.locator(GemText.selector()).nth(1)).hasText("aD");

  }

  /**
   * Given a page that gets a Person as input and displays all its pets.
   * This is done by using another page that gets a List<Animal>, which loads all objects of a type
   * animal if it directly called.
   *
   * We expect that this Person page will not be updated when a new animal gets created that is not
   * a pet of the currently displayed person!
   * @param page
   */
  @Test
  void testLoadAllComponentsInComposedPage(Page page) {
    String animalName="New Pet";
    Long gemIdOfPersonWithNoAnimals = this.personGemIds.get(1);

    navAndWait(page, "/test_load_all/LoadOnePersonAndDisplayPets/"+gemIdOfPersonWithNoAnimals); //go to page we want to test
    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Assert person was loaded
    assertEquals(Boolean.TRUE,page.evaluate(isLoadedPersonFunction,
        gemIdOfPersonWithNoAnimals.intValue()));

    // Assert there is no animal on the page
    assertEquals(0, page.getByText(animalName).all().size());

    // Assert that no animal was loaded since we do not need it on the page
    Log.info("Building new animal now.", this.getClass().getName());

    Long gemId = LoadingTestsManager.animalBuilder().name(animalName).legs(0).food(Set.of()).build().get().getGemId();

    Log.info("Before network wait:"+LocalDateTime.now().toString(), this.getClass().getName());
    page.waitForLoadState(LoadState.NETWORKIDLE);
    Log.info("After network wait:"+LocalDateTime.now().toString(), this.getClass().getName());

    // Assert that there is still no animal on the page with the given animalName
    assertEquals(0, page.getByText(animalName).all().size());
  }

  @Disabled
  @Test
  void testLoadOnlyAnimals(Page page) {
    navAndWait(page, "/test_load_all/LoadOnlyAnimals"); //go to page we want to test
    // We need this to ensure that all Client Request were answered from the Backend
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Assert no person was loaded
    personGemIds.forEach(id -> {
      assertEquals(Boolean.FALSE,page.evaluate(isLoadedPersonFunction,id.intValue()));
    });

    animalGemIds.forEach(id -> {
      assertEquals(Boolean.TRUE,page.evaluate(isLoadedPersonFunction,id.intValue()));
    });

    foodGemIds.forEach(id -> {
      assertEquals(Boolean.FALSE,page.evaluate(isLoadedPersonFunction,id.intValue()));
    });
  }

  @Test
  void testInit(Page page) {
    // go to page to load persons
    navAndWait(page, "/test_load_all/InitPersons");
    page.waitForLoadState(LoadState.NETWORKIDLE);

    navAndWait(page, "/test_load_all/InitPersonsAndTextValue");
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // check text value initialized
    assertEquals(1, page.getByText("foo").all().size());
    assertEquals(1, page.getByText("bar").all().size());
  }
}
