package gui.test;

import def.dom.HTMLInputElement;
import umlp.jsweet.extension.annotation.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static def.dom.Globals.document;

/**
 * Runs java.util.Map scenarios in the browser (the maps are AsyncMaps in transpiled code), see
 * e2e.MapAdapterTestE2ETest
 */
@Component
public class MapAdapterTest extends MapAdapterTestTOP {

  private static final String DEFAULT_ENTRIES = "a=1, b=2, c=3";

  // a map field, kept across the clicks
  private final Map<String, Integer> clicks = new LinkedHashMap<>();

  private void setResultFieldEntry(String value) {
    HTMLInputElement el = (HTMLInputElement) document.getElementById("mapResultIdInput");
    if (el != null) {
      el.value = value;
    }
  }

  /** a fresh LinkedHashMap of the input, e.g. "a=1, b=2, c=3", so each button works on an isolated map */
  private Map<String, Integer> getInputMap() {
    HTMLInputElement el = (HTMLInputElement) document.getElementById("mapInputTestFieldIdInput");
    String raw = el != null && el.value != null && !el.value.trim().isEmpty() ? el.value.trim() : DEFAULT_ENTRIES;
    Map<String, Integer> result = new LinkedHashMap<>();
    for (String entry : raw.split(",")) {
      String[] keyAndValue = entry.trim().split("=");
      result.put(keyAndValue[0].trim(), Integer.parseInt(keyAndValue[1].trim()));
    }
    return result;
  }

  /** the entries in their order, e.g. "a=1, b=2" */
  private String print(Map<String, Integer> map) {
    List<String> entries = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      entries.add(entry.getKey() + "=" + entry.getValue());
    }
    return String.join(", ", entries);
  }

  public Consumer<Void> testPutAndGet() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      Integer added = map.put("d", 4);
      Integer replaced = map.put("a", 10);
      setResultFieldEntry("put(): added=" + added + ", replaced=" + replaced + ", get(a)=" + map.get("a")
          + ", get(z)=" + map.get("z") + ", size=" + map.size());
    };
  }

  /** a LinkedHashMap keeps the insertion order, a replaced value keeps its position */
  public Consumer<Void> testInsertionOrder() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      map.put("d", 4);
      map.put("a", 10);
      setResultFieldEntry("order: " + print(map));
    };
  }

  public Consumer<Void> testContainsAndRemove() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      boolean hasB = map.containsKey("b");
      boolean hasThree = map.containsValue(3);
      Integer removed = map.remove("b");
      Integer missing = map.remove("z");
      setResultFieldEntry("containsKey(b)=" + hasB + ", containsValue(3)=" + hasThree + ", remove(b)=" + removed
          + ", remove(z)=" + missing + ", map=" + print(map));
    };
  }

  public Consumer<Void> testIterate() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      int sum = 0;
      for (Integer value : map.values()) {
        sum += value;
      }
      String keys = "";
      for (String key : map.keySet()) {
        keys += key;
      }
      setResultFieldEntry("keys=" + keys + ", sum=" + sum + ", entries=" + print(map));
    };
  }

  public Consumer<Void> testDefaultMethods() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      int orDefault = map.getOrDefault("z", -1);
      map.putIfAbsent("a", 99);
      map.putIfAbsent("e", 5);
      map.merge("b", 10, (old, added) -> old + added);
      map.compute("c", (key, value) -> value == null ? 0 : value * 100);
      map.computeIfAbsent("f", key -> 6);
      Map<String, List<String>> groups = new LinkedHashMap<>();
      for (String word : Arrays.asList("apple", "avocado", "banana")) {
        groups.computeIfAbsent(word.substring(0, 1), key -> new ArrayList<>()).add(word);
      }
      setResultFieldEntry("getOrDefault=" + orDefault + ", map=" + print(map) + ", groups=" + groups.get("a").size()
          + "/" + groups.get("b").size());
    };
  }

  /** keySet, values and entrySet are live views: changes through them change the map */
  public Consumer<Void> testLiveViews() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      map.keySet().remove("a");
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        entry.setValue(entry.getValue() * 2);
      }
      map.put("x", 7);
      boolean viewSeesPut = map.keySet().contains("x");
      setResultFieldEntry("views: map=" + print(map) + ", keySet sees put=" + viewSeesPut);
    };
  }

  public Consumer<Void> testEqualityAndCopy() {
    return (Void) -> {
      Map<String, Integer> map = getInputMap();
      Map<String, Integer> copy = new HashMap<>(map);
      boolean equalCopy = copy.equals(map) && Objects.equals(map, copy);
      copy.put("a", 0);
      setResultFieldEntry("equals(copy)=" + equalCopy + ", afterChange=" + copy.equals(map) + ", original a="
          + map.get("a"));
    };
  }

  /** a map field keeps its entries across the clicks */
  public Consumer<Void> testMapField() {
    return (Void) -> {
      clicks.merge("field", 1, (old, added) -> old + added);
      setResultFieldEntry("field clicks=" + clicks.get("field"));
    };
  }

  /** java.time keys are equal by value, like in Java: a newly created equal key finds the stored one */
  public Consumer<Void> testTimeKeys() {
    return (Void) -> {
      Map<LocalDate, Integer> visits = new LinkedHashMap<>();
      visits.put(LocalDate.of(2026, 11, 17), 1);
      visits.put(LocalDate.of(2026, 11, 17), visits.get(LocalDate.of(2026, 11, 17)) + 1);
      boolean found = visits.containsKey(LocalDate.parse("2026-11-17"));
      setResultFieldEntry("date keys: size=" + visits.size() + ", visits=" + visits.get(LocalDate.of(2026, 11, 17))
          + ", containsKey=" + found);
    };
  }

  public Consumer<Void> testIsEmptyAndClear() {
    return (Void) -> {
      Map<String, Integer> map = new HashMap<>();
      boolean before = map.isEmpty();
      map.putAll(getInputMap());
      boolean afterPut = map.isEmpty();
      map.clear();
      setResultFieldEntry("isEmptyClear: before=" + before + ", afterPutAll=" + afterPut + ", afterClear="
          + map.isEmpty() + ", size=" + map.size());
    };
  }
}
