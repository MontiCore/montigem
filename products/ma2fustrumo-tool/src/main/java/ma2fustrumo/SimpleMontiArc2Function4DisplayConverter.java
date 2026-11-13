package ma2fustrumo;

import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogPrintCollector;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public class SimpleMontiArc2Function4DisplayConverter {
  protected final String frontendCd2PojoSymbolsDirectoryPath;
  private final List<String> errors = new ArrayList<>();

  public SimpleMontiArc2Function4DisplayConverter(String frontendCd2PojoSymbolsDirectoryPath) {
    this.frontendCd2PojoSymbolsDirectoryPath = frontendCd2PojoSymbolsDirectoryPath;
  }

  public Function4Display convert(String mainFunctionName, String source) {
    // MontiArc's mills and symbol tables are global, so conversions must not overlap.
    synchronized (MA2FuStruMoTool.class) {
      errors.clear();
      Thread conversionThread = Thread.currentThread();
      var log = new LogPrintCollector() {
        @Override
        public void doError(Finding error) {
          if (Thread.currentThread() == conversionThread) {
            errors.add(error.buildMsg());
          }
        }

        @Override
        public void doError(Finding error, Throwable cause) {
          doError(error);
        }
      };
      Log.addLogHook(log);
      try {
        var tool = new MA2FuStruMoTool();
        tool.init();
        tool.execute(source,
                     Path.of(frontendCd2PojoSymbolsDirectoryPath),
                     mainFunctionName,
                     true);
        if (!errors.isEmpty()) {
          throw new IllegalArgumentException("The MontiArc model contains errors.");
        }
        return new FuStruMo2FuStruMo4DisplayTransformer().transform(tool.getResult());
      } finally {
        Log.removeLogHook(log);
      }
    }
  }

  public List<String> getErrors() {
    return List.copyOf(errors);
  }
}
