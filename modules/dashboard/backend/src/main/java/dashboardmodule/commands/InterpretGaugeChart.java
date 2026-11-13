package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.se_rwth.commons.logging.Log;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class InterpretGaugeChart extends InterpretGaugeChartTOP {

  public InterpretGaugeChart(String data) {
    super(data);
  }

  @Override
  @BackendOnly(value = BackendOnlyModes.THROW_ERROR)
  public SimpleResult<GemGaugeChartData> doAction() {
   InterpreterWithErrorHandling.ValueOrError valOrErr = InterpreterWithErrorHandling.interpretString(getData());

    if(!valOrErr.hasErrors) {
      if(valOrErr.getValue().asNativeObject() instanceof GemGaugeChartData data) {
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
