package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.monticore.symboltable.ImportStatement;
import de.monticore.values.MCValue;
import de.se_rwth.commons.logging.Log;
import expressionwrapper.Interpreter;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.GemList;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class InterpretNetworkGraph extends InterpretNetworkGraphTOP {

  public InterpretNetworkGraph(String data) {
    super(data);
  }

  @Override
  @BackendOnly(value = BackendOnlyModes.THROW_ERROR)
  public SimpleResult<Graph> doAction() {
    InterpreterWithErrorHandling.ValueOrError valOrErr = InterpreterWithErrorHandling.interpretString(getData());

    if(!valOrErr.hasErrors) {
      if(valOrErr.getValue().asNativeObject() instanceof Graph data) {
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
