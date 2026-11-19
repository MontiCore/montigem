package chatmodule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AssistantInstructionsTest {
  @TempDir
  Path directory;

  @Test
  void composesArbitraryJarResourcesInManifestOrderWithCommentsAndUtf8() throws Exception {
    try (URLClassLoader resources = jarResources(Map.of(
        "instructions.list", "# Instructions\n\n persona.md \n\tskills/second.md\n# Another skill\nskills/first.md\n",
        "persona.md", "  Explore the dashboard.\n",
        "skills/first.md", "First skill: Größe",
        "skills/second.md", "Second skill: 日本語\n"))) {
      assertEquals("Explore the dashboard.\n\nSecond skill: 日本語\n\nFirst skill: Größe",
          AssistantInstructions.load(resources, "instructions.list"));
    }
  }

  @Test
  void reportsTheMissingManifest() throws Exception {
    try (URLClassLoader resources = jarResources(Map.of())) {
      IllegalStateException error = assertThrows(IllegalStateException.class,
          () -> AssistantInstructions.load(resources, "missing.list"));
      assertEquals("Missing assistant instruction resource: missing.list", error.getMessage());
    }
  }

  @Test
  void reportsAMissingSkillInsteadOfUsingPartialInstructions() throws Exception {
    try (URLClassLoader resources = jarResources(Map.of(
        "instructions.list", "persona.md\nskills/missing.md\n",
        "persona.md", "Persona"))) {
      IllegalStateException error = assertThrows(IllegalStateException.class,
          () -> AssistantInstructions.load(resources, "instructions.list"));
      assertEquals("Missing assistant instruction resource: skills/missing.md", error.getMessage());
    }
  }

  @Test
  void rejectsEmptyResourcesAndManifestsWithoutInstructions() throws Exception {
    try (URLClassLoader resources = jarResources(Map.of(
        "comments.list", "# No instructions\n\n",
        "empty.list", "\n \t",
        "instructions.list", "empty.md\n",
        "empty.md", "  \n"))) {
      assertEquals("No assistant instructions listed in: comments.list", assertThrows(IllegalStateException.class,
          () -> AssistantInstructions.load(resources, "comments.list")).getMessage());
      assertEquals("Empty assistant instruction resource: empty.list", assertThrows(IllegalStateException.class,
          () -> AssistantInstructions.load(resources, "empty.list")).getMessage());
      assertEquals("Empty assistant instruction resource: empty.md", assertThrows(IllegalStateException.class,
          () -> AssistantInstructions.load(resources, "instructions.list")).getMessage());
    }
  }

  @Test
  void reportsReadFailuresWithTheResourceNameAndCause() {
    IOException failure = new IOException("Cannot read resource");
    ClassLoader resources = new ClassLoader(null) {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            throw failure;
          }
        };
      }
    };
    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> AssistantInstructions.load(resources, "instructions.list"));
    assertEquals("Cannot read assistant instruction resource: instructions.list", error.getMessage());
    assertSame(failure, error.getCause());
  }

  private URLClassLoader jarResources(Map<String, String> entries) throws IOException {
    Path jar = directory.resolve("instructions.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      for (var entry : entries.entrySet()) {
        out.putNextEntry(new JarEntry(entry.getKey()));
        out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }
    }
    return new URLClassLoader(new URL[]{jar.toUri().toURL()}, null);
  }
}
