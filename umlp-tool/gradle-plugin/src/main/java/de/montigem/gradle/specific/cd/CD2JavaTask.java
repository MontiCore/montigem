/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific.cd;

import de.montigem.gradle.common.MGSingleFileTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import umlp.UMLPToolOptions;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

@CacheableTask
public abstract class CD2JavaTask extends MGSingleFileTask {

  public CD2JavaTask() {
    super("CD2JavaTask", null);
  }

  @InputDirectory
  @org.gradle.api.tasks.Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  // No @Incremental, changes of the Symbols lead to full rebuild
  public abstract DirectoryProperty getTemplatePath();

  @OutputDirectory
  @org.gradle.api.tasks.Optional
  public abstract DirectoryProperty getSymbolOutDir();

  @Input
  @org.gradle.api.tasks.Optional
  public abstract Property<String> getTslib();

  @Override
  public boolean isInputFile(File f){
    return f.getName().endsWith(".cd");
  }

  @Override
  protected List<String> createArgList(Path filePath, Function<Path, String> handlePath) {
    List<String> result = super.createArgList(filePath, handlePath);

    if (this.getSymbolOutDir().isPresent()) {
      result.add("-s");
      result.add(handlePath.apply(getSymbolOutDir().getAsFile().get().toPath()));
    }
    {
      result.add("--input");
      result.add(handlePath.apply(filePath));
    }
    {
      result.add("--out");
      result.add(handlePath.apply(getOutputDir().get().getAsFile().toPath()));
    }
    if (!getHandWrittenCodeDir().isEmpty()) {
      result.add("-hwc");
      result.add(handlePath.apply(getHandWrittenCodeDir().getSingleFile().toPath()));
    }
    if (getTemplatePath().isPresent()) {
      result.add("--templatePath");
      result.add(handlePath.apply(getTemplatePath().get().getAsFile().toPath()));
    }

    if (this.getTslib().isPresent()) {
      result.add("-" + UMLPToolOptions.GUI_GEN_FOR_TS_LIB.getCMD());
      result.add(getTslib().get());
    }

    result.add("-rte");

    return result;
  }
}
