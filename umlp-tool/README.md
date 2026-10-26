# UMLPTool


## Use in Gradle
0. The SE-Nexus can be read anonymously.
   Only for publishing, request a Nexus-Login from your supervisor & store it as `mavenUser` and `mavenPassword` in `~/.gradle/gradle.properties`
1. Add SE-Nexus to Plugin Repositories.

in ``settings.gradle``:
````
pluginManagement {
  repositories {
    maven { // For UMLP Plugin
      url = "https://nexus.se.rwth-aachen.de/content/groups/umlp"
    }
    gradlePluginPortal()
    mavenCentral()
  }
}
````

2. Load Plugin (Note: there are different plugins, e.g. special plugins for backend&frontend. See below for more information)

at the top of ``build.gradle``:
````
plugins {
  id 'de.rwth.se.montigem' version '7.6.0-SNAPSHOT'
  id 'java'
  // [...]
}
````

3. Add Dependencies of Generated Code in ``build.gradle``

````
repositories {
  maven {
      url "https://nexus.se.rwth-aachen.de/content/groups/umlp"
  }
}
dependencies {
  implementation group: 'umlp', name: 'umlp-rte', version: '7.6.0-SNAPSHOT'
}
````

4. Add Task in ``build.gradle`` 
````
task myFirstUMLPTask(type: UMLPBackendTask) {
  input = file("models/")
  hwcDir = layout.projectDirectory.dir("hwc/")

  outputDir = layout.buildDirectory.dir("generated-sources/")
}
````

5. Add generated Code to Java-Input in ``build.gradle``
````
sourceSets {
  main.java.srcDirs += [myFirstUMLPTask.outputDir]
}
````

### Plugins
1) `de.rwth.se.montigem` - the basic plugin which is always loaded. No task is created
2) `de.rwth.se.montigem-backend` - plugin for generating the UMLP-backend. The following steps are performed:
  * load `java` plugin
  * create task ``umlp2java`` of type `UMLPBackendTask` with the following conventions:
    * ``input=src/models/``
    * ``hwcDir=src/main/java``
    * ``tmplDir=src/templates``
  * add generated files to java main-source-sets
  * create ``generate`` and `continuous` lifecylce task
3) `de.rwth.se.montigem-frontend` - plugin for generating the UMLP-backend. The following steps are performed:
* load `base` plugin
* create configuration ``guiTypes`` which is used to load symbols
* create task ``setupFrontend`` of type `setupFrontend` with the following convention:
  * ``input=config/``
* create task ``umlp2ts`` of type `UMLPFrontendTask` with the following convention: 
  * ``input=../models/``
  * ``hwcDir=src/app/``
* create task ``gui2java`` of type `UMLPGuiGenTask` with the following convention:
  * ``input=../models/``
  * ``modelDir=../models/``
  * ``hwcDir=src/app/``
  * symbols are loaded from configuration ``guiTypes`` and the output of `umlp2ts`
* two sync tasks are created for both ``umlp2ts`` and ``gui2java`` which copy the generated code. This is required for `npm serve` and continuous build (see below)
* create ``generate`` and `continuous` lifecylce task

### Configuration Options for all UMLP-Tasks
The following options are available for all tasks. Some special tasks (e.g. `UMLPBackendTask`) have additional parameters.

| Option         | Type                   | Description                                                                                                           |
|----------------|------------------------|-----------------------------------------------------------------------------------------------------------------------|
| input          | File, Files, Directory | Collection of input models. Each file is a seperate generation process. Only `.umlp` and `.gui` models are processed. |
| hwcDir         | Directory              | Directory with Handwritten Code. Used for TOP-Mechanism.                                                              |
| tmplDir        | Directory              | Location of custom template-files. Can be used to customize generation process.                                       |
| configTemplate | String                 | Filename of `.ftl` file in `tmplDir`                                                                                  |
| moreArgs       | String Array           | Not recommended! Can break incremental build! Allows to pass custom arguments to command line interface of UMLP.      |
| debug          | Boolean                | Enable Debug output and disable Isolation (see below). Default: `false`                                               |
| outputDir      | Directory              | Output for generated Files                                                                                            |
| reportDir      | Directory              | Output for reports. Is used for Incremental Build. Default: `<buildDir>/umlp_reports/task_<taskname>`                 |


### Debug ``UMLPTask``
By default, each run of an ``UMLPTask`` is isolated from its environment and debugging tools / breakpoints do not work.

When passing ``debug=true`` to an `UMLPTask` this isolation is disabled. Hence debugging is not compatible with executing multiple `UMLPTasks` in parallel.
To disable incremental build during debugging, add the argument ``outputs.upToDateWhen { false }``. Then the task is executed during every build, even when the inputs have not changed.

An example for debug configuration:
````
task exampleForDebugging(type: UMLPBackendTask) {
  input = file("models/")

  debug = true  // Disable isolation and set LogLevel to DEBUG. Allows to use debugging tools and breakpoints.
  outputs.upToDateWhen { false } // Disable incremental build for debugging

  outputDir = layout.buildDirectory.dir("generated-sources/")
}
````

To globally enable debugging for every ``UMLPTask``, set in `gradle.properties`:
````
de.monticore.gradle.debug=true
org.gradle.parallel=false  // parallel execution not compatible with debugging
````

Only enable the debug modus for debugging, and disable if afterwards. It must not be active for production builds.

## HotReload

### Hot Reload of UMLP Backend
The build can be configured to automatically compile & re-start the backend on each save-operation.
For example, the developer is changing the UMLP class diagram and saves the file with CTRL+S.
Then the build system will automatically start the ``UMLPTask`` to generate java files, compile these java files and run them.

Initial configuration for Hot Reload of Backend:
* Add the spring-boot gradle plugin to the ``build.gradle``
  * [Documentation](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/)
* Add ``spring-boot-devtools`` as a developmentOnly dependencie in `build.gradle`
  * e.g. `dependencies {   developmentOnly("org.springframework.boot:spring-boot-devtools:$spring_version") } `

Start Hot Reload:
* Start ``gradle <backend_subproject>:run``
  * Command should keep running, do not terminate
* Start ``gradle <backend_subproject>:continuous --continuous`` in new terminal
  * Command should keep running, do not terminate
* Edit files. On each "Save" the new state should be started automatically. Look at the output of the previous commands for information.
* (Optional) Install LiveReload Plugin in the Browser. It will automatically refresh the page after each update.
  * [Firefox Addon](https://addons.mozilla.org/de/firefox/addon/live-reload/) or [Chrome Addon](https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei)

Currently Known Limitations:
* Changes in the database-schemata (e.g. changes of .umlp file) could lead to problems and might require a full restart
  * Workaround: Close the DBConnection on Exit
```java
@PreDestroy
public void onExit() {
  try { DatabaseAccessManager.closeConnection();} catch (SQLException ignored) {}
}
```
* changes in the gradle build setup (e.g., `build.gradle`) do not lead to an automatic reload

### Hot Reload of UMLP Frontend
Requires use of UMLP-Frontend-Plugin

Start Hot Reload:
* Start ``gradle <frontend_subproject>:run``
  * Command should keep running, do not terminate
* Start ``gradle <frontend_subproject>:continuous --continuous`` in new terminal
  * Command should keep running, do not terminate

Currently Known Limitations:
* During reload an error-message might be shown for a few seconds

## Notes for Developers of UMLP-Tool
The gradle-``UMLPTask`` supports incremental builds.
It only runs when a _relevant_ input has changed.
For example, when a new file ``hwc/A.java`` is added and it corresponds to a generated `A.java` in the output, the generator runs again.
But when a new file ``hwc/B.java`` is added which does *not* correspond to a generated `B.java`, the generator does not run again.

To ensure the correctness of this check, ``UMLPTask`` needs information of the input/outputs.

The ``IncGenGradleReporter`` must be enabled and configured with the correct parameters. This is already handled in `UMLPReports.java`.
At the end of the generation process, the reports must be written with ``Reporting.flush(..)``

The reporter must be notified for the following events:
* File Read: ``Reporting.reportOpenInputFile(...)``
* Existence Checks of File: ``Reporting.reportHWCExistenceCheck(...)``
* Output File created: ``Reporting.reportFileCreation(...)``

###### Inspect Report
By default, the `UMLPTask` write the report to ``target/umlp_reports/task_<taskName>/<FileName>_<FileEnding>/<FileName>_<FileEnding>/IncGenGradleCheck.txt``

Meaning of the prefixes:
* mc4: Input File
* gen: Existence Checked File
* out: Output File

All paths are relative to the project-root.
