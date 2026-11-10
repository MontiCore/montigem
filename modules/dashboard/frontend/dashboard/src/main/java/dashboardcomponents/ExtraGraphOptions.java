package dashboardcomponents;

import dashboardmodule.GraphComplexLayoutType;
import dashboardmodule.GraphExtraData;
import dashboardmodule.GraphInitialLayoutType;

import java.util.Optional;

public class ExtraGraphOptions extends ExtraGraphOptionsTOP {

  @Override
  public Boolean isHierarchical() {
    GraphExtraData extra = getGraphExtra();
    return extra != null && (extra.getInitialLayout() == GraphInitialLayoutType.HIERARCHICAL
        || extra.getInitialLayout() == GraphInitialLayoutType.CHANNEL_HIERARCHICAL);
  }

  @Override
  public Boolean hasComplexLayout() {
    GraphExtraData extra = getGraphExtra();
    return extra != null && extra.getComplexLayout() != null && extra.getComplexLayout() != GraphComplexLayoutType.NONE;
  }

  //---------------------------------------------------------------------------------
  //Overriding the generated getter/setter of the maximum iterations, so that an empty
  //input removes the limit (cf. ExtraSpeedOptions)
  //---------------------------------------------------------------------------------

  @Override
  public void setMaxIterationsEntry(String maxIterationsEntry) {
    if (getGraphExtra() == null) {
      return;
    }
    if (maxIterationsEntry == null || maxIterationsEntry.isEmpty()) {
      getGraphExtra().setMaxLayoutIterations(Optional.empty());
      return;
    }
    super.setMaxIterationsEntry(maxIterationsEntry);
  }

  @Override
  public String getMaxIterationsEntry() {
    if (getGraphExtra() == null) {
      return null;
    }
    if (getGraphExtra().getMaxLayoutIterations().isEmpty()) {
      return "";
    }
    return super.getMaxIterationsEntry();
  }

  protected GraphExtraData getGraphExtra() {
    return this.getExtra() == null ? null : this.getExtra().getGraphextra();
  }
}
