/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.commands.Directory_putByShiftStart;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;
import umlp.backendrte.command.CommandManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * time keys of qualified associations: chosen dates and points in time (Directory [LocalDate] -> (byBirthday) Person,
 * Directory [Instant] -> (byShiftStart) Person), derived date-times (Directory [[start]] -> (meetingByStart) Meeting)
 * and a Map attribute with date keys (Roster byDay); equal keys are the same key, chosen keys are stored only to the
 * microsecond
 */
public interface TimeKeyAssocsDbTest extends QualifiedAssocsTestSetup {

  LocalDate DAY = LocalDate.of(2026, 11, 17);

  Instant SHIFT = Instant.parse("2026-11-17T09:30:15.123456Z");

  LocalDateTime START = LocalDateTime.of(2026, 11, 17, 9, 30, 15, 123_456_000);

  @DisplayName("Neo4J-TimeKeyAssocs")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements TimeKeyAssocsDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-TimeKeyAssocs")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements TimeKeyAssocsDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Directory load(Directory directory) {
    return QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
  }

  /** runs an action that is expected to log a finding starting with message (also more than once) */
  default void assertLogs(String message, Runnable action) {
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

  default Meeting meeting(LocalDateTime start) {
    return QualifiedAssocsManager.meetingBuilder().start(start).build().get();
  }

  /** the keys are stored and loaded exactly; a new key object with the same value finds the stored one */
  @Test
  default void testTimeKeysAreSavedToDb() {
    getBackend().activate();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var meeting = meeting(START);
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byBirthdayPut(DAY, anna)
        .meetingByStartPut(meeting)
        .build().get();
    directory.putByShiftStart(SHIFT, bob);
    var clone = load(directory);
    assertEquals(Map.of(DAY, anna.getGemId()), clone._internal_getByBirthdayGemMap().getIDMap());
    assertEquals(Map.of(SHIFT, bob.getGemId()), clone._internal_getByShiftStartGemMap().getIDMap());
    assertEquals(Map.of(START, meeting.getGemId()), clone._internal_getMeetingByStartGemMap().getIDMap());
    assertEquals(anna.getGemId(), clone._internal_getByBirthdayGemMap().getID(LocalDate.parse("2026-11-17")));
  }

  /** a put under a key equal to a stored one replaces its value */
  @Test
  default void testEqualKeyReplacesTheValue() {
    getBackend().activate();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var directory = QualifiedAssocsManager.directoryBuilder().byBirthdayPut(DAY, anna).build().get();
    directory.putByBirthday(LocalDate.of(2026, 11, 17), bob);
    assertEquals(1, directory.sizeByBirthdays());
    assertSame(bob, directory.getByBirthday(DAY));
    assertEquals(Map.of(DAY, bob.getGemId()), load(directory)._internal_getByBirthdayGemMap().getIDMap());
  }

  /**
   * a chosen key finer than microseconds: the owner, the builder and the map refuse it; the server rejects a put
   * command with such a key (contract), so it is neither executed nor sent on
   */
  @Test
  default void testFineChosenKeyIsRefused() {
    getBackend().activate();
    var anna = person("anna", 1);
    Instant fine = SHIFT.plusNanos(1);
    var builder = QualifiedAssocsManager.directoryBuilder();
    assertLogs("0xA7309", () -> builder.byShiftStartPut(fine, anna));
    var directory = builder.build().get();
    assertEquals(0, directory.sizeByShiftStarts());
    assertLogs("0xA7326", () -> directory.putByShiftStart(fine, anna));
    assertLogs("0xA7309", () -> directory.getByShiftStartMap().put(fine, anna));
    assertLogs("0xA7338", () -> assertFalse(CommandManager.executeCommand(
        new Directory_putByShiftStart(directory.getGemId(), anna.getGemId(), fine)).isSuccessful()));
    assertEquals(0, directory.sizeByShiftStarts());
    assertEquals(0, load(directory).sizeByShiftStarts());
  }

  /** a derived key follows its attribute, also loaded again */
  @Test
  default void testDerivedTimeKeyFollowsTheAttribute() {
    getBackend().activate();
    var meeting = meeting(START);
    var directory = QualifiedAssocsManager.directoryBuilder().meetingByStartPut(meeting).build().get();
    meeting.setStart(START.plusHours(1));
    assertFalse(directory.containsKeyMeetingByStart(START));
    assertSame(meeting, directory.getMeetingByStart(START.plusHours(1)));
    assertEquals(Map.of(START.plusHours(1), meeting.getGemId()),
        load(directory)._internal_getMeetingByStartGemMap().getIDMap());
  }

  /** puts and removes of a client reach the server and the database, removes with a key equal to the stored one */
  @Test
  default void testClientPutIsSynchronized() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var anna = person("anna", 1);

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    clientDirectory.putByBirthday(DAY, clientAnna);
    clientDirectory.putByShiftStart(SHIFT, clientAnna);

    getBackend().activate();
    assertSame(anna, directory.getByBirthday(DAY));
    var clone = load(directory);
    assertEquals(Map.of(DAY, anna.getGemId()), clone._internal_getByBirthdayGemMap().getIDMap());
    assertEquals(Map.of(SHIFT, anna.getGemId()), clone._internal_getByShiftStartGemMap().getIDMap());

    getClients(0).activate();
    clientDirectory.removeByBirthday(LocalDate.parse("2026-11-17"));

    getBackend().activate();
    assertEquals(0, directory.sizeByBirthdays());
    assertEquals(0, load(directory).sizeByBirthdays());
  }

  /** a Map attribute with date keys and domain class values is a type qualified association */
  @Test
  default void testMapAttributeWithDateKeys() {
    getBackend().activate();
    var anna = person("anna", 1);
    var roster = QualifiedAssocsManager.rosterBuilder().byDayPut(DAY, anna).build().get();
    var clone = QualifiedAssocsDAOFactory.getRosterDAO().selectRoster(roster.getGemId()).get();
    assertEquals(Map.of(DAY, anna.getGemId()), clone._internal_getByDayGemMap().getIDMap());
  }
}
