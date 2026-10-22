package exectests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.RealTscCheck;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the hand-written TypeScript checks of commonj2ts' AsyncMap (src/test/resources/tsc-check/asyncmap)
 * with a real tsc and node, and compares each result with the same scenario on a java.util.LinkedHashMap.
 */
public class AsyncMapExecutionTest {

  private static final Path FIXTURE = Paths.get("src/test/resources/tsc-check/asyncmap/AsyncMapCheck.ts");

  private static Map<String, String> tsResults;

  @BeforeAll
  public static void runTypeScript() throws IOException {
    assumeTrue(RealTscCheck.commonj2tsAvailable());
    List<String> methods = List.of("basics", "defaults", "nullArguments", "asyncLambdas", "views",
        "viewBulkOperations", "weaklyConsistentIteration", "entrySetValue", "unsupportedOperations",
        "equalsAndHashCode", "constructors");
    RealTscCheck.Result result = RealTscCheck.compileAndRunTypeScript("AsyncMapCheck", FIXTURE,
        RealTscCheck.multiMethodRunner("AsyncMapCheck", methods));
    assertEquals(0, result.exitCode(), "node execution failed:\n" + result.output());
    tsResults = result.resultValues();
  }

  private static void assertTs(String expected, String method) {
    // the runner prints the results as JSON strings
    assertEquals("\"" + expected + "\"", tsResults.get(method), "AsyncMapCheck." + method + "()");
  }

  private static Map<String, Integer> map(Object... keysAndValues) {
    Map<String, Integer> m = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      m.put((String) keysAndValues[i], (Integer) keysAndValues[i + 1]);
    }
    return m;
  }

  private static String s(Object... values) {
    return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(","));
  }

  private static String exceptionName(Runnable r) {
    try {
      r.run();
      return "no exception";
    } catch (UnsupportedOperationException e) {
      return "UnsupportedOperationException";
    }
  }

  @Test
  public void basics() {
    Map<String, Integer> m = map("a", 1, "b", 2);
    assertTs(s(m.put("a", 3), m.get("a"), m.get("x"), m.containsKey("b"), m.containsKey("x"),
        m.containsValue(2), m.containsValue(1), m.size(), m.isEmpty(),
        m.remove("b"), m.remove("x"), m.toString(), m.size()), "basics");
  }

  @Test
  public void defaults() {
    Map<String, Integer> m = map("a", 1, "n", null);
    List<Object> r = new ArrayList<>();
    r.addAll(List.of(m.getOrDefault("a", 9), m.getOrDefault("x", 9)));
    r.add(m.getOrDefault("n", 9));
    r.addAll(Arrays.asList(m.putIfAbsent("a", 5), m.putIfAbsent("b", 2), m.putIfAbsent("n", 7)));
    r.addAll(List.of(m.remove("a", 2), m.remove("a", 1), m.containsKey("a")));
    r.addAll(Arrays.asList(m.replace("b", 3), m.replace("x", 3), m.containsKey("x")));
    r.addAll(List.of(m.replace("b", 9, 4), m.replace("b", 3, 4), m.get("b")));
    r.addAll(Arrays.asList(m.compute("c", (k, v) -> v == null ? 1 : v + 1), m.compute("c", (k, v) -> v == null ? 1 : v + 1),
        m.compute("c", (k, v) -> null), m.containsKey("c")));
    r.addAll(Arrays.asList(m.computeIfAbsent("d", k -> 10), m.computeIfAbsent("d", k -> 20),
        m.computeIfAbsent("e", k -> null), m.containsKey("e")));
    r.addAll(Arrays.asList(m.computeIfPresent("d", (k, v) -> v + 1), m.computeIfPresent("x", (k, v) -> 1),
        m.computeIfPresent("d", (k, v) -> null), m.containsKey("d")));
    r.addAll(Arrays.asList(m.merge("f", 1, Integer::sum), m.merge("f", 5, Integer::sum),
        m.merge("f", 1, (a, b) -> null), m.containsKey("f")));
    r.add(m.toString());
    assertTs(s(r.toArray()), "defaults");
  }

  @Test
  public void nullArguments() {
    Map<String, Integer> m = map("a", 1, "n", null);
    assertTs(s(m.remove("a", null), m.remove("n", null), m.containsKey("n"),
        m.replace("a", null, 5), m.replace("a", 1, null), m.get("a"), m.containsKey("a")), "nullArguments");
  }

  @Test
  public void asyncLambdas() {
    Map<String, Integer> m = map("a", 1, "b", 2);
    m.compute("a", (k, v) -> v + 10);
    m.merge("b", 5, (x, y) -> x * y);
    m.replaceAll((k, v) -> v + 1);
    List<String> seen = new ArrayList<>();
    m.forEach((k, v) -> seen.add(k + "=" + v));
    assertTs(s(m.toString(), String.join(";", seen)), "asyncLambdas");
  }

  @Test
  public void views() {
    Map<String, Integer> m = map("a", 1, "b", 2, "c", 2);
    Set<String> keys = m.keySet();
    Collection<Integer> values = m.values();
    Set<Map.Entry<String, Integer>> entries = m.entrySet();
    m.put("d", 4);
    List<Object> r = new ArrayList<>(List.of(keys.size(), keys.contains("d"), values.contains(4),
        entries.contains(Map.entry("d", 4)), entries.contains(Map.entry("d", 5))));
    r.addAll(List.of(keys.remove("a"), keys.remove("x"), m.containsKey("a")));
    r.addAll(List.of(values.remove(2), m.toString()));
    r.addAll(List.of(entries.remove(Map.entry("c", 3)), entries.remove(Map.entry("c", 2)), m.toString()));
    r.addAll(List.of(keys.equals(Set.of("d")), m.size()));
    keys.clear();
    r.addAll(List.of(m.isEmpty(), keys.size()));
    assertTs(s(r.toArray()), "views");
  }

  @Test
  public void viewBulkOperations() {
    Map<String, Integer> m = map("a", 1, "b", 2, "c", 3, "d", 4);
    Set<String> keys = m.keySet();
    List<Object> r = new ArrayList<>();
    r.addAll(List.of(keys.removeIf(k -> k.equals("a")), m.toString()));
    r.addAll(List.of(keys.removeAll(Set.of("b", "x")), m.toString()));
    r.addAll(List.of(keys.retainAll(Set.of("d")), m.toString()));
    r.addAll(List.of(keys.containsAll(Set.of("d")), keys.containsAll(Set.of("d", "a"))));
    assertTs(s(r.toArray()), "viewBulkOperations");
  }

  @Test
  public void weaklyConsistentIteration() {
    // the keys present at the start, without the removed b, with the current value of c, without the added z
    assertTs("a=1;c=30,{a=1, c=30, z=26}", "weaklyConsistentIteration");
  }

  @Test
  public void entrySetValue() {
    Map<String, Integer> m = map("a", 1, "b", 2);
    List<Object> r = new ArrayList<>();
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      r.add(e.setValue(e.getValue() * 10));
      r.add(e.getValue());
    }
    r.add(m.toString());
    Map.Entry<String, Integer> held = m.entrySet().iterator().next();
    m.remove("a");
    r.addAll(List.of(held.setValue(7), held.getValue(), m.toString()));
    assertTs(s(r.toArray()), "entrySetValue");
  }

  @Test
  public void unsupportedOperations() {
    Map<String, Integer> m = map("a", 1);
    List<Supplier<String>> checks = List.of(
        () -> exceptionName(() -> Map.entry("x", 1).setValue(2)),
        () -> exceptionName(() -> m.keySet().add("b")),
        () -> exceptionName(() -> m.values().add(2)),
        () -> exceptionName(() -> m.entrySet().add(Map.entry("b", 2))));
    List<Object> r = new ArrayList<>();
    checks.forEach(c -> r.add(c.get()));
    r.add(m.toString());
    assertTs(s(r.toArray()), "unsupportedOperations");
  }

  @Test
  public void equalsAndHashCode() {
    Map<String, Integer> m1 = map("a", 1, "b", 2);
    Map<String, Integer> m2 = map("b", 2, "a", 1);
    Map<String, Integer> m3 = map("a", 1, "b", 3);
    Map<String, Integer> m4 = map("a", 1, "c", null);
    Map<String, Integer> m5 = map("a", 1, "d", null);
    assertTs(s(m1.equals(m2), m1.equals(m3), m4.equals(m5), m1.hashCode(), m4.hashCode(),
        m1.hashCode() == m2.hashCode()), "equalsAndHashCode");
  }

  @Test
  public void constructors() {
    // a copy is independent, a wrapped native Map is shared, the capacity is ignored
    assertTs("{a=1},{a=1, b=2},0,{n=1, m=2, w=3},3,true,{t=1, u=2},{a=1, n=1, m=2, w=3}", "constructors");
  }
}
