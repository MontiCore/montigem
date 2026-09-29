/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import org.gradle.api.file.FileSystemLocation;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RelativePaths {

  /**
   * Sorted paths of the files relative to the base directory. Used as @Input when only the
   * existence of files matters, and relative so that the build cache works across checkouts.
   */
  public static List<String> of(File baseDir, Collection<? extends FileSystemLocation> files) {
    Path base = baseDir.toPath();
    return files.stream()
        .map(f -> f.getAsFile().toPath())
        .map(p -> base.getRoot().equals(p.getRoot()) ? base.relativize(p) : p)
        .map(p -> p.toString().replace('\\', '/'))
        .sorted()
        .collect(Collectors.toList());
  }
}
