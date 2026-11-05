/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The other side of an attribute qualified map can be changed: its setters and list mutators put the value into the
 * owner's map under the key derived from it, or remove it, and the map updates the other side, like a bidirectional
 * list updates its other side; the map's commands and rows carry the change.
 * <ul>
 *   <li>[0..1] Club [[number]] (numberClub) &lt;-&gt; (memberByNumber) Person [1]: setNumberClub</li>
 *   <li>[*] Club [[login]] (clubs) &lt;-&gt; (memberByLogin) Person [1]: addClubs, removeClubs, the list</li>
 * </ul>
 */
public interface OtherSideChangesDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-OtherSideChanges")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements OtherSideChangesDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-OtherSideChanges")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements OtherSideChangesDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Club load(Club club) {
    return QualifiedAssocsDAOFactory.getClubDAO().selectClub(club.getGemId()).get();
  }

  default Person person(String login, int number) {
    return QualifiedAssocsManager.personBuilder().login(login).number(number).build().get();
  }

  default Club club() {
    return QualifiedAssocsManager.clubBuilder().build().get();
  }

  /** setting the single owner puts the value into its map (moving it out of the old owner's), null removes it */
  @Test
  default void testSetPutsIntoTheOwnersMap() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);

    anna.setNumberClub(club1);
    assertEquals(Map.of(1, anna.getGemId()), club1._internal_getMemberByNumberGemMap().getIDMap());
    assertEquals(club1.getGemId(), anna.getNumberClubGem().getGemId());
    assertEquals(Map.of(1, anna.getGemId()), load(club1)._internal_getMemberByNumberGemMap().getIDMap());

    anna.setNumberClub(club2);
    assertEquals(0, club1.sizeMemberByNumbers());
    assertEquals(Map.of(1, anna.getGemId()), club2._internal_getMemberByNumberGemMap().getIDMap());
    assertEquals(club2.getGemId(), anna.getNumberClubGem().getGemId());
    assertEquals(0, load(club1).sizeMemberByNumbers());

    anna.setNumberClub(null);
    assertEquals(0, club2.sizeMemberByNumbers());
    assertEquals(-1, anna.getNumberClubGem().getGemId());
    assertEquals(0, load(club2).sizeMemberByNumbers());
  }

  /** adding and removing owners puts the value into and removes it from their maps, also through the list */
  @Test
  default void testAddAndRemoveOwners() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var club3 = club();
    var anna = person("anna", 1);

    anna.addClubs(club1);
    anna.addClubs(club2);
    assertTrue(club1.containsKeyMemberByLogin("anna"));
    assertTrue(club2.containsKeyMemberByLogin("anna"));
    assertEquals(List.of(club1.getGemId(), club2.getGemId()), anna._internal_getClubsGemList().getIDs());

    assertTrue(anna.removeClubs(club1));
    assertEquals(0, club1.sizeMemberByLogins());
    assertEquals(List.of(club2.getGemId()), anna._internal_getClubsGemList().getIDs());
    assertEquals(0, load(club1).sizeMemberByLogins());

    anna._internal_getClubsGemList().add(club3);
    assertTrue(club3.containsKeyMemberByLogin("anna"));
    assertEquals(List.of(club2.getGemId(), club3.getGemId()), anna._internal_getClubsGemList().getIDs());
    assertTrue(load(club3).containsKeyMemberByLogin("anna"));
  }

  /** a change of the other side on a client reaches the server and the database through the owner's map */
  @Test
  default void testClientChangeIsSynchronized() {
    getBackend().activate();
    var club1 = club();
    var anna = person("anna", 1);
    getClients(0).activate();
    var clientClub1 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());

    clientAnna.setNumberClub(clientClub1);
    clientAnna.addClubs(clientClub1);
    assertEquals(club1.getGemId(), clientAnna.getNumberClubGem().getGemId());

    getBackend().activate();
    assertEquals(Map.of(1, anna.getGemId()), club1._internal_getMemberByNumberGemMap().getIDMap());
    assertTrue(club1.containsKeyMemberByLogin("anna"));
    assertEquals(club1.getGemId(), anna.getNumberClubGem().getGemId());
    assertEquals(List.of(club1.getGemId()), anna._internal_getClubsGemList().getIDs());
    assertEquals(Map.of(1, anna.getGemId()), load(club1)._internal_getMemberByNumberGemMap().getIDMap());
  }
}
