/* (c) https://github.com/MontiCore/monticore */
package umlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportLevelTest {

  @Test
  void readsTheOptionValueInAnyCase() {
    assertEquals(ReportLevel.INCGEN, ReportLevel.of("incgen"));
    assertEquals(ReportLevel.ALL, ReportLevel.of("All"));
  }

  @Test
  void rejectsAnUnknownLevel() {
    assertThrows(IllegalArgumentException.class, () -> ReportLevel.of("none"));
  }
}
