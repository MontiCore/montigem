package dashboardmodule.commands;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.json.JsonElement;
import de.monticore.symboltable.serialization.json.JsonObject;
import de.monticore.values.MCValue;
import jsweet.lang.Erased;
import mc.fenix.table.gemtabletypes.GemFilterableTableColumn;
import mc.fenix.table.gemtabletypes.GemFilterableTableColumnBuilder;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import mc.fenix.table.gemtabletypes.GemFilterableTableDataBuilder;
import mc.fenix.table.gemtabletypes.GemFilterableTableFilterBuilder;
import mgscript.MGScriptInterpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes the data of a filterable table from separate scripts: the data script returns the
 * {@link GemFilterableTableData}, the optional filter of a column is a predicate over the values of the column
 * (only rows whose values are accepted by all filters are kept), and the optional render function of a column computes
 * the displayed text of a value.
 * <p>
 * Filters and render functions are expressions over the value {@code v}, e.g., {@code v >= 500} or
 * {@code Math.round(v) + " %"}. A filter may omit {@code v} before an operator or a method call, e.g., {@code >= 500}
 * or {@code .startsWith("A")}; on texts, {@code == "Alice"} and {@code != "Alice"} compare with {@code equals}.
 * Dates and date-times can be compared to typed values or quoted ISO values, e.g., {@code >= "2026-01-01"}.
 * A script containing {@code return} is used as it is and has to return the lambda, e.g.,
 * {@code List<String> roles = ["MID"]; return (String v) -> roles.contains(v);}.
 */
@Erased
public class FilterableTableInterpreter {

  /**
   * @param filters filter scripts by column header, blank scripts are ignored
   * @param renders render scripts by column header, blank scripts are ignored
   * @throws IllegalArgumentException with the reason, if a script fails or returns an unexpected value
   */
  public static GemFilterableTableData interpret(String data, Map<String, String> filters, Map<String, String> renders) {
    // a function returned by a script is applied before the next script is interpreted, which resets the interpreter
    synchronized (MGScriptInterpreter.class) {
      InterpreterWithErrorHandling.ValueOrError result = InterpreterWithErrorHandling.interpretString(data);
      if (result.isError()) {
        throw new IllegalArgumentException(result.getFindings());
      }
      if (!(result.getValue().asNativeObject() instanceof GemFilterableTableData table)) {
        throw new IllegalArgumentException("The data script has to return a GemFilterableTableData, but returned "
            + result.getValue().printType() + ".");
      }
      int rowCount = checkColumns(table);
      checkScriptColumns(table, filters);
      checkScriptColumns(table, renders);

      List<Integer> rows = new ArrayList<>();
      for (int row = 0; row < rowCount; row++) {
        rows.add(row);
      }
      for (GemFilterableTableColumn column : table.getColumnsList()) {
        String filter = filters.get(column.getHeader());
        if (filter != null && !filter.isBlank()) {
          MCValue predicate = interpretFunction(toScript(filter, "filter", column), "filter", column);
          rows.removeIf(row -> !accepts(predicate, valuesOf(column).get(row), column));
        }
      }

      List<GemFilterableTableColumn> columns = new ArrayList<>();
      for (GemFilterableTableColumn column : table.getColumnsList()) {
        String render = renders.get(column.getHeader());
        MCValue renderer = render == null || render.isBlank() ? null
            : interpretFunction(toScript(render, "render", column), "render", column);
        List<String> rendered = new ArrayList<>();
        for (int row : rows) {
          Object value = valuesOf(column).get(row);
          rendered.add(renderer == null ? renderByDefault(value) : render(renderer, value, column));
        }
        GemFilterableTableColumn selected = new GemFilterableTableColumnBuilder()
            .header(column.getHeader())
            .numbers(select(column.getNumbersList(), rows))
            .texts(select(column.getTextsList(), rows))
            .booleans(select(column.getBooleansList(), rows))
            .dates(select(column.getDatesList(), rows))
            .dateTimes(select(column.getDateTimesList(), rows))
            .rendered(rendered)
            .build().get();
        String filter = filters.get(column.getHeader());
        if (filter != null && !filter.isBlank()) {
          new GemFilterableTableFilterBuilder().column(selected).filter(filter.trim()).build().get();
        }
        columns.add(selected);
      }
      return new GemFilterableTableDataBuilder().columns(columns).build().get();
    }
  }

  /**
   * @param columnScripts JSON array of {column, filter, render} objects, e.g., sent by the frontend
   * @return the scripts of the kind (filter or render) by column header
   */
  public static Map<String, String> scriptsOf(String columnScripts, String kind) {
    Map<String, String> scripts = new HashMap<>();
    for (JsonElement element : JsonParser.parseJsonArray(columnScripts).getValues()) {
      JsonObject column = element.getAsJsonObject();
      scripts.put(column.getStringMember("column"), column.getStringMemberOpt(kind).orElse(""));
    }
    return scripts;
  }

  /** @return the number of rows */
  protected static int checkColumns(GemFilterableTableData table) {
    Set<String> headers = new HashSet<>();
    int rowCount = -1;
    String firstHeader = null;
    for (GemFilterableTableColumn column : table.getColumnsList()) {
      if (column.getHeader() == null || !headers.add(column.getHeader())) {
        throw new IllegalArgumentException("Every column needs a unique header, but found \"" + column.getHeader() + "\" twice.");
      }
      int used = (column.isEmptyNumbers() ? 0 : 1) + (column.isEmptyTexts() ? 0 : 1) + (column.isEmptyBooleans() ? 0 : 1)
          + (column.isEmptyDates() ? 0 : 1) + (column.isEmptyDateTimes() ? 0 : 1);
      if (used > 1) {
        throw new IllegalArgumentException("Column \"" + column.getHeader()
            + "\" has to use only one of numbers, texts, booleans, dates, or dateTimes.");
      }
      int size = valuesOf(column).size();
      if (rowCount == -1) {
        rowCount = size;
        firstHeader = column.getHeader();
      } else if (size != rowCount) {
        throw new IllegalArgumentException("Column \"" + column.getHeader() + "\" has " + size + " values, but column \""
            + firstHeader + "\" has " + rowCount + ". All columns need the same number of values.");
      }
    }
    return Math.max(rowCount, 0);
  }

  protected static void checkScriptColumns(GemFilterableTableData table, Map<String, String> scripts) {
    List<String> headers = table.getColumnsList().stream().map(GemFilterableTableColumn::getHeader).toList();
    scripts.forEach((column, script) -> {
      if (script != null && !script.isBlank() && !headers.contains(column)) {
        throw new IllegalArgumentException("There is no column \"" + column + "\" for its script. Columns: " + headers + ".");
      }
    });
  }

  protected static List<?> valuesOf(GemFilterableTableColumn column) {
    if (!column.isEmptyNumbers()) {
      return column.getNumbersList();
    } else if (!column.isEmptyBooleans()) {
      return column.getBooleansList();
    } else if (!column.isEmptyDates()) {
      return column.getDatesList();
    } else if (!column.isEmptyDateTimes()) {
      return column.getDateTimesList();
    }
    return column.getTextsList();
  }

  protected static final Pattern RETURN = Pattern.compile("\\breturn\\b");
  // an operator or a method call, before which a filter omits the value v
  protected static final Pattern LEADING_OPERATOR = Pattern.compile("^(<=|>=|==|!=|<|>|\\.)");
  protected static final Pattern TEXT_EQUALITY = Pattern.compile("^(==|!=)\\s*(\"(?:[^\"\\\\]|\\\\.)*\")$");
  protected static final Pattern COMPARISON = Pattern.compile("^(<=|>=|==|!=|<|>)\\s*(.+)$");
  protected static final Pattern STRING_LITERAL = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

  /** @return the script returning the lambda of the filter or render function, see the class comment */
  protected static String toScript(String function, String kind, GemFilterableTableColumn column) {
    if (RETURN.matcher(function).find()) {
      return function;
    }
    String expression = function.trim();
    if (expression.endsWith(";")) {
      expression = expression.substring(0, expression.length() - 1).trim();
    }
    if (kind.equals("filter")) {
      Matcher equality = TEXT_EQUALITY.matcher(expression);
      Matcher comparison = COMPARISON.matcher(expression);
      if ((!column.isEmptyDates() || !column.isEmptyDateTimes()) && comparison.matches()) {
        String value = comparison.group(2);
        if (STRING_LITERAL.matcher(value).matches()) {
          value = parameterTypeOf(column) + ".parse(" + value + ")";
        }
        expression = "v.compareTo(" + value + ") " + comparison.group(1) + " 0";
      } else if (!column.isEmptyTexts() && equality.matches()) {
        // == on texts compares references
        expression = (equality.group(1).equals("!=") ? "!" : "") + "v.equals(" + equality.group(2) + ")";
      } else if (LEADING_OPERATOR.matcher(expression).lookingAt()) {
        expression = "v " + expression;
      }
    }
    return "return (" + parameterTypeOf(column) + " v) -> " + expression + ";";
  }

  protected static String parameterTypeOf(GemFilterableTableColumn column) {
    if (!column.isEmptyNumbers()) {
      return "Double";
    } else if (!column.isEmptyBooleans()) {
      return "Boolean";
    } else if (!column.isEmptyDates()) {
      return "LocalDate";
    } else if (!column.isEmptyDateTimes()) {
      return "LocalDateTime";
    }
    return "String";
  }

  protected static MCValue interpretFunction(String script, String kind, GemFilterableTableColumn column) {
    InterpreterWithErrorHandling.ValueOrError result = InterpreterWithErrorHandling.interpretString(script);
    if (result.isError()) {
      throw new IllegalArgumentException("The " + kind + " script of column \"" + column.getHeader() + "\" failed: "
          + result.getFindings());
    }
    if (!result.getValue().isFunction()) {
      throw new IllegalArgumentException("The " + kind + " script of column \"" + column.getHeader()
          + "\" has to return a lambda, e.g., " + exampleOf(kind, column) + ", but returned " + result.getValue().printType() + ".");
    }
    return result.getValue();
  }

  protected static String exampleOf(String kind, GemFilterableTableColumn column) {
    return "return (" + parameterTypeOf(column) + " v) -> " + (kind.equals("filter") ? "v != null" : "\"\" + v") + ";";
  }

  protected static boolean accepts(MCValue predicate, Object value, GemFilterableTableColumn column) {
    MCValue result = apply(predicate, value, "filter", column);
    if (!result.isBoolean()) {
      throw new IllegalArgumentException("The filter of column \"" + column.getHeader() + "\" has to return a boolean, but returned "
          + result.printType() + " for " + value + ".");
    }
    return result.asBoolean();
  }

  protected static String render(MCValue renderer, Object value, GemFilterableTableColumn column) {
    return String.valueOf(apply(renderer, value, "render function", column).asNativeObject());
  }

  protected static MCValue apply(MCValue function, Object value, String kind, GemFilterableTableColumn column) {
    InterpreterWithErrorHandling.ValueOrError result = InterpreterWithErrorHandling.applyFunction(function, value);
    if (result.isError()) {
      throw new IllegalArgumentException("The " + kind + " of column \"" + column.getHeader() + "\" failed for "
          + value + ": " + result.getFindings());
    }
    return result.getValue();
  }

  // whole numbers without a fraction, e.g., 3 instead of 3.0
  protected static String renderByDefault(Object value) {
    if (value instanceof Double number && number == Math.rint(number) && Math.abs(number) < 1e15) {
      return String.valueOf(number.longValue());
    }
    return String.valueOf(value);
  }

  protected static <T> List<T> select(List<T> values, List<Integer> rows) {
    if (values.isEmpty()) {
      return values;
    }
    List<T> selected = new ArrayList<>();
    for (int row : rows) {
      selected.add(values.get(row));
    }
    return selected;
  }
}
