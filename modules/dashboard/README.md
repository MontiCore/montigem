# Dashboard module

A module providing a configurable dashboard: views consisting of cells (bar/line/pie/box/scatter/gauge charts,
network graphs, ...), arranged via [gridstack](https://gridstackjs.com/).
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
- `frontend/dashboard`:
  - `CellDialog.java`: call the new command when validating the data script, initialize and check the extra data;
    add an example data script for the new type (also in `MainCellOptions.java`)
  - `CellDisplay.gui`: display the chart; `ExtraCellOptions.gui`: show the options component for its extra data

## Notes for maintainers
- The error messages of the interpreter can be empty or misleading, so debugging data scripts can be difficult.
- gridstack widgets are plain HTML elements and do not update the `CellConfig`s when they are moved, resized,
  added, or removed. `GridstackComp.ts` therefore wraps each `Cell` in a `CellWrapper` and synchronizes the widgets
  and the cells of the `ViewConfig` by hand (`reactOnViewInputChange()`); keep this in mind when changing it.
