package montiarcvizexperiment.commands;

import de.se_rwth.commons.Files;
import de.se_rwth.commons.logging.Log;
import ma2fustrumo.SimpleMontiArc2Function4DisplayConverter;
import ma2fustrumo.SimpleMontiArcWriter;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import montiarcvizexperiment.MontiArcVizExperimentManager;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.io.IOException;

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

    var writer = new SimpleMontiArcWriter("../frontend/build/Model");
    var converter = new SimpleMontiArc2Function4DisplayConverter("../frontend/build/Model",
                                                                 "../backend/build/cd2pojo_symbols",
                                                                 "../frontend/build" +
                                                                 "/cd2pojo_symbols");

    try {
      writer.write(mainFunctionName, maSrc);
    } catch (IOException e) {
      Log.error("Failed to write MA source code to MA directory", e);
    }

    var mainFunction = converter.convert(mainFunctionName);

    Files.deleteFiles(writer.getMontiArcDirectory()); //cleanup

    return new SimpleResult<>(getId(), mainFunction, Function4Display.class.getName());
  }

}
