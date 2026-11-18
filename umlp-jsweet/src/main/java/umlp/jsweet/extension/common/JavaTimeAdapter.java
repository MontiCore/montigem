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
 * This adapter is responsible to enable the usage of java.time classes: LocalDate, LocalDateTime, LocalTime, Instant
 * and Duration become the Temporal types of @umlp/commonj2ts (see TEMPORAL_TYPES), the constants of DayOfWeek and
 * ChronoUnit strings. java.util.Date is not supported (JSweetCoCoChecker, 0xEE013).
 */
public class JavaTimeAdapter extends AbstractAdapter {

  public static final String TEMPORAL = "Temporal";

  public static final Map<String, String> TEMPORAL_TYPES = Map.of(
      "java.time.LocalDate", "Temporal.PlainDate",
      "java.time.LocalDateTime", "Temporal.PlainDateTime",
      "java.time.LocalTime", "Temporal.PlainTime",
      "java.time.Instant", "Temporal.Instant",
      "java.time.Duration", "Temporal.Duration");

  public static final String CHRONO_UNIT = "java.time.temporal.ChronoUnit";

  /** the static methods of Duration, see DurationHelper */
  private static final List<String> DURATION_FACTORIES = List.of(
      "ofDays", "ofHours", "ofMinutes", "ofSeconds", "ofMillis", "ofNanos", "of", "between", "parse");

  /** the methods of a Duration, see DurationHelper; plus and minus of an amount in a unit are plusUnits/minusUnits */
  private static final List<String> DURATION_METHODS = List.of(
      "plus", "minus", "plusDays", "plusHours", "plusMinutes", "plusSeconds", "plusMillis", "plusNanos", "minusDays",
      "minusHours", "minusMinutes", "minusSeconds", "minusMillis", "minusNanos", "multipliedBy", "dividedBy",
      "negated", "abs", "isZero", "isNegative", "getSeconds", "getNano", "toDays", "toHours", "toMinutes", "toSeconds",
      "toMillis", "toNanos", "compareTo", "equals", "toString");

  /** the Temporal duration units of the plus and minus methods of Instant, e.g. plusMillis */
  private static final Map<String, String> INSTANT_UNITS = Map.of(
      "Seconds", "seconds", "Millis", "milliseconds", "Nanos", "nanoseconds");

  /** a printed Temporal type or call, e.g. "Temporal.PlainDate" */
  private static final Pattern TEMPORAL_USE = Pattern.compile("\\bTemporal\\.");

  /** the Temporal expressions for now, by java.time type */
  private static final Map<String, String> TEMPORAL_NOW = Map.of(
      "java.time.LocalDate", "Temporal.Now.plainDateISO()",
      "java.time.LocalDateTime", "Temporal.Now.plainDateTimeISO()",
      "java.time.LocalTime", "Temporal.Now.plainTimeISO()");

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
    if (CHRONO_UNIT.equals(targetType) && "between".equals(invocation.getMethodName())) {
      // unit.between(start, end)
      printMacroName("between");
      List<ExtendedElement> args = new ArrayList<>();
      args.add(invocation.getTargetExpression());
      args.addAll(invocation.getArguments());
      printDurationHelper("unitsBetween", args);
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

    // static methods
    switch (methodName) {
      case "of":
        printMacroName(methodName);
        if ("java.time.LocalDate".equals(javaType)) {
          print("new Temporal.PlainDate(").printArgList(args).print(")");
        } else if ("java.time.LocalDateTime".equals(javaType) && args.size() == 2) {
          // of(LocalDate, LocalTime)
          print("(").print(args.get(0)).print(").toPlainDateTime(").print(args.get(1)).print(")");
        } else {
          printHelper("java.time.LocalTime".equals(javaType) ? "ofTime" : "ofDateTime", args);
        }
        return true;
      case "now":
        printMacroName(methodName);
        print(TEMPORAL_NOW.get(javaType));
        return true;
      case "parse":
        printMacroName(methodName);
        print(temporalType + ".from(").print(args.get(0)).print(")");
        return true;
      case "ofEpochSecond":
        // the offset (third argument) is taken as UTC
        printMacroName(methodName);
        printHelper("ofEpochSecond", args.subList(0, Math.min(args.size(), 2)));
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
        print(temporalType + ".compare(").print(target).print(", ").print(args.get(0)).print(")");
        return true;
      case "toString":
      case "getNano":
      case "getDayOfWeek":
      case "getMonth":
      case "toEpochSecond":
        // toEpochSecond: the offset (argument) is taken as UTC
        printMacroName(methodName);
        printHelper(methodName, List.of(target));
        return true;
      case "withDayOfYear":
        printMacroName(methodName);
        printHelper(methodName, List.of(target, args.get(0)));
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
        printTarget(target).print(".toPlainDateTime()");
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
        && "java.lang.String".equals(binaryOperator.getType().toString()) && (isTemporal(left) || isTemporal(right))) {
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
    if ("+=".equals(assignment.getOperator()) && isTemporal(assignment.getValue()) && assignment.getType() != null
        && "java.lang.String".equals(assignment.getType().toString())) {
      // the target from the tree: getTarget() supports variables only, not e.g. array elements
      getPrinter().print(((CompoundAssignmentTree) ExtendedElementFactory.toTree(assignment)).getVariable())
          .print(" += ");
      printHelper("toString", List.of(assignment.getValue()));
      return true;
    }
    return super.substituteAssignmentWithOperator(assignment);
  }

  /**
   * StringBuilder.append and String.valueOf with a java.time value, see substituteBinaryOperator; the value is
   * evaluated outside the lambda, as it may contain an await
   */
  private boolean substituteStringBuilding(MethodInvocationElement invocation, String targetType) {
    if (invocation.getArgumentCount() != 1 || !isTemporal(invocation.getArgument(0))) {
      return false;
    }
    String methodName = invocation.getMethodName();
    if ("append".equals(methodName)
        && ("java.lang.StringBuilder".equals(targetType) || "java.lang.StringBuffer".equals(targetType))) {
      printMacroName(methodName);
      print("((sb, s) => { sb.str += s; return sb; })(").print(invocation.getTargetExpression()).print(", ");
      printHelper("toString", invocation.getArguments());
      print(")");
      return true;
    }
    if ("valueOf".equals(methodName) && "java.lang.String".equals(targetType)) {
      // String(null) is "null", like Java
      printMacroName(methodName);
      print("String(");
      printHelper("toString", invocation.getArguments());
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
    if (isTemporal(operand)) {
      useTemporal();
      printHelper("toString", List.of(operand));
    } else {
      print(operand);
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
    if (owner != null && "java.time.Duration".equals(owner.toString())
        && "ZERO".equals(variable.getSimpleName().toString())) {
      useDurationHelper();
      print("DurationHelper.ZERO");
      return true;
    }
    return super.substituteVariableAccess(variableAccess);
  }
}
