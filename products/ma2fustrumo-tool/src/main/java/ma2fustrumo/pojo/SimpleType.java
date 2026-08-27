package ma2fustrumo.pojo;

public class SimpleType {
  protected String mctype;
  protected boolean isEnum;

  public SimpleType(String mctype, boolean isEnum) {
    this.mctype = mctype;
    this.isEnum = isEnum;
  }

  public String getMctype() {
    return mctype;
  }

  public void setMctype(String mctype) {
    this.mctype = mctype;
  }

  public boolean isEnum() {
    return isEnum;
  }

  public void setEnum(boolean anEnum) {
    isEnum = anEnum;
  }
}
