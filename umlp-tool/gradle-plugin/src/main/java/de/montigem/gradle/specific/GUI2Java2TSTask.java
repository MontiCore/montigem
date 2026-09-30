/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.montigem.gradle.common.IncGenData;
import de.se_rwth.commons.logging.Log;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
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
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkQueue;

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
 * A combination of `gui2java`, `gui2jsweetAdapter` and `guiJava2ts`
 * `guiJava2ts` trafo depends on `gui2java` (output of `gui2java` is input for `guiJava2ts`).
 * On the other hand, `gui2java` produces `*Component.ts` files that reference files produced by
 * `guiJava2ts` to build relative paths while translating java to ts, i.e. `gui2java` depends on
 * `guiJava2ts`.
 */
// cacheable because the generated imports of HWC files are relative, all inputs must stay relocatable
@CacheableTask
public abstract class GUI2Java2TSTask extends GUI2JavaTask {

  public GUI2Java2TSTask() {
    getJSweetRuntimeClassPath().from(getProject().getConfigurations()
        .matching(c -> c.getName().equals("runtimeClasspath")));
  }

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

  @Override
  protected boolean shouldRunAgain(InputChanges inputChanges) {
    // JSweet transpiles every .java file, but the IncGen reports only list the files the generator reads
    if (hasChangedJavaFile(inputChanges, getHandWrittenCodeDir())
        || hasChangedJavaFile(inputChanges, getInput4RelativePathCalculation())) {
      return true;
    }
    return getInputFilesAsStream()
        .map(this::getIncGenFile)
        .map(incGenFile -> new IncGenData(incGenFile, this.getProject().getProjectDir()))
        .map(incGenData -> incGenData.isUpToDate(getStreamOfChanges(inputChanges)))
        .anyMatch(upToDate -> !upToDate);
  }

  protected boolean hasChangedJavaFile(InputChanges inputChanges, FileCollection files) {
    return StreamSupport.stream(inputChanges.getFileChanges(files).spliterator(), false)
        .anyMatch(change -> change.getFile().getName().endsWith(".java"));
  }

  @Override
  protected void startGeneration(List<String> args, String progressName) {
    super.startGeneration(args, progressName);
    // wait for gui2java to complete
    workQueue.await();

    WorkQueue workQueue2 = getWorkerExecutor().classLoaderIsolation(spec ->
        spec.getClasspath().from(getClassPath())
    );
    ConfigurableFileCollection srcDirs = getProject().files();
    srcDirs.from(getHandWrittenCodeDir());
    srcDirs.from(getInput4RelativePathCalculation());
    srcDirs.from(getOutputDir());
    srcDirs.from(getInput());
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
