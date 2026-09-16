package montiarcvizexperiment.commands;

import bin.MontiArcVizExperiment;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import umlp.backendrte.command.SimpleResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class GetMainFunction extends GetMainFunctionTOP {
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<Function4Display> doAction() {
    return new SimpleResult<>(getId(),
                              MontiArcVizExperiment.createMainFunction(),
                              Function4Display.class.getName());
  }
}
