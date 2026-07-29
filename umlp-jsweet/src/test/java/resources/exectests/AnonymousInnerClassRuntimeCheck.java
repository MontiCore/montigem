package resources.exectests;

import util.JSweetTest;

import java.util.function.Consumer;

/**
 * see examples/jsweet-test TestAnonymousInnerClassE2ETest.java
 */
public class AnonymousInnerClassRuntimeCheck {

  public int x = 10;

  // covers InnerAnonymousClass: new HelperClass($ -> {}) { @Override public void myNotification() {...} }
  @JSweetTest
  public static String anonymousOverrideInvokesConstructorCapturedConsumer() {
    final String[] captured = new String[]{"not-called"};
    HelperClass helper = new HelperClass($ -> captured[0] = "consumer-invoked") {
      @Override
      public void myNotification() {
        this.whatever.accept(null);
      }
    };
    helper.myNotification();
    return "anonymous: " + captured[0];
  }

  @JSweetTest
  public static String anonymousCapturesLocal() {
    final String localValue = "captured-local-42";
    final String[] captured = new String[1];
    Consumer<String> anon = new Consumer<String>() {
      @Override
      public void accept(String prefix) {
        captured[0] = prefix + localValue;
      }
    };
    anon.accept("anonymousConsumer: ");
    return captured[0];
  }

  @JSweetTest
  public static String innerClassOuterFieldAccess() {
    return new AnonymousInnerClassRuntimeCheck().runInnerClassOuterFieldAccess();
  }

  @JSweetTest
  public static String innerClassCtorInt() {
    return new AnonymousInnerClassRuntimeCheck().runInnerClassCtorInt();
  }

  @JSweetTest
  public static String innerClassCtorString() {
    return new AnonymousInnerClassRuntimeCheck().runInnerClassCtorString();
  }

  // covers InnerAnonymousClass.NastyInnerClass reading the enclosing instance's field. "new
  // NastyInnerClass()" here (implicit this as the enclosing instance, called from an instance
  // method) is the exact pattern already proven to transpile in
  // src/test/resources/java/innerclasses/InnerAnonymousClass.java -- the qualified
  // "outer.new NastyInnerClass()" form is not exercised anywhere else in this module, so this
  // avoids relying on untested adapter behavior; the static entry point below just delegates.
  private String runInnerClassOuterFieldAccess() {
    NastyInnerClass inner = new NastyInnerClass();
    return inner.doSomething();
  }

  // covers InnerAnonymousClass.NastyInnerClassWithConstructor(int) / (String)
  private String runInnerClassCtorInt() {
    NastyInnerClassWithConstructor inner = new NastyInnerClassWithConstructor(7);
    return "ctor: " + inner.getMyX();
  }

  private String runInnerClassCtorString() {
    NastyInnerClassWithConstructor inner = new NastyInnerClassWithConstructor("seven");
    return "ctor: " + inner.getMyX();
  }

  static class HelperClass {
    Consumer<Void> whatever;

    HelperClass(Consumer<Void> whatever) {
      this.whatever = whatever;
    }

    public void myNotification() {
    }
  }

  public class NastyInnerClass {
    public String doSomething() {
      return "NastyInnerClass: x=" + x;
    }
  }

  public class NastyInnerClassWithConstructor {
    String myX;

    public NastyInnerClassWithConstructor(int x) {
      myX = "int:" + x;
    }

    public NastyInnerClassWithConstructor(String x) {
      myX = "string:" + x;
    }

    public String getMyX() {
      return myX;
    }
  }
}
