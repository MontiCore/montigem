package de.montigem.gradle.specific;

import de.montigem.gradle.common.WarmIsolation;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Transpile using JSweet
 * A WorkAction to allow different classes
 * JSweet runs in a class loader that stays open between builds, submit this action without isolation.
 */
public abstract class Java2TSAction implements WorkAction<Java2TSAction.Params> {

  protected static final WarmIsolation isolator = new WarmIsolation() {
    @Override
    protected Set<String> getPassThroughPackages() {
      // the isolated loader has no parent, JSweet needs the compiler API of the JDK
      Set<String> packages = super.getPassThroughPackages();
      ModuleLayer.boot().findModule("java.compiler")
          .ifPresent(m -> m.getPackages().forEach(p -> packages.add(p + ".")));
      return packages;
    }
  };

  @Inject
  protected abstract ObjectFactory getObjects();

  @Override
  public void execute() {
    Params params = getParameters();
    List<String> args = new ArrayList<>(List.of(
        JSweetInvoker.BASE, params.getBaseDirectory().getAsFile().get().getPath(),
        JSweetInvoker.WORKING, params.getWorkingDirectory().getAsFile().get().getPath(),
        JSweetInvoker.OUT, params.getOutputDir().getAsFile().get().getPath(),
        JSweetInvoker.CANDIES, params.getExtractedCandiesJavascriptDir().getAsFile().get().getPath(),
        JSweetInvoker.CLASSPATH, params.getClassPath().get(),
        JSweetInvoker.NODE_VERSION, params.getNodeVersion().get(),
        JSweetInvoker.VERBOSE, String.valueOf(params.getVerbose().getOrElse(false)),
        JSweetInvoker.SOURCE_MAPS, String.valueOf(params.getCreateSourceMaps().getOrElse(false))));
    for (File f : params.getInput().getFiles()) {
      args.add(JSweetInvoker.INPUT);
      args.add(f.getPath());
    }
    for (File f : params.getExcludedFromTranspilation().getFiles()) {
      args.add(JSweetInvoker.EXCLUDE);
      args.add(f.getPath());
    }
    isolator.run(JSweetInvoker.class.getName(), args.toArray(String[]::new), "[jsweet] ",
        params.getJSweetClassPath().getFiles(), getObjects());
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
     * JSweet itself, loaded in the warm class loader.
     */
    ConfigurableFileCollection getJSweetClassPath();
  }
}
