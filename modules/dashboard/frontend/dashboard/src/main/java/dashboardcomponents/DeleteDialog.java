package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import dashboardmodule.VizType;
import de.se_rwth.commons.logging.Log;
import mc.fenix.charts.gembarcharttypes.GemBarChartDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBox;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBoxBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntry;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntryBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartDataBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntry;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntryBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartDataBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntry;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntryBuilder;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntryBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotDataBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPoint;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSet;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSetBuilder;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.NodePos;
import mc.fenix.networkgraph.networkgraphtypes.NodePosBuilder;
import static def.dom.Globals.document;
import def.dom.URL;
import def.dom.Blob;
import def.js.JSON;
import def.dom.HTMLAnchorElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DeleteDialog extends DeleteDialogTOP{

  @Override
  public Consumer<Void> deleteAndClose() {
    return $ -> {
      super.getDelete().accept($);
      super.getClose().accept($);
    };
  }

  @Override
  public Consumer<Void> closeDialog() {
    return $ -> {
      super.getClose().accept($);
    };
  }

}
