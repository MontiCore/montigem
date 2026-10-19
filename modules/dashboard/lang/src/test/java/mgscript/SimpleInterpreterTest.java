package mgscript;

import de.monticore.values.MCValue;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mgscript.InterpretationFailedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;


public class SimpleInterpreterTest {

  @Test
  public void testInterpreterPieChart() throws InterpretationFailedException {
    LogStub.initPlusLog();
    Interpreter interpreter = new Interpreter();
    interpreter.setup();
    MCValue val = interpreter.interpret(
        """
         GemPieChartEntry entry1 = GemPieChartEntryBuilder().value(20).label("Women").build().get();
         GemPieChartEntry entry2 = GemPieChartEntryBuilder().value(45).label("Men").build().get();
         List<GemPieChartEntry> entries = ArrayList();
         entries.add(entry1);
         entries.add(entry2);
         return GemPieChartDataBuilder().entries(entries).build().get();""");

    Assertions.assertInstanceOf(GemPieChartData.class, val.asNativeObject());
    if(val.asNativeObject() instanceof GemPieChartData) {
      GemPieChartData data = (GemPieChartData) val.asNativeObject();
    } else {
      Log.warn("Something went wrong with the Interpretation.");
      Assertions.fail();
    }
  }
}
