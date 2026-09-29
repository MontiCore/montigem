/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.monticore.gradle.common.GradleLog;
import de.se_rwth.commons.logging.Log;

/**
 * Code running inside the Gradle daemon must log via {@link GradleLog}: its error hook throws an
 * exception, while the default error hook calls System.exit and terminates the whole daemon.
 */
public class GradleLogGuard extends Log {

  public static synchronized void ensureGradleLog() {
    if (!(getLog() instanceof GradleLog)) {
      GradleLog.init();
    }
  }
}
