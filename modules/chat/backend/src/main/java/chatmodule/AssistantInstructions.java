package chatmodule;

import jsweet.lang.Erased;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Composes UTF-8 instruction resources in manifest order, including when packaged in a JAR. */
@Erased
public final class AssistantInstructions {
  private AssistantInstructions() {}

  /** Manifest entries are classpath resource names, one per line; blank lines and # comments are ignored. */
  public static String load(ClassLoader resources, String manifest) {
    List<String> instructions = new ArrayList<>();
    for (String line : read(resources, manifest).lines().toList()) {
      String resource = line.strip();
      if (!resource.isEmpty() && !resource.startsWith("#")) {
        instructions.add(read(resources, resource));
      }
    }
    if (instructions.isEmpty()) {
      throw new IllegalStateException("No assistant instructions listed in: " + manifest);
    }
    return String.join("\n\n", instructions);
  }

  private static String read(ClassLoader resources, String resource) {
    try (InputStream stream = resources.getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IllegalStateException("Missing assistant instruction resource: " + resource);
      }
      String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
      if (text.isEmpty()) {
        throw new IllegalStateException("Empty assistant instruction resource: " + resource);
      }
      return text;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read assistant instruction resource: " + resource, e);
    }
  }
}
