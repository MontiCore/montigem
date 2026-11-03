package dashboardcomponents;

import dashboardmodule.Axis;
import dashboardmodule.BarExtraData;
import dashboardmodule.BoxExtraData;
import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.DashboardModulePosition;
import dashboardmodule.DashboardModuleSize;
import dashboardmodule.ExtraData;
import dashboardmodule.GaugeExtraData;
import dashboardmodule.GraphExtraData;
import dashboardmodule.LineExtraData;
import dashboardmodule.PieExtraData;

import static dashboardmodule.VizType.LINE;
import java.util.function.Consumer;
import java.util.Optional;
import dashboardmodule.commands.InterpretPieChart;
import dashboardmodule.commands.InterpretLineChart;
import dashboardmodule.commands.InterpretNetworkGraph;
import dashboardmodule.commands.InterpretBarChart;
import dashboardmodule.commands.InterpretBoxPlot;
import dashboardmodule.commands.InterpretGaugeChart;
import dashboardmodule.commands.InterpretScatterPlot;
import umlp.backendrte.command.CommandManager;

public class CellDialog extends CellDialogTOP {

  public void nextStep() {
      this.setFirst(false);
  }

  @Override
  public void initCellForEdit() {
    if (!this._cellForEditSetExternally){
      if (this.dashboardcomponents_CellDialog_cellForEdit_RoutingId != null){
        this.setCellInternal(DashboardModuleManager.getCellConfigSure(this.dashboardcomponents_CellDialog_cellForEdit_RoutingId));
      } else {
        String dataExample = """
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

        DashboardModuleSize defaultSize = DashboardModuleManager.dashboardModuleSizeBuilder()
            .height(4.0)
            .width(3.0);
        DashboardModulePosition defaultPosition = DashboardModuleManager.dashboardModulePositionBuilder()
            .x(0.0)
            .y(0.0);

        CellConfig cell = DashboardModuleManager.cellConfigBuilder()
            .viz(LINE)
            .name("New Cell")
            .data(dataExample)
            .position(defaultPosition)
            .size(defaultSize);
        this.setCellForEditInternal(cell);
        MainCellOptions.setDefaultLineExtra(cell);
      }
      super.initView();
    }
  }

  @Override
  public Consumer<Void> stepBack() {
    return $ -> {
      unshowError();
      this.setFirst(true);
    };
  }

  public void showError(String message) {
      this.setMessage(message);
      this.setError(true);
  }

  public void unshowError() {
      this.setError(false);
  }

  @Override
  public Consumer<Void> abortCreate(Consumer<Void> close) {
    return $ -> {
      unshowError();
      close.accept(null);
    };
  }

  @Override
  public Consumer<Void> changeCurrentTab() {
    return $ -> {
        this.setFirst(!this.isFirst());
    };
  }

  @Override
  public Consumer<Void> nextCreate(CellConfig cell, Consumer<Integer> changeTutorialStep) {
    return $ -> {
      String checked = checkCellMain(cell);
      if (checked != "positive") {
        showError(checked);
      } else {
        cell.build().get();
        // the extra options get their defaults when the type of visualization is selected (MainCellOptions),
        // only set them if they are missing; empty builders would lack e.g. the axes of the scatter plot
        if (!hasExtraOptions(cell)) {
          MainCellOptions.setDefaultExtra(cell);
        }
        unshowError();
        nextStep();
        //tutorial-only related
        if (this.getTutorialStep() == 13) {
          changeTutorialStep.accept(1);
        }
      }
    };
  }

  public boolean hasExtraOptions(CellConfig cell) {
    if (!cell.isPresentExtra()) {
      return false;
    }
    ExtraData extra = cell.getExtra();
    switch (cell.getViz()) {
      case LINE:
        return extra.isPresentLinextra();
      case BAR:
        return extra.isPresentBarextra();
      case SCATTER:
        return extra.isPresentScatterextra();
      case SPEED:
        return extra.isPresentSpeedextra();
      case BOX:
        return extra.isPresentBoxextra();
      case PIE:
        return extra.isPresentPiextra();
      case NETWORK:
        return extra.isPresentGraphextra();
    }
    return true;
  }

  //
  //check main data input for visualization
  //
  public String checkCellMain(CellConfig cell) {
    if (cell.getName() == null || cell.getName().isEmpty()) {
      return "Name of the new cell is not defined";
    }
    if (cell.getViz() == null || cell.getViz().toString().isEmpty()) {
      return "Type of visualization not selected";
    }
    if (cell.getName() == null || cell.getData().isEmpty()) {
      return "Data of the new cell is not defined";
    }
    String potentialErr = checkInterpretation(cell);
    if (potentialErr != "success") {
      return "Interpretation failed: "+potentialErr;
    }
    return "positive";
  }

  public String checkInterpretation(CellConfig cell) {
    String result = "success";
    switch (cell.getViz()) {
      case LINE:
        if (!CommandManager.executeCommand(new InterpretLineChart(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case BAR:
        if (!CommandManager.executeCommand(new InterpretBarChart(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case SCATTER:
        if (!CommandManager.executeCommand(new InterpretScatterPlot(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case SPEED:
        if (!CommandManager.executeCommand(new InterpretGaugeChart(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case BOX:
        if (!CommandManager.executeCommand(new InterpretBoxPlot(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case PIE:
        if (!CommandManager.executeCommand(new InterpretPieChart(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
      case NETWORK:
        if (!CommandManager.executeCommand(new InterpretNetworkGraph(cell.getData())).asSimpleResult().isSuccessful()) {
          result = DashboardModuleManager.getDashboardModuleApp().getError();
        }
        break;
    }
    return result;
  }

  @Override
  public Consumer<Void> completeCreate(CellConfig cell, Consumer<Void> close, Consumer<Integer> changeTutorialStep) {
    return $ -> {
      String checked = checkCellExtra(cell);
      if (checked != "positive") {
        showError(checked);
      } else {
        //tutorial-only related
        if(this.getTutorialStep() == 16) {
          changeTutorialStep.accept(1);
        }
        this.getView().addCells(cell);
        unshowError();
        close.accept(null);
      }
    };
  }

  //-----------------------------------------
  //check extra data inputs for visualizations
  //-------------------------------------------
  public String checkCellExtra(CellConfig cell) {
    String result = "positive";
    switch(cell.getViz()) {
      case LINE:
        result = checkLineExtra(cell);
        break;
      case BAR:
        result = checkBarExtra(cell);
        break;
      case SCATTER:
        result = checkScatterExtra(cell);
        break;
      case SPEED:
        result = checkSpeedExtra(cell);
        break;
      case BOX:
        result = checkBoxExtra(cell);
        break;
      case PIE:
        result = checkPieExtra(cell);
        break;
      case NETWORK:
        result = checkGraphExtra(cell);
        break;
    }
    return result;
  }

  public String checkLineExtra(CellConfig cell) {
    LineExtraData extra = cell.getExtra().getLinextra();
    Integer max = extra.getMaxValue().orElse(null);
    if(max != null){
      if (max <= 0) {
        return "Maximum value is set to a non positive number";
      }
      if(extra.getMinValue() >= max) {
        return "Minimum value set equal or higher than the maximum value ";
      }
    }
    if(extra.getMinValue() < 0) {
      return "Minimum value is set to a negative number";
    }
    return "positive";
  }

  public String checkBarExtra(CellConfig cell) {
    BarExtraData extra = cell.getExtra().getBarextra();
    Integer max = extra.getMaxValue().orElse(null);
    if(max != null) {
      if (max <= 0) {
        return "Maximum value is set to a non positive number";
      }
      if(extra.getMinValue() >= max) {
        return "Minimum value set equal or higher than the maximum value ";
      }
    }
    if(extra.getMinValue() < 0) {
      return "Minimum value is set to a negative number";
    }

    return "positive";
  }

  public String checkSpeedExtra(CellConfig cell) {
    GaugeExtraData extra = cell.getExtra().getSpeedextra();
    Integer max = extra.getMax().orElse(null);
    if(max != null) {
      if (max <= 0) {
        return "Maximum is set to a non positive number";
      }
      if(extra.getMin() >= max) {
        return "Minimum set equal or higher than the maximum";
      }
    }
    if(extra.getMin() < 0) {
      return "Minimum is set to a negative number";
    }
    return "positive";
  }

  public String checkScatterExtra(CellConfig cell) {
    Axis xAxis = cell.getExtra().getScatterextra().getXAxis();
    Double xmax = xAxis.getMaxValue().orElse(null);
    if(xmax != null) {
      if (xmax <= 0) {
        return "Maximum value of the x axis is set to a non positive number";
      }
      if(xAxis.getMinValue() >= xmax) {
        return "Minimum value set equal or higher than the maximum value of x axis ";
      }
    }
    if(xAxis.getMinValue() < 0) {
      return "Minimum value of the x axis is set to a negative number";
    }
    Axis yAxis = cell.getExtra().getScatterextra().getYAxis();
    Double ymax = yAxis.getMaxValue().orElse(null);
    if(ymax != null) {
      if (ymax <= 0) {
        return "Maximum value of the y axis is set to a non positive number";
      }
      if(yAxis.getMinValue() >= ymax) {
        return "Minimum value set equal or higher than the maximum value of y axis ";
      }
    }
    if(yAxis.getMinValue() < 0) {
      return "Minimum value of the y axis is set to a negative number";
    }

    return "positive";
  }

  public String checkBoxExtra(CellConfig cell) {
    BoxExtraData extra = cell.getExtra().getBoxextra();
    Double max = extra.getMax().orElse(null);
    if (max != null) {
      if (max <= 0) {
        return "Maximum is set to a non positive number";
      }
      if(extra.getMin() >= max) {
        return "Minimum set equal or higher than the maximum";
      }
    }
    if(extra.getMin() < 0) {
      return "Minimum is set to a negative number";
    }

    return "positive";
  }

  public String checkPieExtra(CellConfig cell) {
    PieExtraData extra = cell.getExtra().getPiextra();
    if (extra.getInnerRadius() != null) {
      if (extra.getInnerRadius() < 0) {
        return "Inner radius set to a negative number";
      }
    }
    return "positive";
  }

  public String checkGraphExtra(CellConfig cell) {
    GraphExtraData extra = cell.getExtra().getGraphextra();
    if (extra.getMinHeight() != null && extra.getMinHeight().contains("-")) {
      return "Minimal height set to a negative number";
    }
    return "positive";
  }


  @Override
  public Consumer<Void> abortEdit(CellConfig cell, CellConfig initialState, Consumer<Void> close) {
    return $ -> {
      unshowError();
      close.accept(null);
    };
  }

  @Override
  public Consumer<Void> completeEdit(CellConfig cell, CellConfig cellForEdit, Consumer<Void> close, Consumer<Integer> changeTutorialStep) {
    return $ -> {
      String checked = checkCellMain(cellForEdit);
      if (checked != "positive") {
        showError(checked);
        return;
      }
      checked = checkCellExtra(cellForEdit);
      if (checked != "positive") {
        showError(checked);
        return;
      }
      if(cell.isPresentExtra()) {
        if(!cell.getExtra().equals(cellForEdit.getExtra())) {
          cell.setExtra(cellForEdit.getExtra().build().get());
        }
      }
      if(!cell.getViz().equals(cellForEdit.getViz())) {
        cell.setViz(cellForEdit.getViz());
        cell.setExtra(cellForEdit.getExtra().build().get());
      }
      if(!cell.getData().equals(cellForEdit.getData())) {
        cell.setData(cellForEdit.getData());
      }
      if(!cell.getName().equals(cellForEdit.getName())) {
        cell.setName(cellForEdit.getName());
      }

      unshowError();
      close.accept(null);
      //tutorial-only related
      if (this.getTutorialStep() == 26) {
        changeTutorialStep.accept(1);
      }
    };
  }

  //------------------
  //tutorial related
  //-----------------
  @Override
  public Boolean checkIfTutorialStep13() { return this.getTutorialStep() == 13;}

  @Override
  public Boolean checkIfTutorialStep16() { return this.getTutorialStep() == 16;}

  @Override
  public Boolean checkIfTutorialStep26() { return this.getTutorialStep() == 26;}

  @Override
  public Boolean disabledByTutorial() {
    int current = this.getTutorialStep();
    if (current == 0 || current == 13 || current == 16 || current == 26) {
      return false;
    }
    return true;
  }

  @Override
  public Boolean alwaysDisabledByTutorial() {
    if (this.getTutorialStep() != 0) {
      return true;
    }
    return false;
  }
}
