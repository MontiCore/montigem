/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.AssignmentWithOperatorElement;
import org.jsweet.transpiler.model.BinaryOperatorElement;
import org.jsweet.transpiler.model.CaseElement;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This adapter is responsible to enable the usage of java.time classes: LocalDate, LocalDateTime, LocalTime, Instant,
 * Duration and ZonedDateTime become the Temporal types of @umlp/commonj2ts (see TEMPORAL_TYPES), ZoneId and ZoneOffset
 * their ids like in Java ("Europe/Berlin", "+02:00", "Z"), the constants of DayOfWeek and ChronoUnit strings.
 * java.util.Date is not supported (JSweetCoCoChecker, 0xEE013).
 */
public class JavaTimeAdapter extends AbstractAdapter {

  // ---------------------------------------------------------------------------------------------------------------
  // types

  private static final String TEMPORAL = "Temporal";

  private static final String LOCAL_DATE = "java.time.LocalDate";
  private static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";
  private static final String LOCAL_TIME = "java.time.LocalTime";
  private static final String INSTANT = "java.time.Instant";
  private static final String DURATION = "java.time.Duration";
  private static final String ZONED_DATE_TIME = "java.time.ZonedDateTime";
  private static final String ZONE_OFFSET = "java.time.ZoneOffset";
  private static final String DAY_OF_WEEK = "java.time.DayOfWeek";
  private static final String CHRONO_UNIT = "java.time.temporal.ChronoUnit";

  /** the java.time types that are Temporal types */
  private static final Map<String, String> TEMPORAL_TYPES = Map.of(
      LOCAL_DATE, "Temporal.PlainDate",
      LOCAL_DATE_TIME, "Temporal.PlainDateTime",
      LOCAL_TIME, "Temporal.PlainTime",
      INSTANT, "Temporal.Instant",
      DURATION, "Temporal.Duration",
      ZONED_DATE_TIME, "Temporal.ZonedDateTime");

  /** ZoneId and ZoneOffset (a ZoneId), strings in transpiled code, see ZonedDateTimeHelper */
  private static final List<String> ZONE_TYPES = List.of("java.time.ZoneId", ZONE_OFFSET);

  /** the java.time types whose methods are translated, see JSweetCoCoChecker (0xEE009) */
  public static final Set<String> SUPPORTED_TYPES = Set.of(LOCAL_DATE, LOCAL_DATE_TIME, LOCAL_TIME, INSTANT, DURATION,
      ZONED_DATE_TIME, "java.time.ZoneId", ZONE_OFFSET, CHRONO_UNIT);

  /**
   * the translated methods that java.time declares in other types (interfaces of the supported types, DayOfWeek), see
   * JSweetCoCoChecker (0xEE009)
   */
  public static final Map<String, Set<String>> SUPPORTED_METHODS = Map.of(
      "java.time.chrono.ChronoZonedDateTime", Set.of("isBefore", "isAfter", "isEqual", "compareTo", "toInstant",
          "toEpochSecond", "toLocalDate", "toLocalTime", "toLocalDateTime"),
      "java.time.chrono.ChronoLocalDateTime", Set.of("isBefore", "isAfter", "isEqual", "compareTo", "toInstant",
          "toEpochSecond", "toLocalDate", "toLocalTime"),
      "java.time.chrono.ChronoLocalDate", Set.of("isBefore", "isAfter", "isEqual", "compareTo", "toEpochDay",
          "lengthOfYear", "isLeapYear"),
      DAY_OF_WEEK, Set.of("getValue"));

  /** a printed Temporal type or call, e.g. "Temporal.PlainDate" */
  private static final Pattern TEMPORAL_USE = Pattern.compile("\\bTemporal\\.");

  // ---------------------------------------------------------------------------------------------------------------
  // static methods and constants

  /** the Temporal functions for now, by java.time type; with a zone as argument for now(zone) */
  private static final Map<String, String> TEMPORAL_NOW = Map.of(
      LOCAL_DATE, "Temporal.Now.plainDateISO",
      LOCAL_DATE_TIME, "Temporal.Now.plainDateTimeISO",
      LOCAL_TIME, "Temporal.Now.plainTimeISO",
      ZONED_DATE_TIME, "Temporal.Now.zonedDateTimeISO");

  /** ofInstant(instant, zone) of the local types: the local part of the ZonedDateTime */
  private static final Map<String, String> OF_INSTANT_PART = Map.of(
      LOCAL_DATE, ".toPlainDate()",
      LOCAL_DATE_TIME, ".toPlainDateTime()",
      LOCAL_TIME, ".toPlainTime()",
      ZONED_DATE_TIME, "");

  /** the parse methods of LocalDateTimeHelper, which accept the formats of java.time only */
  private static final Map<String, String> PARSE_METHODS = Map.of(
      LOCAL_DATE, "parseDate",
      LOCAL_DATE_TIME, "parseDateTime",
      LOCAL_TIME, "parseTime");

  /** the static methods of ZoneId and ZoneOffset, see ZonedDateTimeHelper; of is zoneOf or offsetOf */
  private static final List<String> ZONE_FACTORIES = List.of(
      "systemDefault", "ofHours", "ofHoursMinutes", "ofHoursMinutesSeconds", "ofTotalSeconds");

  /** the constants of ZoneOffset as ids */
  private static final Map<String, String> ZONE_OFFSET_CONSTANTS = Map.of("UTC", "Z", "MIN", "-18:00", "MAX", "+18:00");

  /** the static methods of Duration, see DurationHelper */
  private static final List<String> DURATION_FACTORIES = List.of(
      "ofDays", "ofHours", "ofMinutes", "ofSeconds", "ofMillis", "ofNanos", "of", "between", "parse");

  // ---------------------------------------------------------------------------------------------------------------
  // instance methods

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

  /** the Temporal duration units of the plus and minus methods, e.g. plusDays */
  private static final Map<String, String> TEMPORAL_UNITS = Map.of(
      "Years", "years", "Months", "months", "Weeks", "weeks", "Days", "days", "Hours", "hours",
      "Minutes", "minutes", "Seconds", "seconds", "Nanos", "nanoseconds");

  /** the Temporal duration units of the plus and minus methods of Instant, e.g. plusMillis */
  private static final Map<String, String> INSTANT_UNITS = Map.of(
      "Seconds", "seconds", "Millis", "milliseconds", "Nanos", "nanoseconds");

  /** the methods of a Duration, see DurationHelper; plus and minus of an amount in a unit are plusUnits/minusUnits */
  private static final List<String> DURATION_METHODS = List.of(
      "plus", "minus", "plusDays", "plusHours", "plusMinutes", "plusSeconds", "plusMillis", "plusNanos", "minusDays",
      "minusHours", "minusMinutes", "minusSeconds", "minusMillis", "minusNanos", "multipliedBy", "dividedBy",
      "negated", "abs", "isZero", "isNegative", "isPositive", "getSeconds", "getNano", "toDays", "toHours", "toMinutes",
      "toSeconds", "toMillis", "toNanos", "toDaysPart", "toHoursPart", "toMinutesPart", "toSecondsPart", "toMillisPart",
      "toNanosPart", "truncatedTo", "compareTo", "toString");

  public JavaTimeAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    getTypeMirrorMappings().add(0, type -> TEMPORAL_TYPES.get(type.toString()));
    TEMPORAL_TYPES.forEach(this::addTypeMapping);
    addTypeMapping(DAY_OF_WEEK, "string");
    addTypeMapping(CHRONO_UNIT, "string");
    ZONE_TYPES.forEach(zoneType -> addTypeMapping(zoneType, "string"));
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

  // ---------------------------------------------------------------------------------------------------------------
  // method invocations

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }
    String targetType = invocation.getTargetType().toString();
    boolean substituted;
    if (substituteStringBuilding(invocation, targetType) || substituteUntypedToString(invocation)) {
      substituted = true;
    } else if (DAY_OF_WEEK.equals(targetType)) {
      substituted = substituteMethodInvocation_DayOfWeek(invocation);
    } else if (CHRONO_UNIT.equals(targetType)) {
      substituted = substituteMethodInvocation_ChronoUnit(invocation);
    } else if (ZONE_TYPES.contains(targetType)) {
      substituted = substituteMethodInvocation_Zone(invocation, targetType);
    } else if (DURATION.equals(targetType)) {
      substituted = substituteMethodInvocation_Duration(invocation);
    } else if (TEMPORAL_TYPES.containsKey(targetType)) {
      substituted = substituteUnitMethods(invocation) || substituteDurationArithmetic(invocation, targetType)
          || (INSTANT.equals(targetType) ? substituteMethodInvocation_Instant(invocation)
          : substituteMethodInvocation_Temporal(invocation, targetType));
    } else {
      substituted = false;
    }
    return substituted || super.substituteMethodInvocation(invocation);
  }

  /**
   * the methods of LocalDate, LocalDateTime, LocalTime and ZonedDateTime on their Temporal types; the ones without a
   * direct counterpart call LocalDateTimeHelper or ZonedDateTimeHelper
   */
  private boolean substituteMethodInvocation_Temporal(MethodInvocationElement invocation, String javaType) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    boolean zoned = ZONED_DATE_TIME.equals(javaType);
    useTemporal();

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
            printTarget(args.get(0)).print(".toPlainDateTime(").print(args.get(1)).print(")");
          } else {
            printLocalDateTimeHelper("ofDateTime", args.subList(0, args.size() - 1));
          }
          print(".toZonedDateTime(");
          printTimeZone(zone);
          print(")");
        } else if (LOCAL_DATE.equals(javaType)) {
          print("new Temporal.PlainDate(").printArgList(args).print(")");
        } else if (LOCAL_DATE_TIME.equals(javaType) && args.size() == 2) {
          // of(LocalDate, LocalTime)
          printTarget(args.get(0)).print(".toPlainDateTime(").print(args.get(1)).print(")");
        } else {
          printLocalDateTimeHelper(LOCAL_TIME.equals(javaType) ? "ofTime" : "ofDateTime", args);
        }
        return true;
      case "now":
        if (!args.isEmpty() && !isZone(args.get(0))) {
          // now(Clock)
          return false;
        }
        printMacroName(methodName);
        print(TEMPORAL_NOW.get(javaType) + "(");
        if (!args.isEmpty()) {
          printTimeZone(args.get(0));
        }
        print(")");
        return true;
      case "parse":
        if (args.size() != 1) {
          // parse(text, formatter)
          return false;
        }
        printMacroName(methodName);
        if (zoned) {
          printZonedDateTimeHelper("parse", args);
        } else {
          printLocalDateTimeHelper(PARSE_METHODS.get(javaType), args);
        }
        return true;
      case "ofEpochSecond":
      case "ofSecondOfDay":
      case "ofNanoOfDay":
      case "ofEpochDay":
        printMacroName(methodName);
        printLocalDateTimeHelper(methodName, args);
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

    String property = TEMPORAL_GETTERS.get(methodName);
    if (property != null) {
      printMacroName(methodName);
      printTarget(target).print("." + property);
      return true;
    }
    if (substitutePlusMinus(methodName, target, args, TEMPORAL_UNITS)
        || substituteComparison(methodName, target, args, javaType)) {
      return true;
    }
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
        // ZonedDateTime.toInstant(), LocalDateTime.toInstant(offset)
        printMacroName(methodName);
        printTarget(target);
        if (!args.isEmpty()) {
          print(".toZonedDateTime(");
          printTimeZone(args.get(0));
          print(")");
        }
        print(".toInstant()");
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
        printZonedDateTimeHelper(methodName, withTarget(target, args));
        return true;
      case "toString":
      case "getNano":
      case "getDayOfWeek":
      case "getMonth":
      case "toSecondOfDay":
      case "toNanoOfDay":
      case "toEpochDay":
        printMacroName(methodName);
        printLocalDateTimeHelper(methodName, List.of(target));
        return true;
      case "withDayOfYear":
      case "withNano":
        printMacroName(methodName);
        printLocalDateTimeHelper(methodName, withTarget(target, args));
        return true;
      case "toEpochSecond":
        // ZonedDateTime: no argument; LocalDateTime: (offset); LocalDate: (time, offset); LocalTime: (date, offset)
        printMacroName(methodName);
        useLocalDateTimeHelper();
        print("LocalDateTimeHelper.toEpochSecond(");
        if (args.size() == 2) {
          boolean isDate = LOCAL_DATE.equals(javaType);
          printTarget(isDate ? target : args.get(0)).print(".toPlainDateTime(").print(isDate ? args.get(0) : target)
              .print("), ").print(args.get(1));
        } else {
          print(target).print(args.isEmpty() ? "" : ", ").printArgList(args);
        }
        print(")");
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
          printLocalDateTimeHelper("ofTime", args);
        }
        print(")");
        return true;
    }
    return false;
  }

  /** the methods of Instant on Temporal.Instant; the ones without a direct counterpart call LocalDateTimeHelper */
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
        printLocalDateTimeHelper("instantOfEpochSecond", args);
        return true;
    }
    if (target == null) {
      return false;
    }

    if (substitutePlusMinus(methodName, target, args, INSTANT_UNITS)
        || substituteComparison(methodName, target, args, INSTANT)) {
      return true;
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
      case "toString":
      case "getNano":
      case "getEpochSecond":
        printMacroName(methodName);
        printLocalDateTimeHelper(methodName, List.of(target));
        return true;
    }
    return false;
  }

  /** plusDays(n), minusMillis(n), ...: add or subtract n of the unit, see TEMPORAL_UNITS and INSTANT_UNITS */
  private boolean substitutePlusMinus(String methodName, ExtendedElement target, List<ExtendedElement> args,
      Map<String, String> units) {
    boolean isPlus = methodName.startsWith("plus");
    if (args.size() != 1 || !isPlus && !methodName.startsWith("minus")) {
      return false;
    }
    String unit = units.get(methodName.substring(isPlus ? "plus".length() : "minus".length()));
    if (unit == null) {
      return false;
    }
    printMacroName(methodName);
    printTarget(target).print(isPlus ? ".add({" : ".subtract({").print(unit + ": ").print(args.get(0)).print("})");
    return true;
  }

  /**
   * equals (only to a value of the same type, see JavaUtilUtils), isAfter, isBefore, isEqual (by the instant of a
   * ZonedDateTime) and compareTo (a ZonedDateTime by the instant, then the local date-time, then the zone, like Java)
   */
  private boolean substituteComparison(String methodName, ExtendedElement target, List<ExtendedElement> args,
      String javaType) {
    String temporalType = TEMPORAL_TYPES.get(javaType);
    switch (methodName) {
      case "equals":
        printMacroName(methodName);
        printEquals(target, args.get(0));
        return true;
      case "isAfter":
      case "isBefore":
      case "isEqual":
        printMacroName(methodName);
        String operator = "isAfter".equals(methodName) ? " > 0" : "isBefore".equals(methodName) ? " < 0" : " === 0";
        print("(" + temporalType + ".compare(").print(target).print(", ").print(args.get(0))
            .print(")" + operator + ")");
        return true;
      case "compareTo":
        printMacroName(methodName);
        if (ZONED_DATE_TIME.equals(javaType)) {
          printZonedDateTimeHelper("compareTo", List.of(target, args.get(0)));
        } else {
          print(temporalType + ".compare(").print(target).print(", ").print(args.get(0)).print(")");
        }
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
      printZonedDateTimeHelper(ZONE_OFFSET.equals(targetType) ? "offsetOf" : "zoneOf", args);
      return true;
    }
    if (ZONE_FACTORIES.contains(methodName) && isStaticCall(invocation)) {
      printMacroName(methodName);
      printZonedDateTimeHelper(methodName, args);
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
      case "equals":
        printMacroName(methodName);
        print("(").print(target).print(" === ").print(args.get(0)).print(")");
        return true;
      case "normalized":
      case "getTotalSeconds":
        printMacroName(methodName);
        printZonedDateTimeHelper(methodName, List.of(target));
        return true;
    }
    return false;
  }

  /** the methods of DayOfWeek on the name of the constant */
  private boolean substituteMethodInvocation_DayOfWeek(MethodInvocationElement invocation) {
    switch (invocation.getMethodName()) {
      case "name":
      case "toString":
        print(invocation.getTargetExpression());
        return true;
      case "getValue":
        useLocalDateTimeHelper();
        print("((LocalDateTimeHelper.DAYS_OF_WEEK.indexOf(").print(invocation.getTargetExpression())
            .print(") + 6) % 7 + 1)");
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
        printDurationHelper("unitsBetween", withTarget(target, args));
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
   * plus(Duration) and minus(Duration) of a LocalDateTime, LocalTime, Instant or ZonedDateTime (a LocalDate has no
   * time, Java rejects a Duration); plus(amount, unit) and minus(amount, unit) of all of them
   */
  private boolean substituteDurationArithmetic(MethodInvocationElement invocation, String targetType) {
    String methodName = invocation.getMethodName();
    List<ExtendedElement> args = invocation.getArguments();
    ExtendedElement target = invocation.getTargetExpression();
    if (target == null || !("plus".equals(methodName) || "minus".equals(methodName))) {
      return false;
    }
    if (args.size() == 1 && args.get(0).getType() != null && DURATION.equals(args.get(0).getType().toString())
        && !LOCAL_DATE.equals(targetType)) {
      printMacroName(methodName);
      useTemporal();
      printTarget(target).print("plus".equals(methodName) ? ".add(" : ".subtract(").print(args.get(0)).print(")");
      return true;
    }
    if (args.size() == 2) {
      printMacroName(methodName);
      printDurationHelper(methodName + "Units", withTarget(target, args));
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
    if (target != null && "equals".equals(methodName)) {
      printMacroName(methodName);
      printEquals(target, args.get(0));
      return true;
    }
    if (target == null || !DURATION_METHODS.contains(methodName)) {
      return false;
    }
    printMacroName(methodName);
    boolean amountInUnit = ("plus".equals(methodName) || "minus".equals(methodName)) && args.size() == 2;
    printDurationHelper(amountInUnit ? methodName + "Units" : methodName, withTarget(target, args));
    return true;
  }

  // ---------------------------------------------------------------------------------------------------------------
  // strings: string building, toString of values of an unknown type

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

  /** toString() of a value of an unknown type (see isUntyped): a java.time value in the format of java.time */
  private boolean substituteUntypedToString(MethodInvocationElement invocation) {
    if (!"toString".equals(invocation.getMethodName()) || invocation.getArgumentCount() != 0
        || invocation.getTargetExpression() == null || !isUntyped(invocation.getTargetExpression())) {
      return false;
    }
    printMacroName("toString");
    printLocalDateTimeHelper("toJavaString", List.of(invocation.getTargetExpression()));
    return true;
  }

  /**
   * whether the string of the element differs from the one JS makes: a java.time value (the format of java.time;
   * Temporal values refuse the + operator), a ChronoUnit (its display name, e.g. "Days") or a value of an unknown type
   * (see isUntyped)
   */
  public static boolean hasJavaString(ExtendedElement element) {
    return isTemporal(element) || isChronoUnit(element) || isUntyped(element);
  }

  /** the toString of the element like in Java, see hasJavaString */
  private void printJavaString(ExtendedElement element) {
    if (isTemporal(element)) {
      useTemporal();
      printLocalDateTimeHelper("toString", List.of(element));
    } else if (isChronoUnit(element)) {
      printDurationHelper("unitToString", List.of(element));
    } else {
      printLocalDateTimeHelper("toJavaString", List.of(element));
    }
  }

  private void printConcatOperand(ExtendedElement operand) {
    if (hasJavaString(operand)) {
      printJavaString(operand);
    } else {
      print(operand);
    }
  }

  // ---------------------------------------------------------------------------------------------------------------
  // constants and case labels

  /** a case label of DayOfWeek or ChronoUnit is the name of the constant, like the constants themselves */
  @Override
  public boolean substituteCaseStatementPattern(CaseElement caseStatement, ExtendedElement pattern) {
    if (pattern.getType() != null && (DAY_OF_WEEK.equals(pattern.getType().toString())
        || CHRONO_UNIT.equals(pattern.getType().toString()))) {
      print("\"" + ExtendedElementFactory.toTree(pattern) + "\"");
      return true;
    }
    return super.substituteCaseStatementPattern(caseStatement, pattern);
  }

  @Override
  public boolean substituteVariableAccess(VariableAccessElement variableAccess) {
    VariableElement variable = variableAccess.getVariable();
    Element owner = variable == null ? null : variable.getEnclosingElement();
    String constant = variable == null ? null : variable.getSimpleName().toString();
    if (owner != null && (DAY_OF_WEEK.equals(owner.toString()) || CHRONO_UNIT.equals(owner.toString()))
        && variable.getKind() == ElementKind.ENUM_CONSTANT) {
      print("\"" + constant + "\"");
      return true;
    }
    if (owner != null && ZONE_OFFSET.equals(owner.toString()) && ZONE_OFFSET_CONSTANTS.containsKey(constant)) {
      print("\"" + ZONE_OFFSET_CONSTANTS.get(constant) + "\"");
      return true;
    }
    if (owner != null && DURATION.equals(owner.toString()) && "ZERO".equals(constant)) {
      useDurationHelper();
      print("DurationHelper.ZERO");
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }

  // ---------------------------------------------------------------------------------------------------------------
  // types of elements

  /** whether the element is a java.time value, see TEMPORAL_TYPES */
  private static boolean isTemporal(ExtendedElement element) {
    return element.getType() != null && TEMPORAL_TYPES.containsKey(element.getType().toString());
  }

  private static boolean isChronoUnit(ExtendedElement element) {
    return element.getType() != null && CHRONO_UNIT.equals(element.getType().toString());
  }

  private static boolean isZone(ExtendedElement element) {
    return element.getType() != null && ZONE_TYPES.contains(element.getType().toString());
  }

  /**
   * whether the static type of the element does not tell whether it is a java.time value: Object, a type parameter or
   * an interface of java.time.temporal or java.time.chrono
   */
  private static boolean isUntyped(ExtendedElement element) {
    TypeMirror type = element.getType();
    if (type == null) {
      return false;
    }
    if (type.getKind() == TypeKind.TYPEVAR) {
      return true;
    }
    String name = type instanceof DeclaredType ? ((DeclaredType) type).asElement().toString() : type.toString();
    boolean timeInterface = name.startsWith("java.time.temporal.") || name.startsWith("java.time.chrono.");
    return "java.lang.Object".equals(name) || timeInterface && !CHRONO_UNIT.equals(name);
  }

  private boolean isStaticCall(MethodInvocationElement invocation) {
    return invocation.getMethod() != null && invocation.getMethod().getModifiers().contains(Modifier.STATIC);
  }

  // ---------------------------------------------------------------------------------------------------------------
  // printing

  private PrinterAdapter printTarget(ExtendedElement target) {
    return print("(").print(target).print(")");
  }

  private static List<ExtendedElement> withTarget(ExtendedElement target, List<ExtendedElement> args) {
    List<ExtendedElement> targetAndArgs = new ArrayList<>();
    targetAndArgs.add(target);
    targetAndArgs.addAll(args);
    return targetAndArgs;
  }

  /** equals of java.time values: only to a value of the same type, like Objects.equals (see JavaBasicsAdapter) */
  private void printEquals(ExtendedElement target, ExtendedElement other) {
    getPrinter().useModule(new ModuleImportDescriptor("JavaUtilUtils", getLibraryLocation()));
    print("JavaUtilUtils.equalsSync(").print(target).print(", ").print(other).print(")");
  }

  /** the Temporal time zone of a ZoneId */
  private void printTimeZone(ExtendedElement zone) {
    printZonedDateTimeHelper("tz", List.of(zone));
  }

  private void printLocalDateTimeHelper(String method, List<ExtendedElement> args) {
    useLocalDateTimeHelper();
    print("LocalDateTimeHelper." + method + "(").printArgList(args).print(")");
  }

  private void printDurationHelper(String method, List<ExtendedElement> args) {
    useDurationHelper();
    print("DurationHelper." + method + "(").printArgList(args).print(")");
  }

  private void printZonedDateTimeHelper(String method, List<ExtendedElement> args) {
    getPrinter().useModule(new ModuleImportDescriptor("ZonedDateTimeHelper", getLibraryLocation()));
    print("ZonedDateTimeHelper." + method + "(").printArgList(args).print(")");
  }

  private void useTemporal() {
    getPrinter().useModule(new ModuleImportDescriptor(TEMPORAL, getLibraryLocation()));
  }

  private void useLocalDateTimeHelper() {
    getPrinter().useModule(new ModuleImportDescriptor("LocalDateTimeHelper", getLibraryLocation()));
  }

  private void useDurationHelper() {
    getPrinter().useModule(new ModuleImportDescriptor("DurationHelper", getLibraryLocation()));
  }
}
