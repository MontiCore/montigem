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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMapTranspilation {

  @Test
  public void testGemMapKeepsItsType() throws IOException {
    File input = Paths.get("src/test/resources/java/map/GemMapFieldTranspilation.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getDomainTranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(sourceFiles[0].getTsFile());
    String normalizedContent = Files.readString(Paths.get(sourceFiles[0].getTsFile().getPath()))
        .replaceAll("/\\*.*?\\*/", "")
        .replaceAll("\\s+", "");
    // a GemValueMap stays the RTE class, only java.util.Map becomes IUMLPMap
    assertTrue(normalizedContent.contains("gemMap:GemValueMap<string,V>"), normalizedContent);
    assertTrue(normalizedContent.contains("map:IUMLPMap<string,V>"), normalizedContent);
    assertTrue(normalizedContent.contains("returnawaitthis.gemMap.size()+awaitthis.map.size();"), normalizedContent);
  }
}
