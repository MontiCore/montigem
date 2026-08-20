/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO is this used anywhere??
@Deprecated
public abstract class Java2TSTask extends DefaultTask {


  public static final String NODE_VERSION = "24.15.0";

  @InputFiles
  public abstract ConfigurableFileCollection getInput();

  /**
   * These sources are excluded from {@link org.jsweet.transpiler.SourceFile}s passed to jsweet
   */
  @Input
  public abstract ListProperty<String> getExcludeSource();

  /**
   * Filters out files from transpilation which are still part of the source files. This allows to
   * skip RTE implementation which otherwise would lead to a symbol not found Exception during java
   * compiling.
   * {@link FileSystem#getPathMatcher(String)} for valid patterns
   */
  @Input
  public abstract ListProperty<String> getExcludeTranspile();

  @InputFiles
  public abstract ConfigurableFileCollection getExcludeTranspileFiles();

  @Input
  @Optional
  public abstract Property<Boolean> getVerbose();

  @Input
  @Optional
  public abstract Property<Boolean> getCreateSourceMaps();

  @Input
  @Optional
  public abstract Property<String> getNodeVersion();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  // Unused hwc?
//  @InputDirectory
//  @Optional
//  public abstract DirectoryProperty getHandWrittenConfigDir();

  @InputDirectory
  public abstract DirectoryProperty getJSweetAdapterDir();

  @OutputDirectory
  public abstract DirectoryProperty getJSweetWorkingDir();

  @OutputDirectory
  public abstract DirectoryProperty getJSweetCandiesDir();


  @InputFiles
  public abstract ConfigurableFileCollection getJSweetClassPath();


  @Inject
  abstract public WorkerExecutor getWorkerExecutor();

  @TaskAction
  public void run() throws IOException {

    File baseDirectory = getJSweetAdapterDir().get().getAsFile();
    File workingDir = getJSweetWorkingDir().get().getAsFile();
    File candiesJsOut = getJSweetCandiesDir().get().getAsFile();

    File outputDir = getOutputDir().get().getAsFile();

    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    File nodeInit = new File(workingDir, ".node-init");
    if (!nodeInit.exists()) {
      nodeInit.mkdirs();
    }


    // classpath passed to jsweet
    List<String> classPath = new ArrayList<>();
    classPath.add(getProject().getConfigurations().getByName("runtimeClasspath").getAsPath());

    // Prepare a WorkQueue in the jsweet class path
    WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(workerSpec -> {
      workerSpec.getClasspath().from(getJSweetClassPath());
    });

    // collect all .java files
    Set<Path> individualInputPaths = walkAndCollectJavaFiles(getInput().getFiles());

    // excluded matchers
    FileSystem fs = FileSystems.getDefault();
    List<PathMatcher> matchers = getExcludeSource().get().stream()
        .map(fs::getPathMatcher).collect(Collectors.toList());

    // collection for the .java sources
    ConfigurableFileCollection filteredSources = getProject().getObjects().fileCollection();
    // add all files to the collection if they are not excluded
    individualInputPaths.stream().filter(p -> matchers.stream().noneMatch(pm -> pm.matches(p)))
        .forEach(filteredSources::from);

    // and collect the excluded from transpilation sources
    ConfigurableFileCollection excludedFromTr = getProject().getObjects().fileCollection();
    List<PathMatcher> excludeFromTransMatchers = getExcludeTranspile().get().stream()
        .map(fs::getPathMatcher).collect(Collectors.toList());


    if (!excludeFromTransMatchers.isEmpty())
      individualInputPaths.stream().filter(p -> excludeFromTransMatchers.stream()
              .allMatch(pm -> pm.matches(p)))
          .forEach(excludedFromTr::from);

    // also add all files (recursively) from getExcludeTranspileFiles
    walkAndCollectJavaFiles(getExcludeTranspileFiles().getFiles()).forEach(excludedFromTr::from);


    // Copy relevant properties into the transpile-work-action
    workQueue.submit(Java2TSAction.class, params -> {
      params.getInput().setFrom(filteredSources);
      params.getExcludedFromTranspilation().setFrom(excludedFromTr);
      params.getVerbose().set(getVerbose());
      params.getCreateSourceMaps().set(getCreateSourceMaps());
      params.getBaseDirectory().set(baseDirectory);
      params.getWorkingDirectory().set(workingDir);
      params.getOutputDir().set(outputDir);
      params.getExtractedCandiesJavascriptDir().set(candiesJsOut);
      params.getClassPath().set(String.join(File.pathSeparator, classPath));
      params.getNodeVersion().set(getNodeVersion().orElse(Java2TSTask.NODE_VERSION));
    });

    // Block until jsweet finishes
    workQueue.await();
  }


  private static final String SOURCE_FILE_EXTENSION = ".java";


  protected Set<Path> walkAndCollectJavaFiles(Set<File> input) throws IOException {
    Set<Path> individualInputPaths = new HashSet<>();
    for (File file : input) {
      if (file.isFile()) {
        if (file.getName().endsWith(SOURCE_FILE_EXTENSION))
          individualInputPaths.add(file.toPath());
      } else if (file.exists()) {
        try (Stream<Path> o = Files.walk(file.toPath())) {
          o.filter(f -> f.getFileName().toString().endsWith(SOURCE_FILE_EXTENSION))
              .forEach(individualInputPaths::add);
        }
      } else {
        getLogger().warn("Failed to open non-existing directory: " + file.getAbsolutePath());
      }
    }
    return individualInputPaths;
  }

}
