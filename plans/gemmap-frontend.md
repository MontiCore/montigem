# Plan: frontend (JSweet/TypeScript) support for GemMaps, maps as async `IUMLPMap`

Instructions for a future conversation. Read this file completely, then check
`git log` on branch `gemmap-attribute-qualifiers` (or wherever it was merged) for
changes made after this plan was written (2026-10-22). Line numbers are hints only;
search for the named classes and methods. Commit each step separately (the user
wants that). Related plan: `plans/gemmap-list-keys.md` (has the backend background).

## 1. Decisions already made with the user

- **Option A: maps work like lists.** In transpiled code `java.util.Map` becomes an async
  interface `IUMLPMap<K,V>` (like `java.util.List` -> `IUMLPList`), ordinary maps
  (`HashMap`, `LinkedHashMap`, `TreeMap`, ...) become `AsyncMap<K,V>` (like `ArrayList` ->
  `AsyncList`), and the TS GemMaps implement `IUMLPMap` lazily. This replaces today's
  mapping of every `java.util.Map` to a native, synchronous JS `Map`.
- **`getRoleMap()` returns a plain `java.util.Map<K,V>`** (done, commit `05c28c9f5`): the
  owner's live GemMap, like `getRoleList()` returns the live GemList.
- **Live updatability is required**: no snapshot copies of the map.
- **Views are live and weakly consistent** (backend done in `GemMapViews`, commit `567073901`):
  `keySet()`, `values()` (declared `Collection<V>`, runtime an `AsyncList`-like view),
  `entrySet()` (TS: tuples `[K, V]`) reflect the map; removal through views/iterators is
  synchronized; adding and `Entry.setValue` are unsupported; iterators iterate the keys
  present at their creation and skip removed entries, no ConcurrentModificationException;
  values/key objects are loaded in one batch per iterator. `AsyncMap` views must be live too
  (today JSweet gives copies).
- **Overloads were renamed** for JSweet: `containsValueID(long)`, `containsKeyID(long)`.
- **GUI support for maps is deferred** (step 6). Until then GUI models reject attribute
  qualified roles with a clear error (GUI CoCo `0x00032`, done, commit `2929197b4`); hand-written
  page code can use `getRoleMap()`. Ordinary maps are not displayable in GUI models either
  (domain attributes cannot be maps, CoCo `0x70876`; no `.gui` in the repo uses `Map`), so full
  GUI support should cover map-typed GUI variables, too.

## 2. Backend state the frontend must mirror

`umlp-rte/backend/src/main/java/umlp/backendrte/common/`: `GemValueMap`, `GemValueMapBuilder`,
`GemEntryMap`, `GemEntryMapBuilder`, `GemMapViews`, `IDOrBuilder`. Key behaviour:
derived keys (`deriveKey`/`deriveKeyID`, `isKeyDerived`), lazily keyed ids
(`unkeyedValueIDs`, `ensureKeyed` loads in one batch), rekeying (`_internal_rekey`),
hooks `onValueKeyed`/`onValueRemoved`, synchronized hooks `_internal_putValueAndSync`,
`_internal_removeValueIDAndSync`, `_internal_removeAndSync`, `put(key, value)` checks the
key (0xA7305/0xA7315), `clear()` removes value by value, builders keep unbuilt builders
(`IDOrBuilder`), insertion order (`LinkedHashMap`). Tests:
`umlp-rte/backend/src/test/java/umlp/backendrte/GemMapBuilderTest.java`.

Generated per attribute qualified association (`umlp-tool`, `GemMapAssocDecorator`, templates
`umlp/gemmap/`, `umlp/methods/gemmap/`): `<Owner>_<Role>GemMap extends GemValueMapBuilder|GemEntryMapBuilder`
with `loadValueObjects` (+ `loadKeyObjects`), `deriveKey(ID)`, `isKeyDerived`, `getOwner`,
the synchronized hook overrides, `onValueKeyed`/`onValueRemoved` with one anonymous
`<Type>Observer` per subtype of the target (instanceof chain, most specific first).
Owner API: `getRole(K)`, `getRoleMap()`, `containsKeyRole`, `containsRole`, `sizeRoles`,
`putRole`, `removeRole`, `_internal_removeRoleId`, `removeRoleByIdSure`,
`_internal_getRoleGemMap()`; builder `rolePut`, `roleGemMapIds`; DeSer serializes value ids,
deserializes via `roleGemMapIds`. End-to-end tests: `examples/it-test` (`QualifiedAssocs.umlp`,
`qualifiedassocs/QualifiedAssocsDbTest`), backend only.

## 3. How the frontend works today (facts collected 2026-10-22)

TS RTE (`umlp-rte/frontend/commonj2ts/src/main/gui/mc/fenix/commonj2ts/`, package `@umlp/commonj2ts`):
- **Hand-maintained**, not generated. `common/GemList.ts`, `GemListBuilder.ts`,
  `IDOrBuilder.ts`, `IUMLPList.ts`; `javautil/AsyncList.ts` (implements `IUMLPList`, wraps an
  array, `__internalArray()`), `javautil/AsyncSet.ts` (can delegate to any `Collection` via
  `__AsyncSetConstructor(collection)` - use this for live views), `javautil/Collection.ts`.
  Exports via `common/index.ts` / root `index.ts`. No Map type exists yet.
- JSweet patterns used by these classes: empty `constructor()` plus
  `async __XConstructor(...)` (callers write `await new X().__XConstructor(...)`),
  `X["__class"] = "umlp.backendrte.common.X"`, `X["__interfaces"] = [...]`, overload
  dispatchers with mangled names (e.g. `getIDs$int$int`) where generated code calls them.

JSweet extension (`umlp-jsweet/src/main/java/umlp/jsweet/extension/`):
- `common/UMLPRTEImportAdapter`: every `umlp.backendrte.*` type is imported from
  `@umlp/commonj2ts` by simple name -> a new RTE class only needs a TS export with that name.
  It also injects `isBuildFinished(): boolean { return true; }` into subclasses of
  `GemBuildable` that do not declare it (also hits generated GemList/GemMap classes - check
  this does not break the builders' `buildFinished` semantics; prefer the field in TS RTE code).
- `common/CollectionsAdapter` (in UMLPAdapter, GUIAdapter, ModuleUMLPAdapter, ModuleDomainCDAdapter):
  - type mappings: any `java.util.Map` subtype -> `Map<K, V>` (`computeMapTypeMapping`,
    registered at index 0), `Map.Entry` -> tuple `[K, V]`; static library: `List`/`AbstractList`
    -> `IUMLPList`, `ArrayList`/`LinkedList`/... -> `AsyncList`, `Set`/`HashSet`/`LinkedHashSet`
    -> `AsyncSet`, `Collection` -> `Collection`;
  - `substituteMapInvocations`: `put`->`.set`, `get`->`.get`, `containsKey`->`.has`,
    `size()`->`.size` (property), `isEmpty`->`.size === 0`, `remove`->`.delete`, `keySet`/`entrySet`
    -> AsyncSet copies, `values`->native iterator, Entry `getKey`/`getValue` -> `[0]`/`[1]`;
  - `substituteNewClass`: `new HashMap<>()` etc. -> `new Map()`;
  - `substituteForEachLoop`: async `for await` for List/Set/Collection only;
  - `substituteInstanceof` for static library types.
  - Calls are matched by the receiver's static type (`targetClassName`); classes whose superclass
    is not a `java.*` class (GemValueMap, generated GemMaps) are not registered as JDK subclasses.
- `module/JavaCollectionToJSArrayAdapter` (only in `ModuleGUIAdapter`): when `asyncList` is
  false (`umlp-tool/.../umlp/jsweetconfig/jsweetGUINoDomain.json`, module GUI libraries), `List`
  -> `Array` and Map -> native `Map`. **Keep native maps in that mode.**
- Async: `common/UMLPAsyncAwaitPropagationScanner` treats a method as async iff it carries
  `@jsweet.lang.Async`. Sources: `umlp-jsweet/src/main/resources/asyncjre.json` (JRE; today only
  `java.util.Map.keySet`/`entrySet` for maps, all `List`/`Set` methods), the backend RTE jar's
  `umlp-rte/backend/src/main/resources/jsweetasync.json` (RTE methods, read by the JSweet fork;
  `umlp-rte/frontend/build.gradle` task `mergeJSweetAsyncMethods` merges the libs'
  `jsweet-async-methods.json`), per-transpilation `jsweet-async-methods.json`, and explicit
  `@Async`. Async propagates along overriding and to callers (`awaitUMLPInvocation`:
  "methods overriding async methods from the RTE must be async again").
  Note: `jsweetasync.json` lists `IDOrBuilder.getIdAndMaybeBuild`, the Java method is
  `getIDAndMaybeBuild` (casing mismatch, check).
- Other adapters printing native Maps internally (not affected by transpiled Java maps):
  `MutexAdapter` (`keyedMutexes`), `GUIComponentObserverPatternAdapter` (`ManyKeysMap`),
  `umlp-tool/.../umlp/gui2ts/TSComponentObserverManager.ftl` (TS template).

Transpile configs (`umlp-tool/.../gui/JSweetConfigGenerator`): app domain builds use
`JSweetConfigDomainJSON.json` (UMLPAdapter), app GUI builds `JSweetConfigGUIJSON.json`
(GUIAdapter), module domain builds `jsweetCDConfig.json` (ModuleUMLPAdapter), module GUI builds
without domain `jsweetGUINoDomain.json` (ModuleGUIAdapter, `asyncList: false`).
Frontend transpile excludes `*ManagerImpl`, `*DAO*`, `*DBObserver`, ... (gradle-plugin
`FrontendPlugin`), not `gemlists/`/`gemmaps/`.

Generated TS samples for lists: `examples/it-ocl-test/frontend/build/app/umlpJava2ts/mydomain/`
(`gemlists/Group_MembersGemList.ts`, `Group.ts`, `GroupBuilder.ts`, `GroupDeSer.ts`,
`MydomainManagerClientImpl.ts`) - mirror these for maps.

## 4. Steps

**Before step 1: settle the open questions in section 6 with the user** (the `IUMLPMap`
method set and the `Map.Entry` representation). The user deliberately left them for the session
doing the frontend work; they determine the interface of step 1 and the JSweet rules of step 2.

### Step 1: TS RTE `IUMLPMap` and `AsyncMap`
- `common/IUMLPMap.ts` (or `javautil/`, next to `IUMLPList` placement conventions): async
  interface with the `java.util.Map` methods transpiled code may call: `get`, `put`, `remove`,
  `containsKey`, `containsValue`, `size`, `isEmpty`, `clear`, `putAll`, `keySet` (`AsyncSet<K>`),
  `values` (`Collection<V>`), `entrySet` (`AsyncSet<[K, V]>`), `getOrDefault`, `forEach`,
  `putIfAbsent`, ... (decide the supported subset, document unsupported ones),
  `__internalMap(): Map<K, V>` for interop (like `__internalArray()`), async iteration.
- `javautil/AsyncMap.ts`: implementation wrapping a native `Map`; constructor pattern
  `__AsyncMapConstructor(data?: Map | IUMLPMap | entries)`; **live, weakly consistent views**
  (view `Collection`s over the map wrapped by `AsyncSet`'s delegate constructor / an `AsyncList`-like
  view for `values`).
- Unit/exec tests in umlp-jsweet's exec test setup (tsc + node) or a commonj2ts test.

### Step 2: JSweet: `java.util.Map` -> `IUMLPMap`, map classes -> `AsyncMap`
- `CollectionsAdapter`: replace the native-Map type mapping by static library mappings
  (`Map`/`AbstractMap` -> `IUMLPMap`; `HashMap`, `LinkedHashMap`, `TreeMap`, `ConcurrentHashMap`,
  `Hashtable`, `WeakHashMap`, ... -> `AsyncMap`); drop the `.set/.has/.size` rewrites in
  `substituteMapInvocations` for async mode (calls become method calls); `new HashMap<>()` ->
  `(await new AsyncMap<any>().__AsyncMapConstructor())`; `Map.Entry` stays a tuple (decide);
  `for` over `keySet()/values()/entrySet()` -> `for await`; `instanceof Map` handling; static
  helpers (`Map.of`, `Collections.unmodifiableMap`, `Collectors.toMap` in `JavaStreamAdapter`).
- `asyncjre.json`: all `java.util.Map` methods async (like `List`).
- Keep `asyncList: false` (`JavaCollectionToJSArrayAdapter`) on native maps.
- Exclude nothing for GemMaps any more: generated GemMap classes extend RTE classes that
  implement `IUMLPMap`, owner fields are typed `GemValueMap`/`GemEntryMap` (RTE imports).
  Verify the `Map`-subtype type mapping no longer turns `GemValueMap<K,V>` fields/`extends`
  clauses into `Map<K,V>`.

### Step 3: Migrate existing map usages and verify all frontends
Everything transpiled that uses maps becomes async; check and fix:
- generated templates: `managerclientimpl/LoadAllWithIDs.ftl`, `gemlist/LoadObjects.ftl`,
  `gemmap/LoadObjects.ftl`, `methods/assocs/InternalRemove.ftl`, `baseclassobjectmapper/Init.ftl`,
  `gui2java/GUI2JavaStaticBuilderInitBody.ftl`, `GUI2JavaStaticConstructorInit.ftl`,
  `GUI2JavaStaticListInit.ftl` (grep `umlp-tool/src/main/resources/umlp` for `Map<`/`HashMap`);
- hand-written TS RTE that receives maps from transpiled code (`__internalMap()` or accept
  `IUMLPMap`): `commonj2ts/common/BaseClassObjectMapper.ts` (`addDeSers(getDeSerMap())`),
  `AggregatedCommandDeSer.ts`, `deser/DeSerRepository.ts`, `util/ComponentObserverManager.ts`,
  `service/websocket/ClientCommandWebsocketCommunication.ts`; charts/networkgraph use maps only
  internally (checked: no `Map` @Inputs in charts);
- hand-written frontend Java: `examples/vis-lib/frontend/src/main/java/example_charts/*` and
  `manual/*` (6 files with `Map`/`HashMap`);
- GUI TypeScript type mapping `umlp-tool/.../generator/gui/gentypescript/printer/TypeScriptTypes.java`:
  `MAP("Map", "Map", "java.util.Map")` and `HASH_MAP(...)` map to the native TS `Map`; change to
  `IUMLPMap`/`AsyncMap` (imported from `@umlp/commonj2ts`, like `GEM_LIST`), otherwise map-typed
  GUI variables/parameters (Angular side) and the transpiled hand-written page code (`AsyncMap`)
  disagree. No `.gui` in the repo uses maps today, but keep option A consistent;
- build every example frontend (`examples/*/frontend`: it-ocl-test, it-sync-protocol-test,
  loading-test, jsweet-test, flensburg-test, vis-lib, ...) and the module libraries.

### Step 4: TS GemMap RTE classes
`common/GemValueMap.ts`, `GemValueMapBuilder.ts`, `GemEntryMap.ts`, `GemEntryMapBuilder.ts`
(+ views) implementing `IUMLPMap`, porting the Java logic (step 2 of this file); async wherever
values/keys may be loaded (`get`, `containsKey`, `size`, `getID`, `getIDMap`, `getKeys`,
`keySet`/`values`/`entrySet`, `build`, `ensureKeyed`, `loadValueObjects`/`loadKeyObjects`,
`_internal_removeValueID`, the `...AndSync` hooks, `getOwner` in generated code). Add the RTE
methods called from generated code to `jsweetasync.json`. Export from `common/index.ts`.
Keep `__class`/`__interfaces` metadata (`GemBuildable`, `IUMLPMap`).

### Step 5: Transpile the generated GemMap code
Add a frontend that transpiles a model with attribute qualified associations (e.g. extend
`examples/loading-test` or `it-sync-protocol-test` models with the `QualifiedAssocs` cases).
Check the TS output of `<Owner>_<Role>GemMap` (anonymous observer classes, instanceof chains,
`getOwner`, injected `isBuildFinished`), the owner (`getRole`/`putRole`/... async), builder,
DeSer, client manager, command observers.

### Step 6: GUI (deferred by the user; do it only when asked)
Current state:
- The GUI imports roles via guidsl's `RoleSymbolDeSer` (external jar, registered in
  `UMLPTool`), which carries no qualifier: `[*]` map roles are seen as `List<T>`,
  `[0..1]`/`[1]` as plain objects, so generated GUI code would call `getRoleList()`/
  `getRoleGem().load()`/`addRole()` (`GUI2JavaClassDecorator#endVisit(ASTFieldAccessExpression)`,
  `ExpressionPrinterConfig.attr2Getter`, `addAssocListMethod`/`addAssocMethod`,
  `GUIGeneratorUtil.isDomainListType`).
- Therefore GUI CoCo `0x00032` (`umlp/generator/gui/coco/GuiNoAttributeQualifiedRoleCoCo`,
  registered in `GUIDSLCoCosForUMLP`, test `GuiNoAttributeQualifiedRoleCoCoTest`) rejects field
  accesses to these roles. It knows them from the CD `.tags` file: `TagRelated.AttributeQualifiedRoleTagVisitor`
  tags the target roles `attributeQualified`, `TagLookUp#isAttributeQualified` reads the tag.
  Verified on the GUI generation of `jsweet-test`, `it-ocl-test`, `loading-test` and the RTE libs
  (no false positives).
- Workaround for users: hand-written page code reads `getRoleMap()`/`values()` into an ordinary
  list variable and refreshes it via the owner observers (`notifyAdd<Role>`/`notifyRemove<Role>`;
  rekeying does not notify the owner).

Full support, when requested:
1. Define the semantics with the user: how a GUI addresses a map (all values as a list, lookup by
   key in expressions, key/value pairs for tables), `assoc_add` -> `putRole`, which notifications
   refresh components (add/remove; rekeying?).
2. Give the GUI the qualifier: possibly without a guidsl change by a custom `RoleSymbolDeSer`
   subclass in `umlp-tool` (the tag mechanism above already transports the information), type
   map roles as `Map<K,V>`.
3. Map cases in the GUI generator (the classes listed above), also for ordinary map-typed GUI
   variables.
4. Remove CoCo `0x00032` (or restrict it to unsupported uses).

### Step 7: Tests
- umlp-jsweet transpile/exec tests for `AsyncMap`, `IUMLPMap` calls and the async propagation.
- `examples/it-sync-protocol-test/frontend` (JVM tests via the Java client, `getClients(0)`/
  `getBackend()`), run `:examples:it-sync-protocol-test:frontend:test`.
- `examples/loading-test/frontend` Playwright E2E (`AbstractPlaywrightE2ETest`, `.gui` page +
  hand-written Java transpiled to TS, see `test_gemlist_listinterface_sync`), run
  `:examples:loading-test:frontend:test` (needs npm build and a Playwright browser).

## 5. Pitfalls already known

- Async ripples: every method calling an async method becomes async; `.size` was a property
  for native maps, `size()` becomes an awaited call.
- JSweet overloads: avoid same-named methods with different parameters in RTE Java; TS
  dispatchers use mangled names.
- The Bash tool keeps its working directory; do not stay inside `build/` folders while Gradle
  cleans them (Windows file locks). Use the quiet-gradle skill for builds.
- Once observed: the it-test test JVM exited with -1 after all tests had passed; a rerun was clean.

## 6. Open questions for the user

Resolved: GUI support is deferred, CoCo `0x00032` built (step 6).

Existing frontend Java only uses `get`/`put`/`remove`, `entrySet()` iteration (`for` and
`.forEach`), `getKey`/`getValue`, `keySet().forEach` and `values()`; no `compute`, `merge`,
`getOrDefault` or `Entry.setValue` anywhere (checked 2026-10-22).

1. Which `java.util.Map` methods `IUMLPMap` supports. Recommendation given to the user (not yet
   confirmed): core set `get`, `put`, `remove`, `containsKey`, `containsValue`, `size`, `isEmpty`,
   `clear`, `putAll`, `keySet`, `values`, `entrySet`, `forEach`, `getOrDefault`, `putIfAbsent`;
   unsupported methods rejected at transpile time via `JSweetCoCoChecker`
   (`umlp-jsweet/.../extension/cocos/`). Function-based default methods (`compute`, `merge`, ...)
   would have to await possibly async lambdas, and for GemMaps a value with a different derived
   key is rejected anyway.
2. `Map.Entry`: recommendation (not yet confirmed): keep tuples `[K, V]` (compatible with existing
   code and native `Map.entries()`), reject `setValue` at transpile time; an own entry type with
   write-through `setValue` only if needed later.
