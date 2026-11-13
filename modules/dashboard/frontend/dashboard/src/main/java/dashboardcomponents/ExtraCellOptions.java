package dashboardcomponents;

import dashboardmodule.Axis;
import dashboardmodule.BarExtraData;
import dashboardmodule.BoxExtraData;
import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.GaugeExtraData;
import dashboardmodule.LineExtraData;
import dashboardmodule.ScatterExtraData;
import dashboardmodule.VizType;

import java.util.function.Consumer;

public class ExtraCellOptions extends ExtraCellOptionsTOP{

  @Override
  public Boolean checkIfTutorialStep14() { return this.getTutorialStep() == 14; }

  @Override
  public Boolean checkIfTutorialStep15() { return this.getTutorialStep() == 15; }

  @Override
  public Boolean compareViz(VizType viz) { return this.getCell() != null && viz.equals(this.getCell().getViz()); }
}
