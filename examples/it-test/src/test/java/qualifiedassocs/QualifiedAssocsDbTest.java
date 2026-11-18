/* (c) https://github.com/MontiCore/monticore */
package qualifiedassocs;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import qualifiedassocs.commands.Directory_putByLogin;
import qualifiedassocs.test_infrastructure.QualifiedAssocsNeo4JDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsSQLDBTestSetup;
import qualifiedassocs.test_infrastructure.QualifiedAssocsTestSetup;
import umlp.backendrte.command.CommandManager;

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

  /**
   * runs an action that is expected to log an error starting with message, e.g. a put that replaces a value deriving
   * the same key: the server reports it (0xA7307, the qualifying attribute is not unique) and replaces the value
   */
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

  /**
   * a put command for a value that does not exist: the server rejects it (contract), so a participant that loaded the
   * owner (and puts by id, without loading) stores nothing either
   */
  @Test
  default void testPutOfMissingObjectIsRejected() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    assertEquals(0, clientDirectory.sizeByLogins());

    getBackend().activate();
    assertFalse(CommandManager.executeCommand(new Directory_putByLogin(directory.getGemId(), 999_999_999L, "ghost"))
        .isSuccessful());
    assertEquals(0, directory.sizeByLogins());
    assertEquals(0, QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get()
        .sizeByLogins());
    getClients(0).activate();
    assertEquals(0, clientDirectory.sizeByLogins());
  }

  @Test
  default void testPutGetRemove() {
    getBackend().activate();
    var directory = QualifiedAssocsManager.directoryBuilder().build().get();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var otherAnna = QualifiedAssocsManager.personBuilder().login("anna").number(2).build().get();

    directory.putByLogin(anna);
    assertSame(anna, directory.getByLogin("anna"));
    assertTrue(directory.containsKeyByLogin("anna"));
    assertTrue(directory.containsByLogin(anna));
    assertEquals(1, directory.sizeByLogins());
    assertFalse(directory.containsKeyByLogin("bob"));

    // the key is unique: putting another target with the same key replaces the first
    assertLogsError("0xA7307", () -> directory.putByLogin(otherAnna));
    assertSame(otherAnna, directory.getByLogin("anna"));
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
    assertEquals(bob.getGemId(), clone.getByNumber(2).getGemId());
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
    assertEquals("anna", directory.getByLogin("anna").getLogin());
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
    assertFalse(directory.containsKeyByLogin("anna"));
    assertSame(anna, directory.getByLogin("annabelle"));
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
    assertSame(anna, directory.getByLogin("annabelle"));
  }

  @Test
  default void testInheritedKeyAttribute() {
    getBackend().activate();
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().adminByLoginPut(admin).build().get();
    assertSame(admin, directory.getAdminByLogin("root"));

    admin.setLogin("admin");
    assertSame(admin, directory.getAdminByLogin("admin"));
    assertFalse(directory.containsKeyAdminByLogin("root"));
  }

  @Test
  default void testRekeySubclassValue() {
    getBackend().activate();
    // Admin values in a map of Persons: Admin notifies its own observers only
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(admin).build().get();

    admin.setLogin("admin");
    assertSame(admin, directory.getByLogin("admin"));
    assertFalse(directory.containsKeyByLogin("root"));
  }

  @Test
  default void testValueThatLeftTheMapIsNotRekeyed() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var otherAnna = QualifiedAssocsManager.personBuilder().login("anna").number(2).build().get();
    var admin = QualifiedAssocsManager.adminBuilder().login("root").number(0).build().get();
    int observers = anna.personObservers.size();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).byLoginPut(admin).build().get();
    QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).build().get();
    // the manager observes the values, one key observer per map role, however many maps hold them
    assertEquals(observers, anna.personObservers.size());

    // replaced by another value for its key
    assertLogsError("0xA7307", () -> directory.putByLogin(otherAnna));
    anna.setLogin("annabelle");
    assertFalse(directory.containsKeyByLogin("annabelle"));
    assertSame(otherAnna, directory.getByLogin("anna"));

    // removed; the subclass value is observed with its own observer
    directory.removeByLogin("root");
    admin.setLogin("admin");
    assertFalse(directory.containsKeyByLogin("admin"));
    assertFalse(directory.containsKeyByLogin("root"));
  }

  @Test
  default void testDomainObjectKeyRekeyIsSynchronized() {
    getBackend().activate();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byDepartmentPut(eve).build().get();
    assertSame(eve, directory.getByDepartment(sales));

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var clientEve = QualifiedAssocsManager.getEmployeeList().get(0);
    var clientIt = QualifiedAssocsManager.getDepartmentSure(it.getGemId());
    assertTrue(clientDirectory.containsKeyByDepartment(QualifiedAssocsManager.getDepartmentSure(sales.getGemId())));
    clientEve.setDepartment(clientIt);
    assertTrue(clientDirectory.containsKeyByDepartment(clientIt));

    getBackend().activate();
    assertSame(eve, directory.getByDepartment(it));
    assertFalse(directory.containsKeyByDepartment(sales));
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
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();

    getClients(0).activate();
    var clientMap = QualifiedAssocsManager.getDirectoryList().get(0).getByLoginMap();
    // by gemId: the order of getPersonList() is not defined
    clientMap.put("anna", QualifiedAssocsManager.getPersonSure(anna.getGemId()));
    clientMap.put("bob", QualifiedAssocsManager.getPersonSure(bob.getGemId()));

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
  default void testLiveViews() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).byLoginPut(bob).build().get();

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    var keys = clientDirectory.getByLoginMap().keySet();
    var values = clientDirectory.getByLoginMap().values();
    assertEquals(Set.of("anna", "bob"), keys);

    // rekeying shows up in the view
    QualifiedAssocsManager.getPersonSure(anna.getGemId()).setLogin("annabelle");
    assertEquals(Set.of("annabelle", "bob"), keys);

    // removing through the views is synchronized
    keys.remove("bob");
    assertEquals(1, values.size());
    values.remove(QualifiedAssocsManager.getPersonSure(anna.getGemId()));

    getBackend().activate();
    assertTrue(directory.getByLoginMap().isEmpty());
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(0, clone.sizeByLogins());
  }

  /** a loaded map knows its values by id only: contains/remove of the views derive the keys first */
  @Test
  default void testViewsOfMapWithPendingKeys() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byLoginPut(anna).byLoginPut(bob).byDepartmentPut(eve).build().get();

    getClients(0).activate();
    var clientDirectory = QualifiedAssocsManager.getDirectoryList().get(0);
    // no access before: the first operations on the maps are the ones of the views
    assertTrue(clientDirectory.getByLoginMap().keySet().contains("anna"));
    assertTrue(clientDirectory.getByLoginMap().keySet().remove("bob"));
    var clientEntry = Map.entry(QualifiedAssocsManager.getDepartmentSure(sales.getGemId()),
        QualifiedAssocsManager.getEmployeeSure(eve.getGemId()));
    assertTrue(clientDirectory.getByDepartmentMap().entrySet().contains(clientEntry));
    assertTrue(clientDirectory.getByDepartmentMap().entrySet().remove(clientEntry));

    getBackend().activate();
    assertTrue(directory.containsKeyByLogin("anna"));
    assertFalse(directory.containsKeyByLogin("bob"));
    assertTrue(directory.getByDepartmentMap().isEmpty());
  }

  @Test
  default void testEntryMapViews() {
    getBackend().activate();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var tom = QualifiedAssocsManager.employeeBuilder().name("tom").department(it).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byDepartmentPut(eve).byDepartmentPut(tom).build().get();

    assertEquals(Set.of(sales, it), directory.getByDepartmentMap().keySet());
    for (var entry : directory.getByDepartmentMap().entrySet()) {
      assertSame(entry.getKey(), entry.getValue().getDepartment());
    }
  }

  @Test
  default void testDomainObjectKey() {
    getBackend().activate();
    var sales = QualifiedAssocsManager.departmentBuilder().name("sales").build().get();
    var it = QualifiedAssocsManager.departmentBuilder().name("it").build().get();
    var eve = QualifiedAssocsManager.employeeBuilder().name("eve").department(sales).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byDepartmentPut(eve).build().get();

    assertSame(eve, directory.getByDepartment(sales));
    assertFalse(directory.containsKeyByDepartment(it));

    eve.setDepartment(it);
    assertSame(eve, directory.getByDepartment(it));
    assertFalse(directory.containsKeyByDepartment(sales));

    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(eve.getGemId(), clone.getByDepartment(it).getGemId());
  }

  /** qualifier on the right side: the owner is the right side of the association and of its table */
  @Test
  default void testRightSideQualifier() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginFromRightPut(anna).build().get();
    directory.putByLoginFromRight(bob);
    assertSame(anna, directory.getByLoginFromRight("anna"));
    assertSame(bob, directory.getByLoginFromRight("bob"));

    // saved and loaded: the keys are derived from the loaded targets
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(Map.of("anna", anna.getGemId(), "bob", bob.getGemId()),
        clone._internal_getByLoginFromRightGemMap().getIDMap());

    anna.setLogin("annabelle");
    assertSame(anna, directory.getByLoginFromRight("annabelle"));
    assertFalse(directory.containsKeyByLoginFromRight("anna"));
    assertTrue(directory.removeByLoginFromRight("bob"));

    clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(Map.of("annabelle", anna.getGemId()), clone._internal_getByLoginFromRightGemMap().getIDMap());
  }

  /**
   * deleting a value removes its entry (key and value) from the maps of the loaded owners and from the database
   * (qualifier on the left and on the right side)
   */
  @Test
  default void testDeleteValue() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder()
        .byLoginPut(anna).byLoginPut(bob)
        .byLoginFromRightPut(anna).byLoginFromRightPut(bob)
        .build().get();

    assertTrue(QualifiedAssocsManager.deletePerson(anna.getGemId()));
    assertFalse(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(anna.getGemId()).isPresent());

    assertFalse(directory.containsKeyByLogin("anna"));
    assertFalse(directory.containsKeyByLoginFromRight("anna"));
    assertEquals(1, directory.sizeByLogins());
    assertSame(bob, directory.getByLogin("bob"));
    assertSame(bob, directory.getByLoginFromRight("bob"));

    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).get();
    assertEquals(Map.of("bob", bob.getGemId()), clone._internal_getByLoginGemMap().getIDMap());
    assertEquals(Map.of("bob", bob.getGemId()), clone._internal_getByLoginFromRightGemMap().getIDMap());
  }

  /**
   * a value deleted while its owner is not loaded: the loaded owner does not know the value any more (its keys are
   * derived lazily, a value that cannot be loaded is dropped), also if a row of the association outlived the value
   */
  @Test
  default void testDeleteValueOfOwnerNotLoaded() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    long directoryId = QualifiedAssocsManager.directoryBuilder()
        .byLoginPut(anna).byLoginPut(bob)
        .byLoginFromRightPut(anna).byLoginFromRightPut(bob)
        .build().get().getGemId();

    // neither the owner nor the value is loaded any more: the value is deleted from the database only
    QualifiedAssocsManager.TESTING_clearCache();
    assertTrue(QualifiedAssocsManager.deletePerson(anna.getGemId()));
    assertFalse(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(anna.getGemId()).isPresent());

    var directory = QualifiedAssocsManager.getDirectorySure(directoryId);
    assertFalse(directory.containsKeyByLogin("anna"));
    assertEquals(Map.of("bob", bob.getGemId()), directory._internal_getByLoginGemMap().getIDMap());
    assertEquals(Map.of("bob", bob.getGemId()), directory._internal_getByLoginFromRightGemMap().getIDMap());
  }

  /** deleting the owner deletes its association rows, the values remain (qualifier on the left and on the right side) */
  @Test
  default void testDeleteOwner() {
    getBackend().activate();
    var anna = QualifiedAssocsManager.personBuilder().login("anna").number(1).build().get();
    var bob = QualifiedAssocsManager.personBuilder().login("bob").number(2).build().get();
    var directory = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).byLoginFromRightPut(bob).build().get();

    assertTrue(QualifiedAssocsManager.deleteDirectory(directory.getGemId()));
    assertFalse(QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(directory.getGemId()).isPresent());
    assertTrue(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(anna.getGemId()).isPresent());
    assertTrue(QualifiedAssocsDAOFactory.getPersonDAO().selectPerson(bob.getGemId()).isPresent());

    // the values can be put into another owner, which is saved and loaded with them only
    var other = QualifiedAssocsManager.directoryBuilder().byLoginPut(anna).byLoginFromRightPut(bob).build().get();
    var clone = QualifiedAssocsDAOFactory.getDirectoryDAO().selectDirectory(other.getGemId()).get();
    assertEquals(Map.of("anna", anna.getGemId()), clone._internal_getByLoginGemMap().getIDMap());
    assertEquals(Map.of("bob", bob.getGemId()), clone._internal_getByLoginFromRightGemMap().getIDMap());
  }
}
