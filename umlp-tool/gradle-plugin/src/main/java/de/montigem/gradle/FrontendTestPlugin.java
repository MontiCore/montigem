package de.montigem.gradle;

import de.monticore.gradle.common.CommonMCTask;
import de.montigem.gradle.common.CheckDirEqualityTask;
import de.montigem.gradle.specific.GUIJava2TSTask;
import de.montigem.gradle.specific.GUI2JavaTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import static de.montigem.gradle.FrontendBasePlugin.GUI2JAVA_TASK;
import static de.montigem.gradle.FrontendPlugin.GUI_JAVA2TS_TASK;

public class FrontendTestPlugin implements Plugin<Project> {

  public static final String CLONE_GUI_JAVA2TS_TASK = "clone_guiJava2ts";
  public static final String CLONE_GUI2JAVA_TASK = "clone_gui2java";
  public static final String COMPARE_GUI_JAVA2TS_TASK = "compare_guiJava2ts";

  @Override
  public void apply(Project project) {

    project.getPluginManager().apply(FrontendPlugin.class);

    // Frontend GUI Tasks:
    TaskProvider<GUI2JavaTask> firstGenerator = project.getTasks().named(GUI2JAVA_TASK, GUI2JavaTask.class);
    TaskProvider<GUIJava2TSTask> firstTask = project.getTasks().named(GUI_JAVA2TS_TASK, GUIJava2TSTask.class);
    TaskProvider<GUI2JavaTask> secondGenerator = project.getTasks().register(CLONE_GUI2JAVA_TASK, GUI2JavaTask.class, task -> {
      task.setGroup("verification");
      // the comparison needs a second run, not a result from the build cache
      task.getOutputs().doNotCacheIf("Compared with gui2java", t -> true);
      task.getImportsDir().from(firstGenerator.map(GUI2JavaTask::getImportsDir));
      task.getInput().from(firstGenerator.map(CommonMCTask::getInput));
      task.getHandWrittenCodeDir().from(firstGenerator.map(CommonMCTask::getHandWrittenCodeDir));
      task.getModelDir().from(firstGenerator.map(GUI2JavaTask::getModelDir));
      task.getSymbolPath().from(firstGenerator.map(GUI2JavaTask::getSymbolPath));
      task.getDomain().set(firstGenerator.flatMap(GUI2JavaTask::getDomain));
      task.getGuiCreateModels().set(firstGenerator.flatMap(GUI2JavaTask::getGuiCreateModels));
      task.getGUIGenJSweet().set(firstGenerator.flatMap(GUI2JavaTask::getGUIGenJSweet));
      task.getGUIGenTSFunctionality().set(firstGenerator.flatMap(GUI2JavaTask::getGUIGenTSFunctionality));

      // Outputs of the task to generate into a different directory & compare after
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediate/clone_gui2java"));
      task.getSymbolOutDir().set(project.getLayout().getBuildDirectory().dir("clone_gui2java_sym"));
    });
    TaskProvider<GUIJava2TSTask> secondTask = project.getTasks().register(CLONE_GUI_JAVA2TS_TASK, GUIJava2TSTask.class, task -> {
      task.setGroup("verification");
      task.getOutputs().doNotCacheIf("Compared with guiJava2ts", t -> true);
      task.getGeneratedSources().set(secondGenerator.flatMap(CommonMCTask::getOutputDir));
      task.getGeneratedSymbols().set(secondGenerator.flatMap(GUI2JavaTask::getSymbolOutDir));
      task.getDomain().set(secondGenerator.flatMap(GUI2JavaTask::getDomain));
      task.getHandWrittenCodeDir().from(secondGenerator.map(CommonMCTask::getHandWrittenCodeDir));
      task.getSourceDirs().from(secondGenerator.map(CommonMCTask::getInput));
      task.getJSweetAdapterDir().set(firstTask.flatMap(GUIJava2TSTask::getJSweetAdapterDir));
      task.getHandWrittenJSweetDir().set(firstTask.flatMap(GUIJava2TSTask::getHandWrittenJSweetDir));
      task.getInput4RelativePathCalculation().from(firstTask.map(GUIJava2TSTask::getInput4RelativePathCalculation));
      task.getJSweetWorkingDir().set(firstTask.flatMap(GUIJava2TSTask::getJSweetWorkingDir));
      task.getJSweetCandiesDir().set(firstTask.flatMap(GUIJava2TSTask::getJSweetCandiesDir));
      task.getJSweetClassPath().from(firstTask.map(GUIJava2TSTask::getJSweetClassPath));
      task.getExcludeTranspileFiles().from(firstTask.map(GUIJava2TSTask::getExcludeTranspileFiles));
      task.getExcludeSource().set(firstTask.flatMap(GUIJava2TSTask::getExcludeSource));

      task.getSymbolOutDir().set(project.getLayout().getBuildDirectory().dir("clone_gui2sym"));
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediate/clone_gui2java2ts"));
    });

    TaskProvider<CheckDirEqualityTask> compareGUI2Java = project.getTasks().register(COMPARE_GUI_JAVA2TS_TASK, CheckDirEqualityTask.class, task -> {
      task.setGroup("verification");
      task.getDir1().set(firstTask.flatMap(GUIJava2TSTask::getOutputDir));
      task.getDir2().set(secondTask.flatMap(GUIJava2TSTask::getOutputDir));
    });

    project.getTasks().named("build").configure(t -> t.dependsOn(compareGUI2Java));
  }

}
