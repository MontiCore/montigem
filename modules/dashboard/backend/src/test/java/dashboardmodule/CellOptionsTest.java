package dashboardmodule;

import de.monticore.symboltable.serialization.JsonParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CellOptionsTest {
  @BeforeEach
  void setUp() {
    DashboardModuleMock.init();
  }

  @Test
  void patchesAllChartOptionTypesAndNestedAxes() {
    ViewConfig view = DashboardModuleManager.viewConfigBuilder().vid(0).name("Options").build().get();
    CellTools tools = new CellTools(view);
    CellConfig cell = DashboardModuleManager.cellConfigBuilder().name("Options")
        .viz(VizType.PIE).data("")
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x(0.0).y(0.0).build().get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(3.0).height(3.0).build().get())
        .build().get();
    view.addCells(cell);
    Map<VizType, String> patches = Map.of(
        VizType.BAR, "{'stacked':true,'overlapping':true,'minValue':-10,'maxValue':100}",
        VizType.LINE, "{'minValue':-10,'maxValue':100}",
        VizType.PIE, "{'innerRadius':50}",
        VizType.BOX, "{'min':-1.5,'max':10.5}",
        VizType.SPEED, "{'unit':'%','textValue':'Done','textPosition':'bottom','min':0,'max':100}",
        VizType.NETWORK, "{'minHeight':'400px','showFilterGroup':true,'showSearchNode':true,'showControlButtons':false,'showInformation':true}",
        VizType.SCATTER, "{'xAxis':{'label':'Age','minValue':10.5,'maxValue':100.5,'stepWidth':5.5},'yAxis':{'label':'Count'}}");
    for (var entry : patches.entrySet()) {
      cell.setViz(entry.getKey());
      cell.setExtra(CellOptions.build(entry.getKey(), CellOptions.defaults(entry.getKey())));
      var expected = CellOptions.patch(entry.getKey(), JsonParser.parseJsonObject(CellOptions.read(cell)), entry.getValue().replace('\'', '"'));
      assertTrue(tools.setCellOptions(cell.getGemId(), entry.getValue().replace('\'', '"')).startsWith("Updated"));
      assertEquals(expected.toString(), JsonParser.parseJsonObject(CellOptions.read(cell)).toString());
    }
    cell.setViz(VizType.SCATTER);
    cell.setExtra(CellOptions.build(VizType.SCATTER, CellOptions.defaults(VizType.SCATTER)));
    long oldAxisId = cell.getExtra().getScatterextra().getXAxis().getGemId();
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'xAxis':{'label':'Age'}}".replace('\'', '"')).startsWith("Updated"));
    assertEquals(oldAxisId, cell.getExtra().getScatterextra().getXAxis().getGemId());
    assertEquals("Y", cell.getExtra().getScatterextra().getYAxis().getLabel());
    assertEquals(999, cell.getExtra().getScatterextra().getXAxis().getStepWidth());
    for (String patch : List.of("{'xAxis':null}", "{'xAxis':{'minValue':10,'maxValue':5}}",
        "{'xAxis':{'stepWidth':0}}", "{'xAxis':{'unknown':1}}")) {
      assertTrue(tools.setCellOptions(cell.getGemId(), patch.replace('\'', '"')).startsWith("Invalid options:"));
    }
    assertEquals("Age", cell.getExtra().getScatterextra().getXAxis().getLabel());
  }

}
