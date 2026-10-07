package de.montigem.gradle;

import de.monticore.gradle.common.CommonMCTask;
import de.montigem.gradle.common.CheckDirEqualityTask;
import de.montigem.gradle.specific.GUI2Java2TSTask;
import de.montigem.gradle.specific.GUI2JavaTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import static de.montigem.gradle.FrontendBasePlugin.GUI2JAVA_TASK;
import static de.montigem.gradle.FrontendPlugin.GUI2JAVA2TS_TASK;

public class FrontendTestPlugin implements Plugin<Project> {

  public static final String CLONE_GUI2JAVA2TS_TASK = "clone_gui2java2ts";
  public static final String CLONE_GUI2JAVA_TASK = "clone_gui2java";
  public static final String COMPARE_GUI2JAVA2TS_TASK = "compare_gui2java2ts";

  @Override
  public void apply(Project project) {

    project.getPluginManager().apply(FrontendPlugin.class);

    // Frontend GUI Tasks:
    TaskProvider<GUI2JavaTask> firstGenerator = project.getTasks().named(GUI2JAVA_TASK, GUI2JavaTask.class);
    TaskProvider<GUI2Java2TSTask> firstTask = project.getTasks().named(GUI2JAVA2TS_TASK, GUI2Java2TSTask.class);
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
      task.getTSOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediate/clone_gui2java2ts"));
      task.getSymbolOutDir().set(project.getLayout().getBuildDirectory().dir("clone_gui2java_sym"));
    });
    TaskProvider<GUI2Java2TSTask> secondTask = project.getTasks().register(CLONE_GUI2JAVA2TS_TASK, GUI2Java2TSTask.class, task -> {
      task.setGroup("verification");
      task.getOutputs().doNotCacheIf("Compared with gui2java2ts", t -> true);
      task.getGeneratedSources().set(secondGenerator.flatMap(CommonMCTask::getOutputDir));
      task.getGeneratedSymbols().set(secondGenerator.flatMap(GUI2JavaTask::getSymbolOutDir));
      task.getDomain().set(secondGenerator.flatMap(GUI2JavaTask::getDomain));
      task.getHandWrittenCodeDir().from(secondGenerator.map(CommonMCTask::getHandWrittenCodeDir));
      task.getSourceDirs().from(secondGenerator.map(CommonMCTask::getInput));
      task.getJSweetAdapterDir().set(firstTask.flatMap(GUI2Java2TSTask::getJSweetAdapterDir));
      task.getHandWrittenJSweetDir().set(firstTask.flatMap(GUI2Java2TSTask::getHandWrittenJSweetDir));
      task.getInput4RelativePathCalculation().from(firstTask.map(GUI2Java2TSTask::getInput4RelativePathCalculation));
      task.getJSweetWorkingDir().set(firstTask.flatMap(GUI2Java2TSTask::getJSweetWorkingDir));
      task.getJSweetCandiesDir().set(firstTask.flatMap(GUI2Java2TSTask::getJSweetCandiesDir));
      task.getJSweetClassPath().from(firstTask.map(GUI2Java2TSTask::getJSweetClassPath));
      task.getExcludeTranspileFiles().from(firstTask.map(GUI2Java2TSTask::getExcludeTranspileFiles));
      task.getExcludeSource().set(firstTask.flatMap(GUI2Java2TSTask::getExcludeSource));

      task.getSymbolOutDir().set(project.getLayout().getBuildDirectory().dir("clone_gui2sym"));
      task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediate/clone_gui2java2ts"));
    });

    TaskProvider<CheckDirEqualityTask> compareGUI2Java = project.getTasks().register(COMPARE_GUI2JAVA2TS_TASK, CheckDirEqualityTask.class, task -> {
      task.setGroup("verification");
      task.getDir1().set(firstTask.flatMap(GUI2Java2TSTask::getOutputDir));
      task.getDir2().set(secondTask.flatMap(GUI2Java2TSTask::getOutputDir));
    });

    project.getTasks().named("build").configure(t -> t.dependsOn(compareGUI2Java));
  }

}
