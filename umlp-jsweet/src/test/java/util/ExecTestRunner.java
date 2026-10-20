package util;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecTestRunner {

  public static void runExecutionTest(Class<?> classToBeTested) {
    runExecutionTest(classToBeTested, new Class<?>[0]);
  }

  public static void runExecutionTest(Class<?> classToBeTested, Class<?>... supportingClasses) {
    Map<String, String> javaResults = null;
    try {
      javaResults = JavaFixtureRunner.runExecTestMethods(classToBeTested);
    } catch (ReflectiveOperationException e) {
      Assertions.fail(e);
    }

    RealTscCheck.Result tsResult = null;
    try {
      tsResult = RealTscCheck.compileAndRun(classToBeTested,
          RealTscCheck.multiMethodRunner(classToBeTested.getSimpleName(), new ArrayList<>(javaResults.keySet())),
          supportingClasses);
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

  /**
   * Like {@link #runExecutionTest(Class, Class[])}, but runs the transpiled code once per time
   * zone (the {@code TZ} of the node process) and compares each run with the Java results. Use it
   * for code whose browser behaviour may depend on the time zone, e.g. java.time.
   */
  public static void runExecutionTestInTimeZones(Class<?> classToBeTested, List<String> timeZones,
      Class<?>... supportingClasses) {
    Map<String, String> javaResults = null;
    try {
      javaResults = JavaFixtureRunner.runExecTestMethods(classToBeTested);
    } catch (ReflectiveOperationException e) {
      Assertions.fail(e);
    }

    Map<String, RealTscCheck.Result> tsResults = null;
    try {
      tsResults = RealTscCheck.compileAndRunInTimeZones(classToBeTested,
          RealTscCheck.multiMethodRunner(classToBeTested.getSimpleName(), new ArrayList<>(javaResults.keySet())),
          timeZones, supportingClasses);
    } catch (IOException e) {
      Assertions.fail(e);
    }

    List<String> failures = new ArrayList<>();
    for (Map.Entry<String, RealTscCheck.Result> entry : tsResults.entrySet()) {
      String timeZone = entry.getKey();
      RealTscCheck.Result tsResult = entry.getValue();
      if (tsResult.exitCode() != 0) {
        failures.add("TZ=" + timeZone + ": node execution failed:\n" + tsResult.output());
        continue;
      }
      Map<String, String> values = tsResult.resultValues();
      for (String methodName : javaResults.keySet()) {
        if (!Objects.equals(javaResults.get(methodName), values.get(methodName))) {
          failures.add("TZ=" + timeZone + ": " + classToBeTested.getSimpleName() + "." + methodName
              + "() is " + values.get(methodName) + " in TypeScript and "
              + javaResults.get(methodName) + " in Java");
        }
      }
    }
    if (!failures.isEmpty()) {
      Assertions.fail("Java and transpiled TypeScript disagree:\n" + String.join("\n", failures));
    }
  }
}
