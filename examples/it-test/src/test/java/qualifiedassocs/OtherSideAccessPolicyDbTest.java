/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The put and remove commands of a bidirectional qualified association change the value's other side, so they need
 * the permission of the value type as well, like the commands of a bidirectional association (PersonAccessPolicy
 * refuses the key "forbidden" and removing the person "locked").
 * <ul>
 *   <li>[0..1] Club [String] (club) &lt;-&gt; (memberByCode) Person [1]</li>
 * </ul>
 */
public interface OtherSideAccessPolicyDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-OtherSideAccessPolicy")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements OtherSideAccessPolicyDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-OtherSideAccessPolicy")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements OtherSideAccessPolicyDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Person person(String login, int number) {
    return QualifiedAssocsManager.personBuilder().login(login).number(number).build().get();
  }

  default Club club() {
    return QualifiedAssocsManager.clubBuilder().build().get();
  }

  /** a put the value type refuses is neither executed on the server nor stored */
  @Test
  default void testPutNeedsTheValuesPermission() {
    getBackend().activate();
    var club = club();
    var anna = person("anna", 1);
    getClients(0).activate();
    var clientClub = QualifiedAssocsManager.getClubSure(club.getGemId());
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    clientClub.putMemberByCode("forbidden", clientAnna);

    getBackend().activate();
    assertEquals(Map.of(), club._internal_getMemberByCodeGemMap().getIDMap());
    assertEquals(-1, anna.getClubGem().getGemId());
    assertEquals(Map.of(), QualifiedAssocsDAOFactory.getClubDAO().selectClub(club.getGemId())
        .get()._internal_getMemberByCodeGemMap().getIDMap());
  }

  /** a removal the value type refuses is not executed on the server */
  @Test
  default void testRemoveNeedsTheValuesPermission() {
    getBackend().activate();
    var club = club();
    var locked = person("locked", 1);
    var anna = person("anna", 2);
    club.putMemberByCode("l1", locked);
    club.putMemberByCode("a1", anna);
    getClients(0).activate();
    var clientClub = QualifiedAssocsManager.getClubSure(club.getGemId());
    clientClub.removeMemberByCode("l1");
    clientClub.removeMemberByCode("a1");

    getBackend().activate();
    assertEquals(Map.of("l1", locked.getGemId()), club._internal_getMemberByCodeGemMap().getIDMap());
    assertEquals(club.getGemId(), locked.getClubGem().getGemId());
    assertEquals(-1, anna.getClubGem().getGemId());
  }
}
