/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle;

import de.monticore.gradle.common.CommonMCTask;
import de.montigem.gradle.specific.CD2GUITask;
import de.montigem.gradle.specific.CreateSymlinkTask;
import de.montigem.gradle.specific.GUI2Java2TSTask;
import de.montigem.gradle.specific.GUIVariabilityTask;
import de.montigem.gradle.specific.SetupFrontendTask;
import de.montigem.gradle.specific.UMLP2JSweetObserverTask;
import de.montigem.gradle.specific.UMLP2SymTask;
import de.montigem.gradle.specific.UMLPJava2TSTask;
import de.montigem.gradle.specific.cd.CD2JavaTask;
import de.montigem.gradle.specific.merge.MergeTask;
import de.montigem.gradle.specific.serialize.CD2SymTask;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.VerificationType;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.montigem.gradle.BackendPlugin.MERGE_UMLP_DIR;
import static de.montigem.gradle.BackendPlugin.MERGE_UMLP_TASK;
import static de.montigem.gradle.BackendPlugin.OCL2JAVA_TASK;
import static de.montigem.gradle.BackendPlugin.UMLP2JAVA_TASK;
import static de.montigem.gradle.FrontendBasePlugin.CREATE_SYMLINK_TASK;
import static de.montigem.gradle.FrontendBasePlugin.GUI_TYPES_CONFIG;
import static de.montigem.gradle.FrontendBasePlugin.SETUP_FRONTEND_TASK;
import static de.montigem.gradle.FrontendBasePlugin.UMLP2SYM_TASK;
import static de.montigem.gradle.FrontendBasePlugin.UMLP_GROUP_NAME;
import static de.montigem.gradle.FrontendBasePlugin.getModels;
import static de.montigem.gradle.FrontendBasePlugin.getProjectProperty;
import static de.montigem.gradle.FrontendBasePlugin.inRTE;
import static de.montigem.gradle.FrontendBasePlugin.inUMLP;
import static de.montigem.gradle.FrontendBasePlugin.useLocalDependencies;
import static de.montigem.gradle.FrontendBasePlugin.umlpVersion;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_IMPORT_MAP_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_UMLP_MODELS_CONFIG;

public abstract class FrontendPlugin implements Plugin<Project> {

  public static final String UMLP_JAVA2TS_TASK = "umlpJava2ts";
  public static final String SYNC_UMLP_JAVA2TS_TASK = "sync_umlpJava2ts";
  public static final String SYNC_GUI2JAVA2TS_TASK = "sync_gui2java2ts";
  public static final String UMLP2JSWEET_OBSERVER_TASK = "umlp2jsweetObserver";
  public static final String GUI2JAVA2TS_TASK = "gui2java2ts";
  public static final String CD2GUI_TASK = "cd2gui";
  public static final String GUI_VARIABILITY = "guiVariability";
  public static final String MERGE_GUI_MODELS_TASK = "mergeGuiModels";
  public static final String CD2JAVA_TASK = "cd2java";
  public static final String CD2SYM_TASK = "cd2sym";

  public static final String SRC_UMLP_CONFIG = "srcUMLPConfig";
  public static final String JSWEET_CONFIG = "jsweetConfig";

  public static List<String> defaultExcludedSources = List.of(
      "glob:**/shellapi/*ShellAPI.java",
      "glob:**/Abstract*E2ETest*.java",
      "glob:**/test_infrastructure/*.java",

      "glob:**/*ManagerImpl.java",
      "glob:**/*TestSetup.java",
      "glob:**/*DummyProvider.java",
      "glob:**/*ServerApplication.java",
      "glob:**/*DAO.java",
      "glob:**/*DAOFactory.java",
      "glob:**/*DAONeo4J.java",
      "glob:**/*DAOInterface.java",
      "glob:**/*DBObserver.java"
  );

  @Override
  public void apply(Project project) {
    // setDirectories(project);
    project.getPluginManager().apply(FrontendBasePlugin.class);

    FrontendPluginExtension extension = project.getExtensions()
        .create("montigem", FrontendPluginExtension.class);

    Directory models = getModels(project);
    // default name(s) of .umlp file(s)

    SourceSet mainSourceSet = project.getExtensions()
        .getByType(SourceSetContainer.class)
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    setupJSweetClasspath(project);

    TaskProvider<SetupFrontendTask> setupFrontend = project.getTasks().named(SETUP_FRONTEND_TASK, SetupFrontendTask.class);

    Provider<Boolean> withCD2GUI = extension.getWithCD2GUI();
    Provider<Directory> cd2guiDir = project.getLayout().getBuildDirectory().dir("cd2gui");
    Provider<Directory> guiVariabilityDir = project.getLayout().getBuildDirectory().dir("guiVariability");
    Provider<Directory> guiModelsDir = project.getLayout().getBuildDirectory().dir("guiModels");
    Provider<Directory> cd2javaDir = project.getLayout().getBuildDirectory().dir("cd2java");
    Provider<Directory> cd2tagsDir = project.getLayout().getBuildDirectory().dir("cd2tags");
    Provider<Directory> gui2jsweetAdapterDir = project.getLayout().getBuildDirectory().dir("gui2jsweetAdapter");
    Provider<Directory> cd2jsweetAdapterDir = project.getLayout().getBuildDirectory().dir("cd2jsweetAdapter");
    Provider<Directory> internalJSweetDir = project.getLayout().getBuildDirectory().dir("internal_jsweet");
    Provider<Directory> umlp2jsweetAdapterDir = project.getLayout().getBuildDirectory().dir("umlp2jsweetAdapter");
    Provider<Directory> umlp2jsweetObserverDir = project.getLayout().getBuildDirectory().dir("umlp2jsweetObserver");
    Provider<RegularFile> hwcLink = project.getLayout().getBuildDirectory().file("app/hwc");
    Provider<RegularFile> intermediateHWCLink = project.getLayout().getBuildDirectory().file("hwc");
    Provider<Directory> gui2symDir = project.getLayout().getBuildDirectory().dir("gui2sym");
    Provider<Directory> gui2java2tsDir = project.getLayout().getBuildDirectory().dir("gui2java2ts");
    Provider<Directory> umlpJava2tsDir = project.getLayout().getBuildDirectory().dir("umlpJava2ts");
    Provider<Directory> syncGUI2Java2TSOut = project.getLayout().getBuildDirectory().dir("app/gui2java2ts");
    Provider<Directory> syncUMLPJava2TSOut = project.getLayout().getBuildDirectory().dir("app/umlpJava2ts");

    Directory srcJSweet = project.getLayout().getProjectDirectory().dir("src/main/jsweet");
    Directory srcUMLPJSweet = srcJSweet.dir("umlp2jsweetAdapter");
    Directory srcGUIJSweet = srcJSweet.dir("gui2jsweetAdapter");
    Directory srcGUI = project.getLayout().getProjectDirectory().dir("src/main/gui");
    Directory srcJava = project.getLayout().getProjectDirectory().dir("src/main/java");

    Optional<Project> parent = Optional.ofNullable(project.getParent());
    Optional<Project> backend = parent.map(value -> value.findProject(value.getPath() + ":backend"));

    TaskProvider<? extends Task> mergeUMLP;
    if (backend.isPresent()) {
      mergeUMLP = backend.get().getTasks().named(MERGE_UMLP_TASK);
      project.getDependencies().add("implementation", backend.get());
      project.getDependencies().add(SRC_UMLP_CONFIG, backend.get());
    }
    else {
      mergeUMLP = project.getTasks().register(MERGE_UMLP_TASK, MergeTask.class, task -> {
        task.source(models);
        task.getOutputFile().set(project.getLayout().getBuildDirectory().file(MERGE_UMLP_DIR));

        task.onlyIf(t -> ((MergeTask) t).getSource().getFiles().size() > 1);
      });
    }

    var umlpFile = mergeUMLP.map(task -> {
      SourceTask t = (SourceTask) task;
      if (t.getSource().getFiles().size() == 1) {
        return t.getSource().getFiles().iterator().next();
      }
      else {
        return ((MergeTask)t).getOutputFile().get();
      }
    });

    var umlpName = mergeUMLP.map(task -> {
      SourceTask t = (SourceTask) task;
      if (t.getSource().getFiles().size() == 1) {
        return FileUtils.removeExtension(t.getSource().getFiles().iterator().next().getName());
      }
      else {
        return FileUtils.removeExtension(((MergeTask)t).getOutputFile().getAsFile().get().getName());
      }
    });

    var oclFiles = models.dir("ocl").getAsFileTree().matching(pf -> pf.include("**/*.ocl"));
    TaskProvider<CD2GUITask> cd2gui = project.getTasks().register(CD2GUI_TASK, CD2GUITask.class);
    cd2gui.configure(task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.dependsOn(mergeUMLP);
      task.getInput().from(umlpFile);
      task.getOutputDir().set(cd2guiDir);
      task.getOcl().from(oclFiles);

      task.onlyIf(t -> withCD2GUI.get());
    });

    TaskProvider<GUIVariabilityTask> guiVariability = project.getTasks().register(GUI_VARIABILITY, GUIVariabilityTask.class);
    guiVariability.configure(task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getInput().from(cd2gui.flatMap(CommonMCTask::getOutputDir));
      task.getInput().from(srcGUI);
      task.getOutputDir().set(guiVariabilityDir);

      task.onlyIf(t -> withCD2GUI.get());
    });

    // GUIVariability
    TaskProvider<Sync> mergeGuiModels = project.getTasks().register(MERGE_GUI_MODELS_TASK, Sync.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.from(cd2gui.flatMap(CommonMCTask::getOutputDir), spec -> spec.include("**/*.gui"));
      task.from(guiVariability.flatMap(CommonMCTask::getOutputDir), spec -> spec.include("**/*.gui"));
      task.into(guiModelsDir);
      task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
    });

    TaskProvider<CreateSymlinkTask> createSymlinkTask = project.getTasks().named(CREATE_SYMLINK_TASK, CreateSymlinkTask.class);

    boolean cdPresent;
    File folder = srcGUI.getAsFile();
    if (folder.exists()) {
      Set<File> cdFiles = project.fileTree(folder, spec -> {
        spec.include("**/*.cd");
      }).getFiles();
      cdPresent = !cdFiles.isEmpty();
    }
    else {
      cdPresent = false;
    }

    TaskProvider<UMLP2SymTask> umlp2sym = project.getTasks().named(UMLP2SYM_TASK, UMLP2SymTask.class);
    umlp2sym.configure(task -> {
      task.dependsOn(mergeUMLP);
      task.getInput().setFrom(umlpFile);
    });

    TaskProvider<UMLPJava2TSTask> umlpJava2ts = project.getTasks().register(UMLP_JAVA2TS_TASK, UMLPJava2TSTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.dependsOn(mergeUMLP);

      task.getDomain().set(Collections.emptyList());
      task.getDomain().add(umlpName);
      task.getJSweetAdapterDir().set(umlp2jsweetAdapterDir);
      if (srcUMLPJSweet.getAsFile().exists()) {
        task.getHandWrittenJSweetDir().set(srcUMLPJSweet);
      }

      task.getJSweetWorkingDir().set(internalJSweetDir.map(dir -> dir.dir("umlpJava2ts")));
      task.getJSweetCandiesDir().set(internalJSweetDir.map(dir -> dir.dir("umlpCandies")));
      task.getOutputDir().set(umlpJava2tsDir);

      // add domain-sources to the to-be-transpiled
      task.getInput().from(project.getConfigurations().named(SRC_UMLP_CONFIG));
      task.getInput4RelativePathCalculation().from(srcGUI.getAsFileTree().matching(
          patternFilter -> patternFilter.include("**/*.ts")
      ));
      task.getJSweetClassPath().from(project.getConfigurations().named(JSWEET_CONFIG));

      for (String s : defaultExcludedSources) {
        task.getExcludeSource().add(s);
      }
    });

    if (backend.isPresent()) {
      umlpJava2ts.configure(t -> t.dependsOn(backend.get().getTasks().named(UMLP2JAVA_TASK)));
      umlpJava2ts.configure(t -> t.dependsOn(backend.get().getTasks().named(OCL2JAVA_TASK)));
    }

    TaskProvider<CD2JavaTask> cd2java = project.getTasks().register(CD2JAVA_TASK, CD2JavaTask.class);
    cd2java.configure(task -> {
      task.setGroup(UMLP_GROUP_NAME);
      if (srcJava.getAsFile().exists()) {
        task.getHandWrittenCodeDir().from(srcJava);
      }
      task.getInput().from(srcGUI);
      task.getOutputDir().set(cd2javaDir);
      task.getSymbolOutDir().set(cd2tagsDir);

      task.onlyIf(t -> cdPresent);
    });
    mainSourceSet.getJava().srcDir(cd2java.flatMap(CommonMCTask::getOutputDir));

    TaskProvider<CD2SymTask> cd2sym = project.getTasks().register(CD2SYM_TASK, CD2SymTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getInput().set(srcGUI);
      task.getOutput().set(project.getLayout().getBuildDirectory().dir("cd2sym"));
      task.onlyIf(t -> cdPresent);
    });

    TaskProvider<GUI2Java2TSTask> gui2java2ts = project.getTasks().register(GUI2JAVA2TS_TASK, GUI2Java2TSTask.class);
    gui2java2ts.configure(task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.dependsOn(createSymlinkTask, setupFrontend, mergeUMLP);

      task.getDomain().set(Collections.emptyList());
      task.getDomain().add(umlpName);
      if (!inRTE(project)) {
        task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
        task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_IMPORT_MAP_CONFIG));
      }
      task.getImportsDir().from(umlpJava2ts.flatMap(UMLPJava2TSTask::getOutputDir));
      if (withCD2GUI.get()) {
        task.getSymbolPath().from(cd2gui.map(t -> t.getOutputDir().file("symbols")));
      }
      else {
        task.getSymbolPath().from(umlp2sym.flatMap(CommonMCTask::getOutputDir));
      }
      task.getSymbolPath().from(project.getConfigurations().named(GUI_TYPES_CONFIG));
      task.getSymbolPath().from(cd2sym.flatMap(CD2SymTask::getOutput));
      task.getSymbolPath().from(project.getLayout().getBuildDirectory().dir("tags"));
      task.getInput().from(srcGUI);
      task.getInput().from(mergeGuiModels);
      if (cdPresent) {
        task.getInput().from(cd2java.flatMap(CommonMCTask::getOutputDir));
        task.getCD2JavaInput().from(cd2java.flatMap(CommonMCTask::getOutputDir));
        task.getCD2JavaJSweetAdapterDir().set(cd2jsweetAdapterDir);
      }
      task.getGuiCreateModelsAsFile().set(
          withCD2GUI.flatMap(v -> {
            if (v) {
              return cd2gui.flatMap(t ->
                  umlpName.flatMap(n ->
                      t.getOutputDir().file("meta/" + n.toLowerCase() + "/guiCreateTextModels")));
            }
            return project.getObjects().fileProperty();
          })
      );
      task.getModelDir().from(srcGUI);
      task.getModelDir().from(mergeGuiModels);
      task.getHandWrittenCodeDir().from(intermediateHWCLink);
      task.getOutputDir().set(gui2java2tsDir);
      task.getSymbolOutDir().set(gui2symDir);

      task.getGUIGenJSweet().set(true);
      task.getGUIGenTSFunctionality().set(false);

      task.getJSweetAdapterDir().set(gui2jsweetAdapterDir);
      if (srcGUIJSweet.getAsFile().exists()) {
        task.getHandWrittenJSweetDir().set(srcGUIJSweet);
      }

      if (srcJava.getAsFile().exists()) {
        task.getHandWrittenCodeDir().from(srcJava);
      }
      if (withCD2GUI.get()) {
        task.getHandWrittenCodeDir().from(cd2gui.flatMap(t -> t.getOutputDir().dir("guiImpl")));
      }
      task.getInput4RelativePathCalculation().from(cd2java.flatMap(CommonMCTask::getOutputDir));
      task.getInput4RelativePathCalculation().from(umlpJava2ts.flatMap(UMLPJava2TSTask::getOutputDir));

      task.getJSweetWorkingDir().set(internalJSweetDir.map(dir -> dir.dir("gui2java2ts")));
      task.getJSweetCandiesDir().set(internalJSweetDir.map(dir -> dir.dir("guiCandies")));
      task.getJSweetClassPath().from(project.getConfigurations().named(JSWEET_CONFIG));

      // TODO: Remove the following at some point
      // required with getExcludeTranspile - otherwise "import { Project } from 'dtcockpit/Project';" is missing in GUI ts files
      // Add the domain-sources as a java-source
      task.getInput4RelativePathCalculation().from(project.getConfigurations().named(SRC_UMLP_CONFIG));
      // but exclude them from being transpiled (thus, only available as source files)
      task.getExcludeTranspileFiles().from(project.getConfigurations().named(SRC_UMLP_CONFIG));
      // TODO: END OF REMOVAL

      // task.getOutputDir().set(guiJava2tsDir);

      // TODO: Fix ShellAPI imports
      // Required due to "imports cannot use * wildcards when using modules"
      for (String s : defaultExcludedSources) {
        task.getExcludeSource().add(s); // via @Erased
      }
    });
    mainSourceSet.getJava().srcDir(gui2java2ts.flatMap(CommonMCTask::getOutputDir));

    TaskProvider<UMLP2JSweetObserverTask> umlp2jsweetObserver = project.getTasks().register(
        UMLP2JSWEET_OBSERVER_TASK, UMLP2JSweetObserverTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.dependsOn(mergeUMLP);
          task.getInput().from(umlpFile);
          task.getOutputDir().set(umlp2jsweetObserverDir);
          task.getHandWrittenCodeDir().from(srcGUI);
          task.getSymbolPath().from(cd2gui.map(t -> t.getOutputDir().dir("symbols")));
          task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
          task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_IMPORT_MAP_CONFIG));
          task.getModelDir().from(project.getConfigurations().named(MONTIGEM_MODULE_UMLP_MODELS_CONFIG));
        });

    addJSweetDependencies(project);

    // Sync Output of a task to the correct directory. This done because "npm serve" cannot
    // handle the intermediate state while the UMLPTasks are running and Hot-Reload aborts
    TaskProvider<Sync> syncUMLPJava2TS = project.getTasks().register(SYNC_UMLP_JAVA2TS_TASK, Sync.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.from(umlpJava2ts.flatMap(UMLPJava2TSTask::getOutputDir));
      task.into(syncUMLPJava2TSOut);
    });

    TaskProvider<Sync> syncGUI2Java2TS = project.getTasks().register(SYNC_GUI2JAVA2TS_TASK, Sync.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.from(gui2java2ts.flatMap(CommonMCTask::getOutputDir));
      task.from(umlp2jsweetObserver.flatMap(CommonMCTask::getOutputDir));
      task.into(syncGUI2Java2TSOut);
      task.exclude("**/*.java");
    });
    TaskProvider<Task> generate = project.getTasks().named("generate");
    generate.configure(task -> task.dependsOn(gui2java2ts, syncUMLPJava2TS, syncGUI2Java2TS));
    project.getTasks().named("compileJava").configure(t -> t.dependsOn(generate));

    createSymlinkTask.configure(task -> {
      task.getLinks().put(
          intermediateHWCLink.get().getAsFile(),
          srcGUI.getAsFile()
      );

      task.getLinks().put(
          hwcLink.get().getAsFile(),
          srcGUI.getAsFile()
      );
    });
  }

  public static void setupJSweetClasspath(Project project) {
    project.getConfigurations().register(SRC_UMLP_CONFIG, config -> {
      config.setTransitive(false);
      config.attributes(at -> {
        at.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
            project.getObjects()
                .named(VerificationType.class, VerificationType.MAIN_SOURCES));
        at.attribute(Bundling.BUNDLING_ATTRIBUTE,
            project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      });
    });

    // Configuration containing the compile/transpile dependencies (for running jsweet)
    project.getConfigurations().register(JSWEET_CONFIG, config -> {
      config.extendsFrom(project.getConfigurations().getByName("compileClasspath"));
    });

    if (inUMLP(project) && useLocalDependencies(project)) {
      project.getDependencies().add(JSWEET_CONFIG, project.project(":umlp-jsweet"));
      project.getDependencies().add("runtimeOnly", project.project(":umlp-jsweet"));
    }
    else {
      project.getDependencies().add(JSWEET_CONFIG, "umlp:umlp-jsweet:" + umlpVersion(project));
    }
  }

  protected void addJSweetDependencies(Project project) {
    // JSweet
    MavenArtifactRepository snapshotRepo = project.getRepositories().maven(repo -> {
      repo.setUrl("https://repository.jsweet.org/artifactory/libs-snapshot-local");
    });
    MavenArtifactRepository releaseRepo = project.getRepositories().maven(repo -> {
      repo.setUrl("https://repository.jsweet.org/artifactory/libs-release-local");
    });

    // Only use jsweet repos for everything in org.jsweet*(excl jsweet-core which is in maven-central)
    // since they throw 409 errors for dependencies in other groups
    Action<MavenRepositoryContentDescriptor> filterContent = it -> it.includeModuleByRegex("org[.]jsweet.*", "(?!jsweet-core).*");
    snapshotRepo.mavenContent(filterContent);
    releaseRepo.mavenContent(filterContent);

    project.getDependencies().add("implementation", "org.jsweet:jsweet-core:6.3.0");
    Dependency dep = project.getDependencies().create("de.rwth.se.umlp.jsweet:jsweet-transpiler:3.2.5-UMLP-SNAPSHOT", new Closure<Object>(this, this) {
      @Override
      public Object call(Object arg) {
        var emd = (ExternalModuleDependency) arg;
        emd.exclude(Map.of(
            "group", "com.kohlschutter.jdk",
            "module", "standalone-home"
        ));
        return arg;
      }

      @Override
      public int getMaximumNumberOfParameters() {
        return 1;
      }
    });
    project.getDependencies().add("implementation", dep);
    project.getDependencies().add("implementation", "com.kohlschutter.jdk.compiler:standalone-jdk21:1.1.3");
    if (inUMLP(project) && useLocalDependencies(project)) {
      project.getDependencies().add(JSWEET_CONFIG, project.project(":umlp-jsweet"));
    }
    else {
      project.getDependencies().add("implementation", "umlp:umlp-jsweet:" + umlpVersion(project));
    }

    // Add dependency to jsweet-core, to import the annotations (such as @Erased)
    // Only during compilation of the backend
    // TODO: Use constant for jsweet-core
    project.getDependencies().add("compileOnly", "org.jsweet:jsweet-core:6.3.0");
  }

  public static String getStringProjectPropertyStrip(Project project, String propertyName, String defaultValue) {
    String stringProjectProperty = getProjectProperty(project, propertyName).orElse(defaultValue);
    return stringProjectProperty
        .replace("'", "")
        .replace("\"", "")
        .strip();
  }

}
