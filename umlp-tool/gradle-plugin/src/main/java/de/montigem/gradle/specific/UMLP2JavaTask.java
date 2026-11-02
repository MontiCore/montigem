/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.montigem.gradle.common.MGSingleFileTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import umlp.UMLPToolOptions;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@CacheableTask
public abstract class UMLP2JavaTask extends MGSingleFileTask {

  @InputFiles
  @org.gradle.api.tasks.Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  @Incremental
  // This Annotation requires custom java-logic to run correctly. See "getStreamOfChanges()"
  public abstract ConfigurableFileCollection getModelDir();

  @OutputDirectory
  @org.gradle.api.tasks.Optional
  public abstract DirectoryProperty getSymbolOutDir();

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getUseSpring();

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getUseShell();

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getUseNeo4j();

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getGenNeo4j();

  /**
   * @deprecated in favor of {@link UMLP2TestInfrastructureTask#getUsePlaywright()}
   */
  @Deprecated
  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<Boolean> getUsePlaywright();

  @Inject
  public UMLP2JavaTask() {
    this("UMLP2JavaTask");
  }

  public UMLP2JavaTask(String type) {
    super(type, null);

    getUseSpring().convention(true);
    getUseShell().convention(false);
    getUsePlaywright().convention(false);
    getUseNeo4j().convention(false);
    getGenNeo4j().convention(false);

    getModelDir().from(getModules());
  }

  ConfigurableFileCollection ocl = getProject().getObjects().fileCollection();

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  @Incremental
  // This Annotation requires custom java-logic to run correctly. See "getStreamOfChanges()"
  public ConfigurableFileCollection getOcl() {
    return ocl;
  }

  public void setOcl(File f) {
    ocl.from(f);
  }

  @Input
  public List<String> getOclFileNames() {
    List<String> names = new ArrayList<>();
    getOcl().getAsFileTree().visit(f -> {
      if (!f.isDirectory()) {
        names.add(f.getRelativePath().getPathString());
      }
    });
    Collections.sort(names);
    return names;
  }

  /**
   * Adds additional arguments for calling the UMLPTool#run method.
   * To allow parallel execution using a WorkQueue, {@link this#getToolAction()} is used to
   * invoke the UMLPTool
   *
   * @param filePath Path of InputFile
   * @return Arguments that can be used to start CLI-Tool
   */
  @Override
  protected List<String> createArgList(Path filePath, Function<Path, String> handlePath) {
    List<String> result = super.createArgList(filePath, handlePath);

    for (var symDir : this.getSymbolPath()) {
      result.add("-path");
      result.add(symDir.getAbsolutePath());
    }

    for (File file : this.getModelDir()) {
      result.add("-modelPath");
      result.add(handlePath.apply(file.toPath()));
    }

    if (this.getSymbolOutDir().isPresent()) {
      result.add("-s");
      result.add(handlePath.apply(getSymbolOutDir().getAsFile().get().toPath()));
    }

    if (!this.getOcl().isEmpty()) {
      result.add("-ocl");
      for (File file : this.getOcl()) {
        result.add(handlePath.apply(file.toPath()));
      }
    }

    if (this.getUseSpring().get()) {
      result.add("-" + UMLPToolOptions.USE_SPRING.getCMD());
    }

    if (this.getUseShell().get()) {
      result.add("-" + UMLPToolOptions.USE_SHELL.getCMD());
    }

    if (this.getGenNeo4j().get()) {
      result.add("-" + UMLPToolOptions.GEN_NEO4J.getCMD());
    }

    if (this.getUseNeo4j().get()) {
      result.add("-" + UMLPToolOptions.USE_NEO4J.getCMD());
    }

    return result;
  }

  @Override
  protected Stream<FileChange> getStreamOfChanges(InputChanges inputChanges) {
    Stream<FileChange> result = super.getStreamOfChanges(inputChanges);
    result = Stream.concat(result, StreamSupport.stream(inputChanges.getFileChanges(getModelDir()).spliterator(), false));
    result = Stream.concat(result, StreamSupport.stream(inputChanges.getFileChanges(getOcl()).spliterator(), false));
    return result;
  }

  @Override
  public boolean isInputFile(File f) {
    return f.getName().endsWith(".umlp") || f.getName().endsWith(".cd");
  }
}
