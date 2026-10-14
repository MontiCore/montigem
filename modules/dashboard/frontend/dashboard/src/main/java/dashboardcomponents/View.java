package dashboardcomponents;

import dashboardmodule.CellConfig;
import dashboardmodule.CellConfigBuilder;
import dashboardmodule.CellConfigObserver;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ExtraDataBuilder;
import dashboardmodule.VizType;
import de.se_rwth.commons.logging.Log;
import dashboardmodule.DashboardModuleSize;
import dashboardmodule.DashboardModulePosition;
import dashboardmodule.ExtraData;
import java.util.function.Consumer;
import dashboardmodule.LineExtraData;
import dashboardmodule.BarExtraData;
import dashboardmodule.ScatterExtraData;
import dashboardmodule.GaugeExtraData;
import dashboardmodule.BoxExtraData;
import dashboardmodule.PieExtraData;
import dashboardmodule.GraphExtraData;
import dashboardmodule.Axis;
import umlp.backendrte.common.NotificationScope;

import static dashboardmodule.VizType.LINE;
import java.util.Optional;
import java.util.Arrays;


public class View extends ViewTOP{

  @Override
  public Consumer<Long> setEditCell() {
    return id -> {
      this.setEditCellFlag(true);
      this.setEditCellId(id);
    };
  }

  // the edit dialog is opened by setting this view's own editCellFlag (setEditCell), so it has to be reset here as
  // well: Dashboard.closeEdit() only resets the Dashboard's flag, which stays false and is not passed down again
  @Override
  public Consumer<Void> closeEditDialog() {
    return $ -> {
      this.setEditCellFlag(false);
      if (this.getCloseEdit() != null) {
        this.getCloseEdit().accept(null);
      }
    };
  }

  @Override
  public CellConfig getEditCell(Long editCellId) {
    Optional<CellConfig> cellConfig = DashboardModuleManager.cellConfigOpt(editCellId);
    if(cellConfig.isPresent()) {
      return cellConfig.get();
    }
    return null;
  }

  protected CellConfig lastInitialState = null;
  protected long lastEditedCell = -1;

  @Override
  public CellConfig getCellBuilder(Long editCellId) {
    if(lastEditedCell == editCellId) {
      return lastInitialState;
    }
    Optional<CellConfig> cellOpt = DashboardModuleManager.cellConfigOpt(editCellId);
    if(cellOpt.isEmpty()) {
      return null;
    }
    CellConfig cell = cellOpt.get();
    ExtraData initialExtra = null;
    if(cell.getExtra() != null) {
      ExtraDataBuilder extraBuilder = DashboardModuleManager.extraDataBuilder();
      switch(cell.getViz()) {
        case LINE:
          initialExtra = extraBuilder.linextra(copyPasteLineExtra(cell));
          break;
        case BAR:
          initialExtra = extraBuilder.barextra(copyPasteBarExtra(cell));
          break;
        case SCATTER:
          initialExtra = extraBuilder.scatterextra(copyPasteScatterExtra(cell));
          break;
        case SPEED:
          initialExtra = extraBuilder.speedextra(copyPasteSpeedExtra(cell));
          break;
        case BOX:
          initialExtra = extraBuilder.boxextra(copyPasteBoxExtra(cell));
          break;
        case PIE:
          initialExtra = extraBuilder.piextra(copyPastePieExtra(cell));
          break;
        case NETWORK:
          initialExtra = extraBuilder.graphextra(copyPasteGraphExtra(cell));
          break;
      }
    }
    CellConfigBuilder builder = DashboardModuleManager.cellConfigBuilder()
        .name(cell.getName())
        .viz(cell.getViz())
        .data(cell.getData())
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x(cell.getPosition().getX()).y(cell.getPosition().getY()))
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(cell.getSize().getWidth()).height(cell.getSize().getHeight()));
    builder.addObserver(new CellConfigObserver() {
      @Override
      public void maybeNotifySetViz(CellConfig cellConfig, VizType oldValue, VizType o, NotificationScope scope) {
        System.err.println("VIZ WAS CHANGED!!");
      }
    });
    CellConfig initialState;
    if (initialExtra != null) {
      initialState = builder.extra(initialExtra);
    } else {
      initialState = builder;
    }
    lastEditedCell = editCellId;
    lastInitialState = initialState;
    return initialState;
  }

  //---------------------------------------------------------------------------------------------------
  //extra data visualization specific copying(create new object with the same values) helping functions
  //---------------------------------------------------------------------------------------------------
  public LineExtraData copyPasteLineExtra(CellConfig cell) {
    if (cell.getExtra().getLinextra() == null) {
      return null;
    }
    LineExtraData original = cell.getExtra().getLinextra();
    return DashboardModuleManager.lineExtraDataBuilder()
        .maxValue(original.getMaxValue())
        .minValue(original.getMinValue());
  }

  public BarExtraData copyPasteBarExtra(CellConfig cell) {
    if (cell.getExtra().getBarextra() == null) {
      return null;
    }
    BarExtraData original = cell.getExtra().getBarextra();
    return DashboardModuleManager.barExtraDataBuilder()
        .stacked(original.isStacked())
        .overlapping(original.isOverlapping())
        .maxValue(original.getMaxValue())
        .minValue(original.getMinValue());
  }

  public ScatterExtraData copyPasteScatterExtra(CellConfig cell) {
    if (cell.getExtra().getScatterextra() == null) {
      return null;
    }
    ScatterExtraData original = cell.getExtra().getScatterextra();
    Axis xAxis = DashboardModuleManager.axisBuilder()
        .label(original.getXAxis().getLabel())
        .minValue(original.getXAxis().getMinValue())
        .maxValue(original.getXAxis().getMaxValue())
        .stepWidth(original.getXAxis().getStepWidth());
    Axis yAxis = DashboardModuleManager.axisBuilder()
        .label(original.getYAxis().getLabel())
        .minValue(original.getYAxis().getMinValue())
        .maxValue(original.getYAxis().getMaxValue())
        .stepWidth(original.getYAxis().getStepWidth());
    return DashboardModuleManager.scatterExtraDataBuilder()
        .xAxis(xAxis)
        .yAxis(yAxis);
  }

  public GaugeExtraData copyPasteSpeedExtra(CellConfig cell) {
    if (cell.getExtra().getSpeedextra() == null) {
      return null;
    }
    GaugeExtraData original = cell.getExtra().getSpeedextra();
    return DashboardModuleManager.gaugeExtraDataBuilder()
        .min(original.getMin())
        .max(original.getMax())
        .unit(original.getUnit())
        .textValue(original.getTextValue())
        .textPosition(original.getTextPosition());
  }

  public BoxExtraData copyPasteBoxExtra(CellConfig cell) {
    if (cell.getExtra().getBoxextra() == null) {
      return null;
    }
    BoxExtraData original = cell.getExtra().getBoxextra();
    return DashboardModuleManager.boxExtraDataBuilder()
        .min(original.getMin())
        .max(original.getMax());
  }

  public PieExtraData copyPastePieExtra(CellConfig cell) {
    if (cell.getExtra().getPiextra() == null) {
      return null;
    }

    PieExtraData original = cell.getExtra().getPiextra();

    return DashboardModuleManager.pieExtraDataBuilder()
        .innerRadius(original.getInnerRadius());
  }

  public GraphExtraData copyPasteGraphExtra(CellConfig cell) {
    if (cell.getExtra().getGraphextra() == null) {
      return null;
    }

    GraphExtraData original = cell.getExtra().getGraphextra();

    return DashboardModuleManager.graphExtraDataBuilder()
        .minHeight(original.getMinHeight())
        .showFilterGroup(original.isShowFilterGroup())
        .showSearchNode(original.isShowSearchNode())
        .showControlButtons(original.isShowControlButtons())
        .showInformation(original.isShowInformation());
  }

}
