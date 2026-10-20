package resources.exectests;

import util.JSweetTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * java.time on days where the browser's time zone matters. Run in several time zones (see
 * ExecutionTests): the results must equal Java's in each of them.
 *
 * <p>DST changes in 2026: Europe/Berlin 2026-03-29 02:00 -> 03:00 and 2026-10-25 03:00 -> 02:00;
 * America/New_York 2026-03-08 and 2026-11-01; America/Santiago 2026-04-05 (24:00 -> 23:00 on
 * 2026-04-04) and 2026-09-06 (00:00 -> 01:00, so that day has no local midnight).
 *
 * <p>No LocalDateTime here lies in an hour that the DST start skips in one of the tested zones:
 * such a value has no local JS Date (see JavaTimeAdapter).
 */
public class LocalDateTimeZoneRuntimeCheck {

  @JSweetTest
  public static String dayOfWeekOfSundaysAroundDstChanges() {
    return LocalDate.of(2026, 3, 22).getDayOfWeek() + " "
        + LocalDate.of(2026, 3, 29).getDayOfWeek() + " "
        + LocalDate.of(2026, 4, 5).getDayOfWeek() + " "
        + LocalDate.of(2026, 10, 18).getDayOfWeek() + " "
        + LocalDate.of(2026, 10, 25).getDayOfWeek() + " "
        + LocalDate.of(2026, 11, 1).getDayOfWeek() + " "
        + LocalDate.of(2026, 3, 28).plusDays(1).getDayOfWeek() + " "
        + LocalDate.of(2026, 10, 24).plusDays(1).getDayOfWeek() + " "
        + LocalDate.of(2026, 10, 18).getDayOfWeek().getValue() + " "
        + LocalDate.of(2026, 10, 18).getDayOfMonth() + " "
        + LocalDate.of(2026, 10, 25).getDayOfYear() + " "
        + LocalDateTime.of(2026, 10, 25, 23, 59).getDayOfWeek() + " "
        + LocalDateTime.of(2026, 3, 29, 0, 0).getDayOfWeek();
  }

  @JSweetTest
  public static String yearBoundary() {
    LocalDate silvester = LocalDate.of(2026, 12, 31);
    LocalDate neujahr = silvester.plusDays(1);
    return neujahr.getYear() + " "
        + neujahr.getMonthValue() + " "
        + neujahr.getMonth() + " "
        + neujahr.getDayOfMonth() + " "
        + neujahr.getDayOfYear() + " "
        + neujahr.getDayOfWeek() + " "
        + neujahr.equals(LocalDate.of(2027, 1, 1)) + " "
        + neujahr.minusDays(1).getYear() + " "
        + silvester.getDayOfYear() + " "
        + neujahr.toString() + " "
        + LocalDate.of(2027, 1, 1).getYear() + " "
        + LocalDateTime.of(2026, 12, 31, 23, 30).plusMinutes(45).toString() + " "
        + LocalDateTime.of(2027, 1, 1, 0, 30).getYear() + " "
        + LocalDateTime.of(2027, 1, 1, 0, 30).minusHours(1).toString();
  }

  @JSweetTest
  public static String plusDaysAcrossDstChanges() {
    return LocalDate.of(2026, 3, 28).plusDays(1).equals(LocalDate.of(2026, 3, 29)) + " "
        + LocalDate.of(2026, 3, 29).plusDays(1).equals(LocalDate.of(2026, 3, 30)) + " "
        + LocalDate.of(2026, 3, 25).plusWeeks(1).equals(LocalDate.of(2026, 4, 1)) + " "
        + LocalDate.of(2026, 3, 30).minusDays(2).equals(LocalDate.of(2026, 3, 28)) + " "
        + LocalDate.of(2026, 10, 25).plusDays(1).equals(LocalDate.of(2026, 10, 26)) + " "
        + LocalDate.of(2026, 10, 24).plusDays(2).equals(LocalDate.of(2026, 10, 26)) + " "
        + LocalDate.of(2026, 10, 26).minusDays(1).equals(LocalDate.of(2026, 10, 25)) + " "
        + LocalDate.of(2026, 10, 19).minusWeeks(1).equals(LocalDate.of(2026, 10, 12)) + " "
        + LocalDate.of(2026, 3, 1).plusDays(30).toString() + " "
        + LocalDate.of(2026, 10, 1).plusDays(30).toString() + " "
        + LocalDate.of(2026, 1, 31).plusMonths(1).toString() + " "
        + LocalDate.of(2026, 10, 31).minusMonths(7).toString() + " "
        + LocalDate.of(2026, 10, 18).plusDays(14 - 1).toString();
  }

  @JSweetTest
  public static String localDateOnDayWithoutLocalMidnight() {
    LocalDate d = LocalDate.of(2026, 9, 6);
    return d.getDayOfWeek() + " "
        + d.toString() + " "
        + d.getDayOfMonth() + " "
        + d.plusDays(1).equals(LocalDate.of(2026, 9, 7)) + " "
        + LocalDate.of(2026, 9, 5).plusDays(1).equals(d) + " "
        + LocalDate.of(2026, 9, 5).plusDays(1).isEqual(d) + " "
        + d.minusDays(1).toString() + " "
        + LocalDate.of(2026, 9, 5).isBefore(d) + " "
        + d.atStartOfDay().toLocalDate().equals(d) + " "
        + LocalDate.of(2026, 8, 6).plusMonths(1).equals(d);
  }

  @JSweetTest
  public static String localDateTimeArithmeticAcrossDstEnd() {
    LocalDateTime t = LocalDateTime.of(2026, 10, 25, 1, 30);
    return t.plusHours(1).toString() + " "
        + t.plusHours(2).toString() + " "
        + t.plusMinutes(90).toString() + " "
        + t.plusMinutes(30).toString() + " "
        + t.plusHours(1).getHour() + " "
        + t.plusHours(1).equals(LocalDateTime.of(2026, 10, 25, 2, 30)) + " "
        + t.plusHours(1).plusHours(1).equals(LocalDateTime.of(2026, 10, 25, 3, 30)) + " "
        + LocalDateTime.of(2026, 10, 24, 22, 0).plusHours(24).toString() + " "
        + LocalDateTime.of(2026, 10, 25, 0, 0).plusDays(1).toString() + " "
        + LocalDateTime.of(2026, 10, 25, 3, 0).minusHours(2).toString() + " "
        + LocalDateTime.of(2026, 10, 25, 4, 0).minusMinutes(150).toString() + " "
        + LocalDateTime.of(2026, 11, 25, 2, 30).minusMonths(1).toString() + " "
        + LocalDateTime.of(2026, 10, 25, 2, 59, 59).plusSeconds(1).toString();
  }

  @JSweetTest
  public static String localDateTimeArithmeticAcrossDstStart() {
    LocalDateTime t = LocalDateTime.of(2026, 3, 29, 1, 30);
    return t.plusHours(2).toString() + " "
        + t.plusMinutes(150).toString() + " "
        + t.plusHours(2).getHour() + " "
        + t.plusHours(2).equals(LocalDateTime.of(2026, 3, 29, 3, 30)) + " "
        + LocalDateTime.of(2026, 3, 28, 22, 0).plusHours(24).toString() + " "
        + LocalDateTime.of(2026, 3, 29, 4, 0).minusHours(3).toString() + " "
        + LocalDateTime.of(2026, 3, 28, 12, 0).plusDays(1).toString() + " "
        + LocalDateTime.of(2026, 3, 29, 0, 0).plusHours(24).equals(LocalDateTime.of(2026, 3, 30, 0, 0)) + " "
        + LocalDateTime.of(2026, 3, 29, 0, 0).plusDays(1).equals(LocalDateTime.of(2026, 3, 30, 0, 0)) + " "
        + LocalDateTime.of(2026, 3, 30, 1, 0).minusDays(1).toString() + " "
        + LocalDateTime.of(2026, 3, 29, 3, 15).minusMinutes(90).toString();
  }

  @JSweetTest
  public static String comparisons() {
    LocalDateTime a = LocalDateTime.of(2026, 10, 25, 1, 59);
    LocalDateTime b = a.plusMinutes(2);
    LocalDateTime c = LocalDateTime.of(2026, 10, 25, 3, 0);
    LocalDate x = LocalDate.of(2026, 12, 31);
    LocalDate y = LocalDate.of(2027, 1, 1);
    return a.isBefore(b) + " "
        + b.isBefore(c) + " "
        + c.isAfter(a) + " "
        + b.isAfter(c) + " "
        + b.isEqual(LocalDateTime.of(2026, 10, 25, 2, 1)) + " "
        + b.equals(LocalDateTime.of(2026, 10, 25, 2, 1)) + " "
        + c.compareTo(a) + " "
        + a.compareTo(a.plusDays(1)) + " "
        + x.isBefore(y) + " "
        + y.isAfter(x) + " "
        + x.isAfter(y) + " "
        + x.plusDays(1).isEqual(y) + " "
        + x.equals(y) + " "
        + y.compareTo(x) + " "
        + LocalDate.of(2026, 3, 1).compareTo(LocalDate.of(2026, 1, 31)) + " "
        + LocalDate.of(2026, 3, 29).isAfter(LocalDate.of(2026, 3, 28)) + " "
        + LocalDate.of(2026, 10, 25).isBefore(LocalDate.of(2026, 10, 26));
  }

  @JSweetTest
  public static String toLocalDateAndAtStartOfDay() {
    LocalDateTime late = LocalDateTime.of(2026, 10, 25, 23, 30);
    LocalDateTime early = LocalDateTime.of(2027, 1, 1, 0, 15);
    return late.toLocalDate().toString() + " "
        + late.toLocalDate().equals(LocalDate.of(2026, 10, 25)) + " "
        + early.toLocalDate().getYear() + " "
        + early.toLocalDate().getDayOfWeek() + " "
        + LocalDate.of(2026, 3, 29).atStartOfDay().toString() + " "
        + LocalDate.of(2026, 10, 25).atStartOfDay().toString() + " "
        + LocalDate.of(2026, 10, 25).atStartOfDay().plusHours(25).toString() + " "
        + LocalDate.of(2026, 10, 25).atStartOfDay().equals(LocalDateTime.of(2026, 10, 25, 0, 0)) + " "
        + late.toLocalDate().atStartOfDay().isBefore(late) + " "
        + LocalDate.of(2026, 12, 31).atStartOfDay().plusDays(1).toLocalDate().getYear();
  }

  @JSweetTest
  public static String withMethods() {
    LocalDate d = LocalDate.of(2024, 3, 31);
    LocalDateTime t = LocalDateTime.of(2026, 10, 25, 1, 30);
    return d.withMonth(2).toString() + " "
        + d.withYear(2025).toString() + " "
        + d.withDayOfMonth(1).toString() + " "
        + d.withDayOfYear(60).toString() + " "
        + LocalDate.of(2024, 2, 29).withYear(2025).toString() + " "
        + t.withHour(3).toString() + " "
        + t.withMinute(5).toString() + " "
        + t.withSecond(7).toString() + " "
        + t.withDayOfMonth(18).getDayOfWeek();
  }

  @JSweetTest
  public static String parseAndToString() {
    return LocalDate.parse("2026-10-25").getDayOfWeek() + " "
        + LocalDate.parse("2027-01-01").getYear() + " "
        + LocalDate.parse("2026-03-29").plusDays(1).toString() + " "
        + LocalDateTime.parse("2026-10-25T02:30").plusHours(1).toString() + " "
        + LocalDateTime.parse("2026-10-25T08:15:30").getHour() + " "
        + LocalDateTime.parse("2026-12-31T23:59:59.5").plusSeconds(1).toString() + " "
        + LocalDateTime.parse("2026-12-31T23:59:59.5").getNano();
  }

  @JSweetTest
  public static String parseRejectsInvalidText() {
    try {
      LocalDate.parse("25.10.2026");
      return "accepted";
    } catch (DateTimeParseException e) {
      return "rejected";
    }
  }

  @JSweetTest
  public static String objectsEquals() {
    LocalDate d = LocalDate.of(2026, 10, 25);
    LocalDateTime t = LocalDateTime.of(2026, 10, 25, 1, 30);
    LocalDate none = null;
    return Objects.equals(d.plusDays(1), LocalDate.of(2026, 10, 26)) + " "
        + Objects.equals(LocalDate.of(2026, 3, 28).plusDays(1), LocalDate.of(2026, 3, 29)) + " "
        + Objects.equals(t.plusHours(1), LocalDateTime.of(2026, 10, 25, 2, 30)) + " "
        + Objects.equals(t, t.plusMinutes(1)) + " "
        + Objects.equals(d, none) + " "
        + Objects.equals(none, d) + " "
        + Objects.equals(none, none);
  }

  @JSweetTest
  public static String epochSeconds() {
    return LocalDateTime.of(2026, 10, 25, 2, 30).toEpochSecond(ZoneOffset.UTC) + " "
        + LocalDateTime.of(2026, 3, 29, 1, 0).toEpochSecond(ZoneOffset.UTC) + " "
        + LocalDateTime.ofEpochSecond(1792895400L, 0, ZoneOffset.UTC).toString();
  }
}
