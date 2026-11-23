/* (c) https://github.com/MontiCore/monticore */
package resources.exectests;

import util.JSweetTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * java.time values as text in transpiled code: string building and toString in the format of java.time, also for
 * values of a static type like Object or a type parameter, and parse with the formats of java.time only
 */
public class JavaTimeStringRuntimeCheck {

  /** values of the static type Object or a type parameter: the format of java.time, other values unchanged */
  @JSweetTest
  public static String checkUntypedValues() {
    Object time = LocalTime.of(10, 15);
    Object number = 5;
    Object text = "x";
    Object none = null;
    Object date = LocalDate.of(2024, 2, 29);
    StringBuilder sb = new StringBuilder();
    sb.append(time).append(' ').append(number);
    String s = "s:";
    s += time;
    return "o=" + time + " " + time.toString() + " " + number + " " + text + " " + none + " " + date + " " + sb + " "
        + s + " " + String.valueOf(time) + " " + describe(Duration.ofMinutes(90)) + " " + describe(7);
  }

  private static <T> String describe(T value) {
    return "<" + value + "|" + value.toString() + ">";
  }

  /** parse takes the formats of java.time only */
  @JSweetTest
  public static String checkParse() {
    String result = LocalDate.parse("2024-02-29") + " " + LocalDateTime.parse("2024-02-29T12:34:56.123456789") + " "
        + LocalDateTime.parse("2024-02-29t12:34") + " " + LocalTime.parse("10:15") + " " + LocalTime.parse("10:15:30.5");
    String[] dates = {"2024-01-01T10:00", "20240101", "2024-1-01", "2024-02-30"};
    for (String text : dates) {
      try {
        result += " " + LocalDate.parse(text);
      } catch (Exception e) {
        result += " date rejected";
      }
    }
    String[] dateTimes = {"2024-01-01", "2024-01-01T10:00+01:00", "2024-01-01 10:00"};
    for (String text : dateTimes) {
      try {
        result += " " + LocalDateTime.parse(text);
      } catch (Exception e) {
        result += " date-time rejected";
      }
    }
    String[] times = {"T10:00", "10", "1:00", "10:00:00.1234567891"};
    for (String text : times) {
      try {
        result += " " + LocalTime.parse(text);
      } catch (Exception e) {
        result += " time rejected";
      }
    }
    return result;
  }

  /** string building with java.time values: the format of java.time, not the one of Temporal */
  @JSweetTest
  public static String checkStringBuilding() {
    LocalTime time = LocalTime.of(10, 15);
    LocalDate date = LocalDate.of(2024, 2, 29);
    String s = "at ";
    s += time;
    String[] parts = {"from "};
    parts[0] += time;
    StringBuilder sb = new StringBuilder("on ");
    sb.append(date).append(' ').append(LocalDateTime.of(date, time));
    return s + " " + parts[0] + " " + sb + " " + String.valueOf(time) + " " + String.valueOf((LocalTime) null);
  }

  /** null values in string building print "null", like Java */
  @JSweetTest
  public static String checkNullInStringBuilding() {
    LocalDate date = null;
    Duration duration = null;
    ChronoUnit unit = null;
    String s = "s:";
    s += date;
    StringBuilder sb = new StringBuilder("sb:");
    sb.append(duration).append(unit);
    return "a:" + date + " " + duration + " " + unit + " " + s + " " + sb + " " + String.valueOf(duration);
  }
}
