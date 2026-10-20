package resources.exectests;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Handwritten frontend code working on values that come from the server: the test deserializes
 * wire strings with the runtime's LocalDateDeSer/LocalDateTimeDeSer, passes them in, and
 * serializes the results again (see exectests.WireFormatRoundTripTest).
 */
public class LocalDateWireRoundTrip {

  public static String describeDate(LocalDate d) {
    return d.toString() + " " + d.getDayOfWeek() + " " + d.getYear() + " " + d.getMonthValue()
        + " " + d.getDayOfMonth() + " " + d.getDayOfYear() + " "
        + d.plusDays(1).equals(d.plusDays(2).minusDays(1)) + " "
        + d.plusDays(7).isAfter(d) + " "
        + d.atStartOfDay().toLocalDate().equals(d) + " "
        + Objects.equals(d.plusDays(1).minusDays(1), d);
  }

  public static LocalDate nextDay(LocalDate d) {
    return d.plusDays(1);
  }

  public static String describeDateTime(LocalDateTime t) {
    return t.toString() + " " + t.getDayOfWeek() + " " + t.getYear() + " " + t.getHour() + " "
        + t.getMinute() + " " + t.toLocalDate().toString() + " "
        + t.toLocalDate().atStartOfDay().isAfter(t);
  }

  public static LocalDateTime plusHours(LocalDateTime t, int hours) {
    return t.plusHours(hours);
  }
}
