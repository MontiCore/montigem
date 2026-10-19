package ma2fustrumo;

import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MA2FuStruMoToolTest {
  private static final String SOURCE = """
      component Root {
        port out int value;
      }
      """;

  @TempDir
  Path directory;

  private Path symbolDir;
  private TestLog logger;

  @BeforeEach
  void setUp() throws IOException {
    logger = TestLog.install();
    // A path separator in a directory name must not split the path.
    symbolDir = Files.createDirectories(directory.resolve("symbol" + File.pathSeparator + "models"));
  }

  @AfterEach
  void restoreLogging() {
    logger.restore();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void executesSourceAndPreservesLogging(boolean failQuick) {
    MA2FuStruMoTool tool = new MA2FuStruMoTool();
    tool.init();
    Log.enableFailQuick(failQuick);
    logger.failQuickChanges = 0;

    tool.execute(SOURCE, symbolDir, "Root", failQuick);

    assertEquals(failQuick, Log.isFailQuickEnabled());
    assertEquals(0, logger.failQuickChanges);
    assertEquals(1, tool.getResult().getOutPorts().size());
    assertEquals("value", tool.getResult().getOutPorts().get(0).getName());
    assertEquals(0, Log.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingMainUsesCallerLoggingPolicy(boolean failQuick) {
    MA2FuStruMoTool tool = new MA2FuStruMoTool();
    tool.init();
    Log.enableFailQuick(failQuick);
    logger.failQuickChanges = 0;

    if (failQuick) {
      assertThrows(IllegalStateException.class,
                   () -> tool.execute(SOURCE, symbolDir, "Missing", true));
    } else {
      assertDoesNotThrow(() -> tool.execute(SOURCE, symbolDir, "Missing", true));
    }

    assertEquals(failQuick, Log.isFailQuickEnabled());
    assertEquals(0, logger.failQuickChanges);
    assertNull(tool.getResult());
    assertTrue(Log.getFindings().stream()
                   .anyMatch(finding -> finding.getMsg().contains("Main component not found")));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void convertsSourceWithoutWritingFilesOrChangingLogging(boolean failQuick) throws IOException {
    var converter = new SimpleMontiArc2Function4DisplayConverter(symbolDir.toString());
    Log.enableFailQuick(failQuick);
    logger.failQuickChanges = 0;
    java.util.List<Path> pathsBefore;
    try (var paths = Files.walk(directory)) {
      pathsBefore = paths.sorted().toList();
    }

    var result = converter.convert("memory.Root", """
        package memory;
        component Root {
          port out int fromString;
        }
        """);

    assertEquals(1, result.sizeOut());
    assertEquals("fromString", result.getOut(0).getName());
    assertEquals(failQuick, Log.isFailQuickEnabled());
    assertEquals(0, logger.failQuickChanges);
    assertEquals(0, Log.getErrorCount());
    try (var paths = Files.walk(directory)) {
      assertEquals(pathsBefore, paths.sorted().toList());
    }
  }

  @Test
  void rejectsInvalidSourceWithoutChangingLogging() {
    MA2FuStruMoTool tool = new MA2FuStruMoTool();
    tool.init();
    Log.enableFailQuick(false);
    logger.failQuickChanges = 0;

    assertThrows(IllegalArgumentException.class,
                 () -> tool.execute("component Root {", symbolDir, "Root", true));

    assertNull(tool.getResult());
    assertFalse(Log.isFailQuickEnabled());
    assertEquals(0, logger.failQuickChanges);
  }

  private static class TestLog extends LogStub {
    private int failQuickChanges;
    private Log previousLog;

    static TestLog install() {
      TestLog logger = new TestLog();
      logger.previousLog = getLog();
      setLog(logger);
      return logger;
    }

    void restore() {
      setLog(previousLog);
    }

    @Override
    protected void doEnableFailQuick(boolean enable) {
      failQuickChanges++;
      super.doEnableFailQuick(enable);
    }

    @Override
    protected void doError(String message) {
      super.doError(message);
      if (isFailQuickEnabled()) {
        throw new IllegalStateException(message);
      }
    }
  }
}
