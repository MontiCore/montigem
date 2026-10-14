/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.monticore.gradle.common.AToolAction;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/**
 * (classpath isolated) execution of the UMLPTool for a Gradle WorkAction.
 * This indirect calling is required to allow for parallel execution despite static usages (e.g., Mills)
 */
public abstract class MGAction extends AToolAction {

  protected static final WarmIsolation isolator = new WarmIsolation();

  @Inject
  protected abstract ObjectFactory getObjects();

  @Override
  protected void doRun(String[] args) {
    final String prefix = "[" + getParameters().getProgressName().get() + "] ";
    isolator.run(UMLPToolInvoker.class.getName(), args, prefix,
        getParameters().getExtraClasspathElements().getFiles(), getObjects());
  }
}
