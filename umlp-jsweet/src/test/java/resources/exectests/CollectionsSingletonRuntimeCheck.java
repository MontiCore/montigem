package resources.exectests;

import util.JSweetTest;

import java.util.Collections;
import java.util.Set;

public class CollectionsSingletonRuntimeCheck {

  @JSweetTest
  public static boolean singletonContainsItsElement() {
    Set<String> single = Collections.singleton("hello");
    return single.contains("hello");
  }
}
