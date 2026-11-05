/* (c) https://github.com/MontiCore/monticore */
package dashboardmodule.commands;

import de.se_rwth.commons.logging.LogStub;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import mc.fenix.table.gemtabletypes.GemFilterableTableDataDeSer;
import mc.fenix.table.gemtabletypes.GemTableTypesDeSer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import umlp.backendrte.command.DeSer;
import umlp.backendrte.common.BaseClassObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterableTableDateTest {
  static final String DATA = """
      List<String> names = ["Late", "Early", "Middle"];
      List<LocalDate> dates = [LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)];
      List<LocalDateTime> times = [LocalDateTime.of(2026, 1, 2, 10, 30),
        LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 2, 8, 0)];
      List<GemFilterableTableColumn> columns = [
        GemFilterableTableColumnBuilder().header("Name").texts(names).build().get(),
        GemFilterableTableColumnBuilder().header("Date").dates(dates).build().get(),
        GemFilterableTableColumnBuilder().header("Time").dateTimes(times).build().get()
      ];
      return GemFilterableTableDataBuilder().columns(columns).build().get();
      """;

  @BeforeEach
  void setUp() {
    LogStub.init();
  }

  @Test
  void rendersIsoValuesByDefault() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA, Map.of(), Map.of());
    assertEquals(List.of("2026-01-03", "2026-01-01", "2026-01-02"), table.getColumns(1).getRenderedList());
    assertEquals(List.of("2026-01-02T10:30", "2026-01-01T09:00", "2026-01-02T08:00"),
        table.getColumns(2).getRenderedList());
    assertEquals(LocalDate.of(2026, 1, 3), table.getColumns(1).getDates(0));
    assertEquals(LocalDateTime.of(2026, 1, 2, 10, 30), table.getColumns(2).getDateTimes(0));
  }

  @ParameterizedTest
  @CsvSource({">=, Late Middle", ">, Late", "<=, Early Middle", "<, Early", "==, Middle", "!=, Late Early"})
  void comparesIsoDateAndDateTimeLiterals(String operator, String names) {
    for (String column : List.of("Date", "Time")) {
      String value = column.equals("Date") ? "2026-01-02" : "2026-01-02T08:00:00";
      GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
          Map.of(column, operator + " \"" + value + "\""), Map.of());
      assertEquals(List.of(names.split(" ")), table.getColumns(0).getTextsList(), column + " " + operator);
      assertEquals(table.getColumns(0).sizeTexts(), table.getColumns(1).sizeDates());
      assertEquals(table.getColumns(0).sizeTexts(), table.getColumns(2).sizeDateTimes());
    }
  }

  @Test
  void supportsTypedPredicatesAndRenderFunctions() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
        Map.of("Date", ".isAfter(LocalDate.of(2026, 1, 1))",
            "Time", "return (LocalDateTime v) -> v.isBefore(LocalDateTime.of(2026, 1, 2, 10, 0));"),
        Map.of("Date", "\"day \" + v.getDayOfMonth()", "Time", "\"hour \" + v.getHour()"));
    assertEquals(List.of("Middle"), table.getColumns(0).getTextsList());
    assertEquals(List.of("day 2"), table.getColumns(1).getRenderedList());
    assertEquals(List.of("hour 8"), table.getColumns(2).getRenderedList());
  }

  @Test
  void comparesTypedDateValues() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
        Map.of("Date", ">= LocalDate.of(2026, 1, 2)", "Time", "!= LocalDateTime.parse(\"2026-01-02T10:30\")"), Map.of());
    assertEquals(List.of("Middle"), table.getColumns(0).getTextsList());
  }

  @Test
  void roundTripsFilteredDatesAndTheirColumnLinks() {
    Map<String, DeSer> previous = new HashMap<>(BaseClassObjectMapper.getInstance().getBaseDeSers());
    try {
      BaseClassObjectMapper.addDeSers(GemTableTypesDeSer.getInstance().getDeSerMap());
      GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
          Map.of("Date", ">= \"2026-01-02\""), Map.of());
      GemFilterableTableDataDeSer deSer = new GemFilterableTableDataDeSer();
      GemFilterableTableData restored = deSer.deserialize(deSer.serialize(table)).orElseThrow();
      assertEquals(table.getColumns(1).getDatesList(), restored.getColumns(1).getDatesList());
      assertEquals(table.getColumns(2).getDateTimesList(), restored.getColumns(2).getDateTimesList());
      assertEquals(table.getColumns(2).getRenderedList(), restored.getColumns(2).getRenderedList());
      assertSame(restored.getColumns(1), restored.getColumns(1).getGemFilterableTableFilter().orElseThrow().getColumn());
    } finally {
      BaseClassObjectMapper.getInstance().getBaseDeSers().clear();
      BaseClassObjectMapper.getInstance().getBaseDeSers().putAll(previous);
    }
  }

  @Test
  void allowsEmptyFilteredResults() {
    GemFilterableTableData table = FilterableTableInterpreter.interpret(DATA,
        Map.of("Date", "> \"2030-01-01\""), Map.of());
    assertTrue(table.getColumns(1).isEmptyDates());
    assertTrue(table.getColumns(2).isEmptyDateTimes());
    assertTrue(table.getColumns(1).isEmptyRendered());
  }

  @Test
  void rejectsInvalidDatesAndMixedValueTypes() {
    assertThrows(IllegalArgumentException.class,
        () -> FilterableTableInterpreter.interpret(DATA, Map.of("Date", ">= \"2026-02-30\""), Map.of()));
    assertThrows(IllegalArgumentException.class,
        () -> FilterableTableInterpreter.interpret(DATA, Map.of("Time", ">= LocalDate.of(2026, 1, 1)"), Map.of()));
    IllegalArgumentException mixed = assertThrows(IllegalArgumentException.class,
        () -> FilterableTableInterpreter.interpret("""
            return GemFilterableTableDataBuilder().columns([
              GemFilterableTableColumnBuilder().header("Mixed")
                .dates([LocalDate.of(2026, 1, 1)]).numbers([1.0]).build().get()
            ]).build().get();
            """, Map.of(), Map.of()));
    assertTrue(mixed.getMessage().contains("only one of"), mixed.getMessage());
    IllegalArgumentException lengths = assertThrows(IllegalArgumentException.class,
        () -> FilterableTableInterpreter.interpret("""
            return GemFilterableTableDataBuilder().columns([
              GemFilterableTableColumnBuilder().header("Dates").dates([LocalDate.of(2026, 1, 1)]).build().get(),
              GemFilterableTableColumnBuilder().header("Times").dateTimes([]).build().get()
            ]).build().get();
            """, Map.of(), Map.of()));
    assertTrue(lengths.getMessage().contains("same number of values"), lengths.getMessage());
  }
}
