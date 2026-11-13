package dashboardmodule;

import de.monticore.symboltable.serialization.JsonParser;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellOptionsValidationTest {
  @BeforeEach
  void setUp() {
    LogStub.init();
  }

  @Test
  void preservesEscapedStringsUnicodeAndExplicitDefaultsInOptions() {
    DashboardModuleMock.init();
    var cell = DashboardModuleManager.cellConfigBuilder().name("Gauge").info("").viz(VizType.SPEED).data("")
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x(0.0).y(0.0).build().get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(3.0).height(3.0).build().get())
        .extra(CellOptions.build(VizType.SPEED, CellOptions.defaults(VizType.SPEED))).build().get();
    String unit = "Müller \"quoted\" \\path\n\t";
    cell.getExtra().getSpeedextra().setUnit(unit);

    var parsed = JsonParser.parseJsonObject(CellOptions.read(cell));

    assertEquals(unit, parsed.getStringMember("unit"));
    assertEquals(0, parsed.getIntegerMember("min"));
    assertEquals("", parsed.getStringMember("textValue"));
    assertTrue(parsed.getMember("max").isJsonNull());
  }

  @Test
  void rejectsMalformedAndTrailingJsonWithoutChangingOptions() {
    var original = CellOptions.defaults(VizType.PIE);
    for (String json : new String[]{"{", "{\"innerRadius\":", "{\"innerRadius\":20,}",
        "{\"innerRadius\":20} {}", "{\"innerRadius\":20} garbage", "[]", "null"}) {
      assertThrows(IllegalArgumentException.class, () -> CellOptions.patch(VizType.PIE, original, json), json);
      assertEquals(0, original.getIntegerMember("innerRadius"));
    }
  }

  @Test
  void preservesIntegerRangeAndFiniteNumberValidation() {
    var original = CellOptions.defaults(VizType.BAR);
    for (String value : new String[]{"2147483648", "-2147483649", "1.5", "1.0", "1e2", "true", "\"2\""}) {
      assertThrows(IllegalArgumentException.class,
          () -> CellOptions.patch(VizType.BAR, original, "{\"minValue\":" + value + "}"), value);
    }
    assertThrows(IllegalArgumentException.class, () -> CellOptions.patch(VizType.BOX,
        CellOptions.defaults(VizType.BOX), "{\"min\":1e999}"));
    assertTrue(CellOptions.patch(VizType.BAR, original, "{\"maxValue\":null}").getMember("maxValue").isJsonNull());
  }
}
