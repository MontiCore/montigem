package de.montigem.gradle.specific;

import de.montigem.gradle.common.WarmIsolation;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Transpile using JSweet
 * A WorkAction to allow different classes
 * JSweet runs in a class loader that stays open between builds, submit this action without isolation.
 */
public abstract class Java2TSAction implements WorkAction<Java2TSAction.Params> {

  protected static final WarmIsolation isolator = new JSweetIsolation();

  /**
   * An older JSweet keeps the adapters of its first transpilation in its own class loader. With such a JSweet
   * every run gets a fresh loader, as it did with Gradle's classLoaderIsolation.
   */
  protected static final WarmIsolation singleUseIsolator = new JSweetIsolation() {
    @Override
    protected long idleMillis() {
      return COLD_IDLE_MS;
    }
  };

  private static final Map<String, Boolean> WARM_LOADER_SUPPORT = new ConcurrentHashMap<>();

  @Inject
  protected abstract ObjectFactory getObjects();

  @Override
  public void execute() {
    Params params = getParameters();
    JSweetSettings settings = new JSweetSettings();
    settings.base = params.getBaseDirectory().getAsFile().get();
    settings.working = params.getWorkingDirectory().getAsFile().get();
    settings.out = params.getOutputDir().getAsFile().get();
    settings.candies = params.getExtractedCandiesJavascriptDir().getAsFile().get();
    settings.classPath = params.getClassPath().get();
    settings.nodeVersion = params.getNodeVersion().get();
    settings.verbose = params.getVerbose().getOrElse(false);
    settings.sourceMaps = params.getCreateSourceMaps().getOrElse(false);
    settings.input.addAll(params.getInput().getFiles());
    settings.excluded.addAll(params.getExcludedFromTranspilation().getFiles());
    settings.outputList = params.getOutputList().getAsFile().getOrNull();
    settings.autoPropagateAsync = params.getAutoPropagateAsync().getOrElse(true);
    String[] args = settings.toArgs();

    Set<File> classpath = params.getJSweetClassPath().getFiles();
    if (supportsWarmLoader(classpath)) {
      isolator.run(JSweetInvoker.class.getName(), args, "[jsweet] ", classpath, getObjects());
    }
    else {
      Set<File> singleUse = new LinkedHashSet<>(classpath);
      // a path no other run has, so the isolator never hands this loader to a later run
      singleUse.add(new File(System.getProperty("java.io.tmpdir"), "jsweet-single-use-" + UUID.randomUUID()));
      singleUseIsolator.run(JSweetInvoker.class.getName(), args, "[jsweet] ", singleUse, getObjects());
    }
  }

  /**
   * True if the JSweet on the classpath loads the adapters of each transpiler in a class loader of its own.
   */
  static boolean supportsWarmLoader(Set<File> classpath) {
    for (File f : classpath) {
      if (f.getName().startsWith("jsweet-transpiler") && f.getName().endsWith(".jar")) {
        String key = f.getAbsolutePath() + '|' + f.length() + '|' + f.lastModified();
        return WARM_LOADER_SUPPORT.computeIfAbsent(key, k -> declaresAdapterClassLoader(f));
      }
    }
    return false;
  }

  private static boolean declaresAdapterClassLoader(File jar) {
    try (JarFile jarFile = new JarFile(jar)) {
      JarEntry options = jarFile.getJarEntry("org/jsweet/transpiler/JSweetOptions.class");
      if (options == null) {
        return false;
      }
      try (InputStream in = jarFile.getInputStream(options)) {
        return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains("getAdapterClassLoader");
      }
    }
    catch (IOException e) {
      return false;
    }
  }

  static class JSweetIsolation extends WarmIsolation {
    @Override
    protected Set<String> getPassThroughPackages() {
      // the isolated loader has no parent, JSweet needs the compiler API of the JDK
      Set<String> packages = super.getPassThroughPackages();
      ModuleLayer.boot().findModule("java.compiler")
          .ifPresent(m -> m.getPackages().forEach(p -> packages.add(p + ".")));
      return packages;
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

    /**
     * Optional, see {@link JSweetSettings#outputList}.
     */
    RegularFileProperty getOutputList();

    /**
     * Optional, see {@link JSweetSettings#autoPropagateAsync}.
     */
    Property<Boolean> getAutoPropagateAsync();

    /**
     * JSweet itself, loaded in the warm class loader.
     */
    ConfigurableFileCollection getJSweetClassPath();
  }
}
