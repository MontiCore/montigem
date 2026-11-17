# 4. Frontend Logic (Java/JSweet)

You write frontend logic in Java, which is then transpiled to TypeScript by JSweet. This enables type-safe, object-oriented development for your Angular components.

## 4.1 Binding Data

*   **Parameters:** Parameters of a GUI component are available via getters.
    *   `page CarOverview(List<Car> cars)` -> `this.getCars()`
*   **Domain Objects:** Attributes of domain objects are available via getters.
    *   `car.getModel()`
*   **State:** You can define internal state variables in your HWC class (or as component parameters) and access them via getters/setters. The UI updates automatically when these values change (Observer Pattern).

## 4.2 Event Handling

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

## 4.3 Java-to-TypeScript Transpilation

MontiGem uses JSweet to transpile Java to TypeScript. For a detailed guide on supported types and methods, see the [JSweet User Guide](../unstructured/JSweet.md).

*   **Supported APIs:** A large subset of the Java standard library is supported:
    *   `java.util.List`, `ArrayList`, `LinkedList` -> JS `Array`
    *   `java.util.Map`, `HashMap` -> JS `Map`
    *   `java.util.Optional` -> Unwrapped value (nullable)
    *   `java.time.LocalDateTime`, `LocalDate`, `LocalTime` -> `Temporal.PlainDateTime`, `PlainDate`, `PlainTime`
    *   `java.lang.String`, `Math`, `System.out.println`
*   **Unsupported:**
    *   `java.util.stream` (Streams): **Do not use.** Use loops instead.
    *   `java.io`, `java.net`, `java.lang.reflect`: Not available in the browser.
    *   `synchronized`: Use `@Synchronized` annotation.

## 4.4 Native Interaction (`@Replace`)

To access browser APIs or write raw TypeScript, use the `@Replace` annotation.

```java
import jsweet.lang.Replace;

@Replace("window.alert(msg)")
public void nativeAlert(String msg) {
    // Dummy body for Java compiler
}
```

The body of the annotated method is discarded, and the string value is injected into the generated TypeScript.

---
[< Previous: GUI Modeling](03_GUIModeling.md) | [Back to Index](../MontiGem_Documentation.md) | [Next: RTE & Synchronization >](05_RTE_Synchronization.md)
