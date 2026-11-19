/* (c) https://github.com/MontiCore/monticore */
package gui.test;

import jsweettest.TimeValues;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Consumer;

/** java.time attributes in the browser, see TimeValuesE2ETest */
public class TestTimeValues extends TestTimeValuesTOP {

  @Override
  public String describe(TimeValues v) {
    String history = "";
    for (int i = 0; i < v.sizeHistory(); i++) {
      history += " " + v.getHistory(i);
    }
    String pause = v.getPause().isPresent() ? "" + v.getPause().get() : "none";
    return "start=" + v.getStart() + " end=" + v.getStart().plus(v.getLength()) + " length=" + v.getLength()
        + " minutes=" + v.getLength().toMinutes() + " berlin="
        + v.getStart().atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime() + " reminder=" + v.getReminder()
        + " pause=" + pause + " history=" + history.trim();
  }

  /** values after 1970, a positive duration, no pause, one history entry */
  @Override
  public Consumer<Void> setA(TimeValues v) {
    return Void -> set(v, Instant.parse("2026-11-17T08:00:00.123456Z"), Duration.ofMinutes(90).plusNanos(5_000),
        LocalTime.of(9, 15), Optional.empty());
  }

  /** values before 1970, a negative duration, a pause, two history entries */
  @Override
  public Consumer<Void> setB(TimeValues v) {
    return Void -> {
      set(v, Instant.parse("1969-12-31T23:59:59.5Z"), Duration.ofSeconds(-90, 250_000_000),
          LocalTime.of(23, 59, 59, 999_999_000), Optional.of(Duration.ofDays(2)));
      v.addHistory(v.sizeHistory(), Instant.ofEpochSecond(1_794_907_815L, 123_000));
    };
  }

  protected void set(TimeValues v, Instant start, Duration length, LocalTime reminder, Optional<Duration> pause) {
    v.setStart(start);
    v.setLength(length);
    v.setReminder(reminder);
    v.setPause(pause);
    while (v.sizeHistory() > 0) {
      v.removeHistory(0);
    }
    v.addHistory(0, start);
  }
}
