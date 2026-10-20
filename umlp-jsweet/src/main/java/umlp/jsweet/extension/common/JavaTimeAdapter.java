/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.VariableAccessElement;
import umlp.jsweet.extension.AbstractAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.function.Function;

/**
 * This adapter is responsible to enable the usage of java.time and Date classes.
 *
 * <p>LocalDate and LocalDateTime are mapped to a JS Date that carries the value in its local
 * calendar fields, the convention of the runtime's (de)serializers. See the section
 * "Representation of LocalDate and LocalDateTime in the browser" below.
 */
public class JavaTimeAdapter extends AbstractAdapter {

  public JavaTimeAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeDateTypeMapping());
    addTypeMapping("java.time.LocalDateTime", "Date");
    addTypeMapping("java.time.LocalDate", "Date");
    addTypeMapping("java.time.DayOfWeek", "string");
  }

  public Function<TypeMirror, String> computeDateTypeMapping() {
    return (TypeMirror type) -> {
      switch (type.toString()) {
        case "java.time.LocalDateTime":
        case "java.time.LocalDate":
        case "java.util.Date":
          return "Date";
      }
      return null;
    };
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }
    String targetType = invocation.getTargetType().toString();
    if ("java.time.DayOfWeek".equals(targetType)) {
      switch (invocation.getMethodName()) {
        case "name":
        case "toString":
          print(invocation.getTargetExpression());
          return true;
        case "getValue":
          print("((LocalDateTimeHelper.DAYS_OF_WEEK.indexOf(")
              .print(invocation.getTargetExpression()).print(") + 6) % 7 + 1)");
          getPrinter().useModule(
              new ModuleImportDescriptor("LocalDateTimeHelper", "@umlp/commonj2ts"));
          return true;
      }
    }
    switch (targetType) {
      case "java.time.LocalDateTime":
      case "java.time.LocalDate":
      case "java.util.Date":
        if (substituteMethodInvocation_Date(invocation)) {
          return true;
        }
    }
    return super.substituteMethodInvocation(invocation);
  }

  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    VariableElement variable = variableAccess.getVariable();
    Element owner = variable == null ? null : variable.getEnclosingElement();
    if (owner != null && "java.time.DayOfWeek".equals(owner.toString())
        && variable.getKind() == ElementKind.ENUM_CONSTANT) {
      print("\"").print(variable.getSimpleName().toString()).print("\"");
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }

  /**
   * Helper method to ensure the target expression is wrapped in a JavaScript Date object.
   */
  private void printDateTarget(MethodInvocationElement invocation, boolean isLocalDateTime) {
    String targetSrc = invocation.getTargetExpression().toString();

    // Check if the target source string indicates it is already a JS Date object.
    boolean alreadyDateLike = targetSrc.contains("new Date(")
        || targetSrc.contains("Date.UTC(")
        || targetSrc.contains(".getTime()")
        || targetSrc.matches(".*\\bDate\\b.*");

    if (isLocalDateTime && !alreadyDateLike) {
      // Wrap the expression in 'new Date()' if it's a raw variable or method call.
      this.print("new Date(").print(invocation.getTargetExpression()).print(")").println();
    } else {
      // Print directly if it's already a Date object.
      this.print(invocation.getTargetExpression()).println();
    }
  }

  // ==============================================================================================
  // Representation of LocalDate and LocalDateTime in the browser
  // ==============================================================================================
  //
  // LocalDate and LocalDateTime are zone-less. The runtime represents them as a JS Date whose
  // *local* calendar fields carry the value: LocalDateDeSer creates `new Date(y, m - 1, d)`,
  // LocalDateTimeDeSer parses "yyyy-MM-ddTHH:mm:ss" as local time, the serializers and
  // Conversion.date2String read the local getters, and so do the input components. The code
  // emitted here follows the same convention:
  //
  //  - it reads only local fields (getFullYear, getMonth, getDate, getHours, ...);
  //  - it computes on calendar fields, i.e. wall-clock arithmetic as in java.time, and builds
  //    every result with the local Date constructor, so results do not depend on DST changes and
  //    are equal (getTime) to what the deserializer produces for the same value;
  //  - it compares LocalDate by its date fields and LocalDateTime by its wall-clock fields.
  //
  // Known limit: a LocalDateTime inside the hour skipped at the start of DST (e.g. 02:30 on the
  // last Sunday of March in Europe/Berlin) has no local Date. The Date constructor (and thus also
  // the deserializer) moves it forward by the DST offset.

  private static final String[] LOCAL_GETTERS = {"getFullYear()", "getMonth()", "getDate()",
      "getHours()", "getMinutes()", "getSeconds()", "getMilliseconds()"};

  private static final String[] UTC_GETTERS = {"getUTCFullYear()", "getUTCMonth()", "getUTCDate()",
      "getUTCHours()", "getUTCMinutes()", "getUTCSeconds()", "getUTCMilliseconds()"};

  private static final String DATE_TIME_PARSE_EXCEPTION_CLASSES =
      "['java.time.format.DateTimeParseException', 'java.time.DateTimeException', "
          + "'java.lang.RuntimeException', 'java.lang.Exception', 'java.lang.Throwable', "
          + "'java.lang.Object']";

  /**
   * The fields of {@code v}, read with {@code getters}; {@code overrides[i]}, if present and not
   * null, replaces field i (0 = year, 1 = month 0-11, 2 = day, 3 = hours, 4 = minutes,
   * 5 = seconds, 6 = milliseconds).
   */
  private static String fields(String v, String[] getters, boolean dateOnly, String... overrides) {
    int count = dateOnly ? 3 : 7;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      if (i < overrides.length && overrides[i] != null) {
        sb.append(overrides[i]);
      } else {
        sb.append(v).append('.').append(getters[i]);
      }
    }
    return sb.toString();
  }

  /** A local Date built from the local fields of {@code v}, see {@link #fields}. */
  private static String localDate(String v, boolean dateOnly, String... overrides) {
    return "new Date(" + fields(v, LOCAL_GETTERS, dateOnly, overrides) + ")";
  }

  /**
   * A number that orders and identifies values like their Java counterparts: the date fields for
   * LocalDate, the wall-clock fields for LocalDateTime, and the instant for java.util.Date.
   */
  private static String comparisonKey(String v, boolean isLocalDate, boolean isUtilDate) {
    if (isUtilDate) {
      return v + ".getTime()";
    }
    return "Date.UTC(" + fields(v, LOCAL_GETTERS, isLocalDate) + ")";
  }

  /**
   * Year and month arithmetic as in java.time: the day of month is clamped to the length of the
   * resulting month. Expects the target in {@code d} and the new year and month (0-11, may be out
   * of range) as {@code yearExpr} and {@code monthExpr}.
   */
  private static String withYearAndMonth(String yearExpr, String monthExpr, boolean isLocalDate) {
    return "const t = (" + yearExpr + ") * 12 + (" + monthExpr + "); "
        + "const y = Math.floor(t / 12); "
        + "const m = t - y * 12; "
        + "const day = Math.min(d.getDate(), new Date(y, m + 1, 0).getDate()); "
        + "return " + localDate("d", isLocalDate, "y", "m", "day") + "; ";
  }

  private boolean substituteMethodInvocation_Date(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    String targetType = invocation.getTargetType().toString();
    boolean isLocalDateTime = targetType.contains("LocalDateTime");
    boolean isLocalDate = targetType.equals("java.time.LocalDate");
    boolean isUtilDate = targetType.equals("java.util.Date");
    List<ExtendedElement> args = invocation.getArguments();

    // ==========================================================================================
    // FACTORY METHOD: LocalDate.of(y, m, d), LocalDateTime.of(y, m, d[, h, min[, s[, nanos]]])
    // ==========================================================================================
    if ("of".equals(methodName)) {
      if (args.size() < 3) {
        return false;
      }
      printMacroName(methodName);
      this.print("new Date(").print(args.get(0)).print(", (").print(args.get(1)).print(") - 1, ")
          .print(args.get(2));
      if (!isLocalDate) {
        for (int i = 3; i < 6; i++) {
          this.print(", ");
          if (args.size() > i) {
            this.print(args.get(i));
          } else {
            this.print("0");
          }
        }
        // Java has nanoseconds, JS milliseconds.
        this.print(", ");
        if (args.size() > 6) {
          this.print("Math.floor((").print(args.get(6)).print(") / 1000000)");
        } else {
          this.print("0");
        }
      }
      this.print(")");
      return true;
    }

    // ==========================================================================================
    // plus(Duration) / minus(Duration)
    // ==========================================================================================
    if (args.size() == 1 &&
        ("plus".equals(methodName) || "minus".equals(methodName))) {
      String targetOp = "plus".equals(methodName) ? "addTo" : "subtractFrom";
      printMacroName(methodName);
      this.print(args.get(0)).print("." + targetOp + "(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(")").println();
      return true;
    }

    // ==========================================================================================
    // plusDays, minusHours, plusMonths, etc.: calendar arithmetic on the local fields
    // ==========================================================================================
    boolean isPlus = methodName.startsWith("plus");
    boolean isMinus = methodName.startsWith("minus");

    if ((isPlus || isMinus) && args.size() == 1) {
      String unit = methodName.substring(isPlus ? "plus".length() : "minus".length());
      String body;
      switch (unit) {
        case "Years":
          body = withYearAndMonth("d.getFullYear() + k", "d.getMonth()", isLocalDate);
          break;
        case "Months":
          body = withYearAndMonth("d.getFullYear()", "d.getMonth() + k", isLocalDate);
          break;
        case "Weeks":
          body = "return " + localDate("d", isLocalDate, null, null, "d.getDate() + k * 7") + "; ";
          break;
        case "Days":
          body = "return " + localDate("d", isLocalDate, null, null, "d.getDate() + k") + "; ";
          break;
        case "Hours":
          body = "return " + localDate("d", false, null, null, null, "d.getHours() + k") + "; ";
          break;
        case "Minutes":
          body = "return " + localDate("d", false, null, null, null, null, "d.getMinutes() + k")
              + "; ";
          break;
        case "Seconds":
          body = "return " + localDate("d", false, null, null, null, null, null,
              "d.getSeconds() + k") + "; ";
          break;
        case "Nanos":
          body = "return " + localDate("d", false, null, null, null, null, null, null,
              "d.getMilliseconds() + Math.trunc(k / 1000000)") + "; ";
          break;
        default:
          return false;
      }

      printMacroName(methodName);

      // The amount is passed in as parameter n instead of being printed into the arrow function:
      // it may contain an await (e.g. an association navigation), which is only allowed in the
      // enclosing async function, not in the synchronous arrow function.
      this.print("((d, n) => { const k = " + (isPlus ? "(n)" : "-(n)") + "; ").println();
      this.print(body).println();
      this.print("})(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(", ").print(args.get(0)).println();
      this.print(")").println();
      return true;
    }

    // ==========================================================================================
    // equals
    // ==========================================================================================
    if ("equals".equals(methodName)) {
      printMacroName(methodName);
      this.print("((a, b) => b instanceof Date && " + comparisonKey("a", isLocalDate, isUtilDate)
              + " === " + comparisonKey("b", isLocalDate, isUtilDate) + ")(")
          .print(invocation.getTargetExpression()).print(", ")
          .printArgList(args).print(")");
      return true;
    }

    // ==========================================================================================
    // isAfter, isBefore, isEqual
    // ==========================================================================================
    if ("isAfter".equals(methodName) || "isBefore".equals(methodName) || "isEqual".equals(methodName)) {
      String operator;
      if ("isAfter".equals(methodName)) {
        operator = " > ";
      } else if ("isBefore".equals(methodName)) {
        operator = " < ";
      } else {
        operator = " === ";
      }
      printMacroName(methodName);
      this.print("((a, b) => " + comparisonKey("a", isLocalDate, isUtilDate) + operator
          + comparisonKey("b", isLocalDate, isUtilDate) + ")(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(", ");
      if (isLocalDateTime) this.print("new Date(");
      this.print(args.get(0));
      if (isLocalDateTime) this.print(")");
      this.print(")").println();
      return true;
    }

    // ==========================================================================================
    // getHour, getMinute, etc.
    // ==========================================================================================
    String jsSimpleMethod = switch (methodName) {
      case "getHour", "getHours" -> "getHours";
      case "getMinute", "getMinutes" -> "getMinutes";
      case "getSecond", "getSeconds" -> "getSeconds";
      case "getYear" -> "getFullYear";
      case "getDayOfMonth", "getDate" -> "getDate";
      default -> null;
    };

    if (jsSimpleMethod != null) {
      printMacroName(methodName);
      printDateTarget(invocation, isLocalDateTime);
      this.print(".").print(jsSimpleMethod).print("()").println();
      return true;
    }

    // ==========================================================================================
    // COMPLEX GETTERS & ENUMS
    // ==========================================================================================
    switch (methodName) {
      case "getMonthValue":
        printMacroName(methodName);
        this.print("(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(".getMonth() + 1)").println();
        return true;

      case "getMonth":
        printMacroName(methodName);
        this.print("[\"JANUARY\", \"FEBRUARY\", \"MARCH\", \"APRIL\", \"MAY\", \"JUNE\", \"JULY\", \"AUGUST\", \"SEPTEMBER\", \"OCTOBER\", \"NOVEMBER\", \"DECEMBER\"][").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(".getMonth()]").println();
        return true;

      case "getDayOfWeek":
        printMacroName(methodName);
        print("LocalDateTimeHelper.DAYS_OF_WEEK[");
        printDateTarget(invocation, isLocalDateTime);
        print(".getDay()]");
        getPrinter().useModule(
            new ModuleImportDescriptor("LocalDateTimeHelper", "@umlp/commonj2ts"));
        return true;

      case "getDayOfYear":
        printMacroName(methodName);
        this.print("((d) => Math.round((Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())"
            + " - Date.UTC(d.getFullYear(), 0, 0)) / 86400000))(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(")").println();
        return true;

      case "getNano":
        printMacroName(methodName);
        this.print("(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(".getMilliseconds() * 1000000)").println();
        return true;

      case "toString":
        printMacroName(methodName);
        this.print("((d) => { ").println();
        this.print("if(d == null || d === undefined){").println();
        this.print("  return null;").println();
        this.print("}").println();
        this.print("  const pad = (n) => ('0' + n).slice(-2); ").println();
        this.print("  let s = d.getFullYear() + '-' + ").println();
        this.print("         pad(d.getMonth() + 1) + '-' + ").println();
        this.print("         pad(d.getDate()); ").println();

        if (!isLocalDate) {
          this.print("  s += 'T' + ").println();
          this.print("         pad(d.getHours()) + ':' + ").println();
          this.print("         pad(d.getMinutes()); ").println();
          this.print("  if (d.getSeconds() > 0 || d.getMilliseconds() > 0) { ").println();
          this.print("    s += ':' + pad(d.getSeconds()); ").println();
          this.print("  } ").println();
          this.print("  if (d.getMilliseconds() > 0) { ").println();
          this.print("    s += '.' + ('00' + d.getMilliseconds()).slice(-3); ").println();
          this.print("  } ").println();
        }
        this.print("  return s; ").println();
        this.print("})(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(")").println();
        return true;

      case "now":
        // Rebuilt from the local fields, so the value is the same as one created by `of` or by
        // the deserializer (relevant in the hour repeated at the end of DST).
        this.print("((t) => ").print(localDate("t", isLocalDate)).print(")(new Date())");
        return true;

      case "parse":
        if (args.size() != 1) {
          return false;
        }
        printMacroName(methodName);
        // Unlike java.time, LocalDate.parse also accepts a time part and ignores it, as the earlier
        // translation `new Date(s + 'Z')` did: GemTextInput emits "yyyy-MM-ddTHH:mm:ss" for date
        // inputs, and the generated GUI code passes that to LocalDate.parse.
        String timePart = "T(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,9}))?)?";
        this.print("((s) => { const p = /^(\\d{4})-(\\d{2})-(\\d{2})"
            + (isLocalDate ? "(?:" + timePart + ")?" : timePart) + "$/.exec(s); ");
        // Field ranges as checked by java.time (ISO_LOCAL_DATE[_TIME] resolves strictly): the
        // Date constructor would roll e.g. 2026-02-30 or 24:00 over into the next month or day.
        String validFields = "+p[2] >= 1 && +p[2] <= 12 && +p[3] >= 1 && +p[3] <= [31, "
            + "(+p[1] % 4 === 0 && +p[1] % 100 !== 0) || +p[1] % 400 === 0 ? 29 : 28, "
            + "31, 30, 31, 30, 31, 31, 30, 31, 30, 31][+p[2] - 1] "
            + "&& +(p[4] || 0) <= 23 && +(p[5] || 0) <= 59 && +(p[6] || 0) <= 59";
        this.print("if (p === null || !(" + validFields + ")) { throw Object.defineProperty(new Error(\"Text '\" + s + \"' "
            + "could not be parsed\"), '__classes', { configurable: true, value: "
            + DATE_TIME_PARSE_EXCEPTION_CLASSES + " }); } ");
        if (isLocalDate) {
          this.print("return new Date(+p[1], +p[2] - 1, +p[3]); ");
        } else {
          this.print("return new Date(+p[1], +p[2] - 1, +p[3], +p[4], +p[5], +(p[6] || 0), "
              + "p[7] === undefined ? 0 : +(p[7] + '00').substring(0, 3)); ");
        }
        this.print("})(").print(args.get(0)).print(")");
        return true;

      case "compareTo":
        if (isUtilDate) {
          print("((a, b) => Math.sign(a.getTime() - b.getTime()))(");
        } else {
          // Like java.time: the first differing date field, then the sign of the time difference.
          String dateDiff = "(a.getFullYear() - b.getFullYear()) || (a.getMonth() - b.getMonth())"
              + " || (a.getDate() - b.getDate())";
          if (isLocalDate) {
            print("((a, b) => " + dateDiff + ")(");
          } else {
            String timeA = "(((a.getHours() * 60 + a.getMinutes()) * 60 + a.getSeconds()) * 1000"
                + " + a.getMilliseconds())";
            String timeB = timeA.replace("a.", "b.");
            print("((a, b) => " + dateDiff + " || Math.sign(" + timeA + " - " + timeB + "))(");
          }
        }
        print(invocation.getTargetExpression()).print(", ").printArgList(args).print(")");
        return true;

      case "toEpochSecond":
        // The ZoneOffset argument is not available in the browser; UTC is assumed.
        print("((d) => Math.floor(Date.UTC(" + fields("d", LOCAL_GETTERS, isLocalDate)
            + ") / 1000))(").print(invocation.getTargetExpression()).print(")");
        return true;

      case "ofEpochSecond":
        // The ZoneOffset argument is not available in the browser; UTC is assumed.
        print("((t) => new Date(" + fields("t", UTC_GETTERS, false) + "))(new Date((")
            .print(args.get(0)).print(") * 1000");
        if (args.size() > 1) {
          print(" + Math.floor((").print(args.get(1)).print(") / 1000000)");
        }
        print("))");
        return true;

      case "toLocalDate":
      case "atStartOfDay":
        if (!args.isEmpty()) {
          return false;
        }
        print("((d) => " + localDate("d", true) + ")(")
            .print(invocation.getTargetExpression()).print(")");
        return true;

      case "toLocalTime":
        print(invocation.getTargetExpression());
        return true;

      case "withYear":
      case "withMonth":
      case "withDayOfMonth":
      case "withDayOfYear":
      case "withHour":
      case "withMinute":
      case "withSecond":
      case "withNano": {
        if (args.size() != 1) {
          return false;
        }
        String body = switch (methodName) {
          case "withYear" -> withYearAndMonth("n", "d.getMonth()", isLocalDate);
          case "withMonth" -> withYearAndMonth("d.getFullYear()", "n - 1", isLocalDate);
          case "withDayOfMonth" -> "return " + localDate("d", isLocalDate, null, null, "n") + "; ";
          case "withDayOfYear" -> "return " + localDate("d", isLocalDate, null, "0", "n") + "; ";
          case "withHour" -> "return " + localDate("d", false, null, null, null, "n") + "; ";
          case "withMinute" -> "return " + localDate("d", false, null, null, null, null, "n")
              + "; ";
          case "withSecond" -> "return " + localDate("d", false, null, null, null, null, null,
              "n") + "; ";
          default -> "return " + localDate("d", false, null, null, null, null, null, null,
              "Math.floor(n / 1000000)") + "; ";
        };
        print("((d, n) => { " + body + "})(").print(invocation.getTargetExpression()).print(", ")
            .print(args.get(0)).print(")");
        return true;
      }
    }
    return false;
  }
}
