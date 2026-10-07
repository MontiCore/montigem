/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.montigem.gradle.common.MGSingleFileTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;
import umlp.UMLPToolOptions;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Task might not be a @CacheableTask since relative paths are generated for HWC-files
public abstract class UMLP2TSTask extends MGSingleFileTask {

  public UMLP2TSTask() {
    super("UMLP2TSTask", null);
  }

  @OutputDirectory
  @org.gradle.api.tasks.Optional
  public abstract DirectoryProperty getSymbolOutDir();

  @InputFiles
  @org.gradle.api.tasks.Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  @Incremental
  // This Annotation requires custom java-logic to run correctly. See "getStreamOfChanges()"
  public abstract ConfigurableFileCollection getModelDir();

  ConfigurableFileCollection ocl = getProject().getObjects().fileCollection();

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public ConfigurableFileCollection getOcl() {
    return ocl;
  }

  public void setOcl(File f){
    ocl.from(f);
  }

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<String> getTslib();

  /**
   * @param filePath Path of InputFile
   * @return Arguments that can be used to start CLI-Tool
   */
  @Override
  protected List<String> createArgList(Path filePath, Function<Path, String> handlePath) {
    List<String> result = super.createArgList(filePath, handlePath);

    result.add("-" + UMLPToolOptions.FOR_CLIENT.getCMD());

    if (this.getSymbolOutDir().isPresent()) {
      result.add("-s");
      result.add(handlePath.apply(getSymbolOutDir().getAsFile().get().toPath()));
    }

    System.out.println("Model path is " + this.getModelDir().getFiles());
    if(!this.getModelDir().isEmpty()) {
      for (File file : this.getModelDir()) {
        result.add("-modelPath");
        result.add(handlePath.apply(file.toPath()));
      }
    }

    if (!this.getSymbolPath().isEmpty()) {
      for (var symDir : this.getSymbolPath()) {
        result.add("-path");
        result.add(symDir.getAbsolutePath());
      }
    }

    if(!this.getOcl().isEmpty()) {
      result.add("-ocl");
      for (File file : this.getOcl()) {
        result.add(handlePath.apply(file.toPath()));
      }
    }

    if (this.getTslib().isPresent()) {
      result.add("-" + UMLPToolOptions.GUI_GEN_FOR_TS_LIB.getCMD());
      result.add(getTslib().get());
    }

    return result;
  }

  @Override
  public boolean isInputFile(File f) {
    return f.getName().endsWith(".umlp");
  }

  @Override
  protected Stream<FileChange> getStreamOfChanges(InputChanges inputChanges) {
    Stream<FileChange> result = super.getStreamOfChanges(inputChanges);
    result = Stream.concat(result, StreamSupport.stream(inputChanges.getFileChanges(getModelDir()).spliterator(), false));
    return result;
  }
}
