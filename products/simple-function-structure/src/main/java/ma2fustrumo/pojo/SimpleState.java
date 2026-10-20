package ma2fustrumo.pojo;

import java.util.HashMap;
import java.util.Map;

public class SimpleState {
  protected String name;
  protected SimpleFunctionStream functionStream;
  protected SimpleCategory category;
  protected Map<String, String> metadata = new HashMap<>();

  public SimpleState(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setFunctionStream(SimpleFunctionStream functionStream) {
    this.functionStream = functionStream;
  }

  public void setCategory(SimpleCategory category) {
    this.category = category;
  }

  public SimpleCategory getCategory() {
    return category;
  }

  public SimpleFunctionStream getFunctionStream() {
    return functionStream;
  }

  public void putMetadata(String key, String value) {
    metadata.put(key, value);
  }

  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  public String getMetadata(String key) {
    return metadata.get(key);
  }
}
