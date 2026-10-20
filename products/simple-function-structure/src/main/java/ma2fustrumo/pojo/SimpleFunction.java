package ma2fustrumo.pojo;

import com.google.common.collect.Streams;
import de.se_rwth.commons.Names;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleFunction {
  protected String name;
  protected String typeFqn;
  protected String fqn;
  protected Map<String, SimpleFunction> subfunctions = new HashMap<>();
  protected List<SimplePort> inPorts = new ArrayList<>();
  protected List<SimplePort> outPorts = new ArrayList<>();
  protected List<SimpleState> states = new ArrayList<>();
  protected Map<String, String> metadata = new HashMap<>();

  public SimpleFunction(String typeFqn, String fqn, String name) {
    this.typeFqn = typeFqn;
    this.name = name;
    this.fqn = fqn;
  }

  public void addSubfunction(String name, SimpleFunction subfunction) {
    subfunctions.put(name, subfunction);
  }

  public void addIn(SimplePort port) {
    inPorts.add(port);
  }

  public void addOut(SimplePort port) {
    outPorts.add(port);
  }

  public void addState(SimpleState state) {
    states.add(state);
  }

  public String getType() {
    return Names.getSimpleName(typeFqn);
  }

  public String getTypeFqn() {
    return typeFqn;
  }

  public String getName() {
    return name;
  }

  public String getFqn() {
    return fqn;
  }

  public Map<String, SimpleFunction> getSubfunctions() {
    return subfunctions;
  }

  public List<SimplePort> getInPorts() {
    return inPorts;
  }

  public List<SimplePort> getOutPorts() {
    return outPorts;
  }

  public List<SimplePort> getAllPorts() {
    return Streams.concat(inPorts.stream(), outPorts.stream()).toList();
  }

  public List<SimpleState> getStates() {
    return states;
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
