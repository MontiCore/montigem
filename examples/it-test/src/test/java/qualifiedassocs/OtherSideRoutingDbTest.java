/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.commands.Club_putMemberByCode;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;
import umlp.backendrte.command.CommandManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The commands of a bidirectional qualified association reach the participants that loaded only the value or only
 * the value's old owner (a single owner), like the commands of a bidirectional association reach both sides: a put
 * or remove command names the value as well, a put command the old owner. They change what is loaded there and load
 * nothing.
 * <ul>
 *   <li>[0..1] Club [String] (club) &lt;-&gt; (memberByCode) Person [1]</li>
 *   <li>[1] Club [Department] (ledClub) &lt;-&gt; (leaderByDepartment) Person [1]</li>
 *   <li>[*] Club [[login]] (clubs) &lt;-&gt; (memberByLogin) Person [1]</li>
 * </ul>
 */
public interface OtherSideRoutingDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-OtherSideRouting")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements OtherSideRoutingDbTest {
    Neo4JTestSuite() {
      super(2);
    }
  }

  @DisplayName("SQL-OtherSideRouting")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements OtherSideRoutingDbTest {
    SQLTestSuite() {
      super(2);
    }
  }

  default Person person(String login, int number) {
    return QualifiedAssocsManager.personBuilder().login(login).number(number).build().get();
  }

  default Club club() {
    return QualifiedAssocsManager.clubBuilder().build().get();
  }

  default Department department(String name) {
    return QualifiedAssocsManager.departmentBuilder().name(name).build().get();
  }

  default long clubId(Person p) {
    return p.getClubGem().getGemId();
  }

  default long ledClubId(Person p) {
    return p.getLedClubGem().getGemId();
  }

  default List<Long> clubsIds(Person p) {
    return p._internal_getClubsGemList().getIDs();
  }

  /** a participant that loaded only the value learns the server's puts and removals through its other side */
  @Test
  default void testServerCommandsReachTheValuesSubscribers() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    var sales = department("sales");
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club1.getGemId(), clubId(clientAnna));

    getBackend().activate();
    club2.putMemberByCode("b1", anna);
    club2.putLeaderByDepartment(sales, anna);
    club2.putMemberByLogin(anna);
    getClients(0).activate();
    assertFalse(QualifiedAssocsManager.isLoadedClub(club2.getGemId()));
    assertEquals(club2.getGemId(), clubId(clientAnna));
    assertEquals(club2.getGemId(), ledClubId(clientAnna));
    assertEquals(List.of(club2.getGemId()), clubsIds(clientAnna));

    getBackend().activate();
    club2.removeMemberByCode("b1");
    club2.removeMemberByLogin("anna");
    getClients(0).activate();
    // (clearing the other side by id with the generated _internal_setClub loads the old value, club2, as for every
    // association, so whether club2 is loaded is not checked here)
    assertEquals(-1, clubId(clientAnna));
    assertEquals(List.of(), clubsIds(clientAnna));
  }

  /** a client's put that moves the value reaches a participant that loaded only the value's old owner */
  @Test
  default void testClientPutReachesTheOldOwnersSubscribers() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    var sales = department("sales");
    var it = department("it");
    club1.putMemberByCode("a1", anna);
    club1.putLeaderByDepartment(sales, anna);
    getClients(0).activate();
    var club1OfClient0 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    assertEquals(1, club1OfClient0.sizeMemberByCodes());
    assertEquals(1, club1OfClient0.sizeLeaderByDepartments());

    getClients(1).activate();
    var annaOfClient1 = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    var itOfClient1 = QualifiedAssocsManager.getDepartmentSure(it.getGemId());
    var club2OfClient1 = QualifiedAssocsManager.getClubSure(club2.getGemId());
    club2OfClient1.putMemberByCode("b1", annaOfClient1);
    club2OfClient1.putLeaderByDepartment(itOfClient1, annaOfClient1);
    getClients(0).activate();
    assertEquals(0, club1OfClient0.sizeMemberByCodes());
    assertEquals(0, club1OfClient0.sizeLeaderByDepartments());
    assertFalse(QualifiedAssocsManager.isLoadedPerson(anna.getGemId()));

    getBackend().activate();
    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(Map.of("b1", anna.getGemId()), club2._internal_getMemberByCodeGemMap().getIDMap());
    assertEquals(club2.getGemId(), clubId(anna));
    assertEquals(club2.getGemId(), ledClubId(anna));
  }

  /** a client's put reaches a participant that loaded only the value */
  @Test
  default void testClientPutReachesTheValuesSubscribers() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var annaOfClient0 = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(club1.getGemId(), clubId(annaOfClient0));

    getClients(1).activate();
    var annaOfClient1 = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    var club2OfClient1 = QualifiedAssocsManager.getClubSure(club2.getGemId());
    club2OfClient1.putMemberByCode("b1", annaOfClient1);
    club2OfClient1.putMemberByLogin(annaOfClient1);
    getClients(0).activate();
    // the command does not load the owner (the generated _internal_setClub(id) loads the old value, club1, as for
    // every association)
    assertFalse(QualifiedAssocsManager.isLoadedClub(club2.getGemId()));
    assertEquals(club2.getGemId(), clubId(annaOfClient0));
    assertEquals(List.of(club2.getGemId()), clubsIds(annaOfClient0));
  }

  /**
   * a put under a free key of a value without an owner (no replaced value, no old owner) reaches only the subscribers
   * of the owner and the value: an unrelated participant that put another value that way before is not subscribed to
   * the other put's objects, so it does not load them on their next change
   */
  @Test
  default void testPutDoesNotReachUnrelatedParticipants() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    getClients(0).activate();
    var club1OfClient0 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    club1OfClient0.putMemberByCode("a1", QualifiedAssocsManager.getPersonSure(anna.getGemId()));

    getClients(1).activate();
    var club2OfClient1 = QualifiedAssocsManager.getClubSure(club2.getGemId());
    club2OfClient1.putMemberByCode("b1", QualifiedAssocsManager.getPersonSure(bob.getGemId()));

    getBackend().activate();
    bob.setNumber(5);
    getClients(0).activate();
    assertFalse(QualifiedAssocsManager.isLoadedPerson(bob.getGemId()));
    assertFalse(QualifiedAssocsManager.isLoadedClub(club2.getGemId()));
  }

  /** a client that names no old owner (it did not load the value): the server names it before sending the put on */
  @Test
  default void testServerNamesTheOldOwner() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var club1OfClient0 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    assertEquals(1, club1OfClient0.sizeMemberByCodes());

    getClients(1).activate();
    assertTrue(CommandManager.executeCommand(
        new Club_putMemberByCode(club2.getGemId(), anna.getGemId(), "b1", -1, -1)).isSuccessful());
    assertFalse(QualifiedAssocsManager.isLoadedPerson(anna.getGemId()));
    getClients(0).activate();
    assertEquals(0, club1OfClient0.sizeMemberByCodes());

    getBackend().activate();
    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(club2.getGemId(), clubId(anna));
  }
}
