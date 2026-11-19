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

The port can be changed with `-PlocalNpmRepoPort=<port>`, it is then also used in the generated `.npmrc` files.
Builds that run at the same time on one host and each bring their own `localNpmRepoDir` need different ports
(e.g. CI jobs that share the network of their host), otherwise they publish to and install from the registry of each other.
With both properties set, a registry that already answers at the port is therefore an error instead of being used.

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
required. Each `publishLib` task builds its library and publishes a new
timestamped version to Verdaccio, see
[Versions and dist-tags](../../umlp-rte/frontend/README.md#versions-and-dist-tags).

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
