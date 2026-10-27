/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle;

import de.monticore.gradle.common.CommonMCTask;
import de.montigem.gradle.specific.UMLP2JavaTask;
import de.montigem.gradle.specific.merge.MergeTask;
import de.montigem.gradle.specific.ocl.OCL2JavaTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.internal.FileUtils;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.util.Optional;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static de.montigem.gradle.FrontendBasePlugin.UMLP_GROUP_NAME;
import static de.montigem.gradle.FrontendBasePlugin.getModels;
import static de.montigem.gradle.FrontendBasePlugin.inUMLP;
import static de.montigem.gradle.FrontendBasePlugin.mcVersion;
import static de.montigem.gradle.FrontendBasePlugin.useLocalDependencies;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG;
import static de.montigem.gradle.module.ModulePlugin.GENERATOR_EXTENSION_COMPILE_CONFIG;

/***
 * MontiGem app backend generation.
 * - Sets minimal dependencies
 * - Adds tasks:
 *   - `umlp2java`
 *   - `generate`
 *   - `continuos`
 *   - `slowtest`
 */
public abstract class BackendPlugin implements Plugin<Project> {

  public final static String UMLP2JAVA_TASK = "umlp2java";
  public final static String OCL2JAVA_TASK = "ocl2java";
  public final static String MERGE_UMLP_TASK = "mergeUMLP";

  public static final String MERGE_UMLP_DIR = "mergeUMLP/Domain.umlp";
  public static final String BOOT_JAR_INCLUDE_GROUP_PROPERTY = "bootJarIncludeGroup";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(MGBasePlugin.class);
    project.getPluginManager().apply("java-library");
    project.getPluginManager().apply("application");
    configureBootJarLibraryNames(project);

    var mainSourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
        .getSourceSets().named(SourceSet.MAIN_SOURCE_SET_NAME);
    addRTEDependency(project);

    // set up generator extension source set for the cases when an app needs one
    Configuration generationExtensionCompile = project.getConfigurations().create(GENERATOR_EXTENSION_COMPILE_CONFIG);
    generationExtensionCompile.setCanBeResolved(true);
    generationExtensionCompile.setCanBeConsumed(false);
    project.getDependencies().add(generationExtensionCompile.getName(), "de.monticore:monticore-runtime:" + mcVersion(project));
    project.getDependencies().add(generationExtensionCompile.getName(), "de.monticore:monticore-grammar:" + mcVersion(project));
    project.getDependencies().add(generationExtensionCompile.getName(), "umlp:umlp-tool:" + mcVersion(project));

    SourceSet generatorExtensionSourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
        .getSourceSets().create("generatorExtension");
    generatorExtensionSourceSet.setCompileClasspath(
        generatorExtensionSourceSet.getCompileClasspath().plus(generationExtensionCompile)
    );

    project.getTasks()
        .named(generatorExtensionSourceSet.getCompileJavaTaskName(), JavaCompile.class)
        .configure(task -> task.onlyIf(
            t -> generatorExtensionSourceSet.getJava().getSrcDirs().stream()
                .anyMatch(dir -> dir.exists() && dir.listFiles() != null && dir.listFiles().length > 0)
        ));

    Directory models = getModels(project);

    TaskProvider<MergeTask> mergeUMLP = project.getTasks().register(MERGE_UMLP_TASK, MergeTask.class, task -> {
      task.source(models);
      task.getOutputFile().set(project.getLayout().getBuildDirectory().file(MERGE_UMLP_DIR));

      task.onlyIf(t -> ((MergeTask) t).getSource().getFiles().size() > 1);
    });

    var umlpFile = mergeUMLP.map(t -> {
      if (t.getSource().getFiles().size() == 1) {
        return t.getSource().getFiles().iterator().next();
      }
      else {
        return t.getOutputFile().get();
      }
    });

    var umlpName = mergeUMLP.map(t -> {
      if (t.getSource().getFiles().size() == 1) {
        return FileUtils.removeExtension(t.getSource().getFiles().iterator().next().getName());
      }
      else {
        return FileUtils.removeExtension(t.getOutputFile().getAsFile().get().getName());
      }
    });

    project.getExtensions()
        .getByType(JavaApplication.class)
        .getMainClass()
        .set(umlpName.map(s -> s.toLowerCase() + "." + s + "ServerApplication"));

    Directory srcJava = project.getLayout().getProjectDirectory().dir("src/main/java");
    Directory srcTpl = project.getLayout().getProjectDirectory().dir("src/generatorExtension/resources");
    var oclFiles = models.dir("ocl").getAsFileTree().matching(pf -> pf.include("**/*.ocl"));
    TaskProvider<UMLP2JavaTask> umlp2java = project.getTasks().register(UMLP2JAVA_TASK, UMLP2JavaTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);

      task.getInput().from(umlpFile);
      task.getOcl().from(oclFiles);
      if (srcJava.getAsFile().exists()) {
        task.getHandWrittenCodeDir().from(srcJava);
      }
      if (srcTpl.getAsFile().exists()) {
        task.getTmplDir().set(srcTpl);
      }
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("umlp2java"));
      task.getSymbolOutDir().set(project.getLayout().getBuildDirectory().dir("umlp2sym"));
      task.getModelDir().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
    });

    TaskProvider<OCL2JavaTask> ocl2java = project.getTasks().register(OCL2JAVA_TASK, OCL2JavaTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getInput().from(oclFiles);
      task.getSymbolPath().from(umlp2java.flatMap(UMLP2JavaTask::getSymbolOutDir));
      task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("ocl2java"));
      task.getDomainModels().add(umlpName);
      task.getPrefixPackage().set(umlpName.map(String::toLowerCase));

      task.onlyIf(t -> ((OCL2JavaTask) t).getInput().getAsFileTree().getFiles().stream().anyMatch(f -> f.getName().endsWith(".ocl")));
    });

    mainSourceSet.configure(sourceSet -> {
      project.getTasks().withType(AbstractCompile.class)
          .matching(t -> t.getName().equals(sourceSet.getCompileJavaTaskName()))
          .configureEach(compile -> {
            compile.dependsOn(umlp2java);
            compile.dependsOn(ocl2java);
          });

      // Generate Backend must run before ProcessResources (e.g. for SQL schemata)
      project.getTasks().withType(ProcessResources.class)
          .matching(t -> t.getName().equals(sourceSet.getProcessResourcesTaskName()))
          .configureEach(procResMain -> {
            procResMain.dependsOn(umlp2java);
            procResMain.dependsOn(ocl2java);
          });

      sourceSet.getJava().srcDir(umlp2java.flatMap(CommonMCTask::getOutputDir));
      sourceSet.getJava().srcDir(ocl2java.flatMap(CommonMCTask::getOutputDir));
      // Add generated resources. e.g. SQL-Schemata
      sourceSet.getResources().srcDir(umlp2java.flatMap(t -> t.getOutputDir().dir("resources")));
    });

    // Lifecycle task "generate" which depends on Backend-Creation
    project.getTasks().register("generate", task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.dependsOn(umlp2java);
      task.dependsOn(ocl2java);
    });

    // Lifecycle task "runServer" which depends on Backend-Creation
    project.getTasks().register("runServer", task -> {
      task.setGroup("application");
      task.dependsOn(project.provider(() -> project.getTasks().named("bootRun")));
    });

    // Lifecycle task "continuous". Intended use is with "--continuous" flag of gradle & HotReload
    project.getTasks().register("continuous", task -> {
      task.setGroup("application");
      task.dependsOn(project.getTasks().withType(Jar.class));
    });

    // Add dependency to jsweet-core, to import the annotations (such as @Erased)
    // Only during compilation of the backend
    project.getDependencies().add("compileOnly", "org.jsweet:jsweet-core:6.3.0");

    project.getTasks().register("slowtest", Test.class, task -> {
      task.setGroup("verification");
      task.useJUnitPlatform(junitPlatform ->
          junitPlatform.includeTags("SlowTest")
      );
      task.shouldRunAfter(project.getTasks().named("test"));
    });

    project.getTasks().withType(Test.class).configureEach(test -> {
      test.testLogging(logging ->
          logging.events(
              TestLogEvent.PASSED,
              TestLogEvent.SKIPPED,
              TestLogEvent.FAILED,
              TestLogEvent.STANDARD_OUT,
              TestLogEvent.STANDARD_ERROR
          )
      );
      test.useJUnitPlatform();
    });

    // apply last, since the test infrastructure tasks import properties from backend tasks
    project.getPluginManager().apply("de.rwth.se.montigem-testing");
  }

  private static void configureBootJarLibraryNames(Project project) {
    project.getPluginManager().withPlugin("org.springframework.boot", plugin -> {
      var includeGroup = project.getProviders().gradleProperty(BOOT_JAR_INCLUDE_GROUP_PROPERTY)
          .map(Boolean::parseBoolean).orElse(true);
      var libraryGroups = project.provider(() -> {
        Map<File, String> groups = new HashMap<>();
        project.getConfigurations().getByName("runtimeClasspath").getResolvedConfiguration()
            .getResolvedArtifacts().forEach(artifact ->
                groups.put(artifact.getFile(), artifact.getModuleVersion().getId().getGroup()));
        return groups;
      });
      project.getTasks().named("bootJar", Jar.class).configure(task -> {
        task.getInputs().property(BOOT_JAR_INCLUDE_GROUP_PROPERTY, includeGroup);
        // Distinct libraries can have identical artifact names and versions across groups.
        task.eachFile(entry -> {
          if (includeGroup.get() && entry.getPath().startsWith("BOOT-INF/lib/")) {
            String group = libraryGroups.get().get(entry.getFile());
            if (group != null && !group.isEmpty()) {
              entry.setName(group + "-" + entry.getName());
            }
          }
        });
      });
    });
  }

  public static void addRTEDependency(Project project) {
    // Add Symbols and Tags of RTE ClassDiagrams to SymbolPath
    if (useLocalDependencies(project) && inUMLP(project)) {
      project.getDependencies().add(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG, project.project(":umlp-rte"));
    }
    else {
      project.getDependencies().add(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG, "umlp:umlp-rte:" + mcVersion(project));
    }
  }

}
