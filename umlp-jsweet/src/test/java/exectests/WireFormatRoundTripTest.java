package exectests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import resources.exectests.LocalDateWireRoundTrip;
import util.RealTscCheck;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deserialize -> transpiled java.time code -> serialize, in several browser time zones.
 *
 * <p>The values are deserialized with the runtime's LocalDateDeSer/LocalDateTimeDeSer, as the
 * frontend receives them from the server, passed to the transpiled {@link LocalDateWireRoundTrip}
 * and the results are serialized with the runtime's serializers. Expected: what the same Java code
 * computes on the parsed values, and the server's wire format for the results.
 */
public class WireFormatRoundTripTest {

  /**
   * Zones with DST changes at 02:00/03:00 (Europe/Berlin, America/New_York), at midnight
   * (America/Santiago) and by 30 minutes (Australia/Lord_Howe), the largest offset east of UTC
   * (Pacific/Kiritimati, UTC+14) and UTC.
   */
  static final List<String> TIME_ZONES = List.of("Europe/Berlin", "America/New_York",
      "America/Santiago", "Australia/Lord_Howe", "Pacific/Kiritimati", "UTC");

  private static final List<String> DATES = List.of(
      "2026-03-28", "2026-03-29", "2026-10-18", "2026-10-24", "2026-10-25", "2026-07-01",
      "2026-12-31", "2027-01-01", "2024-02-29", "2026-09-05", "2026-09-06");

  /** Date-time and hours to add. None of the values or results is in an hour skipped by DST. */
  private static final List<Object[]> DATE_TIMES = List.of(
      new Object[] {"2026-10-18T08:00:00", 0},
      new Object[] {"2026-10-25T00:00:00", 24},
      new Object[] {"2026-10-25T01:30:00", 1},
      new Object[] {"2026-10-24T23:30:00", 3},
      new Object[] {"2026-03-29T01:30:00", 2},
      new Object[] {"2026-03-28T08:00:00", 24},
      new Object[] {"2026-12-31T23:00:00", 1},
      new Object[] {"2027-01-01T00:30:00", -1},
      new Object[] {"2026-09-06T08:00:00", -6});

  private static final DateTimeFormatter WIRE_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  @Test
  public void testRoundTripInTimeZones() throws IOException {
    Map<String, String> expected = new LinkedHashMap<>();
    for (String date : DATES) {
      LocalDate d = LocalDate.parse(date);
      expected.put("date:" + date, LocalDateWireRoundTrip.describeDate(d)
          + " | " + LocalDateWireRoundTrip.nextDay(d));
      // The same date with a time of day, as GemDateInput emits it for a date-only field.
      expected.put("dateFromInput:" + date, LocalDateWireRoundTrip.describeDate(d)
          + " | " + LocalDateWireRoundTrip.nextDay(d));
    }
    for (Object[] dateTime : DATE_TIMES) {
      LocalDateTime t = LocalDateTime.parse((String) dateTime[0]);
      expected.put("dateTime:" + dateTime[0] + "+" + dateTime[1],
          LocalDateWireRoundTrip.describeDateTime(t) + " | "
              + WIRE_DATE_TIME.format(LocalDateWireRoundTrip.plusHours(t, (Integer) dateTime[1])));
    }

    Map<String, RealTscCheck.Result> results = RealTscCheck.compileAndRunInTimeZones(
        LocalDateWireRoundTrip.class, runner(), TIME_ZONES);

    assertSameAsJava(expected, results, "Round trip");
  }

  /**
   * LocalDateTimeHelper, for handwritten TypeScript, on deserialized values: same results as the
   * corresponding java.time methods.
   */
  @Test
  public void testLocalDateTimeHelperInTimeZones() throws IOException {
    Map<String, String> expected = new LinkedHashMap<>();
    Map<String, String> calls = new LinkedHashMap<>();
    addDateCase(expected, calls, "getDayOfWeek(2026-10-18)",
        "H.getDayOfWeek(D('2026-10-18'))", LocalDate.parse("2026-10-18").getDayOfWeek().name());
    addDateCase(expected, calls, "getDayOfWeek(2026-03-29)",
        "H.getDayOfWeek(D('2026-03-29'))", LocalDate.parse("2026-03-29").getDayOfWeek().name());
    addDateCase(expected, calls, "plusDays(2026-10-25,1)",
        "SD(H.plusDays(D('2026-10-25'), 1))", LocalDate.parse("2026-10-25").plusDays(1).toString());
    addDateCase(expected, calls, "minusDays(2026-03-30,1)",
        "SD(H.minusDays(D('2026-03-30'), 1))", LocalDate.parse("2026-03-30").minusDays(1).toString());
    addDateCase(expected, calls, "plusWeeks(2026-03-25,1)",
        "SD(H.plusWeeks(D('2026-03-25'), 1))", LocalDate.parse("2026-03-25").plusWeeks(1).toString());
    addDateCase(expected, calls, "plusMonths(2024-01-31,1)",
        "SD(H.plusMonths(D('2024-01-31'), 1))", LocalDate.parse("2024-01-31").plusMonths(1).toString());
    addDateCase(expected, calls, "plusYears(2024-02-29,1)",
        "SD(H.plusYears(D('2024-02-29'), 1))", LocalDate.parse("2024-02-29").plusYears(1).toString());
    addDateCase(expected, calls, "withMonth(2024-03-31,2)",
        "SD(H.withMonth(D('2024-03-31'), 2))", LocalDate.parse("2024-03-31").withMonth(2).toString());
    addDateCase(expected, calls, "plusDays(2026-12-31,1)",
        "SD(H.plusDays(D('2026-12-31'), 1))", LocalDate.parse("2026-12-31").plusDays(1).toString());
    LocalDateTime endOfDst = LocalDateTime.parse("2026-10-25T01:30:00");
    addDateCase(expected, calls, "plusHours(2026-10-25T01:30,1)",
        "SDT(H.plusHours(DT('2026-10-25T01:30:00'), 1))", WIRE_DATE_TIME.format(endOfDst.plusHours(1)));
    addDateCase(expected, calls, "plusMinutes(2026-10-25T01:30,90)",
        "SDT(H.plusMinutes(DT('2026-10-25T01:30:00'), 90))", WIRE_DATE_TIME.format(endOfDst.plusMinutes(90)));
    addDateCase(expected, calls, "plusHours(2026-10-25T00:00,24)",
        "SDT(H.plusHours(DT('2026-10-25T00:00:00'), 24))",
        WIRE_DATE_TIME.format(LocalDateTime.parse("2026-10-25T00:00:00").plusHours(24)));
    addDateCase(expected, calls, "plusHours(2026-03-29T01:30,2)",
        "SDT(H.plusHours(DT('2026-03-29T01:30:00'), 2))",
        WIRE_DATE_TIME.format(LocalDateTime.parse("2026-03-29T01:30:00").plusHours(2)));
    addDateCase(expected, calls, "plusDays(2026-03-28T08:00,1)",
        "SDT(H.plusDays(DT('2026-03-28T08:00:00'), 1))",
        WIRE_DATE_TIME.format(LocalDateTime.parse("2026-03-28T08:00:00").plusDays(1)));
    addDateCase(expected, calls, "withHour(2026-10-25T01:30,3)",
        "SDT(H.withHour(DT('2026-10-25T01:30:00'), 3))", WIRE_DATE_TIME.format(endOfDst.withHour(3)));
    addDateCase(expected, calls, "atStartOfDay(2026-10-25T23:30)",
        "SDT(H.atStartOfDay(DT('2026-10-25T23:30:00')))",
        WIRE_DATE_TIME.format(LocalDate.parse("2026-10-25").atStartOfDay()));

    StringBuilder js = new StringBuilder();
    js.append("const { LocalDateTimeHelper: H, LocalDateDeSer, LocalDateTimeDeSer, Conversion } = require(\"@umlp/commonj2ts\");\n");
    js.append("const D = (s) => Conversion.string2Date(s);\n");
    js.append("const DT = (s) => new Date(s);\n");
    js.append("const SD = (d) => Conversion.date2String(d);\n");
    js.append("async function main() {\n");
    js.append("  const dateTimeDeSer = new LocalDateTimeDeSer();\n");
    js.append("  const SDT = (d) => dateTimeDeSer.serialize(d);\n");
    for (Map.Entry<String, String> call : calls.entrySet()) {
      js.append("  console.log('RESULT:").append(call.getKey()).append("=' + JSON.stringify(await ")
          .append(call.getValue()).append("));\n");
    }
    js.append("}\n\n");
    js.append("main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });\n");

    Map<String, RealTscCheck.Result> results = RealTscCheck.compileAndRunInTimeZones(
        LocalDateWireRoundTrip.class, js.toString(), TIME_ZONES);
    assertSameAsJava(expected, results, "LocalDateTimeHelper");
  }

  private static void addDateCase(Map<String, String> expected, Map<String, String> calls,
      String key, String jsCall, String javaResult) {
    expected.put(key, javaResult);
    calls.put(key, jsCall);
  }

  private static void assertSameAsJava(Map<String, String> expected,
      Map<String, RealTscCheck.Result> results, String what) {
    List<String> failures = new ArrayList<>();
    for (Map.Entry<String, RealTscCheck.Result> entry : results.entrySet()) {
      RealTscCheck.Result result = entry.getValue();
      if (result.exitCode() != 0) {
        failures.add("TZ=" + entry.getKey() + ": node execution failed:\n" + result.output());
        continue;
      }
      Map<String, String> actual = result.resultValues();
      for (Map.Entry<String, String> e : expected.entrySet()) {
        String expectedJson = "\"" + e.getValue() + "\"";
        if (!Objects.equals(expectedJson, actual.get(e.getKey()))) {
          failures.add("TZ=" + entry.getKey() + ", " + e.getKey() + ": expected " + expectedJson
              + " but was " + actual.get(e.getKey()));
        }
      }
    }
    if (!failures.isEmpty()) {
      Assertions.fail(what + " differs from Java:\n" + String.join("\n", failures));
    }
  }

  private static String runner() {
    StringBuilder js = new StringBuilder();
    js.append("const { LocalDateWireRoundTrip: F } = require(\"./LocalDateWireRoundTrip\");\n");
    js.append("const { LocalDateDeSer, LocalDateTimeDeSer } = require(\"@umlp/commonj2ts\");\n\n");
    js.append("async function main() {\n");
    js.append("  const dateDeSer = new LocalDateDeSer();\n");
    js.append("  const dateTimeDeSer = new LocalDateTimeDeSer();\n");
    for (String date : DATES) {
      // On the wire, a LocalDate is a JSON string.
      js.append("  { const d = await dateDeSer.deserialize('\"").append(date).append("\"');\n");
      js.append("    const r = (await F.describeDate(d)) + ' | ' + (await dateDeSer.serialize(await F.nextDay(d)));\n");
      js.append("    console.log('RESULT:date:").append(date).append("=' + JSON.stringify(r)); }\n");
      js.append("  { const w = await dateDeSer.deserialize('\"").append(date).append("\"');\n");
      js.append("    const d = new Date(w.getFullYear(), w.getMonth(), w.getDate(), 14, 30, 0);\n");
      js.append("    const r = (await F.describeDate(d)) + ' | ' + (await dateDeSer.serialize(await F.nextDay(d)));\n");
      js.append("    console.log('RESULT:dateFromInput:").append(date).append("=' + JSON.stringify(r)); }\n");
    }
    for (Object[] dateTime : DATE_TIMES) {
      js.append("  { const t = await dateTimeDeSer.deserialize('\"").append(dateTime[0]).append("\"');\n");
      js.append("    const r = (await F.describeDateTime(t)) + ' | ' + (await dateTimeDeSer.serialize(await F.plusHours(t, ")
          .append(dateTime[1]).append(")));\n");
      js.append("    console.log('RESULT:dateTime:").append(dateTime[0]).append("+").append(dateTime[1])
          .append("=' + JSON.stringify(r)); }\n");
    }
    js.append("}\n\n");
    js.append("main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });\n");
    return js.toString();
  }
}
