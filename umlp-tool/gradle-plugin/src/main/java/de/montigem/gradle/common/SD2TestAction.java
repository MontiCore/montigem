package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.internal.isolation.CachedIsolation;

public abstract class SD2TestAction extends AToolAction {

  protected static final CachedIsolation.WithClassPath isolator = new CachedIsolation.WithClassPath();

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    try {
      isolator.executeInClassloader(SD2TestToolInvoker.class.getName(), "run",
          args, prefix, getParameters().getExtraClasspathElements());
    } finally {
      IsolatedToolCaches.release();
    }
  }
}
