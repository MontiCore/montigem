package resources.exectests;

import util.JSweetTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LocalDateRuntimeCheck {
  @JSweetTest
  public static boolean checkEquals() {
    return LocalDate.now().equals(LocalDate.now());
  }

  @JSweetTest
  public static boolean checkLocalDateEquality() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.equals(LocalDate.of(2024, 2, 29))
        && !date.equals(LocalDate.of(2024, 3, 1))
        && !date.equals(null);
  }

  @JSweetTest
  public static boolean checkLocalDateFactoryAndGetters() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.getYear() == 2024
        && date.getMonthValue() == 2
        && date.getDayOfMonth() == 29
        && date.getDayOfYear() == 60;
  }

  @JSweetTest
  public static boolean checkLocalDateDayOfWeek() {
    DayOfWeek leapDay = LocalDate.of(2024, 2, 29).getDayOfWeek();
    return leapDay == DayOfWeek.THURSDAY
        && leapDay.toString().equals("THURSDAY")
        && LocalDate.of(2024, 3, 3).getDayOfWeek().getValue() == 7
        && LocalDate.of(2024, 3, 4).getDayOfWeek().getValue() == 1
        && LocalDate.of(2024, 3, 4).getDayOfWeek().name().equals("MONDAY");
  }

  @JSweetTest
  public static boolean checkLocalDateParseAndToString() {
    LocalDate date = LocalDate.parse("2024-02-29");
    return date.toString().equals("2024-02-29");
  }

  @JSweetTest
  public static boolean checkLocalDateArithmetic() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.plusDays(1).equals(LocalDate.of(2024, 3, 1))
        && date.minusWeeks(1).equals(LocalDate.of(2024, 2, 22))
        && date.plusMonths(1).equals(LocalDate.of(2024, 3, 29))
        && date.plusYears(1).equals(LocalDate.of(2025, 2, 28));
  }

  @JSweetTest
  public static boolean checkLocalDateComparison() {
    LocalDate earlier = LocalDate.of(2024, 2, 28);
    LocalDate later = LocalDate.of(2024, 2, 29);
    return earlier.isBefore(later)
        && later.isAfter(earlier)
        && later.isEqual(LocalDate.of(2024, 2, 29))
        && earlier.compareTo(later) < 0;
  }

  @JSweetTest
  public static boolean checkLocalDateTimeEquals() {
    LocalDateTime now = LocalDateTime.now();
    return now.equals(now);
  }

  @JSweetTest
  public static boolean checkLocalDateTimeFactoryAndGetters() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 12, 34, 56, 123_000_000);
    return dateTime.getYear() == 2024
        && dateTime.getMonthValue() == 2
        && dateTime.getDayOfMonth() == 29
        && dateTime.getHour() == 12
        && dateTime.getMinute() == 34
        && dateTime.getSecond() == 56
        && dateTime.getNano() == 123_000_000;
  }

  @JSweetTest
  public static boolean checkLocalDateTimeDayOfWeek() {
    return LocalDateTime.of(2024, 3, 3, 23, 30).getDayOfWeek() == DayOfWeek.SUNDAY;
  }

  @JSweetTest
  public static boolean checkLocalDateTimeParseAndToString() {
    LocalDateTime dateTime = LocalDateTime.parse("2024-02-29T12:34:56");
    LocalDateTime withoutSeconds = LocalDateTime.of(2024, 2, 29, 12, 34, 0);
    LocalDateTime withMillis = LocalDateTime.of(2024, 2, 29, 12, 34, 56, 123_000_000);
    return dateTime.toString().equals("2024-02-29T12:34:56")
        && withoutSeconds.toString().equals("2024-02-29T12:34")
        && withMillis.toString().equals("2024-02-29T12:34:56.123");
  }

  @JSweetTest
  public static boolean checkLocalDateTimeArithmetic() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 23, 30, 0);
    return dateTime.plusHours(2).equals(LocalDateTime.of(2024, 3, 1, 1, 30, 0))
        && dateTime.minusMinutes(30).equals(LocalDateTime.of(2024, 2, 29, 23, 0, 0))
        && dateTime.plusSeconds(30).equals(LocalDateTime.of(2024, 2, 29, 23, 30, 30));
  }

  @JSweetTest
  public static boolean checkLocalDateTimeComparison() {
    LocalDateTime earlier = LocalDateTime.of(2024, 2, 29, 12, 0, 0);
    LocalDateTime later = LocalDateTime.of(2024, 2, 29, 13, 0, 0);
    return earlier.isBefore(later)
        && later.isAfter(earlier)
        && earlier.compareTo(later) < 0;
  }

  @JSweetTest
  public static boolean checkToLocalDate() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 12, 34, 56);
    return dateTime.toLocalDate().equals(LocalDate.of(2024, 2, 29));
  }
}
