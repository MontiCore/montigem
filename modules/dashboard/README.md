# Dashboard module

A module providing a configurable dashboard: views consisting of cells (bar/line/pie/box/scatter/gauge charts,
network graphs, filterable tables, ...), arranged via [gridstack](https://gridstackjs.com/).
The data of a cell is described by a small script, which is interpreted in the backend (see [lang](lang)).

The example application [examples/dashboard](../../examples/dashboard) uses this module.
The module originates from a master thesis, which describes its design in more detail.

## Structure
- `models/DashboardModule.umlp`: the domain of the dashboard (`DashboardModuleConfig`, `ViewConfig`, `CellConfig`, ...)
- `lang`: the MontiCore language and interpreter of the data scripts
- `backend`: hand-written backend code, e.g., the `Interpret*` commands
- `frontend/dashboard`: the GUI components (package `dashboardcomponents`, npm library `@montigem/dashboard`)

## Usage
- Add the module to the build (the example application uses the corresponding project dependencies instead):
  ```groovy
  // backend/build.gradle
  dependencies {
    montigemModule "umlp.modules:dashboard:$mg3_version"
    montigemModuleUMLPModels "umlp.modules:dashboard:$mg3_version"
  }
  tasks.named("umlp2java") {
    modelDir.from(configurations.montigemModuleUMLPModels)
  }

  // frontend/build.gradle
  dependencies {
    montigemModuleUMLPModels "umlp.modules:dashboard:$mg3_version"
    montigemModuleUMLPSymbols "umlp.modules:dashboard:$mg3_version"
    montigemModuleGUISymbols "umlp.modules:dashboard:$mg3_version"
    montigemModuleImportMap "umlp.modules:dashboard:$mg3_version"
  }
  tasks.named("gui2java") {
    symbolPath.from(configurations.montigemModuleUMLPSymbols)
  }
  ```
- Extend the domain of the module: `import DashboardModule.*;` and `<<extends="DashboardModule">>`
- Show the dashboard using the `dashboardcomponents.Dashboard` component, e.g.,
  ```
  page Home (List<DashboardModuleConfig> configs) {
    @Dashboard(config = getConfig(configs));
  }
  ```
- Allow the data scripts to access your domain (backend, e.g., during startup):
  Attention: You have to add other ClassDiagrams as well if you extend / use multiple CDs / modules 
  ```java
  InterpreterWithErrorHandling.addDomain("mydomain", MyDomainManager.class);
  ```
- The frontend requires the npm packages `@montigem/dashboard`, `gridstack` (its styles are included by the module),
  and `ngx-monaco-editor-v2` (`MonacoEditorModule.forRoot(...)` within the `AppGemModuleConfig.ts`),
  e.g., via `config/package.override.json` (`"@montigem/dashboard": "g-${mg_version}"`),
  and the images used by the components in `assets/images`: `montigem-diamond.png`, `class-diagram.png`,
  `preset1.png`, and `empty-preset.png` (cf. `examples/dashboard/frontend/config`).
  The example application instead compiles the sources of `@montigem/dashboard` together with the application
  (see `examples/dashboard/frontend/build.gradle` and `config/tsconfig*.override.json`),
  such that it uses the current module without publishing the library first

## Adding a visualization type
- `models/DashboardModule.umlp`: add the type to `VizType`, add an `interpret*` method returning the data type of the
  chart to `DashboardModuleApp`, and add a `*ExtraData` class (referenced by `ExtraData`) for the further parameters
  of the chart component
- `lang`: make the data classes of the chart available to the data scripts
  (`addAvailableClassesToGlobalScope()` in `MGScriptInterpreter.java`)
- `backend`: implement the `Interpret*` command of the new type (cf. `InterpretLineChart`)
- `frontend/dashboard/index.ts`: export the generated `Interpret*` command (and its `DeSer` and `TOP`)
- `frontend/dashboard`:
  - `CellDialog.java`: call the new command when validating the data script, initialize and check the extra data;
    add an example data script for the new type (also in `MainCellOptions.java`)
  - `CellDisplay.gui`: display the chart; `ExtraCellOptions.gui`: show the options component for its extra data

## Filterable tables
A `FILTERABLE_TABLE` cell is computed from separate scripts (`FilterableTableInterpreter` in the backend):
the data script of the cell returns typed columns (`GemFilterableTableData`), and each column can have a filter and a
render function (`ColumnScripts` of the `TableExtraData`). Both are expressions over the value `v` of their column,
e.g., `>= 3` (short for `v >= 3`) and `Math.round(v) + " %"`, from which the backend builds a script returning a lambda,
e.g., `return (Double v) -> v >= 3;`; a script containing `return` is used as it is. The lambda is applied to every value;
the table shows only the rows accepted by all filters. `GemFilterableTable` sorts the rows by the values (not the rendered
texts) of the column whose header was clicked, and edits the filter of a column in its header (`changeFilter`, stored by
`CellDisplay`); the render functions are edited in the cell dialog.
The interpreter can return lambdas, but not pass them to Java code or store them in objects, so the functions of the
columns cannot be part of the data script.

## Notes for maintainers
- The error messages of the interpreter can be empty or misleading, so debugging data scripts can be difficult.
- The interpreter sets up global state (the global scope of the `MGScriptMill`, the findings of the `Log`),
  so `InterpreterWithErrorHandling` interprets one script at a time; interpret scripts only via this class.
- gridstack widgets are plain HTML elements and do not update the `CellConfig`s when they are moved, resized,
  added, or removed. `GridstackComp.ts` therefore wraps each `Cell` in a `CellWrapper` and synchronizes the widgets
  and the cells of the `ViewConfig` by hand (`reactOnViewInputChange()`); keep this in mind when changing it.

## AI Assistant tools

The backend includes LangChain4j tool implementations that operate on the module's dashboard model:

- `LayoutTools(view)` reads and applies the current view's complete layout, validating all placements
  before changing geometry.
- `CellTools(view)` validates scripts and creates, reads, edits, duplicates, removes, and previews cells,
  including their chart options and filterable-table column scripts.
- `DomainModelTools(classLoader, resource)` exposes an application's packaged UMLP model. The application
  supplies the resource path, for example `models/Inventory.umlp`. A constructor taking just the resource
  path uses the tool class's classloader.

Pass these objects to a LangChain4j AI service or to `chatmodule.ChatService.send(history, tools...)`.
The dashboard module does not depend on the chat module or construct a model provider.
`CellOptions` is the internal chart-option conversion and validation helper used by `CellTools`.

The matching instruction resources are packaged as `skills/dashboard-operations/SKILL.md` and
`skills/dashboard-chart-data/SKILL.md`. Add them to the application's instruction manifest.
Applications still register their domain managers with `InterpreterWithErrorHandling.addDomain(...)`
and package the model resource supplied to `DomainModelTools`.
