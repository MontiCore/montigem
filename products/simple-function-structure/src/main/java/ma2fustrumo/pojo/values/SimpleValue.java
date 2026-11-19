package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleValue {
  protected LocalDateTime timestamp;

  public SimpleValue(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }
}
