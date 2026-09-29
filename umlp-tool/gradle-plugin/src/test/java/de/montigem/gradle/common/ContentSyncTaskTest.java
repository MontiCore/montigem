/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSyncTaskTest {

  @TempDir
  Path tmp;

  private File source(String name, String content) throws IOException {
    Path p = tmp.resolve("src").resolve(name);
    Files.createDirectories(p.getParent());
    Files.writeString(p, content);
    return p.toFile();
  }

  @Test
  void copiesNewFilesAndKeepsUnchangedFiles() throws IOException {
    Path into = tmp.resolve("into");
    Map<String, File> files = new LinkedHashMap<>();
    files.put("a.ts", source("a.ts", "a"));
    files.put("sub/b.ts", source("sub/b.ts", "b"));
    ContentSyncTask.syncByContent(files, into);
    assertEquals("a", Files.readString(into.resolve("a.ts")));
    assertEquals("b", Files.readString(into.resolve("sub/b.ts")));

    FileTime old = FileTime.fromMillis(1_000_000_000_000L);
    Files.setLastModifiedTime(into.resolve("a.ts"), old);
    Files.setLastModifiedTime(into.resolve("sub/b.ts"), old);
    files.put("sub/b.ts", source("sub/b.ts", "b2"));
    ContentSyncTask.syncByContent(files, into);

    assertEquals(old, Files.getLastModifiedTime(into.resolve("a.ts")));
    assertEquals("b2", Files.readString(into.resolve("sub/b.ts")));
  }

  @Test
  void deletesFilesAndEmptyFoldersThatAreGone() throws IOException {
    Path into = tmp.resolve("into");
    Map<String, File> files = new LinkedHashMap<>();
    files.put("a.ts", source("a.ts", "a"));
    files.put("sub/b.ts", source("sub/b.ts", "b"));
    ContentSyncTask.syncByContent(files, into);

    files.remove("sub/b.ts");
    ContentSyncTask.syncByContent(files, into);

    assertTrue(Files.exists(into.resolve("a.ts")));
    assertFalse(Files.exists(into.resolve("sub")));
  }

  @Test
  void laterSourceWins() throws IOException {
    Path into = tmp.resolve("into");
    Map<String, File> files = new LinkedHashMap<>();
    files.put("a.ts", source("first/a.ts", "first"));
    files.put("a.ts", source("second/a.ts", "second"));
    ContentSyncTask.syncByContent(files, into);
    assertEquals("second", Files.readString(into.resolve("a.ts")));
  }
}
