package dashboardcomponents;

import dashboardmodule.Axis;
import dashboardmodule.CellConfig;
import dashboardmodule.VizType;
import dashboardmodule.commands.InterpretPieChart;
import dashboardmodule.commands.InterpretLineChart;
import dashboardmodule.commands.InterpretNetworkGraph;
import dashboardmodule.commands.InterpretBarChart;
import dashboardmodule.commands.InterpretBoxPlot;
import dashboardmodule.commands.InterpretGaugeChart;
import dashboardmodule.commands.InterpretScatterPlot;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartDataBuilder;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
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
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntryBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotAxis;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotAxisBuilder;
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
import umlp.backendrte.command.CommandManager;
import de.se_rwth.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CellDisplay extends CellDisplayTOP{

  public Boolean compareViz(VizType viz) { return this.getCell() != null && viz.equals(this.getCell().getViz()); }

  public Boolean checkOptionalIntExtra(Optional<Integer> extra) {
    Integer trueExtra = extra.orElse(null);
    if (trueExtra == 999 || trueExtra == null) {
      return true;
    }
    return false;
  }

  public Boolean checkIntExtra(int extra) {
    if (extra == 999 ) {
      return true;
    }
    return false;
  }

  public Boolean checkDoubleExtra(double extra) {
    if (extra == 999d) {
      return true;
    }
    return false;
  }

  public Boolean checkOptionalDoubleExtra(Optional<Double> extra) {
    Double trueExtra = extra.orElse(null);
    if (trueExtra == 999d || trueExtra == null) {
      return true;
    }
    return false;
  }

  public GemScatterPlotAxis defaultAxisTranslate(Axis extra) {
    Double trueMax = extra.getMaxValue().orElse(null);
    if (trueMax == 999d || trueMax == null) {
      return new GemScatterPlotAxisBuilder().minValue(Optional.of(extra.getMinValue())).label(Optional.of(extra.getLabel())).build().get();
    }
    return new GemScatterPlotAxisBuilder().minValue(Optional.of(extra.getMinValue())).maxValue((extra.getMaxValue())).stepWidth(Optional.of(extra.getStepWidth())).label(Optional.of(extra.getLabel())).build().get();
  }

  public GemBarChartData getBarData() {
    return CommandManager.executeCommand(new InterpretBarChart(this.getCell().getData())).asSimpleResult().get();
  }

  public GemPieChartData getPieData() {
    return CommandManager.executeCommand(new InterpretPieChart(this.getCell().getData())).asSimpleResult().get();
  }

  public GemLineChartData getLineData() {
    return CommandManager.executeCommand(new InterpretLineChart(this.getCell().getData())).asSimpleResult().get();
  }

  public Graph getNetworkData() {
    return CommandManager.executeCommand(new InterpretNetworkGraph(this.getCell().getData())).asSimpleResult().get();
  }

  // cells without graph extra data would pass null and override GemGraph's default min height,
  // which leaves the sigma container at 0px and the graph is never rendered
  public String getGraphMinHeight() {
    if (this.getCell() == null || this.getCell().getExtra() == null
        || this.getCell().getExtra().getGraphextra() == null
        || this.getCell().getExtra().getGraphextra().getMinHeight() == null) {
      return "50vH";
    }
    return this.getCell().getExtra().getGraphextra().getMinHeight();
  }

  public GemGaugeChartData getSpeedData() {
    return CommandManager.executeCommand(new InterpretGaugeChart(this.getCell().getData())).asSimpleResult().get();
  }

  public GemScatterPlotData getScatterData() {
    return CommandManager.executeCommand(new InterpretScatterPlot(this.getCell().getData())).asSimpleResult().get();
  }

  public GemBoxPlotData getBoxData() {
    return CommandManager.executeCommand(new InterpretBoxPlot(this.getCell().getData())).asSimpleResult().get();
  }

  //Constantin said known to not work so do not expect anything and ignore
//  public GemBoxPlotData getBoxData() {
//
//    List<GemBoxPlotEntry> entries = new ArrayList<>();
//
//    List<Double> points1 = new ArrayList<>();
//    points1.add(4d);
//    points1.add(7d);
//    points1.add(8d);
//    points1.add(3d);
//    points1.add(1d);
//    points1.add(5d);
//    points1.add(4d);
//    points1.add(5d);
//
//    GemBoxPlotBox box1 = new GemBoxPlotBoxBuilder()
//        .median(4.5)
//        .lowerQuantile(3d)
//        .upperQuantile(7d)
//        .lowerExtreme(1d)
//        .upperExtreme(8d)
//        .build()
//        .get();
//
//    entries.add(new GemBoxPlotEntryBuilder()
//        .group("Team 1")
//        .color(Optional.of("red"))
//        .points(points1)
//        .box(box1)
//        .build()
//        .get());
//
//    List<Double> points2 = new ArrayList<>();
//    points2.add(2d);
//    points2.add(5d);
//    points2.add(3d);
//    points2.add(3d);
//    points2.add(3d);
//    points2.add(8d);
//    points2.add(9d);
//    points2.add(4d);
//
//    GemBoxPlotBox box2 = new GemBoxPlotBoxBuilder()
//        .median(3.5)
//        .lowerQuantile(3d)
//        .upperQuantile(8d)
//        .lowerExtreme(2d)
//        .upperExtreme(9d)
//        .build()
//        .get();
//
//    entries.add(new GemBoxPlotEntryBuilder()
//        .group("Team 2")
//        .color(Optional.of("green"))
//        .points(points2)
//        .box(box2)
//        .build()
//        .get());
//
//    List<Double> points3 = new ArrayList<>();
//    points3.add(4d);
//    points3.add(4d);
//    points3.add(4d);
//    points3.add(5d);
//    points3.add(3d);
//    points3.add(4d);
//    points3.add(5d);
//    points3.add(2d);
//
//    GemBoxPlotBox box3 = new GemBoxPlotBoxBuilder()
//        .median(4d)
//        .lowerQuantile(3d)
//        .upperQuantile(5d)
//        .lowerExtreme(2d)
//        .upperExtreme(5d)
//        .build()
//        .get();
//
//    entries.add(new GemBoxPlotEntryBuilder()
//        .group("Team 3")
//        .color(Optional.of("blue"))
//        .points(points3)
//        .box(box3)
//        .build()
//        .get());
//
//    return new GemBoxPlotDataBuilder()
//        .entries(entries)
//        .build()
//        .get();
//  }
}
