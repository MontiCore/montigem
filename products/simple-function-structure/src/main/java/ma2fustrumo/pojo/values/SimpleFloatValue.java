package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleFloatValue extends SimpleValue {
  Float content;

  public SimpleFloatValue(LocalDateTime timestamp, Float content) {
    super(timestamp);
    this.content = content;
  }

  public Float getContent() {
    return content;
  }

  public String printContent() {
    return content.toString() + "f";
  }
}
