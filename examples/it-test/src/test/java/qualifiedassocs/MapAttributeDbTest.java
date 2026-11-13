/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Map attributes become type qualified associations (UMLAttrToAssocTrafo): Roster's {@code Map<String, Person> byNick}
 * and {@code java.util.Map<Department, Person> deputyByDepartment} behave like Directory's {@code [String]} and
 * {@code [Department]} maps, including the GemMap semantics that differ from java.util.Map (a value is under one key
 * only)
 */
public interface MapAttributeDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-MapAttribute")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements MapAttributeDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-MapAttribute")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements MapAttributeDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Roster load(Roster roster) {
    return QualifiedAssocsDAOFactory.getRosterDAO().selectRoster(roster.getGemId()).get();
  }

  /** a String keyed map attribute is a GemValueMap with the API of a type qualified association */
  @Test
  default void testStringKeyedMapAttribute() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var ben = QualifiedAssocsManager.personBuilder().login("ben").number(2).build().get();
    var roster = QualifiedAssocsManager.rosterBuilder().byNickPut("an", anna).build().get();
    roster.putByNick("be", ben);

    assertSame(anna, roster.getByNick("an"));
    assertTrue(roster.containsKeyByNick("be"));
    assertEquals(2, roster.sizeByNicks());
    assertEquals(Set.of("an", "be"), roster.getByNickMap().keySet());

    var loaded = load(roster);
    assertEquals(Set.of("an", "be"), loaded.getByNickMap().keySet());
    assertEquals(anna.getGemId(), loaded.getByNick("an").getGemId());
    assertEquals(ben.getGemId(), loaded.getByNick("be").getGemId());
  }

  /** unlike java.util.Map, a value is under one key only: a put under another key moves it */
  @Test
  default void testPutUnderAnotherKeyMovesTheValue() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var roster = QualifiedAssocsManager.rosterBuilder().build().get();
    roster.putByNick("an", anna);
    roster.putByNick("annie", anna);

    assertFalse(roster.containsKeyByNick("an"));
    assertSame(anna, roster.getByNick("annie"));
    assertEquals(Set.of("annie"), load(roster).getByNickMap().keySet());
  }

  /** a map attribute keyed by a domain class is a GemEntryMap */
  @Test
  default void testDomainClassKeyedMapAttribute() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var roster = QualifiedAssocsManager.rosterBuilder().build().get();
    roster.putDeputyByDepartment(sales, anna);

    assertSame(anna, roster.getDeputyByDepartment(sales));
    var loaded = load(roster);
    assertTrue(loaded.containsKeyDeputyByDepartment(sales));
    assertEquals(anna.getGemId(), loaded.getDeputyByDepartment(sales).getGemId());

    roster.removeDeputyByDepartment(sales);
    assertFalse(load(roster).containsKeyDeputyByDepartment(sales));
  }
}
