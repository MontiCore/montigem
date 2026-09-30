/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolClasspathCopiesTest {

  @TempDir
  Path tmp;

  private Path jar(Path path, String resource, String content) throws IOException {
    Files.createDirectories(path.getParent());
    try (OutputStream out = Files.newOutputStream(path); JarOutputStream jar = new JarOutputStream(out)) {
      jar.putNextEntry(new JarEntry(resource));
      jar.write(content.getBytes());
      jar.closeEntry();
    }
    return path;
  }

  @Test
  void keepsJarsOfTheGradleCacheAndFolders() throws IOException {
    Path cached = jar(tmp.resolve("caches/modules-2/files-2.1/g/a/1/abc/a-1.jar"), "a.txt", "a");
    Path folder = Files.createDirectories(tmp.resolve("classes"));

    List<File> result = ToolClasspathCopies.of(Set.of(cached.toFile()));
    assertEquals(List.of(cached.toFile()), result);
    assertEquals(List.of(folder.toFile()), ToolClasspathCopies.of(Set.of(folder.toFile())));
  }

  @Test
  void aRebuiltJarGetsANewCopyAndTheOriginalStaysWritable() throws Exception {
    Path built = jar(tmp.resolve("umlp-tool/build/libs/umlp-tool.jar"), "version.txt", "old");
    File first = ToolClasspathCopies.of(Set.of(built.toFile())).get(0);
    assertNotEquals(built.toFile(), first);
    assertEquals(first, ToolClasspathCopies.of(Set.of(built.toFile())).get(0));

    try (URLClassLoader warm = new URLClassLoader(new URL[] {first.toURI().toURL()}, null)) {
      assertNotNull(warm.getResource("version.txt"));
      // the build rebuilds the jar while a warm class loader is open
      jar(built, "version.txt", "new content");
      Files.setLastModifiedTime(built, FileTime.from(Instant.now().plusSeconds(5)));

      File second = ToolClasspathCopies.of(Set.of(built.toFile())).get(0);
      assertNotEquals(first, second);
      try (URLClassLoader fresh = new URLClassLoader(new URL[] {second.toURI().toURL()}, null);
           var in = fresh.getResourceAsStream("version.txt")) {
        assertEquals("new content", new String(in.readAllBytes()));
      }
    }
  }
}
