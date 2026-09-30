/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Tool class loaders stay open between builds. A class loader keeps its jars open and reads a jar that was
 * rebuilt in place wrongly, e.g. umlp-tool in the umlp repository. So jars outside the Gradle dependency cache
 * are loaded from a copy whose path depends on the jar's size and time stamp. A rebuilt jar then gets a new
 * class loader, and the build can overwrite the original.
 */
public final class ToolClasspathCopies {

  private static final String GRADLE_DEPENDENCY_CACHE = "/caches/modules-2/files-2.1/";
  // one folder per user, on Linux all users share the temp folder
  private static final Path ROOT = Path.of(System.getProperty("java.io.tmpdir"),
      "umlp-tool-classpath-" + System.getProperty("user.name", "user").replaceAll("[^A-Za-z0-9._-]", "_"));
  private static final Duration KEEP = Duration.ofDays(7);

  private ToolClasspathCopies() {
  }

  public static List<File> of(Set<File> classpath) {
    return classpath.stream().map(f -> needsCopy(f) ? copyOf(f) : f).toList();
  }

  static boolean needsCopy(File file) {
    return file.isFile() && !file.getAbsolutePath().replace(File.separatorChar, '/').contains(GRADLE_DEPENDENCY_CACHE);
  }

  static File copyOf(File jar) {
    Path dir = ROOT.resolve(key(jar));
    Path target = dir.resolve(jar.getName());
    try {
      if (!Files.isRegularFile(target) || Files.size(target) != jar.length()) {
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, jar.getName(), ".tmp");
        Files.copy(jar.toPath(), tmp, StandardCopyOption.REPLACE_EXISTING);
        try {
          Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException | AccessDeniedException e) {
          // a parallel task was faster, its copy has the same content
          Files.deleteIfExists(tmp);
          if (!Files.isRegularFile(target)) {
            throw e;
          }
        }
        deleteOldCopies(dir);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot copy the tool class path entry " + jar, e);
    }
    return target.toFile();
  }

  static String key(File jar) {
    String id = jar.getAbsolutePath() + '|' + jar.length() + '|' + jar.lastModified();
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void deleteOldCopies(Path current) {
    FileTime limit = FileTime.from(Instant.now().minus(KEEP));
    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(ROOT)) {
      for (Path dir : dirs) {
        if (!dir.equals(current) && Files.getLastModifiedTime(dir).compareTo(limit) < 0) {
          deleteQuietly(dir);
        }
      }
    } catch (IOException ignored) {
      // the copies are only temporary files
    }
  }

  private static void deleteQuietly(Path dir) {
    // a copy that an open class loader still uses cannot be deleted on Windows, it is tried again later
    try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
      for (Path f : files) {
        Files.deleteIfExists(f);
      }
      Files.deleteIfExists(dir);
    } catch (IOException ignored) {
      // see above
    }
  }
}
