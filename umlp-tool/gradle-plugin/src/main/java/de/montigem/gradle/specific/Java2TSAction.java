package de.montigem.gradle.specific;

import com.kohlschutter.jdk.standaloneutil.JavaHomeLocator;
import de.montigem.gradle.common.GradleLogGuard;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.jsweet.transpiler.EcmaScriptComplianceLevel;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.JSweetFactory;
import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.ModuleKind;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.jsweet.transpiler.util.ProcessUtil;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transpile using JSweet
 * A WorkAction to allow different classes
 */
public abstract class Java2TSAction implements WorkAction<Java2TSAction.Params> {

  @Override
  public void execute() {
    GradleLogGuard.ensureGradleLog();
    Params params = getParameters();

    // Fix for ExceptionInInitializerError, with cause FileSystemAlreadyExistsException
    // Prevent a race condition in the transpiler by preloading JavaHomeLocator#pathForResourceURL.
    // It's invoked during JavaHomeLocator#<init>(COMPILER_JAVA_HOME).
    // Synchronizing on a shared class ensures thread safety across isolated classloaders.
    synchronized (WorkAction.class) {
      JavaHomeLocator javaHomeLocator = new JavaHomeLocator();
    }

    JSweetFactory factory = new JSweetFactory() {
      @Override
      public PrinterAdapter createAdapter(JSweetContext context) {
        context.useRequireForModules = false; // Fix for @Module annotations
        return super.createAdapter(context);
      }
    };

    JSweetTranspiler transpiler = new JSweetTranspiler(
        params.getBaseDirectory().getAsFile().get(),
        null,
        factory,
        params.getWorkingDirectory().getAsFile().get(),
        params.getOutputDir().getAsFile().get(),
        params.getOutputDir().getAsFile().get(),
        params.getExtractedCandiesJavascriptDir().getAsFile().get(),
        params.getClassPath().get());

    transpiler.setTscWatchMode(false);
    transpiler.setAutoPropagateAsyncAwaits(true);
    transpiler.setVerbose(params.getVerbose().getOrElse(false));
    transpiler.setEncoding("UTF-8");
    transpiler.setGenerateSourceMaps(params.getCreateSourceMaps().getOrElse(false));
    transpiler.setGenerateJsFiles(false);
    transpiler.setEcmaTargetVersion(EcmaScriptComplianceLevel.ES6);
    transpiler.setModuleKind(ModuleKind.commonjs);
    ProcessUtil.addExtraPath(".gradle/nodejs/node-v" + params.getNodeVersion().get() + "-win-x64");

    SourceFile[] sourceFiles = params.getInput().getFiles().stream()
        .map(SourceFile::new).toArray(SourceFile[]::new);


    // collect excluded sources
    Set<String> excludedSources = params.getExcludedFromTranspilation().getFiles().stream()
        .map(SourceFile::new).map(SourceFile::toString).collect(Collectors.toSet());


    ErrorCountTranspilationHandler transpilationHandler = new ErrorCountTranspilationHandler(new ConsoleTranspilationHandler());
    // closing releases the classpath jars that javac keeps open, else they stay locked in the daemon
    try (transpiler) {
      transpiler.transpile(transpilationHandler, excludedSources, sourceFiles);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if(transpilationHandler.getErrorCount()>0) {
      throw new RuntimeException("JSweet Transpile failed with errors.");
    }
  }

  public interface Params extends WorkParameters {
    ConfigurableFileCollection getInput();

    ConfigurableFileCollection getExcludedFromTranspilation();

    Property<Boolean> getVerbose();

    Property<Boolean> getCreateSourceMaps();

    DirectoryProperty getBaseDirectory();

    DirectoryProperty getWorkingDirectory();

    DirectoryProperty getOutputDir();

    DirectoryProperty getExtractedCandiesJavascriptDir();

    Property<String> getClassPath();

    Property<String> getNodeVersion();
  }
}
