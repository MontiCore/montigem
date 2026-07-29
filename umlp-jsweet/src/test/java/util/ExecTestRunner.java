package util;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecTestRunner {

  public static void runExecutionTest(Class<?> classToBeTested) {
    Map<String, String> javaResults = null;
    try {
      javaResults = JavaFixtureRunner.runExecTestMethods(classToBeTested);
    } catch (ReflectiveOperationException e) {
      Assertions.fail(e);
    }

    RealTscCheck.Result tsResult = null;
    try {
      tsResult = RealTscCheck.compileAndRun(classToBeTested,
          RealTscCheck.multiMethodRunner(classToBeTested.getSimpleName(), new ArrayList<>(javaResults.keySet())));
    } catch (IOException e) {
      Assertions.fail(e);
    }

    if (!tsResult.output().isBlank()) {
      System.out.println("[node out/run.js output]\n" + tsResult.output());
    }
    assertEquals(0, tsResult.exitCode(), "node execution failed:\n" + tsResult.output());

    Map<String, String> tsResults = tsResult.resultValues();
    for (String methodName : javaResults.keySet()) {
      assertEquals(javaResults.get(methodName), tsResults.get(methodName),
          "Java and transpiled TypeScript disagree on " + classToBeTested.getSimpleName() + "." + methodName + "()");
    }
  }
}
