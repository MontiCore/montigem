package ma2fustrumo.pojo;

public class SimpleChannel {

  protected static long nextId = 0;

  protected final long id = nextId++; //not thread safe, but probably good enough for our use case
  protected SimpleCategory category;
  protected SimpleFunctionStream functionStream;

  public SimpleChannel(SimpleCategory category, SimpleFunctionStream functionStream) {
    this.category = category;
    this.functionStream = functionStream;
  }

  public long getId() {
    return id;
  }

  public SimpleCategory getCategory() {
    return category;
  }

  public SimpleFunctionStream getFunctionStream() {
    return functionStream;
  }
}
