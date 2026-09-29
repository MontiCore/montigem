package exectests;

import org.junit.jupiter.api.Test;
import resources.exectests.AnonymousInnerClassRuntimeCheck;
import resources.exectests.ArrayStreamAdapterRuntimeCheck;
import resources.exectests.CollectionsSingletonRuntimeCheck;
import resources.exectests.DateArithmeticAsyncArgumentRuntimeCheck;
import resources.exectests.FutureAdapterRuntimeCheck;
import resources.exectests.LocalDateRuntimeCheck;
import resources.exectests.NestedFlatMapElement;
import resources.exectests.NestedFlatMapElementSource;
import resources.exectests.NestedFlatMapRuntimeCheck;
import resources.exectests.OptionalAdapterRuntimeCheck;
import resources.exectests.StringSplitRuntimeCheck;
import resources.exectests.UtilArraysRuntimeCheck;
import util.ExecTestRunner;

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
    ExecTestRunner.runExecutionTest(LocalDateRuntimeCheck.class);
  }

  @Test
  public void testDateArithmeticWithAsyncArgument() {
    ExecTestRunner.runExecutionTest(DateArithmeticAsyncArgumentRuntimeCheck.class);
  }

  @Test
  public void testNestedFlatMap() {
    ExecTestRunner.runExecutionTest(NestedFlatMapRuntimeCheck.class,
        NestedFlatMapElement.class, NestedFlatMapElementSource.class);
  }
}
