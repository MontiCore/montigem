---
name: dashboard-chart-data
description: Write or repair interpreter models stored as dashboard cell data strings, producing data for a MontiGem chart, graph, or (filterable) table from application domain data, and the filter and render scripts of the columns of a filterable table.
---

# Dashboard chart data interpreter models

Use this skill when creating or editing the script stored in a dashboard cell's `data` field. The script runs in the backend and must return the data object expected by that cell's visualization type.

## Access live application data

The class diagram describes the schema, not the current records. Read records from the generated **diagram-level manager's static methods**, then navigate the returned objects with instance getters:

- If the diagram is `umlp Inventory` and declares `class Item`, the entry point is `InventoryManager.getItemList()`.
- The manager name comes from the **diagram name**, not the entity name: there is no generated `ItemManager` for that class.
- Call the manager directly. Do not call `getInstance()` or `getSingleton()`. Never construct a manager with `InventoryManager()` or `new InventoryManager()`, or assign a manager to a variable. No manager instance is needed to read data.
- Once you have an item, call its getters, e.g., `item.getName()`. For collections, iterate `item.get<DeclaredRole>List()` in the direction allowed by the diagram.

For a hypothetical `Inventory` diagram whose `Item` declares `String name` and `int quantity`, a complete live-data table is:

```java
List<String> names = [];
List<Double> quantities = [];
for (Item item : InventoryManager.getItemList()) {
  names.add(item.getName());
  quantities.add(1.0 * item.getQuantity());
}
List<GemFilterableTableColumn> columns = [
  GemFilterableTableColumnBuilder().header("Name").texts(names).build().get(),
  GemFilterableTableColumnBuilder().header("Quantity").numbers(quantities).build().get()
];
return GemFilterableTableDataBuilder().columns(columns).build().get();
```

Substitute the diagram, entity, and field names returned by `getDomainModel`. `Inventory` and `Item` are illustrative, not types guaranteed to exist in the application. The manager and getter conventions are generated framework APIs; they need not be explicitly declared as operations in the UMLP text.

A request for live data means the stored script reads current domain objects whenever it executes. Keep the reads in the script; do not replace them with a snapshot of tool results or literal sample rows. Empty collections produce an empty chart or table; do not fabricate replacement records. When a row combines an object with its parent (for example, an item and its container), start at a suitable manager collection and follow the declared associations. Append all column values inside the same innermost loop so each row remains aligned.

## Workflow

1. Identify the cell visualization (`BAR`, `LINE`, `PIE`, `SCATTER`, `BOX`, `SPEED`, `NETWORK`, or `FILTERABLE_TABLE`) and match the returned object to its `Interpret*` command and chart type.
2. Call `getDomainModel` to read the application's UMLP class diagram before writing a data-backed script. Use its diagram name, classes, attributes, association roles, and enum literals as the source of truth. If loading fails, report the failure; do not guess the schema or substitute a remembered example domain.
3. Follow the language subset and available types listed below. The model language is a Java-like MontiCore subset, not unrestricted Java. A type or method must be both in the supported language and visible to the interpreter.
4. Build the visualization payload with the appropriate chart builders, then return the completed object. Keep collection element types and builder arguments consistent with the chart API.
5. Call `checkInterpreterCode(viz, data)` to execute the script and check its return type. If it reports an error, change the failing expression and check the revised script again. Do not resend an unchanged failed script or use `updateCell` to bypass this check. Errors can be poorly localized, so first check spelling, types, builder arguments, and whether every used class or method is available.
6. Before creating a cell, call `getCurrentLayout` to find available space. Call `createCell(name, info, viz, data)` with the successfully checked script and a concise info on the intent of the cell (see `dashboard-operations`). It validates again and initially appends the cell below the existing tiles (6 columns wide, 4 rows high). Then follow **Place new cells in available space** in the `dashboard-operations` skill: reuse the most recently cleared space that fits, otherwise choose the topmost, leftmost empty rectangle. Refresh the layout and call `setCurrentLayout` with every tile, including the returned new cell ID, preserving existing tiles. Complete placement before reporting success; the initial appended position is only a fallback when no earlier space fits. Follow any explicit placement requested by the user.
7. For a `FILTERABLE_TABLE`, add the filters and render functions of its columns afterwards with `setCellOptions` (see [Filterable tables](#filterable-tables)).

Validation and preview tools execute real interpreter code. Scripts must only read the business domain and construct visualization payloads; do not call domain setters, domain builders, association mutators, deletion methods, or chat operations. Execution is not a sandbox or a rollback transaction. The creation tools support the eight visualizations listed above; `TILE` does not have an interpreter command.

## Return types and payload shape

| Cell visualization | Return type | Payload structure |
| --- | --- | --- |
| `BAR` | `GemBarChartData` | `entries` (`List<GemBarChartEntry>`) plus category `labels`; each entry holds a numeric data list and optional label. |
| `LINE` | `GemLineChartData` | `entries` (`List<GemLineChartEntry>`) plus shared `labels`; each entry has a label and numeric data list aligned to those labels. |
| `PIE` | `GemPieChartData` | `entries` (`List<GemPieChartEntry>`), each with a numeric value and label. |
| `SCATTER` | `GemScatterPlotData` | `sets` (`List<GemScatterPlotSet>`), each with a label and `List<GemScatterPlotPoint>`. Points carry x/y values, a label, and optional radius. |
| `BOX` | `GemBoxPlotData` | `entries` (`List<GemBoxPlotEntry>`), each with its group, sample points, and a `GemBoxPlotBox` summary (median, quartiles, extremes). |
| `SPEED` | `GemGaugeChartData` | `entries` (`List<GemGaugeChartEntry>`), each with type, name, and value. |
| `NETWORK` | `Graph` | `nodes` (`List<GraphNode>`) and `edges` (`List<GraphEdge>`); edge source and target IDs must match node IDs. |
| `FILTERABLE_TABLE` | `GemFilterableTableData` | `columns` (`List<GemFilterableTableColumn>`), each with a unique `header` and exactly one of `numbers` (`List<Double>`), `texts` (`List<String>`), `booleans` (`List<Boolean>`), `dates` (`List<LocalDate>`), or `dateTimes` (`List<LocalDateTime>`): the values of the column from top to bottom. All columns have the same number of values. Do not set `rendered`; it is computed from the render scripts. |

These commands enforce the returned type at runtime. Returning a different object, or omitting the final returned value, causes interpretation to fail.

## Language and construction patterns

- A script is a sequence of imports (if needed) and statements, ending in a `return` of the visualization data object. It can declare typed local variables, use collection literals such as `List<Double> values = [1.0, 2.0];`, mutate lists with methods such as `add`, and use `for`, enhanced `for`, `if`, `while`, `switch`, and `return` statements.
- Supported expression families include literals, arithmetic and boolean operators, member access and method calls, assignments, lambdas, and set expressions. Use only syntax accepted by these families; this is not a complete Java compiler (for example, do not assume arbitrary Java APIs, reflection, or every Java statement is supported).
- `java.lang`, `java.util`, the chart and table packages, `dashboardmodule`, and application domain packages registered with `InterpreterWithErrorHandling.addDomain(...)` are imported by default. `Optional` and `List` are commonly used. A visible import alone does not guarantee an API is callable: its type must also be reachable through the interpreter's class path and supported by interpretation.
- Chart builders use fluent calls followed by `.build().get()`. See the chart examples below for complete models.

- Use the chart API names below and derive domain accessors from the class diagram. Optional builder fields commonly take `Optional.of(value)` or `Optional.empty()`.
- For data-backed charts, derive labels and values from the same source and preserve their order. For line charts, every series should align with the shared labels. For scatter plots and network graphs, construct all nested objects with the expected types and valid references.
- Keep chart-specific presentation options (such as axis limits or bar stacking) in the cell's extra data unless the chart payload API explicitly carries them.
- Tables use typed columns. Store numeric values in `numbers`, strings in `texts`, and booleans in `booleans`. Each column has exactly one value list, and all columns must have the same length and row order. Use column render scripts to format values for display.
- Convert enums to strings with `toString()`, e.g., `record.getStatus().toString()`. Concatenating a string with an enum or object (`"" + record.getStatus()`) is rejected by the type check with `0xB0163 Operator '+' not applicable`.
- Integer division truncates (`ageSum / count`); guard divisions against empty lists.

## Complete examples by visualization

Each example is a complete cell data script and returns the expected type. The chart examples use small fixed datasets so the payload structure is easy to adapt; the table example below also uses fixed values. These demonstrate payload construction only; use the manager-based pattern above when the user requests application data.

### Bar

```java
List<String> labels = [""];
List<Integer> values = [12];
GemBarChartEntry entry = GemBarChartEntryBuilder()
    .data(values)
    .label(Optional.of("Completed"))
    .build().get();
List<GemBarChartEntry> entries = [entry];
return GemBarChartDataBuilder().entries(entries).labels(labels).build().get();
```

### Line

```java
List<String> labels = ["Week 1", "Week 2", "Week 3"];
List<Double> values = [4.0, 7.0, 5.0];
GemLineChartEntry entry = GemLineChartEntryBuilder()
    .label("Issues closed")
    .data(values)
    .build().get();
List<GemLineChartEntry> entries = [entry];
return GemLineChartDataBuilder().entries(entries).labels(labels).build().get();
```

### Pie

```java
GemPieChartEntry entry1 = GemPieChartEntryBuilder().value(20).label("Women").build().get();
GemPieChartEntry entry2 = GemPieChartEntryBuilder().value(30).label("Men").build().get();
List<GemPieChartEntry> entries = [entry1, entry2];
return GemPieChartDataBuilder().entries(entries).build().get();
```

### Scatter

```java
GemScatterPlotPoint point1 = GemScatterPlotPointBuilder()
    .x(1.0).y(4.0).label("Week 1").pointRadius(Optional.of(8)).build().get();
GemScatterPlotPoint point2 = GemScatterPlotPointBuilder()
    .x(2.0).y(6.0).label("Week 2").pointRadius(Optional.of(8)).build().get();
List<GemScatterPlotPoint> points = [point1, point2];
GemScatterPlotSet set = GemScatterPlotSetBuilder().label("Issues").points(points).build().get();
List<GemScatterPlotSet> sets = [set];
return GemScatterPlotDataBuilder().sets(sets).build().get();
```

### Box

```java
List<Double> points = [1.0, 2.0, 3.0, 4.0, 5.0];
GemBoxPlotBox box = GemBoxPlotBoxBuilder()
    .median(3.0).lowerQuantile(2.0).upperQuantile(4.0)
    .lowerExtreme(1.0).upperExtreme(5.0).build().get();
GemBoxPlotEntry entry = GemBoxPlotEntryBuilder()
    .group("Team A").color(Optional.of("blue")).points(points).box(box).build().get();
List<GemBoxPlotEntry> entries = [entry];
return GemBoxPlotDataBuilder().entries(entries).build().get();
```

### Gauge (`SPEED`)

```java
GemGaugeChartEntry entry = GemGaugeChartEntryBuilder()
    .type("#friends").name("Average score").value(8).build().get();
List<GemGaugeChartEntry> entries = [entry];
return GemGaugeChartDataBuilder().entries(entries).build().get();
```

### Network

```java
GraphNode node1 = GraphNodeBuilder().id("1").label("Frontend")
    .fixed(Optional.of(true)).x(Optional.of(1.0)).y(Optional.of(1.0))
    .size(Optional.of(40)).build().get();
GraphNode node2 = GraphNodeBuilder().id("2").label("Backend")
    .fixed(Optional.of(true)).x(Optional.of(3.0)).y(Optional.of(1.0))
    .size(Optional.of(40)).build().get();
List<GraphNode> nodes = [node1, node2];
GraphEdge edge = GraphEdgeBuilder().id("edge-1").source("1").target("2")
    .size(Optional.of(5)).build().get();
List<GraphEdge> edges = [edge];
return GraphBuilder().nodes(nodes).edges(edges).build().get();
```

### Filterable table

```java
List<String> names = ["Alpha", "Beta"];
List<Double> scores = [12.0, 24.0];
List<Boolean> active = [true, false];
List<LocalDate> dates = [LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)];
List<LocalDateTime> times = [LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 2, 14, 30)];
List<GemFilterableTableColumn> columns = [
  GemFilterableTableColumnBuilder().header("Name").texts(names).build().get(),
  GemFilterableTableColumnBuilder().header("Score").numbers(scores).build().get(),
  GemFilterableTableColumnBuilder().header("Active").booleans(active).build().get(),
  GemFilterableTableColumnBuilder().header("Date").dates(dates).build().get(),
  GemFilterableTableColumnBuilder().header("Time").dateTimes(times).build().get()
];
return GemFilterableTableDataBuilder().columns(columns).build().get();
```

## Filterable tables

The data script of a `FILTERABLE_TABLE` only returns the typed values of its columns. Users sort the rows by clicking a header; numbers and booleans are compared as values, texts alphabetically, and dates and date-times chronologically. Users can also set the filter of a column with the filter button of its header. Each column can have two optional, separate expressions, which are stored in the cell options and evaluated after the data script:

- A **filter** is a boolean expression over the value `v` of the column. Only rows whose values are accepted by the filters of all columns are displayed. A filter may omit `v` before an operator or a method call: `>= 30` means `v >= 30`, `.startsWith("A")` means `v.startsWith("A")`. On texts, `== "Alice"` and `!= "Alice"` compare the text (they become `v.equals("Alice")`); inside larger expressions, use `v.equals(...)`, because `==` on texts compares references.
- A **render** function is an expression computing the displayed text of the value `v`, e.g., `Math.round(v) + " years"` or `v ? "yes" : "no"`. Without it, numbers are displayed without a trailing `.0` for whole numbers, texts as they are, booleans as `true` or `false`, and dates and date-times in ISO format (`2026-01-01`, `2026-01-01T09:00`).

`v` is a `Double` for `numbers`, a `String` for `texts`, a `Boolean` for `booleans`, a `LocalDate` for `dates`, and a `LocalDateTime` for `dateTimes`. The `java.time` types are imported by default. Date filters can use `<`, `<=`, `==`, `!=`, `>=`, or `>` followed by a quoted ISO value, e.g., `>= "2026-01-01"` or `< "2026-01-02T14:30"`. A typed value also works: `>= LocalDate.of(2026, 1, 1)`. These comparisons use `compareTo`; for compound date predicates, use methods such as `v.isAfter(LocalDate.of(2026, 1, 1)) && v.getYear() == 2026`.

An expression is a single expression (no statements), so use the conditional operator instead of `if`, and `&&`/`||` to combine conditions (`v >= 30 && v < 40`). For more, write a complete script that returns a lambda with one explicitly typed parameter; it is used as it is whenever it contains `return`, and statements before the `return` can prepare data, e.g., `List<String> chosen = ["SENIOR", "MANAGER"]; return (String v) -> chosen.contains(v);`. Lambdas cannot be stored in variables of a function type, passed to Java methods, or put into lists. For example:

```json
{"columns": [
  {"column": "Age", "filter": ">= 30", "render": "Math.round(v) + \" years\""},
  {"column": "Role", "filter": "List<String> chosen = [\"SENIOR\", \"MANAGER\"]; return (String v) -> chosen.contains(v);"},
  {"column": "International", "render": "v ? \"yes\" : \"no\""}
]}
```

Pass this object to `setCellOptions(cellId, optionsJson)`. `columns` replaces all previous column scripts, including filters set by users, so read the current ones with `getCell` first and keep those that should stay; omit `filter` or `render` (or leave it empty) to keep a column unfiltered or rendered by default. `column` must equal a header of the data script. The tool executes the data script together with the new scripts and rejects them, without changes, if an expression or script fails or a filter does not return a boolean. Use `previewCell` to inspect the filtered and rendered rows.

## Reading the application domain

`getDomainModel` returns the application's UMLP model text. Read the diagram declaration and any package, imports, attributes, associations, and enums before choosing data to visualize. Imported module types are separate from the application's business data. Do not assume that a type mentioned in an import is declared by this diagram.

The generated Java package is the lowercased qualified diagram name, and the manager is `<DiagramName>Manager`. Read all current objects of class `Entity` with the static call `<DiagramName>Manager.getEntityList()`; there is one manager per diagram, not one per entity. Scalar attributes use `get<Field>()`, boolean attributes use `is<Field>()`, and list attributes and association roles with [*] or [+] cardinality use `get<FieldOrRole>List()`. Association roles with [0..1] (optional) or [1] (mandatory) cardinality use `get<Role>()`. Associations are only usable along the defined navigation directions, e.g., association [0..1] Person -> Car [*] allows the navigation `person.getCarList()` but not `car.getPerson()`.
Preserve spelling and capitalization from the model. Optional values need `isPresent()` and `get()` checks before use. Use the generated accessors described here; do not invent additional business operations.

Traverse the collections and associations actually declared in the model to compute chart labels and values. Use chart builders for visualization payloads; do not create or modify business objects. Validate the resulting script with `checkInterpreterCode` even when its names follow these conventions.

## Repair failed data access

If validation reports an unknown method or qualified name, inspect the receiver and the method in the failing expression. For `SomeManager.getInstance()`, remove the instance lookup and call the diagram manager's static collection accessor. For `EntityManager.getEntityList()`, replace `EntityManager` with the manager derived from the diagram declaration. Adding a package qualifier does not fix a nonexistent method. Re-read the diagram only if the diagram name, class, field, or navigation direction is still unclear.

Validate the corrected script with `checkInterpreterCode`. On success, pass that exact script to `createCell` or `updateCell`, then use `previewCell` to inspect its computed data. Validation alone does not save a script. If validation still fails, use the new error to make a specific correction; do not cycle through manager constructors and singleton APIs. Report an unresolved failure without claiming the cell was updated.
