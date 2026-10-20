package resources.exectests;

import util.JSweetTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Further comparisons of transpiled java.time code with Java, run in several time zones (see
 * ExecutionTests), including zones with DST changes at midnight (America/Santiago) and with a
 * 30-minute DST shift (Australia/Lord_Howe).
 *
 * <p>DST changes in 2026 used here, besides those listed in LocalDateTimeZoneRuntimeCheck:
 * Australia/Lord_Howe 2026-04-05 02:00 -> 01:30 and 2026-10-04 02:00 -> 02:30.
 *
 * <p>No LocalDateTime here lies in a period skipped by a DST start in one of the tested zones.
 * Values are converted to text with an explicit toString(): implicit string conversion of a
 * LocalDate/LocalDateTime is not translated (the JS Date's own toString is used).
 */
public class LocalDateTimeJavaSemanticsRuntimeCheck {

  private static String d(LocalDate d) {
    return d.toString();
  }

  private static String t(LocalDateTime t) {
    return t.toString();
  }

  @JSweetTest
  public static String leapDay() {
    LocalDate x = LocalDate.of(2028, 2, 29);
    return x.getDayOfWeek() + " "
        + x.getDayOfYear() + " "
        + d(x.plusDays(1)) + " "
        + d(x.minusDays(1)) + " "
        + d(x.plusMonths(1)) + " "
        + d(x.minusMonths(1)) + " "
        + d(x.plusMonths(12)) + " "
        + d(x.plusYears(1)) + " "
        + d(x.minusYears(4)) + " "
        + d(x.plusYears(4)) + " "
        + d(x.plusWeeks(52)) + " "
        + d(x.withYear(2027)) + " "
        + d(x.withDayOfMonth(1)) + " "
        + d(LocalDate.of(2028, 3, 1).minusDays(1)) + " "
        + LocalDate.of(2028, 12, 31).getDayOfYear() + " "
        + t(LocalDateTime.of(2028, 2, 29, 23, 30).plusMinutes(45)) + " "
        + t(LocalDateTime.of(2028, 2, 29, 12, 0).plusYears(1));
  }

  @JSweetTest
  public static String plusMonthsClamping() {
    return d(LocalDate.of(2026, 1, 31).plusMonths(1)) + " "
        + d(LocalDate.of(2028, 1, 31).plusMonths(1)) + " "
        + d(LocalDate.of(2026, 3, 31).minusMonths(1)) + " "
        + d(LocalDate.of(2026, 8, 31).plusMonths(1)) + " "
        + d(LocalDate.of(2026, 12, 31).plusMonths(2)) + " "
        + d(LocalDate.of(2026, 1, 31).minusMonths(1)) + " "
        + d(LocalDate.of(2026, 1, 31).plusMonths(13)) + " "
        + d(LocalDate.of(2026, 1, 31).minusMonths(25)) + " "
        + d(LocalDate.of(2026, 5, 31).plusMonths(-3)) + " "
        + t(LocalDateTime.of(2026, 1, 31, 10, 15).plusMonths(1)) + " "
        + t(LocalDateTime.of(2026, 10, 31, 2, 30).minusMonths(7)) + " "
        + d(LocalDate.of(2026, 1, 31).plusMonths(1).plusMonths(1));
  }

  @JSweetTest
  public static String calendarFieldsAroundBoundaries() {
    String s = "";
    LocalDate[] dates = {LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 29),
        LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 5), LocalDate.of(2026, 9, 6),
        LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1),
        LocalDate.of(2025, 12, 31)};
    for (LocalDate x : dates) {
      s += d(x) + ":" + x.getDayOfWeek() + "," + x.getYear() + "," + x.getMonthValue() + ","
          + x.getMonth() + "," + x.getDayOfMonth() + "," + x.getDayOfYear() + " ";
    }
    return s;
  }

  @JSweetTest
  public static String repeatedHourAtDstEnd() {
    LocalDateTime x = LocalDateTime.of(2026, 10, 25, 2, 30);
    return t(x) + " "
        + x.getHour() + " "
        + t(x.plusHours(1)) + " "
        + t(x.minusHours(1)) + " "
        + t(x.plusMinutes(30)) + " "
        + t(x.minusMinutes(31)) + " "
        + t(x.plusDays(1)) + " "
        + t(x.minusDays(1)) + " "
        + x.isBefore(LocalDateTime.of(2026, 10, 25, 3, 0)) + " "
        + x.isAfter(LocalDateTime.of(2026, 10, 25, 2, 0)) + " "
        + x.compareTo(LocalDateTime.of(2026, 10, 25, 2, 31)) + " "
        + x.equals(LocalDateTime.parse("2026-10-25T02:30")) + " "
        + LocalDateTime.of(2026, 10, 24, 2, 30).plusDays(1).equals(x) + " "
        + t(LocalDateTime.of(2026, 11, 1, 1, 30).plusHours(1)) + " "
        + t(LocalDateTime.of(2026, 11, 1, 0, 59).plusMinutes(62)) + " "
        + t(LocalDateTime.of(2026, 4, 5, 1, 45).plusMinutes(30)) + " "
        + t(LocalDateTime.of(2026, 4, 4, 23, 30).plusHours(1)) + " "
        + t(LocalDateTime.of(2026, 4, 5, 0, 30).minusHours(1));
  }

  @JSweetTest
  public static String arithmeticAcrossDstStarts() {
    return t(LocalDateTime.of(2026, 3, 29, 1, 59).plusMinutes(61)) + " "
        + t(LocalDateTime.of(2026, 3, 29, 3, 30).minusHours(2)) + " "
        + t(LocalDateTime.of(2026, 3, 8, 1, 30).plusHours(2)) + " "
        + t(LocalDateTime.of(2026, 3, 7, 12, 0).plusDays(1)) + " "
        + t(LocalDateTime.of(2026, 10, 4, 1, 45).plusMinutes(60)) + " "
        + t(LocalDateTime.of(2026, 10, 3, 12, 0).plusDays(1)) + " "
        + t(LocalDateTime.of(2026, 9, 5, 23, 30).plusHours(2)) + " "
        + t(LocalDateTime.of(2026, 9, 5, 12, 0).plusDays(1)) + " "
        + t(LocalDateTime.of(2026, 9, 6, 1, 0).minusMinutes(61)) + " "
        + t(LocalDate.of(2026, 3, 29).atStartOfDay().plusHours(24)) + " "
        + LocalDate.of(2026, 3, 29).atStartOfDay().plusHours(3).getHour() + " "
        + t(LocalDateTime.of(2026, 3, 29, 3, 0).minusSeconds(3601));
  }

  @JSweetTest
  public static String withMethodsOnLocalDateTime() {
    LocalDateTime x = LocalDateTime.of(2026, 10, 25, 23, 45, 10);
    return t(x.withHour(0)) + " "
        + t(x.withHour(2)) + " "
        + t(x.withMinute(0)) + " "
        + t(x.withSecond(0)) + " "
        + t(x.withDayOfMonth(1)) + " "
        + t(x.withMonth(2)) + " "
        + t(LocalDateTime.of(2026, 3, 31, 8, 0).withMonth(2)) + " "
        + t(x.withYear(2027)) + " "
        + t(x.withDayOfYear(1)) + " "
        + t(x.withDayOfMonth(29).withMonth(3).withHour(1)) + " "
        + t(LocalDateTime.of(2026, 10, 25, 1, 30).withMinute(59).plusMinutes(1));
  }

  @JSweetTest
  public static String comparisonsAndEquality() {
    LocalDate a = LocalDate.of(2026, 10, 25);
    LocalDate b = LocalDate.parse("2026-10-25");
    LocalDate c = LocalDate.of(2026, 10, 24).plusDays(1);
    LocalDateTime x = LocalDateTime.of(2026, 10, 25, 8, 0);
    return a.equals(b) + " " + b.equals(c) + " " + a.isEqual(c) + " "
        + a.compareTo(c) + " "
        + LocalDate.of(2028, 1, 1).compareTo(LocalDate.of(2026, 12, 31)) + " "
        + LocalDate.of(2026, 1, 5).compareTo(LocalDate.of(2026, 3, 1)) + " "
        + LocalDate.of(2026, 3, 1).compareTo(LocalDate.of(2026, 3, 31)) + " "
        + x.compareTo(x.plusSeconds(1)) + " "
        + x.compareTo(x.minusHours(5)) + " "
        + x.compareTo(x.plusYears(3)) + " "
        + x.compareTo(x.plusHours(24).minusDays(1)) + " "
        + x.isEqual(x.plusDays(1).minusHours(24)) + " "
        + a.atStartOfDay().isBefore(x) + " "
        + x.toLocalDate().equals(a) + " "
        + x.toLocalDate().isEqual(a) + " "
        + x.toLocalDate().compareTo(a) + " "
        + LocalDate.of(2026, 10, 25).equals(LocalDate.of(2026, 10, 26)) + " "
        + LocalDate.of(2026, 9, 6).isBefore(LocalDate.of(2026, 9, 7)) + " "
        + LocalDate.of(2026, 9, 5).plusDays(1).compareTo(LocalDate.of(2026, 9, 6));
  }

  @JSweetTest
  public static String parseToStringRoundTrip() {
    return d(LocalDate.parse("2026-10-25")) + " "
        + d(LocalDate.parse("2028-02-29")) + " "
        + t(LocalDateTime.parse("2026-10-25T02:30")) + " "
        + t(LocalDateTime.parse("2026-10-25T02:30:00")) + " "
        + t(LocalDateTime.parse("2026-10-25T02:30:05")) + " "
        + t(LocalDateTime.parse("2026-01-01T00:00:00.120")) + " "
        + t(LocalDateTime.parse("2026-01-01T00:00:00.5")) + " "
        + t(LocalDateTime.parse("2026-12-31T23:59:59.999")) + " "
        + t(LocalDateTime.parse(LocalDateTime.of(2026, 9, 6, 8, 0).toString())) + " "
        + LocalDate.parse(LocalDate.of(2026, 9, 6).toString()).equals(LocalDate.of(2026, 9, 6));
  }

  private static String parseDate(String s) {
    try {
      return LocalDate.parse(s).toString();
    } catch (DateTimeParseException e) {
      return "rejected";
    }
  }

  private static String parseDateTime(String s) {
    try {
      return LocalDateTime.parse(s).toString();
    } catch (DateTimeParseException e) {
      return "rejected";
    }
  }

  @JSweetTest
  public static String parseRejectsOutOfRangeDates() {
    return parseDate("2028-02-29") + " "
        + parseDate("2026-02-29") + " "
        + parseDate("2026-02-30") + " "
        + parseDate("2026-13-01") + " "
        + parseDate("2026-00-10") + " "
        + parseDate("2026-04-31") + " "
        + parseDate("2026-10-00") + " "
        + parseDate("26-10-25") + " "
        + parseDate("2026-10-25 ");
  }

  @JSweetTest
  public static String parseRejectsOutOfRangeDateTimes() {
    return parseDateTime("2026-10-25T23:59:59") + " "
        + parseDateTime("2026-10-25T24:00") + " "
        + parseDateTime("2026-10-25T23:60") + " "
        + parseDateTime("2026-10-25T23:59:60") + " "
        + parseDateTime("2026-02-30T08:00") + " "
        + parseDateTime("2026-10-25T08") + " "
        + parseDateTime("2026-10-25 08:00") + " "
        + parseDateTime("2026-10-25T08:00Z");
  }

  @JSweetTest
  public static String dayWithoutLocalMidnight() {
    LocalDate x = LocalDate.of(2026, 9, 6);
    return d(x.atStartOfDay().toLocalDate()) + " "
        + x.atStartOfDay().isEqual(LocalDateTime.of(2026, 9, 6, 0, 0)) + " "
        + x.atStartOfDay().isBefore(LocalDateTime.of(2026, 9, 6, 1, 30)) + " "
        + t(x.plusDays(1).atStartOfDay()) + " "
        + t(x.minusDays(1).atStartOfDay()) + " "
        + LocalDate.of(2026, 8, 31).plusDays(6).getDayOfWeek() + " "
        + d(LocalDate.of(2026, 9, 6).withDayOfMonth(7)) + " "
        + LocalDateTime.of(2026, 9, 6, 12, 0).toLocalDate().plusDays(0).equals(x);
  }
}
