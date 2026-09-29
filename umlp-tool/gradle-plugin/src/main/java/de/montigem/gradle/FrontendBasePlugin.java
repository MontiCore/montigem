/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle;

import com.github.gradle.node.NodeExtension;
import com.github.gradle.node.NodePlugin;
import com.github.gradle.node.npm.task.NpmTask;
import com.github.gradle.node.npm.task.NpxTask;
import de.montigem.gradle.specific.CreateSymlinkTask;
import de.montigem.gradle.specific.FrontendCIImageTask;
import de.montigem.gradle.specific.FrontendLocalImageTask;
import de.montigem.gradle.specific.Java2TSTask;
import de.montigem.gradle.specific.SetupFrontendTask;
import de.montigem.gradle.specific.UMLP2SymTask;
import de.se_rwth.commons.logging.Log;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.ZipEntryCompression;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static de.montigem.gradle.FrontendPlugin.GUI2JAVA2TS_TASK;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_IMPORT_MAP_CONFIG;
import static de.montigem.gradle.MGBasePlugin.MONTIGEM_MODULE_UMLP_MODELS_CONFIG;
import static de.montigem.gradle.module.ModuleFrontendPlugin.BUILD_LIB_TASK;

/**
 * Common UMLP frontend setup.
 * Specific tasks for JSweet or No-Jsweet are configured in the specific plugins
 */
public abstract class FrontendBasePlugin implements Plugin<Project> {

  public static final String SETUP_FRONTEND_TASK = "setupFrontend";
  public static final String UMLP2TS_TASK = "umlp2ts";
  public static final String CREATE_SYMLINK_TASK = "createSymlink";
  public static final String GUI2JAVA_TASK = "gui2java";
  public static final String GENERATE_TASK = "generate";
  public static final String UMLP_FRONTEND_BUILD_IMAGE_LOCAL_TASK = "buildImageLocal";
  public static final String UMLP_FRONTEND_DEPLOY_IMAGE_CI_TASK = "deployImageCI";
  public static final String UMLP2SYM_TASK = "umlp2sym";
  public static final String FULL_APPLICATION_JAR_HOSTING = "fullAppJar";

  public static final String UMLP_GROUP_NAME = "umlp";
  public static final String GUI_TYPES_CONFIG = "guiTypes";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply("base");
    project.getPluginManager().apply("java-library");
    project.getPluginManager().apply(NodePlugin.class);
    project.getPluginManager().apply(MGBasePlugin.class);

    project.getPlugins().withId("com.github.node-gradle.node", plugin -> {
      NodeExtension node = project.getExtensions().getByType(NodeExtension.class);
      node.getNodeProjectDir().set(project.getLayout().getBuildDirectory().dir("app"));
      if (!useLocalDependencies(project) || !inUMLP(project)) {
        node.getDownload().set(true);
      }
      node.getVersion().set(Java2TSTask.NODE_VERSION);
      // track only node_modules/.package-lock.json, hashing all of node_modules takes minutes
      node.getFastNpmInstall().set(true);
    });

    // Create Dependencies
    addProjectDependencies(project);
    addGUITypesDependency(project);
    addClientRTEDependency(project);

    // Create Tasks
    Directory config = project.getLayout().getProjectDirectory().dir("config");
    TaskProvider<SetupFrontendTask> setupFrontend = project.getTasks().register(
        SETUP_FRONTEND_TASK, SetupFrontendTask.class, task -> {
          task.setGroup(UMLP_GROUP_NAME);
          task.getInput().set(config);
          task.getOutputRoot().set(project.getLayout().getBuildDirectory().dir("app"));
          task.getUseLocalNpmRepo().set(project.getProviders()
              .gradleProperty("useLocalNpmRepo")
              .map(Boolean::parseBoolean)
              .orElse(false));
        });
    project.getTasks().named("nodeSetup").configure(t -> t.dependsOn(setupFrontend));

    createUMLP2SymTask(project);

    // Lifecycle task "generate"
    TaskProvider<Task> generate = project.getTasks().register(GENERATE_TASK, task -> task.setGroup(UMLP_GROUP_NAME));

    Provider<Boolean> useLocalNpmRepo = project.getProviders()
        .gradleProperty("useLocalNpmRepo")
        .map(Boolean::parseBoolean)
        .orElse(false);
    TaskProvider<NpmTask> npmInstall = project.getTasks().named("npmInstall", NpmTask.class);
    npmInstall.configure(task -> task.getEnvironment().putAll(useLocalNpmRepo.map(useLocal -> useLocal
        ? java.util.Map.of("npm_config_userconfig", project.getLayout().getBuildDirectory()
            .file("app/.npmrc").get().getAsFile().getAbsolutePath())
        : java.util.Map.of())));
    TaskProvider<Task> npmUpdate = project.getTasks().named("npm_update", task -> {
      task.dependsOn(npmInstall);
      task.getOutputs().dir("node_modules");
      task.onlyIf(t -> project.getGradle().getStartParameter().isRefreshDependencies());
    });

    TaskProvider<Task> prepareBuildAppTask = project.getTasks().register("prepareBuildApp", task -> {
      task.setDescription("Bundles all dependencies for the buildApp task without executing it.");
      task.setGroup("build");
      task.dependsOn(generate, npmUpdate);
    });

    var buildAppTask = project.getTasks().register("buildApp", NpxTask.class, task -> {
      task.setGroup("build");
      task.dependsOn(prepareBuildAppTask);
      task.getCommand().set("ng");
      task.getArgs().set(List.of("build"));
      // the links hwc, cd2gui and prodlibs are followed, libs/ is only used by the dev build
      Provider<Directory> app = project.getLayout().getBuildDirectory().dir("app");
      task.getInputs().files(app.map(dir -> dir.getAsFileTree().matching(spec -> spec.exclude(
              "node_modules/**", "dist/**", ".angular/**", "libs/**"))))
          .withPropertyName("appSources").withPathSensitivity(PathSensitivity.RELATIVE);
      task.getInputs().files(app.map(dir -> dir.file("node_modules/.package-lock.json")))
          .withPropertyName("installedPackages");
      task.getOutputs().dir(app.map(dir -> dir.dir("dist/client"))).withPropertyName("dist");
    });
    var testAppTask = project.getTasks().register("testApp", NpxTask.class, task -> {
      task.setGroup("verification");
      task.dependsOn(generate, npmUpdate);
      task.getCommand().set("ng");
      task.getArgs().set(List.of("test"));
    });
    var runAppTask = project.getTasks().register("runApp", NpxTask.class, task -> {
      task.setGroup("application");
      task.dependsOn(generate, npmUpdate);
      task.getCommand().set("ng");
      task.getArgs().set(List.of("serve"));
    });
    project.getTasks().register("run").configure(task -> {
      task.setGroup("application");
      task.dependsOn(runAppTask);
    });

    TaskProvider<CreateSymlinkTask> createSymlinkTask = project.getTasks().register(CREATE_SYMLINK_TASK, CreateSymlinkTask.class);
    createSymlinkTask.configure(task -> task.dependsOn(setupFrontend));
    testAppTask.configure(t -> t.dependsOn(createSymlinkTask));
    runAppTask.configure(t -> t.dependsOn(createSymlinkTask));
    if (useLocalDependencies(project) && inUMLP(project)) {
      Project rteFrontend = project.project(":umlp-rte:frontend");
      createSymlinkTask.configure(task -> {
        task.setGroup(UMLP_GROUP_NAME);

        // TODO change this in the future, so we do not have to download all library's dependencies in every umlp project
        // Alternative: use pnpm or yarn or something like this
        for (Project subproject : rteFrontend.getSubprojects()) {
          // the links point to the lib sources, which import the lib's generated code
          task.dependsOn(subproject.getTasks().named(GUI2JAVA2TS_TASK));

          task.getLinks().put(
              project.getLayout().getBuildDirectory().dir("app/libs/@umlp/" + subproject.getName()).get().getAsFile(),
              subproject.getLayout().getProjectDirectory().getAsFile()
          );
        }
      });

      // task for linking compiled umlp-rte library for checking production build
      TaskProvider<CreateSymlinkTask> createSymlink4ProdTask = project.getTasks().register("createSymlink4Prod", CreateSymlinkTask.class, task -> {
        task.setGroup(UMLP_GROUP_NAME);

        for (Project subproject : rteFrontend.getSubprojects()) {
          TaskProvider<NpmTask> buildLib = subproject.getTasks().named(BUILD_LIB_TASK, NpmTask.class);
          task.dependsOn(buildLib);

          task.getLinks().put(
              project.getLayout().getBuildDirectory().dir("app/prodlibs/@umlp/" + subproject.getName()).get().getAsFile(),
              buildLib.map(t -> t.getOutputs().getFiles().getSingleFile())
          );
        }
      });

      createSymlink4ProdTask.configure(t -> t.dependsOn(createSymlinkTask));
      prepareBuildAppTask.configure(t -> t.dependsOn(createSymlink4ProdTask));
      // ng test resolves @umlp/* via tsconfig.json, which points to the prodlibs
      testAppTask.configure(t -> t.dependsOn(createSymlink4ProdTask));
    }

    project.getTasks().named("assemble", t -> t.dependsOn(buildAppTask));
    project.getTasks().named("check", t -> t.dependsOn(testAppTask));

    // Lifecycle task "continuous". Intended use is with "--continuous" flag of gradle & HotReload
    project.getTasks().register("continuous", task -> {
      task.setGroup("application");
      task.dependsOn(generate);
    });

    // Docker images
    project.getTasks().register(UMLP_FRONTEND_BUILD_IMAGE_LOCAL_TASK, FrontendLocalImageTask.class);
    project.getTasks().register(UMLP_FRONTEND_DEPLOY_IMAGE_CI_TASK, FrontendCIImageTask.class);

    Optional<Project> parent = Optional.ofNullable(project.getParent());
    Optional<Project> backend = parent.map(value -> value.findProject(value.getPath() + ":backend"));
    if(backend.isPresent()) {
      TaskProvider<Jar> fullAppJar = project.getTasks().register(FULL_APPLICATION_JAR_HOSTING, Jar.class, task -> {
        task.setGroup("build");
        task.getArchiveClassifier().set(FULL_APPLICATION_JAR_HOSTING);
        TaskProvider<Jar> backendBootJar = backend.get().getTasks().named("bootJar", Jar.class);

        task.dependsOn(backendBootJar);
        task.dependsOn(buildAppTask);

        task.setEntryCompression(ZipEntryCompression.STORED);

        // Start from Spring Boot Jar that was created in the backend
        task.from(
            project.zipTree(backendBootJar.flatMap(Jar::getArchiveFile)),
            copySpec -> {
              // Add application.properties with a new entry 'java.frontend.enabled=true' to start
              // the FrontEnd server for hosting the websources
              copySpec.filesMatching(
                  "BOOT-INF/classes/application.properties",
                  fileCopyDetails -> {
                    final boolean[] appended = { false };
                    fileCopyDetails.filter(line -> {
                      if (!appended[0]) {
                        appended[0] = true;
                        return line + "\njava.frontend.enabled=true";
                      }
                      return line;
                    });
                  }
              );
            }
        );

        // Copy websources into jar
        task.from(
            setupFrontend.map(t -> t.getOutputRoot().dir("dist/client")),
            copySpec -> copySpec.into("BOOT-INF/classes")
        );

        task.getManifest().from(backendBootJar.flatMap(Jar::getArchiveFile).map(archive ->
                    project.zipTree(archive)
                        .matching(p -> p.include("META-INF/MANIFEST.MF"))
                        .getSingleFile())
        );
      });
    }
  }

  private TaskProvider<UMLP2SymTask> createUMLP2SymTask(Project project) {
    Provider<Directory> umlp2symOut = project.getLayout().getBuildDirectory().dir("umlp2sym");
    return project.getTasks().register(UMLP2SYM_TASK, UMLP2SymTask.class, task -> {
      task.setGroup(UMLP_GROUP_NAME);
      task.getInput().from(getModels(project));
      task.getSymbolPath().from(project.getConfigurations().named(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG));

      task.getOutputDir().set(umlp2symOut);
      task.getModelDir().from(project.getConfigurations().named(MONTIGEM_MODULE_UMLP_MODELS_CONFIG));
    });
  }

  public static void addProjectDependencies(Project project) {
    // repositories
    if (getBooleanProjectProperty(project, "useLocalRepo")) {
      project.getRepositories().mavenLocal();
    }
    project.getRepositories().maven(repo -> {
      String mavenUser = requireProjectProperty(project, "mavenUser");
      String mavenPassword = requireProjectProperty(project, "mavenPassword");
      String repoURL = requireProjectProperty(project, "repo");
      repo.getCredentials().setUsername(mavenUser);
      repo.getCredentials().setPassword(mavenPassword);
      repo.setUrl(repoURL);
    });
    project.getRepositories().mavenCentral();
  }

  public static Configuration addGUITypesDependency(Project project) {
    var conf = project.getConfigurations().create(GUI_TYPES_CONFIG, config -> {
      config.setCanBeConsumed(false);
      config.setCanBeResolved(true);
      config.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "guidsl-types"));
      config.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, "library"));
      config.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, "jar"));
    });
    project.getDependencies().add(GUI_TYPES_CONFIG, "de.monticore.lang:guidsl:" + mcVersion(project));
    return conf;
  }

  public static void addClientRTEDependency(Project project) {
    if (!inRTE(project)) {
      if (inUMLP(project) && useLocalDependencies(project)) {
        Project rteFrontend = project.project(":umlp-rte:frontend");
        project.getDependencies().add(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG, rteFrontend);
        project.getDependencies().add(MONTIGEM_MODULE_IMPORT_MAP_CONFIG, rteFrontend);
        project.getDependencies().add("implementation", rteFrontend);
      }
      else {
        project.getDependencies().add(MONTIGEM_MODULE_GUI_SYMBOLS_CONFIG, "umlp:umlp-rte:" + umlpVersion(project));
        project.getDependencies().add(MONTIGEM_MODULE_IMPORT_MAP_CONFIG, "umlp:umlp-rte:" + umlpVersion(project));
        project.getDependencies().add("implementation", "umlp:umlp-rte:" + umlpVersion(project));
      }
    }
  }

  @NotNull
  public static Directory getModels(Project project) {
    return project.getLayout().getProjectDirectory().dir("../models");
  }

  @NotNull
  public static Directory getOCLPath(Project project) {
    return getModels(project).dir("ocl");
  }

  public static String mcVersion(Project project) {
    String value = (String) project.findProperty("mc_version");
    if (value == null || value.isEmpty()) {
      if (DependencyPlugin.mc_version == null) {
        Log.error("0x00034 A MontiCore version was requested, but the version is unset." +
            "\nPlease make sure to specify it via, e.g., command line (`-Pmc_version=<value>`), "
            + " in a gradle.properties file or by applying `"
            + DependencyPlugin.class.getSimpleName() + "` plugin.");
      }
      return DependencyPlugin.mc_version;
    }
    return value;
  }

  public static String umlpVersion(Project project) {
    if (DependencyPlugin.version == null) {
      Log.error("0x00034 A MontiCore version was requested, but the version is unset." +
          "\nPlease make sure to specify it via, e.g., command line (`-Pmc_version=<value>`), "
          + " in a gradle.properties file or by applying `"
          + DependencyPlugin.class.getSimpleName() + "` plugin.");
    }
    return DependencyPlugin.version;
  }

  public static String requireProjectProperty(Project project, String propertyName) {
    Object projectProperty = project.findProperty(propertyName);
    if (projectProperty == null) {
      Log.error("0x00033 Required property `" + propertyName
          + "` is unset in your project.\nPlease make sure to specify it via, e.g., command line" +
          " (`-P" + propertyName + "=<value>`) or in a gradle.properties file`");
      return "";
    }
    else {
      return projectProperty.toString();
    }
  }

  @NotNull
  public static Boolean getBooleanProjectProperty(Project project, String propertyName) {
    return getProjectProperty(project, propertyName).map(Boolean::parseBoolean).orElse(false);
  }

  public static Optional<String> getProjectProperty(Project project, String propertyName) {
    Object projectProperty = project.findProperty(propertyName);
    if (projectProperty == null) {
      return Optional.empty();
    }
    else {
      return Optional.of(projectProperty.toString());
    }
  }

  public static boolean inRTE(Project project) {
    return project.getPath().startsWith(":umlp-rte:")
        || project.getPath().equals(":umlp-rte");
  }

  @NotNull
  public static Boolean useLocalDependencies(Project project) {
    return getBooleanProjectProperty(project, "useLocalDependencies");
  }

  public static boolean inUMLP(Project project) {
    return project.getGradle().getRootProject().getName().equals("umlp");
  }

}
