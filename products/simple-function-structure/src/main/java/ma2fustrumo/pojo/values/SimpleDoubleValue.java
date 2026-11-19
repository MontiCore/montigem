package ma2fustrumo.pojo.values;

import java.time.LocalDateTime;

public class SimpleDoubleValue extends SimpleValue {
  Double content;

  public SimpleDoubleValue(LocalDateTime timestamp, Double content) {
    super(timestamp);
    this.content = content;
  }

  public Double getContent() {
    return content;
  }

  public String printContent() {
    return content.toString() + "d";
  }
}
