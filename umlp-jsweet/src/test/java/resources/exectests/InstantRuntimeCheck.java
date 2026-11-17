/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.Instant;
import java.util.Objects;

/**
 * Instant in transpiled code (Temporal.Instant): epoch values, nanoseconds, toString like java.time
 */
public class InstantRuntimeCheck {
  @JSweetTest
  public static String checkToString() {
    return Instant.ofEpochSecond(0) + " " + Instant.ofEpochMilli(1_763_371_815_120L) + " "
        + Instant.ofEpochSecond(1_763_371_815L, 123_400) + " " + Instant.ofEpochSecond(1_763_371_815L, 5);
  }

  @JSweetTest
  public static String checkParseAndEpoch() {
    Instant instant = Instant.parse("2026-11-17T09:30:15.123456789Z");
    return instant.getEpochSecond() + " " + instant.getNano() + " " + instant.toEpochMilli() + " "
        + Instant.parse("1969-12-31T23:59:59.5Z").getEpochSecond() + " "
        + Instant.parse("1969-12-31T23:59:59.5Z").getNano();
  }

  @JSweetTest
  public static String checkArithmetic() {
    Instant instant = Instant.parse("2026-11-17T23:59:59Z");
    return instant.plusSeconds(1) + " " + instant.minusSeconds(60) + " " + instant.plusMillis(1_500) + " "
        + instant.minusMillis(1) + " " + instant.plusNanos(1_000) + " " + instant.minusNanos(1);
  }

  @JSweetTest
  public static boolean checkComparison() {
    Instant earlier = Instant.ofEpochSecond(100);
    Instant later = Instant.ofEpochSecond(100, 1);
    return earlier.isBefore(later) && later.isAfter(earlier) && earlier.compareTo(later) < 0
        && later.compareTo(earlier) > 0 && earlier.equals(Instant.ofEpochMilli(100_000)) && !earlier.equals(later);
  }

  @JSweetTest
  public static boolean checkNow() {
    Instant before = Instant.ofEpochSecond(1_763_371_815L);
    return Instant.now().isAfter(before);
  }

  @JSweetTest
  public static boolean checkObjectsEquals() {
    Instant instant = Instant.ofEpochSecond(100);
    Instant none = null;
    return Objects.equals(instant, Instant.ofEpochMilli(100_000)) && !Objects.equals(instant, none)
        && !Objects.equals(none, instant) && Objects.equals(none, none);
  }
}
