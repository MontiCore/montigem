/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static de.montigem.gradle.FrontendBasePlugin.UMLP_GROUP_NAME;

/**
 * Writes the absolute paths of the folders of the generated code, one per line, for
 * scripts that compare the generator output of two branches (see devScripts/generatorcompare).
 * A folder that is not configured has no line. The lines go to a file because the
 * stdout of Gradle also carries task headers and the log of the generator, even with `-q`.
 */
public abstract class PrepareGeneratorDiffTask extends DefaultTask {

  public static final String PREPARE_GENERATOR_DIFF_TASK = "prepareGeneratorDiff";
  public static final String OUTPUT_FILE_PROPERTY = "generatorDiffFile";
  public static final String DEFAULT_OUTPUT_FILE = "prepareGeneratorDiff/dirs.txt";

  /**
   * Absolute paths in the order they are written
   */
  @Input
  public abstract ListProperty<String> getDirs();

  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  /**
   * Registers the task without folders, the plugins add those of their generator tasks and the
   * dependencies on these tasks.
   */
  public static TaskProvider<PrepareGeneratorDiffTask> register(Project project) {
    return project.getTasks().register(PREPARE_GENERATOR_DIFF_TASK, PrepareGeneratorDiffTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.setDescription("Generates the code and writes the paths of its folders, one per line, to build/"
          + DEFAULT_OUTPUT_FILE + ", or to the file given by -P" + OUTPUT_FILE_PROPERTY + "=<path>.");
      // a relative path is resolved against the project folder
      task.getOutputFile().set(project.getLayout().file(project.getProviders()
              .gradleProperty(OUTPUT_FILE_PROPERTY).map(project::file))
          .orElse(project.getLayout().getBuildDirectory().file(DEFAULT_OUTPUT_FILE)));
    });
  }

  /**
   * Adds the path of the folder, or nothing if the folder is not set
   */
  public void addDir(Provider<Directory> dir) {
    // forward slashes work in bash, PowerShell and Java alike
    getDirs().addAll(dir.map(d -> List.of(d.getAsFile().getAbsolutePath().replace('\\', '/')))
        .orElse(List.of()));
  }

  @TaskAction
  protected void writeDirs() {
    Path file = getOutputFile().get().getAsFile().toPath();
    try {
      Files.createDirectories(file.getParent());
      // always \n, so the file reads the same in bash on Windows
      Files.writeString(file, String.join("\n", getDirs().get()) + "\n", StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    getLogger().lifecycle("Folders of the generated code written to {}", file);
  }
}
