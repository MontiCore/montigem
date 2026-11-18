package resources.exectests;

import java.util.HashMap;
import java.util.Map;

public class AwaitedResultEntry {

  public String title;

  public int[] hours;

  public AwaitedResultEntry next;

  private final Map<String, AwaitedResultEntry> children = new HashMap<>();

  public AwaitedResultEntry(String title, int[] hours) {
    this.title = title;
    this.hours = hours;
  }

  // async, reads a map
  public AwaitedResultEntry child(String key) {
    return children.get(key);
  }

  public void addChild(String key, AwaitedResultEntry child) {
    children.put(key, child);
  }
}
