package domain.e2e;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import domain.test_infrastructure.AbstractDomainE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static umlp.test_rte.playwright.ComponentKind.GemText;


public class ExampleCssTest extends AbstractDomainE2ETest {

  @BeforeEach
  void init(Page page) {
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
    navAndWait(page, "/example_basic/CssStyleTestExample"); //go to page we want to test
  }

  @Disabled
  @Test
  void testGemTextComponentKindLocator(Page page) {
    ElementHandle element = locator(page, ".testText", GemText).elementHandle();
    System.out.printf("Element: %s%n", element);
  }

  @Test
  void testStyles(Page page) {
    ElementHandle element = page.locator(".testText").first().elementHandle();

    String textAlign = CssUtil.getTextAlign(page, element);
    assertEquals(textAlign, "start");

    String textColor = CssUtil.getTextColor(page, element);
    assertEquals(textColor, "rgb(0, 0, 255)");

    String fontFamily = CssUtil.getFontFamily(page, element);
    assertEquals(fontFamily, "\"Lucida Console\", Courier, monospace");

    String fontSize = CssUtil.getFontSize(page, element);
    assertEquals(fontSize, "16px");

    String fontStyle = CssUtil.getFontStyle(page, element);
    assertEquals(fontStyle, "oblique 12deg");

    String fontWeight = CssUtil.getFontWeight(page, element);
    assertEquals(fontWeight, "200");

    // Inherited from the theme's base typography (body { line-height: 1.6 } in
    // theme/base/_base.scss), resolved against the 1rem font size above: 1.6 * 16px.
    String lineHeight = CssUtil.getLineHeight(page, element);
    assertEquals(lineHeight, "25.6px");

    String background = CssUtil.getBackground(page, element);
    assertTrue(background.contains("rgba(0, 0, 0, 0)"));

    String borderColor = CssUtil.getBorderColor(page, element);
    assertEquals(borderColor, "rgb(0, 0, 255)");

    String borderRadius = CssUtil.getBorderRadius(page, element);
    assertEquals(borderRadius, "0px");

    String boxShadow = CssUtil.getBoxShadow(page, element);
    assertEquals(boxShadow, "none");

    String dimensions = CssUtil.getDimensions(page, element);
    assertTrue(dimensions.contains("width=200px"));
    assertTrue(dimensions.contains("height=48px"));

    String display = CssUtil.getDisplay(page, element);
    assertEquals(display, "flex");

    String margin = CssUtil.getMargin(page, element);
    assertTrue(margin.contains("top=0px"));
    assertTrue(margin.contains("right=0px"));
    assertTrue(margin.contains("bottom=0px"));
    assertTrue(margin.contains("left=0px"));

    String opacity = CssUtil.getOpacity(page, element);
    assertEquals(opacity, "1");

    String padding = CssUtil.getPadding(page, element);
    assertTrue(padding.contains("top=0px"));
    assertTrue(padding.contains("right=0px"));
    assertTrue(padding.contains("bottom=0px"));
    assertTrue(padding.contains("left=0px"));

    String position = CssUtil.getPosition(page, element);
    assertEquals(position, "static");

    String zIndex = CssUtil.getZIndex(page, element);
    assertEquals(zIndex, "auto");
  }
}
