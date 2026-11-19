/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * LocalTime and the precision of LocalDateTime in transpiled code (Temporal.PlainTime/PlainDateTime): nanoseconds,
 * toString like java.time, conversions between the types
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

  @JSweetTest
  public static String checkLocalDateTimeNanos() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 12, 34, 56, 123_456_789);
    return dateTime + " " + dateTime.getNano() + " " + LocalDateTime.parse("2024-02-29T12:34:56.000001") + " "
        + dateTime.plusNanos(1);
  }

  @JSweetTest
  public static String checkConversions() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    LocalTime time = LocalTime.of(12, 34, 56);
    LocalDateTime dateTime = LocalDateTime.of(date, time);
    return dateTime + " " + dateTime.toLocalDate() + " " + dateTime.toLocalTime() + " " + date.atTime(time) + " "
        + date.atTime(7, 30) + " " + date.atStartOfDay();
  }

  @JSweetTest
  public static String checkWith() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 1, 31, 12, 0);
    return dateTime.withMonth(2) + " " + dateTime.withYear(2023).withDayOfMonth(15) + " " + dateTime.withHour(23)
        + " " + dateTime.withDayOfYear(60) + " " + dateTime.toLocalDate().lengthOfMonth() + " "
        + dateTime.toLocalDate().isLeapYear();
  }

  @JSweetTest
  public static boolean checkObjectsEquals() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    LocalDate none = null;
    return Objects.equals(date, LocalDate.of(2024, 2, 29)) && !Objects.equals(date, none)
        && !Objects.equals(none, date) && Objects.equals(none, none);
  }

  /** string building with java.time values: the format of java.time, not the one of Temporal */
  @JSweetTest
  public static String checkStringBuilding() {
    LocalTime time = LocalTime.of(10, 15);
    LocalDate date = LocalDate.of(2024, 2, 29);
    String s = "at ";
    s += time;
    String[] parts = {"from "};
    parts[0] += time;
    StringBuilder sb = new StringBuilder("on ");
    sb.append(date).append(' ').append(LocalDateTime.of(date, time));
    return s + " " + parts[0] + " " + sb + " " + String.valueOf(time) + " " + String.valueOf((LocalTime) null);
  }

  /** invalid values for the with methods are rejected, withYear and withMonth adjust the day */
  @JSweetTest
  public static String checkWithInvalid() {
    LocalDate date = LocalDate.of(2024, 1, 31);
    String result = date.withMonth(2) + " " + date.withYear(2023).withMonth(2);
    try {
      result += " " + date.withMonth(4).withDayOfMonth(31);
    } catch (Exception e) {
      result += " day rejected";
    }
    try {
      result += " " + LocalDateTime.of(2024, 1, 31, 12, 0).withHour(24);
    } catch (Exception e) {
      result += " hour rejected";
    }
    try {
      result += " " + LocalDate.of(2023, 1, 1).withDayOfYear(366);
    } catch (Exception e) {
      result += " day of year rejected";
    }
    return result;
  }

  /** values of different java.time types are never equal, even if they denote the same day */
  @JSweetTest
  public static boolean checkObjectsEqualsOfDifferentTypes() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    Object dateTime = date.atStartOfDay();
    Object text = "2024-02-29";
    return !Objects.equals(date, dateTime) && !Objects.equals(dateTime, date) && !Objects.equals(date, text);
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

  /** lengthOfYear, withSecond, LocalDate.getMonth, LocalDateTime.isEqual */
  @JSweetTest
  public static String checkRemainingGettersAndWith() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 12, 34, 56);
    return dateTime.toLocalDate().lengthOfYear() + " " + LocalDate.of(2023, 6, 1).lengthOfYear() + " "
        + dateTime.withSecond(7) + " " + dateTime.toLocalDate().getMonth() + " "
        + dateTime.isEqual(LocalDateTime.of(2024, 2, 29, 12, 34, 56)) + " " + dateTime.isEqual(dateTime.plusNanos(1));
  }

  /** null values in string building print "null", like Java */
  @JSweetTest
  public static String checkNullInStringBuilding() {
    LocalDate date = null;
    Duration duration = null;
    ChronoUnit unit = null;
    String s = "s:";
    s += date;
    StringBuilder sb = new StringBuilder("sb:");
    sb.append(duration).append(unit);
    return "a:" + date + " " + duration + " " + unit + " " + s + " " + sb + " " + String.valueOf(duration);
  }

  /** units a type does not support and invalid arguments are rejected, like Java */
  @JSweetTest
  public static String checkUnsupportedUnitsRejected() {
    LocalDate date = LocalDate.of(2026, 11, 17);
    LocalDateTime dateTime = date.atTime(9, 30);
    String result = "";
    try {
      result += date.until(date.plusDays(1), ChronoUnit.HOURS);
    } catch (Exception e) {
      result += "until rejected";
    }
    try {
      result += " " + dateTime.truncatedTo(ChronoUnit.WEEKS);
    } catch (Exception e) {
      result += " truncatedTo rejected";
    }
    try {
      result += " " + LocalTime.of(9, 0).plus(1, ChronoUnit.DAYS);
    } catch (Exception e) {
      result += " plus rejected";
    }
    try {
      result += " " + LocalTime.of(9, 0).until(LocalTime.of(10, 0), ChronoUnit.DAYS);
    } catch (Exception e) {
      result += " time until rejected";
    }
    try {
      result += " " + date.plus(1, ChronoUnit.HOURS);
    } catch (Exception e) {
      result += " date plus rejected";
    }
    try {
      result += " " + LocalTime.ofSecondOfDay(86_400);
    } catch (Exception e) {
      result += " second of day rejected";
    }
    return result;
  }
}
