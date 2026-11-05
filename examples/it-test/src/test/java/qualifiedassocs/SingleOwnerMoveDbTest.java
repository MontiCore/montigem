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
 * Bidirectional qualified associations with a single owner ([1]/[0..1] owner side): a value is in one owner's map
 * only, a put into another owner's map moves it there, on every participant and in the database; with several owners
 * ([*]) nothing moves.
 * <ul>
 *   <li>[0..1] Club [String] (club) &lt;-&gt; (memberByCode) Person [1]</li>
 *   <li>[1] Club [Department] (ledClub) &lt;-&gt; (leaderByDepartment) Person [1]</li>
 *   <li>[0..1] Club [[number]] (numberClub) &lt;-&gt; (memberByNumber) Person [1]</li>
 *   <li>[*] Club [[login]] (clubs) &lt;-&gt; (memberByLogin) Person [1]</li>
 * </ul>
 */
public interface SingleOwnerMoveDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-SingleOwnerMove")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements SingleOwnerMoveDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-SingleOwnerMove")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements SingleOwnerMoveDbTest {
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

  default Department department(String name) {
    return QualifiedAssocsManager.departmentBuilder().name(name).build().get();
  }

  /** a put into another owner's map moves the value: the old owner's map and its row lose it, the other side follows */
  @Test
  default void testPutMovesTheValue() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    var sales = department("sales");
    var it = department("it");
    club1.putMemberByCode("a1", anna);
    club1.putLeaderByDepartment(sales, anna);
    club1.putMemberByNumber(anna);

    club2.putMemberByCode("b1", anna);
    club2.putLeaderByDepartment(it, anna);
    club2.putMemberByNumber(anna);

    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(0, club1.sizeLeaderByDepartments());
    assertEquals(0, club1.sizeMemberByNumbers());
    assertEquals(Map.of("b1", anna.getGemId()), club2._internal_getMemberByCodeGemMap().getIDMap());
    assertEquals(Map.of(it.getGemId(), anna.getGemId()), club2._internal_getLeaderByDepartmentGemMap().getIDMap());
    assertEquals(Map.of(1, anna.getGemId()), club2._internal_getMemberByNumberGemMap().getIDMap());
    assertEquals(club2.getGemId(), anna.getClubGem().getGemId());
    assertEquals(club2.getGemId(), anna.getLedClubGem().getGemId());
    assertEquals(club2.getGemId(), anna.getNumberClubGem().getGemId());

    var loaded1 = load(club1);
    assertEquals(0, loaded1.sizeMemberByCodes());
    assertEquals(0, loaded1.sizeLeaderByDepartments());
    assertEquals(0, loaded1.sizeMemberByNumbers());
    assertEquals(Map.of("b1", anna.getGemId()), load(club2)._internal_getMemberByCodeGemMap().getIDMap());
  }

  /** with several owners ([*]) a value stays in every map it was put into */
  @Test
  default void testSeveralOwnersKeepTheValue() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    club1.putMemberByLogin(anna);
    club2.putMemberByLogin(anna);
    assertTrue(club1.containsKeyMemberByLogin("anna"));
    assertTrue(club2.containsKeyMemberByLogin("anna"));
    assertEquals(List.of(club1.getGemId(), club2.getGemId()), anna._internal_getClubsGemList().getIDs());
  }

  /** a participant that loaded only the old owner learns the removal (it does not receive the new owner's put) */
  @Test
  default void testMoveReachesTheOldOwnersSubscribers() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var clientClub1 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    assertEquals(1, clientClub1.sizeMemberByCodes());

    getBackend().activate();
    club2.putMemberByCode("b1", anna);
    getClients(0).activate();
    assertEquals(0, clientClub1.sizeMemberByCodes());
  }

  /** a put on a client moves the value there and, through the server, everywhere else */
  @Test
  default void testClientPutMovesTheValue() {
    getBackend().activate();
    var club1 = club();
    var club2 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var clientClub1 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    var clientClub2 = QualifiedAssocsManager.getClubSure(club2.getGemId());
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    assertEquals(1, clientClub1.sizeMemberByCodes());

    clientClub2.putMemberByCode("b1", clientAnna);
    assertEquals(0, clientClub1.sizeMemberByCodes());
    assertEquals(club2.getGemId(), clientAnna.getClubGem().getGemId());
    getBackend().activate();
    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(Map.of("b1", anna.getGemId()), club2._internal_getMemberByCodeGemMap().getIDMap());
    assertEquals(club2.getGemId(), anna.getClubGem().getGemId());
    assertEquals(0, load(club1).sizeMemberByCodes());
  }

  /** an owner built with the value takes it from its old owner */
  @Test
  default void testBuildMovesTheValue() {
    getBackend().activate();
    var club1 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    var club2 = QualifiedAssocsManager.clubBuilder().memberByCodePut("b1", anna).build().get();
    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(club2.getGemId(), anna.getClubGem().getGemId());
    assertEquals(0, load(club1).sizeMemberByCodes());
  }

  /** an owner built on a client takes the value from its old owner, on the client and on the server (create command) */
  @Test
  default void testClientBuildMovesTheValue() {
    getBackend().activate();
    var club1 = club();
    var anna = person("anna", 1);
    club1.putMemberByCode("a1", anna);
    getClients(0).activate();
    var clientClub1 = QualifiedAssocsManager.getClubSure(club1.getGemId());
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    var club2 = QualifiedAssocsManager.clubBuilder().memberByCodePut("b1", clientAnna).build().get();
    assertEquals(0, clientClub1.sizeMemberByCodes());

    getBackend().activate();
    assertEquals(0, club1.sizeMemberByCodes());
    assertEquals(club2.getGemId(), anna.getClubGem().getGemId());
    assertEquals(0, load(club1).sizeMemberByCodes());
    assertEquals(Map.of("b1", anna.getGemId()), load(QualifiedAssocsManager.getClubSure(club2.getGemId()))
        ._internal_getMemberByCodeGemMap().getIDMap());
  }
}
