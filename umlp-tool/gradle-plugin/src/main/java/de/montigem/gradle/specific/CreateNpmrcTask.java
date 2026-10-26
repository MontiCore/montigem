/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
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

  public static final String LOCAL_NPM_REPOSITORY = "http://127.0.0.1:4873/";
  public static final String NEXUS_NPM_REPOSITORY =
      "https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/";

  public CreateNpmrcTask() {
    super();

    getNpmrcFile().convention(getProject().getObjects().fileProperty().fileValue(getProject().file(".npmrc")));
    getGitignore().convention(getProject().getObjects().fileProperty().fileValue(getProject().file(".gitignore")));
    getUnscoped().convention(false);
    getUseLocalNpmRepo().convention(false);
  }

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
    if (useLocalNpmRepo && unscoped) {
      return "registry=" + LOCAL_NPM_REPOSITORY + "\n";
    }

    Optional<String> encodedAuthData = Optional.empty();
    if (project.getProperties().containsKey("mavenUser") && project.getProperties().containsKey("mavenPassword")) {
      String username = project.getProperties().get("mavenUser").toString();
      String password = project.getProperties().get("mavenPassword").toString();

      encodedAuthData = Optional.of(Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
    }

    if (useLocalNpmRepo) {
      return "@monticore:registry=" + NEXUS_NPM_REPOSITORY + "\n" +
          "@umlp:registry=" + LOCAL_NPM_REPOSITORY + "\n" +
          // npm libraries of the MontiGem modules (e.g., @montigem/dashboard)
          "@montigem:registry=" + LOCAL_NPM_REPOSITORY + "\n" +
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
