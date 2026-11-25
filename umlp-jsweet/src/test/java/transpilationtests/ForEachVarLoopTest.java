/* (c) https://github.com/MontiCore/monticore */
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
import java.util.HashSet;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For-each loops with a var loop variable are printed as iterator loops whose loop variable has the inferred type.
 */
public class ForEachVarLoopTest {

  @Test
  public void varLoopVariablesGetTheInferredType() throws Exception {
    File input = Paths.get("src/test/resources/java/list/ForEachVarLoop.java").toFile();
    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, new HashSet<>(), sourceFiles);
    assertEquals(0, handler.getErrorCount());

    String ts = Files.readString(sourceFiles[0].getTsFile().toPath());
    assertTrue(ts.matches("(?s).*\\bvalue: string = .*"), ts);
    assertTrue(ts.matches("(?s).*\\binner: .*List<number> = .*"), ts);
    assertTrue(ts.matches("(?s).*\\bi: number = .*"), ts);
    assertTrue(ts.matches("(?s).*\\bitem: (ForEachVarLoop\\.)?Item = .*"), ts);
  }
}
