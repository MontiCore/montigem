/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bidirectional qualified associations: the owner's GemMap is the source of truth and updates its other side (the
 * value side, read only) on every participant, like a bidirectional list; the other side is loaded from the map's
 * rows.
 * <ul>
 *   <li>[0..1] Club [String] (club) &lt;-&gt; (memberByCode) Person [1]: a single reference</li>
 *   <li>[1] Club [Department] (ledClub) &lt;-&gt; (leaderByDepartment) Person [1]: a single reference, entry map</li>
 *   <li>[*] Club [[login]] (clubs) &lt;-&gt; (memberByLogin) Person [1]: a list of owners, derived keys</li>
 * </ul>
 */
public interface BidirectionalQualifiedAssocsDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-BidirectionalQualifiedAssocs")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup
      implements BidirectionalQualifiedAssocsDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-BidirectionalQualifiedAssocs")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements BidirectionalQualifiedAssocsDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  /** runs an action that is expected to log an error starting with message */
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

  default Club club() {
    return QualifiedAssocsManager.clubBuilder().build().get();
  }

  /** the gemId of the club the single reference refers to, -1 for none */
  default long clubId(Person p) {
    return p.getClubGem().getGemId();
  }

  default long ledClubId(Person p) {
    return p.getLedClubGem().getGemId();
  }

  default List<Long> clubsIds(Person p) {
    return p._internal_getClubsGemList().getIDs();
  }

  /** a put sets the other side, a remove clears it; a value replaced under its key is cleared as well */
  @Test
  default void testOtherSideFollowsPutAndRemove() {
    getBackend().activate();
    var club = club();
    var other = club();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();

    club.putMemberByCode("a1", anna);
    club.putLeaderByDepartment(sales, anna);
    club.putMemberByLogin(anna);
    other.putMemberByLogin(anna);
    assertEquals(club.getGemId(), clubId(anna));
    assertEquals(club.getGemId(), ledClubId(anna));
    assertEquals(List.of(club.getGemId(), other.getGemId()), clubsIds(anna));

    // replaced under its key: the old value no longer refers to the club
    club.putMemberByCode("a1", bob);
    assertEquals(-1, clubId(anna));
    assertEquals(club.getGemId(), clubId(bob));

    club.removeLeaderByDepartment(sales);
    club.removeMemberByLogin("anna");
    assertEquals(-1, ledClubId(anna));
    assertEquals(List.of(other.getGemId()), clubsIds(anna));
  }

  /** a value loaded later reads its owner(s) from the map's rows */
  @Test
  default void testOtherSideIsLoadedFromDb() {
    getBackend().activate();
    var club = club();
    var other = club();
    var anna = person("anna", 1);
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    club.putMemberByCode("a1", anna);
    club.putLeaderByDepartment(sales, anna);
    club.putMemberByLogin(anna);
    other.putMemberByLogin(anna);

    QualifiedAssocsManager.TESTING_clearCache();
    var loaded = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club.getGemId(), clubId(loaded));
    assertEquals(club.getGemId(), ledClubId(loaded));
    assertEquals(2, clubsIds(loaded).size());
    assertTrue(clubsIds(loaded).containsAll(List.of(club.getGemId(), other.getGemId())));
  }

  /** a participant that loaded the value updates its other side when the map's commands arrive, in both directions */
  @Test
  default void testOtherSideIsSynchronized() {
    getBackend().activate();
    var club = club();
    var anna = person("anna", 1);
    getClients(0).activate();
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    var clientClub = QualifiedAssocsManager.getClubSure(club.getGemId());
    assertEquals(-1, clientAnna.getClubGem().getGemId());

    getBackend().activate();
    club.putMemberByCode("a1", anna);
    club.putMemberByLogin(anna);
    getClients(0).activate();
    assertEquals(club.getGemId(), clubId(clientAnna));
    assertEquals(List.of(club.getGemId()), clubsIds(clientAnna));

    clientClub.removeMemberByCode("a1");
    clientClub.removeMemberByLogin("anna");
    assertEquals(-1, clubId(clientAnna));
    assertEquals(List.of(), clubsIds(clientAnna));
    getBackend().activate();
    assertEquals(-1, clubId(anna));
    assertEquals(List.of(), clubsIds(anna));
  }

  /** deleting the key object removes its entry, and with it the value's reference to the owner */
  @Test
  default void testOtherSideAfterKeyObjectDeletion() {
    getBackend().activate();
    var club = club();
    var anna = person("anna", 1);
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    club.putLeaderByDepartment(sales, anna);
    assertEquals(club.getGemId(), ledClubId(anna));

    assertTrue(QualifiedAssocsManager.deleteDepartment(sales.getGemId()));
    assertEquals(0, club.sizeLeaderByDepartments());
    assertEquals(-1, ledClubId(anna));
  }

  /** deleting the owner clears the other side of the loaded values, on the server and on a client, and in the db */
  @Test
  default void testOtherSideAfterOwnerDeletion() {
    getBackend().activate();
    var club = club();
    var other = club();
    var anna = person("anna", 1);
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    club.putMemberByCode("a1", anna);
    club.putLeaderByDepartment(sales, anna);
    club.putMemberByLogin(anna);
    other.putMemberByLogin(anna);
    getClients(0).activate();
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club.getGemId(), clubId(clientAnna));
    assertEquals(2, clubsIds(clientAnna).size());

    getBackend().activate();
    assertTrue(QualifiedAssocsManager.deleteClub(club.getGemId()));
    assertEquals(-1, clubId(anna));
    assertEquals(-1, ledClubId(anna));
    assertEquals(List.of(other.getGemId()), clubsIds(anna));
    getClients(0).activate();
    assertEquals(-1, clubId(clientAnna));
    assertEquals(-1, ledClubId(clientAnna));
    assertEquals(List.of(other.getGemId()), clubsIds(clientAnna));

    getBackend().activate();
    QualifiedAssocsManager.TESTING_clearCache();
    var loaded = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(-1, clubId(loaded));
    assertEquals(-1, ledClubId(loaded));
    assertEquals(List.of(other.getGemId()), clubsIds(loaded));
  }

  /** values put while the owner is a builder refer to it once it is built */
  @Test
  default void testOtherSideAfterBuild() {
    getBackend().activate();
    var anna = person("anna", 1);
    var club = QualifiedAssocsManager.clubBuilder().memberByCodePut("a1", anna).memberByLoginPut(anna).build().get();
    assertEquals(club.getGemId(), clubId(anna));
    assertEquals(List.of(club.getGemId()), clubsIds(anna));
  }

  /**
   * the other side of a type qualified map cannot be changed itself (no setters; its list refuses changes): there is no
   * key to put the value under, the owner's map changes it
   */
  @Test
  default void testTypeQualifiedOtherSideIsReadOnly() {
    getBackend().activate();
    var club = club();
    var anna = person("anna", 1);
    assertLogsError("0xA7330", () -> anna._internal_getSponsoredClubsGemList().add(club));
    assertEquals(List.of(), anna._internal_getSponsoredClubsGemList().getIDs());
    assertEquals(0, club.sizeSponsorByDepartments());
  }
}
