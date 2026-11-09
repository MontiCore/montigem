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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  /**
   * deleting the key object while the owner is not loaded: the server loads the owner, so the entry leaves through it
   * and the other side of the loaded values follows, on the server and on a client that loaded only the value
   */
  @Test
  default void testOtherSideAfterKeyObjectDeletionWithOwnerNotLoaded() {
    getBackend().activate();
    var club = club();
    var other = club();
    var anna = person("anna", 1);
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    club.putLeaderByDepartment(sales, anna);
    club.putSponsorByDepartment(sales, anna);
    other.putSponsorByDepartment(it, anna);
    QualifiedAssocsManager.TESTING_clearCache();
    var serverAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club.getGemId(), ledClubId(serverAnna));
    assertEquals(2, serverAnna._internal_getSponsoredClubsGemList().getIDs().size());
    assertFalse(QualifiedAssocsManager.isLoadedClub(club.getGemId()));
    getClients(0).activate();
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club.getGemId(), ledClubId(clientAnna));

    getBackend().activate();
    assertTrue(QualifiedAssocsManager.deleteDepartment(sales.getGemId()));
    assertEquals(-1, ledClubId(serverAnna));
    assertEquals(List.of(other.getGemId()), serverAnna._internal_getSponsoredClubsGemList().getIDs());
    getClients(0).activate();
    assertEquals(-1, ledClubId(clientAnna));
    assertEquals(List.of(other.getGemId()), clientAnna._internal_getSponsoredClubsGemList().getIDs());

    getBackend().activate();
    QualifiedAssocsManager.TESTING_clearCache();
    var loadedClub = QualifiedAssocsManager.getClubSure(club.getGemId());
    assertEquals(0, loadedClub.sizeLeaderByDepartments());
    assertEquals(0, loadedClub.sizeSponsorByDepartments());
    assertEquals(-1, ledClubId(QualifiedAssocsManager.getPersonSure(anna.getGemId())));
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

  /**
   * a value of a subclass (Admin extends Person): its inherited other side follows a move, also on a participant that
   * loaded only the value, and in the database
   */
  @Test
  default void testOtherSideOfSubclassValue() {
    getBackend().activate();
    var club = club();
    var other = club();
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(7).build().get();
    club.putMemberByCode("a1", admin);
    assertEquals(club.getGemId(), clubId(admin));
    getClients(0).activate();
    var clientAdmin = QualifiedAssocsManager.getAdminSure(admin.getGemId());
    assertEquals(club.getGemId(), clubId(clientAdmin));

    getBackend().activate();
    other.putMemberByCode("b1", admin);
    assertEquals(0, club.sizeMemberByCodes());
    assertEquals(other.getGemId(), clubId(admin));
    getClients(0).activate();
    assertEquals(other.getGemId(), clubId(clientAdmin));

    getBackend().activate();
    QualifiedAssocsManager.TESTING_clearCache();
    assertEquals(other.getGemId(), clubId(QualifiedAssocsManager.getAdminSure(admin.getGemId())));
  }

  /**
   * an owner of a subclass (Team extends Club) has the inherited maps: a put into a club's map takes the value out of
   * the team's, deleting the team clears the other side of its values
   */
  @Test
  default void testOtherSideOfSubclassOwner() {
    getBackend().activate();
    var club = club();
    var team = QualifiedAssocsManager.teamBuilder().build().get();
    var anna = person("anna", 1);
    team.putMemberByCode("t1", anna);
    assertEquals(team.getGemId(), clubId(anna));

    club.putMemberByCode("c1", anna);
    assertEquals(0, team.sizeMemberByCodes());
    assertEquals(club.getGemId(), clubId(anna));

    team.putMemberByCode("t1", anna);
    assertEquals(0, club.sizeMemberByCodes());
    assertTrue(QualifiedAssocsManager.deleteTeam(team.getGemId()));
    assertEquals(-1, clubId(anna));

    QualifiedAssocsManager.TESTING_clearCache();
    assertEquals(-1, clubId(QualifiedAssocsManager.getPersonSure(anna.getGemId())));
  }

  /**
   * a club that is not loaded holds a person as a key and as a value (the key type is the value type): deleting the
   * person loads the club before both entries are removed, the value's other side of the first entry follows
   */
  @Test
  default void testDeletionAsKeyAndValueWithOwnerNotLoaded() {
    getBackend().activate();
    var club = club();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var carl = person("carl", 3);
    club.putMenteeByMentor(anna, bob);
    club.putMenteeByMentor(carl, anna);
    QualifiedAssocsManager.TESTING_clearCache();
    var loadedBob = QualifiedAssocsManager.getPersonSure(bob.getGemId());
    assertEquals(club.getGemId(), loadedBob.getMentorClubGem().getGemId());
    assertFalse(QualifiedAssocsManager.isLoadedClub(club.getGemId()));

    assertTrue(QualifiedAssocsManager.deletePerson(anna.getGemId()));
    var loadedClub = QualifiedAssocsManager.getClubSure(club.getGemId());
    assertEquals(0, loadedClub.sizeMenteeByMentors());
    assertEquals(-1, loadedBob.getMentorClubGem().getGemId());

    QualifiedAssocsManager.TESTING_clearCache();
    assertEquals(0, QualifiedAssocsManager.getClubSure(club.getGemId()).sizeMenteeByMentors());
  }
}
