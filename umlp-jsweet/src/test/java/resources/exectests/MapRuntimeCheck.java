package resources.exectests;

import util.JSweetTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * java.util.Map in transpiled code: java.util.Map becomes IUMLPMap, the map classes become AsyncMap.
 */
public class MapRuntimeCheck {

  private final Map<String, Integer> counts = new LinkedHashMap<>();

  private static Map<String, Integer> abc() {
    Map<String, Integer> m = new LinkedHashMap<>();
    m.put("a", 1);
    m.put("b", 2);
    m.put("c", 3);
    return m;
  }

  private static int sumOfValues(Map<String, Integer> m) {
    int total = 0;
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      total += e.getValue();
    }
    return total;
  }

  private void count(String word) {
    counts.merge(word, 1, (a, b) -> a + b);
  }

  @JSweetTest
  public static String basics() {
    HashMap<String, Integer> m = new HashMap<>();
    Integer first = m.put("a", 1);
    Integer second = m.put("a", 2);
    return first + "," + second + "," + m.get("a") + "," + m.get("x") + "," + m.containsKey("a") + ","
        + m.containsValue(2) + "," + m.size() + "," + m.isEmpty() + "," + m.remove("a") + "," + m.isEmpty();
  }

  @JSweetTest
  public static int interfaceTypedParameter() {
    return sumOfValues(abc());
  }

  @JSweetTest
  public static String iterateViews() {
    Map<String, Integer> m = abc();
    String keys = "";
    for (String k : m.keySet()) {
      keys += k;
    }
    int values = 0;
    for (Integer v : m.values()) {
      values += v;
    }
    String entries = "";
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      entries += e.getKey() + "=" + e.getValue() + ";";
    }
    return keys + "," + values + "," + entries;
  }

  @JSweetTest
  public static String defaultMethods() {
    Map<String, Integer> m = abc();
    Integer absent = m.putIfAbsent("d", 4);
    Integer present = m.putIfAbsent("a", 9);
    Integer computed = m.computeIfAbsent("e", k -> k.length() * 5);
    Integer merged = m.merge("a", 10, (x, y) -> x + y);
    boolean removed = m.remove("b", 2);
    boolean notRemoved = m.remove("c", 2);
    boolean replaced = m.replace("c", 3, 30);
    Integer replacedValue = m.replace("x", 1);
    return absent + "," + present + "," + computed + "," + merged + "," + removed + "," + notRemoved + ","
        + replaced + "," + replacedValue + "," + m.getOrDefault("x", -1) + "," + sumOfValues(m);
  }

  @JSweetTest
  public static int entrySetValue() {
    Map<String, Integer> m = abc();
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      e.setValue(e.getValue() * 10);
    }
    return sumOfValues(m);
  }

  @JSweetTest
  public static String lambdas() {
    Map<String, Integer> m = abc();
    m.replaceAll((k, v) -> v + 1);
    List<String> seen = new ArrayList<>();
    m.forEach((k, v) -> seen.add(k + v));
    return String.join(";", seen);
  }

  @JSweetTest
  public static String nestedCollections() {
    Map<String, List<Integer>> groups = new HashMap<>();
    groups.computeIfAbsent("odd", k -> new ArrayList<>()).add(1);
    groups.computeIfAbsent("even", k -> new ArrayList<>()).add(2);
    groups.computeIfAbsent("odd", k -> new ArrayList<>()).add(3);
    return groups.get("odd").size() + "," + groups.get("even").size() + "," + groups.get("odd").get(1);
  }

  @JSweetTest
  public static String liveViews() {
    Map<String, Integer> m = abc();
    Set<String> keys = m.keySet();
    m.put("d", 4);
    boolean removed = keys.remove("a");
    return keys.size() + "," + removed + "," + m.containsKey("a") + "," + m.size();
  }

  @JSweetTest
  public static String copyConstructor() {
    Map<String, Integer> original = abc();
    Map<String, Integer> copy = new LinkedHashMap<>(original);
    copy.put("z", 26);
    Map<Integer, String> sized = new HashMap<>(16);
    sized.put(1, "one");
    return original.size() + "," + copy.size() + "," + sized.get(1);
  }

  @JSweetTest
  public static String equality() {
    Map<String, Integer> m1 = abc();
    Map<String, Integer> m2 = new HashMap<>(abc());
    boolean equalBefore = m1.equals(m2);
    m2.put("a", 5);
    return equalBefore + "," + m1.equals(m2) + "," + (abc().hashCode() == m1.hashCode());
  }

  @JSweetTest
  public static String instanceOfMap() {
    Object m = abc();
    Object list = new ArrayList<String>();
    return (m instanceof Map) + "," + (list instanceof Map);
  }

  /** only order independent operations: TreeMap sorts its keys, AsyncMap keeps the insertion order */
  @JSweetTest
  public static String sortedMap() {
    TreeMap<String, Integer> m = new TreeMap<>();
    m.put("b", 2);
    m.put("a", 1);
    return m.size() + "," + m.get("a") + "," + m.containsKey("b") + "," + sumOfValues(m);
  }

  @JSweetTest
  public static int mapField() {
    MapRuntimeCheck check = new MapRuntimeCheck();
    check.count("x");
    check.count("y");
    check.count("x");
    return check.counts.get("x") * 10 + check.counts.get("y");
  }
}
