/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * LocalTime in transpiled code (Temporal.PlainTime): nanoseconds, toString like java.time, until, truncatedTo and
 * withNano of the local types, second and nano of day
 */
public class LocalTimeRuntimeCheck {
  @JSweetTest
  public static String checkLocalTimeOfAndToString() {
    return LocalTime.of(9, 5) + " " + LocalTime.of(9, 5, 7) + " " + LocalTime.of(9, 5, 7, 120_000_000) + " "
        + LocalTime.of(9, 5, 7, 123_400) + " " + LocalTime.of(9, 5, 7, 5);
  }

  @JSweetTest
  public static String checkLocalTimeParseAndGetters() {
    LocalTime time = LocalTime.parse("23:59:58.123456789");
    return time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + "." + time.getNano();
  }

  @JSweetTest
  public static String checkLocalTimeArithmetic() {
    LocalTime time = LocalTime.of(23, 30);
    return time.plusHours(1) + " " + time.minusMinutes(45) + " " + time.plusSeconds(90) + " "
        + time.plusNanos(1_500);
  }

  @JSweetTest
  public static boolean checkLocalTimeComparison() {
    LocalTime earlier = LocalTime.of(8, 0);
    LocalTime later = LocalTime.of(8, 0, 0, 1);
    return earlier.isBefore(later) && later.isAfter(earlier) && earlier.compareTo(later) < 0
        && earlier.equals(LocalTime.of(8, 0)) && !earlier.equals(later);
  }

  /** until(end, unit), truncatedTo(unit), withNano(n) of the local types */
  @JSweetTest
  public static String checkUntilTruncatedToWithNano() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 11, 17, 9, 47, 31, 123_456_789);
    LocalTime time = dateTime.toLocalTime();
    String result = LocalDate.of(2026, 1, 31).until(LocalDate.of(2027, 3, 1), ChronoUnit.MONTHS) + " "
        + LocalDate.of(2026, 1, 31).until(LocalDate.of(2026, 1, 1), ChronoUnit.DAYS) + " "
        + dateTime.until(dateTime.plusMinutes(90), ChronoUnit.HOURS) + " "
        + time.until(LocalTime.of(8, 0), ChronoUnit.MINUTES) + " "
        + dateTime.truncatedTo(ChronoUnit.MINUTES) + " " + dateTime.truncatedTo(ChronoUnit.HOURS) + " "
        + dateTime.truncatedTo(ChronoUnit.DAYS) + " " + dateTime.truncatedTo(ChronoUnit.MILLIS) + " "
        + time.truncatedTo(ChronoUnit.HALF_DAYS) + " " + time.truncatedTo(ChronoUnit.DAYS) + " "
        + time.truncatedTo(ChronoUnit.SECONDS) + " " + dateTime.withNano(0) + " " + time.withNano(5);
    try {
      result += " " + time.withNano(1_000_000_000);
    } catch (Exception e) {
      result += " nano rejected";
    }
    return result;
  }

  /** LocalTime.atDate, second and nano of day; LocalDate epoch day */
  @JSweetTest
  public static String checkDayCounters() {
    LocalTime time = LocalTime.of(13, 45, 30, 500);
    return time.atDate(LocalDate.of(2026, 11, 17)) + " " + time.toSecondOfDay() + " " + time.toNanoOfDay() + " "
        + LocalTime.ofSecondOfDay(49_530) + " " + LocalTime.ofNanoOfDay(49_530_000_000_500L) + " "
        + LocalDate.of(2026, 11, 17).toEpochDay() + " " + LocalDate.of(1969, 12, 31).toEpochDay() + " "
        + LocalDate.ofEpochDay(20_409) + " " + LocalDate.ofEpochDay(-1);
  }
}
