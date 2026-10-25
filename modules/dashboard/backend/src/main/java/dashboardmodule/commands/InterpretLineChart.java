package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.se_rwth.commons.logging.Log;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class InterpretLineChart extends InterpretLineChartTOP {

  public InterpretLineChart(String data) {
    super(data);
  }

  @Override
  @BackendOnly(value = BackendOnlyModes.THROW_ERROR)
  public SimpleResult<GemLineChartData> doAction() {
    InterpreterWithErrorHandling.ValueOrError valOrErr = InterpreterWithErrorHandling.interpretString(getData());

    if(!valOrErr.hasErrors) {
      if(valOrErr.getValue().asNativeObject() instanceof GemLineChartData data) {
        return replySuccess(data);
      }
    } else {
      // We can use this case to decide what to do when the interpreter fails
      DashboardModuleManager.getDashboardModuleApp().setError(valOrErr.getFindings());
      Log.warn("Interpreter failed because of: : "+ valOrErr.getFindings());
    }
    return replyFail(LoadingState.FAILED_TO_LOAD);
  }
}
