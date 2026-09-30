/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Java2TSActionTest {

  @TempDir
  Path tmp;

  private File transpilerJar(String name, String optionsContent) throws IOException {
    Path jar = tmp.resolve(name);
    try (OutputStream out = Files.newOutputStream(jar); JarOutputStream j = new JarOutputStream(out)) {
      j.putNextEntry(new JarEntry("org/jsweet/transpiler/JSweetOptions.class"));
      j.write(optionsContent.getBytes(StandardCharsets.ISO_8859_1));
      j.closeEntry();
    }
    return jar.toFile();
  }

  @Test
  void warmLoaderOnlyWithAJSweetThatLoadsAdaptersPerTranspiler() throws IOException {
    File current = transpilerJar("jsweet-transpiler-new.jar", "...getJavaCompilerExtraOptions...getAdapterClassLoader...");
    File older = transpilerJar("jsweet-transpiler-old.jar", "...getJavaCompilerExtraOptions...");
    File other = tmp.resolve("guava.jar").toFile();

    assertTrue(Java2TSAction.supportsWarmLoader(Set.of(other, current)));
    assertFalse(Java2TSAction.supportsWarmLoader(Set.of(other, older)));
    assertFalse(Java2TSAction.supportsWarmLoader(Set.of(other)));
  }
}
