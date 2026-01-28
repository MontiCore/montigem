# Dashboard module

A module providing a configurable dashboard: views consisting of cells (bar/line/pie/box/scatter/gauge charts,
network graphs, ...), arranged via [gridstack](https://gridstackjs.com/).
The data of a cell is described by a small script, which is interpreted in the backend (see [lang](lang)).

The example application [examples/dashboard](../../examples/dashboard) uses this module.
Further documentation can be found in [docs](docs).

## Structure
- `models/DashboardModule.umlp`: the domain of the dashboard (`DashboardModuleConfig`, `ViewConfig`, `CellConfig`, ...)
- `lang`: the MontiCore language and interpreter of the data scripts
- `backend`: hand-written backend code, e.g., the `Interpret*` commands
- `frontend/dashboard`: the GUI components (package `dashboardcomponents`, npm library `@montigem/dashboard`)

## Usage
- Extend the domain of the module: `import DashboardModule.*;` and `<<extends="DashboardModule">>`
- Show the dashboard using the `dashboardcomponents.Dashboard` component, e.g.,
  ```
  page Home (List<DashboardModuleConfig> configs) {
    @Dashboard(config = getConfig(configs));
  }
  ```
- Allow the data scripts to access your domain (backend, e.g., during startup):
  ```java
  InterpreterWithErrorHandling.addDomain("mydomain", MyDomainManager.class);
  ```
- The frontend requires the npm packages `gridstack` and `ngx-monaco-editor-v2`
  (`MonacoEditorModule.forRoot(...)` within the `AppGemModuleConfig.ts`),
  and the images used by the components in `assets/images` (cf. `examples/dashboard/frontend/config`)

## Known limitations
The generated client code of an application using this module imports the module domain via
`dashboardmodule/...` instead of `@montigem/dashboard`.
The example application thus compiles the library sources together with the application
(see `examples/dashboard/frontend/build.gradle`, `config/tsconfig*.override.json`,
and the `DashboardImportAdapter` in `src/main/jsweet`).
