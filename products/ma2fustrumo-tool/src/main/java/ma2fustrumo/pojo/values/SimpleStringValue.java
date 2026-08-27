package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleStringValue extends SimpleValue {
  String content;

  public SimpleStringValue(LocalDateTime timestamp, String content) {
    super(timestamp);
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String printContent() {
    return "\"" + content + "\"";
  }
}
