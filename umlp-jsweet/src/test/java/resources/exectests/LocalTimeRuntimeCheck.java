/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

  /** values of different java.time types are never equal, even if they denote the same day */
  @JSweetTest
  public static boolean checkObjectsEqualsOfDifferentTypes() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    Object dateTime = date.atStartOfDay();
    Object text = "2024-02-29";
    return !Objects.equals(date, dateTime) && !Objects.equals(dateTime, date) && !Objects.equals(date, text);
  }
}
