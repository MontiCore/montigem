package dashboardcomponents;

import dashboardmodule.DashboardModuleConfig;
import umlp.guiutil.Router;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.ViewConfig;
import de.se_rwth.commons.logging.Log;
import static def.dom.Globals.document;

public class InfoDialog extends InfoDialogTOP {

  @Override
  public Consumer<Void> closeDialog(Consumer<Void> close) {
    return $ -> {
      close.accept($);
    };
  }
}
