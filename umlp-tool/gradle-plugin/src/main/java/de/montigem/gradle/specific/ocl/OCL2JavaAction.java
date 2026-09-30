package de.montigem.gradle.specific.ocl;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.internal.isolation.CachedIsolation;

public abstract class OCL2JavaAction extends AToolAction {

  protected static final CachedIsolation.WithClassPath isolator = new CachedIsolation.WithClassPath();

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    isolator.executeInClassloader(OCL2JavaToolInvoker.class.getName(), "run",
        args, prefix, getParameters().getExtraClasspathElements());
  }
}