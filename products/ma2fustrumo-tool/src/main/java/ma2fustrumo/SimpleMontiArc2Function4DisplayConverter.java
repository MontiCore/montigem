package ma2fustrumo;

import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public class SimpleMontiArc2Function4DisplayConverter {
  protected final String frontendCd2PojoSymbolsDirectoryPath;

  public SimpleMontiArc2Function4DisplayConverter(String frontendCd2PojoSymbolsDirectoryPath) {
    this.frontendCd2PojoSymbolsDirectoryPath = frontendCd2PojoSymbolsDirectoryPath;
  }

  public Function4Display convert(String mainFunctionName, String source) {
    var tool = new MA2FuStruMoTool();
    tool.init();
    tool.execute(source,
                 Path.of(frontendCd2PojoSymbolsDirectoryPath),
                 mainFunctionName,
                 true);

    return tool.getResult4Display();
  }
}
