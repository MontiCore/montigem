package ma2fustrumo;

import de.monticore.cd2pojo.CD2PojoTool;
import de.se_rwth.commons.logging.Log;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SimpleMontiArc2Function4DisplayConverter {
  protected final String montiArcDirectoryPath;
  protected final String backendCd2PojoSymbolsDirectoryPath;
  protected final String frontendCd2PojoSymbolsDirectoryPath;

  public SimpleMontiArc2Function4DisplayConverter(String montiArcDirectoryPath,
                                                  String backendCd2PojoSymbolsDirectoryPath,
                                                  String frontendCd2PojoSymbolsDirectoryPath) {
    this.montiArcDirectoryPath = montiArcDirectoryPath;
    this.backendCd2PojoSymbolsDirectoryPath = backendCd2PojoSymbolsDirectoryPath;
    this.frontendCd2PojoSymbolsDirectoryPath = frontendCd2PojoSymbolsDirectoryPath;
  }

  public Function4Display convert(String mainFunctionName) {
    CD2PojoTool.main(new String[]{
        "-i", montiArcDirectoryPath, "-c2mc", "--symboltable", backendCd2PojoSymbolsDirectoryPath,
    });

    var tool = new MA2FuStruMoTool();
    tool.init();
    var failQuick = Log.isFailQuickEnabled();
    Log.enableFailQuick(false); //TODO that does not help for invalid input
    //directly call doRun to avoid unnecessary cli parsing
    tool.doRun(new String[]{montiArcDirectoryPath},
               new String[]{frontendCd2PojoSymbolsDirectoryPath},
               mainFunctionName,
               null,
               null,
               null,
               true,
               false);
    Log.enableFailQuick(failQuick);

    return tool.getResult4Display();
  }
}
