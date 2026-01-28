package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.monticore.symboltable.ImportStatement;
import de.monticore.values.MCValue;
import de.se_rwth.commons.logging.Log;
import expressionwrapper.Interpreter;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.GemList;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class InterpretBarChart extends InterpretBarChartTOP {

  public InterpretBarChart(String data) {
    super(data);
  }

  @Override
  @BackendOnly(value = BackendOnlyModes.THROW_ERROR)
  public SimpleResult<GemBarChartData> doAction() {
    InterpreterWithErrorHandling.ValueOrError valOrErr = InterpreterWithErrorHandling.interpretString(getData());

    if(!valOrErr.hasErrors) {
      if(valOrErr.getValue().asNativeObject() instanceof GemBarChartData data) {
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
