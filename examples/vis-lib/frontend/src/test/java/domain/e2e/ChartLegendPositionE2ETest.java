/* (c) https://github.com/MontiCore/monticore */
package domain.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.WaitForSelectorState;
import domain.test_infrastructure.AbstractDomainE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the legendPosition attribute places the legend of every chart on the
 * requested side and that the legend stays inside the chart.
 * Uses the page manual.GemLegendPositionTest, which shows every chart with a legend
 * once per legend position.
 */
public class ChartLegendPositionE2ETest extends AbstractDomainE2ETest {

  /** Tolerance for sub-pixel rounding. */
  private static final double DELTA = 2.5;

  /** Plotly draws the legend into the SVG, ApexCharts and gem-chart-legend use HTML. */
  private static final String PLOTLY_LEGEND = "g.legend";
  private static final String APEX_LEGEND = ".apexcharts-legend";
  private static final String GEM_LEGEND = "gem-chart-legend .gem-legend";

  /** class name of the chart in manual.LegendPositionCharts and the selector of its legend */
  private static final String[][] CHARTS = {
      {"barChart", PLOTLY_LEGEND},
      {"boxPlot", PLOTLY_LEGEND},
      {"lineChart", PLOTLY_LEGEND},
      {"pieChart", PLOTLY_LEGEND},
      {"radarChart", PLOTLY_LEGEND},
      {"scatterPlot", PLOTLY_LEGEND},
      {"sunburstChart", GEM_LEGEND},
      {"timeSeriesChart", APEX_LEGEND},
      {"timelineChart", PLOTLY_LEGEND},
      {"vennDiagram", GEM_LEGEND},
  };

  @BeforeEach
  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/manual/GemLegendPositionTest");
  }

  @ParameterizedTest(name = "legend {0}")
  @ValueSource(strings = {"left", "top", "right", "bottom"})
  void testLegendIsOnRequestedSide(String position, Page page) {
    String section = ".legend" + Character.toUpperCase(position.charAt(0)) + position.substring(1);
    List<String> errors = new ArrayList<>();
    for (String[] chart : CHARTS) {
      // the timeline chart has no legend on the left (the lane labels are there), it falls back to the right
      String expected = chart[0].equals("timelineChart") && position.equals("left") ? "right" : position;
      errors.addAll(checkLegend(page, section + " ." + chart[0], chart[1], chart[0], expected));
    }
    assertTrue(errors.isEmpty(), String.join("\n", errors));
  }

  private List<String> checkLegend(Page page, String chartSelector, String legendSelector, String chartName,
                                   String expected) {
    List<String> errors = new ArrayList<>();
    Locator chart = page.locator(chartSelector).first();
    chart.scrollIntoViewIfNeeded();
    Locator legend = chart.locator(legendSelector).first();
    try {
      legend.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
    } catch (RuntimeException e) {
      errors.add(chartName + " (" + expected + "): no visible legend");
      return errors;
    }
    // Plotly and ApexCharts may lay out the legend again after the first render
    page.waitForTimeout(500);

    BoundingBox chartBox = chart.boundingBox();
    BoundingBox legendBox = legend.boundingBox();
    String info = chartName + " (" + expected + "): chart " + describe(chartBox) + ", legend " + describe(legendBox);

    double chartCenterX = chartBox.x + chartBox.width / 2;
    double chartCenterY = chartBox.y + chartBox.height / 2;
    double legendCenterX = legendBox.x + legendBox.width / 2;
    double legendCenterY = legendBox.y + legendBox.height / 2;

    switch (expected) {
      case "left":
        check(errors, legendCenterX < chartCenterX && legendBox.width < chartBox.width / 2,
            "legend is not left of the plot: " + info);
        break;
      case "right":
        check(errors, legendCenterX > chartCenterX && legendBox.width < chartBox.width / 2,
            "legend is not right of the plot: " + info);
        break;
      case "top":
        check(errors, legendCenterY < chartCenterY && legendBox.height < chartBox.height / 2,
            "legend is not above the plot: " + info);
        break;
      case "bottom":
        check(errors, legendCenterY > chartCenterY && legendBox.height < chartBox.height / 2,
            "legend is not below the plot: " + info);
        break;
      default:
        throw new IllegalStateException(expected);
    }

    // the legend must not stick out of the chart
    check(errors, legendBox.x >= chartBox.x - DELTA, "legend overflows the LEFT border: " + info);
    check(errors, legendBox.x + legendBox.width <= chartBox.x + chartBox.width + DELTA,
        "legend overflows the RIGHT border: " + info);
    check(errors, legendBox.y >= chartBox.y - DELTA, "legend overflows the TOP border: " + info);
    check(errors, legendBox.y + legendBox.height <= chartBox.y + chartBox.height + DELTA,
        "legend overflows the BOTTOM border: " + info);
    return errors;
  }

  private static void check(List<String> errors, boolean condition, String message) {
    if (!condition) {
      errors.add(message);
    }
  }

  private static String describe(BoundingBox box) {
    return String.format("[x=%.1f, y=%.1f, w=%.1f, h=%.1f]", box.x, box.y, box.width, box.height);
  }
}
