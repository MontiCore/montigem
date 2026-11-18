/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Create ".npmrc" file with authentication data for Nexus.
 * Also ensures that the file is not committed in git.
 */
abstract public class CreateNpmrcTask extends DefaultTask {

  /** Port of the local npm repository, unless the gradle property <code>localNpmRepoPort</code> is set. */
  public static final int DEFAULT_LOCAL_NPM_PORT = 4873;
  /**
   * Gradle property with the port of the local npm repository, e.g. <code>-PlocalNpmRepoPort=24873</code>.
   * Builds that run at the same time on one host (CI jobs sharing the network of their host) need
   * different ports, otherwise they would publish to and install from the registry of each other.
   */
  public static final String LOCAL_NPM_PORT_PROPERTY = "localNpmRepoPort";
  public static final String NEXUS_NPM_REPOSITORY =
      "https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/";

  public CreateNpmrcTask() {
    super();

    getNpmrcFile().convention(getProject().getObjects().fileProperty().fileValue(getProject().file(".npmrc")));
    getGitignore().convention(getProject().getObjects().fileProperty().fileValue(getProject().file(".gitignore")));
    getUnscoped().convention(false);
    getUseLocalNpmRepo().convention(false);
    getLocalNpmRepository().convention(getProject().provider(() -> localNpmRepository(getProject())));
  }

  /** @return whether the port of the local npm repository is given by the gradle property */
  public static boolean hasLocalNpmPort(Project project) {
    Object port = project.findProperty(LOCAL_NPM_PORT_PROPERTY);
    return port != null && !port.toString().isBlank();
  }

  /** @return the port of the local npm repository of this build */
  public static int localNpmPort(Project project) {
    if (!hasLocalNpmPort(project)) {
      return DEFAULT_LOCAL_NPM_PORT;
    }
    String value = project.findProperty(LOCAL_NPM_PORT_PROPERTY).toString().trim();
    try {
      int port = Integer.parseInt(value);
      if (port >= 1 && port <= 65535) {
        return port;
      }
    }
    catch (NumberFormatException e) {
      // reported below
    }
    throw new GradleException("-P" + LOCAL_NPM_PORT_PROPERTY + " must be a port (1-65535), but is '" + value + "'");
  }

  /** @return the URL of the local npm repository at the given port, e.g. http://127.0.0.1:4873/ */
  public static String localNpmRepository(int port) {
    return "http://127.0.0.1:" + port + "/";
  }

  /** @return the URL of the local npm repository of this build */
  public static String localNpmRepository(Project project) {
    return localNpmRepository(localNpmPort(project));
  }

  /** Only an input: the file has to be written again if the port of the local npm repository changes. */
  @Input
  public abstract Property<String> getLocalNpmRepository();

  @OutputFile
  public abstract RegularFileProperty getNpmrcFile();


  @InputFile
  public abstract RegularFileProperty getGitignore();

  /**
   * Set to true in exceptional cases, such as publishing to a Nexus repository, which does not
   * support scoped authentication.
   * @return whether the registry and _auth entries should be unscoped.
   */
  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getUnscoped();

  /** Whether the generated configuration should use the local npm repository. */
  @Input
  public abstract Property<Boolean> getUseLocalNpmRepo();

  void checkGitignore() throws IOException {
    File gitignore = getGitignore().get().getAsFile();
    if(!gitignore.getName().equals(".gitignore")){
      throw new RuntimeException("gitignore input must be a <.gitignore> file!");
    }
    if(!FileUtils.readLines(gitignore, StandardCharsets.UTF_8).contains(".npmrc")){
      throw new RuntimeException("Error in <.gitignore> File: " + gitignore.getAbsolutePath() + "\n"
      + "It must contain the line \".npmrc\" to ensure that credentials are not accidentally committed.");
    }
  }

  @TaskAction
  public void run() throws IOException {
    checkGitignore();

    String data = getNpmrcContent(
        getProject(), getUnscoped().get(), getUseLocalNpmRepo().get());
    FileUtils.writeStringToFile(getNpmrcFile().getAsFile().get(), data, StandardCharsets.UTF_8);
  }

  public static String getNpmrcContent(Project project, boolean unscoped){
    return getNpmrcContent(project, unscoped, false);
  }

  /**
   * Without <mavenUser> / <mavenPassword>, the registries are still configured,
   * but accessed anonymously (sufficient for downloads from Nexus).
   */
  public static String getNpmrcContent(
      Project project, boolean unscoped, boolean useLocalNpmRepo) {
    String localNpmRepository = localNpmRepository(project);
    if (useLocalNpmRepo && unscoped) {
      return "registry=" + localNpmRepository + "\n";
    }

    Optional<String> encodedAuthData = Optional.empty();
    if (project.getProperties().containsKey("mavenUser") && project.getProperties().containsKey("mavenPassword")) {
      String username = project.getProperties().get("mavenUser").toString();
      String password = project.getProperties().get("mavenPassword").toString();

      encodedAuthData = Optional.of(Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
    }

    if (useLocalNpmRepo) {
      return "@monticore:registry=" + NEXUS_NPM_REPOSITORY + "\n" +
          "@umlp:registry=" + localNpmRepository + "\n" +
          // npm libraries of the MontiGem modules (e.g., @montigem/dashboard)
          "@montigem:registry=" + localNpmRepository + "\n" +
          encodedAuthData.map(auth -> "@monticore:_auth=\"" + auth + "\"").orElse("");
    }

    if (unscoped) {
      return "registry=" + NEXUS_NPM_REPOSITORY + "\n" +
          encodedAuthData.map(auth ->
              "//nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/:_auth=\"" + auth + "\"").orElse("");
    }

    return "@monticore:registry=" + NEXUS_NPM_REPOSITORY + "\n" +
        "@umlp:registry=" + NEXUS_NPM_REPOSITORY + "\n" +
        "@montigem:registry=" + NEXUS_NPM_REPOSITORY + "\n" +
        encodedAuthData.map(auth -> "@monticore:_auth=\"" + auth + "\"\n" +
            "@umlp:_auth=\"" + auth + "\"\n" +
            "@montigem:_auth=\"" + auth + "\"").orElse("");
  }

}
