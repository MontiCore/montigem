<!-- (c) https://github.com/MontiCore/monticore -->
# UMLPTool

See more documentation [here](https://www.montigem.de).

MontiGem is designed to assist developers in generating web applications
from class diagrams,
simplifying the development process and accelerating project delivery.
The generated applications are ready to be deployed and can be customized further using handwritten
code to meet your specific requirements.

## Features

- Generate a Java Spring web application from class diagrams

## Installing

Make sure you have installed the following dependencies:
- Java 21 (check it with `java -version`)
- Gradle 8.14.4 (check it with `gradle --version`) older versions may lead to an error with the jackson dependency
- Make sure there is a file `~/.gradle/gradle.properties` with your (SE RWTH) Nexus username and password. Example:
- If that does not work, place the file in `C:\Users\$USERNAME\.gradle`

```
mavenUser=johndoe
mavenPassword=mySuperStrongPassword
```

In case you already have different versions of Java / Gradle and do not want to switch completely,
we recommend [SDKMan](https://sdkman.io/) for managing different versions on the same computer.

On MacOS you might need to install additional packages: `brew install pkgconfig pango cairo`

After installing the dependencies, run `gradle build` in the top folder of the project.

## Quick Start

You can try UMLP using the [Vis-Lib Example](./examples/vis-lib).

- In a terminal window, go to the `examples/vis-lib` folder and run `gradle runServer`, which will (deliberately) not finish
- Once you see the following log message, you can continue: `[INFO]  UMLP API Status: RUNNING`
- Open `http://localhost:4200/` in your browser
- In your browser you can try to add new data to the tables
- Once you're finished you can stop the server in the terminal window by pressing the `CTRL` and `C` keys

## Editor plugins
- Download Latest Plugin
  - [IntelliJ](https://git.rwth-aachen.de/monticore/umlp/-/jobs/artifacts/dev/raw/umlp-tool/umlp-intellij-plugin/build/distributions/umlp-intellij-plugin.zip?job=build_intellij_plugin)
  - [Visual Studio Code](https://git.rwth-aachen.de/monticore/umlp/-/jobs/artifacts/dev/raw/umlp-tool/build/generated-sources/UMLPWithGUI/plugins/umlpwithgui-vscode-plugin/umlpwithgui-lsp.vsix?job=build_with_deploy)
- Install it
  - IntelliJ: https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk
  - VSCode: https://code.visualstudio.com/docs/editor/extension-marketplace#_install-from-a-vsix

## Project Structure
* **doc** contains the documentation for the UMLPTool
* **examples** contains examples that demonstrate commonly used features of the UMLPTool
* **experiments** contains experiments that demonstrate edge cases of specific UMLPTool features and, at the same time, are integration tests for the UMLPTool
* **umlp-rte** contains the implementation of the run-time environment, i.e., the code that is the same for all applications
* **umlp-tool** contains the implementation of the language, tool, and generator

<<<<<<< HEAD
## Developing with MontiGem

MontiGem is available either as a CLI jar tool or a Gradle plugin that wraps the CLI. 
The plugin makes your development much easier by performing automatic configuration with sensible defaults for your project.

If you want to use the CLI tool, you can build it by executing `gradle :umlp-tool:shadowJar`.

The tool can be called with the following options: 

| Short Command | Long Command| Description |
| --- | --- |-------------|
| -4c | --forclient                       | forces UMLP generator to produce TypeScript code for client
| -cdt | --cdtogui                        | runs cd2gui to generate gui files from the input
| -ct | --configTemplate <file>           | Executes this template at the beginning of code generation to customize it. (template path may have to be set)
| -d | --debug                            | Produces more detailed log
| -dm | --domainModels <arg>              | Fully qualified names of the domain models, space separated
| -fp | --templatePath <arg>              | Directories and jars for handwritten templates, space separated.
| -gen_neo4j |                            | Generate database access infrastructure for a Neo4j / Cypher databases.
| -guic | --guiCreateModels <arg>         | Fully qualified names of the gui models for object creation, space separated
| -guits | --guiGenTSFuncionality <arg>   | Flag which determines whether TS functionality should directly be generated from GUI models
| -gv | --guivariability                  | Alterates guiFiles from the gp options with the guivariability files there and writes them to the outDir
| -h | --help                             | Prints this help dialog
| -hwc <arg>  |                           | Sets the artifact paths for handwritten code, space separated.
| -i | --input <file>                     | Reads the source file (mandatory) and parses the contents
| -ip | --importsPath <arg>               | Sets the artifact path for files that are imported by the generated artifacts, space separated
| -jsweet | --guiGenJsweet <arg>          | Flag which determines whether TS code should be transpiled instead of generated from GUI models directly
| -m | --module-config <arg>              | Fully qualified name(s) of config template(s) for added modules
| -mp | --modelPath <arg>                 | Sets the artifact path for imported models, space separated
| -mrg | --merge <arg>                    | Parses the files as additional CDs and merges them with the input CD (iff semantically sound). The result is stored in memory.
| -o | --out <dir>                        | Path of the output files.
| -ocl | --ocl <file>                     | Reads an ocl file (optional) and parses the contents
| -path <arg> |                           | Sets the artifact path for imported symbols, space separated.
| -pp | --prettyprint <file>              | Prints the AST to stdout or the specified file (optional)
| -r | --report <dir>                     | Prints reports of the artifact to the specified directory.
| -rb | --report_base <dir>               | Base Directory used for relative paths in Reports.
| -rte | --rteTypes                       | creates simple Java Types from GUI RTE types
| -s | --symboltable <file>               | Serializes the symbol table of the given artifact.
| -so | --gensymbolsonly                  | forces UMLP generator to only generate symbols for the cd 
| -ti | --testInfrastructure              | Generate infrastructure for unit- and e2e-tests
| -tslib <library_name> |                 | Name of a TS library if generation schema is adjusted for the TS library
| -use_neo4j |                            | Make Neo4j the default database access for the MontiGem app.
| -use_playwright |                       | If spring classes are generated, generate additional playwright e2e infrastructure
| -use_shell |                            | If the backend is generated, generate additional spring shell classes
| -use_spring |                           | If the backend is generated, generate additional spring classes
| -v | --version                          | Prints version information 

=======
## Scripts

* **run backend**: gradle {folder_path}:backend:run
* **run frontend**: gradle {folder_path}:frontend:run
* **refresh frontend**: gradle {folder_path}:frontend:sync_gui2java2ts
