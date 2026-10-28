<!-- (c) https://github.com/MontiCore/monticore -->
This documentation uses the same example setup.

Example setup:
- Example non-existing library: `experimental` would be in
  `experimental`. `experimental` (full name: `@umlp/experimental`) can
  be replaced with any other library name in this instruction. For example, if
  you build library `foo`, every occurence of `experimental` must be replaced
  with `foo`.
- Example existing library: [arrange](arrange). `arrange` (full name:
  `@umlp/arrange`) can be replaced with any other existing library name in this
  instruction.
- [Full library list](./projects)
- [Project root](./). Will be referred to as "root" in this instruction.

# Add library

Decide if you need a new library. When you may need a library:
- The components you build are specific to a domain. For example, your component
  displays circuit diagrams. Such component is unlikely useful in financial
  apps, stores, assistive sysmtes, or other apps unrelated to electrical
  engineering.
- The components you build depend on other libraries that are not included in
  any other existing library. For example, you use `sigma.js` to build a network
  graph. Components that depend on other libraries include the entire library
  they depend on in the compiled bundle, which may make the library
  significantly bigger.

To create a library:
1.  Create a new folder `experimental/`.
2.  Add file `experimental/README.md` and describe what
    you library contains and why it exists.
3.  Add file `experimental/ng-package.json` with following contents:
    ```json
    {
      "$schema": "../../node_modules/ng-packagr/ng-package.schema.json",
      "dest": "../../build/dist/experimental",
      "lib": {
        "entryFile": "index.ts"
      },
      "allowedNonPeerDependencies": []
    }
    ```
4.  Add file `experimental/package.json` with following contents:
    ```json
    {
      "peerDependencies": {
        "@angular/common": "^17.0.7",
        "@angular/core": "^17.0.7",
        "rxjs": "~7.8.0"
      },
      "name": "@umlp/experimental",
      "version": "7.6.0",
      "sideEffects": false,
      "dependencies": {
        "tslib": "^2.3.0"
      }
    }
    ```
    The `version` should be set to the current MontiCore release version. This
    rule is not strict but saves time looking for most recent and compatible
    library versions.

    **My library depends on an existing MontiGem library**. For example, the
    `@umlp/experimental` library uses a component from `@umlp/arrange`. In that
    case add the `@umlp/arrange` to peer dependencies:
    ```diff
    "peerDependencies": {
      "@angular/common": "^17.2.0",
      "@angular/core": "^17.2.0",
    + "@umlp/arrange": "^7.6.0"
    },
    ```
    App developers using your library will have to specify both `@umlp/arrange`
    and `@umlp/experimental` as dependencies if not provided by default (next
    step).
5.  (Optional) If it makes sense to include your library in every MontiGem
    application, add it to the [default `package.json`
    file](../../umlp-tool/gradle-plugin/src/main/resources/frontend/config/package.json):
    ```diff
    dependencies: {
      ...,
    + "@umlp/experimental": "^7.6.0"
    }
    ```
6.  [Add a component](#add-component). You may skip to step 7 if you are going
    to add a component later.
7.  Add file `experimental/src/index.ts`. This file exports the
    components, functions, services and other code that your library contains.
    The example content:
    ```ts
    export * from './lib/MyComp';
    ```
7.  Add following entry to `"projects"` object in `angular.json` file in root
    folder:
    ```json
    "@umlp/experimental": {
      "projectType": "library",
      "root": "experimental",
      "sourceRoot": "experimental",
      "prefix": "gem",
      "architect": {
        "build": {
          "builder": "@angular-devkit/build-angular:ng-packagr",
          "options": {
            "project": "experimental/ng-package.json"
          },
          "configurations": {
            "production": {
              "tsConfig": "tsconfig.prod.json"
            },
            "development": {
              "tsConfig": "tsconfig.json"
            }
          },
          "defaultConfiguration": "production"
        },
        "test": {
          "builder": "@angular-devkit/build-angular:karma",
          "options": {
            "tsConfig": "tsconfig.spec.json",
            "polyfills": [
              "zone.js",
              "zone.js/testing"
            ]
          }
        }
      }
    }
    ```
8.  Add the library to the list of libraries in `build.gradle` file in the root:
    ```diff
    def libs = [
      ...,
    + "experimental"
    ]
    ```

    **My library depends on another MontiGem library**. For example, the
    `@umlp/experimental` library uses a component from `@umlp/arrange`. In that
    case add the dependency:
    ```groovy
    tasks.build_experimental.inputs.files(tasks.build_arrange.outputs.files.singleFile)
    tasks.build_experimental.dependsOn(tasks.build_arrange)
    ```
    If you introduce a circular dependency, you need to reconsider your library
    structure.

# Update library

Whenever changes are made to a library, the version needs to be updated in the
`package.json` file of the library. Example (all references to the `arrange`
example should be replaced with your own library):

- A file is changed in `arrange` library, i.e. any file inside [the library
  folder - `arrange`](arrange).
- Open [`arrange/package.json` file](arrange/package.json) of
  the library. 
- Increment the version number of the `"version"` field.
  - last version number if you make a patch (bug fix, minor stuff) or a backward
    compatible change, e.g., current value is `"7.5.1"`. Set new value
    `"7.5.2"`.
  - middle version number if you add a backward compatible feature, e.g.,
    current value is `"7.5.12"`. Set new value `"7.6.0"`.
  - first version number if you make a breaking change, e.g., current value is
    `"7.5.12"`. Set new value according to MontiCore version `"8.0.0"`.
- Commit and push changes

## Versions and dist-tags

Every build marks its libraries with the npm dist-tag `g-<montigem version>`,
e.g. `g-7.10.0` or `g-7.10.0-SNAPSHOT` (`ModuleFrontendPlugin`). The prefix is
required, as npm rejects tags that are valid semver ranges.
`SetupFrontendTask` sets the `@umlp/*` and `@montigem/*` dependencies of the
default `package.json` of applications to this tag (version from
`mg3BuildInfo.properties`), so a generator uses the libraries of its own build.
Versions defined in `package.override.json` take precedence over automatically set versions.
Applications add further libraries there with the tag of their MontiGem version,
`"g-${mg_version}"`, which `SetupFrontendTask` resolves from the Gradle properties.

Releases (e.g. `7.10.0`) and snapshots (e.g. `7.10.0-SNAPSHOT`) are published
alike: `publishLib` publishes a prepatch version with the build timestamp,
e.g. `9.0.3` becomes `9.0.4-20261028143012`, with the tag instead of `latest`.
Raising the patch is required as the prerelease `9.0.3-…` would be older than
the released `9.0.3`.
Peer dependencies on other `@umlp/*` and `@montigem/*` libraries are set to
the tag in the published `package.json`, as a range like `^9.0.3` does not
match the prerelease `9.0.4-…`.

The `package.json` files in the repository are not changed and still hold the
last released version.
npm keeps a tag dependency that is already locked in `package-lock.json`:
applications get the libraries of a newer build only with `npm update` or
without the lockfile, e.g. after `gradle clean`.

## Add component

For example, you want to add a component `foo.Bar` to the `experimental`
library:
1.  Place `foo/Bar.gui` into `models` folder, if exists. If file additionally
    uses types from CD models, CD-related artifacts are handled similarly
    (placed beside component files).
2.  If the component is not described in a model, add sources in a path of your
    choice inside `experimental/src`. If component is based on a model, use [TOP
    mechanism](https://git.rwth-aachen.de/monticore/umlp/-/wikis/tutorial/step2/TOPMechanism)
    to extend/rewrite the component with hand-written code. For example, create
    `experimental/src/foo/Bar.ts` to extend the TypeScript class of the
    component.
3.  Export the component. If the TypeScript class that defines the component is
    hand-written, export it in `experimental/index.ts` file:
    ```ts
    export { Bar } from './src/foo/Bar';
    ```
    If the class is generated, export it from generated file:
    ```ts
    export { Bar } from './build/gui2java2ts/foo/BarComponent';
    ```

## Remove component

For example, you want to remove a component `foo.Bar` from the `experimental`
library:
1.  Remove hand-written sources for the component, possible artifacts:
    - `experimental/src/foo/Bar.*`
    - `experimental/src/foo/BarTypes.*`
    - `models/foo/Bar.gui`
    - `models/foo/BarTypes.cd`
2.  Remove component export(s) from `experimental/index.ts` if exists:
    ```diff
    - export { Bar } from './src/foo/Bar';
    - export { MyBarType } from './build/gui2java2ts/foo/MyBarType';
    - ...
    ```

# Remove library

To remove a library:
1.  Remove folder `umlp/experimental`.
2.  Remove following entry from `"projects"` object in `angular.json` file in
    root folder:
    ```json
    "@umlp/experimental": {
      "projectType": "library",
      "root": "experimental",
      "sourceRoot": "experimental/src",
      "prefix": "gem",
      "architect": {
        "build": {
          "builder": "@angular-devkit/build-angular:ng-packagr",
          "options": {
            "project": "experimental/ng-package.json"
          },
          "configurations": {
            "production": {
              "tsConfig": "tsconfig.prod.json"
            },
            "development": {
              "tsConfig": "tsconfig.json"
            }
          },
          "defaultConfiguration": "production"
        },
        "test": {
          "builder": "@angular-devkit/build-angular:karma",
          "options": {
            "tsConfig": "tsconfig.spec.json",
            "polyfills": [
              "zone.js",
              "zone.js/testing"
            ]
          }
        }
      }
    }
    ```
3.  Remove the library from the list of libraries in `build.gradle` file in the
    root:
    ```diff
    def libs = [
      ...,
    - "experimental"
    ]
    ```

    Remove any library-related dependencies:
    ```diff
    - tasks.build_experimental.inputs.files(tasks.build_arrange.outputs.files.singleFile)
    - tasks.build_experimental.dependsOn(tasks.build_arrange)
    ```

# Using a library

In a regular app:
- add dependency to the library in your project's `package.json` file if you are
  adding a domain-specific library:
  ```diff
  "dependencies": {
    ...,
  + "@umlp/experimental": "^7.6.0"
  }
  ```
  > Note: domain-specific includes, e.g., `@umlp/networkgraph` and
  > `@umlp/functiondisplay`

In an app inside of a montigem project (`example`, `products`, etc.):
- add path to your project's `tsconfig.json` file:
  ```diff
  "paths": {
  + "@umlp/experimental": ["../../../../../umlp-rte/frontend/experimental"],
    ...,
  }
  ```
