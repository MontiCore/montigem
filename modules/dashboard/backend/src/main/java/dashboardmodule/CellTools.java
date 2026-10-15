package dashboardmodule;

import de.monticore.symboltable.serialization.JsonParser;

import dashboardmodule.commands.FilterableTableInterpreter;
import dashboardmodule.commands.InterpreterWithErrorHandling;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jsweet.lang.Erased;
import de.monticore.symboltable.serialization.json.JsonObject;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.JsonLexer;
import de.monticore.symboltable.serialization.JsonTokenKind;
import de.se_rwth.commons.logging.Log;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/** Cell tools scoped to the view selected when the message was sent. */
@Erased
public class CellTools {
  private final ViewConfig view;

  public CellTools(ViewConfig view) {
    this.view = view;
  }

  @Tool("Executes a dashboard cell data script and checks its returned chart or table type. "
      + "Returns success or interpreter errors to repair before creating a cell. "
      + "Use the dashboard-chart-data skill. Scripts should only read domain data and construct chart payloads; "
      + "execution is real and may have side effects if the script mutates domain objects.")
  public String checkInterpreterCode(
      @P("Visualization: BAR, LINE, PIE, SCATTER, BOX, SPEED, NETWORK or FILTERABLE_TABLE") String viz,
      @P("Complete interpreter script ending in return of the chart data object") String data) {
    String error = validate(viz, data, new JsonObject());
    return error == null ? "Success: script executed and returned the expected chart data type for " + viz + "." : error;
  }

  @Tool("Creates and displays a new chart or table cell in the current dashboard view. "
      + "Call checkInterpreterCode first and fix any errors. This tool validates the script again before creation. "
      + "The cell is placed below existing cells, with size 6 columns by 4 rows and default chart options. "
      + "For FILTERABLE_TABLE, set column filters and render scripts afterwards with setCellOptions. "
      + "Use getCurrentLayout and setCurrentLayout afterwards if a different placement is needed.")
  public String createCell(
      @P("Display name of the new cell") String name,
      @P("Visualization: BAR, LINE, PIE, SCATTER, BOX, SPEED, NETWORK or FILTERABLE_TABLE") String viz,
      @P("Complete validated interpreter script to store as the cell data") String data) {
    if (view == null) {
      return "No current dashboard view is available.";
    }
    if (name == null || name.isBlank()) {
      return "Invalid cell: a name is required.";
    }
    double bottom = 0;
    for (CellConfig cell : view.getCellsList()) {
      bottom = Math.max(bottom, cell.getPosition().getY() + cell.getSize().getHeight());
    }
    double y = Math.ceil(bottom);
    if (!Double.isFinite(y) || y < 0 || y > Integer.MAX_VALUE - 4) {
      return "Cannot create cell: the view has no space within the grid bounds.";
    }
    String error = validate(viz, data, new JsonObject());
    if (error != null) {
      return error;
    }
    VizType type = VizType.valueOf(viz.trim().toUpperCase(Locale.ROOT));
    CellConfig cell = DashboardModuleManager.cellConfigBuilder()
        .name(name.trim()).viz(type).data(data).extra(CellOptions.build(type, CellOptions.defaults(type)))
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x(0.0).y(y).build().get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(6.0).height(4.0).build().get())
        .build().get();
    view.addCells(cell);
    return "Created cell \"" + cell.getName() + "\" with id " + cell.getGemId()
        + " in view \"" + view.getName() + "\" at x=0, y=" + (int) y + ", width=6, height=4.";
  }

  @Tool("Returns a current-view cell as JSON: id, name, viz, complete data script, options, and geometry. "
      + "Use IDs from getCurrentLayout; call before editing an existing cell.")
  public String getCell(@P("Cell ID from the current view") long cellId) {
    CellConfig cell = find(cellId);
    return cell == null ? missing() : describe(cell);
  }

  @Tool("Updates an existing current-view cell. patchJson is a JSON object containing any of name, viz, data. "
      + "Omitted fields are preserved. Geometry is preserved. A visualization change resets chart options to defaults. "
      + "Changed scripts/types are executed and checked before applying any changes. Read getCell first.")
  public String updateCell(@P("Cell ID from the current view") long cellId,
      @P("JSON object with optional name, viz, data fields; provide only fields to change") String patchJson) {
    CellConfig cell = find(cellId);
    if (cell == null) return missing();
    try {
      long errors = Log.getErrorCount();
      JsonLexer lexer = new JsonLexer(patchJson == null ? "" : patchJson);
      var parsed = JsonParser.parseJson(lexer);
      while (lexer.hasNext()) {
        if (lexer.poll().getKind() != JsonTokenKind.WHITESPACE) {
          throw new IllegalArgumentException("Unexpected content after JSON");
        }
      }
      if (parsed == null || errors != Log.getErrorCount()) throw new IllegalArgumentException("Invalid JSON");
      if (!parsed.isJsonObject()) return "Invalid cell: patch must be a JSON object. No changes made.";
      JsonObject patch = parsed.getAsJsonObject();
      if (patch.sizeMembers() == 0) {
        return "Invalid cell: patch must be a nonempty JSON object. No changes made.";
      }
      var fields = patch.getMembers().entrySet().iterator();
      while (fields.hasNext()) {
        var field = fields.next();
        if (!List.of("name", "viz", "data").contains(field.getKey()) || !field.getValue().isJsonString()) {
          return "Invalid cell: only string fields name, viz, data are supported. No changes made.";
        }
      }
      String name = patch.hasMember("name") ? patch.getStringMember("name").trim() : cell.getName();
      if (name.isBlank()) return "Invalid cell: a name is required. No changes made.";
      String viz = patch.hasMember("viz") ? patch.getStringMember("viz") : cell.getViz().name();
      String data = patch.hasMember("data") ? patch.getStringMember("data") : cell.getData();
      if (patch.hasMember("viz") || patch.hasMember("data")) {
        // the scripts of the columns of a filterable table are kept, unless the visualization changes
        boolean keepsScripts = cell.getViz() == VizType.FILTERABLE_TABLE && viz.trim().equalsIgnoreCase(cell.getViz().name());
        String error = validate(viz, data, keepsScripts ? JsonParser.parseJsonObject(CellOptions.read(cell)) : new JsonObject());
        if (error != null) return error;
      }
      VizType type = VizType.valueOf(viz.trim().toUpperCase(Locale.ROOT));
      if (type != cell.getViz()) {
        cell.setExtra(CellOptions.build(type, CellOptions.defaults(type)));
        cell.setViz(type);
      }
      if (patch.hasMember("data")) cell.setData(data);
      if (patch.hasMember("name")) cell.setName(name);
      return "Updated cell: " + describe(cell);
    } catch (IllegalArgumentException e) {
      return "Invalid cell: patch must be valid JSON. No changes made.";
    }
  }

  @Tool("Patches chart options of a current-view cell; omitted options are preserved. Read getCell first. "
      + "optionsJson is a JSON object. BAR: stacked, overlapping, minValue, maxValue; LINE: minValue, maxValue; "
      + "PIE: innerRadius (0..100); BOX: min, max; SPEED: min, max, unit, textValue, textPosition; "
      + "NETWORK: minHeight, showFilterGroup, showSearchNode, showControlButtons, showInformation; "
      + "SCATTER: xAxis/yAxis objects with label, minValue, maxValue, stepWidth. "
      + "Set max/maxValue to null for automatic maximum. "
      + "FILTERABLE_TABLE: columns, an array of {column, filter, render} objects replacing all previous column scripts; "
      + "column is a header; filter (e.g., >= 30) and render (e.g., Math.round(v) + \" years\") are optional "
      + "expressions over the value v of the column (see the dashboard-chart-data skill). "
      + "The scripts are executed with the data script. Invalid options cause no changes.")
  public String setCellOptions(@P("Cell ID from the current view") long cellId,
      @P("JSON object containing only chart options to change") String optionsJson) {
    CellConfig cell = find(cellId);
    if (cell == null) return missing();
    JsonObject options;
    try {
      options = CellOptions.patch(cell.getViz(), JsonParser.parseJsonObject(CellOptions.read(cell)), optionsJson);
    } catch (IllegalArgumentException e) {
      return "Invalid options: " + e.getMessage() + " No changes made.";
    }
    String error = cell.getViz() == VizType.FILTERABLE_TABLE ? validate(cell.getViz().name(), cell.getData(), options) : null;
    if (error != null) {
      return "Invalid options: " + error + " No changes made.";
    }
    CellOptions.apply(cell, options);
    return "Updated cell options: " + describe(cell);
  }

  @Tool("Duplicates a current-view chart or table cell with an independent copy of its options and geometry. "
      + "Preserves the script, visualization and size; places the copy below existing cells. "
      + "Does not execute the script. Returns the new cell ID.")
  public String duplicateCell(@P("Source cell ID from the current view") long cellId,
      @P("Display name for the copy") String name) {
    CellConfig source = find(cellId);
    if (source == null) return missing();
    if (name == null || name.isBlank()) return "Invalid cell: a name is required.";
    JsonObject options;
    try {
      options = JsonParser.parseJsonObject(CellOptions.read(source));
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }
    double y = 0;
    for (CellConfig cell : view.getCellsList()) {
      y = Math.max(y, cell.getPosition().getY() + cell.getSize().getHeight());
    }
    y = Math.ceil(y);
    double width = source.getSize().getWidth();
    double height = source.getSize().getHeight();
    if (!Double.isFinite(y) || y < 0 || !Double.isFinite(width) || width != Math.rint(width) || width < 3 || width > 12
        || !Double.isFinite(height) || height != Math.rint(height) || height < 3 || y + height > Integer.MAX_VALUE) {
      return "Cannot duplicate cell: geometry is outside the grid bounds.";
    }
    CellConfig copy = DashboardModuleManager.cellConfigBuilder().name(name.trim())
        .viz(source.getViz()).data(source.getData()).extra(CellOptions.build(source.getViz(), options))
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x(0.0).y(y).build().get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(width).height(height).build().get())
        .build().get();
    view.addCells(copy);
    return "Duplicated cell: " + describe(copy);
  }

  @Tool("Removes cells from the current view using IDs from getCurrentLayout. "
      + "Validates all IDs before removing any cells. Other views are preserved.")
  public String removeCells(@P("Nonempty list of distinct current-view cell IDs") List<Long> cellIds) {
    if (view == null) return missing();
    if (cellIds == null || cellIds.isEmpty()) return "Invalid cells: provide at least one cell ID.";
    List<CellConfig> cells = new ArrayList<>();
    var ids = new HashSet<Long>();
    for (Long id : cellIds) {
      CellConfig cell = id == null ? null : find(id);
      if (cell == null || !ids.add(id)) {
        return "Invalid cells: unknown or duplicate cell ID. No cells removed.";
      }
      cells.add(cell);
    }
    // Unlink only: cells can be shared with another view. Deleting them would break that view.
    for (CellConfig cell : cells) view.removeCells(cell);
    return "Removed " + cells.size() + " cells from view \"" + view.getName() + "\".";
  }

  @Tool("Executes an existing current-view cell's stored script without changing its configuration. "
      + "Returns type errors or a bounded summary with entry counts, sample labels (tables: headers and sample rows, "
      + "filterable tables after applying their column scripts), "
      + "and an empty-data flag. "
      + "Execution is real; scripts must only read domain data and construct chart payloads.")
  public String previewCell(@P("Cell ID from the current view") long cellId) {
    CellConfig cell = find(cellId);
    if (cell == null) return missing();
    try {
      Object value = execute(cell.getViz().name(), cell.getData(),
          cell.getViz() == VizType.FILTERABLE_TABLE ? JsonParser.parseJsonObject(CellOptions.read(cell)) : new JsonObject());
      JsonPrinter printer = new JsonPrinter(true);
      printer.beginObject();
      printer.member("id", cellId);
      printer.member("viz", cell.getViz().name());
      List<String> labels;
      long count;
      switch (cell.getViz()) {
        case PIE -> {
          var entries = ((GemPieChartData) value).getEntriesList();
          count = entries.size();
          labels = entries.stream().limit(5).map(e -> e.getLabel() + ": " + e.getValue()).toList();
        }
        case BAR -> {
          var chart = (GemBarChartData) value;
          printer.member("seriesCount", chart.getEntriesList().size());
          count = chart.getEntriesList().stream().mapToLong(e -> e.getDataList().size()).sum();
          labels = chart.getLabelsList().stream().limit(5).toList();
        }
        case LINE -> {
          var chart = (GemLineChartData) value;
          printer.member("seriesCount", chart.getEntriesList().size());
          count = chart.getEntriesList().stream().mapToLong(e -> e.getDataList().size()).sum();
          labels = chart.getLabelsList().stream().limit(5).toList();
        }
        case BOX -> {
          var entries = ((GemBoxPlotData) value).getEntriesList();
          count = entries.size();
          labels = entries.stream().limit(5).map(e -> e.getGroup()).toList();
        }
        case SPEED -> {
          var entries = ((GemGaugeChartData) value).getEntriesList();
          count = entries.size();
          labels = entries.stream().limit(5).map(e -> e.getName() + ": " + e.getValue()).toList();
        }
        case SCATTER -> {
          var sets = ((GemScatterPlotData) value).getSetsList();
          printer.member("seriesCount", sets.size());
          count = sets.stream().mapToLong(e -> e.getPointsList().size()).sum();
          labels = sets.stream().limit(5).map(e -> e.getLabel()).toList();
        }
        case NETWORK -> {
          var graph = (Graph) value;
          count = graph.getNodesList().size();
          printer.member("edgeCount", graph.getEdgesList().size());
          labels = graph.getNodesList().stream().limit(5).map(e -> e.getLabel()).toList();
        }
        case FILTERABLE_TABLE -> {
          var columns = ((GemFilterableTableData) value).getColumnsList();
          printer.beginArray("headers");
          columns.forEach(column -> printer.value(column.getHeader()));
          printer.endArray();
          count = columns.isEmpty() ? 0 : columns.get(0).sizeRendered();
          labels = LongStream.range(0, Math.min(5, count)).mapToObj(row -> columns.stream()
              .map(column -> column.getRendered((int) row)).collect(Collectors.joining(" | "))).toList();
        }
        default -> throw new IllegalArgumentException("Unsupported visualization: " + cell.getViz());
      }
      printer.member("itemCount", count);
      printer.member("empty", count == 0);
      printer.beginArray("sample");
      labels.forEach(label -> printer.value(label == null ? "" : label.substring(0, Math.min(200, label.length()))));
      printer.endArray();
      printer.endObject();
      return printer.getContent();
    } catch (RuntimeException e) {
      return "Preview error: " + e.getMessage();
    }
  }

  private CellConfig find(long id) {
    if (view == null) return null;
    return view.getCellsList().stream().filter(cell -> cell.getGemId() == id).findFirst().orElse(null);
  }

  private String missing() {
    return view == null ? "No current dashboard view is available." : "Unknown cell ID in the current view.";
  }

  private String describe(CellConfig cell) {
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    printer.member("id", cell.getGemId());
    printer.member("name", cell.getName());
    printer.member("viz", cell.getViz().name());
    printer.member("data", cell.getData());
    // TILE cells can be inspected, although interpreter tools currently do not support them.
    printer.memberJson("options", cell.getViz() == VizType.TILE ? "{}" : CellOptions.read(cell));
    printer.member("x", cell.getPosition().getX());
    printer.member("y", cell.getPosition().getY());
    printer.member("width", cell.getSize().getWidth());
    printer.member("height", cell.getSize().getHeight());
    printer.endObject();
    return printer.getContent();
  }

  /** Returns an actionable error, or null for a successful execution with the expected type. */
  private String validate(String viz, String data, JsonObject options) {
    try {
      execute(viz, data, options);
      return null;
    } catch (RuntimeException e) {
      return e.getMessage();
    }
  }

  /** @param options of the cell, only used for the scripts of the columns of a filterable table */
  private Object execute(String viz, String data, JsonObject options) {
    Class<?> expected;
    try {
      expected = switch (VizType.valueOf(viz.trim().toUpperCase(Locale.ROOT))) {
        case BAR -> GemBarChartData.class;
        case LINE -> GemLineChartData.class;
        case PIE -> GemPieChartData.class;
        case SCATTER -> GemScatterPlotData.class;
        case BOX -> GemBoxPlotData.class;
        case SPEED -> GemGaugeChartData.class;
        case NETWORK -> Graph.class;
        case FILTERABLE_TABLE -> GemFilterableTableData.class;
        default -> null;
      };
    } catch (IllegalArgumentException | NullPointerException e) {
      expected = null;
    }
    if (expected == null) {
      throw new IllegalArgumentException("Unsupported visualization. Choose BAR, LINE, PIE, SCATTER, BOX, SPEED, NETWORK or FILTERABLE_TABLE.");
    }
    if (data == null || data.isBlank()) {
      throw new IllegalArgumentException("Interpreter error: a nonempty script is required.");
    }
    try {
      if (expected == GemFilterableTableData.class) {
        return FilterableTableInterpreter.interpret(data, CellOptions.scriptsOf(options, "filter"),
            CellOptions.scriptsOf(options, "render"));
      }
      InterpreterWithErrorHandling.ValueOrError result = InterpreterWithErrorHandling.interpretString(data);
      if (result.isError()) {
        throw new IllegalArgumentException("Interpreter error: " + result.getFindings());
      }
      Object value = result.getValue().asNativeObject();
      if (!expected.isInstance(value)) {
        throw new IllegalArgumentException("Interpreter error: expected " + expected.getSimpleName() + " but returned "
            + (value == null ? "null" : value.getClass().getSimpleName()) + ".");
      }
      return value;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Interpreter error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }
  }

}
