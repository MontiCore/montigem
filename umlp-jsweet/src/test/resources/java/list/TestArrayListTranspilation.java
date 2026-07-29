package list;

import resources.domain.Person;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class TestArrayListTranspilation {

  public void test() {
    List<Person> personList = new ArrayList();

    personList.stream().forEach(p -> {
      System.out.println(p);
    });
  }

  public boolean testNegatedSetContains() {
    Set<String> names = new HashSet<>();
    String name = "Alice";

    return !names.contains(name);
  }

  public boolean testNegatedListInstanceOf(Object value) {
    return !(value instanceof List);
  }
}
