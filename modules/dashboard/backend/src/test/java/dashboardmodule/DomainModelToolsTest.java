package dashboardmodule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelToolsTest {
  @TempDir
  Path directory;

  @Test
  void readsUtf8ModelFromJarAndReportsMissingOrEmptyResources() throws Exception {
    String model = "umlp Inventory { class Item { String größe; } }";
    Path jar = directory.resolve("models.jar");
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry("models/Inventory.umlp"));
      output.write(model.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
      output.putNextEntry(new JarEntry("models/Empty.umlp"));
      output.closeEntry();
    }
    try (URLClassLoader resources = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
      assertEquals(model, new DomainModelTools(resources, "models/Inventory.umlp").getDomainModel());
      assertEquals("Error: missing UMLP class diagram resource: missing.umlp",
          new DomainModelTools(resources, "missing.umlp").getDomainModel());
      assertEquals("Error: empty UMLP class diagram resource: models/Empty.umlp",
          new DomainModelTools(resources, "models/Empty.umlp").getDomainModel());
    }
  }
}
