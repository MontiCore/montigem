/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Type qualified maps of a singleton owner (Registry [String] -> (byCode) Person, Registry [Department] ->
 * (headByDepartment) Person): the manager holds the singleton itself, so deleting a value or a key object takes its
 * own branch (DoDelete)
 */
public interface TypeQualifiedSingletonDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-TypeQualifiedSingleton")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements TypeQualifiedSingletonDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-TypeQualifiedSingleton")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements TypeQualifiedSingletonDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Registry load(Registry registry) {
    return QualifiedAssocsDAOFactory.getRegistryDAO().selectRegistry(registry.getGemId()).get();
  }

  default Person person(String login, int number) {
    return QualifiedAssocsManager.personBuilder().login(login).number(number).build().get();
  }

  default Department department(String name) {
    return QualifiedAssocsManager.departmentBuilder().name(name).build().get();
  }

  /** puts into the maps of the singleton are stored and reach a client */
  @Test
  default void testPutIsSavedAndSynchronized() {
    getBackend().activate();
    var registry = QualifiedAssocsManager.getRegistry();
    var anna = person("anna", 1);
    var sales = department("sales");
    registry.putByCode("a1", anna);
    registry.putHeadByDepartment(sales, anna);
    var clone = load(registry);
    assertEquals(Map.of("a1", anna.getGemId()), clone._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of(sales.getGemId(), anna.getGemId()), clone._internal_getHeadByDepartmentGemMap().getIDMap());

    getClients(0).activate();
    var clientRegistry = QualifiedAssocsManager.getRegistrySure(registry.getGemId());
    assertEquals(Map.of("a1", anna.getGemId()), clientRegistry._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of(sales.getGemId(), anna.getGemId()),
        clientRegistry._internal_getHeadByDepartmentGemMap().getIDMap());
  }

  /** deleting a value removes its entries from the maps of the singleton, on the server, a client and in the db */
  @Test
  default void testDeleteValue() {
    getBackend().activate();
    var registry = QualifiedAssocsManager.getRegistry();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    registry.putByCode("a1", anna);
    registry.putByCode("b1", bob);
    getClients(0).activate();
    var clientRegistry = QualifiedAssocsManager.getRegistrySure(registry.getGemId());
    assertEquals(2, clientRegistry.sizeByCodes());

    getBackend().activate();
    assertTrue(QualifiedAssocsManager.deletePerson(anna.getGemId()));
    assertEquals(Map.of("b1", bob.getGemId()), registry._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of("b1", bob.getGemId()), load(registry)._internal_getByCodeGemMap().getIDMap());
    getClients(0).activate();
    assertEquals(Map.of("b1", bob.getGemId()), clientRegistry._internal_getByCodeGemMap().getIDMap());
  }

  /**
   * deleting a key object removes its entry (the value remains) from the map of the singleton, on the server, a client
   * and in the db
   */
  @Test
  default void testDeleteKeyObject() {
    getBackend().activate();
    var registry = QualifiedAssocsManager.getRegistry();
    var anna = person("anna", 1);
    var bob = person("bob", 2);
    var sales = department("sales");
    var it = department("it");
    registry.putHeadByDepartment(sales, anna);
    registry.putHeadByDepartment(it, bob);
    getClients(0).activate();
    var clientRegistry = QualifiedAssocsManager.getRegistrySure(registry.getGemId());
    assertEquals(2, clientRegistry.sizeHeadByDepartments());

    getBackend().activate();
    assertTrue(QualifiedAssocsManager.deleteDepartment(sales.getGemId()));
    assertEquals(Map.of(it.getGemId(), bob.getGemId()), registry._internal_getHeadByDepartmentGemMap().getIDMap());
    assertEquals(Map.of(it.getGemId(), bob.getGemId()),
        load(registry)._internal_getHeadByDepartmentGemMap().getIDMap());
    assertTrue(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(anna.getGemId()).isPresent());
    getClients(0).activate();
    assertEquals(Map.of(it.getGemId(), bob.getGemId()),
        clientRegistry._internal_getHeadByDepartmentGemMap().getIDMap());
  }
}
