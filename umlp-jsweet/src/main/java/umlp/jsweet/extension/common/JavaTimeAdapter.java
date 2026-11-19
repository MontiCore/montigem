/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.AssignmentWithOperatorElement;
import org.jsweet.transpiler.model.BinaryOperatorElement;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ExtendedElementFactory;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.VariableAccessElement;
import standalone.com.sun.source.tree.CompoundAssignmentTree;
import umlp.jsweet.extension.AbstractAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This adapter is responsible to enable the usage of java.time classes: LocalDate, LocalDateTime, LocalTime, Instant,
 * Duration and ZonedDateTime become the Temporal types of @umlp/commonj2ts (see TEMPORAL_TYPES), ZoneId and ZoneOffset
 * their ids like in Java ("Europe/Berlin", "+02:00", "Z"), the constants of DayOfWeek and ChronoUnit strings.
 * java.util.Date is not supported (JSweetCoCoChecker, 0xEE013).
 */
public class JavaTimeAdapter extends AbstractAdapter {

  public static final String TEMPORAL = "Temporal";

  public static final Map<String, String> TEMPORAL_TYPES = Map.of(
      "java.time.LocalDate", "Temporal.PlainDate",
      "java.time.LocalDateTime", "Temporal.PlainDateTime",
      "java.time.LocalTime", "Temporal.PlainTime",
      "java.time.Instant", "Temporal.Instant",
      "java.time.Duration", "Temporal.Duration",
      "java.time.ZonedDateTime", "Temporal.ZonedDateTime");

  /** ZoneId and ZoneOffset (a ZoneId), strings in transpiled code, see ZonedDateTimeHelper */
  private static final List<String> ZONE_TYPES = List.of("java.time.ZoneId", "java.time.ZoneOffset");

  /** the static methods of ZoneId and ZoneOffset, see ZonedDateTimeHelper; of is zoneOf or offsetOf */
  private static final List<String> ZONE_FACTORIES = List.of(
      "systemDefault", "ofHours", "ofHoursMinutes", "ofHoursMinutesSeconds", "ofTotalSeconds");

  /** the Temporal expressions for now in a zone, by java.time type */
  private static final Map<String, String> TEMPORAL_NOW_IN_ZONE = Map.of(
      "java.time.LocalDate", "Temporal.Now.plainDateISO",
      "java.time.LocalDateTime", "Temporal.Now.plainDateTimeISO",
      "java.time.LocalTime", "Temporal.Now.plainTimeISO",
      "java.time.ZonedDateTime", "Temporal.Now.zonedDateTimeISO");

  /** ofInstant(instant, zone) of the local types: the local part of the ZonedDateTime */
  private static final Map<String, String> OF_INSTANT_PART = Map.of(
      "java.time.LocalDate", ".toPlainDate()",
      "java.time.LocalDateTime", ".toPlainDateTime()",
      "java.time.LocalTime", ".toPlainTime()",
      "java.time.ZonedDateTime", "");

  public static final String CHRONO_UNIT = "java.time.temporal.ChronoUnit";

  /** the static methods of Duration, see DurationHelper */
  private static final List<String> DURATION_FACTORIES = List.of(
      "ofDays", "ofHours", "ofMinutes", "ofSeconds", "ofMillis", "ofNanos", "of", "between", "parse");

  /** the methods of a Duration, see DurationHelper; plus and minus of an amount in a unit are plusUnits/minusUnits */
  private static final List<String> DURATION_METHODS = List.of(
      "plus", "minus", "plusDays", "plusHours", "plusMinutes", "plusSeconds", "plusMillis", "plusNanos", "minusDays",
      "minusHours", "minusMinutes", "minusSeconds", "minusMillis", "minusNanos", "multipliedBy", "dividedBy",
      "negated", "abs", "isZero", "isNegative", "isPositive", "getSeconds", "getNano", "toDays", "toHours", "toMinutes",
      "toSeconds", "toMillis", "toNanos", "toDaysPart", "toHoursPart", "toMinutesPart", "toSecondsPart", "toMillisPart",
      "toNanosPart", "truncatedTo", "compareTo", "equals", "toString");

  /** the Temporal duration units of the plus and minus methods of Instant, e.g. plusMillis */
  private static final Map<String, String> INSTANT_UNITS = Map.of(
      "Seconds", "seconds", "Millis", "milliseconds", "Nanos", "nanoseconds");

  /** a printed Temporal type or call, e.g. "Temporal.PlainDate" */
  private static final Pattern TEMPORAL_USE = Pattern.compile("\\bTemporal\\.");

  /** the Temporal expressions for now, by java.time type */
  private static final Map<String, String> TEMPORAL_NOW = Map.of(
      "java.time.LocalDate", "Temporal.Now.plainDateISO()",
      "java.time.LocalDateTime", "Temporal.Now.plainDateTimeISO()",
      "java.time.LocalTime", "Temporal.Now.plainTimeISO()",
      "java.time.ZonedDateTime", "Temporal.Now.zonedDateTimeISO()");

  /** the Temporal duration units of the plus and minus methods, e.g. plusDays */
  private static final Map<String, String> TEMPORAL_UNITS = Map.of(
      "Years", "years", "Months", "months", "Weeks", "weeks", "Days", "days", "Hours", "hours",
      "Minutes", "minutes", "Seconds", "seconds", "Nanos", "nanoseconds");

  /** the Temporal properties of the java.time getters */
  private static final Map<String, String> TEMPORAL_GETTERS = Map.of(
      "getYear", "year", "getMonthValue", "month", "getDayOfMonth", "day", "getDayOfYear", "dayOfYear",
      "getHour", "hour", "getMinute", "minute", "getSecond", "second", "lengthOfMonth", "daysInMonth",
      "lengthOfYear", "daysInYear", "isLeapYear", "inLeapYear");

  /** the Temporal fields of the java.time with methods, e.g. withYear */
  private static final Map<String, String> TEMPORAL_WITH = Map.of(
      "withYear", "year", "withMonth", "month", "withDayOfMonth", "day", "withHour", "hour",
      "withMinute", "minute", "withSecond", "second");

  /** the with methods that adjust the day of month like java.time; the others reject invalid values */
  private static final List<String> TEMPORAL_WITH_ADJUSTING = List.of("withYear", "withMonth");

  public JavaTimeAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeDateTypeMapping());
    TEMPORAL_TYPES.forEach(this::addTypeMapping);
    addTypeMapping("java.time.DayOfWeek", "string");
    addTypeMapping(CHRONO_UNIT, "string");
    ZONE_TYPES.forEach(zoneType -> addTypeMapping(zoneType, "string"));
  }

  public Function<TypeMirror, String> computeDateTypeMapping() {
    return (TypeMirror type) -> {
      String temporalType = TEMPORAL_TYPES.get(type.toString());
      if (temporalType != null) {
        return temporalType;
      }
      return null;
    };
  }

  @Override
  public void afterType(TypeElement type) {
    // the string type mappings print the Temporal types without a hook (also in default methods injected from
    // interfaces), so Temporal is imported if the printed code uses it
    if (TEMPORAL_USE.matcher(getPrinter().getOutput()).find()) {
      useTemporal();
    }
    super.afterType(type);
  }

  private void useTemporal() {
    getPrinter().useModule(new ModuleImportDescriptor(TEMPORAL, getLibraryLocation()));
  }

  private void useHelper() {
    getPrinter().useModule(new ModuleImportDescriptor("LocalDateTimeHelper", getLibraryLocation()));
  }

  private void useDurationHelper() {
    getPrinter().useModule(new ModuleImportDescriptor("DurationHelper", getLibraryLocation()));
  }

  private void useZonedHelper() {
    getPrinter().useModule(new ModuleImportDescriptor("ZonedDateTimeHelper", getLibraryLocation()));
  }

  private void printZonedHelper(String method, List<ExtendedElement> args) {
    useZonedHelper();
    print("ZonedDateTimeHelper." + method + "(").printArgList(args).print(")");
  }

  /** the Temporal time zone of a ZoneId */
  private void printTimeZone(ExtendedElement zone) {
    printZonedHelper("tz", List.of(zone));
  }

  private static boolean isZone(ExtendedElement element) {
    return element.getType() != null && ZONE_TYPES.contains(element.getType().toString());
  }

  private void printDurationHelper(String method, List<ExtendedElement> args) {
    useDurationHelper();
    print("DurationHelper." + method + "(").printArgList(args).print(")");
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
    if (substituteStringBuilding(invocation, targetType)) {
      return true;
    }
    if (CHRONO_UNIT.equals(targetType) && substituteMethodInvocation_ChronoUnit(invocation)) {
      return true;
    }
    if (TEMPORAL_TYPES.containsKey(targetType) && !"java.time.Duration".equals(targetType)
        && substituteUnitMethods(invocation)) {
      return true;
    }
    if (ZONE_TYPES.contains(targetType) && substituteMethodInvocation_Zone(invocation, targetType)) {
      return true;
    }
    if ("java.time.Duration".equals(targetType)) {
      if (substituteMethodInvocation_Duration(invocation)) {
        return true;
      }
    } else if (TEMPORAL_TYPES.containsKey(targetType) && substituteDurationArithmetic(invocation, targetType)) {
      return true;
    } else if ("java.time.Instant".equals(targetType)) {
      if (substituteMethodInvocation_Instant(invocation)) {
        return true;
      }
    } else if (TEMPORAL_TYPES.containsKey(targetType)
        && substituteMethodInvocation_Temporal(invocation, targetType)) {
      return true;
    }
    return super.substituteMethodInvocation(invocation);
  }

  /**
   * the methods of LocalDate, LocalDateTime and LocalTime on their Temporal types; the ones without a direct
   * counterpart call LocalDateTimeHelper
   */
  private boolean substituteMethodInvocation_Temporal(MethodInvocationElement invocation, String javaType) {
    String methodName = invocation.getMethodName();
    String temporalType = TEMPORAL_TYPES.get(javaType);
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    useTemporal();

    boolean zoned = "java.time.ZonedDateTime".equals(javaType);

    // static methods
    switch (methodName) {
      case "of":
        printMacroName(methodName);
        if (zoned) {
          // of(LocalDateTime, zone), of(LocalDate, LocalTime, zone), of(year, ..., nanoOfSecond, zone)
          ExtendedElement zone = args.get(args.size() - 1);
          if (args.size() == 2) {
            printTarget(args.get(0));
          } else if (args.size() == 3) {
            print("(").print(args.get(0)).print(").toPlainDateTime(").print(args.get(1)).print(")");
          } else {
            printHelper("ofDateTime", args.subList(0, args.size() - 1));
          }
          print(".toZonedDateTime(");
          printTimeZone(zone);
          print(")");
        } else if ("java.time.LocalDate".equals(javaType)) {
          print("new Temporal.PlainDate(").printArgList(args).print(")");
        } else if ("java.time.LocalDateTime".equals(javaType) && args.size() == 2) {
          // of(LocalDate, LocalTime)
          print("(").print(args.get(0)).print(").toPlainDateTime(").print(args.get(1)).print(")");
        } else {
          printHelper("java.time.LocalTime".equals(javaType) ? "ofTime" : "ofDateTime", args);
        }
        return true;
      case "now":
        if (args.isEmpty()) {
          printMacroName(methodName);
          print(TEMPORAL_NOW.get(javaType));
          return true;
        }
        if (isZone(args.get(0))) {
          printMacroName(methodName);
          print(TEMPORAL_NOW_IN_ZONE.get(javaType) + "(");
          printTimeZone(args.get(0));
          print(")");
          return true;
        }
        // now(Clock)
        return false;
      case "parse":
        if (args.size() != 1) {
          // parse(text, formatter)
          return false;
        }
        printMacroName(methodName);
        if (zoned) {
          printZonedHelper("parse", args);
        } else {
          print(temporalType + ".from(").print(args.get(0)).print(")");
        }
        return true;
      case "ofEpochSecond":
        printMacroName(methodName);
        printHelper("ofEpochSecond", args);
        return true;
      case "ofSecondOfDay":
      case "ofNanoOfDay":
      case "ofEpochDay":
        printMacroName(methodName);
        printHelper(methodName, args);
        return true;
      case "ofInstant":
        printMacroName(methodName);
        printTarget(args.get(0)).print(".toZonedDateTimeISO(");
        printTimeZone(args.get(1));
        print(")" + OF_INSTANT_PART.get(javaType));
        return true;
    }
    if (target == null) {
      return false;
    }

    // getters
    String property = TEMPORAL_GETTERS.get(methodName);
    if (property != null) {
      printMacroName(methodName);
      printTarget(target).print("." + property);
      return true;
    }

    // plusDays, minusHours, ...
    boolean isPlus = methodName.startsWith("plus");
    if (isPlus || methodName.startsWith("minus")) {
      String unit = TEMPORAL_UNITS.get(methodName.substring(isPlus ? 4 : 5));
      if (unit != null && args.size() == 1) {
        printMacroName(methodName);
        printTarget(target).print(isPlus ? ".add({" : ".subtract({").print(unit + ": ").print(args.get(0))
            .print("})");
        return true;
      }
    }

    // withYear, withHour, ...
    String field = TEMPORAL_WITH.get(methodName);
    if (field != null) {
      printMacroName(methodName);
      printTarget(target).print(".with({" + field + ": ").print(args.get(0))
          .print(TEMPORAL_WITH_ADJUSTING.contains(methodName) ? "})" : "}, {overflow: \"reject\"})");
      return true;
    }

    switch (methodName) {
      case "atZone":
        printMacroName(methodName);
        printTarget(target).print(".toZonedDateTime(");
        printTimeZone(args.get(0));
        print(")");
        return true;
      case "toLocalDateTime":
        printMacroName(methodName);
        printTarget(target).print(".toPlainDateTime()");
        return true;
      case "toInstant":
        printMacroName(methodName);
        printTarget(target).print(".toInstant()");
        return true;
      case "withZoneSameInstant":
        printMacroName(methodName);
        printTarget(target).print(".withTimeZone(");
        printTimeZone(args.get(0));
        print(")");
        return true;
      case "getZone":
      case "getOffset":
      case "withZoneSameLocal":
        printMacroName(methodName);
        List<ExtendedElement> targetAndArgs = new ArrayList<>();
        targetAndArgs.add(target);
        targetAndArgs.addAll(args);
        printZonedHelper(methodName, targetAndArgs);
        return true;
      case "equals":
        printMacroName(methodName);
        print("((a, b) => b instanceof " + temporalType + " && a.equals(b))(").print(target).print(", ")
            .printArgList(args).print(")");
        return true;
      case "isAfter":
      case "isBefore":
      case "isEqual":
        printMacroName(methodName);
        String operator = "isAfter".equals(methodName) ? " > 0"
            : "isBefore".equals(methodName) ? " < 0" : " === 0";
        print("(" + temporalType + ".compare(").print(target).print(", ").print(args.get(0))
            .print(")" + operator + ")");
        return true;
      case "compareTo":
        printMacroName(methodName);
        if (zoned) {
          // by the instant, then the local date-time, then the zone, like Java
          printZonedHelper("compareTo", List.of(target, args.get(0)));
        } else {
          print(temporalType + ".compare(").print(target).print(", ").print(args.get(0)).print(")");
        }
        return true;
      case "toString":
      case "getNano":
      case "getDayOfWeek":
      case "getMonth":
        printMacroName(methodName);
        printHelper(methodName, List.of(target));
        return true;
      case "toEpochSecond":
        // ZonedDateTime: no argument; LocalDateTime: (offset); LocalDate: (time, offset); LocalTime: (date, offset)
        printMacroName(methodName);
        useHelper();
        print("LocalDateTimeHelper.toEpochSecond(");
        if (args.size() == 2) {
          boolean isDate = "java.time.LocalDate".equals(javaType);
          print("(").print(isDate ? target : args.get(0)).print(").toPlainDateTime(")
              .print(isDate ? args.get(0) : target).print("), ").print(args.get(1));
        } else {
          print(target).print(args.isEmpty() ? "" : ", ").printArgList(args);
        }
        print(")");
        return true;
      case "withDayOfYear":
      case "withNano":
        printMacroName(methodName);
        printHelper(methodName, List.of(target, args.get(0)));
        return true;
      case "toSecondOfDay":
      case "toNanoOfDay":
      case "toEpochDay":
        printMacroName(methodName);
        printHelper(methodName, List.of(target));
        return true;
      case "atDate":
        // LocalTime.atDate(date)
        printMacroName(methodName);
        printTarget(args.get(0)).print(".toPlainDateTime(").print(target).print(")");
        return true;
      case "toLocalDate":
        printMacroName(methodName);
        printTarget(target).print(".toPlainDate()");
        return true;
      case "toLocalTime":
        printMacroName(methodName);
        printTarget(target).print(".toPlainTime()");
        return true;
      case "atStartOfDay":
        printMacroName(methodName);
        if (args.isEmpty()) {
          printTarget(target).print(".toPlainDateTime()");
        } else {
          // atStartOfDay(zone): the first valid time of the day in zone, like Java
          printTarget(target).print(".toZonedDateTime({timeZone: ");
          printTimeZone(args.get(0));
          print("})");
        }
        return true;
      case "atTime":
        // atTime(LocalTime) or atTime(hour, minute[, second[, nanoOfSecond]])
        printMacroName(methodName);
        printTarget(target).print(".toPlainDateTime(");
        if (args.size() == 1) {
          print(args.get(0));
        } else {
          printHelper("ofTime", args);
        }
        print(")");
        return true;
    }
    return false;
  }

  /**
   * the methods of Instant on Temporal.Instant; the ones without a direct counterpart call LocalDateTimeHelper
   */
  private boolean substituteMethodInvocation_Instant(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    useTemporal();

    // static methods
    switch (methodName) {
      case "now":
        printMacroName(methodName);
        print("Temporal.Now.instant()");
        return true;
      case "parse":
        printMacroName(methodName);
        print("Temporal.Instant.from(").print(args.get(0)).print(")");
        return true;
      case "ofEpochMilli":
        printMacroName(methodName);
        print("Temporal.Instant.fromEpochMilliseconds(").print(args.get(0)).print(")");
        return true;
      case "ofEpochSecond":
        printMacroName(methodName);
        printHelper("instantOfEpochSecond", args);
        return true;
    }
    if (target == null) {
      return false;
    }

    // plusSeconds, minusMillis, ...
    boolean isPlus = methodName.startsWith("plus");
    if (isPlus || methodName.startsWith("minus")) {
      String unit = INSTANT_UNITS.get(methodName.substring(isPlus ? 4 : 5));
      if (unit != null && args.size() == 1) {
        printMacroName(methodName);
        printTarget(target).print(isPlus ? ".add({" : ".subtract({").print(unit + ": ").print(args.get(0))
            .print("})");
        return true;
      }
    }

    switch (methodName) {
      case "atZone":
        printMacroName(methodName);
        printTarget(target).print(".toZonedDateTimeISO(");
        printTimeZone(args.get(0));
        print(")");
        return true;
      case "toEpochMilli":
        printMacroName(methodName);
        printTarget(target).print(".epochMilliseconds");
        return true;
      case "equals":
        printMacroName(methodName);
        print("((a, b) => b instanceof Temporal.Instant && a.equals(b))(").print(target).print(", ")
            .printArgList(args).print(")");
        return true;
      case "isAfter":
      case "isBefore":
        printMacroName(methodName);
        print("(Temporal.Instant.compare(").print(target).print(", ").print(args.get(0))
            .print(")" + ("isAfter".equals(methodName) ? " > 0" : " < 0") + ")");
        return true;
      case "compareTo":
        printMacroName(methodName);
        print("Temporal.Instant.compare(").print(target).print(", ").print(args.get(0)).print(")");
        return true;
      case "toString":
      case "getNano":
      case "getEpochSecond":
        printMacroName(methodName);
        printHelper(methodName, List.of(target));
        return true;
    }
    return false;
  }

  /** the methods of ZoneId and ZoneOffset on their ids, see ZonedDateTimeHelper */
  private boolean substituteMethodInvocation_Zone(MethodInvocationElement invocation, String targetType) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    if ("of".equals(methodName) && args.size() == 1 && isStaticCall(invocation)) {
      printMacroName(methodName);
      printZonedHelper("java.time.ZoneOffset".equals(targetType) ? "offsetOf" : "zoneOf", args);
      return true;
    }
    if (ZONE_FACTORIES.contains(methodName) && isStaticCall(invocation)) {
      printMacroName(methodName);
      printZonedHelper(methodName, args);
      return true;
    }
    if (target == null) {
      return false;
    }
    switch (methodName) {
      case "getId":
      case "toString":
        printMacroName(methodName);
        printTarget(target);
        return true;
      case "normalized":
        printMacroName(methodName);
        printZonedHelper(methodName, List.of(target));
        return true;
      case "equals":
        printMacroName(methodName);
        print("(").print(target).print(" === ").print(args.get(0)).print(")");
        return true;
      case "getTotalSeconds":
        printMacroName(methodName);
        printZonedHelper(methodName, List.of(target));
        return true;
    }
    return false;
  }

  /** the methods of ChronoUnit on the name of the constant, see DurationHelper */
  private boolean substituteMethodInvocation_ChronoUnit(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    ExtendedElement target = invocation.getTargetExpression();
    List<ExtendedElement> args = invocation.getArguments();
    switch (methodName) {
      case "between":
        // unit.between(start, end)
        printMacroName(methodName);
        List<ExtendedElement> unitAndArgs = new ArrayList<>();
        unitAndArgs.add(target);
        unitAndArgs.addAll(args);
        printDurationHelper("unitsBetween", unitAndArgs);
        return true;
      case "getDuration":
        printMacroName(methodName);
        printDurationHelper(methodName, List.of(target));
        return true;
      case "valueOf":
        if (args.size() != 1 || !isStaticCall(invocation)) {
          return false;
        }
        printMacroName(methodName);
        printDurationHelper("unitOf", args);
        return true;
      case "name":
        printMacroName(methodName);
        printTarget(target);
        return true;
      case "toString":
        printMacroName(methodName);
        printDurationHelper("unitToString", List.of(target));
        return true;
    }
    return false;
  }

  /** until(end, unit) and truncatedTo(unit) of the java.time values, see DurationHelper */
  private boolean substituteUnitMethods(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    ExtendedElement target = invocation.getTargetExpression();
    List<ExtendedElement> args = invocation.getArguments();
    if (target == null) {
      return false;
    }
    if ("until".equals(methodName) && args.size() == 2) {
      printMacroName(methodName);
      printDurationHelper("unitsBetween", List.of(args.get(1), target, args.get(0)));
      return true;
    }
    if ("truncatedTo".equals(methodName) && args.size() == 1) {
      printMacroName(methodName);
      printDurationHelper(methodName, List.of(target, args.get(0)));
      return true;
    }
    return false;
  }

  /**
   * plus(Duration) and minus(Duration) of a LocalDateTime, LocalTime or Instant (a LocalDate has no time, Java rejects
   * a Duration); plus(amount, unit) and minus(amount, unit) of all of them
   */
  private boolean substituteDurationArithmetic(MethodInvocationElement invocation, String targetType) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    if (target == null || !("plus".equals(methodName) || "minus".equals(methodName))) {
      return false;
    }
    if (args.size() == 1 && args.get(0).getType() != null
        && "java.time.Duration".equals(args.get(0).getType().toString())
        && !"java.time.LocalDate".equals(targetType)) {
      printMacroName(methodName);
      useTemporal();
      printTarget(target).print("plus".equals(methodName) ? ".add(" : ".subtract(").print(args.get(0)).print(")");
      return true;
    }
    if (args.size() == 2) {
      printMacroName(methodName);
      printDurationHelper(methodName + "Units", List.of(target, args.get(0), args.get(1)));
      return true;
    }
    return false;
  }

  /** the methods of Duration on Temporal.Duration, all through DurationHelper (exact, java.time formats) */
  private boolean substituteMethodInvocation_Duration(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    useTemporal();
    if (DURATION_FACTORIES.contains(methodName) && (target == null || isStaticCall(invocation))) {
      printMacroName(methodName);
      printDurationHelper(methodName, args);
      return true;
    }
    if (target == null || !DURATION_METHODS.contains(methodName)) {
      return false;
    }
    printMacroName(methodName);
    List<ExtendedElement> targetAndArgs = new ArrayList<>();
    targetAndArgs.add(target);
    targetAndArgs.addAll(args);
    boolean amountInUnit = ("plus".equals(methodName) || "minus".equals(methodName)) && args.size() == 2;
    printDurationHelper(amountInUnit ? methodName + "Units" : methodName, targetAndArgs);
    return true;
  }

  private boolean isStaticCall(MethodInvocationElement invocation) {
    return invocation.getMethod() != null && invocation.getMethod().getModifiers().contains(Modifier.STATIC);
  }

  private PrinterAdapter printTarget(ExtendedElement target) {
    return print("(").print(target).print(")");
  }

  private void printHelper(String method, List<ExtendedElement> args) {
    useHelper();
    print("LocalDateTimeHelper." + method + "(").printArgList(args).print(")");
  }

  /**
   * string concatenation: Temporal values refuse the + operator, so a java.time operand is printed with its
   * java.time toString
   */
  @Override
  public boolean substituteBinaryOperator(BinaryOperatorElement binaryOperator) {
    ExtendedElement left = binaryOperator.getLeftHandSide();
    ExtendedElement right = binaryOperator.getRightHandSide();
    if ("+".equals(binaryOperator.getOperator()) && binaryOperator.getType() != null
        && "java.lang.String".equals(binaryOperator.getType().toString())
        && (hasJavaString(left) || hasJavaString(right))) {
      printConcatOperand(left);
      print(" + ");
      printConcatOperand(right);
      return true;
    }
    return super.substituteBinaryOperator(binaryOperator);
  }

  /** string concatenation by +=, see substituteBinaryOperator */
  @Override
  public boolean substituteAssignmentWithOperator(AssignmentWithOperatorElement assignment) {
    if ("+=".equals(assignment.getOperator()) && hasJavaString(assignment.getValue()) && assignment.getType() != null
        && "java.lang.String".equals(assignment.getType().toString())) {
      // the target from the tree: getTarget() supports variables only, not e.g. array elements
      getPrinter().print(((CompoundAssignmentTree) ExtendedElementFactory.toTree(assignment)).getVariable())
          .print(" += ");
      printJavaString(assignment.getValue());
      return true;
    }
    return super.substituteAssignmentWithOperator(assignment);
  }

  /**
   * StringBuilder.append and String.valueOf with a java.time value, see substituteBinaryOperator; the value is
   * evaluated outside the lambda, as it may contain an await
   */
  private boolean substituteStringBuilding(MethodInvocationElement invocation, String targetType) {
    if (invocation.getArgumentCount() != 1 || !hasJavaString(invocation.getArgument(0))) {
      return false;
    }
    String methodName = invocation.getMethodName();
    if ("append".equals(methodName)
        && ("java.lang.StringBuilder".equals(targetType) || "java.lang.StringBuffer".equals(targetType))) {
      printMacroName(methodName);
      print("((sb, s) => { sb.str += s; return sb; })(").print(invocation.getTargetExpression()).print(", ");
      printJavaString(invocation.getArgument(0));
      print(")");
      return true;
    }
    if ("valueOf".equals(methodName) && "java.lang.String".equals(targetType)) {
      // String(null) is "null", like Java
      printMacroName(methodName);
      print("String(");
      printJavaString(invocation.getArgument(0));
      print(")");
      return true;
    }
    return false;
  }

  /** whether the element is a java.time value, see TEMPORAL_TYPES */
  public static boolean isTemporal(ExtendedElement element) {
    return element.getType() != null && TEMPORAL_TYPES.containsKey(element.getType().toString());
  }

  private void printConcatOperand(ExtendedElement operand) {
    if (hasJavaString(operand)) {
      printJavaString(operand);
    } else {
      print(operand);
    }
  }

  /**
   * whether the string of the element differs from the one JS makes: a java.time value (the format of java.time;
   * Temporal values refuse the conversion) or a ChronoUnit (its display name, e.g. "Days")
   */
  public static boolean hasJavaString(ExtendedElement element) {
    return isTemporal(element) || element.getType() != null && CHRONO_UNIT.equals(element.getType().toString());
  }

  /** the toString of the element like in Java, see hasJavaString */
  private void printJavaString(ExtendedElement element) {
    if (isTemporal(element)) {
      useTemporal();
      printHelper("toString", List.of(element));
    } else {
      printDurationHelper("unitToString", List.of(element));
    }
  }

  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    VariableElement variable = variableAccess.getVariable();
    Element owner = variable == null ? null : variable.getEnclosingElement();
    if (owner != null && ("java.time.DayOfWeek".equals(owner.toString()) || CHRONO_UNIT.equals(owner.toString()))
        && variable.getKind() == ElementKind.ENUM_CONSTANT) {
      print("\"").print(variable.getSimpleName().toString()).print("\"");
      return true;
    }
    if (owner != null && "java.time.ZoneOffset".equals(owner.toString())) {
      String constant = variable.getSimpleName().toString();
      String offset = "UTC".equals(constant) ? "Z" : "MIN".equals(constant) ? "-18:00"
          : "MAX".equals(constant) ? "+18:00" : null;
      if (offset != null) {
        print("\"" + offset + "\"");
        return true;
      }
    }
    if (owner != null && "java.time.Duration".equals(owner.toString())
        && "ZERO".equals(variable.getSimpleName().toString())) {
      useDurationHelper();
      print("DurationHelper.ZERO");
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }
}
