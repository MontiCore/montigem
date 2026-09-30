package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.internal.isolation.CachedIsolation;

public abstract class ODs2DataAction extends AToolAction {

  protected static final CachedIsolation.WithClassPath isolator = new CachedIsolation.WithClassPath();

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    isolator.executeInClassloader(ODs2DataToolInvoker.class.getName(), "run",
        args, prefix, getParameters().getExtraClasspathElements());
  }
}
