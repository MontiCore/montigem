package dashboardcomponents;

import java.util.Optional;
import de.se_rwth.commons.logging.Log;
import java.lang.Integer;

public class ExtraScatterOptions extends ExtraScatterOptionsTOP{

  //----------------------------------------------------------------------------------
  //Overriding the auto generated getter/setter for the input in case of max-value
  //to allow empty value being accepting. Otherwise, jsweet transpiles Optional.empty()
  //values as null and creates errors. Technically could be done also for minimum
  //value input fields, but in that we have some concrete default value we can set
  //-----------------------------------------------------------------------------------

  @Override
  public void setXMaxValEntry(String maxValEntry) {

    if (false || this.getExtra() == null || this.getExtra().getScatterextra() == null || this.getExtra().getScatterextra().getXAxis() == null) {
      return;
    }
    if(maxValEntry == null || maxValEntry.isEmpty()) {
      this.getExtra().getScatterextra().getXAxis().setMaxValue(Optional.of(-1.0));
      return;
    }
    super.setXMaxValEntry(maxValEntry);
  }

  //@Override
  public String getxMaxValEntry() {

    if (false || this.getExtra() == null || this.getExtra().getScatterextra() == null || this.getExtra().getScatterextra().getXAxis() == null) {
      return null;
    }
    if (this.getExtra().getScatterextra().getXAxis().getMaxValue().isEmpty()
        || this.getExtra().getScatterextra().getXAxis().getMaxValue().get() == -1.0) {
      return "";
    }
    return super.getXMaxValEntry();
  }

  @Override
  public void setYMaxValEntry(String maxValEntry) {

    if (false || this.getExtra() == null || this.getExtra().getScatterextra() == null || this.getExtra().getScatterextra().getYAxis() == null) {
      return;
    }
    if(maxValEntry == null || maxValEntry.isEmpty()) {
      this.getExtra().getScatterextra().getYAxis().setMaxValue(Optional.empty());
      return;
    }
    super.setYMaxValEntry(maxValEntry);
  }

  @Override
  public String getYMaxValEntry() {

    if (false || this.getExtra() == null || this.getExtra().getScatterextra() == null || this.getExtra().getScatterextra().getYAxis() == null) {
      return null;
    }
    if (this.getExtra().getScatterextra().getYAxis().getMaxValue().isEmpty()) {
      return "";
    }
    return super.getYMaxValEntry();
  }



}
