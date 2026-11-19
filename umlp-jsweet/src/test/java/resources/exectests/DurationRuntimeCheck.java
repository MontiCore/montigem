/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Duration and ChronoUnit in transpiled code (Temporal.Duration, see DurationHelper): exact in nanoseconds, toString
 * and parse like java.time
 */
public class DurationRuntimeCheck {
  @JSweetTest
  public static String checkFactoriesAndToString() {
    return Duration.ofMinutes(90) + " " + Duration.ofHours(25) + " " + Duration.ofDays(2) + " "
        + Duration.ofSeconds(3_661, 5) + " " + Duration.ofMillis(1_500) + " " + Duration.ofNanos(123_000) + " "
        + Duration.of(3, ChronoUnit.HALF_DAYS) + " " + Duration.ZERO;
  }

  @JSweetTest
  public static String checkNegativeToString() {
    return Duration.ofMillis(-500) + " " + Duration.ofSeconds(-90) + " " + Duration.ofMillis(-61_500) + " "
        + Duration.ofHours(-1).plusNanos(1);
  }

  @JSweetTest
  public static String checkParse() {
    return Duration.parse("PT1H30M") + " " + Duration.parse("P2DT3H") + " " + Duration.parse("-PT0.5S") + " "
        + Duration.parse("PT-1M-30S") + " " + Duration.parse("PT0.000000001S") + " " + Duration.parse("pt-0.25s");
  }

  @JSweetTest
  public static String checkGettersAndConversions() {
    Duration d = Duration.ofMillis(-1_500);
    Duration e = Duration.parse("P1DT2H3M4.005006007S");
    return d.getSeconds() + " " + d.getNano() + " " + d.toMillis() + " " + e.toDays() + " " + e.toHours() + " "
        + e.toMinutes() + " " + e.toSeconds() + " " + e.toMillis() + " " + Duration.ofMinutes(-1).toNanos();
  }

  @JSweetTest
  public static String checkArithmetic() {
    Duration d = Duration.ofMinutes(45);
    return d.plus(Duration.ofMinutes(30)) + " " + d.minus(Duration.ofHours(1)) + " " + d.plusSeconds(15) + " "
        + d.minusMillis(1) + " " + d.multipliedBy(3) + " " + d.dividedBy(7) + " " + d.dividedBy(Duration.ofMinutes(10))
        + " " + d.negated() + " " + d.negated().abs() + " " + d.plus(2, ChronoUnit.HOURS) + " "
        + d.minus(1, ChronoUnit.DAYS);
  }

  @JSweetTest
  public static boolean checkComparison() {
    Duration shorter = Duration.ofSeconds(59, 999_999_999);
    Duration longer = Duration.ofMinutes(1);
    return shorter.compareTo(longer) < 0 && longer.compareTo(shorter) > 0 && longer.equals(Duration.ofSeconds(60))
        && !longer.equals(shorter) && Duration.ZERO.isZero() && shorter.negated().isNegative()
        && !longer.isNegative() && Objects.equals(longer, Duration.ofMillis(60_000))
        && !Objects.equals(longer, shorter);
  }

  @JSweetTest
  public static String checkBetween() {
    LocalDateTime start = LocalDateTime.of(2026, 11, 17, 9, 30);
    LocalDateTime end = LocalDateTime.of(2026, 11, 18, 11, 45, 30, 500_000_000);
    return Duration.between(start, end) + " " + Duration.between(end, start) + " "
        + Duration.between(LocalTime.of(8, 0), LocalTime.of(17, 30)) + " "
        + Duration.between(Instant.ofEpochSecond(0), Instant.ofEpochSecond(90, 1));
  }

  @JSweetTest
  public static String checkPlusDuration() {
    Duration d = Duration.ofMinutes(90);
    return LocalDateTime.of(2026, 11, 17, 23, 0).plus(d) + " " + LocalTime.of(23, 0).plus(d) + " "
        + LocalTime.of(0, 30).minus(d) + " " + Instant.parse("2026-11-17T23:00:00Z").plus(d) + " "
        + Instant.parse("2026-11-17T23:00:00Z").minus(Duration.ofDays(1));
  }

  @JSweetTest
  public static String checkChronoUnits() {
    LocalDate date = LocalDate.of(2026, 1, 31);
    LocalDateTime dateTime = LocalDateTime.of(2026, 11, 17, 9, 30);
    Instant instant = Instant.parse("2026-11-17T09:30:00Z");
    return date.plus(1, ChronoUnit.MONTHS) + " " + date.plus(2, ChronoUnit.WEEKS) + " "
        + date.minus(1, ChronoUnit.DECADES) + " " + dateTime.plus(36, ChronoUnit.HOURS) + " "
        + dateTime.plus(1, ChronoUnit.HALF_DAYS) + " " + instant.plus(2, ChronoUnit.DAYS) + " "
        + instant.minus(500, ChronoUnit.MILLIS) + " " + LocalTime.of(9, 0).plus(90, ChronoUnit.MINUTES);
  }

  @JSweetTest
  public static String checkUnitsBetween() {
    LocalDate from = LocalDate.of(2026, 1, 31);
    LocalDate to = LocalDate.of(2027, 3, 1);
    LocalDateTime start = LocalDateTime.of(2026, 11, 17, 9, 30);
    LocalDateTime end = LocalDateTime.of(2026, 11, 19, 9, 29, 59);
    Instant a = Instant.parse("2026-11-17T09:30:00Z");
    Instant b = Instant.parse("2026-11-17T08:00:00.5Z");
    return ChronoUnit.DAYS.between(from, to) + " " + ChronoUnit.MONTHS.between(from, to) + " "
        + ChronoUnit.YEARS.between(from, to) + " " + ChronoUnit.WEEKS.between(from, to) + " "
        + ChronoUnit.DAYS.between(start, end) + " " + ChronoUnit.HOURS.between(start, end) + " "
        + ChronoUnit.MINUTES.between(a, b) + " " + ChronoUnit.MILLIS.between(a, b) + " "
        + ChronoUnit.SECONDS.between(LocalTime.of(10, 0), LocalTime.of(9, 59, 30));
  }

  /** toDaysPart() to toNanosPart(), isPositive(), truncatedTo(unit) */
  @JSweetTest
  public static String checkPartsAndTruncation() {
    Duration d = Duration.parse("P2DT3H4M5.006007008S");
    Duration negative = Duration.ofMillis(-1_500);
    return d.toDaysPart() + " " + d.toHoursPart() + " " + d.toMinutesPart() + " " + d.toSecondsPart() + " "
        + d.toMillisPart() + " " + d.toNanosPart() + " " + negative.toSecondsPart() + " " + negative.toMillisPart()
        + " " + d.isPositive() + " " + negative.isPositive() + " " + Duration.ZERO.isPositive() + " "
        + d.truncatedTo(ChronoUnit.SECONDS) + " " + d.truncatedTo(ChronoUnit.HOURS) + " "
        + d.truncatedTo(ChronoUnit.DAYS) + " " + negative.truncatedTo(ChronoUnit.SECONDS) + " "
        + Duration.ofMinutes(-90).truncatedTo(ChronoUnit.HOURS);
  }

  /** the ChronoUnit methods: getDuration, valueOf, name, toString (the display name), string concatenation */
  @JSweetTest
  public static String checkChronoUnitMethods() {
    ChronoUnit unit = ChronoUnit.valueOf("HALF_DAYS");
    StringBuilder sb = new StringBuilder();
    sb.append(ChronoUnit.MILLENNIA);
    return unit.name() + " " + unit + " " + unit.toString() + " " + ChronoUnit.DAYS.getDuration() + " "
        + ChronoUnit.MONTHS.getDuration() + " " + ChronoUnit.NANOS.getDuration() + " " + sb + " "
        + String.valueOf(ChronoUnit.WEEKS);
  }

  /** plusDays to minusNanos */
  @JSweetTest
  public static String checkShorthandArithmetic() {
    Duration d = Duration.ofHours(1);
    return d.plusDays(1) + " " + d.plusHours(2) + " " + d.plusMinutes(30) + " " + d.plusMillis(250) + " "
        + d.minusDays(1) + " " + d.minusHours(2) + " " + d.minusMinutes(61) + " " + d.minusSeconds(1) + " "
        + d.minusNanos(1);
  }

  /** invalid text, unknown units and too large truncation units are rejected, like Java */
  @JSweetTest
  public static String checkInvalidRejected() {
    String result = "";
    try {
      result += Duration.parse("1H30M");
    } catch (Exception e) {
      result += "parse rejected";
    }
    try {
      result += " " + ChronoUnit.valueOf("FORTNIGHTS");
    } catch (Exception e) {
      result += " unit rejected";
    }
    try {
      result += " " + Duration.ofHours(30).truncatedTo(ChronoUnit.WEEKS);
    } catch (Exception e) {
      result += " truncation rejected";
    }
    try {
      result += " " + Duration.ofHours(1).dividedBy(0);
    } catch (Exception e) {
      result += " division rejected";
    }
    return result;
  }
}
