/* (c) https://github.com/MontiCore/monticore */
package manual;

import mc.fenix.charts.gemheatmapcharttypes.GemHeatmapChartData;
import mc.fenix.charts.gemheatmapcharttypes.GemHeatmapChartEntry;
import mc.fenix.charts.gemheatmapcharttypes.GemHeatmapChartEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GemHeatmapChartTest extends GemHeatmapChartTestTOP {

  @Override
  public GemHeatmapChartData getData() {
    List<GemHeatmapChartEntry> entries = new ArrayList<>();
    // Generate 500 random points to show a distribution
    // Use Math.random() for JSweet compatibility
    for (int i = 0; i < 500; i++) {
      // Create some clusters
      double cluster = Math.random();
      double temp, speed;
      if (cluster < 0.3) {
        temp = 30 + (Math.random() * 10 - 5);
        speed = 1000 + (Math.random() * 400 - 200);
      } else if (cluster < 0.7) {
        temp = 60 + (Math.random() * 20 - 10);
        speed = 2000 + (Math.random() * 600 - 300);
      } else {
        temp = 80 + (Math.random() * 10 - 5);
        speed = 1500 + (Math.random() * 200 - 100);
      }
      entries.add(new GemHeatmapChartEntryBuilder()
          .xValue(temp)
          .yValue(speed)
          .build().get());
    }
    return new GemHeatmapChartData(entries);
  }

}
