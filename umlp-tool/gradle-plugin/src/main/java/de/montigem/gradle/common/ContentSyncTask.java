/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Like {@link org.gradle.api.tasks.Sync}, but writes a file only if its content changed.
 * ng serve rebuilds for every written file, and Sync writes all files again on every run.
 */
@DisableCachingByDefault(because = "Copying files is not worth caching")
public abstract class ContentSyncTask extends DefaultTask {

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getFrom();

  @Input
  public abstract ListProperty<String> getExcludes();

  /**
   * Only files matching these patterns are copied, all files if empty
   */
  @Input
  public abstract ListProperty<String> getIncludes();

  @OutputDirectory
  public abstract DirectoryProperty getInto();

  @TaskAction
  public void sync() throws IOException {
    // later sources win, as with DuplicatesStrategy.INCLUDE of Sync
    Map<String, File> files = new LinkedHashMap<>();
    getFrom().getAsFileTree().matching(spec -> spec.include(getIncludes().get()).exclude(getExcludes().get())).visit(details -> {
      if (!details.isDirectory()) {
        files.put(details.getRelativePath().getPathString(), details.getFile());
      }
    });
    syncByContent(files, getInto().get().getAsFile().toPath());
  }

  /**
   * Makes {@code into} contain exactly the given files (relative path to source file).
   * Unchanged files keep their timestamp. Links and junctions in {@code into} are neither followed nor deleted.
   */
  public static void syncByContent(Map<String, File> files, Path into) throws IOException {
    Set<Path> wanted = new HashSet<>();
    for (Map.Entry<String, File> entry : files.entrySet()) {
      Path target = into.resolve(entry.getKey());
      wanted.add(target);
      Path source = entry.getValue().toPath();
      if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.mismatch(source, target) != -1) {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    Files.walkFileTree(into, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        // on Windows a junction is a directory with isOther() set
        return dir.equals(into) || !(attrs.isOther() || attrs.isSymbolicLink())
            ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (attrs.isRegularFile() && !wanted.contains(file)) {
          Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        if (!dir.equals(into)) {
          try (Stream<Path> children = Files.list(dir)) {
            if (children.findAny().isEmpty()) {
              Files.delete(dir);
            }
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
