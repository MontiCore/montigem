package aas;

import de.se_rwth.commons.logging.Log;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.junit.jupiter.api.Test;
import aas.decorator.utils.AASParsingUtil;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AASParsingUtil.
 */
class AASParsingUtilTest extends AbstractTest{

  // Normal parsing of an existing file.
  @Test
  void parseAAS_file() {
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aas.aasx"));
    assertNotNull(result);
  }

  // Normal parsing of a file within a directory
  @Test
  void parseAAS_directory() {
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/single"));
    assertNotNull(result);
  }

  // Parsing a directory with no valid model should return null and log an error
  @Test
  void parseAAS_directoryWithNoValidModel() {
    Log.enableFailQuick(false);
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/noValidModel"));
    assertNull(result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("No valid AAS model file found at: src\\test\\resources\\aas\\aasparsingutils\\noValidModel", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void parseAAS_nonexistentFile() {
    Log.enableFailQuick(false);
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/nonexistentAas.aasx"));
    assertNull(result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("No valid AAS model file found at: src\\test\\resources\\aas\\aasparsingutils\\nonexistentAas.aasx", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void parseAAS_nonexistentDirectory() {
    Log.enableFailQuick(false);
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/nonexistentDirectory"));
    assertNull(result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("No valid AAS model file found at: src\\test\\resources\\aas\\aasparsingutils\\nonexistentDirectory", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void parseAAS_nullPath() {
    Log.enableFailQuick(false);
    Environment result = AASParsingUtil.parseAAS(null);
    assertNull(result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("AAS parsing failed: null path", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void parseAAS_fileWithInvalidExtension() {
    Log.enableFailQuick(false);
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/noValidModel/notValidModel"));
    assertNull(result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("No valid AAS model file found at: src\\test\\resources\\aas\\aasparsingutils\\noValidModel\\notValidModel", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void parseAAS_directoryWithMultipleValidFiles_shouldParseFirst() {
    Environment result = AASParsingUtil.parseAAS(Path.of("src/test/resources/aas/aasparsingutils/multiple/aasmerge1.aasx"));
    assertNotNull(result);
    assertEquals("aasMerge1", result.getAssetAdministrationShells().getFirst().getIdShort());
  }
}
