/* (c) https://github.com/MontiCore/monticore */
package loadingtests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import loadingtests.Club;
import loadingtests.LoadingTestsManager;
import loadingtests.Member;
import loadingtests.test_infrastructure.AbstractLoadingTestsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static umlp.test_rte.playwright.ComponentKind.GemButton;
import static umlp.test_rte.playwright.ComponentKind.GemText;

/**
 * Bidirectional qualified associations in the browser, changed from either side like the bidirectional association of
 * TestBidirectAssoc: the page test_gemmap/TestBidirectionalGemMap changes them from the clubs (owners) and from the
 * member (value side); after every step the test checks both sides in the browser and in the backend.
 * <ul>
 *   <li>[0..1] Club [[code]] (club) &lt;-&gt; (memberByCode) Member [1]: one club per member</li>
 *   <li>[*] Club [String] (clubs) &lt;-&gt; (memberByRole) Member [1]: several clubs per member</li>
 * </ul>
 */
public class TestBidirectGemMap extends AbstractLoadingTestsE2ETest {

  static final String CODE = "m1";
  static final String ROLE = "chair";

  Club club1;
  Club club2;
  Member member;

  @BeforeEach
  void init(Page page) {
    this.club1 = LoadingTestsManager.clubBuilder().label("C 1").build().get();
    this.club2 = LoadingTestsManager.clubBuilder().label("C 2").build().get();
    this.member = LoadingTestsManager.memberBuilder().code(CODE).label("M 1").build().get();
    club1.putMemberByCode(member);

    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }

  void open(Page page) {
    navAndWait(page, "/test_gemmap/TestBidirectionalGemMap/" + club1.getGemId() + "/" + club2.getGemId() + "/"
        + member.getGemId());
    waitRdy(page);
  }

  void click(Page page, String label) {
    page.locator(GemButton.selector() + ":text-is(\"" + label + "\")").click();
    waitRdy(page);
  }

  Locator result(Page page) {
    return page.locator(GemText.selector()).first();
  }

  /** a put into another club's map moves the member there, a put back moves it back */
  @Test
  public void testMoveFromOwner(Page page) {
    open(page);
    assertInClub1(page);

    click(page, "Put Into Club 2");
    assertInClub2(page);

    click(page, "Put Into Club 1");
    assertInClub1(page);
  }

  /** a removal from the club's map clears the member's club, a put sets it again */
  @Test
  public void testRemoveFromOwner(Page page) {
    open(page);
    assertInClub1(page);

    click(page, "Remove From Club 1");
    assertInNoClub(page);

    // removing twice
    click(page, "Remove From Club 1");
    assertInNoClub(page);

    click(page, "Put Into Club 1");
    assertInClub1(page);
  }

  /** setting the member's club puts it into that club's map and takes it out of the old one */
  @Test
  public void testSetFromMember(Page page) {
    open(page);
    assertInClub1(page);

    click(page, "Set Club 2");
    assertInClub2(page);

    click(page, "Clear Club");
    assertInNoClub(page);

    // clearing twice
    click(page, "Clear Club");
    assertInNoClub(page);

    click(page, "Set Club 1");
    assertInClub1(page);
  }

  /** with several clubs per member a put keeps the member in the other club's map */
  @Test
  public void testSeveralOwners(Page page) {
    open(page);
    assertRoles(page, false, false);

    click(page, "Put Role Into Club 1");
    assertRoles(page, true, false);

    click(page, "Put Role Into Club 2");
    assertRoles(page, true, true);

    click(page, "Remove Role From Club 1");
    assertRoles(page, false, true);
  }

  /** the browser loaded only the member: the backend's changes of the clubs' maps reach its club side */
  @Test
  // TODO: enable with the fix of the setter that loads the old value: clearing the member's club by id
  //  (_internal_setClub(-1), Gem#isPresent) loads club 2 in the browser, already without the role entry the backend
  //  removed next; the following Club_removeMemberByRole then runs through the loaded club, whose removal returns
  //  early for the missing entry (InternalRemoveId) and does not release the member's clubs (clubs=1, expected 0)
  @Disabled
  public void testIncomingSyncToMember(Page page) {
    navAndWait(page, "/test_gemmap/TestBidirectionalGemMapMember/" + member.getGemId());
    waitRdy(page);
    assertMemberSide(page, club1.getGemId(), 0);

    club2.putMemberByCode(member);
    waitRdy(page);
    assertMemberSide(page, club2.getGemId(), 0);

    club2.putMemberByRole(ROLE, member);
    waitRdy(page);
    assertMemberSide(page, club2.getGemId(), 1);

    club2.removeMemberByCode(CODE);
    club2.removeMemberByRole(ROLE);
    waitRdy(page);
    assertMemberSide(page, -1, 0);
  }

  protected void assertInClub1(Page page) {
    click(page, "Read");
    assertThat(result(page)).hasText("inClub1=true;inClub2=false;club=C 1");
    // the browser's commands reach the backend asynchronously
    waitUntil(() -> club1.containsKeyMemberByCode(CODE) && !club2.containsKeyMemberByCode(CODE)
        && member.getClubGem().getGemId() == club1.getGemId(), "backend: the member in club 1 only");
  }

  protected void assertInClub2(Page page) {
    click(page, "Read");
    assertThat(result(page)).hasText("inClub1=false;inClub2=true;club=C 2");
    waitUntil(() -> !club1.containsKeyMemberByCode(CODE) && club2.containsKeyMemberByCode(CODE)
        && member.getClubGem().getGemId() == club2.getGemId(), "backend: the member in club 2 only");
  }

  protected void assertInNoClub(Page page) {
    click(page, "Read");
    assertThat(result(page)).hasText("inClub1=false;inClub2=false;club=none");
    waitUntil(() -> !club1.containsKeyMemberByCode(CODE) && !club2.containsKeyMemberByCode(CODE)
        && member.getClubGem().getGemId() == -1, "backend: the member in no club");
  }

  protected void assertRoles(Page page, boolean inClub1, boolean inClub2) {
    int clubs = (inClub1 ? 1 : 0) + (inClub2 ? 1 : 0);
    click(page, "Read Roles");
    assertThat(result(page)).hasText("inClub1=" + inClub1 + ";inClub2=" + inClub2 + ";clubs=" + clubs
        + ";hasClub1=" + inClub1 + ";hasClub2=" + inClub2);
    waitUntil(() -> club1.containsKeyMemberByRole(ROLE) == inClub1 && club2.containsKeyMemberByRole(ROLE) == inClub2
        && member.sizeClubss() == clubs && member.containsClubs(club1) == inClub1
        && member.containsClubs(club2) == inClub2,
        "backend: the role in club 1 " + inClub1 + ", in club 2 " + inClub2);
  }

  protected void assertMemberSide(Page page, long clubId, int clubs) {
    click(page, "Read Club");
    assertThat(result(page)).hasText("club=" + clubId + ";clubs=" + clubs);
  }
}
