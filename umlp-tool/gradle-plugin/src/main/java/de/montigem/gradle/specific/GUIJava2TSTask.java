/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.montigem.gradle.common.JavaBeansCacheCleanup;
import de.se_rwth.commons.logging.Log;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
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
import java.util.stream.StreamSupport;


/**
 * Translates the output of the `gui2java` generator task by `gui2jsweetAdapter` and `guiJava2ts`.
 * The `*Component.ts` files of the generator import the translated files by relative paths it
 * predicts from the Java names, and JSweet resolves imports of generated files relative to its own
 * output. Hence, the generated files are copied to {@link #getOutputDir()} and JSweet writes its
 * translation next to them.
 */
// cacheable because all inputs are relocatable
@CacheableTask
public abstract class GUIJava2TSTask extends DefaultTask {

  private final Provider<? extends BuildService<?>> javaBeansCacheCleanup;

  public GUIJava2TSTask() {
    getJSweetRuntimeClassPath().from(getProject().getConfigurations()
        .matching(c -> c.getName().equals("runtimeClasspath")));
    // the adapter actions run the generator engine in isolated class loaders
    javaBeansCacheCleanup = JavaBeansCacheCleanup.register(getProject().getGradle());
    usesService(javaBeansCacheCleanup);
  }

  /**
   * The output of the generator task
   */
  @InputFiles
  @Incremental
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract DirectoryProperty getGeneratedSources();

  /**
   * The symbols of the generator task, copied to {@link #getSymbolOutDir()}. This task keeps the
   * symbol folder it owned before the generator had its own task, otherwise Gradle removes it as
   * output of the previous run of this task after the generator task wrote it.
   */
  @InputFiles
  @Optional
  @Incremental
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract DirectoryProperty getGeneratedSymbols();

  @OutputDirectory
  @Optional
  public abstract DirectoryProperty getSymbolOutDir();

  /**
   * The generated files together with their TypeScript translation
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  /**
   * The models and other sources of the generator, JSweet resolves imports in these folders
   */
  @SkipWhenEmpty
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getSourceDirs();

  @InputFiles
  @Optional
  @Incremental
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getHandWrittenCodeDir();

  @Input
  @Optional
  public abstract ListProperty<String> getDomain();

  @SkipWhenEmpty
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getInput4RelativePathCalculation();

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
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getExcludeTranspileFiles();

  @Input
  @Optional
  public abstract Property<Boolean> getVerbose();

  @Input
  @Optional
  public abstract Property<String> getNodeVersion();

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

  @OutputDirectory
  public abstract DirectoryProperty getJSweetAdapterDir();

  @InputDirectory
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract DirectoryProperty getHandWrittenJSweetDir();

  @InputFiles
  @Classpath
  public abstract ConfigurableFileCollection getClassPath();

  /**
   * Java sources from the cd2java step, transpiled in a separate first Java2TSAction.
   * Leave empty when no .cd files are present.
   */
  @InputFiles
  @Optional
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getCD2JavaInput();

  @OutputDirectory
  @Optional
  public abstract DirectoryProperty getCD2JavaJSweetAdapterDir();

  @Inject
  public abstract WorkerExecutor getWorkerExecutor();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @TaskAction
  public void run(InputChanges inputChanges) {
    if (inputChanges.isIncremental() && !shouldRunAgain(inputChanges)) {
      getLogger().info("UP-TO-DATE, no action required");
      return;
    }
    // Gradle only closes services it has created
    javaBeansCacheCleanup.get();
    translate();
  }

  protected boolean shouldRunAgain(InputChanges inputChanges) {
    if (hasChange(inputChanges, getGeneratedSources())
        || (getGeneratedSymbols().isPresent() && hasChange(inputChanges, getGeneratedSymbols()))) {
      return true;
    }
    // JSweet translates the .java files, other files only count by their existence. Changed models
    // change the output of the generator task.
    return hasRelevantChange(inputChanges, getSourceDirs())
        || hasRelevantChange(inputChanges, getHandWrittenCodeDir())
        || hasRelevantChange(inputChanges, getInput4RelativePathCalculation());
  }

  protected boolean hasChange(InputChanges inputChanges, DirectoryProperty dir) {
    return inputChanges.getFileChanges(dir).iterator().hasNext();
  }

  protected boolean hasRelevantChange(InputChanges inputChanges, FileCollection files) {
    return StreamSupport.stream(inputChanges.getFileChanges(files).spliterator(), false)
        .anyMatch(change -> change.getChangeType() != ChangeType.MODIFIED
            || change.getFile().getName().endsWith(".java"));
  }

  protected void translate() {
    // also removes the translation of the previous run, JSweet translates all files again
    getFileSystemOperations().sync(spec -> {
      spec.from(getGeneratedSources());
      spec.into(getOutputDir());
    });
    if (getGeneratedSymbols().isPresent() && getSymbolOutDir().isPresent()) {
      getFileSystemOperations().sync(spec -> {
        spec.from(getGeneratedSymbols());
        spec.into(getSymbolOutDir());
      });
    }

    WorkQueue workQueue2 = getWorkerExecutor().classLoaderIsolation(spec ->
        spec.getClasspath().from(getClassPath())
    );
    ConfigurableFileCollection srcDirs = getProject().files();
    srcDirs.from(getHandWrittenCodeDir());
    srcDirs.from(getInput4RelativePathCalculation());
    srcDirs.from(getOutputDir());
    srcDirs.from(getSourceDirs());
    FileCollection filteredSrcDirs = srcDirs.minus(getExcludeTranspileFiles());

    workQueue2.submit(GUI2JSweetAdapterAction.class, params -> {
      params.getDomain().set(getDomain());
      params.getUseModuleGUIAdapter().set(!getDomain().isPresent() ||getDomain().get().isEmpty());
      params.getJSweetAdapterDir().set(getJSweetAdapterDir());
      params.getHandWrittenJSweetDir().set(getHandWrittenJSweetDir());
      params.getSrcDirs().from(filteredSrcDirs);
    });

    if (getCD2JavaJSweetAdapterDir().isPresent()) {
      filteredSrcDirs.plus(getCD2JavaInput());
      workQueue2.submit(GUI2JSweetAdapterAction.class, params -> {
        params.getUseModuleGUIAdapter().set(false);
        params.getJSweetAdapterDir().set(getCD2JavaJSweetAdapterDir());
        params.getHandWrittenJSweetDir().set(getHandWrittenJSweetDir());
        params.getSrcDirs().from(filteredSrcDirs);
      });
    }
    // wait for both gui2jsweetAdapter actions to complete
    workQueue2.await();

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

    Directory inputOutputDir = getOutputDir().get();

    if (!inputOutputDir.getAsFile().exists()) {
      if (!inputOutputDir.getAsFile().mkdirs()) {
        Log.error("0x00035 Failed to create directory " + inputOutputDir.getAsFile().getCanonicalPath()
            + " for the " + this.getClass().getSimpleName() + " task");
      }
    }
    File nodeInit = new File(workingDir, ".node-init");
    if (!nodeInit.exists()) {
      if (!nodeInit.mkdirs()) {
        Log.error("0x00035 Failed to create directory " + nodeInit.getCanonicalPath()
            + " for the " + this.getClass().getSimpleName() + " task");
      }
    }

    // classpath passed to jsweet
    List<String> classPath = new ArrayList<>();
    classPath.add(getJSweetRuntimeClassPath().getAsPath());
    String classPathStr = String.join(File.pathSeparator, classPath);

    Set<Path> hwcJavaPaths = walkAndCollectJavaFiles(getHandWrittenCodeDir().getFiles());
    Set<Path> inputOutputPaths = walkAndCollectJavaFiles(inputOutputDir.getAsFileTree().getFiles());
    Set<Path> cd2javaPaths = walkAndCollectJavaFiles(getCD2JavaInput().getFiles());

    // collect all .java files
    Set<Path> individualInputPaths = new HashSet<>();
    individualInputPaths.addAll(inputOutputPaths);
    individualInputPaths.addAll(hwcJavaPaths);
    individualInputPaths.addAll(walkAndCollectJavaFiles(getInput4RelativePathCalculation().getFiles()));
    individualInputPaths.addAll(cd2javaPaths);

    // excluded matchers
    FileSystem fs = FileSystems.getDefault();
    List<PathMatcher> excludeMatchers = getExcludeSource().get().stream()
        .map(fs::getPathMatcher).collect(Collectors.toList());

    // collect the excluded from transpilation sources (shared across both actions)
    ConfigurableFileCollection excludedFromTr = getProject().getObjects().fileCollection();
    List<PathMatcher> excludeFromTransMatchers = getExcludeTranspile().get().stream()
        .map(fs::getPathMatcher).collect(Collectors.toList());

    if (!excludeFromTransMatchers.isEmpty()) {
      individualInputPaths.stream().filter(p -> excludeFromTransMatchers.stream()
              .allMatch(pm -> pm.matches(p)))
          .forEach(excludedFromTr::from);
    }

    // filteredSources include ALL Java sources, including CD Java, GUI Java. They are filtered for
    // specific actions (cdJava2ts, guiJava2ts) by JSweet using CD2Java annotation
    ConfigurableFileCollection filteredSources = getProject().getObjects().fileCollection();
    individualInputPaths.stream()
        .filter(p -> excludeMatchers.stream().noneMatch(pm -> pm.matches(p)))
        .forEach(filteredSources::from);

    // also add all files (recursively) from getExcludeTranspileFiles
    walkAndCollectJavaFiles(getExcludeTranspileFiles().getFiles()).forEach(excludedFromTr::from);

    // JSweet runs in a class loader that stays open between builds, see Java2TSAction
    WorkQueue jsweetQueue = getWorkerExecutor().noIsolation();

    // First action: cd2java Java files (if any)
    if (!cd2javaPaths.isEmpty()) {

      File cd2javaBaseDirectory = getCD2JavaJSweetAdapterDir().isPresent()
          ? getCD2JavaJSweetAdapterDir().get().getAsFile()
          : baseDirectory;

      {
        String cliCommand = "Java2TSAction (cdJava2ts):\n" +
            "- Input\n\t" +
            filteredSources.getAsPath()
                .replace(File.pathSeparator, "\n\t") +
            "\n" +
            "- ExcludedFromTranspilation\n\t" +
            excludedFromTr.getAsPath()
                .replace(File.pathSeparator, "\n\t") +
            "\n" +
            "- BaseDirectory\n\t" +
            cd2javaBaseDirectory +
            "\n" +
            "- WorkingDirectory\n\t" +
            workingDir +
            "\n" +
            "- OutputDir\n\t" +
            inputOutputDir +
            "\n" +
            "- ExtractedCandiesJavascriptDir\n\t" +
            candiesJsOut +
            "\n" +
            "- NodeVersion\n\t" +
            getNodeVersion().orElse(Java2TSTask.NODE_VERSION) +
            "\n";

        Log.debug(cliCommand, Java2TSAction.class.getName());
      }

      jsweetQueue.submit(Java2TSAction.class, params -> {
        params.getInput().setFrom(filteredSources);
        params.getExcludedFromTranspilation().setFrom(excludedFromTr);
        params.getVerbose().set(getVerbose());
        params.getBaseDirectory().set(cd2javaBaseDirectory);
        params.getWorkingDirectory().set(workingDir);
        params.getOutputDir().set(inputOutputDir);
        params.getExtractedCandiesJavascriptDir().set(candiesJsOut);
        params.getClassPath().set(classPathStr);
        params.getNodeVersion().set(getNodeVersion().orElse(Java2TSTask.NODE_VERSION));
        params.getJSweetClassPath().from(getJSweetClassPath());
        // the UMLP adapters run the same rule in their merged async scanner
        params.getAutoPropagateAsync().set(false);
      });
      jsweetQueue.await();
    }

    {
      String cliCommand = "Java2TSAction (guiJava2ts):\n" +
         	"- Input\n\t" +
          filteredSources.getAsPath()
              .replace(File.pathSeparator, "\n\t") +
          "\n" +
          "- ExcludedFromTranspilation\n\t" +
          excludedFromTr.getAsPath()
              .replace(File.pathSeparator, "\n\t") +
         	"\n" +
         	"- BaseDirectory\n\t" +
          baseDirectory +
         	"\n" +
         	"- WorkingDirectory\n\t" +
          workingDir +
         	"\n" +
         	"- OutputDir\n\t" +
          inputOutputDir +
         	"\n" +
         	"- ExtractedCandiesJavascriptDir\n\t" +
          candiesJsOut +
         	"\n" +
         	"- NodeVersion\n\t" +
          getNodeVersion().orElse(Java2TSTask.NODE_VERSION) +
         	"\n";

      Log.debug(cliCommand, Java2TSAction.class.getName());
    }

    jsweetQueue.submit(Java2TSAction.class, params -> {
      params.getInput().setFrom(filteredSources);
      params.getExcludedFromTranspilation().setFrom(excludedFromTr);
      params.getVerbose().set(getVerbose());
      params.getBaseDirectory().set(baseDirectory);
      params.getWorkingDirectory().set(workingDir);
      params.getOutputDir().set(inputOutputDir);
      params.getExtractedCandiesJavascriptDir().set(candiesJsOut);
      params.getClassPath().set(classPathStr);
      params.getNodeVersion().set(getNodeVersion().orElse(Java2TSTask.NODE_VERSION));
      params.getJSweetClassPath().from(getJSweetClassPath());
      // the UMLP adapters run the same rule in their merged async scanner
      params.getAutoPropagateAsync().set(false);
    });

    // Block until jsweet finishes
    jsweetQueue.await();
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
