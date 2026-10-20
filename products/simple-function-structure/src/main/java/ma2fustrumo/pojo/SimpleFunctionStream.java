package ma2fustrumo.pojo;

import ma2fustrumo.pojo.values.SimpleValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimpleFunctionStream {
  protected SimpleCategory category;
  protected List<SimpleValue> values = new ArrayList<>();

  public SimpleFunctionStream(SimpleCategory category) {
    this.category = Objects.requireNonNull(category);
  }

  public SimpleCategory getCategory() {
    return category;
  }

  public void addValue(SimpleValue value) {
    values.add(value);
  }

  public List<SimpleValue> getValues() {
    return values;
  }
}
