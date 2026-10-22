/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Attribute qualified associations: Directory [[attr]] -> (role) Target is a map keyed by attr of the targets
 */
public interface QualifiedAssocsDbTest extends QualifiedAssocsTestSetup {

  @DisplayName("Neo4J-QualifiedAssocs")
  public static class Neo4JTestSuite extends QualifiedAssocsNeo4JDBTestSetup implements QualifiedAssocsDbTest {
    Neo4JTestSuite() {
      super(1);
    }
  }

  @DisplayName("SQL-QualifiedAssocs")
  public static class SQLTestSuite extends QualifiedAssocsSQLDBTestSetup implements QualifiedAssocsDbTest {
    SQLTestSuite() {
      super(1);
    }
  }

  @Test
  default void testPutGetRemove() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var otherAnna = QualifiedAssocsManager.personBuilder().login("anna").number(2).build().get();

    directory.putByLogin(anna);
    assertSame(anna, directory.getByLogin("anna").get());
    assertTrue(directory.containsKeyByLogin("anna"));
    assertTrue(directory.containsByLogin(anna));
    assertEquals(1, directory.sizeByLogins());
    assertTrue(directory.getByLogin("bob").isEmpty());

    // the key is unique: putting another target with the same key replaces the first
    directory.putByLogin(otherAnna);
    assertSame(otherAnna, directory.getByLogin("anna").get());
    assertFalse(directory.containsByLogin(anna));
    assertEquals(1, directory.sizeByLogins());

    assertTrue(directory.removeByLogin("anna"));
    assertFalse(directory.removeByLogin("anna"));
    assertEquals(0, directory.sizeByLogins());
  }

  @Test
  default void testMapIsSavedToDb() {
    getClients(0).activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byLoginPut(anna)
        .departmentByNamePut(sales)
        .build().get();
    directory.putByLogin(bob);
    directory.putByNumber(bob);

    getBackend().activate();
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    // the keys are derived from the loaded targets
    assertEquals(Map.of("anna", anna.getGemId(), "bob", bob.getGemId()), clone._internal_getByLoginGemMap().getIDMap());
    assertEquals(bob.getGemId(), clone.getByNumber(2).get().getGemId());
    assertEquals(sales.getGemId(), clone.getDepartmentByName("sales").getGemId());
  }

  @Test
  default void testClientPutAndRemoveIsSynchronized() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientAnna = QualifiedAssocsManager.getPersonList().get(0);
    clientDirectory.putByLogin(clientAnna);

    getBackend().activate();
    assertEquals("anna", directory.getByLogin("anna").get().getLogin());
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertTrue(clone.containsKeyByLogin("anna"));

    getClients(0).activate();
    clientDirectory.removeByLogin("anna");

    getBackend().activate();
    assertFalse(directory.containsKeyByLogin("anna"));
    clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(0, clone.sizeByLogins());
  }

  @Test
  default void testRekeyOnAttributeChange() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).build().get();

    anna.setLogin("annabelle");
    assertTrue(directory.getByLogin("anna").isEmpty());
    assertSame(anna, directory.getByLogin("annabelle").get());
  }

  @Test
  default void testRekeyOnSynchronizedAttributeChange() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).build().get();
    assertTrue(directory.containsKeyByLogin("anna"));

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientAnna = QualifiedAssocsManager.getPersonList().get(0);
    assertTrue(clientDirectory.containsKeyByLogin("anna"));
    clientAnna.setLogin("annabelle");
    assertTrue(clientDirectory.containsKeyByLogin("annabelle"));

    getBackend().activate();
    assertFalse(directory.containsKeyByLogin("anna"));
    assertSame(anna, directory.getByLogin("annabelle").get());
  }

  @Test
  default void testInheritedKeyAttribute() {
    getBackend().activate();
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().adminByLoginPut(admin).build().get();
    assertSame(admin, directory.getAdminByLogin("root").get());

    admin.setLogin("admin");
    assertSame(admin, directory.getAdminByLogin("admin").get());
    assertTrue(directory.getAdminByLogin("root").isEmpty());
  }

  @Test
  default void testRekeySubclassValue() {
    getBackend().activate();
    // Admin values in a map of Persons: Admin notifies its own observers only
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(admin).build().get();

    admin.setLogin("admin");
    assertSame(admin, directory.getByLogin("admin").get());
    assertTrue(directory.getByLogin("root").isEmpty());
  }

  @Test
  default void testRekeyObserverIsRemovedWithValue() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var otherAnna = QualifiedAssocsManager.personBuilder().login("anna").number(2).build().get();
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    int observers = anna.personObservers.size();
    int adminObservers = admin.personObservers.size() + admin.adminObservers.size();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).byLoginPut(admin).build().get();
    assertEquals(observers + 1, anna.personObservers.size());

    // replaced by another value for its key
    directory.putByLogin(otherAnna);
    assertEquals(observers, anna.personObservers.size());

    // removed; the subclass value is observed with its own observer, also registered with the superclass
    directory.removeByLogin("root");
    assertEquals(adminObservers, admin.personObservers.size() + admin.adminObservers.size());
  }

  @Test
  default void testDomainObjectKeyRekeyIsSynchronized() {
    getBackend().activate();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byDepartmentPut(eve).build().get();
    assertSame(eve, directory.getByDepartment(sales).get());

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientEve = QualifiedAssocsManager.getEmployeeList().get(0);
    var clientIt = QualifiedAssocsManager.getDepartmentSure(it.getGemId());
    assertTrue(clientDirectory.containsKeyByDepartment(QualifiedAssocsManager.getDepartmentSure(sales.getGemId())));
    clientEve.setDepartment(clientIt);
    assertTrue(clientDirectory.containsKeyByDepartment(clientIt));

    getBackend().activate();
    assertSame(eve, directory.getByDepartment(it).get());
    assertTrue(directory.getByDepartment(sales).isEmpty());
  }

  @Test
  default void testMapIsLive() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var map = directory.getByLoginMap();

    directory.putByLogin(anna);
    assertSame(anna, map.get("anna"));
    anna.setLogin("annabelle");
    assertTrue(map.containsKey("annabelle"));
    assertFalse(map.containsKey("anna"));
    directory.removeByLogin("annabelle");
    assertTrue(map.isEmpty());
  }

  @Test
  default void testModificationsThroughMapAreSynchronized() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();

    getClients(0).activate();
    var clientMap = QualifiedAssocsManager.getDirectoryList().get(0).getByLoginMap();
    var clientPersons = QualifiedAssocsManager.getPersonList();
    clientMap.put("anna", clientPersons.get(0));
    clientMap.put("bob", clientPersons.get(1));

    getBackend().activate();
    assertEquals(2, directory.sizeByLogins());
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(2, clone.sizeByLogins());

    getClients(0).activate();
    clientMap.remove("anna");

    getBackend().activate();
    assertEquals(Set.of("bob"), directory.getByLoginMap().keySet());

    getClients(0).activate();
    clientMap.clear();

    getBackend().activate();
    assertTrue(directory.getByLoginMap().isEmpty());
    clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(0, clone.sizeByLogins());
  }

  @Test
  default void testDomainObjectKey() {
    getBackend().activate();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byDepartmentPut(eve).build().get();

    assertSame(eve, directory.getByDepartment(sales).get());
    assertTrue(directory.getByDepartment(it).isEmpty());

    eve.setDepartment(it);
    assertSame(eve, directory.getByDepartment(it).get());
    assertTrue(directory.getByDepartment(sales).isEmpty());

    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(eve.getGemId(), clone.getByDepartment(it).get().getGemId());
  }
}
