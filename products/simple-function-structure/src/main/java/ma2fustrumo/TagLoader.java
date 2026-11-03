package ma2fustrumo;

import de.monticore.tagging.TagRepository;
import de.monticore.tagging.tags.TagsMill;
import de.monticore.tagging.tags._ast.ASTTagUnit;
import de.se_rwth.commons.logging.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TagLoader {
  private TagLoader() {
  }

  public static void load(String[] pathEntries) {
    TagRepository.clearTags();
    List<Path> tagFiles = new ArrayList<>();

    for (String pathEntry : pathEntries) {
      Path path = Path.of(pathEntry);
      if (Files.isDirectory(path)) {
        try (var files = Files.walk(path)) {
          files
              .filter(Files::isRegularFile)
              .filter(file -> file.getFileName().toString().endsWith(".tags"))
              .forEach(tagFiles::add);
        } catch (IOException e) {
          Log.error("0xDA202 Failed to discover tag files in " + path, e);
        }
      } else if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".tags")) {
        tagFiles.add(path);
      } else if (Files.isRegularFile(path) &&
                 (path.getFileName().toString().endsWith(".jar") ||
                  path.getFileName().toString().endsWith(".zip"))) {
        loadArchive(path);
      }
    }

    tagFiles
        .stream()
        .distinct()
        .sorted(Comparator.comparing(Path::toString))
        .forEach(TagLoader::load);
  }

  protected static void load(Path tagFile) {
    try {
      if (TagRepository.loadTagModel(tagFile.toFile()).isEmpty()) {
        Log.error("0xDA203 Failed to parse tag file " + tagFile);
      }
    } catch (IOException e) {
      Log.error("0xDA204 Failed to load tag file " + tagFile, e);
    }
  }

  protected static void loadArchive(Path archive) {
    try (ZipFile zipFile = new ZipFile(archive.toFile())) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory() && entry.getName().endsWith(".tags")) {
          try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(entry),
                                                                StandardCharsets.UTF_8)) {
            Optional<ASTTagUnit> parsed = TagsMill.parser().parse(reader);
            if (parsed.isEmpty()) {
              Log.error("0xDA205 Failed to parse tag file " + archive + "!/" + entry.getName());
            } else {
              TagRepository
                  .loadTempTagModel(archive + "!/" + entry.getName())
                  .ifPresent(tagUnit -> tagUnit.addAllTags(parsed.get().getTagsList()));
            }
          }
        }
      }
    } catch (IOException e) {
      Log.error("0xDA206 Failed to load tags from " + archive, e);
    }
  }

  public static void clear() {
    TagRepository.clearTags();
  }
}
