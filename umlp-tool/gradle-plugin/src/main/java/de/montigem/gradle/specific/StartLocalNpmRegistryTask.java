/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import com.github.gradle.node.NodeExtension;
import com.github.gradle.node.variant.VariantComputer;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import java.io.IOException;

/**
 * Starts the local npm registry (Verdaccio) with the node of the node-gradle plugin.
 * The registry runs until the end of the build, see {@link LocalNpmRegistryService}.
 * <p>
 * Tasks that need the registry are connected with {@link #requiredBy(Project, TaskProvider)}.
 * The storage directory must be given with the gradle property <code>localNpmRepoDir</code>,
 * unless a registry is already running.
 */
public abstract class StartLocalNpmRegistryTask extends DefaultTask {

  public static final String TASK_NAME = "startLocalNpmRegistry";
  public static final String SERVICE_NAME = "localNpmRegistry";

  @Internal
  public abstract Property<LocalNpmRegistryService> getRegistry();

  @TaskAction
  public void start() throws IOException {
    getRegistry().get().ensureStarted();
  }

  /** Makes sure that the registry is up before (and while) the given task runs. */
  public static void requiredBy(Project project, TaskProvider<? extends org.gradle.api.Task> task) {
    TaskProvider<StartLocalNpmRegistryTask> start = register(project);
    task.configure(t -> {
      t.dependsOn(start);
      // usesService only accepts the provider of the registration itself
      t.usesService(project.getGradle().getSharedServices().getRegistrations()
          .getByName(SERVICE_NAME).getService());
    });
  }

  private static TaskProvider<StartLocalNpmRegistryTask> register(Project project) {
    if (project.getTasks().getNames().contains(TASK_NAME)) {
      return project.getTasks().named(TASK_NAME, StartLocalNpmRegistryTask.class);
    }

    NodeExtension node = project.getExtensions().getByType(NodeExtension.class);
    VariantComputer variants = new VariantComputer();
    Provider<Directory> nodeBinDir = variants.computeNodeBinDir(variants.computeNodeDir(node));
    Provider<String> nodeExec = variants.computeNodeExec(node, nodeBinDir);
    Provider<String> npmExec = variants.computeNpmExec(node, nodeBinDir);

    // next to the downloaded node versions, e.g. <project>/.gradle/nodejs/localNpmRepo
    Provider<Directory> localNpmDir = node.getWorkDir().dir("localNpmRepo");
    // no default: without the property only an already running registry can be used
    Provider<Directory> storage = project.getLayout().dir(project.getProviders()
        .gradleProperty("localNpmRepoDir")
        .map(project::file));

    Provider<LocalNpmRegistryService> service = project.getGradle().getSharedServices()
        .registerIfAbsent(SERVICE_NAME, LocalNpmRegistryService.class, spec -> {
          spec.getParameters().getStorageDir().set(storage);
          spec.getParameters().getToolDir().set(localNpmDir.map(dir -> dir.dir("tool")));
          spec.getParameters().getNodeExecutable().set(nodeExec);
          spec.getParameters().getNpmExecutable().set(npmExec);
        });

    return project.getTasks().register(TASK_NAME, StartLocalNpmRegistryTask.class, task -> {
      task.setGroup("publishing");
      task.setDescription("Starts the local npm registry (Verdaccio) until the end of the build.");
      task.getRegistry().set(service);
      task.usesService(service);
      // node is only downloaded by nodeSetup if the node-gradle plugin is configured to do so
      task.dependsOn(project.getTasks().named("nodeSetup"));
    });
  }
}
