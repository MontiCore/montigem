/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific.integrationtesting;

import de.montigem.gradle.specific.CreateNpmrcTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * Runs a local Verdaccio npm registry for the duration of a Gradle build.
 * <p>
 * The service is started on demand by {@link StartLocalNpmRegistryTask} using the node that the
 * node-gradle plugin provides, and is stopped by Gradle when the build ends.
 * Verdaccio itself is installed once into the tool directory.
 * If a registry already answers at the port (e.g., the one of devScripts/localNpmRepo), it is used
 * as is and left running.
 * <p>
 * This must not happen to builds that run at the same time on one host and each bring their own
 * storage, e.g. CI jobs sharing the network of their host: they would publish to and install from
 * the registry of another job. Such builds pass their own port with <code>-PlocalNpmRepoPort</code>,
 * a registry that already answers there is then an error.
 */
public abstract class LocalNpmRegistryService
    implements BuildService<LocalNpmRegistryService.Params>, AutoCloseable {

  public static final String VERDACCIO_VERSION = "6.11.1";

  private static final String CONFIG_RESOURCE = "/localNpmRepo/config.yaml";
  private static final Logger LOG = Logging.getLogger(LocalNpmRegistryService.class);
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(90);

  public interface Params extends BuildServiceParameters {

    /**
     * Where Verdaccio stores the published packages, e.g. the pipeline's ".npmLocal".
     * Optional: if absent, only an already running registry can be used to avoid littering your disk
     */
    DirectoryProperty getStorageDir();

    /** Port the registry listens on. */
    Property<Integer> getPort();

    /** Whether the port was chosen for this build (-PlocalNpmRepoPort) and is not just the default. */
    Property<Boolean> getPortIsExplicit();

    /** Where Verdaccio is installed to; also contains its configuration and log. */
    DirectoryProperty getToolDir();

    /** Path of (or command for) the node executable. */
    Property<String> getNodeExecutable();

    /** Path of (or command for) the npm executable, used to install Verdaccio. */
    Property<String> getNpmExecutable();
  }

  private Process process;

  /** Makes sure that a registry is reachable. Does nothing if it already is. */
  public synchronized void ensureStarted() throws IOException {
    int port = getParameters().getPort().getOrElse(CreateNpmrcTask.DEFAULT_LOCAL_NPM_PORT);
    String registry = CreateNpmrcTask.localNpmRepository(port);
    if (isReachable(port)) {
      if (process == null || !process.isAlive()) {
        if (getParameters().getPortIsExplicit().getOrElse(false)
            && getParameters().getStorageDir().isPresent()) {
          // never fall back to it: its packages are the ones of another build
          throw new GradleException("Another local npm registry is already running at " + registry
              + ", but this build has to start its own one (storage: "
              + getParameters().getStorageDir().get().getAsFile() + "). Choose a free port with -P"
              + CreateNpmrcTask.LOCAL_NPM_PORT_PROPERTY + "=<port>.");
        }
        LOG.lifecycle("Using the already running local npm registry at {}", registry);
      }
      return;
    }

    if (!getParameters().getStorageDir().isPresent()) {
      throw new GradleException("No local npm registry is running at "
          + registry + " and none can be started because the storage "
          + "directory is unknown. Set -PlocalNpmRepoDir=<dir> to let Gradle start one, or start "
          + "devScripts/localNpmRepo with docker.");
    }
    Path toolDir = getParameters().getToolDir().get().getAsFile().toPath();
    Path storageDir = getParameters().getStorageDir().get().getAsFile().toPath();
    Files.createDirectories(toolDir);
    Files.createDirectories(storageDir);
    Path log = toolDir.resolve("verdaccio.log");

    Path verdaccio = installVerdaccio(toolDir, log);

    Path config = toolDir.resolve("config.yaml");
    Files.writeString(config, renderConfig(configTemplate(), storageDir, port), StandardCharsets.UTF_8);

    LOG.lifecycle("Starting the local npm registry at {} (storage: {})", registry, storageDir);
    process = new ProcessBuilder(
        getParameters().getNodeExecutable().get(), verdaccio.toString(), "--config", config.toString())
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
        .start();
    awaitReady(log, port);
  }

  /** Installs Verdaccio into the tool directory, if that was not done yet. */
  private Path installVerdaccio(Path toolDir, Path log) throws IOException {
    Path prefix = toolDir.resolve("verdaccio-" + VERDACCIO_VERSION);
    Path bin = prefix.resolve("node_modules/verdaccio/bin/verdaccio");
    if (Files.isRegularFile(bin)) {
      return bin;
    }
    LOG.lifecycle("Installing verdaccio@{} to {}", VERDACCIO_VERSION, prefix);
    // an empty user config isolates the installation from the .npmrc files of the machine
    Path emptyNpmrc = toolDir.resolve("empty.npmrc");
    Files.writeString(emptyNpmrc, "", StandardCharsets.UTF_8);

    ProcessBuilder install = new ProcessBuilder(
        getParameters().getNpmExecutable().get(), "install", "--prefix", prefix.toString(),
        "--no-audit", "--no-fund", "--registry", "https://registry.npmjs.org/",
        "verdaccio@" + VERDACCIO_VERSION)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
    install.environment().put("npm_config_userconfig", emptyNpmrc.toString());
    // the npm script finds its node through the PATH
    File nodeDir = new File(getParameters().getNodeExecutable().get()).getParentFile();
    if (nodeDir != null) {
      install.environment().merge("PATH", nodeDir.getAbsolutePath(),
          (path, dir) -> dir + File.pathSeparator + path);
    }

    try {
      if (install.start().waitFor() != 0 || !Files.isRegularFile(bin)) {
        throw new GradleException("Installing verdaccio failed, see " + log + ":\n" + tail(log));
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GradleException("Interrupted while installing verdaccio", e);
    }
    return bin;
  }

  private void awaitReady(Path log, int port) throws IOException {
    long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
    try {
      while (System.nanoTime() < deadline) {
        // first, as a registry answering at the port is not ours if our process failed to bind it
        if (!process.isAlive()) {
          break;
        }
        if (isReachable(port)) {
          return;
        }
        Thread.sleep(250);
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    close();
    throw new GradleException("The local npm registry did not become ready, see " + log + ":\n"
        + tail(log));
  }

  private static boolean isReachable(int port) {
    URI ping = URI.create(CreateNpmrcTask.localNpmRepository(port) + "-/ping");
    try {
      HttpResponse<Void> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(ping).timeout(Duration.ofSeconds(2)).GET().build(),
          HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    }
    catch (IOException e) {
      return false;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String tail(Path log) throws IOException {
    if (!Files.exists(log)) {
      return "";
    }
    List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
    return String.join("\n", lines.subList(Math.max(0, lines.size() - 30), lines.size()));
  }

  /**
   * The config of devScripts/localNpmRepo (shared with docker) with the container's
   * storage path and listen address replaced.
   */
  static String renderConfig(String template, Path storageDir, int port) {
    return template.lines()
        .filter(line -> !line.startsWith("plugins:"))
        .map(line -> line
            .replace("/verdaccio/storage", storageDir.toAbsolutePath().toString())
            .replace("0.0.0.0:4873", "127.0.0.1:" + port))
        .collect(Collectors.joining("\n", "", "\n"));
  }

  private static String configTemplate() throws IOException {
    try (InputStream in = LocalNpmRegistryService.class.getResourceAsStream(CONFIG_RESOURCE)) {
      if (in == null) {
        throw new GradleException("Missing resource " + CONFIG_RESOURCE);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Override
  public synchronized void close() {
    if (process == null) {
      return;
    }
    LOG.lifecycle("Stopping the local npm registry");
    process.destroy();
    try {
      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    }
    catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
    }
    process = null;
  }
}
