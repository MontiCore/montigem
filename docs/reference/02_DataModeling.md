# 2. Data Modeling (CD4A)

The data model is defined using the Class Diagram language (CD4A) in `.umlp` files located in the `models` directory.

## 2.1 Classes and Attributes

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
| `LocalDateTime` | `java.time.LocalDateTime` | `Date` | Mapped to native JS Date. |
| `LocalDate` | `java.time.LocalDate` | `Date` | |
| `List<T>` | `java.util.List<T>` | `Array<T>` | Only `List` is supported as a generic collection. |
| `Optional<T>` | `java.util.Optional<T>` | `T` | `undefined` represents empty. |

## 2.2 Enums

Enums define a fixed set of constants.

```java
enum RentableStatus {
  AVAILABLE,
  RENTED,
  MAINTENANCE;
}
```

*   **Usage:** `RentableStatus.AVAILABLE` maps to `RentableStatus.AVAILABLE` in both Java and TypeScript.

## 2.3 Associations

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
*   `List<Document> getDocumentsList()`: **Use this to get the full list.** Calling `getDocuments()` without arguments on a `[*]` association will cause a Java compiler error.
*   `void addDocument(Document d)`: Adds `d` and sets `d.owner = this`.
*   `void removeDocument(Document d)`: Removes `d` and sets `d.owner = null`.
*   `boolean containsDocument(Document d)`

**Generated Methods (Document):**
*   `User getOwner()`
*   `void setOwner(User u)`: Sets owner and adds `this` to `u.documents`.

**Synchronization:**
Modifying an association (e.g., `customer.addDocument(rt)`) automatically triggers a command that updates the backend database and synchronizes all connected clients.

## 2.4 Services (Singletons)

Services are defined as classes with the `<<singleton>>` stereotype. They are used for business logic that doesn't belong to a specific entity, or for managing global state.

```java
<<singleton>>
class RentalService {
  int visitCounter = 0;
  void rentItem(RentableItem item);
  void returnItem(RentalTransaction transaction);
}
```

*   **Backend:** A singleton bean is generated. 
    *   For methods, you must implement the logic in the corresponding HWC class (e.g., `RentalService.java`).
    *   If you defined variables *without* a default value in the data model, you also need to override the builder class 
        (e.g. by extending `RentalServiceBuilderTOP` and setting the variable in the constructor)
*   **Frontend:** A proxy class is generated.
    *   Calling `RentalService.rentItem(item)` in the frontend sends a command to the backend.
    *   Variables can be accessed by their getters and setters (e.g. `RentalService.getVisitCounter()`),
        and will be synced between all clients.
*   **Commands:** For each method, a `Command` class is generated (e.g., `RentalService_rentItem`).

## 2.5 Multiple Models & Merging

For large applications, you can split your data model into multiple `.umlp` files. The generator merges these files into a single domain model.

*   **Merging:** All `.umlp` files in the `models` directory are merged.
*   **Cross-References:** You can reference classes from other files using their fully qualified name.

For details on the merging process and conflict resolution, see [Class Diagram Merge](../unstructured/CDMerge.md).

## 2.6 Local Modifier

When using the `local` modifier keyword with an association or attribute,
the objects.
This is especially useful for UI states, as local singletons do not trigger updates on other clients.

```classdiagram
<<singleton>>
local class UIState {
  int currentMousePosition;
}
local association UIState -> SomeOtherClass [*];
```


---
[< Previous: Introduction](01_Introduction.md) | [Back to Index](../MontiGem_Documentation.md) | [Next: GUI Modeling >](03_GUIModeling.md)
