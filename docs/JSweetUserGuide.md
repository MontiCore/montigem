# User Guide: Supported Types, Methods, and Profiles (UMLP & GUI)

This document describes from a **user's perspective**:

* which Java types and methods are specifically supported by our JSweet adapters,
* how they are mapped to TypeScript,
* the difference between the **UMLP profile** (Backend/Domain) and the **GUI profile** (Angular Frontend).

---

## 1. Profiles & Entry Points

We distinguish between two main transpilation profiles:

| Profile | Entry Class | Purpose |
| :----- | :-------------- | :---- |
| **UMLP** | `umlp.jsweet.extension.UMLPAdapter` | Domain/Backend code, business logic, RTE-based collections (`UMLPArrayList`, `GemList`, ...). |
| **GUI** | `umlp.jsweet.extension.GUIAdapter` | Angular frontend, components, directives, reactive observer pattern. |

Both adapters build a **chain of sub-adapters** that specifically adapt types, methods, and imports.

---

## 2. Common Base (UMLP & GUI)

This functionality is available in both profiles.

### 2.1 Java Basics & Utilities

| Java Type / Method | TypeScript Mapping / Behavior | Remark |
| :----------------- | :----------------------------- | :-------- |
| `java.util.Optional<T>` | `T` | Optional is "unwrapped", i.e., the generated type is the inner type. |
| `Optional.or(supplier)` | Inlined function `((v,get) => v == null ? get() : v)(...)` (possibly with `await`) | Also works with async lambdas. |
| `java.lang.String` | `string` | Standard JS string. |
| `String.format(...)` | `StringHelper.format(...)` | `StringHelper` comes from the RTE library (`@umlp/commonj2ts` or `@umlp/common`). |
| `String.isBlank()` | `str.trim().length === 0` | |
| `java.util.Objects.requireNonNull(x[, msg])` | Lambda with null check and thrown Error instance | Prevents `null`/`undefined`. |
| `Objects.equals(a, b)` | For dates: null-safe, same comparison as `equals` of the date type; otherwise `===` | |

#### Date & Time (`java.time` / `java.util.Date`)

All following types are mapped to the native JavaScript **`Date`** object:

* `java.time.LocalDateTime`
* `java.time.LocalDate`
* `java.util.Date`

A `LocalDate` or `LocalDateTime` is a `Date` whose **local** calendar fields carry the value
(`new Date(y, m - 1, d[, h, min, s, ms])`), the same representation that `LocalDateDeSer`,
`LocalDateTimeDeSer` and `Conversion` (`@umlp/commonj2ts`) and `GemDateInput` use. Results
therefore do not depend on the browser's time zone. Arithmetic is wall-clock arithmetic as in
`java.time`. Limitation: a `LocalDateTime` inside a period skipped by a DST change (e.g.
02:00-03:00 on the last Sunday of March in Europe/Berlin) cannot be represented and is moved
forward by the DST shift; in zones whose DST starts at midnight, `atStartOfDay()` on that day
gives 01:00.

Important methods:

| Java | TypeScript Mapping |
| :--- | :----------------- |
| `now()` | `new Date()`, rebuilt from its local fields |
| `of(...)` | `new Date(y, m - 1, d, ...)` (local time) |
| `parse(String)` | strict ISO-8601 parse into local fields; throws `DateTimeParseException`. Unlike `java.time`, `LocalDate.parse` also accepts a time part (`2024-05-07T00:00:00`, as emitted by date inputs) and ignores it |
| `isEqual(other)`, `equals(other)` | `LocalDate`: same date fields, `LocalDateTime`: same wall-clock fields |
| `isAfter(other)`, `isBefore(other)` | comparison of the date / wall-clock fields |
| `compareTo(other)` | as in `java.time` (field differences, then -1/0/1) |
| `getYear()`, `getMonthValue()`, `getDayOfMonth()`, `getHour()`, ... | local getters (`.getFullYear()`, `.getMonth() + 1`, `.getDate()`, `.getHours()`, ...) |
| `getDayOfWeek()` | `LocalDateTimeHelper.DAYS_OF_WEEK[.getDay()]` |
| `toEpochSecond(offset)` | epoch seconds of the fields read as UTC (the offset argument is ignored) |
| `ofEpochSecond(sec, nanos, offset)` | fields of the UTC instant (the offset argument is ignored) |
| `toString()` | ISO-8601 text from the local fields, as `java.time` prints it |
| `plus*/minus*`, `with*`, `atStartOfDay()`, `toLocalDate()` | inline calendar arithmetic on the local fields |

Handwritten TypeScript can use `LocalDateTimeHelper` from `@umlp/commonj2ts`, which follows the
same convention. Implicit string conversion (`"" + date`) is not translated and yields the JS
`Date` text; call `toString()` explicitly.

---

### 2.2 MontiCore JSON & Logging

| Java Type / Method | Mapping | Remark |
| :----------------- | :------ | :-------- |
| `de.monticore.symboltable.serialization.json.JsonObject` | `any` | Type becomes `any` in TS. |
| `de.monticore.symboltable.serialization.json.JsonElement` | `any` | |
| `JsonParser.parseJsonObject(...)` | `JSON.parse(...)` | |
| `JsonParser.parse(...)` | `JSON.parse(...)` | |
| `JsonElement.isJsonObject()` | `typeof expr == "object"` | |
| `isJsonArray()` | `Array.isArray(expr)` | |
| `isJsonBoolean()` | `typeof expr == "boolean"` | |
| `isJsonString()` | `typeof expr == "string"` | |
| `isJsonNumber()` | `typeof expr == "number"` | |
| various `getAs*`/`get*Member` methods | `expr[key]` or `expr` | Access directly via JS object index. |
| `hasMember(key)` | `expr[key] != undefined && expr[key] != null` | |
| `has*Member(...)` | `typeof expr[key] == "..."/Array.isArray(...)` | |

Logging and helper types are referenced via a static library from the RTE:

* `de.se_rwth.commons.logging.Log`
* `de.monticore.symboltable.serialization.JsonPrinter`
* `de.se_rwth.commons.StringTransformations`

---

### 2.3 UMLP RTE & Angular Annotations

#### RTE Classes

All classes under the package

* `umlp.backendrte` (Command, Common, GUI, Request, Util, ShellAPI)

are imported via the TS RTE library (`@umlp/commonj2ts` or `@umlp/common`, depending on the `rte` option).

Special cases:

* always included: `mc.fenix.commonj2ts.deser.DeSerRepository`
* excluded: `umlp.backendrte.service.websocket.WebSocketConfig`

#### Default Method for RTE Interfaces

For all classes (not interfaces) that implement *_GemBuildable_* or *_GemListBuilder_*:
```ts 
public isBuildFinished(): boolean { return true; }
```

This method is automatically generated if it is not present in the Java code.

#### Angular Annotations

Custom annotation types:

* `umlp.jsweet.extension.annotation.Component`
* `umlp.jsweet.extension.annotation.Directive`
* `umlp.jsweet.extension.annotation.Input`
* `umlp.jsweet.extension.annotation.Output`

are mapped to `@angular/core` upon import. In conjunction with the GUI adapter, these become real Angular decorators.

#### AnnotationModule

Annotations with:
```java
@umlp.jsweet.extension.annotation.AnnotationModule("")
```

lead to module imports on exactly this TS path. This is relevant, for example, for custom decorator annotations.

---

### 2.4 "BackendOnly" & "Synchronized"

#### `@BackendOnly`

Methods with `@umlp.jsweet.extension.annotation.BackendOnly` are **never executed normally** in the browser at runtime. Instead, the body is replaced by generated code depending on the `BackendOnlyModes` value used:

* `STATUS_OK` → Returns `StatusResult.ok(this.getId())`
* `STATUS_ERROR` → Returns `StatusResult.error(this.getId())`
* `THROW_ERROR` (or default) → Throws `new Error('This method can only be called in the backend!');`

This is extremely useful when sharing classes (like Singletons) between backend and frontend, but certain methods rely on backend-only libraries.

#### `@Synchronized`

*Annotation: `umlp.jsweet.extension.annotation.Synchronized`*

* An instance `mutex = new Mutex();` is created (from `async-mutex`).
* The method body is translated into the following pattern:

  ```ts
  return await this.mutex.runExclusive(async () => {
    // original body
  });
  ```

Applies in both UMLP and GUI profiles.

#### `@Serialized`

*Annotation: `umlp.jsweet.extension.annotation.Serialized`*

Opt-in for handwritten binding methods of GUI components (methods with a generated `<method>ForAngular__update` pendant). By default, update runs triggered by observer events and cache misses fire in parallel; methods that mutate state can race. With `@Serialized` on the handwritten method, the generated update runs are chained: while one run is executing and one is waiting, further triggers coalesce into the waiting run. A rejected run does not break the chain (the retry cap of failing updates is kept). For parameterized bindings there is one chain per method, not per cache key.

---

### 2.5 Enums

For all Java `enum`s, `toString()` for instance calls is adapted so that the corresponding string name is fetched from the TS enum at runtime:
```ts
/* Enum.toString */ (EnumType[value] ? EnumType[value] : '')
```

---

### 2.6 Native JS Integration (`@Replace`)

For advanced use cases where direct JavaScript/TypeScript code is required (e.g., access to browser APIs like `fetch` or `setTimeout`), the annotation `@jsweet.lang.Replace` is available.

*   **Function:** The content of the method body in Java is ignored, and the string value of the annotation is used as the method body in the generated TypeScript instead.
*   **Usage:**
    ```java
    import jsweet.lang.Replace;

    @Replace("console.log('Hello from JS');")
    public void logNative() {
        // Java dummy implementation (will not be transpiled)
        System.out.println("Hello from Java");
    }
    ```
*   **Warning:** The code in the `@Replace` string is not validated. Syntax errors lead to errors in the generated TypeScript.

---

### 2.7 Excluding Files from Transpilation

Sometimes, backend classes (like utilities or services) are shared with the frontend source path but contain dependencies that JSweet cannot handle (e.g., external libraries). To exclude these files from transpilation, configure the `excludeSource` property in your `frontend/build.gradle`.

```groovy
tasks.named("umlpJava2ts") {
  excludeSource.add("glob:**/MyBackendOnlyUtil.java")
}

tasks.named("guiJava2ts") {
  excludeSource.add("glob:**/MyBackendOnlyUtil.java")
}
```

---

## 3. UMLP Profile (Backend/Domain)

In the UMLP profile, the focus is on **robust RTE collections** and asynchronous behavior (Commands, GemList, ...).

### 3.1 Collections (UMLP / Domain)

| Java Type | TypeScript Mapping | Remark |
| :------- | :----------------- | :-------- |
| `java.util.List` | `IUMLPList` (Interface) | Imported from TS RTE. |
| `java.util.AbstractList` | `IUMLPList` | |
| `ArrayList`, `LinkedList`, `Vector`, `Stack`, `CopyOnWriteArrayList` | `UMLPArrayList` | Custom RTE list class |
| `umlp.backendrte.common.GemList` | treated like `UMLPArrayList` | |
| `java.util.Map` + subclasses (`HashMap`, `TreeMap`, ...) | `Map<K, V>` | ES6 Map with correct type arguments |
| `umlp.guiutil.UMLPGuiArray` | `Array` / `[]` | GUI array with custom methods |

#### List Functions

* Constructors like `new ArrayList<>()` are generated as `new UMLPArrayList()`.
* `Collections.emptyList()` → `new UMLPArrayList()`
* `Collections.singletonList(x)` → `new UMLPArrayList([x])`
* `Collections.unmodifiableList(list)` → `new UMLPArrayList(list)`
* `Lists.newArrayList(...)` (Guava) → `new UMLPArrayList(...)`
* `Arrays.asList(...)` → `new UMLPArrayList([...])`

`remove(...)` on lists is interpreted as follows:

* Return type `boolean` → Method is named `removeElement(...)` in TS
* otherwise → Method is named `removeIndex(...)` in TS

#### Map Functions (UMLP & GUI same)

| Java | TypeScript |
| :--- | :--------- |
| `put(key, value)` | `map.set(key, value)` |
| `get(key)` | `map.get(key)` |
| `containsKey(key)` | `map.has(key)` |
| `keySet()` | `[...map.keys()]` |
| `values()` | `map.values()` |
| `size()` | `map.size` |
| `isEmpty()` | `map.size === 0` |
| `remove(key)` | `map.delete(key)` |
| `clear()` | `map.clear()` |
| `entrySet()` | `[...map.entries()]` |
| `clone()` | special copy lambda over `Object.keys(o)` |

Constructors:

* `new HashMap<>()`, `new TreeMap<>()`, ... → `new Map()`

#### `instanceof` on List Types

An `instanceof` check against one of the list types mentioned above is generically translated to:
```ts
expr != null && (expr instanceof UMLPArrayList || expr instanceof GemList || expr instanceof Array)
```

---

### 3.2 Asynchronicity (RTE + Lists)

In the UMLP profile (or if `rte=COMMONJ2TS_RTE`), multiple scanners analyze the code:

* **RTE methods** defined as asynchronous (e.g., `CommandManager.executeCommand`, `GemList.build`, `CommandDeSer.serializeCommand`, ...),
* as well as **List operations** (`add`, `get`, `remove`, `toArray`, etc.)
* and `for each` loops over `List`.

These scanners:

* automatically add the `async` keyword to affected methods,
* mark corresponding calls as `await` calls,
* propagate `async` up inheritance trees and across overloads.

For the user, this means:

* RTE calls and `GemList` operations can look *synchronous* in Java code,
* but are correctly implemented with `async/await` in TS code.

---

## 4. GUI Profile (Angular Frontend)

In the GUI profile, the focus is on Angular integration: Components, Directives, Observer pattern, and native arrays.

### 4.1 Collections (GUI)

| Java Type | TypeScript Mapping | Remark |
| :------- | :----------------- | :-------- |
| `java.util.List` / `ArrayList` / `LinkedList` / `GemList` | `Array<T>` (`[]`) | Direct mapping to JS Array |
| `java.util.Map` + subclasses | `Map<K,V>` | Same as in UMLP |

Factories:

* `Collections.emptyList()` → `[]`
* `Collections.singletonList(x)` → `[x]`
* `Collections.unmodifiableList(list)` → `[...list]`
* `Lists.newArrayList(...)` → `[...args]` or `[]`
* `Arrays.asList(...)` → `[...args]`
* `Sets.newHashSet(...)` (Guava) → `[...args]` (Array as Set replacement)

### 4.2 GUI Utilities & Angular Integration

| Java Type / Method | TS Mapping | Remark |
| :----------------- | :--------- | :-------- |
| `umlp.guiutil.Router.navigate(...)` | `this._router.navigate([ ...args ])` | Angular Router |
| `umlp.guiutil.Location.back()` | `this.location.back()` | `@angular/common` |
| `umlp.guiutil.HTMLUtils.alert(msg)` | `alert(msg)` | Browser Alert |
| `de.monticore.guidsl.types.GUIViewElement` | `TemplateRef<any>` | Import from `@angular/core` |

### 4.3 Components & Directives

Via custom Java annotations in the package `umlp.jsweet.extension.annotation`:

* `@Component`
* `@Directive`
* `@Input`
* `@Output`
* `@DecoratorNoObj`
* `@AnnotationModule(...)`

Angular decorators and imports are generated:

* Classes with `@Component`/`@Directive` become Angular Components/Directives.
* HWC Components can control configuration via annotation attributes:
    * `standalone`, `imports`, `selector`, `templateUrl`, `styles`, `styleUrls`, `tsImports`
* For Directives, a `protected router: Router;` field is additionally generated.
* An `@AnnotationModule("...")` on an annotation causes this annotation to be usable as a decorator with module import.

`@DecoratorNoObj` on annotations ensures that methods with these annotations in TS code directly receive a decorator like

```ts 
@MyDecorator(arg1, arg2)
```

### 4.4 Automatic Observer Pattern (GUI)

In Components/Directives, a lot happens automatically for "synchronized" methods:

* Methods with `@UMLPSynchronized` (or recognized as synchronized by `GUIAdapterUtils`)
    * receive additional "wrappers":
        * `method__synchronized(...)`
        * `method__update(...)`
    * use **result caching** via `ManyKeysMap`.
    * register observers for
        * Domain objects (`GemIdentifiable`) and
        * State variables (Getter/Setter).

* In `for each` loops over `List<GemIdentifiable>`, the following is automatically inserted:

  ```ts
  __regObsToDomainObj(element);
  ```

  before the loop body, to trigger UI updates when domain objects change.

* A clean lifecycle is implemented:
    * `ngOnDestroy()` calls `ComponentObserverManager.removeObservers(...)`.
    * For HWC Components, `super.ngOnDestroy()` is additionally called.

Helper functions `GUIUtils.registerObserverToDomainObject(...)`, `registerObserverToStateVariable(...)`, and `update(...)` are mapped to these internally generated observer functions.

### 4.5 GemList → Array (GUI)

`GemList` is mapped to arrays in the frontend, including:

* Mapping of type `GemList<T>` to `Array<T>`.
* Generated helper method in Directives:

  ```ts
  protected toArray<T>(arr: IUMLPList<T> | Array<T>): Array<T> {
    return GemListUtil.toArray(arr);
  }
  ```

* Return values that are `List` subtypes are transpiled – where necessary – to:

  ```ts
  (await GemListUtil.toArraySync(call(...)))
  ```

so that `Array<T>` can be worked with directly in Angular code.

---

## 5. GUI Types & NPM Packages (`TSImportMap`)

All GUI types from the following Java packages are automatically redirected to corresponding NPM packages:

| Java Package Prefix | NPM Package |
| :------------------ | :-------- |
| `mc.fenix.cad` | `@umlp/cad` |
| `mc.fenix.arrange` | `@umlp/arrange` |
| `mc.fenix.basic` | `@umlp/basic` |
| `mc.fenix.charts` | `@umlp/charts` |
| `mc.fenix.common` | `@umlp/common` |
| `mc.fenix.navigation` | `@umlp/navigation` |
| `mc.fenix.input` | `@umlp/input` |
| `mc.fenix.table` | `@umlp/table` |
| `mc.fenix.fischertechnik` | `@umlp/fischertechnik` |
| `mc.fenix.functiondisplay` | `@umlp/functiondisplay` |
| `mc.fenix.networkgraph` | `@umlp/networkgraph` |
| `mc.fenix.editor` | `@umlp/editor` |
| `loginmodule` | `@montigem/login` |
| `commentmodule` | `@montigem/comment` |

This applies in both the UMLP and GUI profiles – but has a stronger impact in the GUI profile due to the many visualization types.

---

## 6. Summary

* In the **UMLP Profile**, you get:
    * RTE-specific types & collections (`UMLPArrayList`, `GemList`, asynchronous Commands),
    * strong support for `Optional`, `Date/Time`, `Objects`, Enums,
    * automatic `async/await` propagation for RTE methods and list operations.

* In the **GUI Profile**, you additionally get:
    * native arrays for lists,
    * automatic mapping of Components/Directives to Angular,
    * an extensive Observer pattern for domain and state changes,
    * Utilities bridge (`Router`, `Location`, `HTMLUtils`, `GUIUtils`),
    * Mapping of GUI DSL types (`GUIViewElement`) to Angular types (`TemplateRef<any>`).

As a user, you can write largely idiomatic code in Java; the adapters ensure that the generated TypeScript code looks idiomatic for Angular and the UMLP RTE.

---

## 7. Supported Java Standard Library (Overview)

This section answers the question: **Which Java APIs can I use in code that is transpiled to TypeScript – and which should I avoid?**

### 7.1 Legend

* ✅ recommended / usable
* ⚠️ restricted / use with caution
* ⛔ not supported / avoid

### 7.2 Overview by Packages / Type Families

#### `java.util.stream.*` – ⛔ do not use

* Streams (`Stream<T>`, `IntStream`, `LongStream`, `DoubleStream`, ...) should **not** be used in transpiled code.
* All calls to `.stream()` are detected and lead to warnings.
* **Instead:** use classic loops (`for`, `for-each`) or helper methods without streams.

---

#### `java.util.Optional<T>` – ⚠️ restricted

**Usable methods (recommended / common):**

* `Optional.of(...)`
* `Optional.ofNullable(...)`
* `Optional.empty()`
* `Optional.isPresent()` / `isEmpty()`
* `Optional.get()`
* `Optional.orElse(...)`
* `Optional.orElseGet(Supplier)`
* `Optional.orElseThrow()` / `orElseThrow(Supplier)`
* `Optional.ifPresent(Consumer)`
* `Optional.ifPresentOrElse(Consumer, Runnable)` (if provided by your Java/JSweet version)
* `Optional.or(Supplier)` – explicitly supported by our adapters.

**Do not use:**

* `Optional.stream()` (belongs to the Stream API, which is not supported).

---

#### `java.util.Objects` – ⚠️ only selected methods

**Recommended:**

* `Objects.requireNonNull(obj)`
* `Objects.requireNonNull(obj, message)`
* `Objects.equals(a, b)`

**With caution / better avoid:**

* `Objects.isNull(obj)`
* `Objects.nonNull(obj)`
* `Objects.hash(...)`
* `Objects.hashCode(obj)`
* `Objects.deepEquals(a, b)`
* `Objects.compare(...)`, `compareUnsigned(...)`
* `Objects.requireNonNullElse(...)`, `requireNonNullElseGet(...)`

The latter might work technically but are not specifically secured by our transpilation.

---

#### `java.lang.String` – ✅ largely usable

**Recommended / explicitly supported:**

* `String.format(...)`
* `String.isBlank()`

**Usable beyond that (typical String API):**

* `length()`
* `charAt(int)`
* `substring(...)`
* `toLowerCase()`, `toUpperCase()`
* `trim()`
* `contains(CharSequence)`
* `startsWith(...)`, `endsWith(...)`
* `indexOf(...)`, `lastIndexOf(...)`
* `replace(...)`, `replaceAll(...)`
* `split(String)`
* `String.join(...)` (static)

---

#### `java.time.LocalDateTime`, `java.time.LocalDate`, `java.util.Date` – ✅ recommended

**Recommended and secured by adapters:**

* Creation:
  * `now()`
  * `of(...)` (for date/time)
* Parsing:
  * `parse(String)`
* Comparisons:
  * `isAfter(...)`, `isBefore(...)`, `isEqual(...)`, `compareTo(...)`
* Getters:
  * `getYear()`, `getMonthValue()`, `getDayOfMonth()`
* Epoch functions:
  * `toEpochSecond()`
  * `ofEpochSecond(...)`
* String representation:
  * `toString()` on `LocalDateTime`, `LocalDate`, `Date`
* Calculation operations:
  * `plusYears/Months/Weeks/Days/Hours/Minutes/Seconds`
  * `minusYears/Months/Weeks/Days/Hours/Minutes/Seconds`
  * `atStartOfDay()`

**Not recommended / not supported:**

* Other `java.time` types like `Instant`, `ZonedDateTime`, `OffsetDateTime`, `Period`, `Duration`, etc.
* Methods working with `TemporalAmount`/`TemporalAdjuster` (`plus(TemporalAmount)`, `with(TemporalAdjuster)`, etc.).

---

#### `java.util.List` and Implementations – ✅ use, but without streams

**Usable List Types:**

* `java.util.List`
* `java.util.ArrayList`
* `java.util.LinkedList`
* `java.util.Vector`
* `java.util.Stack`
* `java.util.concurrent.CopyOnWriteArrayList`

**Typical methods you can use:**

* Creation:
  * `new ArrayList<>()`, `new LinkedList<>()`, ...
* Access / Mutation:
  * `add(E)`, `add(int, E)`
  * `addAll(Collection)`, `addAll(int, Collection)`
  * `get(int)`
  * `set(int, E)`
  * `remove(int)`, `remove(Object)`
  * `size()`
  * `isEmpty()`
  * `contains(Object)`, `containsAll(Collection)`
  * `indexOf(...)`, `lastIndexOf(...)`
  * `toArray()` (especially in combination with our RTE helper methods)
* Iteration:
  * `for-each` loops over `List<E>`

**Supported Factories / Helper Methods:**

* `java.util.Collections.emptyList()`
* `Collections.singletonList(E)`
* `Collections.unmodifiableList(List)`
* `Collections.unmodifiableCollection(Collection)`
* `Collections.nCopies(int, E)`
* `java.util.Arrays.asList(E...)`
* `com.google.common.collect.Lists.newArrayList(...)`

**Do not use:**

* `List.of(...)` (Java 9 Factory)
* `List.stream()` (belongs to Stream API)

Other `Collections` helper functions not specifically supported by us (e.g., `Collections.sort`, `reverse`, `shuffle`, `binarySearch`, `fill`, `copy`) should be avoided in transpiled code.

---

#### `java.util.Map` and Implementations – ✅ recommended

* `Map`, `HashMap`, `TreeMap`, `LinkedHashMap`, `WeakHashMap`, `Hashtable`, `NavigableMap`, etc. can be used.
* Common methods like `put`, `get`, `containsKey`, `keySet`, `values`, `size`, `isEmpty`, `remove`, `clear`, `entrySet`, `clone` are usable.

---

#### `java.util.Set` and Guava Sets – ⚠️ restricted

**Partially supported:**

* `com.google.common.collect.Sets.newHashSet(...)`
  → can be used, internally creates a collection treated like a list/array.

**Basically usable (but without special adapter support):**

* `java.util.Set` and implementations (`HashSet`, `TreeSet`, ...) with standard methods like `add`, `remove`, `contains`, `size`.

**Note:**

* There is no targeted transpilation for Set-specific operations (e.g., `retainAll`, `removeAll`, true set logic).
* In doubt, prefer `List`/`Map` with clear semantics.

---

#### `java.util.concurrent` – ⚠️ very restricted

**Acceptable:**

* `java.util.concurrent.atomic.AtomicReference<T>` as a simple container type:
  * Creation (`new AtomicReference<>(value)`),
  * `get()`, `set(value)` are syntactically unproblematic.
  * However, there is **no** real Java concurrency semantics behind it.

**Do not use:**

* All other `java.util.concurrent` types:
  * `Thread`, `ExecutorService`, `Future`, `CompletableFuture`, `Semaphore`, `Lock`, `CountDownLatch`, ...
  * These do not fit the single-thread JavaScript environment and are not supported by our adapters.

**Instead:**

* Use asynchronous methods (`async`/`await`).
* Use `@Synchronized` in conjunction with the Mutex mechanism (`Mutex` from `async-mutex`).

---

#### `java.io.*`, `java.net.*`, `java.sql.*`, `java.security.*` – ⚠️ to ⛔

* These packages are only very limitedly or not at all usable for browser/TS environments.
* There is no special support by our adapters.
* **Recommendation:** use only in pure backend (not in code transpiled to TypeScript).

---

#### `java.lang.reflect.*` – ⛔ avoid

* Reflection is practically not usable in transpiled code.
* **Please do not** use in classes transpiled to TypeScript.

---

### 7.3 Short Table "Use / Avoid"

| Package / Type Family | Status | Recommendation |
| :----------------- | :----- | :-------- |
| `java.util.stream.*` | ⛔ | Do not use. Use loops/`forEach` instead. |
| `java.util.Optional` | ⚠️ | Basic API usable, but no streams; test complex patterns. |
| `java.util.Objects` | ⚠️ | `requireNonNull`/`equals` ok; avoid other methods if possible. |
| `java.lang.String` | ✅ | Well usable (incl. `format`, `isBlank`). |
| `java.time.LocalDateTime`, `LocalDate`, `java.util.Date` | ✅ | Recommended for date/time, avoid other `java.time` types. |
| `java.util.List` & Implementations | ✅ | Use standard List API + targeted factories, no streams/`List.of`. |
| `java.util.Map` & Implementations | ✅ | Standard Map API, avoid `Map.of`/`unmodifiableMap`. |
| `java.util.Set` & Guava Sets | ⚠️ | Basic usage ok, avoid complex set logic. |
| `java.util.concurrent` | ⚠️ | Only `AtomicReference` as container type; otherwise prefer `async/await` + `@Synchronized`. |
| `java.io`, `java.net`, `java.sql`, `java.security` | ⚠️/⛔ | Do not use in transpiled code. |
| `java.lang.reflect` | ⛔ | Do not use in transpiled code. |

---

### 7.4 JSweet Quirks and Limitations

*   **Avoid `var`:** Do not use the `var` keyword for complex generated MontiGem types (especially lists or builders). JSweet's type inference may fail, leading to compilation errors in the generated TypeScript. Use explicit type declarations instead.
*   **Avoid `getLast()`:** Calling `list.getLast()` on MontiGem lists may fail during transpilation or at runtime. Always use `list.get(list.size() - 1)` instead.