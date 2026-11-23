package resources.exectests;

import util.JSweetTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

  /** switch over a DayOfWeek */
  @JSweetTest
  public static String checkSwitchOverDayOfWeek() {
    String result = "";
    for (int day = 16; day <= 22; day++) {
      switch (LocalDate.of(2026, 11, day).getDayOfWeek()) {
        case SATURDAY:
        case SUNDAY:
          result += "w";
          break;
        case FRIDAY:
          result += "f";
          break;
        default:
          result += "d";
      }
    }
    return result;
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
  public static boolean checkObjectsEqualsWithNull() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    LocalDate none = null;
    return Objects.equals(date, LocalDate.of(2024, 2, 29)) && !Objects.equals(date, none)
        && !Objects.equals(none, date) && Objects.equals(none, none);
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

  /** lengthOfYear, withSecond, LocalDate.getMonth, LocalDateTime.isEqual */
  @JSweetTest
  public static String checkLengthOfYearWithSecondIsEqual() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 12, 34, 56);
    return dateTime.toLocalDate().lengthOfYear() + " " + LocalDate.of(2023, 6, 1).lengthOfYear() + " "
        + dateTime.withSecond(7) + " " + dateTime.toLocalDate().getMonth() + " "
        + dateTime.isEqual(LocalDateTime.of(2024, 2, 29, 12, 34, 56)) + " " + dateTime.isEqual(dateTime.plusNanos(1));
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
