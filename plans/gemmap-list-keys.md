# Plan: list-valued keys (and per-key lists) for GemMaps

Instructions for a future conversation. Read this file completely, then
check `git log` on branch `gemmap-attribute-qualifiers` (or wherever it was merged)
for changes made after this plan was written (2026-10-22). Line numbers are
not given on purpose; search for the named classes and methods.

## 1. Background: what exists

An **attribute qualified association** `Owner [[attr]] -> (role) Target [card]`
is generated as a **GemMap** of `Owner`, keyed by the value of `Target.attr`.
The key is *derived* from the target and never persisted; the association table
only stores `(owner id, target id)` like any to-many association.

| Key attribute type | Map class | Stored as |
|---|---|---|
| primitive or `String` (MontiCore typecheck primitives) | `GemValueMap<K, V>` | `K -> value gemId` |
| domain class (becomes a to-one association of `Target` via `UMLAttrToAssocTrafo`) | `GemEntryMap<K, V>` | `key gemId -> value gemId` |

Current restrictions (all intentional):
- **Keys are unique**: one value per key. Target cardinality `[1]` gives
  `V getRole(K)`, `[0..1]`/`[*]` give `Optional<V> getRole(K)`.
- **Only unidirectional** qualified associations (CoCo 0xCDC6A).
- **Type qualifiers** (`[String]`) are ignored and still generated as GemLists.
- **List-valued or optional key attributes are rejected** (CoCo 0xCDC68):
  `List<String> tags`, `List<Department> departments`, `Optional<...>`.

### Where the code lives

RTE (backend, `umlp-rte/backend/src/main/java/umlp/backendrte/common/`):
- `GemValueMap`, `GemValueMapBuilder`, `GemEntryMap`, `GemEntryMapBuilder`.
  Key concepts: `idMap` (key -> value id), `unkeyedValueIDs` (values loaded by id
  whose key is derived on first access, `ensureKeyed()` loads them in one batch),
  `deriveKey`/`deriveKeyID`, `keyLoadedValue`, `_internal_putValue`,
  `_internal_rekey` + `moveEntry` + `lookupID`, `_internal_removeValueID`,
  hooks `onValueKeyed` (start observing a value) and `onValueRemoved`
  (stop observing; called on every path a value leaves the map).
- `GemMapViews`: live, weakly consistent `keySet()`/`values()`/`entrySet()` of both maps
  (removal through views is synchronized via `_internal_removeValueIDAndSync`, adding
  is unsupported, iterators load values/key objects in one batch). A multimap must keep
  these semantics; `getRoleMap()` returns the live map, modifications through it are
  synchronized via the owner (`isKeyDerived`, `_internal_putValueAndSync`).
- Builders keep unbuilt builders in `builderElemStorage` (`IDOrBuilder`);
  builders have a unique gemId from construction (see `GemIdentifiable#getGemId`).
- Tests: `umlp-rte/backend/src/test/java/umlp/backendrte/GemMapBuilderTest.java`.

Generator (`umlp-tool/src/main/java/umlp/`):
- CoCos: `_cocos/UMLPAssociationQualifierByAttributeName` (`isValueKey`,
  `isDomainKey`), `_cocos/UMLPAssociationQualifierUnidirectional`.
- `generator/domain/decorator/GenService`: `isMap`, `getQualifierAttributeName`
  (the qualifier stands on the *owner's* side, `getOtherSide()` of the target side),
  `isMultiValued` (list or map; used by DAO, DeSer, observers, add/remove commands),
  `isGemMap`/`isGemEntryMap` (field type checks).
- `generator/domain/decorator/QualifierKey`: **the single place deciding the key
  kind** (value vs. entry), key type, getter, declaring type, observer parameter type.
  A new kind belongs here.
- `generator/domain/decorator/GemMapAssocDecorator` + templates
  `umlp-tool/src/main/resources/umlp/gemmap/` (`DeriveKey`, `KeyObserver`,
  `OnValueKeyed`, `OnValueRemoved`, `LoadObjects`, `Constructor`):
  generates `<Owner>_<Role>GemMap`. Values are observed with the observer of their
  most specific type known in the CD (subclasses notify only their own observers).
- Owner API: `DomainTypeDecorator#getAttributesForAssociations` (field type,
  stereotype `qualifiedOne` for `[1]`), `methods/AssocMapAccessorDecorator`,
  `methods/AssocMapMutatorDecorator`, templates `umlp/methods/gemmap/`.
- Builder: `BuilderDecorator` + `umlp/builder/AssocMapPut.ftl`, `AssocGemMapIds.ftl`.

End-to-end tests: `examples/it-test/src/models/QualifiedAssocs.umlp`,
`examples/it-test/src/test/java/qualifiedassocs/QualifiedAssocsDbTest.java`
(SQL and Neo4j suites, backend + simulated client).
Run: `:examples:it-test:test`, `:umlp-rte:backend:test`, umlp-tool `:test`
(use the quiet-gradle skill).

## 2. Goal

Support two related extensions, sharing one storage shape:

1. **List-valued keys**: `[[tags]]` with `List<String> tags`, or
   `[[departments]]` with `List<Department> departments`. Every element is a key,
   so one target is found under several keys.
2. **Several values per key** (per-key lists): needed by 1. as soon as two
   targets share an element, and wanted anyway for target cardinality `[*]`
   (originally requested semantics: `[1]` -> one value, `[0..1]` -> optional,
   `[*]` -> list per key; postponed in favour of unique keys).

Decide with the user first (open questions, section 6), then implement.

## 3. Design

### Storage: a multimap with reverse index

Replace (or add next to) `key -> value id` with:
- `key -> ordered set of value ids` (multimap), and
- `value id -> set of keys` (reverse index; needed for rekeying, removal and
  for list keys, where one value has several keys).

Unique-key maps can stay on the current classes, or become the multimap with a
"at most one value per key" check. Prefer **one** storage shape if the RTE stays
readable; the unique case then only differs in the generated owner API and in
error reporting on collisions (today: `0xA7303`/`0xA7304`, keep old key).

Keys remain derived and unpersisted, so **no DB or DeSer change** is needed:
loading still adds value ids via `_internal_addValueIDs` and keys are derived lazily.

### Key derivation

`deriveKey(V)` returns one key today. Generalize to "all keys of a value":
- single attribute: one key (as today),
- list attribute: the list's elements (value map), or the gemIds of the referenced
  objects (entry map; for a `[*]` association use the GemList's ids without loading).

`keyLoadedValue` / `_internal_putValue` store the value under every key and fill the
reverse index.

### Rekeying

Today the rekey observer overrides `notifySet<Attr>(declaringType o, T old, T new)`.
For list keys it additionally needs the element notifications, generated by
`ObserverDecorator` for list attributes and list associations:
- `notifyAdd<Attr>(<Type> o, <elementType> arg, int indexInList)` -> add key `arg`
- `notifyRemove<Attr>(<Type> o, <elementType> arg, int idx)` -> remove key `arg`
- `notifySet<Attr>(...)` (whole list, or element at index for associations) ->
  diff old/new keys
- for a list association the element type is `long` (target gemId).

Use the reverse index to find the value's current keys; do not rely on the
notification's `old` value alone.

### Owner API (generated)

Keep the unique-key API unchanged. For multi-valued maps (list keys and/or `[*]`
per-key lists):
- `List<V> getRole(K key)` (live view or freshly loaded list; decide, see 6.)
- `getRoleMap()`, `containsKeyRole(K)`, `containsRole(V)`, `sizeRoles()`
  (number of values, not keys - document)
- `putRole(V)` / `addRole(V)` (keys derived), `removeRole(V)` (by value),
  possibly `removeRole(K)` (all values under a key)
- observer notifications stay per value (`notifyAdd/RemoveRole(owner, valueId, -1)`),
  so DB, sync commands and AllObserver need no change.

## 4. Implementation steps

Commit each step separately (the user wants that), on a feature branch.

1. **RTE storage** (Java): multimap + reverse index in `GemValueMap`/`GemEntryMap`
   and builders; multi-key derivation hook; rekey for added/removed/replaced keys;
   keep `onValueKeyed`/`onValueRemoved` semantics (a value leaves the map only when
   its last key is gone). Extend `GemMapBuilderTest` (several keys per value,
   several values per key, rekey on element add/remove, builder before/after build).
2. **CoCo**: allow `List<primitive|String>` and to-many domain associations as
   qualifier attributes (`UMLPAssociationQualifierByAttributeName`); keep rejecting
   `Optional` and nested collections. Update/extend the CoCo tests and test models
   in `umlp-tool/src/test/resources/coco(in)valid/`.
3. **`QualifierKey`**: new key kind(s) (e.g. `isMultiKey`, element type, element
   observer method names/types). Note: CoCos run *before* `UMLAttrToAssocTrafo`,
   the generator *after* it, so a `List<Domain>` attribute is a `[*]` association
   role of the target in the generator.
4. **GemMap class**: `DeriveKey` returns all keys; `KeyObserver` overrides the
   element notifications as well; check the subclass observer chain still works.
5. **Owner API + builder**: new templates for the multi-valued API; select them via
   a stereotype on the map field (like `qualifiedOne`).
6. **Frontend**: if the TS RTE classes for GemMaps exist by then (planned
   separately: hand-written `GemValueMap.ts`, ... in
   `umlp-rte/frontend/commonj2ts/.../common/`, `jsweetasync.json` entries, JSweet
   Map type mapping excluded for GemMaps), port the RTE change there too.
7. **End-to-end tests**: extend `QualifiedAssocs.umlp` (e.g. `Person { List<String> tags; }`,
   `association Directory [[tags]] (tagDirectory) -> (byTag) Person [*];` and a
   domain list key) and `QualifiedAssocsDbTest` (put/get, persistence, client sync,
   rekey on element add/remove and on sync, values leaving the map).

## 5. Pitfalls already known

- Role names: in CD4A the target's role goes **after** the arrow
  (`Directory [[login]] (loginDirectory) -> (byLogin) Person`); unnamed roles default
  to the class name and collide quickly.
- Subclasses regenerate setters of inherited attributes and notify only their own
  observer list; observe values with their most specific type (already handled in
  `GemMapAssocDecorator#getObservedTypes`). Subclasses from *extending* CDs are not
  covered.
- Deleting an object does not update objects referencing it in memory (framework-wide);
  keys of deleted key objects stay in the map.
- The Bash tool keeps its working directory; do not stay inside `build/` folders
  while Gradle cleans them (Windows file locks).

## 6. Open questions for the user

1. Per-key lists also for `[*]` with single-valued keys, or only where list keys
   force them?
2. `getRole(K)` for multi-valued maps: live list (needs a live per-key view, also in
   TS) or a loaded list? (The user requires live updatability for `getRoleMap()`.)
3. One multimap storage for everything, or keep unique-key maps as they are and add
   new classes (e.g. `GemValueMultiMap`)?
4. `sizeRoles()`: number of values or number of keys?
