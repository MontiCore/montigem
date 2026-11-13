/* (c) https://github.com/MontiCore/monticore */
package domain.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import domain.test_infrastructure.AbstractDomainE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that width and height of a GemCard reach the visible card
 * frame and its body, also through the collapse wrapper, and that a collapsed card
 * shrinks to its header.
 */
public class GemCardE2ETest extends AbstractDomainE2ETest {

  /** Padding of the gem-card host element (2px on each side). */
  private static final double HOST_PADDING = 4;

  /** Tolerance for sub-pixel rounding and the 1px border of the frame. */
  private static final double DELTA = 2.5;

  /** Duration of the collapse animation (0.35s) plus a margin. */
  private static final int COLLAPSE_ANIMATION_MS = 700;

  @BeforeEach
  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/manual/GemCardTest");
  }

  @Test
  void testFixedHeightIsApplied(Page page) {
    // height = "250px": before the fix the frame was only as high as its content
    assertTrue(frame(page, ".fixedCard").height >= 250 - HOST_PADDING - DELTA,
        "frame height was " + frame(page, ".fixedCard").height);
    assertFrameFillsHost(page, ".fixedCard");
  }

  @Test
  void testPercentHeightIsApplied(Page page) {
    // width = "100%", height = "100%" inside a column of 300px height
    BoundingBox container = page.locator(".percentCard").first().locator("xpath=..").boundingBox();
    assertEquals(300, container.height, DELTA);
    assertTrue(frame(page, ".percentCard").height >= 300 - HOST_PADDING - DELTA,
        "frame height was " + frame(page, ".percentCard").height);
    assertTrue(frame(page, ".percentCard").width >= container.width - HOST_PADDING - DELTA,
        "frame width was " + frame(page, ".percentCard").width);
    assertFrameFillsHost(page, ".percentCard");
  }

  @Test
  void testBodyFillsCard(Page page) {
    // compare with the gem-card host: it always gets the 250px, also without the fix,
    // while frame and body shrank to their content
    BoundingBox host = box(page, ".fixedCard");
    BoundingBox header = page.locator(".fixedCard .gem-card-header").first().boundingBox();
    BoundingBox body = page.locator(".fixedCard .gem-card-body").first().boundingBox();
    // the body reaches the bottom of the card (apart from host padding and frame border)
    assertEquals(host.y + host.height - HOST_PADDING / 2, body.y + body.height, DELTA);
    // and takes the whole height below the header
    assertTrue(body.height >= host.height - HOST_PADDING - header.height - 2 * DELTA,
        "body height was " + body.height + ", header height " + header.height);
  }

  @Test
  void testChartFillsCard(Page page) {
    Locator chart = page.locator(".chartCard gem-pie-chart").first();
    chart.scrollIntoViewIfNeeded();
    BoundingBox chartBox = chart.boundingBox();
    BoundingBox body = page.locator(".chartCard .gem-card-body").first().boundingBox();
    // height = "400px": before the fix the chart was squeezed to a height of ~0px
    assertTrue(chartBox.height > 200, "chart height was " + chartBox.height);
    assertTrue(chartBox.y + chartBox.height <= body.y + body.height + DELTA,
        "chart overflows the card body");
  }

  @Test
  void testCollapseShowsOnlyHeaderAndMovesFollowingCardsUp(Page page) {
    Locator header = page.locator(".collapsibleCard .gem-card-header").first();
    header.scrollIntoViewIfNeeded();
    double expandedFrameHeight = frame(page, ".collapsibleCard").height;
    double expandedOffset = offsetOfCardBelow(page);
    assertTrue(expandedFrameHeight >= 250 - HOST_PADDING - DELTA,
        "frame height was " + expandedFrameHeight);

    header.click();
    page.waitForTimeout(COLLAPSE_ANIMATION_MS);

    // collapsed: only the header (plus the border of the frame) is left
    assertEquals(header.boundingBox().height, frame(page, ".collapsibleCard").height, DELTA);
    double collapsedOffset = offsetOfCardBelow(page);
    assertTrue(expandedOffset - collapsedOffset > 150,
        "card below did not move up: offset " + expandedOffset + " -> " + collapsedOffset);

    header.click();
    page.waitForTimeout(COLLAPSE_ANIMATION_MS);

    // expanded again: the height set on the card applies again
    assertEquals(expandedFrameHeight, frame(page, ".collapsibleCard").height, DELTA);
    assertEquals(expandedOffset, offsetOfCardBelow(page), DELTA);
  }

  @Test
  void testDropdownOptionsAreNotCutOffByCard(Page page) {
    Locator dropdown = page.locator(".dropdownCard gem-dropdown-input").first();
    dropdown.scrollIntoViewIfNeeded();
    dropdown.locator(".gem-dropdown-input").click();

    Locator lastOption = page.locator(".dropdownCard .gem-dropdown-option").last();
    lastOption.scrollIntoViewIfNeeded();
    BoundingBox option = lastOption.boundingBox();
    // the options reach beyond the bottom of the card, i.e. the test covers an overflowing overlay
    assertTrue(option.y > frame(page, ".dropdownCard").y + frame(page, ".dropdownCard").height,
        "the options do not overflow the card, the test has to be adapted");
    // the last option is neither clipped by the card nor covered by the card below
    assertTrue(isTopmostAt(page, lastOption, option), "the last option is cut off or covered");

    lastOption.click();
    assertEquals("auto", dropdown.locator(".gem-dropdown-text").innerText().trim());
  }

  @Test
  void testCollapsedCardStillHidesItsContent(Page page) {
    Locator header = page.locator(".dropdownCard .gem-card-header").first();
    header.scrollIntoViewIfNeeded();
    header.click();
    page.waitForTimeout(COLLAPSE_ANIMATION_MS);
    assertTrue(page.locator(".dropdownCard .gem-dropdown-input").first().isHidden(),
        "the content of a collapsed card is visible");
  }

  /** Whether the element is the one hit at the center of its bounding box, i.e. visible on top. */
  private boolean isTopmostAt(Page page, Locator element, BoundingBox box) {
    return (Boolean) element.evaluate(
        "(el, p) => { const hit = document.elementFromPoint(p[0], p[1]); return hit === el || el.contains(hit); }",
        java.util.List.of(box.x + box.width / 2, box.y + box.height / 2));
  }

  /** Vertical distance between the collapsible card and the card below it (independent of scrolling). */
  private double offsetOfCardBelow(Page page) {
    return box(page, ".cardBelow").y - box(page, ".collapsibleCard").y;
  }

  /** The visible frame fills the gem-card host apart from the host padding. */
  private void assertFrameFillsHost(Page page, String cardSelector) {
    assertEquals(box(page, cardSelector).height - HOST_PADDING, frame(page, cardSelector).height, DELTA);
  }

  /** Bounding box of the gem-card host element. */
  private BoundingBox box(Page page, String cardSelector) {
    return page.locator(cardSelector).first().boundingBox();
  }

  /** Bounding box of the visible frame of the card. */
  private BoundingBox frame(Page page, String cardSelector) {
    return page.locator(cardSelector + " > .gem-card").first().boundingBox();
  }
}
