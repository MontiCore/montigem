package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.common.MCSingleFileTask;
import de.monticore.symboltable.serialization.json.JsonElement;
import de.montigem.gradle.MGBasePlugin;
import de.montigem.gradle.stat.GradleTaskStatistic;
import de.montigem.gradle.stat.TaskStatData;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import umlp.module_system.TaskModuleMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class MGSingleFileTask extends MCSingleFileTask implements GradleTaskStatistic {
  protected final TaskStatData statdata = new TaskStatData("SingleFile");

  @InputFiles     // Not Incremental. Any change should trigger full rebuild
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @Optional
  public abstract ConfigurableFileCollection getModules();

  @Input
  @Optional
  public abstract ListProperty<String> getModuleConfigTemplates();

  // the base class fingerprints absolute paths, which prevents build cache hits across checkouts
  @Override
  @InputFiles
  @Optional
  @Classpath
  public abstract ConfigurableFileCollection getExtraClasspathElements();

  public MGSingleFileTask(String type, String symbolPathConfigurationName) {
    super(type, symbolPathConfigurationName);

    Configuration montigemModuleGeneratorExtension = getProject().getConfigurations().findByName(MGBasePlugin.MONTIGEM_MODULE_GENERATOR_EXTENSION_CONFIG);
    if (montigemModuleGeneratorExtension != null) {
      getModules().from(montigemModuleGeneratorExtension);
    }

    getModuleConfigTemplates().convention(getProject().provider(() -> {
      List<String> res = new ArrayList<>();
      for (TaskModuleMetadata taskMetadata : ModuleSystemUtil.getMetadataForThisTask(this, getModules(), type)) {
        res.addAll(taskMetadata.getAdditionalConfigs());
      }
      return res;
    }));

    getExtraClasspathElements().from(getModules());
  }

  @Override
  protected List<String> createArgList(Path filePath, Function<Path, String> handlePath) {
    List<String> result = super.createArgList(filePath, handlePath);
    if (getModuleConfigTemplates().isPresent()) {
      for (String moduleConfigTemplate : getModuleConfigTemplates().get()) {
        result.add("--module-config");
        result.add(moduleConfigTemplate);
      }
    }

    return result;
  }

  @Override
  protected Consumer<String[]> getRunMethod() {
    // The run method is directly invoked when debugging
    return UMLPToolInvoker::run;
  }

  @Override
  protected Class<? extends AToolAction> getToolAction() {
    // The WorkAction respresentation when using a Gradle WorkQueue
    return MGAction.class;
  }

  @Override
  protected void startGeneration(List<String> args, String progressName) {
    statdata.countRun();
    super.startGeneration(args, progressName);
  }

  @Override
  public void prepareRun() {
    super.prepareRun();
    statdata.setNumberOfInputFiles(getInput().getAsFileTree().getFiles().stream().filter(this::isInputFile).count());
  }

  @Override
  @Internal
  public JsonElement getGradleStatisticData() {
    return statdata.getData();
  }

}
