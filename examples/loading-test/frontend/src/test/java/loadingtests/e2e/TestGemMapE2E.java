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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

/**
 * Attribute qualified associations in the browser: the page test_gemmap/TestGemMap runs the scenarios
 * against the TypeScript RTE GemMaps, the test checks the backend after synchronization and/or what the
 * page read in the browser
 */
public class TestGemMapE2E extends AbstractLoadingTestsE2ETest {

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

  @Test
  public void testPutByOwner(Page page) {
    open(page, anna);
    click(page, "Put By Owner");
    assertTrue(course.containsKeyParticipantByMatNr(1001));
    assertEquals(anna.getGemId(), course.getParticipantByMatNr(1001).get().getGemId());
  }

  @Test
  public void testPutByMap(Page page) {
    open(page, anna);
    click(page, "Put By Map");
    assertTrue(course.containsKeyParticipantByMatNr(1001));
    assertEquals(anna.getGemId(), course.getParticipantByMatNr(1001).get().getGemId());
  }

  @Test
  public void testRemoveByOwner(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By Owner");
    assertFalse(course.containsKeyParticipantByMatNr(1001));
  }

  @Test
  public void testRemoveByMap(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By Map");
    assertFalse(course.containsKeyParticipantByMatNr(1001));
  }

  @Test
  public void testRemoveByKeySet(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Remove By KeySet");
    assertFalse(course.containsKeyParticipantByMatNr(1001));
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
    assertFalse(course.containsKeyParticipantByMatNr(1001));
    assertTrue(course.containsKeyParticipantByMatNr(2001));
  }

  /** the key attribute staffId is declared in Staff, the superclass of the value Tutor */
  @Test
  public void testKeyOfSuperclass(Page page) {
    open(page, anna);
    click(page, "Put Tutor");
    assertThat(result(page)).hasText("tutor=true");
    assertTrue(course.containsKeyTutorByStaffId("T1"));

    click(page, "Rekey Tutor");
    assertThat(result(page)).hasText("before=true;old=false;new=true");
    assertFalse(course.containsKeyTutorByStaffId("T1"));
    assertTrue(course.containsKeyTutorByStaffId("T1-new"));
  }

  /** GemEntryMap: the key is the Student the Enrollment references */
  @Test
  public void testEntryMap(Page page) {
    open(page, anna);
    click(page, "Put Enrollment");
    assertThat(result(page)).hasText("enrollment=true;grade=2");
    assertTrue(course.containsKeyEnrollmentByStudent(anna));
    assertEquals(enrollment.getGemId(), course.getEnrollmentByStudent(anna).get().getGemId());
  }

  /** a builder in the browser keeps the values until it is built */
  @Test
  public void testBuilder(Page page) {
    open(page, anna);
    click(page, "Build Course");
    assertThat(result(page)).hasText("built=true");
    Course built = LoadingTestsManager.getCourseList().stream()
        .filter(c -> c.getTitle().equals("Built In Browser")).findFirst().get();
    assertTrue(built.containsKeyParticipantByMatNr(1001));
    assertEquals(anna.getGemId(), built.getParticipantByMatNr(1001).get().getGemId());
  }

  /** a put in the backend reaches the GemMap in the browser */
  @Test
  public void testIncomingSync(Page page) {
    course.putParticipantByMatNr(anna);
    open(page, anna);
    click(page, "Read");
    assertThat(result(page)).hasText("size=1;1001=Anna;contains1001=true;contains9999=false");

    course.putParticipantByMatNr(ben);
    waitRdy(page);
    click(page, "Read");
    assertThat(result(page)).hasText("size=2;1001=Anna;1002=Ben;contains1001=true;contains9999=false");
  }

  @Test
  public void testClear(Page page) {
    course.putParticipantByMatNr(anna);
    course.putParticipantByMatNr(ben);
    open(page, anna);
    click(page, "Clear");
    assertEquals(0, course.sizeParticipantByMatNrs());
  }
}
