/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific.serialize;

import de.montigem.gradle.specific.cd.CDGeneratorAction;
import de.se_rwth.commons.logging.Log;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// to be replaced by UMLP2SymTask
@Deprecated
@CacheableTask
abstract public class CD2SymTask extends DefaultTask {

  // only the models of this directory are read, see getModelFiles()
  @Internal
  public abstract DirectoryProperty getInput();

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileTree getModelFiles() {
    return getInput().getAsFileTree().matching(spec -> spec.include("**/*.cd", "**/*.umlp"));
  }

  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  @Optional
  public abstract DirectoryProperty getSymbolPath();

  @OutputDirectory
  public abstract DirectoryProperty getOutput();

  public CD2SymTask() {
    super();
  }

  @Inject
  abstract public WorkerExecutor getWorkerExecutor();

  @InputFiles
  @Classpath
  public abstract ConfigurableFileCollection getClassPath();

  @TaskAction
  public void run() throws IOException {
    WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(spec -> spec.getClasspath().from(getClassPath()));
    // TODO: Isolate TaskAction, so that no side-effects (... static variables / Logger / Mill) occur
    // Also important for parallelization

    // all models are serialized again, so symbols of removed models must not survive
    FileUtils.cleanDirectory(getOutput().get().getAsFile());

    Set<File> inputFiles = getModelFiles().getFiles();

    for (File inputFile : inputFiles) {
      List<String> args = new ArrayList<>();
      args.add("-i");
      args.add(inputFile.getAbsolutePath());

      if (getSymbolPath().isPresent()) {
        args.add("-path");
        args.add(getSymbolPath().get().getAsFile().getAbsolutePath());
      }

      args.add("-s");
      args.add(getOutput().getAsFile().get().getAbsolutePath());

      args.add("-c2mc");

      Log.trace(args.toString(), this.getName());

      String[] argsA = args.toArray(new String[0]);
      workQueue.submit(CDGeneratorAction.class, cdGenParams -> {
        cdGenParams.getArgs().set(argsA);
      });

    }
  }
}
