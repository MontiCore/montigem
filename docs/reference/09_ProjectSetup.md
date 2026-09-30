# 9. Project Setup & Build

## 9.1 Folder Structure

*   `models/`: `.umlp` and `.gui` files.
*   `backend/`: Spring Boot application.
*   `frontend/`: Angular application.

## 9.2 Gradle Tasks

*   `gradle build`: Builds the entire project.
*   `gradle :backend:run`: Starts the backend server.
*   `gradle :frontend:runApp`: Starts the frontend dev server.
*   `gradle :frontend:sync_gui2java2ts`: Updates the frontend code after model changes (faster than full restart).

## Using Conitnuous Build / Hot Reload with MontiGem

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

### Warm Generator and JSweet
The generator and JSweet stay loaded for 10 minutes after their last use, so the build after an edit starts faster.
At most 4 class loaders of each stay open. A large parallel build closes its class loaders after 6 seconds.
The system property `umlp.toolLoaderIdleSeconds` sets the time, e.g. in `gradle.properties`:
```properties
systemProp.umlp.toolLoaderIdleSeconds=6
```
The value `6` gives the behavior of earlier versions, e.g. when the memory of the machine is low.

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

## 9.3 Custom Build Tasks

You can extend `build.gradle` to add custom tasks, e.g., to copy assets.

```groovy
// frontend/build.gradle
tasks.register("copySourceCodeToAssets", Copy) {
  from("src/main/gui") { include "**/*.gui" }
  into layout.buildDirectory.dir("app/src/assets/source-code")
}
tasks.named("sync_gui2java2ts") { dependsOn "copySourceCodeToAssets" }
```

---
[< Previous: TOP Mechanism](08_TOPMechanism.md) | [Back to Index](../MontiGem_Documentation.md) | [Next: How-To Guides >](10_HowTo_MasterDetail.md)
