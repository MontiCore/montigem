/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.internal.isolation.CachedIsolation;

import java.util.concurrent.TimeUnit;

/**
 * (classpath isolated) execution of the UMLPTool for a Gradle WorkAction.
 * This indirect calling is required to allow for parallel execution despite static usages (e.g., Mills)
 */
public abstract class MGAction extends AToolAction {

  /**
   * How long an unused generator class loader stays open. A warm loader saves the class loading and
   * JIT warm-up of the next build, e.g. after a model or GUI edit.
   */
  public static final String IDLE_SECONDS_PROPERTY = "umlp.toolLoaderIdleSeconds";
  protected static final long DEFAULT_IDLE_SECONDS = 600;

  /**
   * An edit-and-build loop uses up to 3 loaders. A large parallel build leaves many more, these close as before.
   */
  protected static final int MAX_WARM_LOADERS = 4;
  protected static final long COLD_IDLE_MS = 6000;

  protected static final CachedIsolation.WithClassPath isolator = new CachedIsolation.WithClassPath() {
    @Override
    protected synchronized void cleanupOld(long threshold) {
      super.cleanupOld(internalRunners.size() > MAX_WARM_LOADERS ? Math.min(threshold, COLD_IDLE_MS) : threshold);
    }
  };

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    isolator.setCloseThreshold(TimeUnit.SECONDS.toMillis(Long.getLong(IDLE_SECONDS_PROPERTY, DEFAULT_IDLE_SECONDS)));
    isolator.executeInClassloader(UMLPToolInvoker.class.getName(), "run",
        args, prefix, getParameters().getExtraClasspathElements());
  }
}
