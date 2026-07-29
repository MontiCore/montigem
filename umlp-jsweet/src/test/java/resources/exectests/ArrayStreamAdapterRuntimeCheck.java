package resources.exectests;

import util.JSweetTest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Mirrors the pure Stream logic exercised by examples/jsweet-test's browser-driven
 * ArrayStreamAdapterTestE2ETest.java / ArrayStreamAdapterTest.java, minus the GUI/DOM plumbing.
 * Deliberately excludes the checks whose result is a List/Set (map, filter, mapPlusFilter,
 * flatMap, distinct, limit, skip, sorted, collectToSet): JavaFixtureRunner only supports
 * boolean/numeric/String returns, where Java's and JS's own formatting agree byte-for-byte --
 * a List's toString() does not (Java: "[1, 2, 3]" vs the RTE's AsyncList: "[1,2,3]"), so those
 * would need real, matching JSON serialization on both sides to compare, not attempted here.
 * Also excludes toArray(), which is deliberately unsupported by the adapters and expected to
 * throw at the JS runtime -- not a value comparison.
 */
public class ArrayStreamAdapterRuntimeCheck {

  @JSweetTest
  public static long countAboveFive() {
    return List.of(1, 2, 10, 20).stream().filter(n -> n > 5).count();
  }

  @JSweetTest
  public static int reduceSum() {
    return List.of(1, 2, 3, 4).stream().reduce(0, (a, b) -> a + b);
  }

  @JSweetTest
  public static String matches() {
    List<Integer> nums = List.of(1, 2, 3);
    boolean any = nums.stream().anyMatch(n -> n > 2);
    boolean all = nums.stream().allMatch(n -> n < 5);
    return "any>2: " + any + ", all<5: " + all;
  }

  @JSweetTest
  public static int findFirstAboveFive() {
    return List.of(1, 2, 3, 99, 100, 101).stream().filter(n -> n > 5).findFirst().orElse(-1);
  }

  @JSweetTest
  public static int findAnyAboveFive() {
    return List.of(1, 2, 10).stream().filter(n -> n > 5).findAny().orElse(-1);
  }

  @JSweetTest
  public static int min() {
    return List.of(10, 5, 20, 3).stream().min((a, b) -> a.compareTo(b)).orElse(-1);
  }

  @JSweetTest
  public static int max() {
    return List.of(10, 5, 20, 3).stream().max((a, b) -> a.compareTo(b)).orElse(-1);
  }

  @JSweetTest
  public static String collectJoining() {
    return List.of("Apple", "Banana", "Cherry").stream().collect(Collectors.joining(" - "));
  }

  @JSweetTest
  public static String forEachAppend() {
    StringBuilder sb = new StringBuilder();
    List.of(1, 2, 3).stream().forEach(n -> sb.append(n).append("!"));
    return sb.toString();
  }

  @JSweetTest
  public static boolean noneMatchAboveTen() {
    return List.of(1, 2, 3).stream().noneMatch(n -> n > 10);
  }

  @JSweetTest
  public static long collectCounting() {
    return List.of(1, 2, 3, 4, 5).stream().collect(Collectors.counting());
  }

  @JSweetTest
  public static String peekAppend() {
    StringBuilder sb = new StringBuilder();
    List.of(8, 9).stream().peek(x -> sb.append(x).append(";")).collect(Collectors.toList());
    return sb.toString();
  }
}
