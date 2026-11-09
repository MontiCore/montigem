package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.se_rwth.commons.logging.Log;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class InterpretFilterableTable extends InterpretFilterableTableTOP {

  public InterpretFilterableTable(String data, String columnScripts) {
    super(data, columnScripts);
  }

  @Override
  @BackendOnly(value = BackendOnlyModes.THROW_ERROR)
  public SimpleResult<GemFilterableTableData> doAction() {
    try {
      return replySuccess(FilterableTableInterpreter.interpret(getData(),
          FilterableTableInterpreter.scriptsOf(getColumnScripts(), "filter"),
          FilterableTableInterpreter.scriptsOf(getColumnScripts(), "render")));
    } catch (IllegalArgumentException e) {
      DashboardModuleManager.getDashboardModuleApp().setError(e.getMessage());
      Log.warn("Interpreter failed because of: " + e.getMessage());
      return replyFail(LoadingState.FAILED_TO_LOAD);
    }
  }
}
