package aas.utils;

public enum AASFormat {
  AASX("aasx"),
  JSON("json"),
  XML("xml");

  private final String value;

  AASFormat(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
