/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSweetSettingsTest {

  @Test
  void settingsSurviveTheWayIntoTheJSweetClassLoader() {
    JSweetSettings s = new JSweetSettings();
    s.base = new File("build/gui2jsweetAdapter");
    s.working = new File("build/internal_jsweet/gui2java2ts");
    s.out = new File("build/gui2java2ts");
    s.candies = new File("build/internal_jsweet/guiCandies");
    s.classPath = "a.jar" + File.pathSeparator + "dir with space/b.jar";
    s.nodeVersion = "24.15.0";
    s.verbose = true;
    s.sourceMaps = false;
    s.input.addAll(List.of(new File("src/A.java"), new File("src/B.java")));
    s.excluded.add(new File("src/C.java"));

    JSweetSettings read = JSweetSettings.parse(s.toArgs());

    assertEquals(s.base, read.base);
    assertEquals(s.working, read.working);
    assertEquals(s.out, read.out);
    assertEquals(s.candies, read.candies);
    assertEquals(s.classPath, read.classPath);
    assertEquals(s.nodeVersion, read.nodeVersion);
    assertTrue(read.verbose);
    assertEquals(s.sourceMaps, read.sourceMaps);
    assertEquals(s.input, read.input);
    assertEquals(s.excluded, read.excluded);
    assertNull(read.outputList);

    s.outputList = new File("build/internal_jsweet/umlpJava2ts/outputs.txt");
    assertEquals(s.outputList, JSweetSettings.parse(s.toArgs()).outputList);
    assertTrue(read.autoPropagateAsync);

    s.autoPropagateAsync = false;
    assertEquals(false, JSweetSettings.parse(s.toArgs()).autoPropagateAsync);
  }

  @Test
  void rejectsUnknownOrIncompleteArguments() {
    assertThrows(IllegalArgumentException.class, () -> JSweetSettings.parse(new String[] {"--unknown", "x"}));
    assertThrows(IllegalArgumentException.class, () -> JSweetSettings.parse(new String[] {"--base"}));
  }
}
