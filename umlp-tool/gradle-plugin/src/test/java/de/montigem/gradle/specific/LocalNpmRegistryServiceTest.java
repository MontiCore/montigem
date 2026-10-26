/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalNpmRegistryServiceTest {

  private static String sharedConfig() throws IOException {
    try (var in = LocalNpmRegistryServiceTest.class.getResourceAsStream("/localNpmRepo/config.yaml")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void sharedConfigIsAdaptedToTheHost() throws IOException {
    String config = LocalNpmRegistryService.renderConfig(sharedConfig(), Path.of("/tmp/npmLocal"));

    assertTrue(config.contains("storage: /tmp/npmLocal\n"));
    assertTrue(config.contains("file: /tmp/npmLocal/htpasswd\n"));
    assertTrue(config.contains("listen: 127.0.0.1:4873\n"));
    assertFalse(config.contains("/verdaccio"));
  }

  @Test
  void umlpAndMontigemPackagesAreNotProxied() throws IOException {
    String config = sharedConfig();

    assertTrue(config.contains("'@umlp/*':\n    access: $all\n    publish: $all\n    unpublish: $all\n\n  '@montigem/*':"));
    assertTrue(config.contains("'@montigem/*':\n    access: $all\n    publish: $all\n    unpublish: $all\n\n  '@*/*':"));
  }
}
