/* (c) https://github.com/MontiCore/monticore */
package dashboardmodule.commands;

import de.se_rwth.commons.logging.LogStub;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class FilterableTableInterpreterTest {

  static final String DATA = """
      List<String> names = ["Alpha", "Beta", "Gamma"];
      List<Double> sizes = [3.0, 12.0, 7.5];
      List<Boolean> remote = [true, false, true];
      List<GemFilterableTableColumn> columns = [
        GemFilterableTableColumnBuilder().header("Team").texts(names).build().get(),
        GemFilterableTableColumnBuilder().header("Size").numbers(sizes).build().get(),
        GemFilterableTableColumnBuilder().header("Remote").booleans(remote).build().get()
      ];
      return GemFilterableTableDataBuilder().columns(columns).build().get();
      """;

  @BeforeEach
  public void setUp() {
    LogStub.init();
  }

  @Test
  public void testWithoutScripts() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA, Map.of(), Map.of());

    Assertions.assertEquals(3, table.sizeColumns());
    Assertions.assertEquals(List.of(3.0, 12.0, 7.5), table.getColumns(1).getNumbersList());
    Assertions.assertEquals(List.of("3", "12", "7.5"), table.getColumns(1).getRenderedList());
    Assertions.assertEquals(List.of("true", "false", "true"), table.getColumns(2).getRenderedList());
    Assertions.assertTrue(table.getColumns(1).getGemFilterableTableFilter().isEmpty());
  }

  @Test
  public void testFiltersAndRenders() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
        Map.of("Size", "return (Double v) -> v > 5.0;", "Remote", "return (Boolean v) -> v;", "Team", " "),
        Map.of("Size", "return (Double v) -> Math.round(v * 2.0) + \" halves\";", "Team", "return (String v) -> v.toUpperCase();"));

    Assertions.assertEquals(List.of("Gamma"), table.getColumns(0).getTextsList());
    Assertions.assertEquals(List.of("GAMMA"), table.getColumns(0).getRenderedList());
    Assertions.assertEquals(List.of(7.5), table.getColumns(1).getNumbersList());
    Assertions.assertEquals(List.of("15 halves"), table.getColumns(1).getRenderedList());
    Assertions.assertEquals(List.of(true), table.getColumns(2).getBooleansList());
    Assertions.assertTrue(table.getColumns(0).getGemFilterableTableFilter().isEmpty());
    Assertions.assertEquals("return (Double v) -> v > 5.0;", table.getColumns(1).getGemFilterableTableFilter().get().getFilter());
    Assertions.assertSame(table.getColumns(1), table.getColumns(1).getGemFilterableTableFilter().get().getColumn());
  }

  @Test
  public void testExpressions() {
    Assertions.assertEquals(List.of(12.0, 7.5), filter("Size", ">= 7").getColumns(1).getNumbersList());
    Assertions.assertEquals(List.of(12.0, 7.5), filter("Size", "v > 5 && v < 10 || v == 12;").getColumns(1).getNumbersList());
    Assertions.assertEquals(List.of("Beta"), filter("Team", "== \"Beta\"").getColumns(0).getTextsList());
    Assertions.assertEquals(List.of("Alpha", "Gamma"), filter("Team", "!= \"Beta\"").getColumns(0).getTextsList());
    Assertions.assertEquals(List.of("Gamma"), filter("Team", ".startsWith(\"G\")").getColumns(0).getTextsList());
    Assertions.assertEquals(List.of("Beta"), filter("Remote", "!v").getColumns(0).getTextsList());
    Assertions.assertEquals(List.of("Alpha", "Gamma"), filter("Remote", "== true").getColumns(0).getTextsList());
    Assertions.assertEquals(">= 7", filter("Size", ">= 7").getColumns(1).getGemFilterableTableFilter().get().getFilter());

    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA, Map.of(),
        Map.of("Size", "Math.round(v) + \" people\"", "Remote", "v ? \"yes\" : \"no\""));
    Assertions.assertEquals(List.of("3 people", "12 people", "8 people"), table.getColumns(1).getRenderedList());
    Assertions.assertEquals(List.of("yes", "no", "yes"), table.getColumns(2).getRenderedList());
  }

  GemFilterableTableData filter(String column, String filter) {
    return FilterableTableInterpreter.interpret(DATA, Map.of(column, filter), Map.of());
  }

  @Test
  public void testFilterUsingDataOfTheScript() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
        Map.of("Team", "List<String> chosen = [\"Alpha\", \"Gamma\"]; return (String v) -> chosen.contains(v);"), Map.of());

    Assertions.assertEquals(List.of("Alpha", "Gamma"), table.getColumns(0).getTextsList());
  }

  @Test
  public void testScriptsOfJson() {
    String json = "[{\"column\":\"Size\",\"filter\":\"return (Double v) -> v > 5.0;\",\"render\":\"return (Double v) -> \\\" \\\" + v;\"},"
        + "{\"column\":\"Team\",\"render\":\"\"}]";

    Assertions.assertEquals(Map.of("Size", "return (Double v) -> v > 5.0;", "Team", ""),
        FilterableTableInterpreter.scriptsOf(json, "filter"));
    Assertions.assertEquals("return (Double v) -> \" \" + v;", FilterableTableInterpreter.scriptsOf(json, "render").get("Size"));
    Assertions.assertEquals(Map.of(), FilterableTableInterpreter.scriptsOf("[]", "filter"));
  }

  @Test
  public void testInvalidScripts() {
    assertFails("has to return a GemFilterableTableData", "return 1;", Map.of(), Map.of());
    assertFails("has to return a lambda", DATA, Map.of("Size", "return true;"), Map.of());
    assertFails("has to return a boolean", DATA, Map.of("Size", "return (Double v) -> v + 1.0;"), Map.of());
    assertFails("has to return a boolean", DATA, Map.of("Size", "+ 1.0"), Map.of());
    assertFails("The filter script of column \"Size\" failed", DATA, Map.of("Size", ">= \"a\""), Map.of());
    assertFails("There is no column \"Age\"", DATA, Map.of(), Map.of("Age", "return (Double v) -> \"\" + v;"));
    assertFails("The render script of column \"Team\" failed", DATA, Map.of(), Map.of("Team", "return UnknownType();"));
    assertFails("has 1 values, but column \"A\" has 2", """
        List<GemFilterableTableColumn> columns = [
          GemFilterableTableColumnBuilder().header("A").numbers([1.0, 2.0]).build().get(),
          GemFilterableTableColumnBuilder().header("B").texts(["x"]).build().get()
        ];
        return GemFilterableTableDataBuilder().columns(columns).build().get();
        """, Map.of(), Map.of());
    assertFails("only one of numbers, texts, booleans, dates, or dateTimes", """
        List<GemFilterableTableColumn> columns = [
          GemFilterableTableColumnBuilder().header("A").numbers([1.0]).texts(["x"]).build().get()
        ];
        return GemFilterableTableDataBuilder().columns(columns).build().get();
        """, Map.of(), Map.of());
  }

  void assertFails(String expected, String data, Map<String, String> filters, Map<String, String> renders) {
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> FilterableTableInterpreter.interpret(data, filters, renders));
    Assertions.assertTrue(e.getMessage().contains(expected), e.getMessage());
  }
}
