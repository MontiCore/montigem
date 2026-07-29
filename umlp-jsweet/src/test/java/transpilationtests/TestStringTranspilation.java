package transpilationtests;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestStringTranspilation {

  @Test
  public void testStringEquals() throws IOException {
    File input = Paths.get("src/test/resources/java/string/TestStringTranspilation.java").toFile();

    testStringEqualsPerConfig(input, TranspilerSetupUtil.getDomainTranspiler());
    testStringEqualsPerConfig(input, TranspilerSetupUtil.getGUITranspiler());
  }

  public void testStringEqualsPerConfig(File input, JSweetTranspiler transpiler) throws IOException {
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] files = getSourceFiles(input);
    transpiler.transpile(handler,files);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(files[0]);

    File transpiledFile = files[0].getTsFile();
    String transpiledContent = Files.readString(Paths.get(transpiledFile.getPath()));

    int occurrences = 0;
    int idx = 0;

    String find = "===";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(5,occurrences);
  }

  @Test
  public void testStringSplit() throws IOException {
    File input = Paths.get("src/test/resources/java/string/TestStringTranspilation.java").toFile();

    testStringSplitPerConfig(input, TranspilerSetupUtil.getDomainTranspiler());
    testStringSplitPerConfig(input, TranspilerSetupUtil.getGUITranspiler());
  }

  public void testStringSplitPerConfig(File input, JSweetTranspiler transpiler) throws IOException {
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] files = getSourceFiles(input);
    transpiler.transpile(handler,files);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(files[0]);

    File transpiledFile = files[0].getTsFile();
    String transpiledContent = Files.readString(Paths.get(transpiledFile.getPath()));

    int occurrences = 0;
    int idx = 0;

    String find = "StringHelper.split(";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(2,occurrences);
  }
}
