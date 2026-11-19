package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import dashboardmodule.VizType;
import de.se_rwth.commons.logging.Log;
import def.js.Array;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.networkgraph.networkgraphtypes.Graph;

import static def.dom.Globals.document;

import def.js.JSON;
import def.dom.HTMLAnchorElement;
import def.dom.HTMLCanvasElement;
import def.dom.HTMLElement;
import def.dom.NodeList;
import def.dom.CanvasRenderingContext2D;

import static jsweet.util.Lang.union;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import dashboardmodule.commands.InterpretPieChart;
import dashboardmodule.commands.InterpretLineChart;
import dashboardmodule.commands.InterpretNetworkGraph;
import dashboardmodule.commands.InterpretBarChart;
import dashboardmodule.commands.InterpretBoxPlot;
import dashboardmodule.commands.InterpretGaugeChart;
import dashboardmodule.commands.InterpretScatterPlot;
import umlp.backendrte.command.CommandManager;
import dashboardmodule.DashboardModuleBaseClassObjectMapper;

public class Cell extends CellTOP{

  @Override
  public Boolean checkIfDialogOpen() {
    if (this.isCreate() || this.isEditCellFlag() || this.isDelete() || this.isDownload()) {
      return true;
    }
    return false;
  }


  @Override
  public Consumer<Void> openDeleteDialog() {
    return $ -> {
      this.setLocalDelete(true);
      super.getChangeDelete().accept(true);
    };
  }

  @Override
  public Consumer<Void> closeDeleteDialog() {
    return $ -> {
      super.getChangeDelete().accept(false);
      this.setLocalDelete(false);
    };
  }

  @Override
  public Consumer<Void> openDownloadDialog() {
    return $ -> {
      super.getChangeDownload().accept(true);
      this.setLocalDownload(true);
    };
  }

  @Override
  public Consumer<Void> closeDownloadDialog() {
    return $ -> {
      super.getChangeDownload().accept(false);
      this.setLocalDownload(false);
    };
  }

  public Consumer<Void> acceptEditCell(CellConfig cell, Consumer<Long> editCell, Consumer<Integer> changeTutorialStep) {
    return $ -> {
      editCell.accept(cell.getGemId());
      //tutorial-only related
      if (checkIfTutorialStep23()) {
        changeTutorialStep.accept(1);
      }
    };
  }

  @Override
  public Consumer<Void> deleteCell() {
    return $ -> {
      ViewConfig viewConfig = DashboardModuleManager.getViewConfigSure(this.getViewGemId());
      viewConfig.removeCells(this.getCell());
      DashboardModuleManager.deleteCellConfig(this.getCell().getGemId());
      this.getDeleteWidget().accept($);
    };
  }

  public Boolean compareViz(VizType viz) {
    return this.getCell().getViz().equals(viz);
  }

  //-----------------------------
  //tutorial conditional checks
  //-----------------------------
  @Override
  public Boolean checkIfTutorialStep23() { return this.getTutorialStep() == 23; }

  @Override
  public Boolean checkIfTutorialStep22() { return this.getTutorialStep() == 22; }

  @Override
  public Boolean alwaysDisabledByTutorial() { return checkIfDialogOpen() || this.getTutorialStep() != 0; }

}
