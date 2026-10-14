/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.common;

import de.se_rwth.commons.logging.Log;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class IncGenData {
  protected Set<File> generatedFiles = new TreeSet<>();
  protected Set<File> readFiles = new TreeSet<>();
  protected Set<File> existenceCheckedFiles = new TreeSet<>();
  protected File basePath;

  protected boolean reportExists;

  public IncGenData() {
    reportExists = false;
  }

  // Parse from file
  public IncGenData(File file, File basePath) {
    this.basePath = basePath;

    if (file.exists()) {
      assert (file.isFile());
      reportExists = true;

      try (Stream<String> lines = Files.lines(file.toPath(), Charset.defaultCharset())) {

        lines.forEach(line -> {

          checkAndAddToSet("out:", generatedFiles, line, 0);
          // content changes of hand-written Java files only matter to JSweet, see GUIJava2TSTask#shouldRunAgain
          checkAndAddToSet("hwc:", existenceCheckedFiles, line, 0);
          checkAndAddToSet("gen:", existenceCheckedFiles, line, 0);

          checkAndAddToSet("mc4:", readFiles, line, 33);
            // manually remove md5 hash for input files
            // TODO: Better
        });
      } catch (Exception e) {
        // no error, with fail-quick logging it would terminate the Gradle daemon
        Log.warn("0xdf60e Error parsing report from previous run. Incremental check not working. \n" +
            e.getMessage());
        reportExists = false;
      }
    } else {
      Log.warn("Report from previous run not found. No incremental build.");
      reportExists = false;
    }
  }

  protected void checkAndAddToSet(String prefix, Set<File> store, String line, int deleteSuffix) throws RuntimeException {
    if (line.startsWith(prefix)) {
      String path = line.substring(prefix.length(), line.length() - deleteSuffix);
      try {
        store.add(Paths.get(this.basePath.getAbsolutePath(), path).toFile().getCanonicalFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void deleteGeneratedFiles() {
    generatedFiles.forEach(toDelete -> {
      Log.info("\t Delete " + toDelete.getPath(), this.getClass().getName());
      toDelete.delete();
    });
  }

  protected boolean isRelevantChange(FileChange ch) {
    try {
      File f = ch.getFile().getCanonicalFile();

      return ((ch.getChangeType() == ChangeType.ADDED || ch.getChangeType() == ChangeType.REMOVED)
          && existenceCheckedFiles.contains(f)
      )
          || readFiles.contains(f);
    } catch (IOException e) {
      return true;
    }
  }


  /**
   * @param inputChanges List of file-changes from gradle
   * @return if the tool should run again, or if the previous output can be reused
   */
  public boolean isUpToDate(Stream<FileChange> inputChanges) {
    if (!reportExists) {
      // If no report exists, we assume it is NOT up-to-date
      return false;
    } else {
      Optional<FileChange> change = inputChanges
          .filter(this::isRelevantChange)
          .findAny();
      if(change.isPresent()){
        Log.warn("Not UP-TO-DATE! Changed: " + change.get().getChangeType() + " on " + change.get().getFile().getAbsolutePath());;
      }
      return change.isEmpty();
    }
  }
}
