package resources.exectests;

import java.util.List;

public final class NestedFlatMapElementSource {

  private NestedFlatMapElementSource() {
  }

  public static List<NestedFlatMapElement> elements() {
    return List.of(new NestedFlatMapElement(42));
  }
}
