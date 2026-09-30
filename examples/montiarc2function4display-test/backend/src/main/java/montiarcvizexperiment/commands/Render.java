package montiarcvizexperiment.commands;

import de.monticore.cd2pojo.CD2PojoTool;
import de.se_rwth.commons.Files;
import de.se_rwth.commons.logging.Log;
import ma2fustrumo.MA2FuStruMoTool;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import montiarcvizexperiment.MontiArcVizExperimentManager;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

    //TODO extract this logic
    var maDirPath = "../frontend/build/Model";
    var backendCd2PojoSymbolsDirPath = "../backend/build/cd2pojo_symbols";
    var frontendCd2PojoSymbolsDirPath = "../frontend/build/cd2pojo_symbols";

    var maDir = new File(maDirPath);
    var mainFunctionParts = mainFunctionName.split("\\.");
    var srcFilePath = String.join(Character.toString(File.separatorChar), mainFunctionParts) +
                      ".arc";
    var srcFile = new File(maDir, srcFilePath);
    var srcDir = srcFile.getParentFile();
    var ignoredCreateSuccess = srcDir.mkdirs();

    Log.info("Writing MA source code to " + srcFile, Render.class.getName());
    try (var writer = new BufferedWriter(new FileWriter(srcFile))) {
      writer.write(maSrc);
    } catch (IOException e) {
      Log.error("Failed to write MA source code to MA directory", e);
    }

    Log.info("Parsing MA source code", Render.class.getName());
    CD2PojoTool.main(new String[]{
        "-i", maDirPath, "-c2mc", "--symboltable", backendCd2PojoSymbolsDirPath,
    });

    var tool = new MA2FuStruMoTool();
    tool.init();
    var failQuick = Log.isFailQuickEnabled();
    Log.enableFailQuick(false); //TODO that does not help for invalid input
    tool.run(new String[]{
        "-i", maDirPath, "-path", frontendCd2PojoSymbolsDirPath, "-main", mainFunctionName, "-c2mc",
    });
    Log.enableFailQuick(failQuick);

    Files.deleteFiles(maDir); //cleanup

    var mainFunction = tool.getResult4Display();
    return new SimpleResult<>(getId(), mainFunction, Function4Display.class.getName());
  }
}
