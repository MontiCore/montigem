/* (c) https://github.com/MontiCore/monticore */
package dashboardmodule.commands;

import de.se_rwth.commons.logging.LogStub;
import mc.fenix.table.gemtabletypes.GemFilterableTableData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InterpreterWithErrorHandlingTest {

  @BeforeEach
  public void setUp() {
    LogStub.init();
  }

  @Test
  public void testTable() {
    InterpreterWithErrorHandling.ValueOrError result = InterpreterWithErrorHandling.interpretString("""
        List<String> teams = [];
        List<Double> members = [];
        for (int i = 1; i <= 2; i++) {
          teams.add("Team " + i);
          members.add(i * 3.0);
        }
        List<GemFilterableTableColumn> columns = [
          GemFilterableTableColumnBuilder().header("Team").texts(teams).build().get(),
          GemFilterableTableColumnBuilder().header("Members").numbers(members).build().get()
        ];
        return GemFilterableTableDataBuilder().columns(columns).build().get();
        """);

    Assertions.assertTrue(result.isValue(), result.getFindings());
    GemFilterableTableData table = (GemFilterableTableData) result.getValue().asNativeObject();
    Assertions.assertEquals(2, table.sizeColumns());
    Assertions.assertEquals("Team", table.getColumns(0).getHeader());
    Assertions.assertEquals(List.of("Team 1", "Team 2"), table.getColumns(0).getTextsList());
    Assertions.assertEquals("Members", table.getColumns(1).getHeader());
    Assertions.assertEquals(List.of(3.0, 6.0), table.getColumns(1).getNumbersList());
  }

  @Test
  public void testInvalidScript() {
    Assertions.assertTrue(InterpreterWithErrorHandling.interpretString("return UnknownType();").isError());
  }

  @Test
  public void testConcurrentInterpretations() throws Exception {
    String script = """
        List<GemFilterableTableColumn> columns = [
          GemFilterableTableColumnBuilder().header("Column").texts(["a"]).build().get()
        ];
        return GemFilterableTableDataBuilder().columns(columns).build().get();
        """;
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      List<Future<InterpreterWithErrorHandling.ValueOrError>> results = new ArrayList<>();
      for (int i = 0; i < 16; i++) {
        results.add(executor.submit(() -> InterpreterWithErrorHandling.interpretString(script)));
      }
      for (Future<InterpreterWithErrorHandling.ValueOrError> result : results) {
        InterpreterWithErrorHandling.ValueOrError value = result.get();
        Assertions.assertTrue(value.isValue(), value.getFindings());
        GemFilterableTableData table = (GemFilterableTableData) value.getValue().asNativeObject();
        Assertions.assertEquals("Column", table.getColumns(0).getHeader());
        Assertions.assertEquals(List.of("a"), table.getColumns(0).getTextsList());
      }
    } finally {
      executor.shutdownNow();
    }
  }
}
