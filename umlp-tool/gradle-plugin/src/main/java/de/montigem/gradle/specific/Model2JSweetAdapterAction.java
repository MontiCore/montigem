/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import de.monticore.generating.GeneratorSetup;
import de.monticore.io.paths.MCPath;
import de.montigem.gradle.common.GradleLogGuard;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import umlp.generator.gui.JSweetConfigGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class Model2JSweetAdapterAction<T extends Model2JSweetAdapterAction.Params> implements WorkAction<T> {

  @Override
  public void execute() {
    GradleLogGuard.ensureGradleLog();
    try {
      this.generateJSweetConfig();
    } catch (IOException e) {
      throw new RuntimeException(this.getClass().getSimpleName() + " failed", e);
    }
  }

  protected abstract void generateJSweetConfig() throws IOException;

  /**
   * Generates configuration files required for the JSweet transpilation.
   */
  protected JSweetConfigGenerator getGenerator() throws IOException {
    GeneratorSetup gs = new GeneratorSetup();
    gs.setOutputDirectory(getParameters().getJSweetAdapterDir().get().getAsFile());
    if (getParameters().getHandWrittenJSweetDir().isPresent()) {
      Path hwc = getParameters().getHandWrittenJSweetDir().getAsFile().get().toPath();
      Path out = getParameters().getJSweetAdapterDir().getAsFile().get().toPath();
      Files.walk(hwc).forEach(path -> {
        try {
          Path target = out.resolve(hwc.relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(target);
          }
          else {
            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      gs.setHandcodedPath(new MCPath(hwc));
    }
    return new JSweetConfigGenerator(gs);
  }

  interface Params extends WorkParameters {
    @OutputDirectory
    DirectoryProperty getJSweetAdapterDir();

    @Optional
    @InputDirectory
    DirectoryProperty getHandWrittenJSweetDir();

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getSrcDirs();
  }
}
