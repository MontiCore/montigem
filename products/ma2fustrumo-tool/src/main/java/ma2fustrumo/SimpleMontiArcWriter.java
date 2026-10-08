package ma2fustrumo;

import org.jspecify.annotations.NullMarked;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@NullMarked
public class SimpleMontiArcWriter {
  protected final String montiArcDirectoryPath;

  public SimpleMontiArcWriter(String montiArcDirectoryPath) {
    this.montiArcDirectoryPath = montiArcDirectoryPath;
  }

  public void write(String mainFunctionName, String montiArcSrc) throws IOException {
    var maDir = new File(montiArcDirectoryPath);
    var mainFunctionParts = mainFunctionName.split("\\.");
    var srcFilePath = String.join(Character.toString(File.separatorChar), mainFunctionParts) +
                      ".arc";
    var srcFile = new File(maDir, srcFilePath);
    var srcDir = srcFile.getParentFile();
    var ignoredCreateSuccess = srcDir.mkdirs();

    try (var writer = new BufferedWriter(new FileWriter(srcFile))) {
      writer.write(montiArcSrc);
    }
  }
}
