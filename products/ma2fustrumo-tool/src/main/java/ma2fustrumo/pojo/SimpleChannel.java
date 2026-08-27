package ma2fustrumo.pojo;

public class SimpleChannel {
  protected SimpleCategory category;
  protected SimpleFunctionStream functionStream;

  public SimpleChannel(SimpleCategory category, SimpleFunctionStream functionStream) {
    this.category = category;
    this.functionStream = functionStream;
  }

  public SimpleCategory getCategory() {
    return category;
  }

  public SimpleFunctionStream getFunctionStream() {
    return functionStream;
  }
}
