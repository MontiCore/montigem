package dashboardcomponents;

import def.dom.KeyboardEvent;
import mc.fenix.table.gemtabletypes.GemFilterableTableColumn;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import mc.fenix.table.gemtabletypes.GemFilterableTableFilter;
import mc.fenix.table.gemtabletypes.GemFilterableTableFilterBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GemFilterableTable extends GemFilterableTableTOP {

  @Override
  public String getHeaderLabel(GemFilterableTableColumn column, String sorting) {
    if (sorting.equals("+" + column.getHeader())) {
      return column.getHeader() + " ▲";
    } else if (sorting.equals("-" + column.getHeader())) {
      return column.getHeader() + " ▼";
    }
    return column.getHeader();
  }

  @Override
  public Consumer<Void> sortBy(GemFilterableTableColumn column) {
    return $ -> {
      String ascending = "+" + column.getHeader();
      this.setSorting(ascending.equals(this.getSorting()) ? "-" + column.getHeader() : ascending);
    };
  }

  @Override
  public Boolean hasError(String error) {
    return error != null && !error.isEmpty();
  }

  @Override
  public Boolean isFiltered(GemFilterableTableColumn column) {
    return !getFilterText(column).isEmpty();
  }

  @Override
  public String getFilterText(GemFilterableTableColumn column) {
    if (column.getGemFilterableTableFilter().isPresent()) {
      return column.getGemFilterableTableFilter().get().getFilter();
    }
    return "";
  }

  @Override
  public Boolean isEdited(GemFilterableTableColumn column, String editedColumn) {
    return column.getHeader().equals(editedColumn);
  }

  // opens the filter editor of the column with its current filter, or closes it
  @Override
  public Consumer<Void> editFilter(GemFilterableTableColumn column) {
    return $ -> {
      if (column.getHeader().equals(this.getEditedColumn())) {
        this.setEditedColumn("");
      } else {
        this.setFilterEntry(getFilterText(column));
        this.setEditedColumn(column.getHeader());
      }
    };
  }

  @Override
  public String getFilterPlaceholder(GemFilterableTableColumn column) {
    if (!column.isEmptyNumbers()) {
      return ">= 500";
    } else if (!column.isEmptyBooleans()) {
      return "== true";
    } else if (!column.isEmptyDates()) {
      return ">= \"2026-01-01\"";
    } else if (!column.isEmptyDateTimes()) {
      return ">= \"2026-01-01T09:00:00\"";
    }
    return "== \"Alice\" or .startsWith(\"A\")";
  }

  @Override
  public Consumer<Object> applyFilterOnEnter(GemFilterableTableColumn column) {
    return event -> {
      KeyboardEvent keyboardEvent = (KeyboardEvent) event;
      if (keyboardEvent.key.equals("Enter")) {
        keyboardEvent.preventDefault();
        emitFilter(column, this.getFilterEntry());
      } else if (keyboardEvent.key.equals("Escape")) {
        this.setEditedColumn("");
      }
    };
  }

  @Override
  public Consumer<Void> applyFilter(GemFilterableTableColumn column) {
    return $ -> emitFilter(column, this.getFilterEntry());
  }

  @Override
  public Consumer<Void> removeFilter(GemFilterableTableColumn column) {
    return $ -> emitFilter(column, "");
  }

  protected void emitFilter(GemFilterableTableColumn column, String filter) {
    this.setEditedColumn("");
    GemFilterableTableFilter change = new GemFilterableTableFilterBuilder()
        .column(column)
        .filter(filter == null ? "" : filter.trim())
        .build().get();
    if (this.getChangeFilter() != null) {
      this.getChangeFilter().accept(change);
    }
  }

  // the order of the rows in a new list, see GemFilterableTable.gui
  @Override
  public List<List<Integer>> getRowOrders(GemFilterableTableData data, String sorting) {
    List<List<Integer>> orders = new ArrayList<>();
    orders.add(getRowOrder(data, sorting));
    return orders;
  }

  protected List<Integer> getRowOrder(GemFilterableTableData data, String sorting) {
    List<Integer> order = new ArrayList<>();
    if (data == null || data.isEmptyColumns()) {
      return order;
    }
    GemFilterableTableColumn sorted = null;
    for (GemFilterableTableColumn column : data.getColumnsList()) {
      if (sorting.equals("+" + column.getHeader()) || sorting.equals("-" + column.getHeader())) {
        sorted = column;
      }
    }
    boolean ascending = sorting.startsWith("+");
    // insertion sort, which keeps the order of the data for equal values
    int rowCount = data.getColumns(0).sizeRendered();
    for (int row = 0; row < rowCount; row++) {
      int position = order.size();
      while (sorted != null && position > 0 && compare(sorted, order.get(position - 1), row, ascending) > 0) {
        position--;
      }
      order.add(position, row);
    }
    return order;
  }

  // compares the values of the column, e.g., 9 < 10 although "9" > "10"
  protected int compare(GemFilterableTableColumn column, int a, int b, boolean ascending) {
    int result;
    if (!column.isEmptyNumbers()) {
      double x = column.getNumbers(a);
      double y = column.getNumbers(b);
      result = x < y ? -1 : x > y ? 1 : 0;
    } else if (!column.isEmptyBooleans()) {
      boolean x = column.getBooleans(a);
      boolean y = column.getBooleans(b);
      result = x == y ? 0 : x ? 1 : -1;
    } else if (!column.isEmptyDates()) {
      result = column.getDates(a).compareTo(column.getDates(b));
    } else if (!column.isEmptyDateTimes()) {
      result = column.getDateTimes(a).compareTo(column.getDateTimes(b));
    } else {
      result = column.getTexts(a).toLowerCase().compareTo(column.getTexts(b).toLowerCase());
    }
    return ascending ? result : -result;
  }

  @Override
  public String getCellText(GemFilterableTableColumn column, Integer row) {
    return column.getRendered(row);
  }
}
