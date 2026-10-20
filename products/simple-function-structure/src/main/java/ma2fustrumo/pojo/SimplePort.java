package ma2fustrumo.pojo;

import de.se_rwth.commons.Names;

import java.util.HashMap;
import java.util.Map;

public class SimplePort {
  protected String fqn;
  protected boolean isOutput;
  protected SimpleChannel channel;
  protected Map<String, String> metadata = new HashMap<>();
  protected boolean spansChannel = false;

  public SimplePort(String fqn, boolean isOutput) {
    this.fqn = fqn;
    this.isOutput = isOutput;
  }

  public SimpleChannel getChannel() {
    return channel;
  }

  public void setChannel(SimpleChannel channel) {
    this.channel = channel;
  }

  public boolean isOutput() {
    return isOutput;
  }

  public String getName() {
    return Names.getSimpleName(fqn);
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

  public String getFqn() {
    return fqn;
  }

  public void setSpansChannel(boolean b) {
    this.spansChannel = b;
  }

  public boolean isSpansChannel() {
    return spansChannel;
  }
}
