package transpilationtests;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.nio.file.Paths;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestInnerClassTranspilation {


  @Test
  public void testInnerClassTranspilation() throws Exception {
    File input = Paths.get("src/test/resources/java/innerclasses/InnerAnonymousClass.java").toFile();
    File input2 = Paths.get("src/test/resources/java/innerclasses/HelperClass.java").toFile();
    File input3 = Paths.get("src/test/resources/java/innerclasses/HelperClass2.java").toFile();

    JSweetTranspiler transpiler = TranspilerSetupUtil.getDomainTranspiler();

    SourceFile[] sourceFiles = getSourceFiles(input, input2, input3);
    transpiler.transpile(new ConsoleTranspilationHandler(), sourceFiles);
    assertNotNull(sourceFiles[0]);
    assertNotNull(sourceFiles[0].getTsFile());
    assertNotNull(sourceFiles[1]);
    assertNotNull(sourceFiles[1].getTsFile());
    assertNotNull(sourceFiles[2]);
    assertNotNull(sourceFiles[2].getTsFile());
  }
}
