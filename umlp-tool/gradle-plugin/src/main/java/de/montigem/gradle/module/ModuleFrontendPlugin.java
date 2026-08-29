package de.montigem.gradle.module;

import com.github.gradle.node.NodeExtension;
import com.github.gradle.node.NodePlugin;
import com.github.gradle.node.npm.task.NpmTask;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.monticore.gradle.common.CommonMCTask;
import de.montigem.gradle.DependencyPlugin;
import de.montigem.gradle.MGBasePlugin;
import de.montigem.gradle.specific.CreateNpmrcTask;
import de.montigem.gradle.specific.GUI2Java2TSTask;
import de.montigem.gradle.specific.GUI2SymTask;
import de.montigem.gradle.specific.UMLP2SymTask;
import de.montigem.gradle.specific.UMLP2TSTask;
import de.montigem.gradle.specific.UMLPJava2TSTask;
import de.montigem.gradle.specific.cd.CD2JavaTask;
import de.montigem.gradle.specific.serialize.CD2SymTask;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.file.Directory;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.internal.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.montigem.gradle.FrontendBasePlugin.UMLP_GROUP_NAME;
import static de.montigem.gradle.FrontendBasePlugin.addClientRTEDependency;
import static de.montigem.gradle.FrontendBasePlugin.addGUITypesDependency;
import static de.montigem.gradle.FrontendBasePlugin.addProjectDependencies;
import static de.montigem.gradle.FrontendBasePlugin.getModels;
import static de.montigem.gradle.FrontendBasePlugin.inRTE;
import static de.montigem.gradle.FrontendBasePlugin.inUMLP;
import static de.montigem.gradle.FrontendBasePlugin.mcVersion;
import static de.montigem.gradle.FrontendBasePlugin.umlpVersion;
import static de.montigem.gradle.FrontendBasePlugin.useLocalDependencies;
import static de.montigem.gradle.FrontendPlugin.CD2JAVA_TASK;
import static de.montigem.gradle.FrontendPlugin.CD2SYM_TASK;
import static de.montigem.gradle.FrontendPlugin.GUI2JAVA2TS_TASK;
import static de.montigem.gradle.FrontendPlugin.JSWEET_CONFIG;
import static de.montigem.gradle.FrontendPlugin.SRC_UMLP_CONFIG;
import static de.montigem.gradle.FrontendPlugin.UMLP_JAVA2TS_TASK;
import static de.montigem.gradle.FrontendPlugin.defaultExcludedSources;
import static de.montigem.gradle.FrontendPlugin.setupJSweetClasspath;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_IMPORT_MAP_CONFIG;
import static de.montigem.gradle.module.ModulePlugin.UMLP2SYM_TASK;


public class ModuleFrontendPlugin extends ALazyPublishingPlugin implements Plugin<Project> {

  public static final String GUI2SYM_TASK = "gui2sym";
  public static final String UMLP2TS_TASK = "umlp2ts";
  public static final String GUI2SYM_JAR_TASK = "gui2symJar";
  public static final String BUILD_LIB_TASK = "buildLib";
  public static final String TEST_LIB_TASK = "testLib";

  @Inject
  public ModuleFrontendPlugin(SoftwareComponentFactory softwareComponentFactory, TaskDependencyFactory taskDependencyFactory) {
    super(softwareComponentFactory, taskDependencyFactory);
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(NodePlugin.class);
    project.getPluginManager().apply("java-library");
    project.getPluginManager().apply("maven-publish");
    if (!inRTE(project)) {
      project.getPluginManager().apply(MGBasePlugin.class);
    }
    else {
      project.getPluginManager().apply(DependencyPlugin.class);
    }

    addClientRTEDependency(project);

    AdhocComponentWithVariants javaComponent = (AdhocComponentWithVariants) project.getComponents().getByName("java");
    // contains e.g. [ project(":umlp-rte:frontend:arrange"), project(":umlp-rte:frontend:basic") ]
    ModuleFrontendPluginExtension extension = project.getExtensions()
        .create("montigem", ModuleFrontendPluginExtension.class);

    // create .npmrc near build.gradle for `npm install`
    TaskProvider<CreateNpmrcTask> npmrc4download = project.getTasks().register("npmrc4download", CreateNpmrcTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getNpmrcFile().set(project.getLayout().getProjectDirectory().file(".npmrc"));
    });
    TaskProvider<NpmTask> npmInstall = project.getTasks().named("npmInstall", NpmTask.class);
    npmInstall.configure(task -> task.dependsOn(npmrc4download));

    // default models folder
    Directory models = getModels(project);
    // default .umlp file(s)
    Set<File> umlp;
    // default name(s) of .umlp file(s)
    List<String> domainNames;

    if (models.getAsFile().exists()) {
      umlp = models.getAsFileTree().matching(spec -> spec.include("**/*.umlp")).getFiles();
      if (!umlp.isEmpty()) {
        domainNames = umlp.stream()
            .map(File::getName)
            .map(FileUtils::removeExtension)
            .collect(Collectors.toList());
      }
      else {
        domainNames = Collections.emptyList();
      }
    }
    else {
      umlp = new HashSet<>();
      domainNames = Collections.emptyList();
    }

    Optional<Project> parent = Optional.ofNullable(project.getParent());
    Optional<Project> backend;
    if (parent.isPresent()) {
      backend = Optional.ofNullable(parent.get()
          .findProject(parent.get().getPath() + ":backend"));
      if (backend.isPresent()) {
        project.getDependencies().add("implementation", backend.get());
      }
    }
    else {
      backend = Optional.empty();
    }

    // look for umlp2sym task in parent and take its output if parent present
    Optional<TaskProvider<UMLP2SymTask>> umlp2sym;
    if (parent.isPresent()) {
      umlp2sym = Optional.of(parent.get().getTasks().named(UMLP2SYM_TASK, UMLP2SymTask.class));
    }
    else {
      umlp2sym = Optional.empty();
    }

    // Create the import map jar
    TaskProvider<Jar> umlp2impJar = umlp2impInit(project, javaComponent);
    TaskProvider<Jar> gui2symJar = gui2symInit(project, javaComponent);

    List<TaskProvider<NpmTask>> buildSubprojects = new ArrayList<>();
    List<TaskProvider<NpmTask>> publishSubprojects = new ArrayList<>();
    List<TaskProvider<NpmTask>> testSubprojects = new ArrayList<>();

    SourceSet mainSourceSet = project.getExtensions()
        .getByType(SourceSetContainer.class)
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    if (!inRTE(project)) {
      if (inUMLP(project) && useLocalDependencies(project)) {
        project.getDependencies().add("implementation", project.project(":umlp-rte:backend"));
      }
      else {
        project.getDependencies().add("implementation", "umlp:umlp-rte:" + umlpVersion(project));
      }
    }

    addJSweetDependencies(project);

    project.afterEvaluate(p -> {
      for (Project subproject : extension.getLibs()) {
        subproject.getPluginManager().apply(NodePlugin.class);
        subproject.getPlugins().withId("com.github.node-gradle.node", plugin -> {
          NodeExtension node = subproject.getExtensions().getByType(NodeExtension.class);
          node.getNodeProjectDir().set(project.getLayout().getProjectDirectory());
        });
        if (!inRTE(subproject)) {
          subproject.getPluginManager().apply(MGBasePlugin.class);
        }
        else {
          subproject.getPluginManager().apply(DependencyPlugin.class);
        }

        // if java plugin is applied in build.gradle, disable compileJava for subprojects
        subproject.getTasks().matching(t -> t.getName().equals("compileJava"))
            .configureEach(t -> t.setEnabled(false));

        Provider<String> libQName = extension.getLibScope().map(v -> v + "/" + subproject.getName());
        Provider<Directory> libBuildDir = p.getLayout().getBuildDirectory().dir("dist/" + subproject.getName());
        Provider<Directory> gui2java2tsDir = subproject.getLayout().getBuildDirectory().dir("gui2java2ts");
        Provider<Directory> cd2javaDir = subproject.getLayout().getBuildDirectory().dir("cd2java");
        Provider<Directory> cd2tagsDir = subproject.getLayout().getBuildDirectory().dir("cd2tags");
        Provider<Directory> umlp2tsDir = subproject.getLayout().getBuildDirectory().dir("umlp2ts");
        Provider<Directory> gui2jsweetAdapterDir = subproject.getLayout().getBuildDirectory().dir("gui2jsweetAdapter");
        Provider<Directory> cd2jsweetAdapterDir = subproject.getLayout().getBuildDirectory().dir("cd2jsweetAdapter");
        Provider<Directory> internalJSweetDir = subproject.getLayout().getBuildDirectory().dir("internal_jsweet");
        Provider<Directory> umlpJava2tsDir = subproject.getLayout().getBuildDirectory().dir("umlpJava2ts");
        Provider<Directory> umlp2jsweetAdapterDir = subproject.getLayout().getBuildDirectory().dir("umlp2jsweetAdapter");

        Directory srcJSweet = subproject.getLayout().getProjectDirectory().dir("src/main/jsweet");
        Directory srcUMLPJSweet = srcJSweet.dir("umlp2jsweetAdapter");
        Directory srcGUIJSweet = srcJSweet.dir("gui2jsweetAdapter");
        Directory srcGUI = subproject.getLayout().getProjectDirectory().dir("src/main/gui");
        Directory srcJava = subproject.getLayout().getProjectDirectory().dir("src/main/java");

        Configuration guiTypes = addGUITypesDependency(subproject);
        addProjectDependencies(subproject);
        addClientRTEDependency(subproject);
        setupJSweetClasspath(subproject);
        addJSweetDependencies(subproject);

        if (backend.isPresent()) {
          subproject.getDependencies().add("implementation", backend.get());
          if (!inRTE(subproject)) {
            subproject.getDependencies().add(SRC_UMLP_CONFIG, backend.get());
          }
        }

        mainSourceSet.getJava().srcDir(srcJava);
        boolean guiPresent;
        boolean cdPresent;
        File folder = srcGUI.getAsFile();
        if (folder.exists()) {
          Set<File> guiFiles = subproject.fileTree(folder, spec -> {
            spec.include("**/*.gui");
          }).getFiles();
          guiPresent = !guiFiles.isEmpty();

          Set<File> cdFiles = subproject.fileTree(folder, spec -> {
            spec.include("**/*.cd");
          }).getFiles();
          cdPresent = !cdFiles.isEmpty();
        }
        else {
          guiPresent = false;
          cdPresent = false;
        }

        TaskProvider<UMLPJava2TSTask> umlpJava2ts = subproject.getTasks().register(UMLP_JAVA2TS_TASK, UMLPJava2TSTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);

          if (!domainNames.isEmpty()) {
            task.getDomain().set(domainNames);
          }

          task.getJSweetAdapterDir().set(umlp2jsweetAdapterDir);
          if (srcUMLPJSweet.getAsFile().exists()) {
            task.getHandWrittenJSweetDir().set(srcUMLPJSweet);
          }

          task.getJSweetWorkingDir().set(internalJSweetDir.map(dir -> dir.dir("umlpJava2ts")));
          task.getJSweetCandiesDir().set(internalJSweetDir.map(dir -> dir.dir("umlpCandies")));
          task.getOutputDir().set(umlpJava2tsDir);

          // add domain-sources to the to-be-transpiled
          task.getInput().from(subproject.getConfigurations().named(SRC_UMLP_CONFIG));
          task.getInput4RelativePathCalculation().from(srcGUI.getAsFileTree().matching(
              patternFilter -> patternFilter.include("**/*.ts")
          ));
          task.getJSweetClassPath().from(subproject.getConfigurations().named(JSWEET_CONFIG));

          for (String s : defaultExcludedSources) {
            task.getExcludeSource().add(s);
          }

          task.onlyIf(t -> !umlp.isEmpty());
        });

        gui2symJar.configure(task -> task.from(
            srcGUI,
            spec -> spec.include("**/*.tags")
        ));

        TaskProvider<CD2SymTask> cd2sym = subproject.getTasks().register(CD2SYM_TASK, CD2SymTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.getInput().set(srcGUI);
          // TODO: enable when CD2SymTask is replaced
        /*
        if (!inRTE(subproject) && local) {
          task.getSymbolPath().set(subproject.getConfigurations().named(GUI_LIB_NAME));
        }
        /**/
          task.getOutput().set(subproject.getLayout().getBuildDirectory().dir("cd2sym"));
          task.onlyIf(t -> cdPresent);
        });

        gui2symJar.configure(task -> {
          task.from(
              cd2sym.flatMap(CD2SymTask::getOutput),
              spec -> spec.include("**/*sym", "**/*import.map", "**/*tags")
          );
          task.dependsOn(cd2sym);
        });

        TaskProvider<GUI2SymTask> gui2sym = subproject.getTasks().register(GUI2SYM_TASK, GUI2SymTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.getModelDir().set(srcGUI);
          task.getInput().from(srcGUI);
          task.getSymbolPath().from(guiTypes);
          if (umlp2sym.isPresent()) {
            task.getSymbolPath().from(umlp2sym.get().flatMap(CommonMCTask::getOutputDir));
            task.dependsOn(umlp2sym.get());
          }
          task.getSymbolPath().from(cd2sym.flatMap(CD2SymTask::getOutput));
          if (!inRTE(subproject)) {
            task.getSymbolPath().from(subproject.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
            task.getSymbolPath().from(subproject.getConfigurations().named(MONTIGEM_MODULE_IMPORT_MAP_CONFIG));
          }
          task.getOutputDir().set(subproject.getLayout().getBuildDirectory().dir("gui2sym"));

          task.getTSlib().set(libQName);
          task.dependsOn(cd2sym);
          task.onlyIf(t -> guiPresent);
        });
        gui2symJar.configure(task -> {
          task.from(
              gui2sym.flatMap(GUI2SymTask::getOutputDir),
              spec -> spec.include("**/*sym", "**/*import.map", "**/*tags")
          );
          task.dependsOn(gui2sym);
        });

        TaskProvider<CD2JavaTask> cd2java = subproject.getTasks().register(CD2JAVA_TASK, CD2JavaTask.class);
        cd2java.configure(task -> {
          task.setGroup(UMLP_GROUP_NAME);
          if (srcJava.getAsFile().exists()) {
            task.getHandWrittenCodeDir().from(srcJava);
          }
          task.getInput().from(srcGUI);
          task.getOutputDir().set(cd2javaDir);
          task.getSymbolOutDir().set(cd2tagsDir);
          task.getTslib().set(libQName);

          task.onlyIf(t -> cdPresent);
        });

        mainSourceSet.getJava().srcDir(cd2java.flatMap(CD2JavaTask::getOutputDir));

        gui2symJar.configure(task -> {
          task.from(
              cd2java.flatMap(CD2JavaTask::getOutputDir),
              spec -> spec.include("**/*sym", "**/*import.map", "**/*tags")
          );
          task.from(
              cd2java.flatMap(CD2JavaTask::getSymbolOutDir),
              spec -> spec.include("**/*sym", "**/*import.map", "**/*tags")
          );
        });

        TaskProvider<GUI2Java2TSTask> gui2java2ts = subproject.getTasks().register(GUI2JAVA2TS_TASK, GUI2Java2TSTask.class);
        gui2java2ts.configure(task -> {
          task.setGroup(UMLP_GROUP_NAME);
          if (!inRTE(subproject)) {
            task.getSymbolPath().from(subproject.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));
            task.getSymbolPath().from(subproject.getConfigurations().named(MONTIGEM_MODULE_IMPORT_MAP_CONFIG));
          }
          if (!domainNames.isEmpty()) {
            task.getDomain().set(Collections.emptyList());
            task.getDomain().addAll(domainNames);
          }
          task.getImportsDir().from(umlpJava2ts.flatMap(UMLPJava2TSTask::getOutputDir));
          if (umlp2sym.isPresent()) {
            task.getSymbolPath().from(umlp2sym.get().flatMap(CommonMCTask::getOutputDir));
            task.dependsOn(umlp2sym.get());
          }
          task.getSymbolPath().from(guiTypes);
          task.getSymbolPath().from(cd2sym.flatMap(CD2SymTask::getOutput));
          task.getInput().from(srcGUI);
          if (cdPresent) {
            task.getInput().from(cd2java.flatMap(CommonMCTask::getOutputDir));
            task.getCD2JavaInput().from(cd2java.flatMap(CommonMCTask::getOutputDir));
            task.getCD2JavaJSweetAdapterDir().set(cd2jsweetAdapterDir);
          }
          task.getModelDir().from(srcGUI);
          task.getHandWrittenCodeDir().from(srcGUI);
          task.getOutputDir().set(gui2java2tsDir);
          task.getTSlib().set(libQName);

          task.getGUIGenJSweet().set(true);
          task.getGUIGenTSFunctionality().set(false);

          task.getJSweetAdapterDir().set(gui2jsweetAdapterDir);
          if (srcGUIJSweet.getAsFile().exists()) {
            task.getHandWrittenJSweetDir().set(srcGUIJSweet);
          }

          if (srcJava.getAsFile().exists()) {
            task.getHandWrittenCodeDir().from(srcJava);
          }
          task.getInput4RelativePathCalculation().from(cd2java.flatMap(CommonMCTask::getOutputDir));
          task.getInput4RelativePathCalculation().from(umlpJava2ts.flatMap(UMLPJava2TSTask::getOutputDir));

          task.getJSweetWorkingDir().set(internalJSweetDir.map(dir -> dir.dir("gui2java2ts")));
          task.getJSweetCandiesDir().set(internalJSweetDir.map(dir -> dir.dir("guiCandies")));
          task.getJSweetClassPath().from(subproject.getConfigurations().named(JSWEET_CONFIG));

          // TODO: Remove the following at some point
          // required with getExcludeTranspile - otherwise "import { Project } from 'dtcockpit/Project';" is missing in GUI ts files
          // Add the domain-sources as a java-source
          task.getInput4RelativePathCalculation().from(subproject.getConfigurations().named(SRC_UMLP_CONFIG));
          // but exclude them from being transpiled (thus, only available as source files)
          task.getExcludeTranspileFiles().from(subproject.getConfigurations().named(SRC_UMLP_CONFIG));
          // TODO: END OF REMOVAL

          // task.getOutputDir().set(guiJava2tsDir);

          // TODO: Fix ShellAPI imports
          // Required due to "imports cannot use * wildcards when using modules"
          for (String s : defaultExcludedSources) {
            task.getExcludeSource().add(s); // via @Erased
            task.getExcludeSource().add("glob:**/common/Neo4jDatabaseStarter.java");
            task.getExcludeSource().add("glob:**/common/DatabaseAccessManage*.java");
            task.getExcludeSource().add("glob:**/common/GemIdManager4*.java");
          }

          task.onlyIf(t -> guiPresent);
        });
        mainSourceSet.getJava().srcDir(gui2java2ts.flatMap(CommonMCTask::getOutputDir));

        gui2symJar.configure(task -> {
          task.from(
              gui2java2ts.flatMap(GUI2Java2TSTask::getOutputDir),
              spec -> spec.include("**/*sym", "**/*import.map", "**/*tags")
          );
        });

        // TODO: remove after jsweet transition is complete and import mapping is reworked
        if (!umlp.isEmpty()) {
          TaskProvider<UMLP2TSTask> umlp2ts = subproject.getTasks().register(UMLP2TS_TASK, UMLP2TSTask.class);
          umlp2ts.configure(task -> {
            task.setGroup(UMLP_GROUP_NAME);
            task.getInput().from(umlp);
            if (srcGUI.getAsFile().exists()) {
              task.getHandWrittenCodeDir().from(srcGUI);
            }
            if (srcJava.getAsFile().exists()) {
              task.getHandWrittenCodeDir().from(srcJava);
            }
            task.getOutputDir().set(umlp2tsDir);
            task.getTslib().set(libQName);
          });

          umlp2impJar.configure(task -> {
            task.dependsOn(umlp2ts);
            task.from(
                umlp2ts.flatMap(UMLP2TSTask::getOutputDir),
                spec -> spec.include("**/*import.map")
            );
          });
        }

        TaskProvider<NpmTask> buildLib = subproject.getTasks().register(BUILD_LIB_TASK, NpmTask.class, task -> {
          task.setGroup("build");
          task.dependsOn(npmInstall, gui2java2ts);
          if (inUMLP(subproject) && !inRTE(subproject) && useLocalDependencies(subproject)) {
            task.dependsOn(subproject.project(":umlp-rte:frontend").getTasks().named("build"));
          }
          task.getArgs().addAll(libQName.map(v -> List.of("run", "build", v)));
          task.getWorkingDir().set(p.getLayout().getProjectDirectory().getAsFile());
          task.getInputs().files(srcGUI.getAsFileTree().matching(pattern -> {
            pattern.exclude("**/build/**");
            pattern.exclude("**/*.spec.ts", "**/*.md");
          }));
          task.getInputs().files("angular.json", "tsconfig.json", "tsconfig.prod.json", "package.json");
          // e.g. `build/dist/login/`
          task.getOutputs().dir(libBuildDir);
        });
        buildSubprojects.add(buildLib);

        TaskProvider<NpmTask> testLib = subproject.getTasks().register(TEST_LIB_TASK, NpmTask.class, task -> {
          task.setGroup("verification");
          task.dependsOn(buildLib);
          task.getArgs().addAll(libQName.map(v -> List.of("run", "test", v)));
          task.getWorkingDir().set(p.getLayout().getProjectDirectory().getAsFile());
          task.getInputs().files(srcGUI.getAsFileTree().matching(pattern -> {
            pattern.exclude( "**/*.md");
          }));
          task.getInputs().files("angular.json", "tsconfig.json", "tsconfig.spec.json", "package.json");
          //TODO outputs task.getOutputs().dir(libBuildDir);
          task.onlyIf("lib directory contains *.spec.ts files", t ->
              !srcGUI.getAsFileTree().matching(pattern -> pattern.include("**/*.spec.ts")).isEmpty()
          );
        });
        testSubprojects.add(testLib);

        TaskProvider<CreateNpmrcTask> npmrc4publish = subproject.getTasks().register("npmrc4publish", CreateNpmrcTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.dependsOn(buildLib);
          task.getNpmrcFile().set(libBuildDir.map(dir -> dir.file(".npmrc")));
          task.getGitignore().set(p.getLayout().getProjectDirectory().file(".gitignore"));
          task.getUnscoped().set(true);
        });

        // Pack the built lib for to compare if the hashes are exactly the same for publishLib
        Provider<Directory> packDestDir = subproject.getLayout().getBuildDirectory().dir("publishCheck");
        TaskProvider<NpmTask> packLibForPublish = subproject.getTasks().register("packLibForPublish", NpmTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.dependsOn(buildLib, npmrc4publish);
          // use buildLib's output for uptodate checks
          task.getInputs().dir(libBuildDir).withPropertyName("libBuildDirContent");
          task.getArgs().set(packDestDir.map(dir -> List.of("pack", "--pack-destination", dir.getAsFile().getAbsolutePath())));
          task.getWorkingDir().set(libBuildDir.map(dir -> dir.file(".")));
          task.doFirst(t -> packDestDir.get().getAsFile().mkdirs());
          task.getOutputs().dir(packDestDir);
        });

        TaskProvider<NpmTask> publishLib = subproject.getTasks().register("publishLib", NpmTask.class, task -> {
          task.setGroup("publishing");
          task.dependsOn(npmrc4publish, packLibForPublish);
          task.getArgs().set(List.of("publish"));
          // e.g. `build/dist/basic/`
          task.getWorkingDir().set(libBuildDir.map(dir -> dir.file(".")));
          task.onlyIf("version is not yet published to Nexus", t -> {
            try {
              JsonObject packageJson = readJson(libBuildDir.get().file("package.json").getAsFile());
              String name = packageJson.get("name").getAsString();
              String version = packageJson.get("version").getAsString();

              String[] registryAndAuth = parseNpmrc(libBuildDir.get().file(".npmrc").getAsFile());
              String remoteShasum = fetchRemoteShasum(registryAndAuth[0], registryAndAuth[1], name, version);
              if (remoteShasum == null) {
                // version not published yet -> publish it
                return true;
              }

              // version already published on Nexus -> skip
              // Report differing content as a warning instead, so that a forgotten
              // version bump stays visible without breaking every pipeline run.
              File tarball = new File(packDestDir.get().getAsFile(), npmPackFileName(name, version));
              if (tarball.isFile() && !sha1Hex(tarball).equals(remoteShasum)) {
                t.getLogger().warn(
                    "Not publishing {}: {}@{} is already published to Nexus, but the locally built "
                        + "package differs (local shasum {} != remote shasum {}). Bump the package "
                        + "version if this change has to be released.",
                    t.getPath(), name, version, sha1Hex(tarball), remoteShasum);
              }
              else {
                t.getLogger().lifecycle("Skipping {}: {}@{} is already published to Nexus.",
                    t.getPath(), name, version);
              }
              return false;
            }
            catch (IOException e) {
              t.getLogger().error("Failed to check whether {} is already published to Nexus: {}"
                  + "\nRun with --stacktrace for the full failure detail.", t.getPath(), e.toString());
              throw new UncheckedIOException(e);
            }
          });
        });
        publishSubprojects.add(publishLib);
      }
    });

    TaskProvider<Task> build = project.getTasks().named("build");
    build.configure(task -> task.dependsOn(buildSubprojects));
    TaskProvider<Task> publish = project.getTasks().named("publish");
    publish.configure(task -> task.dependsOn(publishSubprojects));
    TaskProvider<Task> test = project.getTasks().named("test");
    test.configure(task -> task.dependsOn(testSubprojects));
  }

  public static void addUMLPSymbolsJarAttributes(Configuration config, Project project) {
    config.attributes(it -> {
      it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "montigem-module-domain-symbols"));
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    });
  }

  protected TaskProvider<Jar> gui2symInit(Project project, AdhocComponentWithVariants javaComponent) {
    // Create the gui jar
    Configuration gui2symOutput = project.getConfigurations().create("gui2symOutput");
    gui2symOutput.setCanBeConsumed(true);
    gui2symOutput.setCanBeResolved(false);
    gui2symOutput.setDescription("Published GUI symbols of the module");

    addGuiSymbolsJarAttributes(gui2symOutput, project);

    TaskProvider<Jar> gui2symJar = project.getTasks().register(GUI2SYM_JAR_TASK, Jar.class, task -> {
      task.setGroup("build");
      task.getArchiveClassifier().set("guisymbols");
      task.setIncludeEmptyDirs(false);
    });
    project.getTasks().named(org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME).configure(it -> it.dependsOn(gui2symJar));

    // Add createGuiSymbolsJar-jar as artifact to the config
    project.getArtifacts().add(gui2symOutput.getName(), gui2symJar);
    javaComponent.addVariantsFromConfiguration(gui2symOutput, ConfigurationVariantDetails::mapToOptional);

    // Tell gradle, that the jar tasks builds a publication
    // and add the publication to the outgoing artifacts
    gui2symOutput.getOutgoing().getArtifacts().add(createPublishedArtifact(gui2symJar, project));
    return gui2symJar;
  }

  public static void addGuiSymbolsJarAttributes(Configuration config, Project project) {
    config.attributes(it -> {
      it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "montigem-module-gui-symbols"));
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    });
  }

  protected TaskProvider<Jar> umlp2impInit(Project project, AdhocComponentWithVariants javaComponent) {
    Configuration umlp2impOutput = project.getConfigurations().create("umlp2impOutput");
    umlp2impOutput.setCanBeConsumed(true);
    umlp2impOutput.setCanBeResolved(false);
    umlp2impOutput.setDescription("Published domain import map of the module");

    addImportMapJarAttributes(umlp2impOutput, project);

    TaskProvider<Jar> umlp2impJar = project.getTasks().register("umlp2impJar", Jar.class, task -> {
      task.setGroup("build");
      task.getArchiveClassifier().set("importmap");
      task.setIncludeEmptyDirs(false);
    });
    project.getTasks().named(org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME).configure(it -> it.dependsOn(umlp2impJar));

    // Add jar as artifact to the config
    project.getArtifacts().add(umlp2impOutput.getName(), umlp2impJar);
    javaComponent.addVariantsFromConfiguration(umlp2impOutput, ConfigurationVariantDetails::mapToOptional);

    // Tell gradle, that the jar tasks builds a publication
    // and add the publication to the outgoing artifacts
    umlp2impOutput.getOutgoing().getArtifacts().add(
        createPublishedArtifact(umlp2impJar, project)
    );
    return umlp2impJar;
  }

  public static void addImportMapJarAttributes(Configuration config, Project project) {
    config.attributes(it -> {
      it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
      it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "montigem-module-import-map"));
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
      it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
    });
  }

  private static JsonObject readJson(File file) throws IOException {
    try (Reader reader = new FileReader(file)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    }
  }

  /**
   * @return the file name `npm pack` gives to {@code name@version}, e.g. `umlp-input-10.0.1.tgz`
   * for `@umlp/input`. Looking the tarball up by name (instead of expecting the pack directory to
   * hold exactly one file) keeps stale tarballs from earlier versions harmless.
   */
  private static String npmPackFileName(String name, String version) {
    return name.replace("@", "").replace("/", "-") + "-" + version + ".tgz";
  }


  private static String sha1Hex(File file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
    StringBuilder hex = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  /**
   * @return {registryUrl, base64AuthToken} as already resolved into the .npmrc created by {@link CreateNpmrcTask}.
   */
  private static String[] parseNpmrc(File npmrcFile) throws IOException {
    String registryUrl = null;
    String authToken = null;
    for (String line : Files.readAllLines(npmrcFile.toPath())) {
      if (line.startsWith("registry=")) {
        registryUrl = line.substring("registry=".length()).trim();
      }
      else if (line.contains(":_auth=")) {
        authToken = line.substring(line.indexOf(":_auth=") + ":_auth=".length()).trim().replace("\"", "");
      }
    }
    if (registryUrl == null || authToken == null) {
      throw new GradleException("Could not find registry/auth token in " + npmrcFile);
    }
    return new String[]{registryUrl, authToken};
  }

  /**
   * @return the shasum of {@code name@version} as published in Nexus, or {@code null} if that version does not exist there yet.
   */
  private static String fetchRemoteShasum(String registryUrl, String authToken, String name, String version) throws IOException {
    String base = registryUrl.endsWith("/") ? registryUrl : registryUrl + "/";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(base + name.replace("/", "%2f")))
        .header("Authorization", "Basic " + authToken)
        .GET()
        .build();
    HttpResponse<String> response;
    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
    if (response.statusCode() == 404) {
      return null;
    }
    if (response.statusCode() != 200) {
      throw new GradleException("Unexpected HTTP " + response.statusCode() + " from Nexus while checking " + name + "@" + version);
    }
    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
    JsonObject versions = root.has("versions") ? root.getAsJsonObject("versions") : null;
    if (versions == null || !versions.has(version)) {
      return null;
    }
    return versions.getAsJsonObject(version).getAsJsonObject("dist").get("shasum").getAsString();
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
    Optional<Project> jsweet = Optional.ofNullable(project.findProject(":umlp-jsweet"));
    if (inUMLP(project) && useLocalDependencies(project) && jsweet.isPresent()) {
      project.getDependencies().add("implementation", jsweet.get());
    }
    else {
      project.getDependencies().add("implementation", "umlp:umlp-jsweet:" + umlpVersion(project));
    }

    // Add dependency to jsweet-core, to import the annotations (such as @Erased)
    // Only during compilation of the backend
    // TODO: Use constant for jsweet-core
    project.getDependencies().add("compileOnly", "org.jsweet:jsweet-core:6.3.0");
  }

}
