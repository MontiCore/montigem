/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkSafeFilesTest {

  @TempDir
  Path tmp;

  /** A junction on Windows, a symbolic link elsewhere. */
  private void link(Path link, Path target) throws IOException, InterruptedException {
    if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
      Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
          .redirectErrorStream(true).start();
      p.getInputStream().readAllBytes();
      assertEquals(0, p.waitFor());
    } else {
      Files.createSymbolicLink(link, target);
    }
  }

  @Test
  void deletesContentButNotTheLinkTarget() throws Exception {
    Path sources = Files.createDirectories(tmp.resolve("sources"));
    Files.writeString(sources.resolve("Model.gui"), "keep");
    Path out = Files.createDirectories(tmp.resolve("out/sub"));
    Files.writeString(out.resolve("a.ts"), "a");
    link(tmp.resolve("out/hwc"), sources);

    LinkSafeFiles.deleteContents(tmp.resolve("out"));

    assertTrue(Files.isDirectory(tmp.resolve("out")));
    try (var children = Files.list(tmp.resolve("out"))) {
      assertEquals(0, children.count());
    }
    assertEquals("keep", Files.readString(sources.resolve("Model.gui")));
  }

  @Test
  void removesOnlyTheLinkIfTheFolderItselfIsALink() throws Exception {
    Path sources = Files.createDirectories(tmp.resolve("sources"));
    Files.writeString(sources.resolve("Model.gui"), "keep");
    link(tmp.resolve("out"), sources);

    LinkSafeFiles.deleteContents(tmp.resolve("out"));

    assertFalse(Files.exists(tmp.resolve("out")));
    assertEquals("keep", Files.readString(sources.resolve("Model.gui")));
  }

  @Test
  void ignoresAMissingFolder() throws IOException {
    LinkSafeFiles.deleteContents(tmp.resolve("missing"));
    assertFalse(Files.exists(tmp.resolve("missing")));
  }

  @Test
  void keepsTheListedFilesAndTheirFolders() throws Exception {
    Path sources = Files.createDirectories(tmp.resolve("sources"));
    Files.writeString(sources.resolve("Model.gui"), "keep");
    Path out = tmp.resolve("out");
    Path kept = out.resolve("a/Kept.ts");
    Files.createDirectories(kept.getParent());
    Files.writeString(kept, "kept");
    Files.writeString(out.resolve("a/Removed.ts"), "stale");
    Files.createDirectories(out.resolve("b"));
    Files.writeString(out.resolve("b/Removed.ts"), "stale");
    link(out.resolve("hwc"), sources);

    LinkSafeFiles.deleteContentsExcept(out, Set.of(kept.toAbsolutePath().normalize()));

    assertEquals("kept", Files.readString(kept));
    assertFalse(Files.exists(out.resolve("a/Removed.ts")));
    assertFalse(Files.exists(out.resolve("b")));
    assertFalse(Files.exists(out.resolve("hwc")));
    assertEquals("keep", Files.readString(sources.resolve("Model.gui")));
  }
}
