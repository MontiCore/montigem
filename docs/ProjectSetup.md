# Setting up a MontiGem app

A full standard MontiGem app folder structure:

```
- backend
  - build // generated sources
    - umlp2java
    - umlp2sym
    - mergeUMLP
    - resources
      - *.sql
  - src // hwc, extends generated sources from backend/build
    - main // application source set
      - java
        - *.java
      - resources
      - ...
    - test
      - java
        - *.java
      - resources
        - ...
    - generatorExtension // generator extension source set
      - resources
        - *.ftl
  - build.gradle // uses plugin 'de.rwth.se.montigem-backend'
- frontend
  - build // generated sources + app folder
    - app // entire application code
      - gui2java2ts
      - hwc // symlink
      - umlpJava2ts
      - ...
    - gui2java // output of the GUI generator
    - gui2java_sym // symbols of the GUI generator, copied to gui2sym
    - gui2java2ts // gui2java plus its JSweet translation, copied to frontend/build/app
    - hwc // symlink
    - umlpJava2ts // copied to frontend/build/app
    - umlp2jsweetAdapter // JSweet configuration for umlpJava2ts
    - umlpJava2ts // copied to frontend/build/app
    - gui2sym
    - ...
  - config // copied to frontend/build/app
    - tsconfig.json
    - package.override.json // overrides the default package.json
    - ...
  - src // hwc, extends generated sources from frontend/build
    - main
      - gui
        - *.gui
        - *.ts
        - *.html
        - *.scss
      - java
        - *.java
      - jsweet
        - umlp2jsweetAdapter
          - *.java
          - jsweetconfig.json
        - gui2jsweetAdapter
          - *.java
          - jsweetconfig.json
    - test
      - java
        - *.java
      - resources
        - ...
  - build.gradle // uses plugin 'de.rwth.se.montigem-frontend'
- models
  - *.umlp
```

The structure may be adjusted, but plugin tasks also would need to be adjusted.
For example, moving `*.umlp` outside of `models` folder will require adjusting
inputs of `umlp2java`, `umlp2sym`, and `gui2java` tasks and any additional
tasks using the `*.umlp` files as inputs.

> The folder structure is desinged with developers' convenience in mind, but it
> also must obey best practices and restrictions imposed by the tooling used to
> build the application.

The generation process overview:

![Generation process](./img/gen.png)

The generated folders correspond to a gradle task and a section of the
generation process, for example, the `gui2java` gradle task takes `*.gui` files
as an input and gives them to the GUI generator, which outputs `*.scss`, `*.html`,
`*.java` and `*.ts` files to the `gui2java` folder. Then the `gui2java2ts` task
copies them to the `gui2java2ts` folder and gives the `*.java` files to JSweet,
which outputs `*.ts` files next to them. This section of the generation process
produces the application's GUI. The settings of the generator, e.g.
`guiCreateModels`, belong to `gui2java`, the settings of JSweet, e.g.
`excludeSource`, to `gui2java2ts`.

Generally, the generation process has several steps and is described using
`<input>2<output>` notation, e.g., `umlp2java`, `gui2java2ts`.

> Generation process overview omits advanced transformations, which usually do
> not require adjustments, such as `umlp2jsweetAdapter` or `gui2sym`. These
> transformations produce files used by the generator, not the application.

## 1. Models

`models` directory contains `*.umlp` and non-`*.gui` models. It is one of the
source root for the models. Any model has a package corresponding to the path
from the source root, same as in Java.

Domain classes (classes defined in the domain model, i.e. `*.umlp` file) can be
imported and used as types in GUI models. For example, the import statement in
the GUI model for the class `User` defined in the domain model `Dtcockpit.umlp`
looks like this:
```java
import Dtcockpit.User;
```

If several `*.umlp` files are present, the files are merged in `mergeUMLP` task.
The default output is a single `Domain.umlp` file.

## 2. HWC

The folders `backend/src` und `frontend/src` are where you can override - if
necessary - generated files using the TOP- and similar mechanisms.

Both backend (BE) and frontend (FE) require specific configuration files. See
[section 3](#3-be) for BE and [section 4](#4-fe) for FE for detailed
explanations.

The deployment configuration file needs to be extended accordingly.  
The following are sample entries in `.gitlab-ci.yml`:  
```
...
build:
stage: build
script:
- "gradle --version"
- "gradle build --refresh-dependencies --stacktrace -P mavenPassword=$pass -P mavenUser=$user $GRADLE_OPTS $ARGS1"
cache:
paths:
- ...
- examples/dtcockpit/frontend/build/app/node_modules
- examples/dtcockpit/frontend/.gradle/nodejs
artifacts:
paths:
- ...
- examples/dtcockpit/frontend/build/app/dist/**
- examples/dtcockpit/frontend/build/app/Dockerfile
- examples/dtcockpit/frontend/build/app/Caddyfile
- examples/dtcockpit/frontend/build/app/.dockerignore
```

## 3. BE

The BE either uses a Neo4j or an SQL database. SQL is default and does not
require any changes. Neo4j DAO classes are always generated in parallel to SQL
DAO classes. To use a neo4j database, `DAOAFactory.initNeo4J()` and
`<DomainModel>ManagerImpl.initNeo4J()` have to be called when starting the
application and the properties files have to be adapted (see below).

BE requires specific configuration files, which must be placed in the
`backend/src/main/resources` directory.  
The configuration includes `database.properties` and several other optional
files such as `application.properties`, `logback.xml`.

Examples for `database.properties`, depending on which database is used:
```properties
jdbc_driver=org.h2.Driver
database_url=jdbc:h2:mem:umlpdb
user=...
password=...
```

```
jdbc_driver=org.neo4j.jdbc.bolt.BoltDriver
database_url=jdbc:neo4j:bolt://localhost:7687/?flatten=1
user=...
password=...
```

Here is an example for `application.properties`:
```properties
server.servlet.context-path=/umlp/api
server.port=${PORT:8081}
umlp.service.websocket.impl=auth

# No caching in browser during dev, otherwise old versions are displayed
spring.web.resources.cache.cachecontrol.no-cache=true
spring.web.resources.cache.cachecontrol.no-store=true
spring.web.resources.cache.cachecontrol.must-revalidate=true
```

Besides the classes that can be overridden, there is a class where HWC implementation is required.  
It's a ServerApplication class where, in the *main()* method, the
SpringApplication is started, and in the *init()* method, a new web server is
created and dummy data is generated if necessary/desired.
 
Here is an example from the **DTCockpit** project:

```java
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class},
    scanBasePackages = {"umlp.backendrte.service.websocket", "umlp.backendrte.service.rest", "dtcockpit.service", "security"}
)
public class DtcockpitServerApplication extends dtcockpit.DtcockpitServerApplicationTOP {

  public static void main(String[] args) {
    SpringApplication.run(DtcockpitServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();

    // Only create DummyData if database is empty
    if (DtcockpitManager.getUserList().isEmpty()) { // Is there a better way to check if dummy-data was already created?
      Log.println("Creating Dummy Data");
      DtcockpitManager.userBuilder().username("MyUser").password(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("MyPasswort"))
          //...
          .build();
    }
    //...
    try {
      Server webServer = Server.createWebServer("-web", "-webAllowOthers").start();
      Log.info(String.format("H2 Web Server started at: http://localhost:%s", webServer.getPort()), DtcockpitServerApplication.class.getName());
      //...
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}    

```

## 4. FE

In the FE source directory, besides generated domain classes, can also generated
components be overridden as well, for example if GUI actions must be defined.  
Here is an example for the `UserCreateComponent.ts`:
```
@Component({
selector: 'user-create',
templateUrl: '../../../../build/app/build/appgui/gui/login/UserCreate.html',
styles: [":host{ display: flex; flex-direction: column }"],
styleUrls: ["/Login.scss"]
})

export class UserCreateComponent extends UserCreateComponentTOP {

  public constructor(protected location: Location, protected router: Router) {
    super(router);
  }
  ...
  public override navigateBack(): void {
    this.location.back();
  }
}
```

FE also requires specific configuration files, which must be placed in the
*frontend/config/src* directory.  
The configuration includes some files in the sub folder app, which describe the
main component (root component) of a client application (will be generated later
as well).

In AppComponent we usually initialize the generated implementations of Client-
and CommandManager (can be overridden as well):
```
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  constructor() {
    DtcockpitManagerClientImpl.init();
    const ws_protocol = document.location.protocol == "https:" ? "wss": "ws";
    const cmdManager = new ClientCommandManager(new ClientCommandWebsocketCommunication(
    ws_protocol + '://' + document.location.host + '/umlp/api/command'
  ));
  CommandManager.initMe(cmdManager);
}
```

The application also needs AppModule (`AppGemModuleConfig.ts`), which imports
other used modules and AppRouting (`AppGemRouting.ts`), which allows users to
navigate between different parts of application.  

Icons, images and videos must be stored in the `config/src/assets` folder.

Frontend may additionally include `tsconfig.json` file alongside `build.gradle`
with following content:
```json
{ "extends": "./build/app/tsconfig.json" }
```
This file provides a hint for IDEs to look for the root TypeScript configuration
file in the build folder.

Instead of copying the entire JSON file from the `config` directory,
we also support overriding JSON files.
E.g., all key-values of `config/package.override.json` will be merged into the `package.json`.
Null-values will result in the removal of the original element.
For example, the following JSON overrides the name of the app,
 adds the editor dependency, and removes the cad dependency.
````json
// package.override.json
{
  "name": "my_fancy_app",
  "dependencies": {
    "@umlp/editor": "^8.0.0",
    "@umlp/cad": null
  }
}

````


### 4.1 Playwright E2E-Tests
Additional playwright e2e test infrastructure can be generated.
Only works if spring is selected in the backend.

```gradle
generateTestInfrastructure {
  ...
  usePlaywright.set(true)
  ...
}
```

Playwright end-to-end tests can be put into the `src/java/test` folder of the frontend module. 
Additionally, a dependency on playwright, spring-boot-starter-test, and the backend is needed:

```groovy
// frontend/build.gradle

dependencies {
  implementation project(":backend")
  testImplementation 'com.microsoft.playwright:playwright:1.44.0'
  testImplementation group: 'org.springframework.boot', name: 'spring-boot-starter-test', version: spring_version
  compileOnlyApi group: 'org.springframework.boot', name: 'spring-boot-starter-test', version: spring_version
}

test {
  dependsOn buildApp
  useJUnitPlatform()
}
```

Also make sure, that `app.component.ts` sets the variable `window.websocket_connected = true`, once the CommandManager is initialized:
```typescript
// frontend/config/src/app/app.component.ts

constructor() {
  ...
  CommandManager.initMe(cmdManager);
  // Add:
  (<any>window).websocket_connected = true;
}
```

## 5. How to start the application

The project is built using Gradle.  

- To start the local server (using the DTCockpit project as an example), use the following command:
```
gradle :examples:dtcockpit:backend:runServer
```
- To start the client, use the following command:
```
gradle :examples:dtcockpit:frontend:runApp
```

Before starting the client, it is advisable to stop any running Node processes. Use the following command:
```
taskkill /f /im node.exe ;  gradle:examples:dtcockpit:frontend:runApp
```
