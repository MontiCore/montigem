package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.ColumnScripts;
import dashboardmodule.commands.InterpretFilterableTable;
import def.js.JSON;
import jsweet.lang.Async;

// The command interpreting the data script of a FILTERABLE_TABLE cell together with the scripts of its columns
public class FilterableTableCommand {

  @Async
  public static InterpretFilterableTable of(CellConfig cell) {
    return withFilter(cell, null, null);
  }

  /** @param changedColumn whose filter is replaced by changedFilter, null to use the scripts of the cell as they are */
  @Async
  public static InterpretFilterableTable withFilter(CellConfig cell, String changedColumn, String changedFilter) {
    // a JSON array of {column, filter, render} objects, JSON.stringify escapes the scripts
    String columnScripts = "";
    boolean changed = false;
    if (cell.isPresentExtra() && cell.getExtra().isPresentTableextra()) {
      for (ColumnScripts scripts : cell.getExtra().getTableextra().getColumnsList()) {
        String filter = scripts.getFilter();
        if (scripts.getColumn().equals(changedColumn)) {
          filter = changedFilter;
          changed = true;
        }
        columnScripts = append(columnScripts, scripts.getColumn(), filter, scripts.getRender());
      }
    }
    if (changedColumn != null && !changed) {
      columnScripts = append(columnScripts, changedColumn, changedFilter, "");
    }
    return new InterpretFilterableTable(cell.getData(), "[" + columnScripts + "]");
  }

  protected static String append(String columnScripts, String column, String filter, String render) {
    return (columnScripts.isEmpty() ? "" : columnScripts + ",") + "{\"column\":" + JSON.stringify(column)
        + ",\"filter\":" + JSON.stringify(filter) + ",\"render\":" + JSON.stringify(render) + "}";
  }
}
