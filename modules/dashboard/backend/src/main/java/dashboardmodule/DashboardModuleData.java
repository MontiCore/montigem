package dashboardmodule;

import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.networkgraph.networkgraphtypes.Graph;

import java.util.ArrayList;
import java.util.Optional;

public class DashboardModuleData extends DashboardModuleDataTOP {


  @Override
  public Optional<GemBarChartData> getBardata() {
    return Optional.empty();
  }

  @Override
  public Optional<GemBoxPlotData> getBoxdata() {
    return Optional.empty();
  }

  @Override
  public Optional<GemGaugeChartData> getGaugedata() {
    return Optional.empty();
  }

  @Override
  public Optional<GemLineChartData> getLinedata() {
    return Optional.empty();
  }

  @Override
  public Optional<Graph> getNetdata() {
    return Optional.empty();
  }

  @Override
  public Optional<GemPieChartData> getPiedata() {
    return Optional.empty();
  }

  @Override
  public Optional<GemScatterPlotData> getScatterdata() {
    return Optional.empty();
  }
}
