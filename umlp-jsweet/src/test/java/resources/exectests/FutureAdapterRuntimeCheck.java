package resources.exectests;

import util.JSweetTest;

import java.util.concurrent.CompletableFuture;

/**
 * see examples/jsweet-test's browser-driven FutureAdapterTestE2ETest.java
 */
public class FutureAdapterRuntimeCheck {

  private CompletableFuture<String> futureAttribute;

  @JSweetTest
  public static String completedFuture() {
    return "completedFuture: " + CompletableFuture.completedFuture("hello").join();
  }

  @JSweetTest
  public static String supplyAsync() {
    return "supplyAsync: " + CompletableFuture.supplyAsync(() -> "hello" + "Async").join();
  }

  @JSweetTest
  public static String runAsync() {
    CompletableFuture.runAsync(() -> {
      // side effect
    }).join();
    return "runAsync: hello";
  }

  @JSweetTest
  public static String failedFuture() {
    String res = CompletableFuture.<String>failedFuture(new RuntimeException("failed"))
        .exceptionally(ex -> "caught")
        .join();
    return "failedFuture: " + res;
  }

  @JSweetTest
  public static String thenApply() {
    String res = CompletableFuture.completedFuture("hello")
        .thenApply(r -> r + "Applied")
        .join();
    return "thenApply: " + res;
  }

  @JSweetTest
  public static String thenApplyAsync() {
    String res = CompletableFuture.completedFuture("hello")
        .thenApplyAsync(r -> r + "AppliedAsync")
        .join();
    return "thenApplyAsync: " + res;
  }

  @JSweetTest
  public static String thenAccept() {
    StringBuilder sb = new StringBuilder();
    CompletableFuture.completedFuture("hello")
        .thenAccept(r -> sb.append(r))
        .join();
    return "thenAccept: " + sb;
  }

  @JSweetTest
  public static String thenAcceptAsync() {
    StringBuilder sb = new StringBuilder();
    CompletableFuture.completedFuture("hello")
        .thenAcceptAsync(r -> sb.append(r))
        .join();
    return "thenAcceptAsync: " + sb;
  }

  @JSweetTest
  public static String thenRun() {
    CompletableFuture.completedFuture("hello")
        .thenRun(() -> {
        })
        .join();
    return "thenRun: hello";
  }

  @JSweetTest
  public static String thenRunAsync() {
    CompletableFuture.completedFuture("hello")
        .thenRunAsync(() -> {
        })
        .join();
    return "thenRunAsync: hello";
  }

  @JSweetTest
  public static String thenCompose() {
    String res = CompletableFuture.completedFuture("hello")
        .thenCompose(r -> CompletableFuture.completedFuture(r + "Composed"))
        .join();
    return "thenCompose: " + res;
  }

  @JSweetTest
  public static String thenComposeAsync() {
    String res = CompletableFuture.completedFuture("hello")
        .thenComposeAsync(r -> CompletableFuture.completedFuture(r + "ComposedAsync"))
        .join();
    return "thenComposeAsync: " + res;
  }

  @JSweetTest
  public static String exceptionally() {
    String res = CompletableFuture.<String>failedFuture(new RuntimeException("error"))
        .exceptionally(ex -> "hello" + "Exception")
        .join();
    return "exceptionally: " + res;
  }

  @JSweetTest
  public static String whenComplete() {
    StringBuilder sb = new StringBuilder();
    CompletableFuture.completedFuture("hello")
        .whenComplete((res, ex) -> {
          if (ex == null) {
            sb.append(res);
          }
        }).join();
    return "whenComplete: " + sb;
  }

  @JSweetTest
  public static String whenCompleteAsync() {
    StringBuilder sb = new StringBuilder();
    CompletableFuture.completedFuture("hello")
        .whenCompleteAsync((res, ex) -> {
          if (ex == null) {
            sb.append(res);
          }
        }).join();
    return "whenCompleteAsync: " + sb;
  }

  @JSweetTest
  public static String handle() {
    String res = CompletableFuture.<String>failedFuture(new RuntimeException("error"))
        .handle((r, ex) -> ex != null ? "hello" + "Handled" : r)
        .join();
    return "handle: " + res;
  }

  @JSweetTest
  public static String handleAsync() {
    String res = CompletableFuture.<String>failedFuture(new RuntimeException("error"))
        .handleAsync((r, ex) -> ex != null ? "hello" + "HandledAsync" : r)
        .join();
    return "handleAsync: " + res;
  }

  @JSweetTest
  public static String join() {
    return "join: " + CompletableFuture.completedFuture("hello").join();
  }

  @JSweetTest
  public static String get() {
    try {
      return "get: " + CompletableFuture.completedFuture("hello").get();
    } catch (Exception e) {
      return "get: failed";
    }
  }

  @JSweetTest
  public static String futureAttribute() {
    FutureAdapterRuntimeCheck instance = new FutureAdapterRuntimeCheck();
    instance.futureAttribute = CompletableFuture.completedFuture("hello" + "Attr");
    return "futureAttribute: " + instance.futureAttribute.join();
  }

  @JSweetTest
  public static String allOf() {
    CompletableFuture<String> f1 = CompletableFuture.completedFuture("hello1");
    CompletableFuture<String> f2 = CompletableFuture.completedFuture("hello2");
    CompletableFuture.allOf(f1, f2).join();
    return "allOf: done";
  }

  @JSweetTest
  public static String anyOf() {
    CompletableFuture<String> f1 = CompletableFuture.completedFuture("helloAny");
    Object res = CompletableFuture.anyOf(f1).join();
    return "anyOf: " + res;
  }
}
