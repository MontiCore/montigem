/* (c) https://github.com/MontiCore/monticore */
package dummy;

import sehub.Project;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class ExtractorUtils {


  public static final int SIMPLE_NAME_POS = 0;

  public static final int NAME_EXTENSION_POS = 1;
  public static final String DIRECTORY_FILE_TYPE = "";

  /**
   * Converts the last modification time of a file to the value of Artifact.modified
   * @param lastModified milliseconds since the epoch, as returned by File.lastModified()
   * @return the time in the system time zone, to the second
   */
  public static LocalDateTime toModified(long lastModified) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault())
        .truncatedTo(ChronoUnit.SECONDS);
  }

  /**
   * Make all paths unique in their appearance so only "/" is used never "\"
   * @param path with potential "\"
   * @return path without "\"
   */
  public static String transformWindowsPathToLinuxStyle(String path){
    return path.replace("\\","/");
  }

  public static String normalizePath(String path) {
    if(path.endsWith("/")) {
      return path.substring(0, path.length()-1);
    }
    return path;
  }

  /**
   * Make all paths unique in their appearance so only "/" is used never "\"
   * And also remove part of the root
   * @param path with potential "\" + root path
   * @param r the root
   * @return path without "\" and root path yielding a relative path inside of the root
   */
  public static String removeRootPathFromAbsolutePath(String path, Project r){
    return transformWindowsPathToLinuxStyle(path).replace(r.getFullName()+"/", "");
  }


  public static String[] splitSimpleNameAndExtension(String fileName) {
    String[] nameAndExtension = new String[2];

    int pointFoundAt = fileName.lastIndexOf(".");
    nameAndExtension[SIMPLE_NAME_POS] = pointFoundAt >= 0 ? fileName.substring(0, pointFoundAt) : fileName;
    nameAndExtension[NAME_EXTENSION_POS] = pointFoundAt >= 0 ?
        fileName.substring(pointFoundAt) :
        "";
    return nameAndExtension;
  }
}
