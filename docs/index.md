---
template: landingpage.html
title: MontiGem
hide:
  - navigation
  - toc
---
<!-- (c) https://github.com/MontiCore/monticore -->

<h1 id="more" align="center">
  <picture>
    <img width="300" src="img/Gem.svg" alt="MontiGem">
  </picture>
</h1>

## Installing

Make sure you have installed the following dependencies:
- Java 21 (check it with `java -version`)
- Gradle 8.14.4 (check it with `gradle --version`) older versions may lead to an error with the jackson dependency
- Building needs no credentials, dependencies are downloaded anonymously from the SE RWTH Nexus.
- Only for publishing, make sure there is a file `~/.gradle/gradle.properties` with your (SE RWTH) Nexus username and password. Example:
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

You can try MontiGem using the [Vis Lib Example](https://git.rwth-aachen.de/monticore/umlp/-/tree/dev/examples/vis-lib).

- In a terminal window, go to the `examples/vis-lib` folder and run `gradle runServer`, which will (deliberately) not finish
- Once you see the following log message, you can continue: `[INFO]  UMLP API Status: RUNNING`
- Open `http://localhost:4200/` in your browser
- In your browser you can try to add new data to the tables
- Once you're finished you can stop the server in the terminal window by pressing the `CTRL` and `C` keys

<div class="full-width bg-primary" markdown>
<center markdown>
<h2 style="margin: 0;">Get Started with MontiGem Today!</h2>

<a href="Tutorial.html" class="btn btn-primary">
            Take the Tour
<svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
    <path d="M5 12h14M12 5l7 7-7 7" />
</svg>
</a>
</center>
</div>


## Editor plugins
- Download Latest Plugin
    - [IntelliJ](https://git.rwth-aachen.de/monticore/umlp/-/jobs/artifacts/dev/raw/umlp-tool/umlp-intellij-plugin/build/distributions/umlp-intellij-plugin.zip?job=build_intellij_plugin)
    - [Visual Studio Code](https://git.rwth-aachen.de/monticore/umlp/-/jobs/artifacts/dev/raw/umlp-tool/build/generated-sources/UMLPWithGUI/plugins/umlpwithgui-vscode-plugin/umlpwithgui-lsp.vsix?job=build_with_deploy)
- Install it
    - IntelliJ: [https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)
    - VSCode: [https://code.visualstudio.com/docs/editor/extension-marketplace#_install-from-a-vsix](https://code.visualstudio.com/docs/editor/extension-marketplace#_install-from-a-vsix)

## Project Structure
* **docs** contains the documentation for MontiGem
* **examples** contains examples that demonstrate commonly used features of MontiGem
* **experiments** contains experiments that demonstrate edge cases of specific MontiGem features and, at the same time, are integration tests for MontiGem
* **umlp-rte** contains the implementation of the run-time environment, i.e., the code that is the same for all applications
* **umlp-tool** contains the implementation of the language, tool, and generator
