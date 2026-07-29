package transpilationtests;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtilArraysTranspilation {
  @Test
  public void test() throws Exception {
    File input = Paths.get("src/test/resources/java/arrays/UtilArrays.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());

    SourceFile[] files = getSourceFiles(input);
    transpiler.transpile(handler,files);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(files[0]);

    File transpiledFile = files[0].getTsFile();
    String transpiledContent = Files.readString(Paths.get(transpiledFile.getPath()));


    var importPattern = Pattern.compile(
        "^import \\{.* Arrays,? .*} from '@umlp/commonj2ts';$", Pattern.MULTILINE);
    assertEquals(1, importPattern.matcher(transpiledContent).results().count());

    // var callPattern = Pattern.compile("Arrays\\.\\w+\\(.+\\);");
    // assertEquals(3, callPattern.matcher(transpiledContent).results().count());

    // var streamRangePattern = Pattern.compile("Arrays\\.streamRange\\(.+\\);");
    // assertEquals(1, streamRangePattern.matcher(transpiledContent).results().count());
  }

  @Test
  public void testUnsupportedErrors() throws Exception {
    File input = Paths.get("src/test/resources/java/arrays/UtilArraysUnsupported.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());

    SourceFile[] files = getSourceFiles(input);
    transpiler.transpile(handler,files);

    assertEquals(13, handler.getErrorCount());
  }
}
