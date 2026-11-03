package util;

import associations.test_infrastructure.AssociationsTestSetup;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * the support of the generated GemMap sync protocol tests (GemMapSyncTestGenerator), like SyncProtocolTestSupport
 * for lists; the methods specific to a map are generated
 */
public interface GemMapSyncProtocolTestSupport extends AssociationsTestSetup {
  @BeforeEach
  default void configLog() {
    Log.enableFailQuick(false);
    Log.getFindings().clear();
  }

  @AfterEach
  default void checkLogForErrors() {
    assertEquals(0, Log.getErrorCount(),
        "Log errors:\n" + Log.getFindings().stream().map(Object::toString)
            .collect(Collectors.joining("\n")));
  }

  /**
   * an error starting with code was logged, as expected: removed, so that checkLogForErrors accepts the test
   */
  default void assertLogged(String code) {
    assertTrue(Log.getFindings().removeIf(f -> f.getMsg().startsWith(code)), "expected the error " + code);
  }

  GemMapSyncProtocolTestScenario createDoneToDoneInitial(int valueCount);

  GemMapSyncProtocolTestScenario createBuilderToDoneInitial(int valueCount);

  GemMapSyncProtocolTestScenario createBuilderToBuilderInitial(int valueCount);

  void applyOperation(GemMapOperation operation, GemMapSyncProtocolTestScenario scenario);

  void preloadOwner(long ownerId);

  void preloadValue(long valueId);

  void assertValuesNotLoaded(GemMapSyncProtocolTestScenario scenario);

  void assertEntryPresent(GemMapSyncProtocolTestScenario scenario, int keyIndex, long valueId);

  void assertEntryAbsent(GemMapSyncProtocolTestScenario scenario, int keyIndex);

  void assertMapSize(GemMapSyncProtocolTestScenario scenario, int size);

  boolean isValueKnown(long valueId);

  void assertPersistedEntryPresent(GemMapSyncProtocolTestScenario scenario, int keyIndex, long valueId);

  void assertPersistedEntryAbsent(GemMapSyncProtocolTestScenario scenario, int keyIndex);

  void assertPersistedMapSize(GemMapSyncProtocolTestScenario scenario, int size);

  void assertPersistedValueDeleted(long valueId);
}
