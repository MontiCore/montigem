package observer;

import umlp.jsweet.extension.annotation.ObserverRegistrationParams;

public class ObserverParamsHierarchy {

  public static class Base {
    public String label() {
      return "base";
    }

    public String other() {
      return "other";
    }
  }

  public static class Middle extends Base {
  }

  public static class Sub extends Middle {
    @Override
    @ObserverRegistrationParams
    public String label() {
      return "sub";
    }
  }

  public static class Sibling extends Base {
    @Override
    public String label() {
      return "sibling";
    }

    @Override
    public String toString() {
      return "sibling";
    }
  }

  public static class Caller {
    public String describe(Object o) {
      return o.toString();
    }
  }
}
