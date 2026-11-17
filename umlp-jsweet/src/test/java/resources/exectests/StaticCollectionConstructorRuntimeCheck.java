package resources.exectests;

import util.JSweetTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Methods whose only asynchronous call is the constructor of a JDK collection that becomes an async RTE collection
 * (asyncjre.json: java.util.HashMap.new, ...), like a lazy static accessor: they must become async, or the awaited
 * constructor call ends up in a synchronous function
 */
public class StaticCollectionConstructorRuntimeCheck {

  private static Map<String, Double> minima;

  // lazy static accessor: a static field must not be initialized with an async constructor call (0xEE010)
  private static Map<String, Double> minima() {
    if (minima == null) {
      minima = new HashMap<>();
    }
    return minima;
  }

  private static Map<String, Integer> newLinkedMap() {
    return new LinkedHashMap<>();
  }

  private static Set<String> newSet() {
    return new HashSet<>();
  }

  @JSweetTest
  public static int lazyStaticMap() {
    minima().put("a", 1.0);
    minima().put("b", 2.0);
    return minima().size();
  }

  @JSweetTest
  public static int staticLinkedMap() {
    Map<String, Integer> map = newLinkedMap();
    map.put("x", 1);
    return map.get("x");
  }

  @JSweetTest
  public static boolean staticSet() {
    Set<String> set = newSet();
    set.add("x");
    return set.contains("x");
  }
}
