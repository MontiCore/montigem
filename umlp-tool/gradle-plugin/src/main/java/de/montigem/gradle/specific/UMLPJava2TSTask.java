/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.montigem.gradle.common.LinkSafeFiles;
import de.montigem.gradle.common.RelativePaths;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
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

// cacheable because the generated imports of HWC files are relative, all inputs must stay relocatable
@CacheableTask
public abstract class UMLPJava2TSTask extends DefaultTask {

  @SkipWhenEmpty    // Implies @Incremental. Do nothing if no input model exists
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getInput();

  // JSweet's RelativeImportAdapter only checks that these files exist, so their content is no input
  @Internal
  public abstract ConfigurableFileCollection getInput4RelativePathCalculation();

  @Input
  public Provider<List<String>> getInput4RelativePathCalculationPaths() {
    File projectDir = getProjectLayout().getProjectDirectory().getAsFile();
    return getInput4RelativePathCalculation().getAsFileTree().getElements()
        .map(files -> RelativePaths.of(projectDir, files));
  }

  @Inject
  protected abstract ProjectLayout getProjectLayout();

  public UMLPJava2TSTask() {
    // @Internal drops the implicit task dependencies of this collection
    dependsOn(getInput4RelativePathCalculation());
    getJSweetRuntimeClassPath().from(getProject().getConfigurations()
        .matching(c -> c.getName().equals("runtimeClasspath")));
  }

  @Input
  public abstract ListProperty<String> getExcludeSource();

  @Input
  public abstract ListProperty<String> getExcludeTranspile();

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getExcludeTranspileFiles();

  @Input
  @Optional
  public abstract Property<Boolean> getVerbose();

  @Input
  @Optional
  public abstract Property<String> getNodeVersion();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  @OutputDirectory
  public abstract DirectoryProperty getJSweetWorkingDir();

  @OutputDirectory
  public abstract DirectoryProperty getJSweetCandiesDir();

  @Classpath
  public abstract ConfigurableFileCollection getJSweetClassPath();

  /**
   * The classpath JSweet compiles the sources against (the project's runtimeClasspath).
   */
  @Classpath
  public abstract ConfigurableFileCollection getJSweetRuntimeClassPath();

  @Input
  public abstract ListProperty<String> getDomain();

  @OutputDirectory
  public abstract DirectoryProperty getJSweetAdapterDir();

  @Optional
  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract DirectoryProperty getHandWrittenJSweetDir();

  @Inject
  abstract public WorkerExecutor getWorkerExecutor();

  @InputFiles
  @Classpath
  public abstract ConfigurableFileCollection getClassPath();

  @TaskAction
  public void run() {
    WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(spec ->
        spec.getClasspath().from(getClassPath())
    );
    ConfigurableFileCollection srcDirs = getProject().files();
    srcDirs.from(getInput4RelativePathCalculation());
    srcDirs.from(getOutputDir());
    srcDirs.from(getInput());
    FileCollection filteredSrcDirs = srcDirs.minus(getExcludeTranspileFiles());

    workQueue.submit(UMLP2JSweetAdapterAction.class, params -> {
      params.getDomain().set(getDomain());
      params.getJSweetAdapterDir().set(getJSweetAdapterDir());
      params.getHandWrittenJSweetDir().set(getHandWrittenJSweetDir());
      params.getSrcDirs().from(filteredSrcDirs);
    });
    workQueue.await();

    try {
      java2ts();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void java2ts() throws IOException {
    File baseDirectory = getJSweetAdapterDir().get().getAsFile();
    File workingDir = getJSweetWorkingDir().get().getAsFile();
    File candiesJsOut = getJSweetCandiesDir().get().getAsFile();

    File outputDir = getOutputDir().get().getAsFile();

    // every run transpiles all sources, so outputs of removed classes must not survive
    LinkSafeFiles.deleteContents(outputDir.toPath());
    outputDir.mkdirs();
    File nodeInit = new File(workingDir, ".node-init");
    if (!nodeInit.exists()) {
      nodeInit.mkdirs();
    }


    // classpath passed to jsweet
    List<String> classPath = new ArrayList<>();
    classPath.add(getJSweetRuntimeClassPath().getAsPath());

    // JSweet runs in a class loader that stays open between builds, see Java2TSAction
    WorkQueue workQueue = getWorkerExecutor().noIsolation();

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
      params.getBaseDirectory().set(baseDirectory);
      params.getWorkingDirectory().set(workingDir);
      params.getOutputDir().set(outputDir);
      params.getExtractedCandiesJavascriptDir().set(candiesJsOut);
      params.getClassPath().set(String.join(File.pathSeparator, classPath));
      params.getNodeVersion().set(getNodeVersion().orElse(Java2TSTask.NODE_VERSION));
      params.getJSweetClassPath().from(getJSweetClassPath());
    });

    // Block until jsweet finishes
    workQueue.await();
  }

  protected Set<Path> walkAndCollectJavaFiles(Set<File> input) throws IOException {
    Set<Path> individualInputPaths = new HashSet<>();
    for (File file : input) {
      if (file.isFile()) {
        if (file.getName().endsWith(".java")) {
          individualInputPaths.add(file.toPath());
        }
      }
      else if (file.exists()) {
        try (Stream<Path> o = Files.walk(file.toPath())) {
          o.filter(f -> f.getFileName().toString().endsWith(".java"))
              .forEach(individualInputPaths::add);
        }
      }
      else {
        getLogger().warn("Failed to open non-existing directory: " + file.getAbsolutePath());
      }
    }
    return individualInputPaths;
  }
}
