/* (c) https://github.com/MontiCore/monticore */
package manual;

import com.google.common.collect.Lists;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotDataBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPoint;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPointBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSet;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSetBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GemScatterPlotTest extends GemScatterPlotTestTOP {
  public GemScatterPlotData getSimpleData() {

    List<GemScatterPlotSet> entries = new ArrayList<>();
    List<GemScatterPlotPoint> points = Lists.newArrayList(
        new GemScatterPlotPointBuilder().x(7.0).y(5.0).label("").build().get(),
        new GemScatterPlotPointBuilder().x(9.0).y(6.0).label("").build().get(),
        new GemScatterPlotPointBuilder().x(10.0).y(7.0).label("").build().get());
    Optional<GemScatterPlotSet> greenSet = new GemScatterPlotSetBuilder().label("Green data")
        .color(Optional.of("green")).points(points).build();
    greenSet.ifPresent(e -> entries.add(e));

    points = Lists.newArrayList(new GemScatterPlotPoint(3.0, 3.0, "", Optional.empty()));
    Optional<GemScatterPlotSet> blueSet = new GemScatterPlotSetBuilder().label("Blue data")
        .color(Optional.of("blue")).points(points).build();
    blueSet.ifPresent(e -> entries.add(e));

    return new GemScatterPlotDataBuilder().sets(entries).build().get();
  }
}
