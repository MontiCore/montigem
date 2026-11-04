/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.commands.Directory_putByBadgeCode;
import qualifiedassocs.commands.Directory_putByPhone;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.common.GemValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * long keys of qualified associations: a browser represents only the integers within -(2^53-1) and 2^53-1 exactly
 * (GemValueMap#MAX_SAFE_KEY), so larger keys are refused, chosen ones (Directory [long] -> (byPhone) Person) as well
 * as derived ones (Directory [[code]] -> (byBadgeCode) Badge)
 */
public interface LongKeyAssocsDbTest extends QualifiedAssocsTestSetup {

  long MAX = GemValueMap.MAX_SAFE_KEY;

  @DisplayName("Neo4J-LongKeyAssocs")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements LongKeyAssocsDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-LongKeyAssocs")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements LongKeyAssocsDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Directory load(Directory directory) {
    return QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
  }

  /** runs an action that is expected to log an error starting with message (also more than once) */
  default void assertLogsError(String message, Runnable action) {
    boolean failQuick = Log.isFailQuickEnabled();
    Log.enableFailQuick(false);
    try {
      action.run();
      // removed before fail quick is enabled again: enabling it terminates if errors were logged
      assertTrue(Log.getFindings().removeIf(f -> f.getMsg().startsWith(message)));
    } finally {
      Log.enableFailQuick(failQuick);
    }
  }

  default Person person(String login, int number) {
    return QualifiedAssocsManager.personBuilder().login(login).number(number).build().get();
  }

  default Badge badge(long code) {
    return QualifiedAssocsManager.badgeBuilder().code(code).build().get();
  }

  /** the keys at the limits are stored and loaded exactly */
  @Test
  default void testSafeLongKeysAreSavedToDb() {
    getBackend().activate();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var high = badge(MAX);
    var low = badge(-MAX);
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byPhonePut(MAX, anna)
        .byBadgeCodePut(high)
        .build().get();
    directory.putByPhone(-MAX, bob);
    directory.putByBadgeCode(low);
    var clone = load(directory);
    assertEquals(Map.of(MAX, anna.getGemId(), -MAX, bob.getGemId()), clone._internal_getByPhoneGemMap().getIDMap());
    assertEquals(Map.of(MAX, high.getGemId(), -MAX, low.getGemId()),
        clone._internal_getByBadgeCodeGemMap().getIDMap());
  }

  /**
   * a chosen key beyond the limits: the owner, the builder and the map refuse it; the server rejects a put command
   * with such a key (contract), so it is neither executed nor sent on
   */
  @Test
  default void testUnsafeChosenKeyIsRefused() {
    getBackend().activate();
    var anna = person("anna", 1);
    var builder = QualifiedAssocsManager.directoryBuilder();
    assertLogsError("0xA7309", () -> builder.byPhonePut(MAX + 1, anna));
    var directory = builder.build().get();
    assertEquals(0, directory.sizeByPhones());
    assertLogsError("0xA7326", () -> directory.putByPhone(MAX + 1, anna));
    assertLogsError("0xA7309", () -> directory.getByPhoneMap().put(-MAX - 1, anna));
    var result = CommandManager.executeCommand(new Directory_putByPhone(directory.getGemId(), anna.getGemId(),
        Long.MAX_VALUE));
    assertFalse(result.isSuccessful());
    assertEquals(0, directory.sizeByPhones());
    assertEquals(0, load(directory).sizeByPhones());
  }

  /**
   * a value whose derived key is beyond the limits: the owner, the builder and the map refuse it; the server rejects a
   * put command with such a key (contract), so it is neither executed nor sent on
   */
  @Test
  default void testUnsafeDerivedKeyIsRefused() {
    getBackend().activate();
    var unsafe = badge(MAX + 1);
    var builder = QualifiedAssocsManager.directoryBuilder();
    assertLogsError("0xA7309", () -> builder.byBadgeCodePut(unsafe));
    var directory = builder.build().get();
    assertEquals(0, directory.sizeByBadgeCodes());
    assertLogsError("0xA7326", () -> directory.putByBadgeCode(unsafe));
    assertLogsError("0xA7309", () -> directory.getByBadgeCodeMap().put(MAX + 1, unsafe));
    var result = CommandManager.executeCommand(new Directory_putByBadgeCode(directory.getGemId(), unsafe.getGemId(),
        MAX + 1));
    assertFalse(result.isSuccessful());
    assertEquals(0, directory.sizeByBadgeCodes());
    assertEquals(0, load(directory).sizeByBadgeCodes());
  }

  /**
   * the key attribute of a value changes to a key beyond the limits: like a key that is not unique, the rekeying is
   * refused on every participant, the value stays under its old key; loaded again, it has no key
   */
  @Test
  default void testRekeyToUnsafeKeyIsRefused() {
    getBackend().activate();
    var badge = badge(7);
    var directory = QualifiedAssocsManager.directoryBuilder().byBadgeCodePut(badge).build().get();
    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    // keys the values on the client, which then observes the badge
    assertTrue(clientDirectory.containsKeyByBadgeCode(7L));

    assertLogsError("0xA7309", () -> {
      getBackend().activate();
      badge.setCode(MAX + 1);
      assertEquals(Map.of(7L, badge.getGemId()), directory._internal_getByBadgeCodeGemMap().getIDMap());
      getClients(0).activate();
      assertEquals(MAX + 1, QualifiedAssocsManager.getBadgeSure(badge.getGemId()).getCode());
      assertEquals(Map.of(7L, badge.getGemId()), clientDirectory._internal_getByBadgeCodeGemMap().getIDMap());
      getBackend().activate();
      assertEquals(0, load(directory).sizeByBadgeCodes());
    });
  }
}
