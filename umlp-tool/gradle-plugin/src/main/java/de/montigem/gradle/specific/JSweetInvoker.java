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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Transpiles with JSweet. It runs in a JSweet class loader that stays open between builds, see {@link Java2TSAction}.
 */
public class JSweetInvoker {

  static final String BASE = "--base";
  static final String WORKING = "--working";
  static final String OUT = "--out";
  static final String CANDIES = "--candies";
  static final String CLASSPATH = "--classpath";
  static final String NODE_VERSION = "--node-version";
  static final String VERBOSE = "--verbose";
  static final String SOURCE_MAPS = "--source-maps";
  static final String INPUT = "--input";
  static final String EXCLUDE = "--exclude";

  public static void run(String[] args) {
    GradleLogGuard.ensureGradleLog();

    File base = null;
    File working = null;
    File out = null;
    File candies = null;
    String classPath = null;
    String nodeVersion = null;
    boolean verbose = false;
    boolean sourceMaps = false;
    List<SourceFile> input = new ArrayList<>();
    Set<String> excluded = new HashSet<>();
    for (int i = 0; i + 1 < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case BASE -> base = new File(value);
        case WORKING -> working = new File(value);
        case OUT -> out = new File(value);
        case CANDIES -> candies = new File(value);
        case CLASSPATH -> classPath = value;
        case NODE_VERSION -> nodeVersion = value;
        case VERBOSE -> verbose = Boolean.parseBoolean(value);
        case SOURCE_MAPS -> sourceMaps = Boolean.parseBoolean(value);
        case INPUT -> input.add(new SourceFile(new File(value)));
        case EXCLUDE -> excluded.add(new SourceFile(new File(value)).toString());
        default -> throw new IllegalArgumentException("Unknown JSweet argument " + args[i]);
      }
    }

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

    JSweetTranspiler transpiler = new JSweetTranspiler(base, null, factory, working, out, out, candies, classPath);

    transpiler.setTscWatchMode(false);
    transpiler.setAutoPropagateAsyncAwaits(true);
    transpiler.setVerbose(verbose);
    transpiler.setEncoding("UTF-8");
    transpiler.setGenerateSourceMaps(sourceMaps);
    transpiler.setGenerateJsFiles(false);
    transpiler.setEcmaTargetVersion(EcmaScriptComplianceLevel.ES6);
    transpiler.setModuleKind(ModuleKind.commonjs);
    ProcessUtil.addExtraPath(".gradle/nodejs/node-v" + nodeVersion + "-win-x64");

    ErrorCountTranspilationHandler transpilationHandler = new ErrorCountTranspilationHandler(new ConsoleTranspilationHandler());
    // closing releases the classpath jars that javac keeps open, else they stay locked in the daemon
    try (transpiler) {
      transpiler.transpile(transpilationHandler, excluded, input.toArray(SourceFile[]::new));
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
