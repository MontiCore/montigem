package exectests;

import org.junit.jupiter.api.Test;
import resources.exectests.AnonymousInnerClassRuntimeCheck;
import resources.exectests.ArrayStreamAdapterRuntimeCheck;
import resources.exectests.CollectionsSingletonRuntimeCheck;
import resources.exectests.DateArithmeticAsyncArgumentRuntimeCheck;
import resources.exectests.FutureAdapterRuntimeCheck;
import resources.exectests.LocalDateParseTimePartRuntimeCheck;
import resources.exectests.LocalDateRuntimeCheck;
import resources.exectests.LocalDateTimeZoneRuntimeCheck;
import resources.exectests.NestedFlatMapElement;
import resources.exectests.NestedFlatMapElementSource;
import resources.exectests.NestedFlatMapRuntimeCheck;
import resources.exectests.OptionalAdapterRuntimeCheck;
import resources.exectests.StringSplitRuntimeCheck;
import resources.exectests.UtilArraysRuntimeCheck;
import util.ExecTestRunner;
import util.RealTscCheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutionTests {

  @Test
  public void testAnonymousInnerClasses()  {
    ExecTestRunner.runExecutionTest(AnonymousInnerClassRuntimeCheck.class);
  }

  @Test
  public void testArrayStream() {
    ExecTestRunner.runExecutionTest(ArrayStreamAdapterRuntimeCheck.class);
  }

  @Test
  public void testOptional() {
    ExecTestRunner.runExecutionTest(OptionalAdapterRuntimeCheck.class);
  }

  @Test
  public void testCollection() {
    ExecTestRunner.runExecutionTest(CollectionsSingletonRuntimeCheck.class);
  }

  @Test
  public void testFuture() {
    ExecTestRunner.runExecutionTest(FutureAdapterRuntimeCheck.class);
  }

  @Test
  public void testUtilArrays() {
    ExecTestRunner.runExecutionTest(UtilArraysRuntimeCheck.class);
  }

  @Test
  public void testStringSplit() {
    ExecTestRunner.runExecutionTest(StringSplitRuntimeCheck.class);
  }

  @Test
  public void testLocalDate() {
    ExecTestRunner.runExecutionTestInTimeZones(LocalDateRuntimeCheck.class,
        WireFormatRoundTripTest.TIME_ZONES);
  }

  @Test
  public void testDateArithmeticWithAsyncArgument() {
    ExecTestRunner.runExecutionTestInTimeZones(DateArithmeticAsyncArgumentRuntimeCheck.class,
        WireFormatRoundTripTest.TIME_ZONES);
  }

  @Test
  public void testLocalDateTimeZones() {
    ExecTestRunner.runExecutionTestInTimeZones(LocalDateTimeZoneRuntimeCheck.class,
        WireFormatRoundTripTest.TIME_ZONES);
  }

  /**
   * LocalDate.parse deviates from java.time on purpose (it ignores a time part), so the expected
   * values are given here instead of being computed in Java.
   */
  @Test
  public void testLocalDateParseIgnoresTimePart() throws IOException {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("parseIgnoresTimePart", "\"2024-05-07 2024-05-07 2026-10-25 2026-09-06 true\"");
    expected.put("parseRejectsInvalidTimePart",
        "\"rejected rejected rejected rejected rejected\"");
    Map<String, RealTscCheck.Result> results = RealTscCheck.compileAndRunInTimeZones(
        LocalDateParseTimePartRuntimeCheck.class,
        RealTscCheck.multiMethodRunner(LocalDateParseTimePartRuntimeCheck.class.getSimpleName(),
            new ArrayList<>(expected.keySet())),
        WireFormatRoundTripTest.TIME_ZONES);
    for (Map.Entry<String, RealTscCheck.Result> entry : results.entrySet()) {
      assertEquals(0, entry.getValue().exitCode(),
          "TZ=" + entry.getKey() + ": node execution failed:\n" + entry.getValue().output());
      assertEquals(expected, entry.getValue().resultValues(), "TZ=" + entry.getKey());
    }
  }

  @Test
  public void testNestedFlatMap() {
    ExecTestRunner.runExecutionTest(NestedFlatMapRuntimeCheck.class,
        NestedFlatMapElement.class, NestedFlatMapElementSource.class);
  }
}
