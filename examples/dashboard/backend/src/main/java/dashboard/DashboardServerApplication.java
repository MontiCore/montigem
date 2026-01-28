/* (c) https://github.com/MontiCore/monticore */
package dashboard;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import dashboardmodule.commands.InterpreterWithErrorHandling;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static dashboardmodule.VizType.BAR;
import static dashboardmodule.VizType.LINE;
import static dashboardmodule.VizType.NETWORK;
import static dashboardmodule.VizType.PIE;
import static dashboardmodule.VizType.SCATTER;

@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class},
    scanBasePackages = {"umlp.backendrte.service.websocket", "umlp.backendrte.service.rest", "service"}
)
public class DashboardServerApplication extends DashboardServerApplicationTOP {

  public static void main(String[] args) {
    SpringApplication.run(DashboardServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();
    // Allow the data scripts of the dashboard cells to access the example domain (e.g., DashboardManager.getOrganizationList())
    InterpreterWithErrorHandling.addDomain("dashboard", DashboardManager.class);
    initUseCaseData();
    initEntities();
  }
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

  String realisticBarData = """
      List<Team> teams = DashboardManager.getOrganizationList().get(0).getTeamsList();

      List<String> labels = [""];
      List<GemBarChartEntry> entries = [];

      for (Team team : teams) {
          int completedTasks = 0;

          for (Task task : team.getTasksList()) {
              if (task.isCompleted()) {
                  completedTasks++;
              }
          }

          List<Integer> data = [completedTasks];

          GemBarChartEntry entry = GemBarChartEntryBuilder()
              .label(Optional.of(team.getName()))
              .data(data)
              .build()
              .get();

          entries.add(entry);
      }

      return GemBarChartDataBuilder()
          .entries(entries)
          .labels(labels)
          .build()
          .get();
      """;

  String realisticPieData = """
      List<Team> teams = DashboardManager.getOrganizationList().get(0).getTeamsList();

      int below25 = 0;
      int between25And35 = 0;
      int above35 = 0;

      for (Team team : teams) {
          for (Person person : team.getPeopleList()) {
              int age = person.getAge();

              if (age < 25) {
                  below25++;
              } else if (age <= 35) {
                  between25And35++;
              } else {
                  above35++;
              }
          }
      }

      GemPieChartEntry entry1 = GemPieChartEntryBuilder()
          .value(below25)
          .label("<25")
          .build()
          .get();

      GemPieChartEntry entry2 = GemPieChartEntryBuilder()
          .value(between25And35)
          .label("25-35")
          .build()
          .get();

      GemPieChartEntry entry3 = GemPieChartEntryBuilder()
          .value(above35)
          .label(">35")
          .build()
          .get();

      List<GemPieChartEntry> entries = [entry1, entry2, entry3];

      return GemPieChartDataBuilder()
          .entries(entries)
          .build()
          .get();
      """;

  String realisticLineData = """
      List<Team> teams = DashboardManager.getOrganizationList().get(0).getTeamsList();

      List<String> labels = [];

      List<DataPoint> firstTeamIssues = teams.get(0).getPeopleList().get(0).getStatistics().getNumberOfIssuesClosedList();

      int numberOfWeeks = firstTeamIssues.size();

      for (int i = 0; i < numberOfWeeks; i++) {
        labels.add(firstTeamIssues.get(i).getTime());
      }

      List<GemLineChartEntry> entries = [];

      for (Team team : teams) {
        List<Double> teamAverages = [];

        for (int week = 0; week < numberOfWeeks; week++) {

          double totalIssues = 0.0;
          int peopleCount = 0;

          for (Person person : team.getPeopleList()) {
            List<DataPoint> issuesHistory = person.getStatistics().getNumberOfIssuesClosedList();

            totalIssues += issuesHistory.get(week).getValue();
            peopleCount++;
          }

          if (peopleCount > 0) {
            teamAverages.add(totalIssues / peopleCount);
          } else {
            teamAverages.add(0.0);
          }
        }

        GemLineChartEntry entry = GemLineChartEntryBuilder()
            .label(team.getName())
            .data(teamAverages)
            .build()
            .get();

        entries.add(entry);
      }

      return GemLineChartDataBuilder()
          .entries(entries)
          .labels(labels)
          .build()
          .get();
      """;

  String realisticScatterData = """
      List<Team> teams = DashboardManager.getOrganizationList().get(0).getTeamsList();

      Team team = teams.get(0);
      List<GemScatterPlotSet> sets = [];

      for (Person person : team.getPeopleList()) {

          List<DataPoint> issuesHistory = person.getStatistics().getNumberOfIssuesClosedList();

          List<GemScatterPlotPoint> points = [];
          int week = 1;

          for (DataPoint issues : issuesHistory) {

              GemScatterPlotPoint point = GemScatterPlotPointBuilder()
                  .x(week * 1.0)
                  .y(issues.getValue() * 1.0)
                  .label(person.getName())
                  .pointRadius(Optional.of(8))
                  .build()
                  .get();

              points.add(point);
              week++;
          }

          GemScatterPlotSet set = GemScatterPlotSetBuilder()
              .label(person.getName())
              .points(points)
              .build()
              .get();
          sets.add(set);
      }

      return GemScatterPlotDataBuilder()
          .sets(sets)
          .build()
          .get();
      """;

  public void initEntities() throws IOException {
    // Addding CellConfigs to more than one ViewConfig does not make sense, so we create new ones.
    Set<CellConfig> cells1 = createCellConfigsForNiceLookingDashboard();
    Set<CellConfig> cells2 = createCellConfigForRealisticDashboard();
    ViewConfig view1 = DashboardModuleManager.viewConfigBuilder()
        .name("View 1")
        .vid(0)
        .cells(cells1)
        .build()
        .get();
    Set<ViewConfig> views = new HashSet<ViewConfig>();
    ViewConfig view2 = DashboardModuleManager.viewConfigBuilder()
        .name("View 2")
        .vid(1)
        .cells(cells2)
        .build()
        .get();
    views.add(view1);
    views.add(view2);

    DashboardModuleConfig data = DashboardModuleManager.dashboardModuleConfigBuilder()
        .views(views)
        .colormode("light")
        .build()
        .get();
  }

  public Set<CellConfig> createCellConfigForRealisticDashboard() {
    Set<CellConfig> cells = new HashSet<CellConfig>();

    CellConfig cell0 = DashboardModuleManager.cellConfigBuilder()
        //.cid(1)
        .data(realisticBarData)
        .name("Tasks Completed Per Teams")
        .viz(BAR)
        .extra(DashboardModuleManager.extraDataBuilder()
            .barextra(DashboardModuleManager.barExtraDataBuilder()
                .minValue(0)
                .maxValue(Optional.empty())
                .overlapping(false)
                .stacked(false)
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(4.0)
            .width(4.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(0.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();

    CellConfig cell1 = DashboardModuleManager.cellConfigBuilder()
        //.cid(2)
        .data(realisticLineData)
        .name("Avg No. Of Issues Closed")
        .viz(LINE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .linextra(DashboardModuleManager.lineExtraDataBuilder()
                .minValue(0)
                .maxValue(Optional.empty())
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(4.0)
            .width(5.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(8.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();

    CellConfig cell2 = DashboardModuleManager.cellConfigBuilder()
        //.cid(3)
        .data(realisticPieData)
        .name("Age Distribution Of Organization")
        .viz(PIE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .piextra(DashboardModuleManager.pieExtraDataBuilder()
                .innerRadius(0)
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(4.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(4.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();

    CellConfig cell3 = DashboardModuleManager.cellConfigBuilder()
        //.cid(4)
        .data(realisticScatterData)
        .name("Issues Closed")
        .viz(SCATTER)
        .extra(DashboardModuleManager.extraDataBuilder()
            .scatterextra(DashboardModuleManager.scatterExtraDataBuilder()
                .xAxis(DashboardModuleManager.axisBuilder()
                    .label("Week")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get())
                .yAxis(DashboardModuleManager.axisBuilder()
                    .label("Issues Closed")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get())
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(4.0)
            .width(7.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(0.0)
            .y(4.0)
            .build()
            .get())
        .build()
        .get();

    cells.add(cell0);
    cells.add(cell1);
    cells.add(cell2);
    cells.add(cell3);

    return cells;
  }

  public Set<CellConfig> createCellConfigsForNiceLookingDashboard() {
    Set<CellConfig> cells = new HashSet<CellConfig>();
    String dataString = "";
    CellConfig cell0 = DashboardModuleManager.cellConfigBuilder()
        //.cid(1)
        .data(defaultNetworkData)
        .name("Group Structure")
        .viz(NETWORK)
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(6.0)
            .width(6.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(0.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell1 = DashboardModuleManager.cellConfigBuilder()
        //.cid(2)
        .data(defaultBarData)
        .name("Lines of Code")
        .viz(BAR)
        .extra(DashboardModuleManager.extraDataBuilder()
            .barextra(DashboardModuleManager.barExtraDataBuilder()
                .maxValue(Optional.empty())//999
                .minValue(0)
                .overlapping(false)
                .stacked(false)
                .build()
                .get()
            )
            .build()
            .get()
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(3.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(6.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell2 = DashboardModuleManager.cellConfigBuilder()
        //.cid(3)
        .data(defaultLineData)
        .name("No. of Issues Closed")
        .viz(LINE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .linextra(DashboardModuleManager.lineExtraDataBuilder()
                .minValue(0)
                .maxValue(Optional.empty())//999
                .build()
                .get()
            )
            .build()
            .get()
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(3.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(6.0)
            .y(3.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell3 = DashboardModuleManager.cellConfigBuilder()
        //.cid(4)
        .data(defaultPieData)
        .name("No. of Commits")
        .viz(PIE)
        .extra(DashboardModuleManager.extraDataBuilder()
            .piextra(DashboardModuleManager.pieExtraDataBuilder()
                .innerRadius(0)
                .build()
                .get())
            .build()
            .get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(3.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(9.0)
            .y(0.0)
            .build()
            .get())
        .build()
        .get();
    CellConfig cell4 = DashboardModuleManager.cellConfigBuilder()
        //.cid(5)
        .data(defaultScatterData)
        .name("No. of issues closed")
        .viz(SCATTER)
        .extra(DashboardModuleManager.extraDataBuilder()
            .scatterextra(DashboardModuleManager.scatterExtraDataBuilder()
                .xAxis(DashboardModuleManager.axisBuilder()
                    .label("X-Axis Label")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get()
                )
                .yAxis(DashboardModuleManager.axisBuilder()
                    .label("Y-Axis Label")
                    .stepWidth(999d)
                    .maxValue(Optional.empty())
                    .minValue(0d)
                    .build()
                    .get()
                )
                .build()
                .get()
            )
        )
        .size(DashboardModuleManager.dashboardModuleSizeBuilder()
            .length(3.0)
            .width(3.0)
            .build()
            .get())
        .position(DashboardModuleManager.dashboardModulePositionBuilder()
            .x(9.0)
            .y(3.0)
            .build()
            .get())
        .build()
        .get();
    cells.add(cell0);
    cells.add(cell1);
    cells.add(cell2);
    cells.add(cell3);
    cells.add(cell4);
    return cells;
  }

  //to build a use case for study and thesis
  public void initUseCaseData() {
      //statistics
    Statistics statsAlice = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(9).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(13).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(14).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(15).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(15).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(16).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(17).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(9).build().get()
        ))
        .build().get();

    Statistics statsBob = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(12).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(13).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(13).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(2).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .build().get();

    Statistics statsCharlie = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(5).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(10).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .build().get();

    Statistics statsDiana = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(5).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(8).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(0).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(0).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(10).build().get()
        ))
        .build().get();

    Statistics statsEmma = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(8).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(12).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(12).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(13).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(14).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(15).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(2).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(8).build().get()
        ))
        .build().get();

    Statistics statsFrank = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(6).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(12).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(6).build().get()
        ))
        .build().get();

    Statistics statsGrace = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(2).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .build().get();

    Statistics statsHenry = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(10).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(14).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(15).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(15).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(16).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(17).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(18).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(10).build().get()
        ))
        .build().get();

    Statistics statsIrene = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(6).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(12).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(12).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(13).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(2).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .build().get();

    Statistics statsJack = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(8).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(9).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(1).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(2).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .build().get();

    Statistics statsKaren = DashboardManager.statisticsBuilder()
        .numberOfIssuesClosed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(3).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(4).build().get()
        ))
        .numberOfCommits(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(4).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(5).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(7).build().get()
        ))
        .numberOfDocumentationPages(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(6).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(7).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(8).build().get()
        ))
        .numberOfBugsReported(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(1).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(0).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(0).build().get()
        ))
        .numberOfPullRequestsReviewed(Set.of(
            DashboardManager.dataPointBuilder().time("Week 1").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 2").value(9).build().get(),
            DashboardManager.dataPointBuilder().time("Week 3").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 4").value(10).build().get(),
            DashboardManager.dataPointBuilder().time("Week 5").value(11).build().get(),
            DashboardManager.dataPointBuilder().time("Week 6").value(12).build().get()
        ))
        .build().get();
      //people
      Person alice = DashboardManager.personBuilder()
              .name("Alice")
              .role(Role.SENIOR)
              .tenure(7)
              .education(Education.MASTER)
              .international(false)
              .age(33)
              .statistics(statsAlice)
              .build().get();

      Person bob = DashboardManager.personBuilder()
              .name("Bob")
              .role(Role.MID)
              .tenure(3)
              .education(Education.BACHELOR)
              .international(true)
              .age(27)
              .statistics(statsBob)
              .build().get();

      Person charlie = DashboardManager.personBuilder()
              .name("Charlie")
              .role(Role.JUNIOR)
              .tenure(1)
              .education(Education.BACHELOR)
              .international(false)
              .age(24)
              .statistics(statsCharlie)
              .build().get();

      Person diana = DashboardManager.personBuilder()
              .name("Diana")
              .role(Role.MANAGER)
              .tenure(10)
              .education(Education.PHD)
              .international(false)
              .age(39)
              .statistics(statsDiana)
              .build().get();

      Person emma = DashboardManager.personBuilder()
              .name("Emma")
              .role(Role.SENIOR)
              .tenure(6)
              .education(Education.MASTER)
              .international(false)
              .age(31)
              .statistics(statsEmma)
              .build().get();

      Person frank = DashboardManager.personBuilder()
              .name("Frank")
              .role(Role.MID)
              .tenure(4)
              .education(Education.BACHELOR)
              .international(true)
              .age(28)
              .statistics(statsFrank)
              .build().get();

      Person grace = DashboardManager.personBuilder()
              .name("Grace")
              .role(Role.JUNIOR)
              .tenure(1)
              .education(Education.BACHELOR)
              .international(false)
              .age(23)
              .statistics(statsGrace)
              .build().get();

      Person henry = DashboardManager.personBuilder()
              .name("Henry")
              .role(Role.SENIOR)
              .tenure(8)
              .education(Education.MASTER)
              .international(false)
              .age(35)
              .statistics(statsHenry)
              .build().get();

      Person irene = DashboardManager.personBuilder()
              .name("Irene")
              .role(Role.MID)
              .tenure(5)
              .education(Education.MASTER)
              .international(true)
              .age(30)
              .statistics(statsIrene)
              .build().get();

      Person jack = DashboardManager.personBuilder()
              .name("Jack")
              .role(Role.JUNIOR)
              .tenure(2)
              .education(Education.BACHELOR)
              .international(false)
              .age(25)
              .statistics(statsJack)
              .build().get();

      Person karen = DashboardManager.personBuilder()
              .name("Karen")
              .role(Role.MANAGER)
              .tenure(12)
              .education(Education.PHD)
              .international(false)
              .age(42)
              .statistics(statsKaren)
              .build().get();

      //tasks
      Task task1 = DashboardManager.taskBuilder()
              .name("Cloud Infrastructure Migration")
              .description("Migrate core services from virtual machines to a Kubernetes cluster.")
              .completed(true)
              .build().get();

      Task task2 = DashboardManager.taskBuilder()
              .name("Authentication System Upgrade")
              .description("Replace the legacy login system with OAuth 2.0 and multi-factor authentication.")
              .completed(true)
              .build().get();

      Task task3 = DashboardManager.taskBuilder()
              .name("Quarterly Security Audit")
              .description("Perform vulnerability scanning, penetration testing, and dependency reviews.")
              .completed(true)
              .build().get();

      Task task4 = DashboardManager.taskBuilder()
              .name("Automated Testing Expansion")
              .description("Increase integration and end-to-end test coverage for all core services.")
              .completed(false)
              .build().get();

      Task task5 = DashboardManager.taskBuilder()
              .name("Monitoring & Observability")
              .description("Deploy centralized logging, metrics dashboards, and alerting across all services.")
              .completed(true)
              .build().get();

      //teams
      Team platformEngineering = DashboardManager.teamBuilder()
              .name("Platform Engineering")
              .department(Department.DEVOPS)
              .people(Set.of(
                      alice,
                      bob,
                      charlie,
                      diana
              ))
              .tasks(Set.of(
                      task1,
                      task5
              ))
              .build().get();


      Team productDevelopment = DashboardManager.teamBuilder()
              .name("Product Development")
              .department(Department.BACKEND)
                .people(Set.of(
                      emma,
                      frank,
                      grace,
                      henry
              ))
              .tasks(Set.of(
                      task2,
                      task4
              ))
              .build().get();


      Team securityAndQA = DashboardManager.teamBuilder()
              .name("Security & QA")
              .department(Department.SECURITY)
              .people(Set.of(
                      irene,
                      jack,
                      karen
              ))
              .tasks(Set.of(
                      task3
              ))
              .build().get();

      //org
      Organization organization = DashboardManager.organizationBuilder()
              .name("NovaTech Solutions")
              .teams(Set.of(
                      platformEngineering,
                      productDevelopment,
                      securityAndQA
              ))
              .build().get();
  }
}
