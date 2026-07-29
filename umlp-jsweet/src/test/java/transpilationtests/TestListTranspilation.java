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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestListTranspilation {

  @Test
  public void testArrayListDomainConfigTranspilation() throws Exception {
    File input = Paths.get("src/test/resources/java/list/TestArrayListTranspilation.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getDomainTranspiler();

    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);
    assertNotNull(sourceFiles[0]);
    assertNotNull(sourceFiles[0].getTsFile());
    assertEquals(0, handler.getErrorCount());
  }

  @Test
  public void testArrayListGUIConfigTranspilation() throws Exception {
    File input = Paths.get("src/test/resources/java/list/TestArrayListTranspilation.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();

    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    transpiler.transpile(handler, getSourceFiles(input));
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);
    assertNotNull(sourceFiles[0]);
    assertNotNull(sourceFiles[0].getTsFile());
    assertEquals(0, handler.getErrorCount());
  }

  @Test
  public void testNegatedCollectionExpressionsAreParenthesized() throws IOException {
    File input = Paths.get("src/test/resources/java/list/TestArrayListTranspilation.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getDomainTranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(sourceFiles[0]);
    assertNotNull(sourceFiles[0].getTsFile());

    String transpiledContent = Files.readString(Paths.get(sourceFiles[0].getTsFile().getPath()));
    String normalizedContent = transpiledContent
        .replaceAll("/\\*.*?\\*/", "")
        .replaceAll("\\s+", "");
    assertTrue(normalizedContent.contains("!awaitnames.contains(name)"));
    assertTrue(normalizedContent.contains(
        "return!((value!=null&&(valueinstanceofAsyncList||valueinstanceofGemList)));"));
    assertFalse(normalizedContent.contains(
        "return!value!=null&&(valueinstanceofAsyncList||valueinstanceofGemList);"));
  }
}
