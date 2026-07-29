package cocos;

import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.Finding;
import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSweetCoCoCheckerTest {

  @BeforeEach
  public void setup() {
    Log.init();
    Log.enableFailQuick(false);
  }

  @Test
  public void testCoCoInvalid() throws IOException {
    File input = Paths.get("src/test/resources/java/coco/CoCoInvalid.java").toFile();
    assertTrue(input.exists(), "Input file must exist: " + input.getAbsolutePath());

    // Use the transpiler that has the ModuleUMLPAdapter configured (which adds JSweetCoCoChecker)
    JSweetTranspiler transpiler = TranspilerSetupUtil.transpilerModuleUMLPAdapter();

    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());

    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);

    // JSweetCoCoChecker logs warnings with Log.warn (0xEE...)
    List<String> findings = Log.getFindings().stream()
        .map(Finding::getMsg)
        .collect(Collectors.toList());

    boolean found0xEE003 = findings.stream().anyMatch(s -> s.contains("0xEE003"));
    boolean found0xEE004 = findings.stream().anyMatch(s -> s.contains("0xEE004"));
    boolean found0xEE006 = findings.stream().anyMatch(s -> s.contains("0xEE006"));
    boolean found0xEE008 = findings.stream().anyMatch(s -> s.contains("0xEE008"));
    boolean found0xEE009 = findings.stream().anyMatch(s -> s.contains("0xEE009"));

    assertTrue(found0xEE003, "Should have found IO violation (0xEE003). Findings: " + findings);
    assertTrue(found0xEE006, "Should have found Collections violation (0xEE006). Findings: " + findings);
    assertTrue(found0xEE008, "Should have found Objects violation (0xEE008). Findings: " + findings);
    assertTrue(found0xEE009, "Should have found Time violation (0xEE009). Findings: " + findings);
    assertTrue(found0xEE004, "Should have found Concurrent violation (0xEE004). Findings: " + findings);
  }

  @Test
  public void testStaticFieldInit() throws IOException {
    File input = Paths.get("src/test/resources/java/coco/StaticFieldInit.java").toFile();
    assertTrue(input.exists(), "Input file must exist: " + input.getAbsolutePath());

    // Use the transpiler that has the ModuleUMLPAdapter configured (which adds JSweetCoCoChecker)
    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();

    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());

    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);

    // JSweetCoCoChecker logs warnings with Log.warn (0xEE...)
    List<String> findings = Log.getFindings().stream()
        .map(Finding::getMsg)
        .collect(Collectors.toList());

    boolean found0xEE010 = findings.stream().anyMatch(s -> s.contains("0xEE010"));

    assertTrue(found0xEE010, "Should have found IO violation (0xEE010). Findings: " + findings);
  }


  @Test
  public void testErasedDoesNotThrowError() throws IOException {
    File input = Paths.get("src/test/resources/java/coco/ErasedCoco.java").toFile();
    assertTrue(input.exists(), "Input file must exist: " + input.getAbsolutePath());

    // Use the transpiler that has the ModuleUMLPAdapter configured (which adds JSweetCoCoChecker)
    JSweetTranspiler transpiler = TranspilerSetupUtil.transpilerModuleUMLPAdapter();

    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());

    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);

    // JSweetCoCoChecker logs warnings with Log.warn (0xEE...)
    List<String> findings = Log.getFindings().stream()
        .map(Finding::getMsg)
        .collect(Collectors.toList());

    assertEquals(0, Log.getErrorCount());
  }
}
