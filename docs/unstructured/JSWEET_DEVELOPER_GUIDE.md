## 10. Technical Notes on JDK Transpilation

This section complements the User Guide with technical details on the mapping of some central JDK types. It is intended for developers who want to understand *how* methods are mapped to TypeScript in detail.

### 10.1 Optional, Objects, String

* `java.util.Optional<T>`
  * Type Mapping: `Optional<T>` → inner type `T` (via `JavaBasicsAdapter.mapOptionalType`).
  * Special Method: `Optional.or(Supplier)` is translated into an inline function and correctly annotated with `await` for async suppliers.
* `java.util.Objects`
  * `requireNonNull` generates a lambda that throws an Error with matching "Java classes" in the `__classes` field if `null`/`undefined`.
  * `equals` checks via `getTime()` for Date types, otherwise via `===`.
* `java.lang.String`
  * `format` is mapped to a TS helper `StringHelper.format(...)`.
  * `isBlank` is translated to `.trim().length === 0`.

### 10.2 Date/Time

* `java.time.LocalDateTime`, `java.time.LocalDate`, `java.time.LocalTime`, `java.time.Instant`, `java.time.Duration`,
  `java.time.ZonedDateTime`
  * are mapped to `Temporal.PlainDateTime`, `PlainDate`, `PlainTime`, `Instant`, `Duration`, `ZonedDateTime` (type
    mappings in `JavaTimeAdapter`; `ZoneId` and `ZoneOffset` are strings,
    `Temporal` from `@umlp/commonj2ts`); `java.util.Date` is not supported (error `0xEE013`).
  * `now`, `parse`, comparison and getter methods as well as `toEpochSecond`/`ofEpochSecond` are explicitly translated into TS expressions.
  * Calculation operations (`plus*/minus*`, `atStartOfDay`) are delegated via `LocalDateTimeHelper` (`@umlp/commonj2ts`).

### 10.3 Collections

* UMLP Profile (`CollectionsAdapter`)
  * `List`/`AbstractList` → `IUMLPList`
  * `ArrayList`, `LinkedList`, `Vector`, `Stack`, `CopyOnWriteArrayList` → `UMLPArrayList`
  * `UMLPGuiArray` → `Array`
  * `Collections.emptyList`, `singletonList`, `unmodifiableList`, `nCopies`, `Lists.newArrayList`, `Arrays.asList` are all translated into constructor calls on `UMLPArrayList` or `Array`.
  * List method `remove` is renamed to `removeElement` or `removeIndex` based on return type analysis (`getContext().addMethodNameMapping`).
  * Foreach over `List` is translated into `for await (...)` loops.
  * `instanceof` against List types is replaced by checks against `UMLPArrayList`, `GemList`, and `Array`.
* GUI Profile (`UMLPGUICollectionsAdapter`)
  * `GemList` is mapped to `Array` via type mapping.
  * Collections factories (`emptyList`, `singletonList`, `unmodifiableList`, `newArrayList`, `asList`, `newHashSet`) are translated to array literals.
* Map API (both profiles)
  * Map subtypes (`Map`, `HashMap`, `TreeMap`, `LinkedHashMap`, `Hashtable`, `WeakHashMap`, `NavigableMap`) are mapped to a TS `Map<K,V>` API:
    * `put` → `.set(...)`
    * `get` → `.get(...)`
    * `containsKey` → `.has(...)`
    * `keySet` → `[...map.keys()]`
    * `values` → `map.values()`
    * `size` → `map.size`
    * `isEmpty` → `map.size === 0`
    * `remove` → `.delete(...)`
    * `clear` → `.clear()`
    * `entrySet` → `[...map.entries()]`
    * `clone` → Shallow copy via `Object.keys(...)`.

### 10.4 Streams

* `JSweetCoCoChecker` checks `visitMethodInvocation` for occurrences of `.stream()`.
* If found, a warning is logged; **no** translation of the Stream API takes place.
* This ensures that stream usage is noticed early and does not lead to hard-to-trace runtime errors in TS.

## 11. Extending JSweet Adapters – Approach & Pitfalls

This section describes how to integrate **new types or methods** into the transpilation and what to watch out for.

### 11.1 Basic Principle: Adapter Chain

Transpilation runs via a **chain of `PrinterAdapter` instances**, e.g.
```text
UMLPAdapter └─ GUITypesImportAdapter └─ CollectionsAdapter └─ JavaBasicsAdapter └─ UMLPRTEImportAdapter └─ BasicsAdapter └─ BackendOnlyAdapter └─ MutexAdapter └─ UMLPEnumAdapter └─ JSweet-Standardadapter
```
Each adapter can:

* Remap types (`addTypeMapping`, `mapType`, `getTypeMirrorMappings()`),
* Adjust imports (`needsImport`, `getModuleImportDescriptor`),
* Replace concrete syntax elements (`substituteMethodInvocation`, `substituteNewClass`, `substituteTypeCast`, ...),
* Insert additional code fragments (`beforeTypeBody`, `afterTypeBody`, ...).

**Important:** The **order** in the chain determines which adapter makes which decision first.

---

### 11.2 Adding Type Mappings

**Goal:** A Java type `X` should appear as a different type in TS code (e.g., `Optional<T>` → `T`, `LocalDateTime` → `Temporal.PlainDateTime`).

**Procedure:**

1. Choose suitable adapter:
  * DOMAIN → `umlp.jsweet.extension.common.JavaBasicsAdapter`, `CollectionsAdapter`, `BasicsAdapter`.
  * GUI-specific → `umlp.jsweet.extension.gui.UMLPGUICollectionsAdapter`, `TSTypesAdapter`, `GUIUtilAdapter`.
2. In this adapter:
  * Either register a function in `getTypeMirrorMappings()`:
    ```java
    getTypeMirrorMappings().add(0, (TypeMirror type) -> {
      if (type.toString().equals("my.package.MyType")) {
        return "MyTsType";
      }
      return null;
    });
    ```
  * Or add an explicit string mapping:
    ```java
    addTypeMapping("my.package.MyType", "MyTsType");
    ```
3. If the type should come from the RTE library:
  * Register via `putToStaticLibrary(...)` in `initStaticLibrary()` so that import and module path are set correctly.

**Attention:**

* Mappings are **profile-dependent**:
  * UMLP Profile: e.g., `List` → `IUMLPList`/`UMLPArrayList`.
  * GUI Profile: e.g., `GemList` → `Array`.
* Always check if the mapping makes sense for both profiles – otherwise only enter it in the profile-specific adapter.

---

### 11.3 Supplementing Method Mappings (`substituteMethodInvocation`)

**Goal:** Certain method calls should look different in TS code or go to helper functions.

**Examples in the project:**

* `Optional.or(...)` → Inline function
* `Date.parse(...)` → `Conversion.string2Date(...)`
* `Objects.requireNonNull(...)` → Inline null check with Error
* `Collections.emptyList()` → `new UMLPArrayList()` / `[]`

**Procedure:**

1. Override `substituteMethodInvocation(MethodInvocationElement invocation)` in the appropriate adapter.
2. Distinguish by `invocation.getTargetType().toString()` or the "enclosing" type (`targetClassName`).
3. Only write **own output** if all relevant cases are covered – otherwise call `super.substituteMethodInvocation`.

**Pitfalls:**

* **Overloads:**  
  If multiple overloaded methods exist, the mapping must be meaningful for all relevant signatures.
* **Generics:**  
  For generics (`List<T>`, `Optional<T>`), prefer working on `TypeMirror` and `DeclaredType` instead of just comparing strings.
* **Async/await:**  
  If a method is to become asynchronous in combination with `UMLPAsyncAwaitPropagationScanner`, ensure that the mapping does not "swallow" or double-add an `await`.
* **Macro Names:**  
  You can mark which replacement took place in the generated code via `UMLPAdapterUtils.printMacroName("...")` – helpful for debugging.

---

### 11.4 Adjusting Imports and Module Paths

**Important Methods:**

* `needsImport(ImportElement importElement, String qualifiedName)`
  * controls when a type must be imported and with which "root name".
* `getModuleImportDescriptor(CompilationUnitElement cu, String importedName, TypeElement importedClass)`
  * controls from **which module path** a type is imported (e.g., `@umlp/common`, `@umlp/commonj2ts`, `@angular/core`, `@umlp/input` etc.).

**Typical Extensions:**

* Enter new TS packages in `TSImportMap` (GUI side).
* Register new RTE types in `AbstractAdapter`-based adapters via `initStaticLibrary()`.

**Attention:**

* Avoid **duplicate imports**:
  * `UMLPAdapterUtils.importFromWithoutModule(...)` checks via `context.getImportedNames(...)` if a name is already imported.
* Risk of confusion with identically named types in different packages (e.g., own `Map` vs. `java.util.Map`) – clear naming conventions and static mapping help here.

---

### 11.5 Extending CoCos (Code Checks)

**Goal:** Warn developers early that they are using **unsupported** Java constructs (e.g., Streams, `List.of`, certain `Collections` helpers).

In the project, `JSweetCoCoChecker` currently handles this:

* runs in `onTranspilationStarted()` of `UMLPAdapter` and `GUIAdapter`.
* checks among others:
  * `.stream()` usages,
  * Calls to `java.util.stream.*`,
  * `List.of`, `Map.of`, `Set.of`,
  * unsupported `Collections` helpers,
  * problematic `Objects` methods,
  * unwanted `java.util.concurrent` types.

**Procedure for new CoCos:**

1. Add a new check in `JSweetCoCoChecker`:
  * either directly in `visitMethodInvocation(...)`,
  * or via a helper method like `checkUnsupportedApis(...)`.
2. Always:
  * consider `surroundingExcludedFromTranspilation()` (do not check BackendOnly/Erased),
  * use a **unique Log ID** (`0xEE0xx`),
  * append the position via `getStartSourcePosition(...)`.
3. Choose Log Level consciously:
  * `Log.warn(...)` for "please avoid",
  * `Log.error(...)` only if transpilation really cannot continue.

---

### 11.6 Hooking Adapters into the Chain or Reordering

If a completely new adapter is needed:

1. Create **Adapter Class**:
  * Either inherit from `PrinterAdapter` if you don't need a static library.
  * Or inherit from `AbstractAdapter` if you also need type/import mappings.
2. Insert into the constructor in the corresponding "entry adapter" (e.g., `UMLPAdapter`, `GUIAdapter`, `Module*Adapter`), e.g.:

   ```java
   public class UMLPAdapter extends PrinterAdapter {
     public UMLPAdapter(PrinterAdapter parentAdapter) {
       super(
           new GUITypesImportAdapter(
               new CollectionsAdapter(
                   new JavaBasicsAdapter(
                       new UMLPRTEImportAdapter(
                           new MyNewAdapter( // <--- hook in here
                               new BasicsAdapter(
                                   new BackendOnlyAdapter(
                                       new MutexAdapter(
                                           new UMLPEnumAdapter(
                                               parentAdapter
                                           )
                                       )
                                   )
                               )
                           )
                       )
                   )
               )
           )
       );
     }
   }
   ```

**What to watch out for:**

* Adapters that set **Type Mappings** should come as early as possible in the chain so that later adapters already see the "correct" TS types.
* Adapters that only add **refinements** (e.g., Observer pattern, Decorators) can hang further out.
* If multiple adapters influence the same aspect (e.g., Collections), pay attention to which adapter applies "last".

---

### 11.7 Typical Error Sources in Extensions

* **Asymmetric Profiles:**  
  Change only in UMLP profile, but not in GUI profile (or vice versa) → leads to different TS types for the same Java type.
* **Incomplete Overload Handling:**  
  Only one of several overloads is handled → leads to inconsistent behavior.
* **Missing CoCos:**  
  New restriction in User Guide, but no CoCo → developers only encounter problems at runtime.
* **Import Conflicts:**  
  Same SimpleName in multiple packages without clear mapping → hard-to-trace TS errors.

---

With these guidelines, you can extend JSweet transpilation and UMLP-specific adapters in a controlled manner:

* **First**: define what is allowed in the User Guide.
* **Then**: teach adapters the desired TS representation.
* **Finally**: add CoCos to report violations early.
