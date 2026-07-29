package resources.exectests;

import util.JSweetTest;

import java.util.Optional;

/**
 * see examples/jsweet-test's browser-driven OptionalAdapterTestE2ETest.java
 */
public class OptionalAdapterRuntimeCheck {

  @JSweetTest
  public static boolean isPresentWithValue() {
    return Optional.ofNullable("hello").isPresent();
  }

  @JSweetTest
  public static boolean isPresentWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.isPresent();
  }

  @JSweetTest
  public static boolean isEmptyWithValue() {
    return Optional.ofNullable("hello").isEmpty();
  }

  @JSweetTest
  public static boolean isEmptyWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.isEmpty();
  }

  @JSweetTest
  public static String getWithValue() {
    return Optional.of("hello").get();
  }

  @JSweetTest
  public static String orElseWithValue() {
    return Optional.ofNullable("hello").orElse("other");
  }

  @JSweetTest
  public static String orElseWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.orElse("other");
  }

  @JSweetTest
  public static String orElseGetWithValue() {
    return Optional.ofNullable("hello").orElseGet(() -> "otherGet");
  }

  @JSweetTest
  public static String orElseGetWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.orElseGet(() -> "otherGet");
  }

  //@JSweetTest
  public static String mapWithValue() {
    Optional<String> opt = Optional.ofNullable("hello");
    Optional<Integer> mapped = opt.map(s -> s.length());
    return mapped.isPresent() ? String.valueOf(mapped.get()) : "empty";
  }

  //@JSweetTest
  public static String mapWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    Optional<Integer> mapped = opt.map(s -> s.length());
    return mapped.isPresent() ? String.valueOf(mapped.get()) : "empty";
  }

  @JSweetTest
  public static String flatMapWithValue() {
    Optional<String> opt = Optional.ofNullable("hello");
    Optional<Integer> flatMapped = opt.flatMap(s -> Optional.of(s.length()));
    return flatMapped.isPresent() ? String.valueOf(flatMapped.get()) : "empty";
  }

  @JSweetTest
  public static String flatMapWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    Optional<Integer> flatMapped = opt.flatMap(s -> Optional.of(s.length()));
    return flatMapped.isPresent() ? String.valueOf(flatMapped.get()) : "empty";
  }

  @JSweetTest
  public static String filterPasses() {
    Optional<String> opt = Optional.ofNullable("hello");
    Optional<String> filtered = opt.filter(s -> s.length() > 3);
    return filtered.isPresent() ? filtered.get() : "empty";
  }

  @JSweetTest
  public static String filterRejects() {
    Optional<String> opt = Optional.ofNullable("abc");
    Optional<String> filtered = opt.filter(s -> s.length() > 3);
    return filtered.isPresent() ? filtered.get() : "empty";
  }

  @JSweetTest
  public static String ifPresentWithValue() {
    StringBuilder sb = new StringBuilder();
    Optional.ofNullable("hello").ifPresent(s -> sb.append("present: ").append(s));
    if (sb.length() == 0) {
      sb.append("not present");
    }
    return sb.toString();
  }

  @JSweetTest
  public static String ifPresentWithoutValue() {
    StringBuilder sb = new StringBuilder();
    Optional<String> opt = Optional.ofNullable(null);
    opt.ifPresent(s -> sb.append("present: ").append(s));
    if (sb.length() == 0) {
      sb.append("not present");
    }
    return sb.toString();
  }

  @JSweetTest
  public static String ifPresentOrElseWithValue() {
    StringBuilder sb = new StringBuilder();
    Optional.ofNullable("hello").ifPresentOrElse(
        s -> sb.append("present: ").append(s), () -> sb.append("else empty"));
    return sb.toString();
  }

  @JSweetTest
  public static String ifPresentOrElseWithoutValue() {
    StringBuilder sb = new StringBuilder();
    Optional<String> opt = Optional.ofNullable(null);
    opt.ifPresentOrElse(s -> sb.append("present: ").append(s), () -> sb.append("else empty"));
    return sb.toString();
  }

  @JSweetTest
  public static String orWithValue() {
    Optional<String> opt = Optional.ofNullable("hello");
    return opt.or(() -> Optional.of("or fallback")).get();
  }

  @JSweetTest
  public static String orWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.or(() -> Optional.of("or fallback")).get();
  }

  @JSweetTest
  public static long streamCountWithValue() {
    return Optional.ofNullable("hello").stream().count();
  }

  @JSweetTest
  public static long streamCountWithoutValue() {
    Optional<String> opt = Optional.ofNullable(null);
    return opt.stream().count();
  }
}
