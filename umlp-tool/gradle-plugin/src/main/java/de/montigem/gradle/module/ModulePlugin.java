package de.montigem.gradle.module;

import de.montigem.gradle.MGBasePlugin;
import de.montigem.gradle.specific.UMLP2SymTask;
import de.se_rwth.commons.logging.Log;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.file.Directory;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import javax.inject.Inject;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static de.montigem.gradle.BackendPlugin.addRTEDependency;
import static de.montigem.gradle.FrontendBasePlugin.UMLP_GROUP_NAME;
import static de.montigem.gradle.FrontendBasePlugin.inRTE;
import static de.montigem.gradle.FrontendBasePlugin.mcVersion;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_IMPORT_MAP_CONFIG;
import static de.montigem.gradle.module.ModuleFrontendPlugin.addGuiSymbolsJarAttributes;
import static de.montigem.gradle.module.ModuleFrontendPlugin.addImportMapJarAttributes;
import static de.montigem.gradle.module.ModuleFrontendPlugin.addUMLPSymbolsJarAttributes;
import static org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME;

public class ModulePlugin extends ALazyPublishingPlugin implements Plugin<Project> {

  public static final String UMLP2SYM_TASK = "umlp2sym";
  public static final String UMLP2SYM_CONFIG = "umlp2symConfig";
  public static final String GUI2SYM_CONFIG = "gui2symConfig";
  public static final String GUI2JAVA_CONFIG = "gui2javaConfig";
  public static final String GENERATOR_EXTENSION_COMPILE_CONFIG = "generatorExtensionCompile";

  @Inject
  public ModulePlugin(SoftwareComponentFactory softwareComponentFactory, TaskDependencyFactory taskDependencyFactory) {
    super(softwareComponentFactory, taskDependencyFactory);
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply("base");
    project.getPlugins().apply("java-library");
    project.getPlugins().apply("maven-publish");
    project.getPlugins().apply(MGBasePlugin.class);

    Configuration generationExtensionCompile = project.getConfigurations().create(GENERATOR_EXTENSION_COMPILE_CONFIG);
    generationExtensionCompile.setCanBeResolved(true);
    generationExtensionCompile.setCanBeConsumed(false);
    project.getDependencies().add(generationExtensionCompile.getName(), "de.monticore:monticore-runtime:" + mcVersion(project));
    project.getDependencies().add(generationExtensionCompile.getName(), "de.monticore:monticore-grammar:" + mcVersion(project));
    project.getDependencies().add(generationExtensionCompile.getName(), "umlp:umlp-tool:" + mcVersion(project));
    Project backend = project.findProject(project.getPath() + ":backend");
    if (backend != null) {
      project.getDependencies().add("api", backend);
    }

    var javaComponent = (AdhocComponentWithVariants) project.getComponents().getByName("java");

    Directory models = project.getLayout().getProjectDirectory().dir("models");

    Set<File> umlp;
    boolean umlpPresent;
    if (models.getAsFile().exists()) {
      umlp = models.getAsFileTree().matching(spec -> spec.include("**/*.umlp")).getFiles();
      umlpPresent = !umlp.isEmpty();
    }
    else {
      umlpPresent = false;
      umlp = new HashSet<>();
    }
    if (umlpPresent && !inRTE(project)) {
      addRTEDependency(project);
    }

    // Create the models jar
    Configuration umlpModels = project.getConfigurations().create("umlpModels");
    umlpModels.setCanBeConsumed(true);
    umlpModels.setCanBeResolved(false);
    umlpModels.setDescription("Published models of the module");

    addUMLPModelsJarAttributes(umlpModels, project);

    TaskProvider<Jar> umlpModelsJar = project.getTasks().register("umlpModelsJar", Jar.class, jar -> {
      jar.setGroup("build");
      jar.getArchiveClassifier().set("models");
      jar.from(umlp);
      // generator tasks fingerprint this jar byte-wise, file timestamps would defeat the build cache
      jar.setPreserveFileTimestamps(false);
      jar.setReproducibleFileOrder(true);

      jar.onlyIf(t -> umlpPresent);
    });
    project.getTasks().named(ASSEMBLE_TASK_NAME).configure(it -> it.dependsOn(umlpModelsJar));
    if (umlpPresent) {
      project.getArtifacts().add(umlpModels.getName(), umlpModelsJar);
      // Tell gradle, that the jar tasks builds a publication
      var umlpModelsPublication = createPublishedArtifact(umlpModelsJar, project);
      // Add a variant
      javaComponent.addVariantsFromConfiguration(umlpModels, ConfigurationVariantDetails::mapToOptional);
      // Add the publication as an artifact
      umlpModels.getOutgoing().getArtifacts().add(umlpModelsPublication);
    }

    // Same thing for the module gen time
    Configuration generatorExtension = project.getConfigurations().create("generatorExtension");
    generatorExtension.setCanBeConsumed(true);
    generatorExtension.setCanBeResolved(false);
    generatorExtension.setDescription("Published generation modifications of the module");

    addGeneratorExtensionJarAttributes(generatorExtension, project);

    SourceSet generatorExtensionSourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
        .getSourceSets().create("generatorExtension");
    generatorExtensionSourceSet.setCompileClasspath(
        generatorExtensionSourceSet.getCompileClasspath().plus(generationExtensionCompile)
    );
    TaskProvider<Jar> generatorExtensionJar = project.getTasks().register("generatorExtensionJar", Jar.class, jar -> {
      jar.setGroup("build");
      jar.getArchiveClassifier().set("module-generator-time");
      jar.from(generatorExtensionSourceSet.getOutput());
      jar.setPreserveFileTimestamps(false);
      jar.setReproducibleFileOrder(true);

      jar.onlyIf(t -> generatorExtensionSourceSet.getAllSource().getFiles().stream().anyMatch(File::exists));
    });
    project.getTasks().named(ASSEMBLE_TASK_NAME).configure(it -> it.dependsOn(generatorExtensionJar));
    project.afterEvaluate(p -> {
      if (!generatorExtensionSourceSet.getAllSource().isEmpty()) {
        project.getArtifacts().add(generatorExtension.getName(), generatorExtensionJar);
        javaComponent.addVariantsFromConfiguration(generatorExtension, ConfigurationVariantDetails::mapToOptional);

        // Tell gradle, that the jar tasks builds a publication
        var genTimePublication = createPublishedArtifact(generatorExtensionJar, project);
        // Add the publication as an artifact
        generatorExtension.getOutgoing().getArtifacts().add(genTimePublication);
      }
      else {
        Log.info("Skipping artifact: no sources in source set", getClass().getName());
      }
    });

    TaskProvider<UMLP2SymTask> umlp2sym = project.getTasks().register(UMLP2SYM_TASK, UMLP2SymTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getModelDir().from(models);
      task.getInput().from(models);
      if (!inRTE(project)) {
        task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
        task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_IMPORT_MAP_CONFIG));
      }
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("umlp2sym"));

      task.onlyIf(t -> umlpPresent);
    });

    TaskProvider<Jar> umlp2symJar = project.getTasks().register("umlp2symJar", Jar.class, task -> {
      task.setGroup("build");
      task.getArchiveClassifier().set("domainsymbols");
      task.setPreserveFileTimestamps(false);
      task.setReproducibleFileOrder(true);
    });
    project.getTasks().named(ASSEMBLE_TASK_NAME).configure(it -> it.dependsOn(umlp2symJar));

    if (umlpPresent) {
      // Create the domain symbols jar
      Configuration umlp2symConfig = project.getConfigurations().create(UMLP2SYM_CONFIG);
      umlp2symConfig.setCanBeConsumed(true);
      umlp2symConfig.setCanBeResolved(false);
      umlp2symConfig.setDescription("Published domain Symbols of the module");

      addUMLPSymbolsJarAttributes(umlp2symConfig, project);

      // Add umlp2symJar-jar as artifact to the config
      project.getArtifacts().add(umlp2symConfig.getName(), umlp2symJar);
      javaComponent.addVariantsFromConfiguration(umlp2symConfig, ConfigurationVariantDetails::mapToOptional);

      // Tell gradle, that the jar tasks builds a publication
      // and add the publication to the outgoing artifacts
      umlp2symConfig.getOutgoing().getArtifacts().add(
          createPublishedArtifact(umlp2symJar, project)
      );
    }

    umlp2symJar.configure(task -> {
      task.from(
          umlp2sym.flatMap(UMLP2SymTask::getOutputDir),
          spec -> spec.include("**/*sym", "**/*tags")
      );

      task.onlyIf(t -> umlpPresent);
    });

    Project frontend = project.findProject(project.getPath() + ":frontend");
    if (frontend != null) {
      project.getDependencies().add("api", frontend);
      project.getTasks().named("build").configure(t -> {
        Task frontendBuild = frontend.getTasks().findByName("build");
        if (frontendBuild != null) {
          t.dependsOn(frontendBuild);
        }
      });

      Configuration gui2symConfig = project.getConfigurations().create(GUI2SYM_CONFIG, conf -> {
        conf.setCanBeConsumed(true);
        addGuiSymbolsJarAttributes(conf, project);
      });
      project.getDependencies().add(gui2symConfig.getName(), frontend);
      javaComponent.addVariantsFromConfiguration(gui2symConfig, ConfigurationVariantDetails::mapToOptional);

      Configuration gui2javaConfig = project.getConfigurations().create(GUI2JAVA_CONFIG, conf -> {
        conf.setCanBeConsumed(true);
        addImportMapJarAttributes(conf, project);
      });
      project.getDependencies().add(gui2javaConfig.getName(), frontend);
      javaComponent.addVariantsFromConfiguration(gui2javaConfig, ConfigurationVariantDetails::mapToOptional);
    }

  }

  public static void addUMLPModelsJarAttributes(Configuration config, Project project) {
    config.attributes(it -> {
      it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "montigem-module-models"));
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    });
  }

  public static void addGeneratorExtensionJarAttributes(Configuration config, Project project) {
    config.attributes(it -> {
      it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "montigem-module-gentime"));
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    });
  }

}
