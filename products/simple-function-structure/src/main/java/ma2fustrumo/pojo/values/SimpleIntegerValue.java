package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleIntegerValue extends SimpleValue {
  Integer content;

  public SimpleIntegerValue(LocalDateTime timestamp, Integer content) {
    super(timestamp);
    this.content = content;
  }

  public Integer getContent() {
    return content;
  }

  public String printContent() {
    return content.toString();
  }
}
