package resources.exectests;

import util.JSweetTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Field and array accesses on the result of an awaited call: the await needs parentheses.
 */
public class AwaitedResultRuntimeCheck {

  private final Map<String, AwaitedResultEntry> entries = new HashMap<>();

  private final AwaitedResultEntry empty = new AwaitedResultEntry("-", new int[0]);

  // setup in a method: local variables in a constructor are not transpiled correctly
  private static AwaitedResultRuntimeCheck create() {
    AwaitedResultRuntimeCheck check = new AwaitedResultRuntimeCheck();
    check.fill();
    return check;
  }

  private void fill() {
    AwaitedResultEntry a = new AwaitedResultEntry("A", new int[] {1, 2});
    AwaitedResultEntry b = new AwaitedResultEntry("B", new int[] {4});
    AwaitedResultEntry c = new AwaitedResultEntry("C", new int[] {5});
    a.next = b;
    a.addChild("c", c);
    b.addChild("c", c);
    c.next = b;
    entries.put("a", a);
    entries.put("A", b);
    entries.put("B", c);
  }

  // async, reads a map
  private AwaitedResultEntry entry(String key) {
    AwaitedResultEntry entry = entries.get(key);
    return entry != null ? entry : empty;
  }

  // async, calls entry
  private int[] hours(String key) {
    return entry(key).hours;
  }

  // async without a map: iterates a list
  private static AwaitedResultEntry first(List<AwaitedResultEntry> list) {
    for (AwaitedResultEntry entry : list) {
      return entry;
    }
    return null;
  }

  @JSweetTest
  public static String fieldOfAwaitedListCall() {
    List<AwaitedResultEntry> list = new ArrayList<>();
    list.add(new AwaitedResultEntry("L", new int[] {3}));
    return first(list).title + "," + first(list).hours[0];
  }

  @JSweetTest
  public static String fieldOfAwaitedCall() {
    AwaitedResultRuntimeCheck check = create();
    return check.entry("a").title + "," + check.entry("x").title;
  }

  @JSweetTest
  public static String fieldOfAwaitedMapGet() {
    AwaitedResultRuntimeCheck check = create();
    return check.entries.get("a").title + "," + check.entries.get("a").hours.length;
  }

  @JSweetTest
  public static int arrayElementOfAwaitedCall() {
    AwaitedResultRuntimeCheck check = create();
    return check.hours("a")[1] + check.entry("a").hours[0];
  }

  @JSweetTest
  public static int lengthOfAwaitedCall() {
    AwaitedResultRuntimeCheck check = create();
    return check.hours("a").length;
  }

  @JSweetTest
  public static String fieldOfChainedAwaitedCalls() {
    AwaitedResultRuntimeCheck check = create();
    return check.entry("a").child("c").title + "," + check.entry("a").child("c").next.child("c").hours[0];
  }

  @JSweetTest
  public static String awaitedCallAfterField() {
    AwaitedResultRuntimeCheck check = create();
    return check.entry("a").next.child("c").title + "," + check.entries.get("a").next.child("c").title;
  }

  @JSweetTest
  public static String awaitedCallAsArgument() {
    AwaitedResultRuntimeCheck check = create();
    return check.entry(check.entry("a").title).title + "," + check.hours(check.entry("a").child("c").next.title)[0];
  }
}
