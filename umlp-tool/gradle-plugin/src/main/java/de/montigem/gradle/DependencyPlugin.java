/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle;

import de.montigem.gradle.specific.GUI2JSweetAdapterTask;
import de.montigem.gradle.specific.GUIJava2TSTask;
import de.montigem.gradle.specific.UMLP2JSweetAdapterTask;
import de.montigem.gradle.specific.UMLPJava2TSTask;
import de.montigem.gradle.specific.serialize.GUI2SymTask;
import de.se_rwth.commons.logging.Log;
import de.montigem.gradle.common.GradleLogGuard;
import de.montigem.gradle.common.MGAllFilesTask;
import de.montigem.gradle.common.MGSingleFileTask;
import de.montigem.gradle.specific.merge.MergeTask;
import de.montigem.gradle.specific.serialize.CD2SymTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Sets up all tasks with their required Tool-classpath.
 */
public abstract class DependencyPlugin implements Plugin<Project> {


  public static final String UMLP_CONFIG_TOOL = "umlpTool";
  public static String version;
  public static String mc_version;

  @Override
  public void apply(Project project) {
    // tasks of this plugin and of MontiCore log in the daemon, an error must not System.exit it
    GradleLogGuard.ensureGradleLog();

    // add a new configuration in which the umlp-tool (.jar) is loaded
    //  (this avoids loading the tool+all its dependencies into the shared plugin-classpath)
    var toolConfig = project.getConfigurations().maybeCreate(UMLP_CONFIG_TOOL);
    toolConfig.setDescription("Classpath of the executed montigem/umlp-tool (jar)");
    // Load the buildInfo.properties file (containing the current version)
    Properties buildInfo = new Properties();
    try (InputStream is = DependencyPlugin.class.getResourceAsStream("/mg3BuildInfo.properties")) {
      buildInfo.load(is);
    } catch (Exception e) {
      Log.error("Cannot load /mg3BuildInfo.properties from classpath", e);
    }
    version = Objects.requireNonNull(
        buildInfo.getProperty("version"),
        "Cannot find version in mg3BuildInfo.properties"
    );
    mc_version = Objects.requireNonNull(
        buildInfo.getProperty("mc_version"),
        "Cannot find mc_version in mg3BuildInfo.properties"
    );
    // Add the dependencies to the toolConfig
    toolConfig.setCanBeResolved(true);
    toolConfig.defaultDependencies(dependencies -> {
      dependencies.add(project.getDependencies().create("umlp:umlp-tool:" + version));
      dependencies.add(project.getDependencies().create("de.monticore.lang:cd4analysis:" + mc_version + ":cdmerge"));
      dependencies.add(project.getDependencies().create("de.monticore.lang:od:" + mc_version));
    });
    toolConfig.attributes(it -> {
      // Do not use the shadowed bundling variant
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
    });

    // And add the config to the classpath of various tasks
    project.getTasks().withType(MGSingleFileTask.class).configureEach(t -> t.getExtraClasspathElements().from(toolConfig));
    project.getTasks().withType(MGAllFilesTask.class).configureEach(t -> t.getExtraClasspathElements().from(toolConfig));
    project.getTasks().withType(CD2SymTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(GUI2SymTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(MergeTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(GUI2JSweetAdapterTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(UMLP2JSweetAdapterTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(GUIJava2TSTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
    project.getTasks().withType(UMLPJava2TSTask.class).configureEach(t -> t.getClassPath().from(toolConfig));
  }

}
