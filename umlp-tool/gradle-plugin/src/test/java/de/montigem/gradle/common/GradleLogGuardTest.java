/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.MCFatalError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GradleLogGuardTest {

  @AfterEach
  public void resetLog() {
    Log.clearFindings();
  }

  @Test
  public void errorFailsTheTaskInsteadOfExitingTheDaemon() {
    // the default Log would call System.exit here and abort the whole test JVM
    Log.init();
    GradleLogGuard.ensureGradleLog();
    assertThrows(MCFatalError.class, () -> Log.error("0xTEST1 simulated generator error"));
  }

  @Test
  public void keepsAnExistingGradleLog() {
    GradleLogGuard.ensureGradleLog();
    Log.warn("0xTEST2 finding that must survive a second guard call");
    GradleLogGuard.ensureGradleLog();
    assertThrows(MCFatalError.class, () -> Log.error("0xTEST3 simulated generator error"));
  }
}
