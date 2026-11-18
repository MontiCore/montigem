/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * ZonedDateTime, ZoneId and ZoneOffset in transpiled code (Temporal.ZonedDateTime, zone ids as strings, see
 * ZonedDateTimeHelper): conversions, DST gaps and overlaps, arithmetic and toString like java.time
 */
public class ZonedDateTimeRuntimeCheck {

  static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @JSweetTest
  public static String checkZonesAndOffsets() {
    return BERLIN + " " + ZoneId.of("UTC") + " " + ZoneId.of("+02:00") + " " + ZoneId.of("Z") + " "
        + ZoneOffset.UTC + " " + ZoneOffset.ofHours(2) + " " + ZoneOffset.ofHoursMinutes(-5, -30) + " "
        + ZoneOffset.of("+0530").getTotalSeconds() + " " + ZoneOffset.ofTotalSeconds(0) + " "
        + ZoneOffset.ofHours(-2).getId() + " " + BERLIN.equals(ZoneId.of("Europe/Berlin"));
  }

  @JSweetTest
  public static String checkAtZoneAndToString() {
    LocalDateTime nine = LocalDateTime.of(2026, 11, 17, 9, 0);
    return nine.atZone(BERLIN) + " " + nine.atZone(BERLIN).toInstant() + " " + nine.atZone(ZoneOffset.UTC) + " "
        + nine.atZone(ZoneId.of("UTC")) + " " + nine.atZone(ZoneOffset.ofHours(2)) + " "
        + LocalDateTime.of(2026, 7, 1, 9, 0, 30, 500_000_000).atZone(BERLIN);
  }

  @JSweetTest
  public static String checkGapAndOverlap() {
    // Berlin: 2026-03-29 02:00 -> 03:00, 2026-10-25 03:00 -> 02:00
    return LocalDateTime.of(2026, 3, 29, 2, 30).atZone(BERLIN) + " "
        + LocalDateTime.of(2026, 10, 25, 2, 30).atZone(BERLIN) + " "
        + LocalDate.of(2026, 3, 29).atStartOfDay(ZoneId.of("America/Sao_Paulo")) + " "
        + ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, BERLIN).getOffset();
  }

  @JSweetTest
  public static String checkInstantConversions() {
    Instant instant = Instant.parse("2026-11-17T08:00:00Z");
    return instant.atZone(BERLIN) + " " + instant.atZone(BERLIN).toLocalDateTime() + " "
        + LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York")) + " "
        + LocalDate.ofInstant(instant, ZoneId.of("Pacific/Kiritimati")) + " " + LocalTime.ofInstant(instant, BERLIN)
        + " " + ZonedDateTime.ofInstant(instant, ZoneOffset.ofHours(-3)) + " "
        + instant.atZone(BERLIN).withZoneSameInstant(ZoneId.of("Asia/Tokyo")) + " "
        + instant.atZone(BERLIN).withZoneSameLocal(ZoneId.of("Asia/Tokyo"));
  }

  @JSweetTest
  public static String checkFactoriesAndParse() {
    return ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(9, 0), BERLIN) + " "
        + ZonedDateTime.of(LocalDateTime.of(2026, 11, 17, 9, 0), BERLIN) + " "
        + ZonedDateTime.parse("2026-11-17T09:00+01:00[Europe/Berlin]") + " "
        + ZonedDateTime.parse("2026-11-17T09:00:15+01:00") + " " + ZonedDateTime.parse("2026-11-17T08:00Z") + " "
        + ZonedDateTime.parse("2026-11-17T09:00+01:00[Europe/Berlin]").getZone();
  }

  @JSweetTest
  public static String checkGetters() {
    ZonedDateTime dateTime = ZonedDateTime.of(2026, 11, 17, 9, 30, 15, 123_000_000, BERLIN);
    return dateTime.getYear() + " " + dateTime.getMonthValue() + " " + dateTime.getDayOfMonth() + " "
        + dateTime.getHour() + " " + dateTime.getMinute() + " " + dateTime.getSecond() + " " + dateTime.getNano() + " "
        + dateTime.getDayOfWeek() + " " + dateTime.getMonth() + " " + dateTime.getDayOfYear() + " "
        + dateTime.getZone() + " " + dateTime.getOffset() + " " + dateTime.toEpochSecond() + " "
        + dateTime.toLocalDate() + " " + dateTime.toLocalTime();
  }

  @JSweetTest
  public static String checkArithmeticAcrossDst() {
    ZonedDateTime before = ZonedDateTime.of(2026, 3, 28, 9, 0, 0, 0, BERLIN);
    return before.plusDays(1) + " " + before.plusHours(24) + " " + before.plus(Duration.ofHours(24)) + " "
        + before.plus(1, ChronoUnit.DAYS) + " " + before.plusWeeks(1).minusMonths(1) + " "
        + before.withHour(2).withMinute(30) + " " + before.plusDays(1).withHour(2) + " "
        + ChronoUnit.DAYS.between(before, before.plusHours(23)) + " "
        + ChronoUnit.HOURS.between(before, before.plusDays(1)) + " "
        + Duration.between(before, before.plusDays(1)) + " "
        + ChronoUnit.DAYS.between(before, before.plusDays(1).withZoneSameInstant(ZoneId.of("Asia/Tokyo")));
  }

  @JSweetTest
  public static boolean checkComparison() {
    ZonedDateTime berlin = ZonedDateTime.of(2026, 11, 17, 9, 0, 0, 0, BERLIN);
    ZonedDateTime utc = berlin.withZoneSameInstant(ZoneOffset.UTC);
    return berlin.isEqual(utc) && !berlin.equals(utc) && berlin.compareTo(utc) != 0
        && berlin.isBefore(berlin.plusNanos(1)) && berlin.plusNanos(1).isAfter(berlin)
        && berlin.equals(ZonedDateTime.parse("2026-11-17T09:00+01:00[Europe/Berlin]"))
        && Objects.equals(berlin, utc.withZoneSameInstant(BERLIN)) && !Objects.equals(berlin, utc);
  }

  @JSweetTest
  public static String checkEpochWithOffsets() {
    LocalDateTime nine = LocalDateTime.of(2026, 11, 17, 9, 0);
    return nine.toEpochSecond(ZoneOffset.ofHours(2)) + " " + nine.toEpochSecond(ZoneOffset.UTC) + " "
        + LocalDateTime.ofEpochSecond(1_794_907_815L, 5, ZoneOffset.ofHours(-5)) + " "
        + LocalDate.of(2026, 11, 17).toEpochSecond(LocalTime.of(9, 0), ZoneOffset.ofHours(1)) + " "
        + LocalTime.of(9, 0).toEpochSecond(LocalDate.of(2026, 11, 17), ZoneOffset.ofHours(1));
  }
}
