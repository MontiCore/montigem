/* (c) https://github.com/MontiCore/monticore */
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** translated java.time methods declared in interfaces (java.time.chrono) or DayOfWeek: no 0xEE009 */
public class TimeCoco {

  public void translated() {
    ZonedDateTime zoned = ZonedDateTime.of(2026, 11, 17, 9, 0, 0, 0, ZoneId.of("Europe/Berlin"));
    LocalDateTime local = LocalDateTime.of(2026, 11, 17, 9, 0);
    boolean before = zoned.isBefore(zoned.plusHours(1)) && zoned.isEqual(zoned) && zoned.compareTo(zoned) == 0;
    long seconds = zoned.toEpochSecond() + local.toEpochSecond(ZoneOffset.UTC);
    int day = DayOfWeek.MONDAY.getValue() + local.getDayOfWeek().getValue();
    Object instant = local.toInstant(ZoneOffset.UTC);
  }

  /** not translated: 0xEE009 */
  public Object untranslated() {
    return ZonedDateTime.now().getChronology();
  }
}
