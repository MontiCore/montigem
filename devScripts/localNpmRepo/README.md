# Local npm repository

This docker-compose can be used to host all umlp-rte npm packages in a local repository for debugging purposes.
It is helpful to resolve npm resolution problems or test updates to the npm packages without triggering CI multiple times.
The local npm repository Verdaccio is run at `http://127.0.0.1:4873/`. The registry stores
`@umlp/*` packages locally and proxies other public packages to npmjs.

## Without docker: started by Gradle

With `useLocalNpmRepo=true` or `publishToLocalNpmRepo=true`, the plugin starts Verdaccio itself
(task `startLocalNpmRegistry`, a dependency of `npmInstall` and `localNpmAdduser`) and stops it when the build ends.
It uses the node of the node-gradle plugin and installs Verdaccio once next to the downloaded node versions, to `<project>/.gradle/nodejs/localNpmRepo/tool`.
Packages are stored in the directory given by `-PlocalNpmRepoDir=<dir>`, which is required to start a registry
(the CI pipeline uses it to hand the packages to downstream jobs).
Without `localNpmRepoDir`, only a registry that already runs can be used. If one already answers at `http://127.0.0.1:4873/` (e.g. the docker one below), it is used and left running.

## Start and stop with docker

From this directory:

```bash
docker compose up -d # start
docker compose down # stop
```

Registry data is retained in the `verdaccio-storage` volume. To delete all
locally published packages and users:

```bash
docker compose down -v
```

*Note*: NPM does not allow overriding of already published packages, thus a new local version needs to be created by restarting the npm repo and deleting its contents:

```bash
docker compose down -v
docker compose up -d
```

## Publish UMLP frontend libraries

In the UMLP root `gradle.properties`, enable:

```properties
publishToLocalNpmRepo=true
```

Then publish through the existing Gradle lifecycle:

```bash
gradle publishLib
```

The plugin creates/logs in the fixed local development user automatically when
required. Each `publishLib` task builds its library, checks
whether the version already exists, and publishes to Verdaccio. npm versions
are immutable; bump the package version or reset the volume before republishing
different content under the same version.

## Use from an external MontiGem Gradle project

In `gradle.properties` enable:

```properties
useLocalNpmRepo=true
```

`useLocalNpmRepo` makes the plugin-generated `.npmrc` resolve `@umlp/*` from
Verdaccio; `@monticore/*` continues to use Nexus.

Refresh the plugin and perform a clean Gradle-owned frontend installation:

```bash
gradle :frontend:clean :frontend:npmInstall
gradle :frontend:buildApp
```

You can check if `npmInstall` pulled the dependencies from the local registry by looking for `http://127.0.0.1:4873` in the resolved field of each dependency in `package-lock.json`
