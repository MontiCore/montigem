package montiarcvizexperiment.commands;

import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import montiarcvizexperiment.MontiArcVizExperimentServerApplication;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class GetMainFunction extends GetMainFunctionTOP {
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<Function4Display> doAction() {
    var id = getId();
    var typeParameter = Function4Display.class.getName();
    return MontiArcVizExperimentServerApplication
        .getMainFunction()
        .map(it -> new SimpleResult<>(id, it, typeParameter))
        .orElseGet(() -> new SimpleResult<>(id, LoadingState.NOT_YET_LOADED, typeParameter));
  }
}
