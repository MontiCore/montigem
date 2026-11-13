<!-- (c) https://github.com/MontiCore/monticore -->

This document describes rules for configuring the build process.

Terminology:
- **Task.** A unit of work. [Read
  more](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html)
- **Input and output.** A task implementation has annotated properties that
  define inputs and outputs of a task. The inputs and outputs define whether the
  task is complete and if it is up-to-date. For example, if you run a task for
  the first time it is neither complete nor up-to-date. If task `Foo` creates a
  file `bar` in a specific folder, the folder is an **input** and the `bar` is
  an **output**. [Read
  more](https://docs.gradle.org/current/userguide/incremental_build.html)

Rules:
- Multiple tasks must not have the same output folder. Current exception:
  `nodeSetup` and `frontedSetup` work with the same output folder.
- `npmInstall` tasks must not run in parallel. This is configured in
  [`build.gradle` file](../build.gradle).
  The lines:
  ```groovy
  var previousInstallTask
  subprojects {
    tasks.withType(com.github.gradle.node.npm.task.NpmInstallTask).configureEach {
      if (previousInstallTask)
        it.mustRunAfter(previousInstallTask)
      previousInstallTask = it
    }
  }
  ```
  mean that if `npmInstall` task of two projects must be executed, it will be
  done sequentially (the order is defined but not relevant in this case). If
  `npmInstall` is executed only for one project, the other `npmInstall` tasks
  will not be triggered.
  This configuration using subprojects & configureEach detects all npmInstall 
  tasks across the entire project.

  Otherwise, an error will appear:
  ```
  npm ERR! code 1
  npm ERR! path /builds/monticore/umlp/examples/<my-project>/frontend/build/app/node_modules/puppeteer
  npm ERR! command failed
  npm ERR! command sh -c -- node install.mjs
  npm ERR! chrome-headless-shell (121.0.6167.85) downloaded to /root/.cache/puppeteer/chrome-headless-shell/linux-121.0.6167.85
  npm ERR! Error: ERROR: Failed to set up Chrome v121.0.6167.85! Set "PUPPETEER_SKIP_DOWNLOAD" env variable to skip download.
  npm ERR!     at file:///builds/monticore/umlp/examples/<my-project>/frontend/build/app/node_modules/puppeteer/lib/esm/puppeteer/node/install.js:58:23
  > Task :examples:<my-project>:compileTestJava NO-SOURCE
  > Task :examples:<my-project>:frontend:npmInstall
  npm ERR!     at async Promise.all (index 0)
  npm ERR!     at async downloadBrowser (file:///builds/monticore/umlp/examples/<my-project>/frontend/build/app/node_modules/puppeteer/lib/esm/puppeteer/node/install.js:87:9) {
  npm ERR!   [cause]: Error: unexpected end of file
  npm ERR!       at Zlib.zlibOnError [as onerror] (node:zlib:189:17) {
  npm ERR!     errno: -5,
  npm ERR!     code: 'Z_BUF_ERROR'
  npm ERR!   }
  npm ERR! }
  ```

  Reason: `npmInstall` installs Chromium on a machine to be able to run frontend
  tests. If several tasks try to install Chromium, they may fail as they try to
  concurrently access the same (locked by one of the tasks) resource.
