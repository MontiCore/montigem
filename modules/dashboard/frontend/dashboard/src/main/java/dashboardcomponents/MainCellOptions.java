package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import static def.dom.Globals.document;
import def.dom.HTMLAnchorElement;

import java.util.Optional;
import java.util.function.Consumer;

public class MainCellOptions extends MainCellOptionsTOP{

  @Override
  public Consumer<Void> downloadClassDiagram() {
    return $ -> {
      HTMLAnchorElement a =
          (HTMLAnchorElement) document.createElement("a");

      a.href = "assets/images/class-diagram.png";
      a.setAttribute("download", "class_diagram.png");

      a.click();
    };
  }

  @Override
  public Boolean checkIfTutorialStep1112() { return this.getTutorialStep() == 11 || this.getTutorialStep() == 12; }

  @Override
  public void setDefaultValueEntry(String val) {

    super.setDefaultValueEntry(val);

    switch (this.getCell().getViz()) {
      case LINE:
        this.getCell().setData(defaultLineData);
        this.setDefaultLineExtra(this.getCell());
        break;
      case BAR:
        this.getCell().setData(defaultBarData);
        this.setDefaultBarExtra(this.getCell());
        break;
      case SCATTER:
        this.getCell().setData(defaultScatterData);
        this.setDefaultScatterExtra(this.getCell());
        break;
      case SPEED:
        this.getCell().setData(defaultSpeedData);
        this.setDefaultSpeedExtra(this.getCell());
        break;
      case BOX:
        this.getCell().setData(defaultBoxData);
        this.setDefaultBoxExtra(this.getCell());
        break;
      case PIE:
        this.getCell().setData(defaultPieData);
        this.setDefaultPieExtra(this.getCell());
        break;
      case NETWORK:
        this.getCell().setData(defaultNetworkData);
        this.setDefaultGraphExtra(this.getCell());
        break;
    };
  }

  //---------------------------------------------------------------------------------------------------
  //default data to give users some starting template for data if they change the type of visualization
  //---------------------------------------------------------------------------------------------------

  String defaultLineData = """
    List<String> labels = ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7", "Week 8"];
    List<Double> entrydata1 = [4.0, 7.0, 8.0, 3.0, 1.0, 5.0, 4.0, 5.0];
    List<Double> entrydata2 = [2.0, 5.0, 3.0, 3.0, 3.0, 8.0, 9.0, 4.0];
    List<Double> entrydata3 = [4.0, 4.0, 4.0, 5.0, 3.0, 4.0, 5.0, 2.0];
    GemLineChartEntry entry1 = GemLineChartEntryBuilder().label("Team 1").data(entrydata1).build().get();
    GemLineChartEntry entry2 = GemLineChartEntryBuilder().label("Team 2").data(entrydata2).build().get();
    GemLineChartEntry entry3 = GemLineChartEntryBuilder().label("Team 3").data(entrydata3).build().get();
    List<GemLineChartEntry> entries = [entry1, entry2, entry3];
    return GemLineChartDataBuilder().entries(entries).labels(labels).build().get();
    """;

  String defaultBarData = """
    List<String> labels = [""];
    List<Integer> numbers1 = [576];
    List<Integer> numbers2 = [455];
    List<Integer> numbers3 = [505];
    GemBarChartEntry entry1 = GemBarChartEntryBuilder().data(numbers1).label(Optional.of("Team 1")).build().get();
    GemBarChartEntry entry2 = GemBarChartEntryBuilder().data(numbers2).label(Optional.of("Team 2")).build().get();
    GemBarChartEntry entry3 = GemBarChartEntryBuilder().data(numbers3).label(Optional.of("Team 3")).build().get();
    List<GemBarChartEntry> entries = [entry1, entry2, entry3];
    return GemBarChartDataBuilder().entries(entries).labels(labels).build().get();
    """;

  String defaultScatterData = """
     List<GemScatterPlotPoint> points1 = [GemScatterPlotPoint(1.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 7.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 8.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 1.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 5.0, "", Optional.of(8))];
     List<GemScatterPlotPoint> points2 = [GemScatterPlotPoint(1.0, 2.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 8.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 9.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 4.0, "", Optional.of(8))];
     List<GemScatterPlotPoint> points3 = [GemScatterPlotPoint(1.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(2.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(3.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(4.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(5.0, 3.0, "", Optional.of(8)), GemScatterPlotPoint(6.0, 4.0, "", Optional.of(8)), GemScatterPlotPoint(7.0, 5.0, "", Optional.of(8)), GemScatterPlotPoint(8.0, 2.0, "", Optional.of(8))];
     GemScatterPlotSet set1 = GemScatterPlotSetBuilder().label("Team 1").points(points1).build().get();
     GemScatterPlotSet set2 = GemScatterPlotSetBuilder().label("Team 2").points(points2).build().get();
     GemScatterPlotSet set3 = GemScatterPlotSetBuilder().label("Team 3").points(points3).build().get();
     List<GemScatterPlotSet> sets = [set1, set2, set3];
     return GemScatterPlotDataBuilder().sets(sets).build().get();
     """;

  String defaultBoxData = """
      List<Double> points1 = [4.0, 7.0, 8.0, 3.0, 1.0, 5.0, 4.0, 5.0];
      GemBoxPlotBox box1 = GemBoxPlotBoxBuilder().median(4.5).lowerQuantile(3.0).upperQuantile(7.0).lowerExtreme(1.0).upperExtreme(8.0).build().get();
      List<Double> points2 = [2.0, 5.0, 3.0, 3.0, 3.0, 8.0, 9.0, 4.0];
      GemBoxPlotBox box2 = GemBoxPlotBoxBuilder().median(3.5).lowerQuantile(3.0).upperQuantile(8.0).lowerExtreme(2.0).upperExtreme(9.0).build().get();
      List<Double> points3 = [4.0, 4.0, 4.0, 5.0, 3.0, 4.0, 5.0, 2.0];
      GemBoxPlotBox box3 = GemBoxPlotBoxBuilder().median(4.0).lowerQuantile(3.0).upperQuantile(5.0).lowerExtreme(2.0).upperExtreme(5.0).build().get();
      GemBoxPlotEntry entry1 = GemBoxPlotEntryBuilder().group("Team 1").color(Optional.of("red")).points(points1).box(box1).build().get();
      GemBoxPlotEntry entry2 = GemBoxPlotEntryBuilder().group("Team 2").color(Optional.of("green")).points(points2).box(box2).build().get();
      GemBoxPlotEntry entry3 = GemBoxPlotEntryBuilder().group("Team 3").color(Optional.of("blue")).points(points3).box(box3).build().get();
      List<GemBoxPlotEntry> entries = [entry1, entry2, entry3];
      return GemBoxPlotDataBuilder().entries(entries).build().get();
      """;

  String defaultPieData = """
     GemPieChartEntry entry1 = GemPieChartEntryBuilder().value(30).label("Team 1").build().get();
     GemPieChartEntry entry2 = GemPieChartEntryBuilder().value(60).label("Team 2").build().get();
     GemPieChartEntry entry3 = GemPieChartEntryBuilder().value(10).label("Team 3").build().get();
     List<GemPieChartEntry> entries = [entry1, entry2, entry3];
     return GemPieChartDataBuilder().entries(entries).build().get();
     """;

  String defaultSpeedData = """
      GemGaugeChartEntry entry1 = GemGaugeChartEntryBuilder().type("#friends").name("Avg Performance Score").value(8).build().get();
      List<GemGaugeChartEntry> entries = [entry1];
      return GemGaugeChartDataBuilder().entries(entries).build().get();
      """;

  String defaultNetworkData = """
      GraphNode node1 = GraphNodeBuilder().id("1").label("Frontend").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(2.5)).size(Optional.of(40)).build().get();
      GraphNode node2 = GraphNodeBuilder().id("2").label("Team 1").fixed(Optional.of(true)).x(Optional.of(1.0)).y(Optional.of(1.0)).size(Optional.of(40)).build().get();
      GraphNode node3 = GraphNodeBuilder().id("3").label("Team 2").fixed(Optional.of(true)).x(Optional.of(4.0)).y(Optional.of(1.0)).size(Optional.of(40)).build().get();
      GraphNode node4 = GraphNodeBuilder().id("4").label("Team 3").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node5 = GraphNodeBuilder().id("5").label("Marc").fixed(Optional.of(true)).x(Optional.of(1.0)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node6 = GraphNodeBuilder().id("6").label("Constantin").fixed(Optional.of(true)).x(Optional.of(4.0)).y(Optional.of(4.0)).size(Optional.of(40)).build().get();
      GraphNode node7 = GraphNodeBuilder().id("7").label("Kelsi").fixed(Optional.of(true)).x(Optional.of(2.5)).y(Optional.of(5.5)).size(Optional.of(40)).build().get();
      List<GraphNode> nodes = [node1, node2, node3, node4, node5, node6, node7];
      GraphEdge edge1 = GraphEdgeBuilder().id("1").source("1").target("2").size(Optional.of(5)).build().get();
      GraphEdge edge2 = GraphEdgeBuilder().id("2").source("1").target("3").size(Optional.of(5)).build().get();
      GraphEdge edge3 = GraphEdgeBuilder().id("3").source("1").target("4").size(Optional.of(5)).build().get();
      GraphEdge edge4 = GraphEdgeBuilder().id("4").source("2").target("5").size(Optional.of(5)).build().get();
      GraphEdge edge5 = GraphEdgeBuilder().id("5").source("2").target("6").size(Optional.of(5)).build().get();
      GraphEdge edge6 = GraphEdgeBuilder().id("6").source("2").target("7").size(Optional.of(5)).build().get();
      List<GraphEdge> edges = [edge1, edge2, edge3, edge4, edge5, edge6];
      return GraphBuilder().nodes(nodes).edges(edges).build().get();
      """;

  //--------------------------
  //default extra data setters
  //--------------------------

  public void setDefaultLineExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .linextra(DashboardModuleManager.lineExtraDataBuilder()
            .minValue(0)
            .maxValue(Optional.empty())
        )
    );
  }

  public void setDefaultBarExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .barextra(DashboardModuleManager.barExtraDataBuilder()
            .maxValue(Optional.empty())
            .minValue(0)
            .overlapping(false)
            .stacked(false)
        )
    );
  }

  public void setDefaultBoxExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .boxextra(DashboardModuleManager.boxExtraDataBuilder()
            .min(0d)
            .max(Optional.empty())
        )
    );
  }

  public void setDefaultSpeedExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .speedextra(DashboardModuleManager.gaugeExtraDataBuilder()
            .max(Optional.empty())
            .min(0)
            .unit("Unit")
            .textPosition("bottom")
            .textValue("Speedometer description")
        )
    );
  }

  public void setDefaultScatterExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .scatterextra(DashboardModuleManager.scatterExtraDataBuilder()
            .xAxis(DashboardModuleManager.axisBuilder()
                .label("X-Axis Label")
                .stepWidth(999d)
                .maxValue(Optional.empty())
                .minValue(0d)
            )
            .yAxis(DashboardModuleManager.axisBuilder()
                .label("Y-Axis Label")
                .stepWidth(999d)
                .maxValue(Optional.empty())
                .minValue(0d)
            )
        )
    );
  }

  public void setDefaultPieExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .piextra(DashboardModuleManager.pieExtraDataBuilder()
            .innerRadius(10)
        )
    );
  }

  public void setDefaultGraphExtra(CellConfig cell) {
    cell.setExtra(DashboardModuleManager.extraDataBuilder()
        .graphextra(DashboardModuleManager.graphExtraDataBuilder()
            .minHeight("50vH")
            .showFilterGroup(false)
            .showSearchNode(false)
            .showControlButtons(false)
            .showInformation(false)
        )
    );
  }

}
