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
    if (data.isEmpty()) {
      return replyFail(LoadingState.EMPTY);
    }
    if (!data.get().isValid()) {
      String errors = "Enter a main function name and MontiArc source.";
      if (!data.get().getErrors().equals(errors)) {
        data.get().setErrors(errors);
      }
      return replyFail(LoadingState.EMPTY);
    }

    var mainFunctionName = data.get().getMainFunctionName();
    var maSrc = data.get().getSrc();

    var converter = new SimpleMontiArc2Function4DisplayConverter(
        "../frontend/build/cd2pojo_symbols");
    try {
      var mainFunction = converter.convert(mainFunctionName, maSrc);
      if (!data.get().getErrors().isEmpty()) {
        data.get().setErrors("");
      }
      return replySuccess(mainFunction);
    } catch (RuntimeException e) {
      String errors = String.join("\n", converter.getErrors());
      if (errors.isEmpty()) {
        errors = "Could not render the MontiArc model: " + e.getMessage();
      }
      if (!data.get().getErrors().equals(errors)) {
        data.get().setErrors(errors);
      }
      return replyFail(LoadingState.EMPTY);
    }
  }

}
