package dashboardcomponents;

import dashboardmodule.ColumnScripts;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ExtraData;

import java.util.function.Consumer;

public class ExtraTableOptions extends ExtraTableOptionsTOP {

  @Override
  public Consumer<Void> addColumnScripts(ExtraData extra) {
    return $ -> extra.getTableextra().addColumns(
        DashboardModuleManager.columnScriptsBuilder().column("").filter("").render("").build().get());
  }

  @Override
  public Consumer<Void> removeColumnScripts(ExtraData extra, ColumnScripts scripts) {
    // keeps the filter of the column, which is edited in the table
    return $ -> {
      if (scripts.getFilter().isEmpty()) {
        extra.getTableextra().removeColumns(scripts);
      } else {
        scripts.setRender("");
      }
    };
  }
}
