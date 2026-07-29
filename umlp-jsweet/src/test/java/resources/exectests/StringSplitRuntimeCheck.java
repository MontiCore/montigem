package resources.exectests;

import util.JSweetTest;

/**
 * see examples/jsweet-test's browser-driven StringSplitE2ETest.java / StringSplit.java. The
 * original reads input/regex/limit from DOM fields set per-scenario by the E2E test; here those
 * exact literal scenarios (one static method pair per @Test in StringSplitE2ETest) are inlined
 * directly instead.
 */
public class StringSplitRuntimeCheck {

  private static final String MULTILINE = "foo\noOooO\nbar\nOobaz\noooooooO\nmonticore\no";

  private static String split(String input, String regex) {
    return String.join("#", input.split(regex));
  }

  private static String limitSplit(String input, String regex, int limit) {
    return String.join("#", input.split(regex, limit));
  }

  // testSplitSimple
  @JSweetTest
  public static String splitSimple() {
    return split("a,,b,c,d,e,", ",");
  }

  @JSweetTest
  public static String limitSplitSimple() {
    return limitSplit("a,,b,c,d,e,", ",", 4);
  }

  // testSplitRegex
  @JSweetTest
  public static String splitRegex() {
    return split("a||b|c|d|e|", "\\|");
  }

  @JSweetTest
  public static String limitSplitRegex() {
    return limitSplit("a||b|c|d|e|", "\\|", 4);
  }

  // testSplitEmptyRegex
  @JSweetTest
  public static String splitEmptyRegex() {
    return split("abcde", "");
  }

  @JSweetTest
  public static String limitSplitEmptyRegex() {
    return limitSplit("abcde", "", 3);
  }

  @JSweetTest
  public static String splitCaseInsensitiveEmptyRegex() {
    return split("abcde", "(?i)");
  }

  @JSweetTest
  public static String limitSplitCaseInsensitiveEmptyRegex() {
    return limitSplit("abcde", "(?i)", 3);
  }

  // testSplitEmbeddedIFlag
  @JSweetTest
  public static String splitEmbeddedIFlag() {
    return split("azbYccZdddyeezfYgz", "(?i)[yz]");
  }

  @JSweetTest
  public static String limitSplitEmbeddedIFlag() {
    return limitSplit("azbYccZdddyeezfYgz", "(?i)[yz]", 6);
  }

  // testSplitEmbeddedIMFlags
  @JSweetTest
  public static String splitEmbeddedImThenIFlags() {
    return split(MULTILINE, "(?m)(?i)^o+$");
  }

  @JSweetTest
  public static String limitSplitEmbeddedImThenIFlags() {
    return limitSplit(MULTILINE, "(?m)(?i)^o+$", 2);
  }

  @JSweetTest
  public static String splitEmbeddedCombinedImFlag() {
    return split(MULTILINE, "(?im)^o+$");
  }

  @JSweetTest
  public static String limitSplitEmbeddedCombinedImFlag() {
    return limitSplit(MULTILINE, "(?im)^o+$", 2);
  }

  @JSweetTest
  public static String splitEmbeddedIFlagOnly() {
    return split(MULTILINE, "(?i)^o+$");
  }

  @JSweetTest
  public static String limitSplitEmbeddedIFlagOnly() {
    return limitSplit(MULTILINE, "(?i)^o+$", 2);
  }
}
