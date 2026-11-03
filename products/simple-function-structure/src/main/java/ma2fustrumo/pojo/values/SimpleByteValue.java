package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleByteValue extends SimpleValue {
  Byte content;

  public SimpleByteValue(LocalDateTime timestamp, Byte content) {
    super(timestamp);
    this.content = content;
  }

  public Byte getContent() {
    return content;
  }

  public String printContent() {
    return content.toString();
  }
}
