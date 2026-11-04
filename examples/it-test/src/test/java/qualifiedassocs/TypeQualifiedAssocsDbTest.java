/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Type qualified associations: Directory [Type] -> (role) Person is a map whose keys are chosen on put and stored in
 * the association table (Neo4j: the relationship)
 */
public interface TypeQualifiedAssocsDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-TypeQualifiedAssocs")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements TypeQualifiedAssocsDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-TypeQualifiedAssocs")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements TypeQualifiedAssocsDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  default Directory load(Directory directory) {
    return QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
  }

  @Test
  default void testPutGetRemove() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();

    directory.putByCode("a1", anna);
    assertSame(anna, directory.getByCode("a1"));
    assertTrue(directory.containsKeyByCode("a1"));
    assertTrue(directory.containsByCode(anna));
    assertFalse(directory.containsKeyByCode("b1"));

    // replacing the value under a key is no error
    directory.putByCode("a1", bob);
    assertSame(bob, directory.getByCode("a1"));
    assertFalse(directory.containsByCode(anna));

    // a value is stored under one key: putting it under another key moves it
    directory.putByCode("b1", bob);
    assertEquals(Map.of("b1", bob.getGemId()), directory._internal_getByCodeGemMap().getIDMap());

    assertTrue(directory.removeByCode("b1"));
    assertFalse(directory.removeByCode("b1"));
    assertEquals(0, directory.sizeByCodes());
  }

  @Test
  default void testKeysAreSavedToDb() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    // put while building (insert) and afterwards (DB observer)
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byCodePut("a1", anna)
        .bySeatPut(7, anna)
        .headByDepartmentPut(sales, anna)
        .byAliasFromRightPut("ann", anna)
        .build().get();
    directory.putByCode("b1", bob);
    directory.putBySeat(8, bob);
    directory.putHeadByDepartment(it, bob);
    directory.putByAliasFromRight("bobby", bob);

    var clone = load(directory);
    assertEquals(Map.of("a1", anna.getGemId(), "b1", bob.getGemId()), clone._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of(7, anna.getGemId(), 8, bob.getGemId()), clone._internal_getBySeatGemMap().getIDMap());
    assertEquals(Map.of(sales.getGemId(), anna.getGemId(), it.getGemId(), bob.getGemId()),
        clone._internal_getHeadByDepartmentGemMap().getIDMap());
    assertEquals(Map.of("ann", anna.getGemId(), "bobby", bob.getGemId()),
        clone._internal_getByAliasFromRightGemMap().getIDMap());
    assertEquals(bob.getGemId(), clone.getBySeat(8).getGemId());
    assertEquals(anna.getGemId(), clone.getHeadByDepartment(sales).getGemId());
  }

  /** moving a value to another key updates the key of its row, replacing a value replaces its row */
  @Test
  default void testMovedAndReplacedValuesAreSavedToDb() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byCodePut("a1", anna).build().get();

    directory.putByCode("a2", anna);
    assertEquals(Map.of("a2", anna.getGemId()), load(directory)._internal_getByCodeGemMap().getIDMap());

    directory.putByCode("a2", bob);
    assertEquals(Map.of("a2", bob.getGemId()), load(directory)._internal_getByCodeGemMap().getIDMap());
  }

  @Test
  default void testClientPutIsSynchronized() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientAnna = QualifiedAssocsManager.getPersonSure(anna.getGemId());
    clientDirectory.putByCode("a1", clientAnna);
    clientDirectory.putHeadByDepartment(QualifiedAssocsManager.getDepartmentSure(sales.getGemId()), clientAnna);

    getBackend().activate();
    assertSame(anna, directory.getByCode("a1"));
    assertSame(anna, directory.getHeadByDepartment(sales));
    var clone = load(directory);
    assertEquals(Map.of("a1", anna.getGemId()), clone._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of(sales.getGemId(), anna.getGemId()), clone._internal_getHeadByDepartmentGemMap().getIDMap());

    getClients(0).activate();
    clientDirectory.putByCode("a2", clientAnna);
    assertEquals(Map.of("a2", anna.getGemId()), clientDirectory._internal_getByCodeGemMap().getIDMap());

    getBackend().activate();
    assertFalse(directory.containsKeyByCode("a1"));
    assertSame(anna, directory.getByCode("a2"));
    assertEquals(Map.of("a2", anna.getGemId()), load(directory)._internal_getByCodeGemMap().getIDMap());

    getClients(0).activate();
    clientDirectory.removeByCode("a2");

    getBackend().activate();
    assertEquals(0, directory.sizeByCodes());
    assertEquals(0, load(directory).sizeByCodes());
  }

  /**
   * deleting a key object removes its entry (the value remains) from the maps of the loaded owners, on the server and
   * on the participants that loaded an owner, and from the database
   */
  @Test
  default void testDeleteKeyObject() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var directory = QualifiedAssocsManager.directoryBuilder()
        .headByDepartmentPut(sales, anna).headByDepartmentPut(it, bob).build().get();

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    assertEquals(2, clientDirectory.sizeHeadByDepartments());

    getBackend().activate();
    assertTrue(QualifiedAssocsManager.deleteDepartment(sales.getGemId()));
    assertEquals(Map.of(it.getGemId(), bob.getGemId()), directory._internal_getHeadByDepartmentGemMap().getIDMap());
    assertTrue(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(anna.getGemId()).isPresent());
    assertEquals(Map.of(it.getGemId(), bob.getGemId()),
        load(directory)._internal_getHeadByDepartmentGemMap().getIDMap());

    getClients(0).activate();
    assertEquals(Map.of(it.getGemId(), bob.getGemId()),
        clientDirectory._internal_getHeadByDepartmentGemMap().getIDMap());
  }

  /** a key object deleted while its owner is not loaded: the database deletes the pairs with that key */
  @Test
  default void testDeleteKeyObjectOfOwnerNotLoaded() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    long directoryId = QualifiedAssocsManager.directoryBuilder()
        .headByDepartmentPut(sales, anna).headByDepartmentPut(it, bob).build().get().getGemId();

    QualifiedAssocsManager.TESTING_clearCache();
    assertTrue(QualifiedAssocsManager.deleteDepartment(sales.getGemId()));

    var directory = QualifiedAssocsManager.getDirectorySure(directoryId);
    assertEquals(Map.of(it.getGemId(), bob.getGemId()), directory._internal_getHeadByDepartmentGemMap().getIDMap());
    assertSame(QualifiedAssocsManager.getDepartmentSure(it.getGemId()),
        directory.getHeadByDepartmentMap().keySet().iterator().next());
  }

  /** deleting a value removes its entries from the loaded owners and from the database */
  @Test
  default void testDeleteValue() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byCodePut("a1", anna).byCodePut("b1", bob)
        .byAliasFromRightPut("ann", anna).byAliasFromRightPut("bobby", bob)
        .build().get();

    assertTrue(QualifiedAssocsManager.deletePerson(anna.getGemId()));
    assertFalse(directory.containsKeyByCode("a1"));
    assertFalse(directory.containsKeyByAliasFromRight("ann"));

    var clone = load(directory);
    assertEquals(Map.of("b1", bob.getGemId()), clone._internal_getByCodeGemMap().getIDMap());
    assertEquals(Map.of("bobby", bob.getGemId()), clone._internal_getByAliasFromRightGemMap().getIDMap());
  }
}
