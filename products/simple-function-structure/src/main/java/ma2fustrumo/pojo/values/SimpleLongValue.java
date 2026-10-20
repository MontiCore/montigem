package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleLongValue extends SimpleValue {
  Long content;

  public SimpleLongValue(LocalDateTime timestamp, Long content) {
    super(timestamp);
    this.content = content;
  }

  public Long getContent() {
    return content;
  }

  public String printContent() {
    return content.toString() + "l";
  }
}
