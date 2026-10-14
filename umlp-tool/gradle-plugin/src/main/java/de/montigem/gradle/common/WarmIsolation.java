/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.monticore.gradle.internal.isolation.CachedIsolation;
import org.gradle.api.model.ObjectFactory;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs a tool in a class loader that stays open between builds. A warm loader saves the class loading and
 * JIT warm-up of the next build, e.g. after a model or GUI edit.
 */
public class WarmIsolation extends CachedIsolation.WithClassPath {

  /**
   * How long an unused tool class loader stays open.
   */
  public static final String IDLE_SECONDS_PROPERTY = "umlp.toolLoaderIdleSeconds";
  protected static final long DEFAULT_IDLE_SECONDS = 600;

  /**
   * An edit-and-build loop uses up to 3 loaders. A large parallel build leaves many more, these close as before.
   */
  protected static final int MAX_WARM_LOADERS = 4;
  protected static final long COLD_IDLE_MS = 6000;

  protected long idleMillis() {
    return TimeUnit.SECONDS.toMillis(Long.getLong(IDLE_SECONDS_PROPERTY, DEFAULT_IDLE_SECONDS));
  }

  @Override
  protected synchronized void cleanupOld(long threshold) {
    super.cleanupOld(internalRunners.size() > MAX_WARM_LOADERS ? Math.min(threshold, COLD_IDLE_MS) : threshold);
  }

  /**
   * Calls the static method run(String[]) of the given class in a warm class loader for the classpath.
   */
  public void run(String className, String[] args, String prefix, Set<File> classpath, ObjectFactory objects) {
    setCloseThreshold(idleMillis());
    executeInClassloader(className, "run", args, prefix,
        objects.fileCollection().from(ToolClasspathCopies.of(classpath)));
  }
}
