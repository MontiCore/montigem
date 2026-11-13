package dashboardmodule;

import de.monticore.symboltable.serialization.json.JsonElement;
import de.monticore.symboltable.serialization.json.JsonObject;
import de.monticore.symboltable.serialization.json.JsonArray;
import de.monticore.symboltable.serialization.json.JsonString;
import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.JsonLexer;
import de.monticore.symboltable.serialization.JsonTokenKind;
import de.se_rwth.commons.logging.Log;
import java.util.LinkedHashMap;
import jsweet.lang.Erased;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used in CellTools in tool calls to change a cell.
 */
@Erased
final class CellOptions {

  private CellOptions() {}

  static JsonObject defaults(VizType type) {
    return JsonParser.parseJsonObject(switch (type) {
      case BAR -> "{\"stacked\":false,\"overlapping\":false,\"minValue\":0,\"maxValue\":null}";
      case LINE -> "{\"minValue\":0,\"maxValue\":null}";
      case PIE -> "{\"innerRadius\":0}";
      case BOX -> "{\"min\":0.0,\"max\":null}";
      case SPEED -> "{\"min\":0,\"max\":null,\"unit\":\"\",\"textValue\":\"\",\"textPosition\":\"center\"}";
      case NETWORK -> "{\"minHeight\":\"50vH\",\"showFilterGroup\":false,\"showSearchNode\":false,"
          + "\"showControlButtons\":true,\"showInformation\":false,"
          // the layouts GemGraph uses by default, as in the cell options of the frontend
          + "\"initialLayout\":\"RANDOM\",\"horizontalLayout\":false,\"complexLayout\":\"FORCEATLAS2\","
          + "\"maxLayoutIterations\":100}";
      case SCATTER -> "{\"xAxis\":{\"label\":\"X\",\"minValue\":0.0,\"maxValue\":null,\"stepWidth\":999.0},"
          + "\"yAxis\":{\"label\":\"Y\",\"minValue\":0.0,\"maxValue\":null,\"stepWidth\":999.0}}";
      case FILTERABLE_TABLE -> "{\"columns\":[]}";
      default -> throw new IllegalArgumentException("Unsupported chart type: " + type);
    });
  }

  /** Serializes chart options with explicit fields, without exposing generated domain observers. */
  static String read(CellConfig cell) {
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    ExtraData e = cell.isPresentExtra() ? cell.getExtra() : null;
    switch (cell.getViz()) {
      case BAR -> {
        var b = e != null && e.isPresentBarextra() ? e.getBarextra() : null;
        printer.member("stacked", b != null && b.isStacked());
        printer.member("overlapping", b != null && b.isOverlapping());
        printer.member("minValue", b == null ? 0 : b.getMinValue());
        printer.memberJson("maxValue", b == null ? "null" : b.getMaxValue().map(Object::toString).orElse("null"));
      }
      case LINE -> {
        var l = e != null && e.isPresentLinextra() ? e.getLinextra() : null;
        printer.member("minValue", l == null ? 0 : l.getMinValue());
        printer.memberJson("maxValue", l == null ? "null" : l.getMaxValue().map(Object::toString).orElse("null"));
      }
      case PIE -> printer.member("innerRadius", e != null && e.isPresentPiextra() ? e.getPiextra().getInnerRadius() : 0);
      case BOX -> {
        var b = e != null && e.isPresentBoxextra() ? e.getBoxextra() : null;
        printer.member("min", b == null ? 0.0 : b.getMin());
        printer.memberJson("max", b == null ? "null" : b.getMax().map(Object::toString).orElse("null"));
      }
      case SPEED -> {
        var g = e != null && e.isPresentSpeedextra() ? e.getSpeedextra() : null;
        printer.member("min", g == null ? 0 : g.getMin());
        printer.memberJson("max", g == null ? "null" : g.getMax().map(Object::toString).orElse("null"));
        printer.member("unit", g == null ? "" : g.getUnit());
        printer.member("textValue", g == null ? "" : g.getTextValue());
        printer.member("textPosition", g == null ? "center" : g.getTextPosition());
      }
      case NETWORK -> {
        var g = e != null && e.isPresentGraphextra() ? e.getGraphextra() : null;
        printer.member("minHeight", g == null ? "50vH" : g.getMinHeight());
        printer.member("showFilterGroup", g != null && g.isShowFilterGroup());
        printer.member("showSearchNode", g != null && g.isShowSearchNode());
        printer.member("showControlButtons", g == null || g.isShowControlButtons());
        printer.member("showInformation", g != null && g.isShowInformation());
        // cells stored before the layouts could be chosen have none, they get the layouts used before
        boolean legacy = g == null || g.getComplexLayout() == null;
        printer.member("initialLayout", g == null || g.getInitialLayout() == null
            ? GraphInitialLayoutType.RANDOM.name() : g.getInitialLayout().name());
        printer.member("horizontalLayout", g != null && g.isHorizontalLayout());
        printer.member("complexLayout", legacy ? GraphComplexLayoutType.FORCEATLAS2.name() : g.getComplexLayout().name());
        printer.memberJson("maxLayoutIterations", legacy || g.getMaxLayoutIterations() == null ? "100"
            : g.getMaxLayoutIterations().map(Object::toString).orElse("null"));
      }
      case SCATTER -> {
        var scatter = e != null && e.isPresentScatterextra() ? e.getScatterextra() : null;
        for (String key : List.of("xAxis", "yAxis")) {
          Axis axis = scatter == null ? null : key.equals("xAxis") ? scatter.getXAxis() : scatter.getYAxis();
          printer.beginObject(key);
          printer.member("label", axis == null ? key.equals("xAxis") ? "X" : "Y" : axis.getLabel());
          printer.member("minValue", axis == null ? 0.0 : axis.getMinValue());
          printer.memberJson("maxValue", axis == null ? "null" : axis.getMaxValue().map(Object::toString).orElse("null"));
          printer.member("stepWidth", axis == null ? 999.0 : axis.getStepWidth());
          printer.endObject();
        }
      }
      case FILTERABLE_TABLE -> {
        printer.beginArray("columns");
        if (e != null && e.isPresentTableextra()) {
          for (ColumnScripts scripts : e.getTableextra().getColumnsList()) {
            printer.beginObject();
            printer.member("column", scripts.getColumn());
            printer.member("filter", scripts.getFilter());
            printer.member("render", scripts.getRender());
            printer.endObject();
          }
        }
        printer.endArray();
      }
      default -> throw new IllegalArgumentException("Unsupported chart type: " + cell.getViz());
    }
    printer.endObject();
    return printer.getContent();
  }

  /** Merge and validate entirely in plain JSON before allocating or mutating domain objects. */
  static JsonObject patch(VizType type, JsonObject current, String json) {
    JsonElement patch;
    try {
      long errors = Log.getErrorCount();
      JsonLexer lexer = new JsonLexer(json == null ? "" : json);
      patch = JsonParser.parseJson(lexer);
      while (lexer.hasNext()) {
        if (lexer.poll().getKind() != JsonTokenKind.WHITESPACE) {
          throw new IllegalArgumentException("Unexpected content after JSON");
        }
      }
      if (errors != Log.getErrorCount()) throw new IllegalArgumentException("Invalid JSON");
    } catch (Exception e) {
      throw new IllegalArgumentException("Options must be a JSON object.");
    }
    if (patch == null || !patch.isJsonObject() || patch.getAsJsonObject().sizeMembers() == 0) {
      throw new IllegalArgumentException("Options must be a nonempty JSON object.");
    }
    JsonObject result = new JsonObject();
    result.setMembers(new LinkedHashMap<>(current.getMembers()));
    for (String key : List.of("xAxis", "yAxis")) {
      if (current.hasObjectMember(key)) {
        JsonObject axis = new JsonObject();
        axis.setMembers(new LinkedHashMap<>(current.getObjectMember(key).getMembers()));
        result.putMember(key, axis);
      }
    }
    merge(result, patch.getAsJsonObject(), defaults(type));
    range(result, "minValue", "maxValue");
    range(result, "min", "max");
    if (type == VizType.PIE && (result.getIntegerMember("innerRadius") < 0
        || result.getIntegerMember("innerRadius") > 100)) {
      throw new IllegalArgumentException("innerRadius must be between 0 and 100.");
    }
    for (String key : new String[]{"xAxis", "yAxis"}) {
      if (result.hasMember(key)) {
        range(result.getObjectMember(key), "minValue", "maxValue");
        if (result.getObjectMember(key).getDoubleMember("stepWidth") <= 0) {
          throw new IllegalArgumentException(key + ".stepWidth must be positive.");
        }
      }
    }
    if (type == VizType.SPEED && !List.of("top", "center", "bottom")
        .contains(result.getStringMember("textPosition"))) {
      throw new IllegalArgumentException("textPosition must be top, center or bottom.");
    }
    if (type == VizType.NETWORK) {
      if (!names(GraphInitialLayoutType.values()).contains(result.getStringMember("initialLayout"))) {
        throw new IllegalArgumentException("initialLayout must be one of " + names(GraphInitialLayoutType.values()) + ".");
      }
      if (!names(GraphComplexLayoutType.values()).contains(result.getStringMember("complexLayout"))) {
        throw new IllegalArgumentException("complexLayout must be one of " + names(GraphComplexLayoutType.values()) + ".");
      }
      if (!result.getMember("maxLayoutIterations").isJsonNull() && result.getIntegerMember("maxLayoutIterations") <= 0) {
        throw new IllegalArgumentException("maxLayoutIterations must be positive, or null for no limit.");
      }
    }
    return result;
  }

  private static List<String> names(Enum<?>[] values) {
    return java.util.Arrays.stream(values).map(Enum::name).toList();
  }

  private static void merge(JsonObject target, JsonObject patch, JsonObject schema) {
    patch.getMembers().forEach((key, value) -> {
      if (!schema.hasMember(key)) throw new IllegalArgumentException("Unsupported option: " + key);
      JsonElement template = schema.getMember(key);
      if (template.isJsonArray()) {
        target.putMember(key, columnScripts(value));
        return;
      }
      if (template.isJsonObject()) {
        if (!value.isJsonObject()) throw new IllegalArgumentException(key + " must be an object.");
        merge(target.getObjectMember(key), value.getAsJsonObject(), template.getAsJsonObject());
        return;
      }
      boolean valid;
      if (key.equals("maxLayoutIterations")) {
        // integer by default, null for no limit
        valid = value.isJsonNull() || isInteger(value);
      } else if (template.isJsonNull()) {
        boolean integer = schema.hasMember("minValue") && isInteger(schema.getMember("minValue"))
            || schema.hasMember("min") && isInteger(schema.getMember("min"));
        valid = value.isJsonNull() || (integer ? isInteger(value) : isFiniteNumber(value));
      } else if (isInteger(template)) {
        valid = isInteger(value);
      } else if (template.isJsonNumber()) {
        valid = isFiniteNumber(value);
      } else if (template.isJsonBoolean()) {
        valid = value.isJsonBoolean();
      } else {
        valid = value.isJsonString();
      }
      if (!valid) throw new IllegalArgumentException("Invalid value for option: " + key);
      target.putMember(key, value);
    });
  }

  private static boolean isInteger(JsonElement value) {
    if (!value.isJsonNumber()) return false;
    try {
      Integer.parseInt(value.getAsJsonNumber().toString());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isFiniteNumber(JsonElement value) {
    return value.isJsonNumber() && Double.isFinite(value.getAsJsonNumber().getNumberAsDouble());
  }

  private static JsonArray columnScripts(JsonElement value) {
    if (!value.isJsonArray()) throw new IllegalArgumentException("columns must be an array.");
    JsonArray columns = new JsonArray();
    var names = new HashSet<String>();
    for (JsonElement scripts : value.getAsJsonArray().getValues()) {
      if (!scripts.isJsonObject() || !scripts.getAsJsonObject().hasStringMember("column")
          || !names.add(scripts.getAsJsonObject().getStringMember("column"))) {
        throw new IllegalArgumentException("Every entry of columns needs the header of a distinct column as text.");
      }
      JsonObject source = scripts.getAsJsonObject();
      JsonObject entry = new JsonObject();
      entry.putMember("column", source.getMember("column"));
      for (String kind : new String[]{"filter", "render"}) {
        if (source.hasMember(kind) && !source.hasStringMember(kind) && !source.getMember(kind).isJsonNull()) {
          throw new IllegalArgumentException(kind + " must be a script as text.");
        }
        entry.putMember(kind, new JsonString(source.getStringMemberOpt(kind).orElse("")));
      }
      for (String field : source.getMemberNames()) {
        if (!List.of("column", "filter", "render").contains(field)) {
          throw new IllegalArgumentException("Unsupported field of columns: " + field);
        }
      }
      columns.add(entry);
    }
    return columns;
  }

  /** The filter or render scripts of a filterable table by column header. */
  static Map<String, String> scriptsOf(JsonObject o, String kind) {
    Map<String, String> scripts = new HashMap<>();
    o.getArrayMemberOpt("columns").orElse(List.of()).forEach(column -> {
      JsonObject entry = column.getAsJsonObject();
      scripts.put(entry.getStringMember("column"), entry.getStringMember(kind));
    });
    return scripts;
  }

  private static void range(JsonObject o, String min, String max) {
    if (o.hasMember(min) && o.hasMember(max) && !o.getMember(max).isJsonNull()
        && o.getDoubleMember(max) <= o.getDoubleMember(min)) {
      throw new IllegalArgumentException(max + " must be greater than " + min + ".");
    }
  }

  /** Update existing option objects through generated setters so observers retain their IDs. */
  static void apply(CellConfig cell, JsonObject o) {
    if (!cell.isPresentExtra()) {
      cell.setExtra(build(cell.getViz(), o));
      return;
    }
    ExtraData e = cell.getExtra();
    switch (cell.getViz()) {
      case BAR -> {
        if (!e.isPresentBarextra()) fill(cell.getViz(), o, e);
        else {
          var b = e.getBarextra();
          b.setStacked(o.getBooleanMember("stacked"));
          b.setOverlapping(o.getBooleanMember("overlapping"));
          b.setMinValue(o.getIntegerMember("minValue"));
          b.setMaxValue(integer(o, "maxValue"));
        }
      }
      case LINE -> {
        if (!e.isPresentLinextra()) fill(cell.getViz(), o, e);
        else {
          e.getLinextra().setMinValue(o.getIntegerMember("minValue"));
          e.getLinextra().setMaxValue(integer(o, "maxValue"));
        }
      }
      case PIE -> {
        if (!e.isPresentPiextra()) fill(cell.getViz(), o, e);
        else e.getPiextra().setInnerRadius(o.getIntegerMember("innerRadius"));
      }
      case BOX -> {
        if (!e.isPresentBoxextra()) fill(cell.getViz(), o, e);
        else {
          e.getBoxextra().setMin(o.getDoubleMember("min"));
          e.getBoxextra().setMax(decimal(o, "max"));
        }
      }
      case SPEED -> {
        if (!e.isPresentSpeedextra()) fill(cell.getViz(), o, e);
        else {
          var g = e.getSpeedextra();
          g.setMin(o.getIntegerMember("min"));
          g.setMax(integer(o, "max"));
          g.setUnit(o.getStringMember("unit"));
          g.setTextValue(o.getStringMember("textValue"));
          g.setTextPosition(o.getStringMember("textPosition"));
        }
      }
      case NETWORK -> {
        if (!e.isPresentGraphextra()) fill(cell.getViz(), o, e);
        else {
          var g = e.getGraphextra();
          g.setMinHeight(o.getStringMember("minHeight"));
          g.setShowFilterGroup(o.getBooleanMember("showFilterGroup"));
          g.setShowSearchNode(o.getBooleanMember("showSearchNode"));
          g.setShowControlButtons(o.getBooleanMember("showControlButtons"));
          g.setShowInformation(o.getBooleanMember("showInformation"));
          g.setInitialLayout(GraphInitialLayoutType.valueOf(o.getStringMember("initialLayout")));
          g.setHorizontalLayout(o.getBooleanMember("horizontalLayout"));
          g.setComplexLayout(GraphComplexLayoutType.valueOf(o.getStringMember("complexLayout")));
          g.setMaxLayoutIterations(integer(o, "maxLayoutIterations"));
        }
      }
      case SCATTER -> {
        if (!e.isPresentScatterextra()) fill(cell.getViz(), o, e);
        else {
          applyAxis(e.getScatterextra().getXAxis(), o.getObjectMember("xAxis"));
          applyAxis(e.getScatterextra().getYAxis(), o.getObjectMember("yAxis"));
        }
      }
      case FILTERABLE_TABLE -> {
        if (!e.isPresentTableextra()) fill(cell.getViz(), o, e);
        else applyColumnScripts(e.getTableextra(), o.getArrayMember("columns"));
      }
      default -> throw new IllegalArgumentException("Unsupported chart type: " + cell.getViz());
    }
  }

  private static void applyColumnScripts(TableExtraData extra, List<JsonElement> columns) {
    for (int i = 0; i < columns.size(); i++) {
      JsonObject o = columns.get(i).getAsJsonObject();
      if (i < extra.getColumnsList().size()) {
        ColumnScripts scripts = extra.getColumns(i);
        scripts.setColumn(o.getStringMember("column"));
        scripts.setFilter(o.getStringMember("filter"));
        scripts.setRender(o.getStringMember("render"));
      } else {
        extra.addColumns(buildColumnScripts(o));
      }
    }
    while (extra.getColumnsList().size() > columns.size()) {
      extra.removeColumns(extra.getColumnsList().size() - 1);
    }
  }

  private static void applyAxis(Axis axis, JsonObject o) {
    axis.setLabel(o.getStringMember("label"));
    axis.setMinValue(o.getDoubleMember("minValue"));
    axis.setMaxValue(decimal(o, "maxValue"));
    axis.setStepWidth(o.getDoubleMember("stepWidth"));
  }

  static ExtraData build(VizType type, JsonObject o) {
    ExtraData extra = DashboardModuleManager.extraDataBuilder().build().get();
    fill(type, o, extra);
    return extra;
  }

  private static void fill(VizType type, JsonObject o, ExtraData extra) {
    switch (type) {
      case BAR -> extra.setBarextra(DashboardModuleManager.barExtraDataBuilder()
          .stacked(o.getBooleanMember("stacked")).overlapping(o.getBooleanMember("overlapping"))
          .minValue(o.getIntegerMember("minValue")).maxValue(integer(o, "maxValue")).build().get());
      case LINE -> extra.setLinextra(DashboardModuleManager.lineExtraDataBuilder()
          .minValue(o.getIntegerMember("minValue")).maxValue(integer(o, "maxValue")).build().get());
      case PIE -> extra.setPiextra(DashboardModuleManager.pieExtraDataBuilder()
          .innerRadius(o.getIntegerMember("innerRadius")).build().get());
      case BOX -> extra.setBoxextra(DashboardModuleManager.boxExtraDataBuilder()
          .min(o.getDoubleMember("min")).max(decimal(o, "max")).build().get());
      case SPEED -> extra.setSpeedextra(DashboardModuleManager.gaugeExtraDataBuilder()
          .min(o.getIntegerMember("min")).max(integer(o, "max")).unit(o.getStringMember("unit"))
          .textValue(o.getStringMember("textValue")).textPosition(o.getStringMember("textPosition")).build().get());
      case NETWORK -> extra.setGraphextra(DashboardModuleManager.graphExtraDataBuilder()
          .minHeight(o.getStringMember("minHeight")).showFilterGroup(o.getBooleanMember("showFilterGroup"))
          .showSearchNode(o.getBooleanMember("showSearchNode"))
          .showControlButtons(o.getBooleanMember("showControlButtons"))
          .showInformation(o.getBooleanMember("showInformation"))
          .initialLayout(GraphInitialLayoutType.valueOf(o.getStringMember("initialLayout")))
          .horizontalLayout(o.getBooleanMember("horizontalLayout"))
          .complexLayout(GraphComplexLayoutType.valueOf(o.getStringMember("complexLayout")))
          .maxLayoutIterations(integer(o, "maxLayoutIterations")).build().get());
      case SCATTER -> extra.setScatterextra(DashboardModuleManager.scatterExtraDataBuilder()
          .xAxis(buildAxis(o.getObjectMember("xAxis"))).yAxis(buildAxis(o.getObjectMember("yAxis"))).build().get());
      case FILTERABLE_TABLE -> {
        // columnsAdd instead of columns(Set) keeps the order of the columns
        var builder = DashboardModuleManager.tableExtraDataBuilder();
        o.getArrayMember("columns").forEach(column -> builder.columnsAdd(buildColumnScripts(column.getAsJsonObject())));
        extra.setTableextra(builder.build().get());
      }
      default -> throw new IllegalArgumentException("Unsupported chart type: " + type);
    }
  }

  private static ColumnScripts buildColumnScripts(JsonObject o) {
    return DashboardModuleManager.columnScriptsBuilder().column(o.getStringMember("column"))
        .filter(o.getStringMember("filter")).render(o.getStringMember("render")).build().get();
  }

  private static Axis buildAxis(JsonObject o) {
    return DashboardModuleManager.axisBuilder().label(o.getStringMember("label"))
        .minValue(o.getDoubleMember("minValue")).maxValue(decimal(o, "maxValue"))
        .stepWidth(o.getDoubleMember("stepWidth")).build().get();
  }

  private static Optional<Integer> integer(JsonObject o, String key) {
    return o.hasMember(key) && !o.getMember(key).isJsonNull() ? Optional.of(o.getIntegerMember(key)) : Optional.empty();
  }

  private static Optional<Double> decimal(JsonObject o, String key) {
    return o.hasMember(key) && !o.getMember(key).isJsonNull() ? Optional.of(o.getDoubleMember(key)) : Optional.empty();
  }
}
