# MontiGem Framework Specification

This document is the comprehensive reference for the MontiGem framework. It details the modeling languages, code generation rules, runtime environment, and development workflows.

---

## 1. Introduction

MontiGem is a model-driven, low-code platform for generating full-stack web applications. It bridges the gap between high-level modeling and modern web technologies by generating a Spring Boot backend and an Angular frontend from a common set of models.

### 1.1 Architecture Overview

A MontiGem application consists of three main layers:

1.  **Models:** The single source of truth.
    *   **Class Diagrams (CD4A):** Define the data structure, domain entities, and services.
    *   **GUI Models (GUI DSL):** Define the user interface structure, layout, and binding to domain data.
2.  **Generator:** Transforms models into executable code.
    *   **Backend Generator:** Produces Java code (Spring Boot, JPA/Hibernate) for the server.
    *   **Frontend Generator:** Produces TypeScript (Angular) and HTML/SCSS for the client. It uses **JSweet** to transpile hand-written Java frontend logic into TypeScript.
3.  **Runtime Environment (RTE):** A set of libraries providing common functionality.
    *   **Synchronization:** Handles real-time data sync between server and clients via WebSockets.
    *   **Command System:** Encapsulates all data modifications as commands.
    *   **Manager:** A singleton providing access to domain objects.

### 1.2 The TOP Mechanism

MontiGem employs the **Generation Gap Pattern** via the **TOP Mechanism** to allow safe customization of generated code.

*   **Generated Code:** For a model element `Foo`, the generator creates an abstract base class `FooTOP`.
*   **Hand-Written Code (HWC):** The developer creates a subclass `Foo extends FooTOP`.
*   **Injection:** The generator and RTE are configured to instantiate `Foo` instead of `FooTOP` whenever an instance is needed.

This applies to:
*   **Backend Entities:** `Customer extends CustomerTOP`
*   **Commands:** `RentItem extends RentItemTOP`
*   **Frontend Components:** `CarOverview extends CarOverviewTOP`

### 1.3 Packages & Imports

There is a strict distinction between referencing models in the GUI DSL and referencing generated classes in Java.

*   **In GUI DSL (`.gui`):** You reference the **Model**.
    *   Syntax: `import models.MyModel.MyEntity;` (assuming `.umlp` file is in `models/`).
*   **In Java HWC (`.java`):** You reference the **Generated Class**.
    *   Syntax: `import mymodel.MyEntity;` (Package is usually lowercase and without `models` prefix).

---

## 2. Data Modeling (CD4A)

The data model is defined using the Class Diagram language (CD4A) in `.umlp` files located in the `models` directory.

### 2.1 Classes and Attributes

Classes represent domain entities. They are mapped to Java Entities (Backend) and TypeScript classes (Frontend).

**Syntax:**
```java
class Customer {
  String name;
  String email;
  int age;
  LocalDateTime registrationDate;
}
```

**Inheritance:**
You can use `extends` to model inheritance.
```java
class Person { String name; }
class Customer extends Person { String customerId; }
```
*   `Customer` will have both `name` and `customerId` attributes.

**Supported Types:**

| CD4A / Java Type | Backend Java | Frontend TypeScript | Notes |
| :--- | :--- | :--- | :--- |
| `boolean`, `Boolean` | `boolean`, `Boolean` | `boolean` | |
| `int`, `Integer` | `int`, `Integer` | `number` | |
| `long`, `Long` | `long`, `Long` | `number` | JS numbers are doubles; precision loss possible for large longs. |
| `double`, `Double` | `double`, `Double` | `number` | |
| `String` | `String` | `string` | |
| `LocalDateTime` | `java.time.LocalDateTime` | `Temporal.PlainDateTime` | Temporal API from `@umlp/commonj2ts`. |
| `LocalDate` | `java.time.LocalDate` | `Temporal.PlainDate` | |
| `LocalTime` | `java.time.LocalTime` | `Temporal.PlainTime` | |
| `Instant` | `java.time.Instant` | `Temporal.Instant` | A point in time; SQL `TIMESTAMP WITH TIME ZONE`. |
| `Duration` | `java.time.Duration` | `Temporal.Duration` | An exact length of time; SQL `BIGINT` nanoseconds. |
| `List<T>` | `java.util.List<T>` | `Array<T>` | Only `List` is supported as a generic collection. |
| `Optional<T>` | `java.util.Optional<T>` | `T` | `undefined` represents empty. |

### 2.2 Enums

Enums define a fixed set of constants.

```java
enum RentableStatus {
  AVAILABLE,
  RENTED,
  MAINTENANCE;
}
```

*   **Usage:** `RentableStatus.AVAILABLE` maps to `RentableStatus.AVAILABLE` in both Java and TypeScript.

### 2.3 Associations

Associations describe relationships between domain objects. MontiGem supports bidirectional and unidirectional associations with various cardinalities.

**Syntax:**
```
association [cardinality] ClassA (roleA) <-> (roleB) ClassB [cardinality];
```

*   **Cardinality:** `[1]` (exactly one), `[0..1]` (optional), `[*]` (many).
*   **Direction:** `<->` (bidirectional), `->` (unidirectional).
*   **Role Name:** Optional name for the relationship (defaults to class name in camelCase).

**Example: 1-to-Many**
```java
association [1] User (owner) <-> (documents) Document [*];
```

**Generated Methods (User):**
*   `List<Document> getDocumentsList()`
*   `void addDocument(Document d)`: Adds `d` and sets `d.owner = this`.
*   `void removeDocument(Document d)`: Removes `d` and sets `d.owner = null`.
*   `boolean containsDocument(Document d)`

**Generated Methods (Document):**
*   `User getOwner()`
*   `void setOwner(User u)`: Sets owner and adds `this` to `u.documents`.

**Synchronization:**
Modifying an association (e.g., `customer.addDocument(rt)`) automatically triggers a command that updates the backend database and synchronizes all connected clients.

### 2.4 Services (Singletons)

Services are defined as classes with the `<<singleton>>` stereotype. They are used for business logic that doesn't belong to a specific entity, or for managing global state.

```java
<<singleton>>
class RentalService {
  void rentItem(RentableItem item);
  void returnItem(RentalTransaction transaction);
}
```

*   **Backend:** A singleton bean is generated. You must implement the logic in the corresponding HWC class (e.g., `RentalService.java`).
*   **Frontend:** A proxy class is generated. Calling `RentalService.rentItem(item)` in the frontend sends a command to the backend.
*   **Commands:** For each method, a `Command` class is generated (e.g., `RentalService_rentItem`).

## 2.5 Multiple Models & Merging

For large applications, you can split your data model into multiple `.umlp` files. The generator merges these files into a single domain model.

*   **Merging:** All `.umlp` files in the `models` directory are merged.
*   **Cross-References:** You can reference classes from other files using their fully qualified name.

For details on the merging process and conflict resolution, see [Class Diagram Merge](../unstructured/CDMerge.md).

---

## 3. GUI Modeling (GUI DSL)

The user interface is defined in `.gui` files.

### 3.1 Components and Pages

*   **`component Name(...)`**: Defines a reusable UI part.
*   **`page Name(...)`**: Defines a root component accessible via a URL route.

**Structure:**
```java
package mypackage;

import external.Component;

component MyComponent(String title, boolean isVisible) {
  /* content */
}
```

**Parameters:**
Parameters defined in the component signature become **Inputs** in Angular.
*   `title`: Available as `this.getTitle()` in Java HWC.
*   `isVisible`: Available as `this.isIsVisible()` (boolean convention).

### 3.2 Expressions & Helper Methods

The GUI DSL supports basic Java expressions, but has limitations with complex types like Enums or String concatenation.

*   **Avoid:** `@GemText(value = "Status: " + project.status)` (Often fails with Enums).
*   **Prefer:** Delegate to a Java helper method.
    *   **GUI:** `@GemText(value = getStatusLabel(project))`
    *   **Java:** `public String getStatusLabel(Project p) { return "Status: " + p.getStatus(); }`

### 3.3 Component Usage

Components are instantiated using the `@` syntax inside another component's body.

```java
@MyComponent(
  title = "Hello World",
  isVisible = true
);
```

*   **Named Arguments:** Arguments must be passed by name (`param = value`).
*   **Identifiers:** An instance can be given a name: `myInstance@MyComponent(...)`. This name is used as a CSS class and for referencing the component in the HWC.

### 3.4 Layout Components

MontiGem provides standard layout components in `mc.fenix.arrange`.

#### `GemColumn`
Arranges children vertically (Flexbox column).
```java
@GemColumn(
  rowGap = "10px",
  width = "50%",
  hAlign = "center", // left, center, right
  components = [ @GemText(...), @GemButton(...) ]
);
```

#### `GemRow`
Arranges children horizontally (Flexbox row).
```java
@GemRow(
  colGap = "20px",
  vAlign = "bottom", // top, center, bottom
  wrap = "wrap",     // wrap, nowrap
  components = [ ... ]
);
```

#### `GemGrid`
Arranges children in a 2D grid.
```java
@GemGrid(
  rows = [
    [ @GemText("Row 1, Col 1"), @GemText("Row 1, Col 2") ],
    [ @GemText("Row 2, Col 1"), @GemText("Row 2, Col 2") ]
  ]
);
```

#### `GemCard`
A container with a title and content area.
```java
@GemCard(
  title = "My Card",
  component = @GemText("Content")
);
```

### 3.5 Control Structures

#### `@iterate`
Renders the block for each element in a list.
```java
@iterate(Car c : cars) {
  @GemText(value = c.model);
}
```
*   Automatically updates when the list changes.
*   **Note:** If used inside a `components = [...]` list, it does **not** require a semicolon.

#### `@guard`
Renders the block only if the condition is true.
```java
@guard(c.price > 1000) {
  @GemText(value = "Expensive", color = "red");
} else {
  @GemText(value = "Cheap", color = "green");
}
```
*   `condition` is a Java boolean expression.
*   **Syntax Note:** If used inside a `components = [...]` list, the syntax is strict. Use semicolons inside the block or separate guards with commas.
    ```java
    components = [
      @guard(cond) { @Comp(...); }, // Semicolon inside
      @guard(!cond) { @Comp(...); }
    ]
    ```

### 3.6 Navigation

*   **URLs:** A page `gui.CarUpdate` is accessible at `/gui/CarUpdate`.
*   **Parameters:** If a page takes a domain object as a parameter (e.g., `page CarUpdate(Car car)`), the URL includes the object's ID: `/gui/CarUpdate/:carId`.
*   **Router:** Use `umlp.guiutil.Router.navigate(...)` in Java HWC to navigate.
    ```java
    Router.navigate("gui", "CarUpdate", String.valueOf(car.getGemId()));
    ```

## 3.7 Generating GUIs from Data Models (CD2GUI)

For rapid prototyping, MontiGem can generate basic CRUD (Create, Read, Update, Delete) views directly from your class diagram.

*   **Functionality:** Generates list views, detail views, and edit forms.
*   **Usage:** This is a powerful feature for quickly getting a working UI, which can then be customized.

For a detailed guide on how to use this feature, see [Class Diagram to GUI](../unstructured/CD2GUI.md).

---

## 4. Frontend Logic (Java/JSweet)

You write frontend logic in Java, which is then transpiled to TypeScript by JSweet. This enables type-safe, object-oriented development for your Angular components.

### 4.1 Binding Data

*   **Parameters:** Parameters of a GUI component are available via getters.
    *   `page CarOverview(List<Car> cars)` -> `this.getCars()`
*   **Domain Objects:** Attributes of domain objects are available via getters.
    *   `car.getModel()`
*   **State:** You can define internal state variables in your HWC class (or as component parameters) and access them via getters/setters. The UI updates automatically when these values change (Observer Pattern).

### 4.2 Event Handling

Events (like `leftClick`) are bound to methods in the Java HWC. These methods must return a `Consumer`.

**Model:**
```java
@GemButton(leftClick = rentCar(c));
```

**Java HWC:**
```java
public Consumer<Void> rentCar(Car car) {
    return event -> {
        // Logic executed in the browser
        car.setRentableStatus(RentableStatus.RENTED);
        // Or execute a command:
        CommandManager.executeCommand(new RentItem(car.getGemId()));
    };
}
```

**Example with Event Data:**
Some components emit data with their events.
```java
// In .gui
// GemTextInput emits the current value on enter
@GemTextInput(labelText = "Search", enter = handleSearch());

// In .java
public Consumer<String> handleSearch() {
    return searchTerm -> {
        System.out.println("User searched for: " + searchTerm);
        // Call a service to filter results
    };
}
```

**Best Practice:**
Avoid overloading event handler methods in the GUI DSL. Use unique names (e.g., `handleRent`, `handleReturn`) and delegate to a common implementation if needed.

### 4.3 Java-to-TypeScript Transpilation

MontiGem uses JSweet to transpile Java to TypeScript. For a detailed guide on supported types and methods, see the [JSweet User Guide](../unstructured/JSweet.md).

*   **Supported APIs:** A large subset of the Java standard library is supported:
    *   `java.util.List`, `ArrayList`, `LinkedList` -> JS `Array`
    *   `java.util.Map`, `HashMap` -> JS `Map`
    *   `java.util.Optional` -> Unwrapped value (nullable)
    *   `java.time.LocalDateTime`, `LocalDate`, `LocalTime`, `Instant`, `Duration` -> `Temporal.PlainDateTime`,
        `PlainDate`, `PlainTime`, `Instant`, `Duration`
    *   `java.lang.String`, `Math`, `System.out.println`
*   **Unsupported:**
    *   `java.util.stream` (Streams): **Do not use.** Use loops instead.
    *   `java.io`, `java.net`, `java.lang.reflect`: Not available in the browser.
    *   `synchronized`: Use `@Synchronized` annotation.

### 4.4 Native Interaction (`@Replace`)

To access browser APIs or write raw TypeScript, use the `@Replace` annotation.

```java
import jsweet.lang.Replace;

@Replace("window.alert(msg)")
public void nativeAlert(String msg) {
    // Dummy body for Java compiler
}
```

The body of the annotated method is discarded, and the string value is injected into the generated TypeScript.

**Common Use Cases:**
*   **Scrolling:** `document.querySelector(selector).scrollIntoView({behavior: 'smooth'})`
*   **Navigation:** `window.open(url, '_blank')`

---

## 5. Runtime Environment (RTE) & Synchronization

The RTE handles the communication between frontend and backend.

### 5.1 The Manager

The `Manager` (e.g., `CarrentalManager`) is a singleton available in both Backend (Java) and Frontend (TS). It holds the state of all loaded domain objects.

*   **Identity Map:** Ensures that for a given ID, only one object instance exists in memory.
*   **CRUD:** `getById(id)`, `getAll()`, `create()`, `delete()`.

### 5.2 Creating Objects (Builder Pattern)

To create new domain objects, use the generated Builder from the Manager class. This ensures the object is correctly instantiated and synchronized with the backend.

**Pattern:**
```java
MyEntity newObj = MyModelManager.myEntityBuilder()
    .attribute1("Value")
    .attribute2(123)
    .build(); // Creates and syncs the object
```

**Example:**
```java
Employee newEmp = SEOrgaManager.employeeBuilder()
    .firstName("Alice")
    .build();
```

### 5.3 The Command System

Synchronization is based on the **Command Pattern**.

1.  **Action:** User clicks a button -> `user.setName("Alice")`.
2.  **Command Creation:** The setter `setName` creates a `User_SetNameCommand(id, "Alice")`.
3.  **Execution (Local):** The command is executed locally (optimistic update).
4.  **Transmission:** The command is serialized to JSON and sent via WebSocket to the backend.
5.  **Execution (Backend):**
    *   The backend receives the JSON.
    *   Deserializes it to the Java Command class.
    *   Executes `doAction()`: Updates the backend object and persists it to the DB.
6.  **Broadcast:** The backend sends the command JSON to all *other* connected clients.
7.  **Execution (Remote):** Other clients receive the JSON, deserialize it, and execute it locally.

### 5.4 The Observer Pattern

The frontend uses an Observer pattern to automatically update the UI.

*   **Registration:** When a GUI component uses a domain object (e.g., `@GemText(value=user.name)`), the generated code automatically registers an observer on `user`.
*   **Notification:** When `user.setName(...)` is called (locally or via sync), the observer is triggered.
*   **Update:** The Angular Change Detection is triggered, and the UI updates.

**Manual Observation:**
You can manually register observers in your HWC:
```java
GUIUtils.registerObserverToDomainObject(user, () -> {
    // Callback when user changes
});
```

---

## 6. Styling & Theming

### 6.1 SCSS Structure

*   **Global Styles:** `frontend/config/src/styles.scss`.
*   **Component Styles:** `MyComponent.scss` next to `MyComponent.gui`.

### 6.2 Identifiers

Assign identifiers in GUI models to target elements in SCSS.

**GUI:**
```java
myButton@GemButton(...)
```

**SCSS:**
The identifier becomes a CSS class.
```scss
.myButton .gem-button {
  background-color: green;
  color: white;
}
```

### 6.3 Deep Styling

To style elements inside a child component (e.g., inside a `GemCard` or `GemButton`), you often need to pierce Angular's view encapsulation using `::ng-deep`.

```scss
:host ::ng-deep .gem-card-title {
  color: blue;
}
```

**Important:**
*   Always prefix `::ng-deep` with `:host` to limit the scope to your component. Otherwise, the style becomes global!
*   Many MontiGem components (like `GemButton`) wrap their content. To style the inner element, you must target the inner class (e.g., `.gem-button`).
*   **Host Styling:** To style the wrapper itself (e.g., to force width), target the identifier class directly:
    ```scss
    .myButton { width: 100%; display: block; }
    ```

## 6.4 GUI Design Patterns

For best practices on structuring your GUI and applying styles consistently, see [GUI Design Patterns](../designpatterns/GUIPatterns.md).

---

## 7. Backend Customization

### 7.1 Startup Logic

You can customize the server startup by overriding the `init()` method in your `...ServerApplication` class (HWC). This is the place to create dummy data.

```java
@PostConstruct
@Override
public void init() throws IOException {
  super.init();
  if (CarrentalManager.getCarList().isEmpty()) {
      // Create dummy data
      CarrentalManager.carBuilder().model("Tesla").build();
  }
}
```

### 7.2 Command Implementation

For every service method defined in the class diagram, a Command class is generated. You must implement the `doAction()` method in the HWC class.

```java
public class RentItem extends RentItemTOP {
  @Override
  public SimpleResult<RentalTransaction> doAction() {
    // Business logic here
    return new SimpleResult<>(getId(), result, RentalTransaction.class.getName());
  }
}
```

### 7.3 Custom REST Endpoints

To add custom REST endpoints, create a standard Spring Boot `@RestController` class anywhere in your backend source path.

```java
@RestController
public class MyEndpoint {
  @GetMapping("/api/hello")
  public String sayHello() {
    return "Hello";
  }
}
```

---

## 8. The TOP Mechanism

The TOP mechanism allows you to extend generated code without modifying it.

*   **Backend (Java):** Create a class `Foo extends FooTOP` in `backend/src/main/java`. The generator will use your class.
*   **Frontend (Java):** Create a class `Foo extends FooTOP` in `frontend/src/main/java`. Add `@umlp.jsweet.extension.annotation.Component`.
*   **Frontend (Templates):** Create `Foo.html` or `Foo.scss` in `frontend/src/main/gui` to completely overwrite the generated files.

---

## 9. Project Setup & Build

### 9.1 Folder Structure

*   `models/`: `.umlp` and `.gui` files.
*   `backend/`: Spring Boot application.
*   `frontend/`: Angular application.

### 9.2 Gradle Tasks

*   `gradle build`: Builds the entire project.
*   `gradle :backend:run`: Starts the backend server.
*   `gradle :frontend:runApp`: Starts the frontend dev server.
*   `gradle :frontend:sync_gui2java2ts`: Updates the frontend code after model changes (faster than full restart).

### 9.3 Custom Build Tasks

You can extend `build.gradle` to add custom tasks, e.g., to copy assets.

```groovy
// frontend/build.gradle
tasks.register("copySourceCodeToAssets", Copy) {
  from("src/main/gui") { include "**/*.gui" }
  into layout.buildDirectory.dir("app/src/assets/source-code")
}
tasks.named("sync_gui2java2ts") { dependsOn "copySourceCodeToAssets" }
```

---

## 10. How-To Guides

### 10.1 How-To: Build a Master-Detail View

This guide shows how to create a common application pattern: a list of items (master) where each item links to an editable detail page.

**Scenario:** We have a list of `Car` objects and want to edit each one on a separate page.

**1. Master Page (`CarOverview.gui`)**

This page displays a list of all cars. It receives `List<Car> cars` from the backend automatically.

```java
package gui;

import mc.fenix.basic.GemText;
import mc.fenix.basic.GemButton;
import mc.fenix.arrange.GemColumn;
import Carrental.Car;

page CarOverview(List<Car> cars) {
  @GemColumn(components = [
    @iterate(Car c : cars) {
      @GemRow(components = [
        @GemText(value = c.make + " " + c.model),
        @GemButton(label = "Edit", leftClick = editCar(c))
      ]);
    }
  ]);
}
```

**2. Detail Page (`CarUpdate.gui`)**

This page displays a form for a single `Car` object. The `Car` is identified by its ID in the URL.

```java
package gui;

import Carrental.Car;
import mc.fenix.input.GemTextInput;
import mc.fenix.arrange.GemColumn;
import mc.fenix.basic.GemButton;

page CarUpdate(Car car) {
  @GemColumn(components = [
    @GemTextInput(labelText = "Model:", entry = car.model),
    @GemTextInput(labelText = "Name:", entry = car.name),
    @GemButton(label = "Back", leftClick = goBack())
  ]);
}
```
*   The `entry = car.model` creates a two-way data binding. Changes in the input field are automatically written back to the `car` object.

**3. Navigation Logic (`CarOverview.java`)**

Implement the event handler to navigate to the detail page.

```java
package gui;

import umlp.guiutil.Router;
import java.util.function.Consumer;

public class CarOverview extends CarOverviewTOP {
  public Consumer<Void> editCar(Car car) {
    return event -> {
      Router.navigate("gui", "CarUpdate", String.valueOf(car.getGemId()));
    };
  }
}
```
*   `Router.navigate` constructs the URL `/gui/CarUpdate/123` (where 123 is the car's ID).

**4. Back Navigation (`CarUpdate.java`)**

Implement the event handler to navigate back.

```java
package gui;

import umlp.guiutil.Location;
import java.util.function.Consumer;

public class CarUpdate extends CarUpdateTOP {
  public Consumer<Void> goBack() {
    return event -> {
      Location.back();
    };
  }
}
```
*   `Location.back()` uses the browser's history to go back to the previous page.

---

## 11. Further Reading & External Resources

### 11.1 Core Technologies

*   **JSweet:** [Language Specifications](https://github.com/cincheo/jsweet/blob/master/doc/jsweet-language-specifications.md)
*   **Angular:** [Official Documentation](https://angular.io/docs)
*   **Spring Boot:** [Official Documentation](https://spring.io/projects/spring-boot)
*   **CSS Flexbox:** [MDN Guide](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flexible_Box_Layout/Basic_Concepts_of_Flexbox)
*   **CSS Grid:** [MDN Guide](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout/Basic_Concepts_of_Grid_Layout)

### 11.2 Advanced MontiGem Topics

*   [Class Diagram Merge](../unstructured/CDMerge.md)
*   [Class Diagram to GUI](../unstructured/CD2GUI.md)
*   [JSweet Integration Details](../unstructured/JSweet.md)
*   [MontiGem Module System](../unstructured/ModuleSystem.md)
*   [Instantiating Data with Object Diagrams](../unstructured/OD2Data.md)
*   [Testing in MontiGem](../unstructured/Testing.md)
*   [GUI Design Patterns](../designpatterns/GUIPatterns.md)

---

## 12. Deployment

This chapter describes how to deploy a MontiGem application using Docker.

### 12.1 Requirements

**Hardware (Recommended):**
*   2 CPUs
*   4 GB RAM
*   5 GB HDD

**Software:**
*   Linux OS with [Docker Compose](https://docs.docker.com/compose/install/).
*   Open Ports 80 and 443 (TCP).
*   A Domain (for SSL via Let's Encrypt).

### 12.2 Building Docker Images

#### Backend
We recommend using [Jib](https://github.com/GoogleContainerTools/jib/) to build the backend image without a local Docker daemon.

```sh
cd backend
gradle jib \
      -Djib.to.image=<imageNameOfBackend> \
      -Djib.to.auth.username=<RegistryUser>  \
      -Djib.to.auth.password=<RegistryPassword>
```

#### Frontend
The frontend uses [Caddy](https://caddyserver.com/) as a web server and reverse proxy.

1.  Ensure `Dockerfile` and `Caddyfile` are present (generated by `setupFrontend`).
2.  Build the image:
    ```sh
    cd frontend
    gradle assemble
    docker build . -t <imageNameOfFrontend>
    ```
3.  Push the image to your registry.

### 12.3 Deploying on Server

1.  Copy the `docker-compose.yml` (from `docs/deploy/`) to your server.
2.  Adjust the configuration (image names, ports).
3.  Login to your registry:
    ```sh
    docker login -u user -p <token> registry.example.com
    ```
4.  Start the application:
    ```sh
    docker compose up --detach
    ```

---

## 13. Testing

MontiGem supports various testing strategies to ensure the quality of your application.

### 13.1 E2E Testing with Playwright

End-to-End (E2E) tests verify the application flow from the user's perspective. MontiGem integrates with [Playwright](https://playwright.dev/) for robust browser automation.

#### Setup
Enable Playwright in your `frontend/build.gradle`:
```groovy
generateTestInfrastructure {
  usePlaywright.set(true)
}
```

#### Writing Tests
Tests are written in Java and located in `frontend/src/test/java`. They extend `AbstractPlaywrightE2ETest`.

```java
public class MyE2ETest extends AbstractPlaywrightE2ETest {
  @Test
  public void testLogin() {
    page.navigate("http://localhost:4200");
    page.fill("#username", "admin");
    page.click("#loginButton");
    assertThat(page.locator("#welcomeMessage")).isVisible();
  }
}
```

### 13.2 Simulated GUI Tests

For faster feedback on GUI logic without a full browser, MontiGem generates "Simulated GUI" classes. These allow you to test component interaction (clicks, input) in a pure Java environment.

For more details, see [Testing in MontiGem](../unstructured/Testing.md).
