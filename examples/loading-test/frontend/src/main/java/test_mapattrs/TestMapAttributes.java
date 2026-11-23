/* (c) https://github.com/MontiCore/monticore */
package test_mapattrs;

import loadingtests.Gradebook;
import loadingtests.MyEnum;
import umlp.backendrte.common.GemValueMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs map attribute scenarios in the browser; see loadingtests.e2e.TestMapAttributes
 */
public class TestMapAttributes extends TestMapAttributesTOP {

  @Override
  public Consumer<Void> putGrade(Gradebook g) {
    return $ -> g.putGrades("art", 2);
  }

  @Override
  public Consumer<Void> replaceGrade(Gradebook g) {
    return $ -> g.putGrades("math", 3);
  }

  @Override
  public Consumer<Void> removeGrade(Gradebook g) {
    return $ -> g.removeGrades("math");
  }

  /**
   * the entries in their order, e.g. "math=1;art=2"
   */
  @Override
  public Consumer<Void> readGrades(Gradebook g) {
    return $ -> {
      String result = "";
      for (Map.Entry<String, Integer> e : g.getGradesMap().entrySet()) {
        result += (result.isEmpty() ? "" : ";") + e.getKey() + "=" + e.getValue();
      }
      setResult(result);
    };
  }

  /**
   * the largest long key a browser represents exactly
   */
  @Override
  public Consumer<Void> putState(Gradebook g) {
    return $ -> g.putStates(GemValueMap.MAX_SAFE_KEY, MyEnum.B);
  }

  @Override
  public Consumer<Void> readStates(Gradebook g) {
    return $ -> setResult("size=" + g.sizeStates() + ";max=" + g.getStates(GemValueMap.MAX_SAFE_KEY).name());
  }

  @Override
  public Consumer<Void> readDueDate(Gradebook g) {
    return $ -> setResult("due=" + g.getDueDates("math"));
  }

  @Override
  public Consumer<Void> putDueDate(Gradebook g) {
    return $ -> g.putDueDates("exam", LocalDate.of(2027, 2, 10));
  }

  /**
   * increments the visits of a loaded day, with a key created here: equal dates are the same key
   */
  @Override
  public Consumer<Void> countVisit(Gradebook g) {
    return $ -> g.putVisitsByDay(LocalDate.of(2026, 11, 17), g.getVisitsByDay(LocalDate.of(2026, 11, 17)) + 1);
  }

  @Override
  public Consumer<Void> readVisits(Gradebook g) {
    return $ -> setResult("size=" + g.sizeVisitsByDay() + ";visits=" + g.getVisitsByDay(LocalDate.of(2026, 11, 17)));
  }

  /**
   * LocalTime, LocalDateTime and Instant keys put by the backend, read with keys created here; the map in the format
   * of Java
   */
  @Override
  public Consumer<Void> readTimes(Gradebook g) {
    return $ -> setResult("slot=" + g.getSlot(LocalTime.of(10, 0)) + ";note="
        + g.getNoteAt(LocalDateTime.of(2026, 11, 17, 10, 0, 30, 500_000_000)) + ";reading="
        + g.getReadingAt(Instant.parse("2026-11-17T09:00:00.123456Z")) + ";slots=" + g.getSlotMap());
  }

  /**
   * a key finer than the microseconds a database stores is refused
   */
  @Override
  public Consumer<Void> putFineReading(Gradebook g) {
    return $ -> {
      g.putReadingAt(Instant.parse("2026-11-17T09:00:00.1234567Z"), "fine");
      setResult("readings=" + g.getReadingAtMap().size());
    };
  }
}
