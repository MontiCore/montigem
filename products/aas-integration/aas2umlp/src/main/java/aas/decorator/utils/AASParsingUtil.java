package aas.decorator.utils;

import de.se_rwth.commons.logging.Log;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.AASXDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.xml.XmlDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AASParsingUtil {

  /**
   * Parses an Asset Administration Shell (AAS) model from the given file path.
   * <p>
   * If the provided path points to a file, the file is parsed.
   * If the provided path points to a directory, the directory
   * is walked to find a file that contains an AAS.
   *
   *
   * @param aasPath the path to the AAS model file to parse
   * @return the parsed environment containing the AAS model or
   * null if the file could not be parsed.
   * @throws RuntimeException if an error occurs while parsing
   *  the model (e.g., file not found, invalid format)
   */
  public static Environment parseAAS(Path aasPath) {

    Environment env = null;

    if (aasPath == null) {
      Log.error("AAS parsing failed: null path");
      return env;
    }

    if(Files.isRegularFile(aasPath)) {
      File aasFile = aasPath.toFile();
      env = parse(aasFile);
    } else if(Files.isDirectory(aasPath)) {
      try {
        // Walk the directory to find the first valid AAS model (.aasx, .json, or .xml)
        java.util.stream.Stream<Path> paths = Files.walk(aasPath).filter(it -> {
          String fileName = it.toFile().getName().toLowerCase();
          return fileName.endsWith(".aasx") || fileName.endsWith(".json") || fileName.endsWith(".xml");
        });
        java.util.Optional<Path> firstValid = paths.findFirst();
        if (firstValid.isPresent()) {
          env = parse(firstValid.get().toFile());
        }
        paths.close();
      } catch (Exception e) {
        throw new RuntimeException("Error walking directory while looking for AAS model: " + aasPath.toString(), e);
      }
    }

    if (env == null) {
      Log.error("No valid AAS model file found at: " + aasPath.toString());
    }
    return env;
  }

  private static Environment parse(File aasPath) {
    Environment env = null;

    try (InputStream input = new FileInputStream(aasPath)) {
      if (aasPath.getName().endsWith(".aasx")) {
        env = new AASXDeserializer(input).read();
        return env;
      } else if (aasPath.getName().endsWith(".json")) {
        env = new JsonDeserializer()
            .read(input, StandardCharsets.UTF_8, Environment.class);
        return env;
      } else if (aasPath.getName().endsWith(".xml")) {
        env = new XmlDeserializer()
            .read(input, StandardCharsets.UTF_8);
        return env;
      } else {
        return null;
      }
    } catch (Exception e) {
      Log.error("Error parsing AAS file: " + aasPath.toString(), e);
    }

    return null;

  }
}
