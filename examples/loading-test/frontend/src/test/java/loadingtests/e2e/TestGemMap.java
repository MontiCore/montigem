package loadingtests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import loadingtests.Course;
import loadingtests.Enrollment;
import loadingtests.LoadingTestsManager;
import loadingtests.Student;
import loadingtests.Tutor;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

/**
 * Qualified associations in the browser: the page test_gemmap/TestGemMap runs the scenarios
 * against the TypeScript RTE GemMaps, the test checks the backend after synchronization and/or what the
 * page read in the browser
 */
public class TestGemMap extends AbstractLoadingTestsE2ETest {

  Course course;
  Student anna;
  Student ben;
  Tutor tutor;
  Enrollment enrollment;

  @BeforeEach
  void init(Page page) {
    this.course = LoadingTestsManager.courseBuilder().title("Software Engineering").ects(6).build().get();
    this.anna = LoadingTestsManager.studentBuilder().name("Anna").age(21).matNr(1001).build().get();
    this.ben = LoadingTestsManager.studentBuilder().name("Ben").age(22).matNr(1002).build().get();
    this.tutor = LoadingTestsManager.tutorBuilder().staffId("T1").weeklyHours(10).build().get();
    this.enrollment = LoadingTestsManager.enrollmentBuilder().student(anna).grade(2).build().get();

    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }

  void open(Page page, Student student) {
    navAndWait(page, "/test_gemmap/TestGemMap/" + course.getGemId() + "/" + student.getGemId()
        + "/" + tutor.getGemId() + "/" + enrollment.getGemId());
    waitRdy(page);
  }

  void click(Page page, String label) {
    page.locator(GemButton.selector() + ":text-is(\"" + label + "\")").click();
    waitRdy(page);
  }

  Locator result(Page page) {
    return page.locator(GemText.selector()).first();
  }

  /**
   * clicks the button until the result contains the text: a command pushed by the backend reaches the browser
   * asynchronously, so a single click right after the change may still read the old state
   */
  void clickUntil(Page page, String label, String text) {
    waitUntil(() -> {
      click(page, label);
      return result(page).textContent().contains(text);
    }, "\"" + label + "\" never showed " + text);
    assertThat(result(page)).hasText(text);
  }

  @Test
  public void testPutByOwner(Page page) {
    open(page, anna);
    click(page, "Put By Owner");
    waitUntil(() -> course.containsKeyParticipantByMatNr(1001), "backend: course.containsKeyParticipantByMatNr(1001)");
    assertEquals(anna.getGemId(), course.getParticipantByMatNr(1001).getGemId());
  }

  @Test
  public void testPutByMap(Page page) {
    open(page, anna);
    click(page, "Put By Map");
    waitUntil(() -> course.containsKeyParticipantByMatNr(1001), "backend: course.containsKeyParticipantByMatNr(1001)");
    assertEquals(anna.getGemId(), course.getParticipantByMatNr(1001).getGemId());
  }

  @Test
  public void testRemoveByOwner(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By Owner");
    waitUntil(() -> !course.containsKeyParticipantByMatNr(1001), "backend: !course.containsKeyParticipantByMatNr(1001)");
  }

  @Test
  public void testRemoveByMap(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By Map");
    waitUntil(() -> !course.containsKeyParticipantByMatNr(1001), "backend: !course.containsKeyParticipantByMatNr(1001)");
  }

  @Test
  public void testRemoveByKeySet(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By KeySet");
    waitUntil(() -> !course.containsKeyParticipantByMatNr(1001), "backend: !course.containsKeyParticipantByMatNr(1001)");
  }

  /** the browser loads the map from the backend with value ids only and keys them on first access */
  @Test
  public void testRead(Page page) {
    course.putParticipantByMatNr(anna);
    course.putParticipantByMatNr(ben);
    open(page, anna);
    click(page, "Read");
    assertThat(result(page)).hasText("size=2;1001=Anna;1002=Ben;contains1001=true;contains9999=false");
  }

  /** changing the key attribute in the browser rekeys the map there and, after synchronization, in the backend */
  @Test
  public void testRekey(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Rekey");
    assertThat(result(page)).hasText("before=true;old=false;new=true");
    waitUntil(() -> !course.containsKeyParticipantByMatNr(1001), "backend: !course.containsKeyParticipantByMatNr(1001)");
    waitUntil(() -> course.containsKeyParticipantByMatNr(2001), "backend: course.containsKeyParticipantByMatNr(2001)");
  }

  /** the key attribute staffId is declared in Staff, the superclass of the value Tutor */
  @Test
  public void testKeyOfSuperclass(Page page) {
    open(page, anna);
    click(page, "Put Tutor");
    assertThat(result(page)).hasText("tutor=true");
    waitUntil(() -> course.containsKeyTutorByStaffId("T1"), "backend: course.containsKeyTutorByStaffId(\"T1\")");

    click(page, "Rekey Tutor");
    assertThat(result(page)).hasText("before=true;old=false;new=true");
    waitUntil(() -> !course.containsKeyTutorByStaffId("T1"), "backend: !course.containsKeyTutorByStaffId(\"T1\")");
    waitUntil(() -> course.containsKeyTutorByStaffId("T1-new"), "backend: course.containsKeyTutorByStaffId(\"T1-new\")");
  }

  /** GemEntryMap: the key is the Student the Enrollment references */
  @Test
  public void testEntryMap(Page page) {
    open(page, anna);
    click(page, "Put Enrollment");
    assertThat(result(page)).hasText("enrollment=true;grade=2");
    waitUntil(() -> course.containsKeyEnrollmentByStudent(anna), "backend: course.containsKeyEnrollmentByStudent(anna)");
    assertEquals(enrollment.getGemId(), course.getEnrollmentByStudent(anna).getGemId());
  }

  /** a builder in the browser keeps the values until it is built */
  @Test
  public void testBuilder(Page page) {
    open(page, anna);
    click(page, "Build Course");
    assertThat(result(page)).hasText("built=true");
    waitUntil(() -> LoadingTestsManager.getCourseList().stream()
        .anyMatch(c -> c.getTitle().equals("Built In Browser")), "the built course in the backend");
    Course built = LoadingTestsManager.getCourseList().stream()
        .filter(c -> c.getTitle().equals("Built In Browser")).findFirst().get();
    waitUntil(() -> built.containsKeyParticipantByMatNr(1001), "1001 in the built course in the backend");
    assertEquals(anna.getGemId(), built.getParticipantByMatNr(1001).getGemId());
  }

  /** a put in the backend reaches the GemMap in the browser */
  @Test
  public void testIncomingSync(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Read");
    assertThat(result(page)).hasText("size=1;1001=Anna;contains1001=true;contains9999=false");

    course.putParticipantByMatNr(ben);
    clickUntil(page, "Read", "size=2;1001=Anna;1002=Ben;contains1001=true;contains9999=false");
  }

  @Test
  public void testClear(Page page) {
    course.putParticipantByMatNr(anna);
    course.putParticipantByMatNr(ben);
    open(page, anna);
    click(page, "Clear");
    waitUntil(() -> course.sizeParticipantByMatNrs() == 0, "backend: course.sizeParticipantByMatNrs() == 0");
  }

  /** type qualified association: the key chosen in the browser reaches the backend */
  @Test
  public void testTypeQualifiedPut(Page page) {
    open(page, anna);
    click(page, "Put Seat");
    assertThat(result(page)).hasText("seat=true");
    waitUntil(() -> course.containsKeyStudentBySeat("A1"), "backend: course.containsKeyStudentBySeat(\"A1\")");
    assertEquals(anna.getGemId(), course.getStudentBySeat("A1").getGemId());
  }

  /** a value put under another key moves, in the browser and, after synchronization, in the backend */
  @Test
  public void testTypeQualifiedMove(Page page) {
    course.putStudentBySeat("A1", anna);
    open(page, anna);
    click(page, "Move Seat");
    assertThat(result(page)).hasText("A1=false;B2=true;size=1");
    waitUntil(() -> !course.containsKeyStudentBySeat("A1"), "backend: !course.containsKeyStudentBySeat(\"A1\")");
    waitUntil(() -> course.containsKeyStudentBySeat("B2"), "backend: course.containsKeyStudentBySeat(\"B2\")");
    assertEquals(anna.getGemId(), course.getStudentBySeat("B2").getGemId());
    waitUntil(() -> course.sizeStudentBySeats() == 1, "backend: course.sizeStudentBySeats() == 1");
  }

  /** the browser loads the map from the backend with the keys and the value ids */
  @Test
  public void testTypeQualifiedRead(Page page) {
    course.putStudentBySeat("A1", anna);
    course.putStudentBySeat("B2", ben);
    open(page, anna);
    click(page, "Read Seats");
    assertThat(result(page)).hasText("size=2;A1=Anna;B2=Ben");
  }

  /** a remove by the owner in the browser reaches the backend */
  @Test
  public void testTypeQualifiedRemove(Page page) {
    course.putStudentBySeat("A1", anna);
    open(page, anna);
    click(page, "Remove Seat");
    waitUntil(() -> !course.containsKeyStudentBySeat("A1"), "backend: !course.containsKeyStudentBySeat(\"A1\")");
  }

  /** a remove through the map in the browser reaches the backend */
  @Test
  public void testTypeQualifiedRemoveByMap(Page page) {
    course.putStudentBySeat("A1", anna);
    open(page, anna);
    click(page, "Remove Seat By Map");
    waitUntil(() -> !course.containsKeyStudentBySeat("A1"), "backend: !course.containsKeyStudentBySeat(\"A1\")");
  }

  /** a put in the backend reaches the type qualified map in the browser */
  @Test
  public void testTypeQualifiedIncomingSync(Page page) {
    open(page, anna);
    click(page, "Read Seats");
    assertThat(result(page)).hasText("size=0;A1=-;B2=-");

    course.putStudentBySeat("A1", anna);
    clickUntil(page, "Read Seats", "size=1;A1=Anna;B2=-");
  }

  /** deleting a key object in the backend removes its entry from the map in the browser */
  @Test
  public void testTypeQualifiedKeyObjectDeleted(Page page) {
    Tutor mentor = LoadingTestsManager.tutorBuilder().staffId("T2").weeklyHours(5).build().get();
    course.putMenteeByTutor(mentor, anna);
    open(page, anna);
    click(page, "Read Mentees");
    assertThat(result(page)).hasText("mentees=1");

    assertTrue(LoadingTestsManager.deleteTutor(mentor.getGemId()));
    clickUntil(page, "Read Mentees", "mentees=0");
    waitUntil(() -> course.sizeMenteeByTutors() == 0, "backend: course.sizeMenteeByTutors() == 0");
  }

  /** the largest long key the browser represents exactly arrives in the backend unchanged */
  @Test
  public void testLongKeyAtTheLimit(Page page) {
    open(page, anna);
    click(page, "Put Max Phone");
    assertThat(result(page)).hasText("max=true");
    waitUntil(() -> course.containsKeyStudentByPhone(9007199254740991L), "backend: course.containsKeyStudentByPhone(9007199254740991L)");
    assertEquals(anna.getGemId(), course.getStudentByPhone(9007199254740991L).getGemId());
  }

  /** a long key beyond the limit is refused in the browser, nothing reaches the backend */
  @Test
  public void testUnsafeLongKeyIsRefused(Page page) {
    open(page, anna);
    click(page, "Put Unsafe Phone");
    assertThat(result(page)).hasText("unsafe=false;size=0");
    waitUntil(() -> course.sizeStudentByPhones() == 0, "backend: course.sizeStudentByPhones() == 0");
  }

  /** type qualified GemEntryMap: the key is the Tutor chosen in the browser */
  @Test
  public void testTypeQualifiedEntryMap(Page page) {
    open(page, anna);
    click(page, "Put Mentee");
    assertThat(result(page)).hasText("mentee=Anna");
    waitUntil(() -> course.containsKeyMenteeByTutor(tutor), "backend: course.containsKeyMenteeByTutor(tutor)");
    assertEquals(anna.getGemId(), course.getMenteeByTutor(tutor).getGemId());
  }
}
