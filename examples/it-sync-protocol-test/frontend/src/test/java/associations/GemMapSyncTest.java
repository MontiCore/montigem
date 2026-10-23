/* (c) https://github.com/MontiCore/monticore */
package associations;

import associations.test_infrastructure.AssociationsMockTestSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synchronization of attribute qualified associations (GemMaps) between the backend and two clients.
 * A change carries the value id only: every participant derives the key itself, and rekeys locally
 * when the key attribute changes.
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

  @Test
  public void testPutOnClientReachesBackendAndOtherClient() {
    // client 1 keyed its (empty) map before
    getClients(1).activate();
    assertEquals(0, course().sizeParticipantByMatNrs());

    getClients(0).activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getBackend().activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertEquals(annaId, course().getParticipantByMatNr(1001).get().getGemId());

    getClients(1).activate();
    assertTrue(course().containsKeyParticipantByMatNr(1001));
    assertEquals(annaId, course().getParticipantByMatNr(1001).get().getGemId());
  }

  /** the change carries the value id only: client 1, which never loaded the value, derives its key itself */
  @Test
  public void testRemotePutDerivesKeyOnOtherClient() {
    getClients(1).activate();
    Course client1Course = course();

    getClients(0).activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(benId));

    getClients(1).activate();
    assertTrue(client1Course.containsKeyParticipantByMatNr(1002));
    assertEquals("Ben", client1Course.getParticipantByMatNr(1002).get().getName());
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

  /** client 1 has the value ids only, so it derives the key from the changed value on first access */
  @Test
  public void testRekeyOnOtherClientWithUnkeyedMap() {
    getBackend().activate();
    course().putParticipantByMatNr(AssociationsManager.getStudentSure(annaId));

    getClients(1).activate();
    Course client1Course = course();

    getClients(0).activate();
    AssociationsManager.getStudentSure(annaId).setMatNr(2001);

    getClients(1).activate();
    assertFalse(client1Course.containsKeyParticipantByMatNr(1001));
    assertTrue(client1Course.containsKeyParticipantByMatNr(2001));
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
    assertEquals(enrollmentId, course().getEnrollmentByStudent(client1Anna).get().getGemId());
  }
}
