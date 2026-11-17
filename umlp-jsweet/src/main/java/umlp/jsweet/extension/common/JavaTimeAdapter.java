/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.BinaryOperatorElement;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.VariableAccessElement;
import umlp.jsweet.extension.AbstractAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This adapter is responsible to enable the usage of java.time and Date classes: LocalDate, LocalDateTime and
 * LocalTime become the Temporal types of @umlp/commonj2ts (see TEMPORAL_TYPES), java.util.Date stays a Date.
 */
public class JavaTimeAdapter extends AbstractAdapter {

  public static final String TEMPORAL = "Temporal";

  public static final Map<String, String> TEMPORAL_TYPES = Map.of(
      "java.time.LocalDate", "Temporal.PlainDate",
      "java.time.LocalDateTime", "Temporal.PlainDateTime",
      "java.time.LocalTime", "Temporal.PlainTime");

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

  public JavaTimeAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeDateTypeMapping());
    TEMPORAL_TYPES.forEach(this::addTypeMapping);
    addTypeMapping("java.time.DayOfWeek", "string");
  }

  public Function<TypeMirror, String> computeDateTypeMapping() {
    return (TypeMirror type) -> {
      String temporalType = TEMPORAL_TYPES.get(type.toString());
      if (temporalType != null) {
        return temporalType;
      }
      if ("java.util.Date".equals(type.toString())) {
        return "Date";
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
    if (TEMPORAL_TYPES.containsKey(targetType) && substituteMethodInvocation_Temporal(invocation, targetType)) {
      return true;
    }
    if ("java.util.Date".equals(targetType) && substituteMethodInvocation_Date(invocation)) {
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
      printTarget(target).print(".with({" + field + ": ").print(args.get(0)).print("})");
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

  private boolean isTemporal(ExtendedElement element) {
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

  private boolean substituteMethodInvocation_Date(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    String targetType = invocation.getTargetType().toString();
    boolean isLocalDateTime = targetType.contains("LocalDateTime");
    boolean isLocalDate = targetType.equals("java.time.LocalDate");

    // ==========================================================================================
    // FACTORY METHOD: LocalDateTime.of(...)
    // ==========================================================================================
    if ("of".equals(methodName)) {
      printMacroName(methodName);

      // Use UTC to create the date so browser timezone doesn't shift the values.
      this.print("new Date(Date.UTC(").println();

      List<ExtendedElement> args = invocation.getArguments();
      if (args.size() >= 3) {
        this.print(args.get(0)).print(", ").println();       // Year
        this.print(args.get(1)).print(" - 1, ").println();   // Subtract 1 because Java months are 1-12, JS are 0-11.
        this.print(args.get(2)).println();                   // Day

        // Handle optional arguments for Hour, Minute, and Second, defaulting to 0.
        this.print(", ").print(args.size() >= 4 ? args.get(3).toString() : "0").println();
        this.print(", ").print(args.size() >= 5 ? args.get(4).toString() : "0").println();
        this.print(", ").print(args.size() >= 6 ? args.get(5).toString() : "0").println();

        // Convert nanoseconds (Java) to milliseconds (JS), losing precision.
        if (args.size() >= 7) {
          this.print(", Math.floor(").print(args.get(6)).print(" / 1000000)").println();
        } else {
          this.print(", 0").println();
        }
      }
      this.print("))").println();
      return true;
    }

    // ==========================================================================================
    // plus(Duration) / minus(Duration)
    // ==========================================================================================
    if (invocation.getArguments().size() == 1 &&
        ("plus".equals(methodName) || "minus".equals(methodName))) {
      String targetOp = "plus".equals(methodName) ? "addTo" : "subtractFrom";
      printMacroName(methodName);
      this.print(invocation.getArguments().get(0)).print("." + targetOp + "(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(")").println();
      return true;
    }

    // ==========================================================================================
    // plusDays, minusHours, plusMonths, etc.
    // ==========================================================================================
    boolean isPlus = methodName.startsWith("plus");
    boolean isMinus = methodName.startsWith("minus");

    if (isPlus || isMinus) {
      String baseName = methodName.replace("plus", "").replace("minus", "");

      String jsGetter = null;
      String jsSetter = null;
      long factor = 1;

      switch (baseName) {
        case "Years":   jsGetter = "getUTCFullYear"; jsSetter = "setUTCFullYear"; break;
        case "Months":  jsGetter = "getUTCMonth";    jsSetter = "setUTCMonth";    break;
        case "Weeks":   jsGetter = "getUTCDate";     jsSetter = "setUTCDate"; factor = 7; break;
        case "Days":    jsGetter = "getUTCDate";     jsSetter = "setUTCDate";     break;
        case "Hours":   jsGetter = "getUTCHours";    jsSetter = "setUTCHours";    break;
        case "Minutes": jsGetter = "getUTCMinutes";  jsSetter = "setUTCMinutes";  break;
        case "Seconds": jsGetter = "getUTCSeconds";  jsSetter = "setUTCSeconds";  break;
        case "Nanos":   jsGetter = "getUTCMilliseconds"; jsSetter = "setUTCMilliseconds"; break;
      }

      printMacroName(methodName);

      // The amount is passed in as parameter n instead of being printed into the arrow function:
      // it may contain an await (e.g. an association navigation), which is only allowed in the
      // enclosing async function, not in the synchronous arrow function.
      this.print("((d, n) => { ").println();
      this.print("let res = new Date(d.getTime()); ").println();

      if ("Months".equals(baseName) || "Years".equals(baseName)) {
        this.print("let checkDay = res.getUTCDate(); ").println();
        this.print("res." + jsSetter + "(res." + jsGetter + "()").println();
        this.print(isPlus ? " + n" : " - n").println();
        this.print("); ").println();
        this.print("if (res.getUTCDate() !== checkDay) { res.setUTCDate(0); } ").println();
      } else if ("Nanos".equals(baseName)) {
        this.print("res." + jsSetter + "(res." + jsGetter + "()").println();
        this.print(isPlus ? " + Math.floor(n / 1000000)" : " - Math.floor(n / 1000000)").println();
        this.print("); ").println();
      } else {
        this.print("res." + jsSetter + "(res." + jsGetter + "()").println();
        this.print(isPlus ? " + (n" : " - (n").println();
        if (factor > 1) this.print(" * " + factor).println();
        this.print(")); ").println();
      }

      this.print("return res; })(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(", ").print(invocation.getArguments().get(0)).println();
      this.print(")").println();
      return true;
    }

    // ==========================================================================================
    // equals
    // ==========================================================================================
    if ("equals".equals(methodName)) {
      printMacroName(methodName);
      this.print("((a, b) => b instanceof Date && a.getTime() === b.getTime())(")
          .print(invocation.getTargetExpression()).print(", ")
          .printArgList(invocation.getArguments()).print(")");
      return true;
    }

    // ==========================================================================================
    // isAfter, isBefore, isEqual
    // ==========================================================================================
    if ("isAfter".equals(methodName) || "isBefore".equals(methodName) || "isEqual".equals(methodName)) {
      printMacroName(methodName);
      this.print("(").println();
      printDateTarget(invocation, isLocalDateTime);
      this.print(".getTime()").println();

      if ("isAfter".equals(methodName))       this.print(" > ").println();
      else if ("isBefore".equals(methodName)) this.print(" < ").println();
      else                                    this.print(" === ").println();

      if (isLocalDateTime) this.print("new Date(").println();
      this.print(invocation.getArguments().get(0)).println();
      if (isLocalDateTime) this.print(")").println();
      this.print(".getTime())").println();
      return true;
    }

    // ==========================================================================================
    // getHour, getMinute, etc.
    // ==========================================================================================
    String jsSimpleMethod = switch (methodName) {
      case "getHour", "getHours" -> "getUTCHours";
      case "getMinute", "getMinutes" -> "getUTCMinutes";
      case "getSecond", "getSeconds" -> "getUTCSeconds";
      case "getYear" -> "getUTCFullYear";
      case "getDayOfMonth", "getDate" -> "getUTCDate";
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
        this.print(".getUTCMonth() + 1)").println();
        return true;

      case "getMonth":
        printMacroName(methodName);
        this.print("[\"JANUARY\", \"FEBRUARY\", \"MARCH\", \"APRIL\", \"MAY\", \"JUNE\", \"JULY\", \"AUGUST\", \"SEPTEMBER\", \"OCTOBER\", \"NOVEMBER\", \"DECEMBER\"][").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(".getMonth()]").println();
        return true;

      case "getDayOfWeek":
        printMacroName(methodName);
        print("LocalDateTimeHelper.getDayOfWeek(")
            .print(invocation.getTargetExpression()).print(")");
        getPrinter().useModule(
            new ModuleImportDescriptor("LocalDateTimeHelper", "@umlp/commonj2ts"));
        return true;

      case "getDayOfYear":
        printMacroName(methodName);
        this.print("((d) => { ").println();
        this.print("let utcNow = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()); ").println();
        this.print("let utcStart = Date.UTC(d.getUTCFullYear(), 0, 0); ").println();
        this.print("return Math.floor((utcNow - utcStart) / 86400000); })(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(")").println();
        return true;

      case "getNano":
        printMacroName(methodName);
        this.print("(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(".getUTCMilliseconds() * 1000000)").println();
        return true;

      case "toString":
        printMacroName(methodName);
        this.print("((d) => { ").println();
        this.print("if(d == null || d === undefined){").println();
        this.print("  return null;").println();
        this.print("}").println();
        this.print("  const pad = (n) => ('0' + n).slice(-2); ").println();
        this.print("  let s = d.getUTCFullYear() + '-' + ").println();
        this.print("         pad(d.getUTCMonth() + 1) + '-' + ").println();
        this.print("         pad(d.getUTCDate()); ").println();

        if (!targetType.equals("java.time.LocalDate")) {
          this.print("  s += 'T' + ").println();
          this.print("         pad(d.getUTCHours()) + ':' + ").println();
          this.print("         pad(d.getUTCMinutes()); ").println();
          this.print("  if (d.getUTCSeconds() > 0 || d.getUTCMilliseconds() > 0) { ").println();
          this.print("    s += ':' + pad(d.getUTCSeconds()); ").println();
          this.print("  } ").println();
          this.print("  if (d.getUTCMilliseconds() > 0) { ").println();
          this.print("    s += '.' + ('00' + d.getUTCMilliseconds()).slice(-3); ").println();
          this.print("  } ").println();
        }
        this.print("  return s; ").println();
        this.print("})(").println();
        printDateTarget(invocation, isLocalDateTime);
        this.print(")").println();
        return true;

      case "now":
        this.print("((() => { ").println();
        this.print("  const now = new Date(); ").println();
        this.print("  return new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()");
        if (!isLocalDate) {
          this.print(", now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds()");
        }
        this.print(")); ").println();
        this.print("})())").println();
        return true;

      case "parse":
        printMacroName(methodName);
        this.print("new Date(");
        this.print(invocation.getArguments().get(0));
        this.print(" + 'Z')");
        return true;

      case "compareTo":
        print("(").print(invocation.getTargetExpression()).print(".getTime() - ")
            .printArgList(invocation.getArguments()).print(".getTime())");
        return true;

      case "toEpochSecond":
        print(invocation.getTargetExpression()).print(".getTime() / 1000");
        return true;

      case "ofEpochSecond":
        print("new Date((").print(invocation.getArguments().get(0)).print(") * 1000)");
        return true;

      case "toLocalDate":
        print("((d) => new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate())))(")
            .print(invocation.getTargetExpression()).print(")");
        return true;

      case "toLocalTime":
        print(invocation.getTargetExpression());
        return true;

      case "plusYears":
      case "plusMonths":
      case "plusWeeks":
      case "plusDays":
      case "plusHours":
      case "plusMinutes":
      case "plusSeconds":
      case "minusYears":
      case "minusMonths":
      case "minusWeeks":
      case "minusDays":
      case "minusHours":
      case "minusMinutes":
      case "minusSeconds":
      case "atStartOfDay":
      case "withYear":
      case "withMonth":
      case "withDayOfMonth":
      case "withDayOfYear":
      case "withHour":
      case "withMinute":
      case "withSecond":
        print("LocalDateTimeHelper." + invocation.getMethodName() + "(")
            .print(invocation.getTargetExpression()).print(", ")
            .printArgList(invocation.getArguments()).print(")");
        getPrinter().useModule(
            new ModuleImportDescriptor("LocalDateTimeHelper", "@umlp/commonj2ts"));
        return true;
    }
    return false;
  }
}
