# Testing MontiGem Applications

[TOC]

## Motivation
Since MontiGem transpiles all handwritten Java code into TypeScript via JSweet, MontiGem applications can be end-to-end tested purely in Java.
Thus, the application developer does not need to learn two general purpose programming languages to implement and test an application.
Additionally, these end-to-end tests are executed purely sequentially, avoiding complicated multi-threading logic.
The concept is explained in detail in [Chapter 8](https://mbse.se-rwth.de/book2/index.php?c=chapter8-4) of `Agile Modellierung mit UML`(German). 

## Base Concept

The base concept for testing in MontiGem revolves around the `TestSetup` interface.
This interface defines the basic structure for setting up and cleaning up JUnit test environments.
For each domain class diagram, 3 different implementations of `TestSetup` are generated.
Here for the `Flensburg` example:

1. `FlensburgMockTestSetup`, which keeps all domain objects in RAM
2. `FlensburgSQLDBTestSetup`, which keeps all domain objects in a SQL database (typically H2 during testing, Postgres during production)
3. `FlensburgNeo4JDBTestSetup`, which keeps all domain objects in a graph database (typically Neo4J)

Some tests want to execute actions in the role of the backend as well as in the role of a client.
To enable this in conjunction with the static delegate pattern, we bundle all static state in the generated implementations of `TestContext`.
The `TestSetup` sets up one backend `TestContext`, available via `getBackend()`, and a configurable amount of client `TestContext` instances, available via `getClients(i)`.
To switch the current role, the `TestContext` has the `activate()` function, which switches all static delegates (such as the `FlensburgManager`). 

## Testing Backend Features

Testing base features typically involves verifying the functionality of individual components or interactions within a single context (either backend or client).  These tests often utilize mock objects to isolate the unit under test. Check [CityInfoTest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/flensburg-test/backend/src/test/java/flensburg/CityInfoTest.java) for an example.

**Example (`CityInfoTest.java` excerpt):**

```java
public class CityInfoTest extends FlensburgMockTestSetup {

  public CityInfoTest() {
    super(ClientSetup.of(0)); // No client needed
  }

  @Test
  public void testCityInfo(){
    // Backend is activated by default
    Car c1 = FlensburgManager.carBuilder().wheelLeft(false).namesOfAllParts(new ArrayList<>()).build().get();
    // ...
    
    CityInfo cityInfo = new CityInfo();

    assertNotNull(cityInfo);
    assertEquals(2, cityInfo.calcNumberOfCities());
    // ... other assertions ...
  }
}
```

This example tests calculations performed by the `CityInfo` class without involving any client-server or database synchronization.

## E2E Testing using Simulated GUI

MontiGem provides a way to perform end-to-end (E2E) testing using simulated GUIs.
These tests simulate user interactions with generated GUI components and verify the resulting state changes in the underlying objects and across contexts.

**Key Concepts:**

*   **GUI Model:**  The tests are based on GUI models that define the structure and behavior of the UI (see comment in `EditierePersonGeneratedGUITest.java`).
*   **Generated GUI Classes:**  MontiGem generates Java classes representing the GUI components (e.g., `PersonEdit`).
*   **Simulating User Actions:**  The tests interact with the generated GUI classes to simulate user actions like setting text in input fields (`setGemTextInput...Entry`) or clicking buttons (`gemButton...LeftClick`).
*   **Observing State Changes:**  The tests verify that the simulated actions lead to the expected state changes in the underlying objects (both in the client and server contexts).

**Example ([EditierePersonGeneratedGUITest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/flensburg-test/frontend/src/test/java/flensburg/gui/editierepersoncars/PersonEditGUITest.java) excerpt):**

```java
public class PersonEditGUITest extends AbstractGUITest {
  // ...

  @Test
  public void editPersonGUITest() {
    // ... object creation ...
    Person p1 = FlensburgManager.personBuilder()
        .age(24)
        .nickname("theo")
        .mainResidence(a)
        .build().get();
    // ...
    
    // Create simulated GUI page 
    PersonEdit e = new PersonEdit();
    e.setP(p1); // Link GUI to the Person object
    e.init(); // IMPORTANT: init() must be called manually in tests to trigger startup logic!

    // Check initial GUI values
    assertEquals("theo", e.getGemTextInput4Entry());
    assertEquals("24", e.getGemTextInput5Entry());

    // Simulate changing the age in the GUI
    e.setGemTextInput5Entry("33");
    assertEquals(33, p1.getAge().intValue()); // Verify object change

    // Simulate clicking the decrease button
    e.gemButton6LeftClick(null);
    e.gemButton6LeftClick(null);
    assertEquals("31", e.getGemTextInput5Entry()); // Verify GUI change
    assertEquals(31, p1.getAge().intValue()); // Verify object change
  }
  //...

  @Test
  public void observePersonGUITest() {
    // ... setup multiple clients and GUI instances ...
    PersonEdit e1 = new PersonEdit();
    PersonEdit e2 = new PersonEdit();

    // Simulate user interaction
    getClients(0).activate();
    // Simulate changing the age in GUI of client 1
    e1.setGemTextInput5Entry("33");
    assertEquals("33", e2.getGemTextInput5Entry()); // Verify change in GUI of client 2

    getClients(1).activate();
    // Simulate clicking button in GUI of client 2
    e2.gemButton6LeftClick(null);
    e2.gemButton6LeftClick(null);
    assertEquals("31", e1.getGemTextInput5Entry()); //verify change in GUI of client 1.
  }
}
```

This example demonstrates:

1.  Creating a `PersonEdit` GUI instance.
2.  Linking it to a `Person` object.
3.  Simulating user input by setting text in an input field.
4.  Verifying that the change is reflected in the underlying `Person` object.
5.  Simulating a button click and observing the effect on both the GUI and the object.
6.  Testing GUI updates across multiple clients.

# Testing the MontiGem platform
The developers of the MontiGem platform need to ensure that the RTE and generated code work correctly.
In addition to unit tests, integration tests are used, since they can access both the RTE and an instance of the generated code.
For most tests, the same Java only testing framework based on the `TestSetup` as described in the application test section is used,
but more of the internal infrastructure is used and observed.
Some typical patterns are described below.

## Testing Synchronization between Server and Clients

A crucial aspect of internal MontiGem testing is verifying synchronization between the server (backend) and one or more clients.
This involves creating objects in one context (e.g., a client) and verifying that the changes are reflected in another context (e.g., the server or another client).
It is crucial to activate the context, where changes should happen, before object modifications.

**Key Concepts:**

*   **Context Activation:**  Use `getBackend().activate()` or `getClients(i).activate()` to switch the active context before making changes.  The active context is where operations like object creation and modification will occur.
*   **Object Creation and Modification:**  Create and modify objects within the active context.
*   **Verification:** Switch to a different context (server or another client) and verify that the changes are reflected correctly.
*   **`assertNotSame`:** Use this to confirm that you're dealing with distinct object instances in different contexts (even though they represent the same data).
*   **`assertEquals`:** Use this to verify that the data within the objects is consistent across contexts.

**Example ([PersonSyncTest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/flensburg-test/backend/src/test/java/flensburg/sync/PersonSyncTest.java) excerpt):**

```java
public class PersonSyncTest extends FlensburgMockTestSetup {
  public PersonSyncTest(){
    super(2); // set number of clients
  }

  @Test
  public void testMainResidenceIsSyncedToServer(){
    // ...
    getClients(0).activate(); // Activate client 0
    Person p1001 = /* ... create person ... */;

    getBackend().activate(); // Activate the backend
    Person p1001Server = FlensburgManager.getPersonSure(startGemId); // Retrieve person from server

    assertNotSame(p1001, p1001Server); // Verify they are different instances
    assertEquals(startGemId, p1001Server.getGemId()); // Verify data consistency
    // ... more assertions ...
  }
    @Test
  public void testMainResidenceChangeIsSyncedToClient(){
      //...
    getClients(0).activate(); // Activate client 0
    Person p1001 = /* ... create person ... */;

    getBackend().activate();  // Activate server
    Person p1001Server = FlensburgManager.getPersonSure(startGemId);
    Address a2 = FlensburgManager.addressBuilder().city("Aachen").build().get();
    p1001Server.setMainResidence(a2); // Modify the object in the SERVER context

    getClients(0).activate(); // Activate client context
    assertEquals("Aachen", p1001.getMainResidence().getCity()); // Check client

    //...
  }
}
```

This example demonstrates:

1.  Creating a `Person` object in the client context.
2.  Activating the backend context.
3.  Retrieving the corresponding `Person` object from the server.
4.  Verifying that the objects are different instances (`assertNotSame`).
5.  Asserting that the data is synchronized (`assertEquals`).

## Testing Synchronization between Server and (Multiple) Databases

MontiGem also supports testing synchronization with databases.
When `hasDatabase()` returns `true`, the tests interact with a real database.
The testing framework provides helper classes (e.g., `FlensburgSQLDBTestSetup`) to manage database connections and setup.

**Key Concepts:**

*  **`hasDatabase()`:**  Checks if a database is used in the test.
*  **DAO (Data Access Object):**  Use DAOs (e.g., `FlensburgDAOFactory.getPersonDAO()`) to interact directly with the database and verify data persistence.
*  **`TESTING_deepEquals`:** Provides a deep comparison method to check if objects retrieved from different context are identical.

**Example ([HumanSyncDbTest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/flensburg-test/backend/src/test/java/flensburg/sync/HumanSyncDbTest.java)):**

```java
public class HumanSyncDbTest extends FlensburgSQLDBTestSetup {
  // ...
  @Test
  public void testFieldChangeIsSavedInDb(){
    getClients(0).activate();
    var h = FlensburgManager.humanBuilder().age(20).build().get();

    HumanDAO dao = new HumanDAO(); // Get the DAO
    var hDb1 = dao.selectHuman(h.getGemId()).get(); // Retrieve from DB
    assertEquals(h.getAge(), hDb1.getAge());

    h.setAge(21); // Modify in the CLIENT context
    var hDb2 = dao.selectHuman(h.getGemId()).get(); // Retrieve again
    assertEquals(h.getAge(), hDb2.getAge()); // Verify change in DB
  }
}
```

We can also test multiple persistence implementations at the same time by defining all test cases(=methods) in an interface and creating one subclass for the mock, one for SQL, and one for Neo4J.
To enable variation points depending on the availability of the database, the `TestSetup` provides the method `hasDatabase`.

**Example ([PersonSyncTest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/flensburg-test/backend/src/test/java/flensburg/sync/PersonSyncTest.java) excerpt using `TESTING_deepEquals`):**
```java
public class PersonSyncTest extends TestSetup<FlensburgTestContext> {
  class PersonSyncMockTest extends FlensburgMockTestSetup implements PersonSyncTest {
    public PersonSyncMockTest() {
      super(2);
    }
  }

  class PersonSyncDBTest extends FlensburgSQLDBTestSetup implements PersonSyncTest {
    public PersonSyncDBTest() {
      super(2);
    }
  }

  @Test
  default void testAddOwnsIsSyncedToServer(){
      PersonBuilder pb1001 = /*...*/;
      // ...
      if(hasDatabase()){
        assertTrue(p1001Server.TESTING_deepEquals(FlensburgDAOFactory.getPersonDAO().selectPerson(p1001Server.getGemId()).get()));
        assertTrue(c_i_server.TESTING_deepEquals(FlensburgDAOFactory.getCarDAO().selectCar(c_i_server.getGemId()).get()));
      }
  }
}
```
This example demonstrates persisting a change to a `Human` object and verifies it in the SQL and Neo4J database implementations.

## E2E Testing with Playwright

MontiGem also supports end-to-end (E2E) testing using [Playwright](https://playwright.dev/), a library for automating web browsers.
This allows for testing the entire application flow, including interactions between the frontend and backend, in a real browser environment.
These tests execute much slower than testing using the simulated GUI, therefore it should only be used to test the MontiGem platform itself, not generated applications. 
Additionally, the execution of these tests involves multiple threads, making them much harder to write and understand due to the need to synchronize the threads.

**Key Concepts:**
*   **`Abstract${Domain}E2ETest`:** A generated class that provides common functionality for Playwright-based E2E tests. It handles starting and stopping the frontend server, cleaning up the backend database between tests, and provides utility methods for interacting with the UI.
*   **Frontend and Backend Server Ports:**  The tests automatically allocate free ports for the frontend and backend servers, ensuring that tests can run in parallel without port conflicts.  These are accessed via `getFrontendServerPort()` and `getBackendServerPort()`.
*   **`FrontendServer4Test`:**  This class manages the frontend server, serving the compiled frontend application during the tests.  It's automatically started and stopped by `AbstractPlaywrightE2ETest`.
*  **`waitForBackend()`:** Use this utility method after interacting with the UI and triggering backend operations.  It waits until no open page of the test has a command in progress: every command sent to the backend has been answered, and every command pushed by the backend has been processed. The frontend publishes this count as `window.umlp_pending_commands` (`@umlp/commonj2ts` 9.0.4 or later); for frontends without it, the method falls back to a fixed delay of 200 ms. `waitForBackend(page)` waits for a single page. HTTP requests of the frontend, such as file uploads (`GemFileInput`), are not counted: after them, wait for their result with a waiting assertion. Assertions on asynchronously updated content should still use Playwright's waiting assertions (`assertThat(locator).isVisible()`, `hasText(...)`) instead of immediate checks such as `locator.isVisible()`.
*   **`ComponentKind`:**  The `ComponentKind` enum (used with `locator()`) helps in locating specific generated GUI elements within the DOM.  Examples include `GemButton` and `GemTextInput`.
* **Multi-Page Testing:** Playwright allows the creation of multiple browser contexts and pages, facilitating testing of scenarios involving multiple simultaneous users or views, such as synchronization between clients.
* **Navigation and Waiting:** Use `navAndWait()` to navigate the page to the required URL, followed by waiting for frontend to initialize with `waitRdy()`.

**Example ([ExampleE2ETest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/vis-lib/frontend/src/test/java/domain/e2e/ExampleE2ETest.java) excerpt):**

```java
public class ExampleE2ETest extends AbstractDomainE2ETest {

  @BeforeEach
  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/person/Overview"); //go to page we want to test
  }

  @Test
  void testOverview(Page page) {
    waitRdy(page);
    assertTrue(page.getByText("Alice").first().isVisible());
  }
}
```

This example shows:

1.  Extending `AbstractDomainE2ETest` to set up the testing environment.
2.  Using `@BeforeEach` to initialize the Playwright `Page` object and navigate to the page under test.
3. Using `navAndWait()` navigates to the correct page and waits for it to fully load.
4.  A simple test case that verifies the visibility of an element on the page.

**Example ([MultiPageE2ETest.java](https://git.rwth-aachen.de/monticore/umlp/-/blob/dev/examples/vis-lib/frontend/src/test/java/domain/e2e/MultiPageE2ETest.java) excerpt):**

```java
public class MultiPageE2ETest extends AbstractDomainE2ETest {

  Page page1, page2;

  @BeforeEach
  void setup(Browser browser) {
    //create the number of pages we want to test with
    page1 = browser.newPage();
    page2 = browser.newPage();
    init(page1);
    init(page2);
  }

  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER1] " + cm.text()));
    navAndWait(page, "/person/Overview"); //go to page we want to test
  }

  @Test
  void testSync() {
    var createPerson = page1.locator(".createPerson");
    locator(createPerson, ".name", GemTextInput).fill("Abby");
    // ... fill other fields ...
    locator(createPerson, ".createButton", GemButton).click();

    waitForBackend(); //give commands time to reach the backend

    page2.reload();
    waitRdy(page2);

    assertThat(page2.getByText("Abby").first()).isVisible();
  }
}
```
This example demonstrates multi-page testing:

1. Two `Page` objects (`page1`, `page2`) are created, representing two different browser tabs/windows.
2. The `init` method sets up each page.
3. User interactions are simulated on `page1` to create a new Person.
4.  `waitForBackend()` is called to ensure the changes are processed.
5. `page2` is reloaded, and after waiting for readiness, the test verifies that the changes made on `page1` are reflected on `page2`. This checks client-to-client synchronization through the backend.

**Debugging:**
To debug Playwright tests, set the environment variable `PWDEBUG=1` in the run configuration.
This will open the Playwright Inspector, allowing you to step through the test execution and inspect the browser state.

**Important Note:**  In the frontend application's `app.component.ts` constructor, ensure the following line is present: `(<any>window).websocket_connected = true;`.
This is used to signal to the test framework that the frontend has successfully connected to the backend via websockets.
