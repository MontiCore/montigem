package util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Invokes a static, no-arg method on an already-compiled test fixture class (e.g.
 * src/test/java/runtime/CollectionsSingletonRuntimeCheck.java), so its actual runtime output can
 * be compared against the transpiled/compiled TypeScript's output -- see
 * {@link RealTscCheck.Result#resultValue()}, which formats the TypeScript side the same way.
 *
 * <p>Fixture classes live under src/test/java (not src/test/resources), so Gradle compiles them
 * normally as part of the test sourceSet -- no in-memory compilation needed here. They typically
 * aren't referenced anywhere else in test code; {@link RealTscCheck} separately feeds their
 * .java source text to the JSweet transpiler by path.
 */
public final class JavaFixtureRunner {

  // Return types this can format comparably with JS's JSON.stringify() (used by
  // RealTscCheck#staticMethodRunner on the TypeScript side): boolean, the numeric types (where
  // Java's toString() already agrees with JSON byte-for-byte) and String (JSON-quoted below).
  // Notably excludes char (Java "a" vs JSON "\"a\"") -- anything else (objects, arrays,
  // collections) would need real JSON serialization here to stay comparable.
  private static final Set<Class<?>> SUPPORTED_RETURN_TYPES = Set.of(
      boolean.class, Boolean.class,
      byte.class, Byte.class,
      short.class, Short.class,
      int.class, Integer.class,
      long.class, Long.class,
      float.class, Float.class,
      double.class, Double.class,
      String.class);

  private JavaFixtureRunner() {
  }

  /**
   * Only the return types in {@link #SUPPORTED_RETURN_TYPES} are supported. Throws
   * {@link IllegalArgumentException} for anything else.
   */
  public static String runStaticMethod(Class<?> fixtureClass, String methodName) throws ReflectiveOperationException {
    return formatResult(fixtureClass, methodName);
  }

  /**
   * Like {@link #runStaticMethod}, but for many methods on the same fixture class, matching
   * {@link RealTscCheck#multiMethodRunner} on the TypeScript side. Order is preserved; a thrown
   * method fails the whole call rather than being silently omitted.
   */
  public static LinkedHashMap<String, String> runExecTestMethods(Class<?> fixtureClass)
      throws ReflectiveOperationException {
    List<String> methodNames = new ArrayList<>();
    for (Method declaredMethod : fixtureClass.getDeclaredMethods()) {
      if(declaredMethod.getAnnotation(JSweetTest.class) != null) {
        methodNames.add(declaredMethod.getName());
      }
    }
    LinkedHashMap<String, String> results = new LinkedHashMap<>();
    for (String methodName : methodNames) {
      results.put(methodName, formatResult(fixtureClass, methodName));
    }
    return results;
  }

  private static String formatResult(Class<?> fixtureClass, String methodName) throws ReflectiveOperationException {
    Method method = fixtureClass.getMethod(methodName);
    if (!SUPPORTED_RETURN_TYPES.contains(method.getReturnType())) {
      throw new IllegalArgumentException(
          "Only supports boolean/numeric/String return types (Java's formatting must match JS "
              + "JSON.stringify() byte-for-byte); " + fixtureClass.getName() + "." + methodName
              + "() returns " + method.getReturnType().getName());
    }
    Object result;
    try {
      result = method.invoke(null);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(
          "Fixture method " + fixtureClass.getName() + "." + methodName + "() threw", e.getCause());
    }
    if (result instanceof String s) {
      return jsonQuote(s);
    }
    return String.valueOf(result);
  }

  /** Minimal JSON string quoting/escaping, matching JS's JSON.stringify() for a plain string. */
  private static String jsonQuote(String s) {
    StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
