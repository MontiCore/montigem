/* (c) https://github.com/MontiCore/monticore */
package manual;

import mc.fenix.charts.gemcandlestickcharttypes.GemCandlestickChartData;
import mc.fenix.charts.gemcandlestickcharttypes.GemCandlestickChartDataBuilder;
import mc.fenix.charts.gemcandlestickcharttypes.GemCandlestickChartEntry;
import mc.fenix.charts.gemcandlestickcharttypes.GemCandlestickChartEntryBuilder;
import umlp.jsweet.extension.annotation.Component;

import java.util.Arrays;

@Component
public class GemCandlestickChartTest extends GemCandlestickChartTestTOP {

  /**
   * Provides sample data for the candlestick chart.
   * This method is called by the GUI file via "data = getData()".
   */
  @Override
  public GemCandlestickChartData getData() {

    // The expected order of values is: open, high, low, close
    // We use the ISO format YYYY-MM-DD for the date labels.

    GemCandlestickChartEntry entry1 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-01")
        .value(Arrays.asList(150f, 165f, 145f, 160f))
        .build().get();

    GemCandlestickChartEntry entry2 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-02")
        .value(Arrays.asList(160f, 170f, 155f, 168f))
        .build().get();

    GemCandlestickChartEntry entry3 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-03")
        .value(Arrays.asList(168f, 175f, 160f, 162f))
        .build().get();

    GemCandlestickChartEntry entry4 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-04")
        .value(Arrays.asList(162f, 165f, 140f, 145f)) // Sharp drop
        .build().get();

    GemCandlestickChartEntry entry5 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-05")
        .value(Arrays.asList(145f, 155f, 142f, 150f))
        .build().get();

    GemCandlestickChartEntry entry6 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-06")
        .value(Arrays.asList(150f, 180f, 148f, 175f)) // Strong recovery
        .build().get();

    GemCandlestickChartEntry entry7 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-07")
        .value(Arrays.asList(175f, 185f, 170f, 180f))
        .build().get();

    GemCandlestickChartEntry entry8 = new GemCandlestickChartEntryBuilder()
        .label("2026-03-08")
        .value(Arrays.asList(180f, 195f, 175f, 190f))
        .build().get();

    // Compile the entries into a single data object to be sent to the frontend
    return new GemCandlestickChartDataBuilder()
        .entries(Arrays.asList(entry1, entry2, entry3, entry4, entry5, entry6, entry7, entry8))
        .build().get();
  }
}
