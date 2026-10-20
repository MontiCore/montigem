package resources.exectests;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * LocalDate.parse with a time part, which java.time rejects and the translation accepts and
 * ignores (see JavaTimeAdapter). The expected values are therefore given in ExecutionTests
 * instead of being computed in Java.
 */
public class LocalDateParseTimePartRuntimeCheck {

  private static String parseDate(String s) {
    try {
      return LocalDate.parse(s).toString();
    } catch (DateTimeParseException e) {
      return "rejected";
    }
  }

  public static String parseIgnoresTimePart() {
    return parseDate("2024-05-07T00:00:00") + " "
        + parseDate("2024-05-07T23:30") + " "
        + parseDate("2026-10-25T02:30:00.500") + " "
        + parseDate("2026-09-06T00:00:00") + " "
        + LocalDate.parse("2024-05-07T00:00:00").equals(LocalDate.of(2024, 5, 7));
  }

  public static String parseRejectsInvalidTimePart() {
    return parseDate("2024-05-07T") + " "
        + parseDate("2024-05-07T24:00:00") + " "
        + parseDate("2024-05-07T00:60") + " "
        + parseDate("2024-05-07T00:00:00Z") + " "
        + parseDate("2026-02-30T00:00:00");
  }
}
