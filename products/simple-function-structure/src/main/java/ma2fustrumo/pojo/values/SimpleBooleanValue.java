package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleBooleanValue extends SimpleValue {
  Boolean content;

  public SimpleBooleanValue(LocalDateTime timestamp, Boolean content) {
    super(timestamp);
    this.content = content;
  }

  public Boolean getContent() {
    return content;
  }

  public String printContent() {
    return content ? "true" : "false";
  }
}
