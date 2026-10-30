package doc;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import domain.test_infrastructure.AbstractDomainE2ETest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

/**
 * Takes the screenshots that the documentation generator (comment2doc) embeds into
 * the online documentation of the component library.
 *
 * <p>Pages built on {@code common.ExamplePage} carry demonstrator notes and the
 * code-viewer toolbar above the demonstrated component, so their screenshot is
 * taken from the {@code .examplePreview} column that holds only the component
 * (the instance name of that column in common/ExamplePage.gui becomes its CSS class).
 * Pages that use the chart components directly (the *FromCommand pages) are
 * captured as a whole.
 */
public class ScreenshotGen extends AbstractDomainE2ETest {

  /** Selector of the demonstrated component on a page built on common.ExamplePage. */
  private static String preview(String pageTag) {
    return pageTag + " .examplePreview";
  }

  @Test
  @Tag("Screenshot")
  void takeScreenshots(Page page) {
    List<Screenshot> screenshotList = List.of(
        new Screenshot("/example_arrange/GemCardExample", preview("gem-card-example"), "../../../docs/gendoc_img/GemCardExample.png"),
        new Screenshot("/example_arrange/GemColumnExample", preview("gem-column-example"), "../../../docs/gendoc_img/GemColumnExample.png"),
        new Screenshot("/example_arrange/GemColumnExample2", preview("gem-column-example2"), "../../../docs/gendoc_img/GemColumnExample2.png"),
        new Screenshot("/example_arrange/GemDialogExample", preview("gem-dialog-example"), "../../../docs/gendoc_img/GemDialogExample.png"),
        new Screenshot("/example_arrange/GemGridExample", preview("gem-grid-example"), "../../../docs/gendoc_img/GemGridExample.png"),
        new Screenshot("/example_arrange/GemRowExample", preview("gem-row-example"), "../../../docs/gendoc_img/GemRowExample.png"),
        new Screenshot("/example_arrange/GemRowExample2", preview("gem-row-example2"), "../../../docs/gendoc_img/GemRowExample2.png"),
        new Screenshot("/example_arrange/GemTreeNodeExample", preview("gem-tree-node-example"), "../../../docs/gendoc_img/GemTreeNodeExample.png"),
        new Screenshot("/example_basic/GemAudioExample", preview("gem-audio-example"), "../../../docs/gendoc_img/GemAudioExample.png"),
        new Screenshot("/example_basic/GemButtonExample", preview("gem-button-example"), "../../../docs/gendoc_img/GemButtonExample.png"),
        new Screenshot("/example_basic/GemIconExample", preview("gem-icon-example"), "../../../docs/gendoc_img/GemIconExample.png"),
        new Screenshot("/example_basic/GemImageExample", preview("gem-image-example"), "../../../docs/gendoc_img/GemImageExample.png"),
        new Screenshot("/example_basic/GemLinkExample", preview("gem-link-example"), "../../../docs/gendoc_img/GemLinkExample.png"),
        new Screenshot("/example_basic/GemTextExample", preview("gem-text-example"), "../../../docs/gendoc_img/GemTextExample.png"),
        new Screenshot("/example_basic/GemVideoExample", preview("gem-video-example"), "../../../docs/gendoc_img/GemVideoExample.png"),
        new Screenshot("/example_cad/GemCADExample", preview("gem-c-a-d-example"), "../../../docs/gendoc_img/GemCADExample.png"),
        new Screenshot("/example_charts/GemBarChartExample", preview("gem-bar-chart-example"), "../../../docs/gendoc_img/GemBarChartExample.png"),
        new Screenshot("/example_charts/GemBarChartFromCommand", "gem-bar-chart-from-command", "../../../docs/gendoc_img/GemBarChartFromCommand.png"),
        new Screenshot("/example_charts/GemBarChartFromCommandCopy", "gem-bar-chart-from-command-copy", "../../../docs/gendoc_img/GemBarChartFromCommandCopy.png"),
        new Screenshot("/example_charts/GemBoxPlotExample", preview("gem-box-plot-example"), "../../../docs/gendoc_img/GemBoxPlotExample.png"),
        new Screenshot("/example_charts/GemGaugeChartExample", preview("gem-gauge-chart-example"), "../../../docs/gendoc_img/GemGaugeChartExample.png"),
        new Screenshot("/example_charts/GemNetworkGraphExample", preview("gem-network-graph-example"), "../../../docs/gendoc_img/GemNetworkGraphExample.png"),
        new Screenshot("/example_charts/GemHeatmapChartExample", preview("gem-heatmap-chart-example"), "../../../docs/gendoc_img/GemHeatmapChartExample.png"),
        new Screenshot("/example_charts/GemLineChartExample", preview("gem-line-chart-example"), "../../../docs/gendoc_img/GemLineChartExample.png"),
        new Screenshot("/example_charts/GemPieChartExample", preview("gem-pie-chart-example"), "../../../docs/gendoc_img/GemPieChartExample.png"),
        new Screenshot("/example_charts/GemRadarChartExample", preview("gem-radar-chart-example"), "../../../docs/gendoc_img/GemRadarChartExample.png"),
        new Screenshot("/example_charts/GemScatterPlotExample", preview("gem-scatter-plot-example"), "../../../docs/gendoc_img/GemScatterPlotExample.png"),
        new Screenshot("/example_charts/GemSunburstChartExample", preview("gem-sunburst-chart-example"), "../../../docs/gendoc_img/GemSunburstChartExample.png"),
        new Screenshot("/example_charts/GemTimeSeriesChartExmaple", preview("gem-time-series-chart-exmaple"), "../../../docs/gendoc_img/GemTimeSeriesChartExmaple.png"),
        new Screenshot("/example_charts/GemVennDiagramExample", preview("gem-venn-diagram-example"), "../../../docs/gendoc_img/GemVennDiagramExample.png"),
        new Screenshot("/example_input/GemAutocompleteInputExample", preview("gem-autocomplete-input-example"), "../../../docs/gendoc_img/GemAutocompleteInputExample.png"),
        new Screenshot("/example_input/GemChatExample", preview("gem-chat-example"), "../../../docs/gendoc_img/GemChatExample.png"),
        new Screenshot("/example_input/GemCheckBoxInputExample", preview("gem-check-box-input-example"), "../../../docs/gendoc_img/GemCheckBoxInputExample.png"),
        new Screenshot("/example_input/GemDropdownInputExample", preview("gem-dropdown-input-example"), "../../../docs/gendoc_img/GemDropdownInputExample.png"),
        new Screenshot("/example_input/GemRadioInputExample", preview("gem-radio-input-example"), "../../../docs/gendoc_img/GemRadioInputExample.png"),
        new Screenshot("/example_input/GemRangeInputExample", preview("gem-range-input-example"), "../../../docs/gendoc_img/GemRangeInputExample.png"),
        new Screenshot("/example_input/GemTextareaInputExample", preview("gem-textarea-input-example"), "../../../docs/gendoc_img/GemTextareaInputExample.png"),
        new Screenshot("/example_input/GemTextInputExample", preview("gem-text-input-example"), "../../../docs/gendoc_img/GemTextInputExample.png"),
        new Screenshot("/example_input/GemNumberInputExample", preview("gem-number-input-example"), "../../../docs/gendoc_img/GemNumberInputExample.png"),
        new Screenshot("/example_network/NetworkChartExample", preview("network-chart-example"), "../../../docs/gendoc_img/NetworkChartExample.png"),
        new Screenshot("/example_table/GemTableExample", preview("gem-table-example"), "../../../docs/gendoc_img/GemTableExample.png"),
        new Screenshot("/fischertechnik/FischertechnikExample", preview("fischertechnik-example"), "../../../docs/gendoc_img/FischertechnikExample.png")
    );
    for (Screenshot screenshot : screenshotList) {
      navAndWait(page, screenshot.page()); //go to page we want to test
      Locator el = page.locator(screenshot.component());
      el.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(screenshot.location())));
    }
  }

}
