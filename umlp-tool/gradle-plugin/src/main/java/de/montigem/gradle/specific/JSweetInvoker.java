/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import com.kohlschutter.jdk.standaloneutil.JavaHomeLocator;
import de.montigem.gradle.common.GradleLogGuard;
import org.gradle.workers.WorkAction;
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
 * Transpiles with JSweet. It runs in a JSweet class loader that stays open between builds, see {@link Java2TSAction}.
 */
public class JSweetInvoker {

  public static void run(String[] args) {
    GradleLogGuard.ensureGradleLog();
    JSweetSettings settings = JSweetSettings.parse(args);

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

    JSweetTranspiler transpiler = new JSweetTranspiler(settings.base, null, factory, settings.working,
        settings.out, settings.out, settings.candies, settings.classPath);

    transpiler.setTscWatchMode(false);
    transpiler.setAutoPropagateAsyncAwaits(true);
    transpiler.setVerbose(settings.verbose);
    transpiler.setEncoding("UTF-8");
    transpiler.setGenerateSourceMaps(settings.sourceMaps);
    transpiler.setGenerateJsFiles(false);
    transpiler.setEcmaTargetVersion(EcmaScriptComplianceLevel.ES6);
    transpiler.setModuleKind(ModuleKind.commonjs);
    ProcessUtil.addExtraPath(".gradle/nodejs/node-v" + settings.nodeVersion + "-win-x64");

    SourceFile[] sourceFiles = settings.input.stream().map(SourceFile::new).toArray(SourceFile[]::new);
    Set<String> excludedSources = settings.excluded.stream()
        .map(SourceFile::new).map(SourceFile::toString).collect(Collectors.toSet());

    ErrorCountTranspilationHandler transpilationHandler = new ErrorCountTranspilationHandler(new ConsoleTranspilationHandler());
    // closing releases the classpath jars that javac keeps open, else they stay locked in the daemon
    try (transpiler) {
      transpiler.transpile(transpilationHandler, excludedSources, sourceFiles);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      // JSweet keeps its context in thread locals of the pooled worker thread, which keeps the whole class loader alive
      JSweetContext.current.remove();
      JSweetContext.currentCompilationUnit.remove();
    }
    if (transpilationHandler.getErrorCount() > 0) {
      throw new RuntimeException("JSweet Transpile failed with errors.");
    }
  }
}
