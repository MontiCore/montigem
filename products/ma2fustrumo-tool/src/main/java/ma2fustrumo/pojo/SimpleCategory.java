package ma2fustrumo.pojo;

import java.util.Optional;

public class SimpleCategory {
  protected Optional<Boolean> continuous;
  protected SimpleKind kind;
  protected SimpleType type;

  public SimpleCategory(Optional<Boolean> continuous, SimpleKind kind, SimpleType type) {
    this.continuous = continuous;
    this.kind = kind;
    this.type = type;
  }

  public Optional<Boolean> getContinuous() {
    return continuous;
  }

  public void setContinuous(Optional<Boolean> continuous) {
    this.continuous = continuous;
  }

  public SimpleKind getKind() {
    return kind;
  }

  public void setKind(SimpleKind kind) {
    this.kind = kind;
  }

  public SimpleType getType() {
    return type;
  }

  public void setType(SimpleType type) {
    this.type = type;
  }
}
