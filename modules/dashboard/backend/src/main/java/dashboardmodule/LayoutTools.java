package dashboardmodule;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jsweet.lang.Erased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import de.monticore.symboltable.serialization.JsonPrinter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Layout operations scoped to the view selected by the client when sending the message. */
@Erased
public class LayoutTools {
  private final ViewConfig view;

  public LayoutTools(ViewConfig view) {
    this.view = view;
  }

  /** Coordinates and dimensions are integral GridStack units, not pixels. */
  public record TilePlacement(Long id, Integer x, Integer y, Integer width, Integer height) {}

  @Tool("Returns the current view's layout as JSON: viewId, name, info (intent of the view), columns=12, minWidth=3, minHeight=3, "
      + "and tiles with id, name, x, y, width, height. "
      + "Call for layout questions and before rearranging tiles. Coordinates start at zero.")
  public String getCurrentLayout() {
    if (view == null) {
      return "No current dashboard view is available.";
    }
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    printer.member("viewId", view.getGemId());
    printer.member("name", view.getName());
    printer.member("info", view.getInfo());
    printer.member("columns", 12);
    printer.member("minWidth", 3);
    printer.member("minHeight", 3);
    printer.beginArray("tiles");
    for (CellConfig cell : view.getCellsList()) {
      printer.beginObject();
      printer.member("id", cell.getGemId());
      printer.member("name", cell.getName());
      printer.member("x", cell.getPosition().getX());
      printer.member("y", cell.getPosition().getY());
      printer.member("width", cell.getSize().getWidth());
      printer.member("height", cell.getSize().getHeight());
      printer.endObject();
    }
    printer.endArray();
    printer.endObject();
    return printer.getContent();
  }

  @Tool(name = "setCurrentLayout", value = "Sets the current view's complete layout. Include every tile exactly once, using IDs from "
      + "getCurrentLayout. Preserve unchanged positions and sizes. "
      + "Integer x/y must be nonnegative, width/height at least 3, x+width at most 12. "
      + "Tiles must not overlap. Preserves tile content and other views. Returns an error without changes if invalid.")
  public String applyCurrentLayout(@P(name = "tiles", description = "Complete list of tile placements. "
      + "Each entry requires id, x, y, width and height, all integers.") List<Map<String, Object>> tiles) {
    if (tiles == null) return setCurrentLayout(null);
    List<TilePlacement> placements = new ArrayList<>();
    try {
      for (Map<String, Object> tile : tiles) {
        if (tile == null) return "Invalid layout: each tile is required. No tiles were changed.";
        for (String field : List.of("id", "x", "y", "width", "height")) {
          if (!(tile.get(field) instanceof Number)) {
            return "Invalid layout: each tile requires integer id, x, y, width and height. No tiles were changed.";
          }
        }
        placements.add(new TilePlacement(Long.valueOf(tile.get("id").toString()), Integer.valueOf(tile.get("x").toString()),
            Integer.valueOf(tile.get("y").toString()), Integer.valueOf(tile.get("width").toString()), Integer.valueOf(tile.get("height").toString())));
      }
    } catch (RuntimeException e) {
      return "Invalid layout: each tile requires integer id, x, y, width and height. No tiles were changed.";
    }
    return setCurrentLayout(placements);
  }

  public String setCurrentLayout(List<TilePlacement> tiles) {
    if (view == null) {
      return "No current dashboard view is available.";
    }
    if (tiles == null) {
      return "Invalid layout: tiles are required. No tiles were changed.";
    }
    Map<Long, CellConfig> cells = new HashMap<>();
    for (CellConfig cell : view.getCellsList()) {
      cells.put(cell.getGemId(), cell);
    }
    Set<Long> ids = new HashSet<>();
    for (TilePlacement tile : tiles) {
      if (tile == null || tile.id() == null || tile.x() == null || tile.y() == null
          || tile.width() == null || tile.height() == null) {
        return "Invalid layout: each tile requires id, x, y, width and height. No tiles were changed.";
      }
      if (!cells.containsKey(tile.id()) || !ids.add(tile.id())) {
        return "Invalid layout: unknown or duplicate tile ID. Use only IDs from the current view. No tiles were changed.";
      }
      if (tile.x() < 0 || tile.y() < 0 || tile.width() < 3 || tile.height() < 3
          || (long) tile.x() + tile.width() > 12 || (long) tile.y() + tile.height() > Integer.MAX_VALUE) {
        return "Invalid layout: coordinates or sizes are outside the grid bounds. No tiles were changed.";
      }
    }
    if (ids.size() != cells.size()) {
      return "Invalid layout: include every tile in the current view exactly once. No tiles were changed.";
    }
    for (int i = 0; i < tiles.size(); i++) {
      TilePlacement a = tiles.get(i);
      for (int j = i + 1; j < tiles.size(); j++) {
        TilePlacement b = tiles.get(j);
        if (a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
            && a.y() < b.y() + b.height() && b.y() < a.y() + a.height()) {
          return "Invalid layout: tiles overlap. No tiles were changed.";
        }
      }
    }
    // Validate the whole proposal before broadcasting any geometry changes.
    for (TilePlacement tile : tiles) {
      CellConfig cell = cells.get(tile.id());
      cell.getPosition().setX(tile.x().doubleValue());
      cell.getPosition().setY(tile.y().doubleValue());
      cell.getSize().setWidth(tile.width().doubleValue());
      cell.getSize().setHeight(tile.height().doubleValue());
    }
    return "Updated the layout of view \"" + view.getName() + "\" (" + tiles.size() + " tiles).";
  }
}
