package montiarcvizexperiment.commands;

import ma2fustrumo.SimpleMontiArc2Function4DisplayConverter;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import montiarcvizexperiment.MontiArcVizExperimentManager;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class Render extends RenderTOP {


  public Render(long dataGemId) {
    super(dataGemId);
  }

  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  @Override
  public SimpleResult<Function4Display> doAction() {
    var data = MontiArcVizExperimentManager.renderDataOpt(getDataGemId());
    if (data.isEmpty() || !data.get().isValid()) {
      return new SimpleResult<>(getId(), LoadingState.EMPTY, Function4Display.class.getName());
    }

    var mainFunctionName = data.get().getMainFunctionName();
    var maSrc = data.get().getSrc();

    var converter = new SimpleMontiArc2Function4DisplayConverter(
        "../frontend/build/cd2pojo_symbols");
    var mainFunction = converter.convert(mainFunctionName, maSrc);

    return new SimpleResult<>(getId(), mainFunction, Function4Display.class.getName());
  }

}
