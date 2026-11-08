/* (c) https://github.com/MontiCore/monticore */
package manual;

import com.google.common.collect.Lists;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBoxBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntry;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntryBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartDataBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntryBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import mc.fenix.charts.gemradarcharttypes.GemRadarChartData;
import mc.fenix.charts.gemradarcharttypes.GemRadarChartDataBuilder;
import mc.fenix.charts.gemradarcharttypes.GemRadarChartDataSetBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotDataBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPoint;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSetBuilder;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstData;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstDataBuilder;
import mc.fenix.charts.gemsunburstcharttypes.GemSunburstEntry;
import mc.fenix.charts.gemtimelinecharttypes.Timeline;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBlock;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBlockBuilder;
import mc.fenix.charts.gemtimelinecharttypes.TimelineBuilder;
import mc.fenix.charts.gemtimelinecharttypes.TimelineData;
import mc.fenix.charts.gemtimelinecharttypes.TimelineDataBuilder;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeries;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeriesData;
import mc.fenix.charts.gemtimeseriescharttypes.TimeSeriesDataPoint;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramData;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramDataBuilder;
import mc.fenix.charts.gemvenndiagramtypes.GemVennDiagramEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Static test data for the charts in LegendPositionCharts. Every chart has at
 * least two legend entries. The data is created once, so that change detection does
 * not render the charts again and again.
 */
@Component
public class LegendPositionCharts extends LegendPositionChartsTOP {

  private GemBarChartData barData;
  private GemBoxPlotData boxPlotData;
  private GemLineChartData lineData;
  private GemPieChartData pieData;
  private GemRadarChartData radarData;
  private GemScatterPlotData scatterData;
  private GemSunburstData sunburstData;
  private TimeSeriesData timeSeriesData;
  private TimelineData timelineData;
  private GemVennDiagramData vennData;

  @Override
  public GemBarChartData computeBarData() {
    if (barData == null) {
      List<GemBarChartEntry> entries = new ArrayList<>();
      entries.add(new GemBarChartEntryBuilder().label(Optional.of("Series A"))
          .data(Arrays.asList(10, 15, 20)).build().get());
      entries.add(new GemBarChartEntryBuilder().label(Optional.of("Series B"))
          .data(Arrays.asList(5, 8, 12)).build().get());
      barData = new GemBarChartData(entries, Arrays.asList("2024", "2025", "2026"));
    }
    return barData;
  }

  @Override
  public GemBoxPlotData computeBoxPlotData() {
    if (boxPlotData == null) {
      boxPlotData = new GemBoxPlotDataBuilder().entries(Arrays.asList(
          boxPlotEntry("Group A", "#69b3a2", 4d, 11d, 13d, 18d, 22d),
          boxPlotEntry("Group B", "#f6a800", 2d, 8d, 10d, 14d, 19d)
      )).build().get();
    }
    return boxPlotData;
  }

  private GemBoxPlotEntry boxPlotEntry(String group, String color, double min, double lower, double median,
                                       double upper, double max) {
    return new GemBoxPlotEntryBuilder()
        .group(group)
        .color(Optional.of(color))
        .points(Arrays.asList(min, lower, median, upper, max))
        .box(new GemBoxPlotBoxBuilder()
            .median(median)
            .lowerQuantile(lower)
            .upperQuantile(upper)
            .lowerExtreme(min)
            .upperExtreme(max)
            .build().get())
        .build().get();
  }

  @Override
  public GemLineChartData computeLineData() {
    if (lineData == null) {
      lineData = new GemLineChartDataBuilder()
          .entries(Lists.newArrayList(
              new GemLineChartEntryBuilder().label("Series A")
                  .data(Lists.newArrayList(10.0, 20.0, 15.0, 30.0)).build().get(),
              new GemLineChartEntryBuilder().label("Series B")
                  .data(Lists.newArrayList(5.0, 15.0, 10.0, 20.0)).build().get()))
          .labels(Lists.newArrayList("Q1", "Q2", "Q3", "Q4"))
          .build().get();
    }
    return lineData;
  }

  @Override
  public GemPieChartData computePieData() {
    if (pieData == null) {
      List<GemPieChartEntry> entries = new ArrayList<>();
      entries.add(new GemPieChartEntry(45, "Available", Optional.empty()));
      entries.add(new GemPieChartEntry(30, "Rented", Optional.of("#57ab27")));
      entries.add(new GemPieChartEntry(25, "In repair", Optional.empty()));
      pieData = new GemPieChartDataBuilder().entries(entries).build().get();
    }
    return pieData;
  }

  @Override
  public GemRadarChartData computeRadarData() {
    if (radarData == null) {
      radarData = new GemRadarChartDataBuilder()
          .labels(Arrays.asList("Speed", "Quality", "Price", "Support", "Design"))
          .dataSets(Arrays.asList(
              new GemRadarChartDataSetBuilder().label("Current State")
                  .data(Arrays.asList(3d, 2d, 4d, 1d, 2d)).build().get(),
              new GemRadarChartDataSetBuilder().label("Target State")
                  .data(Arrays.asList(5d, 3d, 4d, 2d, 3d)).build().get()))
          .build().get();
    }
    return radarData;
  }

  @Override
  public GemScatterPlotData computeScatterData() {
    if (scatterData == null) {
      scatterData = new GemScatterPlotDataBuilder().sets(Lists.newArrayList(
          new GemScatterPlotSetBuilder().label("Set A").color(Optional.of("green"))
              .points(Lists.newArrayList(
                  new GemScatterPlotPoint(7.0, 5.0, "", Optional.empty()),
                  new GemScatterPlotPoint(9.0, 6.0, "", Optional.empty()))).build().get(),
          new GemScatterPlotSetBuilder().label("Set B").color(Optional.of("blue"))
              .points(Lists.newArrayList(
                  new GemScatterPlotPoint(3.0, 3.0, "", Optional.empty()),
                  new GemScatterPlotPoint(4.0, 7.0, "", Optional.empty()))).build().get()
      )).build().get();
    }
    return scatterData;
  }

  @Override
  public GemSunburstData computeSunburstData() {
    if (sunburstData == null) {
      sunburstData = new GemSunburstDataBuilder()
          .entry(new GemSunburstEntry("All", 100))
          .subEntries(Lists.newArrayList(
              sunburstLeaf("Part A", 45),
              sunburstLeaf("Part B", 30),
              sunburstLeaf("Part C", 25)))
          .build().get();
    }
    return sunburstData;
  }

  private GemSunburstData sunburstLeaf(String label, int value) {
    return new GemSunburstDataBuilder().entry(new GemSunburstEntry(label, value))
        .subEntries(new ArrayList<>()).build().get();
  }

  @Override
  public TimeSeriesData computeTimeSeriesData() {
    if (timeSeriesData == null) {
      TimeSeriesData data = new TimeSeriesData();
      LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      data.addSeries(timeSeries(1L, "Sine Wave", "#008FFB", start, true));
      data.addSeries(timeSeries(2L, "Cosine Wave", "#00E396", start, false));
      timeSeriesData = data;
    }
    return timeSeriesData;
  }

  private TimeSeries timeSeries(long id, String name, String color, LocalDateTime start, boolean sine) {
    TimeSeries series = new TimeSeries();
    series.setName(name);
    series.setIdentifier(id);
    series.setColor(Optional.of(color));
    for (int i = 0; i < 30; i++) {
      TimeSeriesDataPoint point = new TimeSeriesDataPoint();
      point.setTimestamp(start.plusMinutes(i));
      point.setValue(50.0 + 30.0 * (sine ? Math.sin(i * 0.2) : Math.cos(i * 0.2)));
      series.addDatapoints(point);
    }
    return series;
  }

  @Override
  public TimelineData computeTimelineData() {
    if (timelineData == null) {
      String[] colors = {"red", "green", "blue"};
      List<Timeline> timelines = new ArrayList<>();
      for (int i = 0; i < colors.length; i++) {
        List<TimelineBlock> blocks = new ArrayList<>();
        blocks.add(new TimelineBlockBuilder()
            .start(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
            .end(LocalDateTime.of(2026, 3 + i, 1, 0, 0, 0))
            .lane("Planning").build().get());
        blocks.add(new TimelineBlockBuilder()
            .start(LocalDateTime.of(2026, 3 + i, 1, 0, 0, 0))
            .end(LocalDateTime.of(2026, 6 + i, 1, 0, 0, 0))
            .lane("Execution").build().get());
        timelines.add(new TimelineBuilder()
            .color(Optional.of(colors[i]))
            .name("Project " + (i + 1))
            .identifier((long) i)
            .deactivated(false)
            .hidden(false)
            .block(blocks)
            .build().get());
      }
      timelineData = new TimelineDataBuilder().timeLine(timelines).build().get();
    }
    return timelineData;
  }

  @Override
  public GemVennDiagramData computeVennData() {
    if (vennData == null) {
      vennData = new GemVennDiagramDataBuilder().entries(Lists.newArrayList(
          new GemVennDiagramEntryBuilder().sets(Lists.newArrayList("A")).label("A").size(12).build().get(),
          new GemVennDiagramEntryBuilder().sets(Lists.newArrayList("B")).label("B").size(10).build().get(),
          new GemVennDiagramEntryBuilder().sets(Lists.newArrayList("C")).label("C").size(8).build().get(),
          new GemVennDiagramEntryBuilder().sets(Lists.newArrayList("A", "B")).label("").size(3).build().get(),
          new GemVennDiagramEntryBuilder().sets(Lists.newArrayList("B", "C")).label("").size(2).build().get()
      )).build().get();
    }
    return vennData;
  }
}
