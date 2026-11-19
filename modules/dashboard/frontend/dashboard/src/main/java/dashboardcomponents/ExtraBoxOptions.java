package dashboardcomponents;

import java.util.Optional;
import de.se_rwth.commons.logging.Log;
import java.lang.Integer;

public class ExtraBoxOptions extends ExtraBoxOptionsTOP{

  //----------------------------------------------------------------------------------
  //Overriding the auto generated getter/setter for the input in case of max-value
  //to allow empty value being accepting. Otherwise, jsweet transpiles Optional.empty()
  //values as null and creates errors. Technically could be done also for minimum
  //value input fields, but in that we have some concrete default value we can set
  //-----------------------------------------------------------------------------------

  @Override
  public void setMaxValEntry(String maxValEntry) {

    if (false || this.getExtra() == null || this.getExtra().getBoxextra() == null) {
      return;
    }
    if(maxValEntry == null || maxValEntry.isEmpty()) {
      this.getExtra().getBoxextra().setMax(Optional.empty());
      return;
    }
    super.setMaxValEntry(maxValEntry);
  }

  @Override
  public String getMaxValEntry() {

    if (false || this.getExtra() == null || this.getExtra().getBoxextra() == null) {
      return null;
    }
    if (this.getExtra().getBoxextra().getMax().isEmpty()) {
      return "";
    }
    return super.getMaxValEntry();
  }



}
