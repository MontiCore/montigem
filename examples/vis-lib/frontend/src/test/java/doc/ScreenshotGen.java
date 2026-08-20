package doc;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import domain.test_infrastructure.AbstractDomainE2ETest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

public class ScreenshotGen extends AbstractDomainE2ETest {

  @Test
  @Tag("Screenshot")
  void takeScreenshots(Page page) {
    List<Screenshot> screenshotList = List.of(
        new Screenshot("/example_arrange/GemCardExample", "gem-card-example", "../../../docs/gendoc_img/GemCardExample.png"),
        new Screenshot("/example_arrange/GemColumnExample", "gem-column-example", "../../../docs/gendoc_img/GemColumnExample.png"),
        new Screenshot("/example_arrange/GemColumnExample2", "gem-column-example2", "../../../docs/gendoc_img/GemColumnExample2.png"),
        new Screenshot("/example_arrange/GemDialogExample", "gem-dialog-example", "../../../docs/gendoc_img/GemDialogExample.png"),
        new Screenshot("/example_arrange/GemGridExample", "gem-grid-example", "../../../docs/gendoc_img/GemGridExample.png"),
        new Screenshot("/example_arrange/GemRowExample", "gem-row-example", "../../../docs/gendoc_img/GemRowExample.png"),
        new Screenshot("/example_arrange/GemRowExample2", "gem-row-example2", "../../../docs/gendoc_img/GemRowExample2.png"),
        new Screenshot("/example_arrange/GemTreeNodeExample", "gem-tree-node-example", "../../../docs/gendoc_img/GemTreeNodeExample.png"),
        new Screenshot("/example_basic/GemAudioExample", "gem-audio-example", "../../../docs/gendoc_img/GemAudioExample.png"),
        new Screenshot("/example_basic/GemButtonExample", "gem-button-example", "../../../docs/gendoc_img/GemButtonExample.png"),
        new Screenshot("/example_basic/GemIconExample", "gem-icon-example", "../../../docs/gendoc_img/GemIconExample.png"),
        new Screenshot("/example_basic/GemImageExample", "gem-image-example", "../../../docs/gendoc_img/GemImageExample.png"),
        new Screenshot("/example_basic/GemLinkExample", "gem-link-example", "../../../docs/gendoc_img/GemLinkExample.png"),
        new Screenshot("/example_basic/GemTextExample", "gem-text-example", "../../../docs/gendoc_img/GemTextExample.png"),
        new Screenshot("/example_basic/GemVideoExample", "gem-video-example", "../../../docs/gendoc_img/GemVideoExample.png"),
        new Screenshot("/example_cad/GemCADExample", "gem-c-a-d-example", "../../../docs/gendoc_img/GemCADExample.png"),
        new Screenshot("/example_charts/GemBarChartExample", "gem-bar-chart-example", "../../../docs/gendoc_img/GemBarChartExample.png"),
        new Screenshot("/example_charts/GemBarChartFromCommand", "gem-bar-chart-from-command", "../../../docs/gendoc_img/GemBarChartFromCommand.png"),
        new Screenshot("/example_charts/GemBarChartFromCommandCopy", "gem-bar-chart-from-command-copy", "../../../docs/gendoc_img/GemBarChartFromCommandCopy.png"),
        new Screenshot("/example_charts/GemBoxPlotExample", "gem-box-plot-example", "../../../docs/gendoc_img/GemBoxPlotExample.png"),
        new Screenshot("/example_charts/GemGaugeChartExample", "gem-gauge-chart-example", "../../../docs/gendoc_img/GemGaugeChartExample.png"),
        new Screenshot("/example_charts/GemNetworkGraphExample", "gem-network-graph-example", "../../../docs/gendoc_img/GemNetworkGraphExample.png"),
        new Screenshot("/example_charts/GemHeatmapChartExample", "gem-heatmap-chart-example", "../../../docs/gendoc_img/GemHeatmapChartExample.png"),
        new Screenshot("/example_charts/GemLineChartExample", "gem-line-chart-example", "../../../docs/gendoc_img/GemLineChartExample.png"),
        new Screenshot("/example_charts/GemPieChartExample", "gem-pie-chart-example", "../../../docs/gendoc_img/GemPieChartExample.png"),
        new Screenshot("/example_charts/GemRadarChartExample", "gem-radar-chart-example", "../../../docs/gendoc_img/GemRadarChartExample.png"),
        new Screenshot("/example_charts/GemScatterPlotExample", "gem-scatter-plot-example", "../../../docs/gendoc_img/GemScatterPlotExample.png"),
        new Screenshot("/example_charts/GemSunburstChartExample", "gem-sunburst-chart-example", "../../../docs/gendoc_img/GemSunburstChartExample.png"),
        new Screenshot("/example_charts/GemTimeSeriesChartExmaple", "gem-time-series-chart-exmaple", "../../../docs/gendoc_img/GemTimeSeriesChartExmaple.png"),
        new Screenshot("/example_charts/GemVennDiagramExample", "gem-venn-diagram-example", "../../../docs/gendoc_img/GemVennDiagramExample.png"),
        new Screenshot("/example_input/GemAutocompleteInputExample", "gem-autocomplete-input-example", "../../../docs/gendoc_img/GemAutocompleteInputExample.png"),
        new Screenshot("/example_input/GemChatExample", "gem-chat-example", "../../../docs/gendoc_img/GemChatExample.png"),
        new Screenshot("/example_input/GemCheckBoxInputExample", "gem-check-box-input-example", "../../../docs/gendoc_img/GemCheckBoxInputExample.png"),
        new Screenshot("/example_input/GemDropdownInputExample", "gem-dropdown-input-example", "../../../docs/gendoc_img/GemDropdownInputExample.png"),
        new Screenshot("/example_input/GemRadioInputExample", "gem-radio-input-example", "../../../docs/gendoc_img/GemRadioInputExample.png"),
        new Screenshot("/example_input/GemRangeInputExample", "gem-range-input-example", "../../../docs/gendoc_img/GemRangeInputExample.png"),
        new Screenshot("/example_input/GemTextareaInputExample", "gem-textarea-input-example", "../../../docs/gendoc_img/GemTextareaInputExample.png"),
        new Screenshot("/example_input/GemTextInputExample", "gem-text-input-example", "../../../docs/gendoc_img/GemTextInputExample.png"),
        new Screenshot("/example_input/GemNumberInputExample", "gem-number-input-example", "../../../docs/gendoc_img/GemNumberInputExample.png"),
        new Screenshot("/example_network/NetworkChartExample", "network-chart-example", "../../../docs/gendoc_img/NetworkChartExample.png"),
        new Screenshot("/example_table/GemTableExample", "gem-table-example", "../../../docs/gendoc_img/GemTableExample.png"),
        new Screenshot("/fischertechnik/FischertechnikExample", "fischertechnik-example", "../../../docs/gendoc_img/FischertechnikExample.png")
    );
    for (Screenshot screenshot : screenshotList) {
      navAndWait(page, screenshot.page()); //go to page we want to test
      Locator el = page.locator(screenshot.component());
      el.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(screenshot.location())));
    }
  }

}
