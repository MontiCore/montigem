package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleShortValue extends SimpleValue {
  Short content;

  public SimpleShortValue(LocalDateTime timestamp, Short content) {
    super(timestamp);
    this.content = content;
  }

  public Short getContent() {
    return content;
  }

  public String printContent() {
    return content.toString();
  }
}
