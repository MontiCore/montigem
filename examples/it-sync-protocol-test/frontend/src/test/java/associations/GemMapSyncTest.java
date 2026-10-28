/* (c) https://github.com/MontiCore/monticore */
package associations;

import associations.commands.Course_putParticipantByMatNr;
import associations.commands.Course_rekeyParticipantByMatNr;
import associations.test_infrastructure.AssociationsMockTestSetup;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import umlp.backendrte.command.CommandManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synchronization of attribute qualified associations (GemMaps) between the backend and two clients.
 * Maps and changes carry key/id pairs, so a client knows the keys without loading the values. The server
 * derives the keys and sends rekeying (command _rekey) to the participants that loaded the owner.
 */
public class GemMapSyncTest extends AssociationsMockTestSetup {

  long courseId;
  long annaId;
  long benId;
  long tutorId;
  long enrollmentId;

  public GemMapSyncTest() {
    super(2);
  }

  @BeforeEach
  public void createObjectsInBackend() {
    getBackend().activate();
    this.courseId = AssociationsManager.courseBuilder().title("Software Engineering").build().get().getGemId();
    Student anna = AssociationsManager.studentBuilder().name("Anna").matNr(1001).build().get();
    this.annaId = anna.getGemId();
    this.benId = AssociationsManager.studentBuilder().name("Ben").matNr(1002).build().get().getGemId();
    this.tutorId = AssociationsManager.tutorBuilder().staffId("T1").weeklyHours(10).build().get().getGemId();
    this.enrollmentId = AssociationsManager.enrollmentBuilder().student(anna).grade(2).build().get().getGemId();
  }

  Course course() {
    return AssociationsManager.getCourseSure(courseId);
  }

  /** for the tests that provoke errors: collect them instead of failing */
  void collectFindings() {
    Log.init();
    Log.enableFailQuick(false);
  }

  boolean logged(String code) {
    return Log.getFindings().stream().anyMatch(f -> f.getMsg().startsWith(code));
  }

  @AfterEach
  public void resetLog() {
    Log.init();
  }

  @Test
  public void testPutOnClientReachesBackendAndOtherClient() {
    // client 1 keyed its (empty) map before
    getClients(1).activate();
    assertEquals(0, course().sizeParticipantByMatNrs());

    getClients(0).activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getBackend().activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertEquals(annaId, course().getParticipantByMatNr(1001).getGemId());

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertEquals(annaId, course().getParticipantByMatNr(1001).getGemId());
  }

  /** the change carries the key: client 1 knows it without loading the value */
  @Test
  public void testRemotePutCarriesKeyToOtherClient() {
    getClients(1).activate();
    Course client1Course = course();

    getClients(0).activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));

    getClients(1).activate();
    assertTrue(client1Course.containsKeyParticipantByMatNr(1002));
    assertFalse(AssociationsManager.isLoadedStudent(benId));
    assertEquals("Ben", client1Course.getParticipantByMatNr(1002).getName());
  }

  /** a loaded map carries its keys: size, keys and ids are known without loading any value */
  @Test
  public void testLoadedMapKnowsKeysWithoutLoadingValues() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));

    getClients(1).activate();
    assertEquals(2, course().sizeParticipantByMatNrs());
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertTrue(course().containsKeyParticipantByMatNr(1002));
    assertEquals(annaId, course()._internal_getParticipantByMatNrGemMap().getID(1001));
    assertFalse(AssociationsManager.isLoadedStudent(annaId));
    assertFalse(AssociationsManager.isLoadedStudent(benId));
  }

  /** client 1 keyed its map, so it observes the value and rekeys it when client 0 changes the key */
  @Test
  public void testRekeyOnOtherClientWithKeyedMap() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));

    getClients(0).activate();
    AssociationsManager.getStudentSure(annaId).setMatNr(2001);

    getClients(1).activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertTrue(course().containsKeyParticipantByMatNr(2001));

    getBackend().activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertTrue(course().containsKeyParticipantByMatNr(2001));
  }

  /**
   * client 1 never loaded the value, so it does not receive its setter command: the server sends the rekeying
   * through the owner (command _rekey)
   */
  @Test
  public void testRekeyOnOtherClientWithoutTheValue() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    Course client1Course = course();
    assertTrue(client1Course.containsKeyParticipantByMatNr(1001));

    getClients(0).activate();
    AssociationsManager.getStudentSure(annaId).setMatNr(2001);

    getClients(1).activate();
    assertFalse(client1Course.containsKeyParticipantByMatNr(1001));
    assertTrue(client1Course.containsKeyParticipantByMatNr(2001));
    assertFalse(AssociationsManager.isLoadedStudent(annaId));
  }

  /** a change on the backend itself is rekeyed on the clients through the owner, too */
  @Test
  public void testRekeyByBackend() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));

    getBackend().activate();
    AssociationsManager.getStudentSure(annaId).setMatNr(3001);

    getClients(1).activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertTrue(course().containsKeyParticipantByMatNr(3001));
  }

  /** a key that does not match the value (stale on the client) is replaced by the server's derived key */
  @Test
  public void testPutWithWrongKeyIsCorrectedByServer() {
    getClients(1).activate();
    assertEquals(0, course().sizeParticipantByMatNrs());

    getClients(0).activate();
    CommandManager.executeCommand(new Course_putParticipantByMatNr(courseId, annaId, 9999));

    getBackend().activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(9999));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(9999));
  }

  /** only the server sends rekeying: it rejects a rekeying sent by a client and does not send it on */
  @Test
  public void testRekeyFromClientIsRejected() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));

    getClients(0).activate();
    assertFalse(CommandManager.executeCommand(new Course_rekeyParticipantByMatNr(courseId, annaId, 1001, 7777))
        .isSuccessful());

    getBackend().activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(7777));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(7777));
  }

  @Test
  public void testRemoveOnClient() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));

    getClients(1).activate();
    assertEquals(2, course().sizeParticipantByMatNrs());

    getClients(0).activate();
    course().removeParticipantByMatNr(1002);

    getBackend().activate();
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertFalse(course().containsKeyParticipantByMatNr(1002));

    getClients(1).activate();
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertFalse(course().containsKeyParticipantByMatNr(1002));
  }

  /**
   * a removal does not subscribe client 1 to the removed value: it does not receive (and load) the value with its
   * next change
   */
  @Test
  public void testRemoveDoesNotLoadValueOnOtherClient() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));

    getClients(0).activate();
    course().removeParticipantByMatNr(1001);
    AssociationsManager.getStudentSure(annaId).setMatNr(4001);

    getClients(1).activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(4001));
    assertFalse(AssociationsManager.isLoadedStudent(annaId));
  }

  /** a builder on client 0 keeps the value until it is built */
  @Test
  public void testBuilderOnClient() {
    getClients(0).activate();
    Course built = AssociationsManager.courseBuilder().title("Built On Client")
        .participantByMatNrPut(AssociationsManager.getStudentSure(annaId)).build().get();
    long builtId = built.getGemId();
    assertTrue(built.containsKeyParticipantByMatNr(1001));

    getBackend().activate();
    assertTrue(AssociationsManager.getCourseSure(builtId).containsKeyParticipantByMatNr(1001));

    getClients(1).activate();
    assertTrue(AssociationsManager.getCourseSure(builtId).containsKeyParticipantByMatNr(1001));
  }

  /** the key attribute staffId is declared in Staff, the superclass of the value Tutor */
  @Test
  public void testKeyOfSuperclass() {
    getClients(1).activate();
    assertEquals(0, course().sizeTutorByStaffIds());

    getClients(0).activate();
    course().putTutorByStaffId(AssociationsManager.getTutorSure(tutorId));

    getClients(1).activate();
    assertTrue(course().containsKeyTutorByStaffId("T1"));

    getClients(0).activate();
    AssociationsManager.getTutorSure(tutorId).setStaffId("T2");

    getClients(1).activate();
    assertFalse(course().containsKeyTutorByStaffId("T1"));
    assertTrue(course().containsKeyTutorByStaffId("T2"));

    getBackend().activate();
    assertTrue(course().containsKeyTutorByStaffId("T2"));
  }

  /**
   * a value is stored under one key only: after its rekeying was refused it keeps its old key; putting it again moves
   * it to the key it derives, on every participant, instead of storing it twice
   */
  @Test
  public void testPutMovesValueFromStaleKey() {
    collectFindings();
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));

    getClients(1).activate();
    assertEquals(2, course().sizeParticipantByMatNrs());

    getBackend().activate();
    // Ben takes Anna's matNr: his rekeying is refused, he stays under 1002
    AssociationsManager.getStudentSure(benId).setMatNr(1001);
    assertTrue(logged("0xA7304"));
    assertEquals(benId, course().getParticipantByMatNr(1002).getGemId());

    // Anna leaves, Ben is put again: moved from 1002 to 1001
    course().removeParticipantByMatNr(1001);
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertEquals(benId, course().getParticipantByMatNr(1001).getGemId());
    assertFalse(course().containsKeyParticipantByMatNr(1002));

    getClients(1).activate();
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertFalse(course().containsKeyParticipantByMatNr(1002));
    assertFalse(logged("0xA7307"));
  }

  /**
   * a put replacing a value that derives the same key (the qualifying attribute is not unique) is reported by the
   * server; the value is replaced nonetheless, like Map#put, as the client that put it has replaced it already
   */
  @Test
  public void testPutReplacingValueWithSameKeyIsReported() {
    collectFindings();
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    assertEquals(1, course().sizeParticipantByMatNrs());

    getClients(0).activate();
    Student ben = AssociationsManager.getStudentSure(benId);
    ben.setMatNr(1001);
    course().putParticipantByMatNr(ben);

    getBackend().activate();
    assertTrue(logged("0xA7307"));
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertEquals(benId, course().getParticipantByMatNr(1001).getGemId());

    getClients(1).activate();
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertEquals(benId, course().getParticipantByMatNr(1001).getGemId());
  }

  /** replacing a stale entry (its value derives another key by now) is a repair, not an error */
  @Test
  public void testPutReplacingStaleEntryIsNotReported() {
    collectFindings();
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));
    // Anna takes Ben's matNr: her rekeying is refused, she stays under 1001
    AssociationsManager.getStudentSure(annaId).setMatNr(1002);
    assertTrue(logged("0xA7304"));

    long carlId = AssociationsManager.studentBuilder().name("Carl").matNr(1001).build().get().getGemId();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(carlId));
    assertFalse(logged("0xA7307"));
    assertEquals(2, course().sizeParticipantByMatNrs());
    assertEquals(carlId, course().getParticipantByMatNr(1001).getGemId());
    assertEquals(benId, course().getParticipantByMatNr(1002).getGemId());
  }

  @Test
  public void testEntryMapPutReplacingValueWithSameKeyIsReported() {
    collectFindings();
    getBackend().activate();
    Student anna = AssociationsManager.getStudentSure(annaId);
    course().putEnrollmentByStudent(AssociationsManager.getEnrollmentSure(enrollmentId));
    long secondId = AssociationsManager.enrollmentBuilder().student(anna).grade(1).build().get().getGemId();
    course().putEnrollmentByStudent(AssociationsManager.getEnrollmentSure(secondId));

    assertTrue(logged("0xA7317"));
    assertEquals(1, course().sizeEnrollmentByStudents());
    assertEquals(secondId, course().getEnrollmentByStudent(anna).getGemId());
  }

  /**
   * deleting a value (not removing it from one map) removes its entry from every map holding it, on every participant
   * that loaded the owner, without loading the value there
   */
  @Test
  public void testDeleteValueRemovesItsEntryEverywhere() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));
    long otherCourseId = AssociationsManager.courseBuilder().title("Compilers")
        .participantByMatNrPut(AssociationsManager.getStudentSure(annaId)).build().get().getGemId();

    getClients(1).activate();
    assertEquals(2, course().sizeParticipantByMatNrs());
    assertEquals(1, AssociationsManager.getCourseSure(otherCourseId).sizeParticipantByMatNrs());

    getClients(0).activate();
    assertTrue(AssociationsManager.deleteStudent(annaId));

    getBackend().activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertTrue(course().containsKeyParticipantByMatNr(1002));
    assertFalse(AssociationsManager.getCourseSure(otherCourseId).containsKeyParticipantByMatNr(1001));

    getClients(1).activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertEquals(1, course().sizeParticipantByMatNrs());
    assertEquals(0, AssociationsManager.getCourseSure(otherCourseId).sizeParticipantByMatNrs());
    assertFalse(AssociationsManager.isLoadedStudent(annaId));
  }

  /** removing a value from one map keeps it in the others */
  @Test
  public void testRemoveKeepsTheValueInOtherMaps() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));
    long otherCourseId = AssociationsManager.courseBuilder().title("Compilers")
        .participantByMatNrPut(AssociationsManager.getStudentSure(annaId)).build().get().getGemId();

    getClients(0).activate();
    course().removeParticipantByMatNr(1001);

    getBackend().activate();
    assertFalse(course().containsKeyParticipantByMatNr(1001));
    assertTrue(AssociationsManager.getCourseSure(otherCourseId).containsKeyParticipantByMatNr(1001));

    getClients(1).activate();
    assertTrue(AssociationsManager.getCourseSure(otherCourseId).containsKeyParticipantByMatNr(1001));
  }

  /** GemEntryMap: the key is the Student the Enrollment references, identified by its gemId */
  @Test
  public void testEntryMap() {
    getClients(1).activate();
    assertEquals(0, course().sizeEnrollmentByStudents());

    getClients(0).activate();
    course().putEnrollmentByStudent(AssociationsManager.getEnrollmentSure(enrollmentId));

    getBackend().activate();
    assertTrue(course().containsKeyEnrollmentByStudent(AssociationsManager.getStudentSure(annaId)));

    getClients(1).activate();
    Student client1Anna = AssociationsManager.getStudentSure(annaId);
    assertTrue(course().containsKeyEnrollmentByStudent(client1Anna));
    assertEquals(enrollmentId, course().getEnrollmentByStudent(client1Anna).getGemId());
  }
}
