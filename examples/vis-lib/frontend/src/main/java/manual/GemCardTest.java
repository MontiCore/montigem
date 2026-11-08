/* (c) https://github.com/MontiCore/monticore */
package manual;

import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GemCardTest extends GemCardTestTOP {

  @Override
  public GemPieChartData getPieChartData() {
    List<GemPieChartEntry> dataEntries = new ArrayList<>();
    dataEntries.add(new GemPieChartEntry(45, "Available", Optional.empty()));
    dataEntries.add(new GemPieChartEntry(30, "Rented", Optional.of("#57ab27")));
    dataEntries.add(new GemPieChartEntry(25, "In repair", Optional.empty()));
    return new GemPieChartDataBuilder().entries(dataEntries).build().get();
  }
}
