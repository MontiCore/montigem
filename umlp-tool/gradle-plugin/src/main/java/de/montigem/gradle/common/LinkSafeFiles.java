/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Deleting that never follows links. commons-io and Files.walk follow Windows junctions,
 * and build/app contains junctions into the sources.
 */
public final class LinkSafeFiles {

  private LinkSafeFiles() {
  }

  /**
   * Deletes everything inside {@code dir}. Links and junctions are removed, their targets stay untouched.
   * If {@code dir} itself is a link, only the link is removed.
   */
  public static void deleteContents(Path dir) throws IOException {
    if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (isLink(Files.readAttributes(dir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))) {
      Files.delete(dir);
      return;
    }
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
        if (!d.equals(dir) && isLink(attrs)) {
          // removes the junction itself, not the folder it points to
          Files.delete(d);
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        if (!d.equals(dir)) {
          Files.delete(d);
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static boolean isLink(BasicFileAttributes attrs) {
    // on Windows a junction is a directory with isOther() set
    return attrs.isSymbolicLink() || attrs.isOther();
  }
}
