package resources.exectests;

import util.JSweetTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * see examples/jsweet-test's browser-driven UtilArraysE2ETest.java / TestUtilArrays.java.
 * Deliberately excludes testStream/testStreamRange: the original test itself has them disabled
 * ("TODO toArray is erased"), since Arrays.stream(...).map(...).toArray() relies on toArray(),
 * which the adapters deliberately leave unsupported (same reason
 * ArrayStreamAdapterRuntimeCheck excludes its toArray check).
 */
public class UtilArraysRuntimeCheck {

  private static String intArrayAsString(int[] array) {
    ArrayList<String> strings = new ArrayList<>();
    for (int value : array) {
      strings.add(Integer.toString(value));
    }
    return stringsAsString(strings);
  }

  private static String integerArrayAsString(Integer[] array) {
    ArrayList<String> strings = new ArrayList<>();
    for (Integer value : array) {
      strings.add(Integer.toString(value));
    }
    return stringsAsString(strings);
  }

  private static String integerListAsString(List<Integer> list) {
    ArrayList<String> strings = new ArrayList<>();
    for (int value : list) {
      strings.add(Integer.toString(value));
    }
    return stringsAsString(strings);
  }

  private static String stringsAsString(List<String> strings) {
    return "[" + String.join(",", strings) + "]";
  }

  @JSweetTest
  public static String asListArr() {
    Integer[] array = new Integer[]{1, 2, 3};
    List<Integer> list = Arrays.asList(array);

    Object arrayAsObj = array;
    Object listAsObj = list;
    boolean notEqual = (arrayAsObj != listAsObj);

    return "{ "
        + "notEqual: " + notEqual + ", "
        + "array: " + integerArrayAsString(array) + ", "
        + "list: " + integerListAsString(list) + " }";
  }

  @JSweetTest
  public static String asListParams() {
    List<Integer> list = Arrays.asList(1, 2, 3);
    return integerListAsString(list);
  }

  @JSweetTest
  public static String copyOf() {
    int[] array = new int[]{6, 5, 4, 3, 2, 1};
    int[] copy = Arrays.copyOf(array, 3);

    return "{ "
        + "array: " + intArrayAsString(array) + ", "
        + "copy: " + intArrayAsString(copy) + " }";
  }

  @JSweetTest
  public static String copyOfRange() {
    int[] array = new int[]{6, 5, 4, 3, 2, 1};
    int[] copy = Arrays.copyOfRange(array, 1, 3);

    return "{ "
        + "array: " + intArrayAsString(array) + ", "
        + "copy: " + intArrayAsString(copy) + " }";
  }

  @JSweetTest
  public static String fill() {
    int[] array = new int[]{6, 5, 4, 3, 2, 1};
    Arrays.fill(array, 7);
    return intArrayAsString(array);
  }

  @JSweetTest
  public static String fillRange() {
    int[] array = new int[]{6, 5, 4, 3, 2, 1};
    Arrays.fill(array, 1, 3, 7);
    return intArrayAsString(array);
  }

  @JSweetTest
  public static String sort() {
    int[] array = new int[]{6, 5, 4, 3, 2, 1};
    Arrays.sort(array);
    return intArrayAsString(array);
  }

  @JSweetTest
  public static String toStringCheck() {
    return Arrays.toString(new Integer[]{1, 2, null, 3});
  }
}
