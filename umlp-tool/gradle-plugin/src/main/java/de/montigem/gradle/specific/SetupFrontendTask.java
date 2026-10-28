/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.StreamSupport;

import static de.montigem.gradle.FrontendBasePlugin.inUMLP;
import static de.montigem.gradle.FrontendBasePlugin.umlpVersion;
import static de.montigem.gradle.FrontendBasePlugin.useLocalDependencies;
import static de.montigem.gradle.FrontendBasePlugin.useNpmDistTag;

public abstract class SetupFrontendTask extends DefaultTask {

  private static final List<String> EXPECTED_FILES = List.of(
      ".npmrc", "package.json", "angular.json",
      "tsconfig.json", "tsconfig.app.json", "tsconfig.prod.json", "tsconfig.spec.json");
  private static final List<String> EXPECTED_DIRS = List.of(
      "src", "configs");

  private static final String FRONTEND_CONFIG_COMMON = "frontend/config/common/";
  // config for projects inside umlp (examples, products, ...)
  private static final String FRONTEND_CONFIG_DEV = "frontend/config/dev/";
  // config for projects outside umlp
  private static final String FRONTEND_CONFIG_PROD = "frontend/config/prod/";

  // Explicit on @OutputDirectory so Gradle does not delete or check the whole app dir
  @Internal
  public abstract DirectoryProperty getOutputRoot();

  // ---- Tracked outputs: exactly these paths ----

  @OutputFile
  public Provider<RegularFile> getPackageJson() {
    return getOutputRoot().file("package.json");
  }

  @OutputFile
  public Provider<RegularFile> getAngularJson() {
    return getOutputRoot().file("angular.json");
  }

  @OutputFile
  public Provider<RegularFile> getTsconfigJson() {
    return getOutputRoot().file("tsconfig.json");
  }

  @OutputFile
  public Provider<RegularFile> getTsconfigAppJson() {
    return getOutputRoot().file("tsconfig.app.json");
  }

  @OutputFile
  public Provider<RegularFile> getTsconfigProdJson() {
    return getOutputRoot().file("tsconfig.prod.json");
  }

  @OutputFile
  public Provider<RegularFile> getTsconfigSpecJson() {
    return getOutputRoot().file("tsconfig.spec.json");
  }

  @OutputDirectory
  public Provider<Directory> getSrcDir() {
    return getOutputRoot().dir("src");
  }

  @OutputDirectory
  public Provider<Directory> getConfigsDir() {
    return getOutputRoot().dir("configs");
  }

  @InputFiles
  @Incremental
  @Optional
  public abstract DirectoryProperty getInput();

  @Input
  @Optional
  public abstract Property<Boolean> getCreateNpmrc();

  @Input
  @Optional
  public abstract Property<Boolean> getUnscopedNpmrc();

  @Input
  public abstract Property<Boolean> getUseLocalNpmRepo();

  public SetupFrontendTask() {
    super();
    getCreateNpmrc().convention(true);
    getUnscopedNpmrc().convention(false);
    getUseLocalNpmRepo().convention(false);
  }

  @TaskAction
  public void run(InputChanges changes) throws IOException, URISyntaxException {
    getOutputRoot().getAsFile().get().mkdirs();

    if (getInput().isPresent()) {
      // Delete files in output, if file in input was deleted
      StreamSupport.stream(changes.getFileChanges(getInput()).spliterator(), false)
          .filter(ch -> ch.getChangeType().equals(ChangeType.REMOVED))
          .forEach(ch -> {
            String filename = getInput().getAsFile().get().toPath().relativize(ch.getFile().toPath()).toString();
            FileUtils.deleteQuietly(getOutputRoot().file(filename).get().getAsFile());
          });
    }

    if (getCreateNpmrc().get()) {
      String data = CreateNpmrcTask.getNpmrcContent(
          getProject(), getUnscopedNpmrc().get(), getUseLocalNpmRepo().get());
      FileUtils.writeStringToFile(getOutputRoot().get().file(".npmrc").getAsFile(), data, StandardCharsets.UTF_8);
    }

    // Avoid duplicate copy operations
    Set<String> usedHandwrittenFiles = new HashSet<>();

    // copy common config from resources of UMLP-Tool
    for (String filename : getResourceListing(this.getClass(), FRONTEND_CONFIG_COMMON)) {
      InputStream file = this.getClass().getClassLoader().getResourceAsStream(FRONTEND_CONFIG_COMMON + filename);
      if (filename.startsWith("/") || filename.startsWith("\\")) {
        filename = filename.substring(1);
      }
      usedHandwrittenFiles.add(filename);

      // if a handwritten file exists, use that instead
      if (doOverrideWithHWC(filename, file)) continue;

      FileUtils.copyInputStreamToFile(file, getOutputRoot().file(filename).get().getAsFile());
    }

    // copy dev or prod config
    String config = useLocalDependencies(getProject()) && inUMLP(getProject()) ? FRONTEND_CONFIG_DEV : FRONTEND_CONFIG_PROD;
    for (String filename : getResourceListing(this.getClass(), config)) {
      InputStream file = this.getClass().getClassLoader().getResourceAsStream(config + filename);
      if (filename.startsWith("/") || filename.startsWith("\\")) {
        filename = filename.substring(1);
      }
      usedHandwrittenFiles.add(filename);

      // the generator uses the libraries published by the same build
      if (config.equals(FRONTEND_CONFIG_PROD) && filename.equals("package.json")) {
        JsonObject packageJson = JsonParser.parseReader(new InputStreamReader(file, StandardCharsets.UTF_8)).getAsJsonObject();
        useNpmDistTag(packageJson.getAsJsonObject("dependencies"), umlpVersion(getProject()));
        file = new ByteArrayInputStream(new GsonBuilder().setPrettyPrinting().create().toJson(packageJson).getBytes(StandardCharsets.UTF_8));
      }

      // if a handwritten file exists, use that instead
      if (doOverrideWithHWC(filename, file)) continue;

      FileUtils.copyInputStreamToFile(file, getOutputRoot().file(filename).get().getAsFile());
    }

    if (getInput().isPresent()) {
      // Then Copy User-Defined Files (which were not handled previously)
      for (File f : getInput().getAsFileTree()) {
        Path rel = getInput().getAsFile().get().toPath().relativize(f.toPath());
        String relString = rel.toString();
        String first = rel.getName(0).toString();

        boolean isExpectedRootFile = rel.getNameCount() == 1 && EXPECTED_FILES.contains(first);
        boolean isInExpectedDir    = rel.getNameCount() > 1 && EXPECTED_DIRS.contains(first);

        if (isExpectedRootFile || isInExpectedDir) {
          FileUtils.copyFile(f, new File(getOutputRoot().getAsFile().get(), relString));
        } else {
          getLogger().warn(
              "setupFrontend: unexpected entry '{}' in input directory {} – "
                  + "only {} (files at the root) and {} (directories) are used; ignoring it.",
              first, getInput(), EXPECTED_FILES, EXPECTED_DIRS);
        }
      }
    }
  }

  boolean doOverrideWithHWC(String filename, InputStream file) throws IOException {
    if (getInput().isPresent()) {
      // Check if a file "file.override.ext" exists (e.g., angular.override.json)
      int dot = filename.lastIndexOf('.');
      Path hwcOverride = dot >= 0 ? getInput().getAsFile().get().toPath().resolve(filename.substring(0, dot) + ".override" + filename.substring(dot)) : null;

      if (dot >= 0 && hwcOverride.toFile().exists()) {
        if (!filename.endsWith(".json")) {
          getLogger().error("Failed to merge config file {}, as its file ending is not supported )(.json)", filename);
          return false;
        }
        getLogger().info("Merging JSON-based file with hand-written overrides one{}", filename);

        JsonElement base = JsonParser.parseReader(new BufferedReader(new InputStreamReader(file)));
        try (BufferedReader reader = new BufferedReader(new FileReader(hwcOverride.toFile()))) {
          JsonElement override = JsonParser.parseReader(reader);
          JsonElement merged = merge(base, override);
          FileUtils.writeStringToFile(getOutputRoot().file(filename).get().getAsFile(), merged.toString(), StandardCharsets.UTF_8);
        }
        return true;
      }
      Path hwc = getInput().getAsFile().get().toPath().resolve(filename);
      if (hwc.toFile().exists()) {
        getLogger().info("Overriding config file {} with hand-written one", filename);
        FileUtils.copyFile(hwc.toFile(), getOutputRoot().file(filename).get().getAsFile());
        return true;
      }
    }
    return false;
  }


  /**
   * List directory contents for a resource folder. Not recursive.
   * This is basically a brute-force implementation.
   * Works for regular files and also JARs.
   *
   * @param clazz Any java class that lives in the same place as the resources you want.
   * @param path  Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   * @author Greg Briggs
   */
  String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
    URL dirURL = clazz.getClassLoader().getResource(path);
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      /* A file path: easy enough */
      return new File(dirURL.toURI()).list();
    }

    if (dirURL == null) {
      /*
       * In case of a jar file, we can't actually find a directory.
       * Have to assume the same jar as clazz.
       */
      String me = clazz.getName().replace(".", "/") + ".class";
      dirURL = clazz.getClassLoader().getResource(me);
    }

    if (dirURL.getProtocol().equals("jar")) {
      /* A JAR path */
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
      Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
      Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
      for (Iterator<JarEntry> it = entries.asIterator(); it.hasNext(); ) {
        JarEntry entry = it.next();
        if (!entry.isDirectory()) {
          String name = entry.getName();
          if (name.startsWith(path)) { //filter according to the path
            result.add(name.substring(path.length()));
          }
        }
      }
      return result.toArray(new String[result.size()]);
    }

    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }

  protected static JsonElement merge(JsonElement base, JsonElement override) {
    if (!base.isJsonObject() || !override.isJsonObject()) {
      return override;
    }

    JsonObject merged = base.getAsJsonObject().deepCopy();
    for (String field : override.getAsJsonObject().keySet()) {
      JsonElement baseValue = base.getAsJsonObject().get(field);
      JsonElement overrideValue = override.getAsJsonObject().get(field);

      if (baseValue != null && baseValue.isJsonObject() && overrideValue.isJsonObject()) {
        merged.add(field, merge(baseValue, overrideValue));
      } else if (baseValue != null && !baseValue.isJsonNull() && overrideValue.isJsonNull()) {
        // do not add
      } else {
        merged.add(field, overrideValue);
      }
    }
    return merged;
  }

}
