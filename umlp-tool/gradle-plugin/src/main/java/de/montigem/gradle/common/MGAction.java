/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import de.monticore.gradle.internal.isolation.CachedIsolation;

/**
 * (classpath isolated) execution of the UMLPTool for a Gradle WorkAction.
 * This indirect calling is required to allow for parallel execution despite static usages (e.g., Mills)
 */
public abstract class MGAction extends AToolAction {

  protected static final CachedIsolation.WithClassPath isolator = new CachedIsolation.WithClassPath();

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    isolator.executeInClassloader(UMLPToolInvoker.class.getName(), "run",
        args, prefix, getParameters().getExtraClasspathElements());
  }
}
