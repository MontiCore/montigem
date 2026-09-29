/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui;

import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.io.FileReaderWriter;
import de.monticore.io.paths.MCPath;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.json.JsonArray;
import de.monticore.symboltable.serialization.json.JsonElement;
import de.monticore.symboltable.serialization.json.JsonObject;
import de.monticore.symboltable.serialization.json.JsonString;
import de.monticore.symboltable.serialization.json.UserJsonString;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class JSweetConfigGenerator {

  protected GeneratorSetup generatorSetup;
  protected GeneratorEngine generatorEngine;

  public JSweetConfigGenerator(GeneratorSetup generatorSetup) {
    this.generatorSetup = generatorSetup;
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    generatorSetup.setTracing(false);
  }

  public void generate(String outputDir, boolean forCD, List<String> srcDirs) {
    if(forCD) {
      createJsweetConfig("umlp/jsweetconfig/jsweetCDConfig.json", outputDir, srcDirs);
    } else {
      createJsweetConfig("umlp/jsweetconfig/jsweetGUINoDomain.json", outputDir, srcDirs);
    }
  }

  // forDomain differentiates between the config file generated for the Domain JSweet Task and the
  //   GUI JSweet task
  public void generate(String outputDir, String domainName, boolean forDomain, List<String> srcDirs) {
    String defaultConfigPath = forDomain
        ? "umlp/jsweetconfig/JSweetConfigDomainJSON.json"
        : "umlp/jsweetconfig/JSweetConfigGUIJSON.json";

    String adapterQName = "umlp.jsweet.extension.umlp." + domainName + "ImportAdapter";

    createJsweetConfig(defaultConfigPath, outputDir, srcDirs,
        jsweetconfig -> {
          jsweetconfig.getAsJsonObject().putMember("domainName", new JsonString(domainName));
          JsonElement adaptersElement = jsweetconfig.getAsJsonObject().getMember("adapters");
          if (adaptersElement.isJsonArray()) {
            JsonArray adapters = adaptersElement.getAsJsonArray();
            adapters.add(new JsonString(adapterQName));
          }
        });

    String adapterNoExt = "jsweet_extension" + File.separator + Names.getPathFromPackage(adapterQName);
    File adapter = new File(adapterNoExt + ".java");
    Optional<File> topAdapter = calcTOPFile(adapterNoExt);
    if (topAdapter.isPresent()) {
      generatorEngine.generateNoA(
          "umlp.jsweetconfig.JSweetDomainImportAdapter",
          topAdapter.get().toPath(),
          Names.getSimpleName(adapterQName) + "TOP",
          domainName
      );
    }
    else {
      generatorEngine.generateNoA(
          "umlp.jsweetconfig.JSweetDomainImportAdapter",
          adapter.toPath(),
          Names.getSimpleName(adapterQName),
          domainName
      );
    }
  }

  protected void createJsweetConfig(String defaultConfig, String outputDir, List<String> srcDirs) {
    createJsweetConfig(defaultConfig, outputDir, srcDirs, $ -> {
    });
  }

  protected void createJsweetConfig(
      String defaultConfig, String outputDir, List<String> srcDirs, Consumer<JsonObject> configTrafo
  ) {
    Path outputPath = Paths.get(outputDir, "jsweetconfig.json");
    URL resource = this.getClass().getClassLoader().getResource(defaultConfig);
    if (resource == null) {
      Log.error("0x0002F Generating jsweetconfig.json failed:"
          + "\nResource not found: " + defaultConfig);
      return;
    }
    try (InputStream is = openUncached(resource)) {
      JsonElement jsweetconfigElement = JsonParser.parse(
          new String(is.readAllBytes(), StandardCharsets.UTF_8)
      );
      if (jsweetconfigElement.isJsonObject()) {
        JsonObject jsweetconfig = jsweetconfigElement.getAsJsonObject();
        JsonArray dirs = new JsonArray();
        for (String path : srcDirs) {
          dirs.add(new JsonString(path));
        }
        jsweetconfig.putMember("srcDirs", dirs);

        configTrafo.accept(jsweetconfig);
      }
      else {
        Log.error("0x00030 Generating jsweetconfig.json failed: `" + defaultConfig
            + "`. Internal MontiGem error - please report to MontiGem developers.\n");
      }
      FileReaderWriter.storeInFile(outputPath, maybeTOP(jsweetconfigElement));
      Log.debug(outputPath + " written successfully", this.getClass().getName());
    } catch (IOException e) {
      Log.error("0x00031 Tried reading resource file `" + defaultConfig
          + "`, but failed with exception\n" + e);
    }
  }

  /**
   * Without the JVM-wide jar cache: in the Gradle daemon, closing another class loader on the
   * same jar would otherwise close this stream ("Stream closed").
   */
  protected static InputStream openUncached(URL resource) throws IOException {
    URLConnection connection = resource.openConnection();
    connection.setUseCaches(false);
    return connection.getInputStream();
  }

  protected Optional<File> calcTOPFile(String origName) {
    File f = new File(origName + ".java");
    Optional<Path> hwc = getHWC(generatorSetup.getHandcodedPath(), f.getPath());
    if (hwc.isPresent()) {
      return Optional.of(new File(origName + "TOP" + ".java"));
    }
    return Optional.empty();
  }

  protected String maybeTOP(JsonElement jsweetconfig) {
    File f = new File("jsweetconfig.json");
    Optional<Path> hwc = getHWC(generatorSetup.getHandcodedPath(), f.getPath());
    if (hwc.isPresent()) {
      try {
        String config = Files.readString(hwc.get());
        JsonElement json = JsonParser.parse(config);
        merge(jsweetconfig, json);
        jsonString2UserJsonString(json);
        return json.print(new IndentPrinter());
      } catch (IOException e) {
        Log.error("0x0002D attempted reading JSON file "
            + hwc.get() + " but failed with the exception: " + e);
      }
    }
    jsonString2UserJsonString(jsweetconfig);
    return jsweetconfig.print(new IndentPrinter());
  }

  protected void merge(JsonElement from, JsonElement to) {
    merge(from, to, "");
  }

  protected void merge(JsonElement from, JsonElement to, String pathPrefix) {
    if (match(from, to)) {
      if (from.isJsonObject()) {
        JsonObject f = from.getAsJsonObject();
        JsonObject t = to.getAsJsonObject();
        for (String key : f.getMemberNames()) {
          JsonElement value = f.getMember(key);
          if (t.hasMember(key)) {
            boolean valueTypesMatch = match(value, t.getMember(key));
            if (valueTypesMatch) {
              merge(value, t.getMember(key), key);
            }
            else {
              warn0002E(pathPrefix);
            }
          }
          else {
            t.putMember(key, value);
          }
        }
      }
    }
    else {
      warn0002E(pathPrefix);
    }
  }

  protected void warn0002E(String pathPrefix) {
    Log.warn("0x0002E Merging jsweetconfig.json: mismatching value types for the path \n\t`"
        + pathPrefix + "`\nKeeping hand-written value.");
  }

  protected boolean match(JsonElement first, JsonElement second) {
    return first.isJsonArray() == second.isJsonArray()
        && first.isJsonObject() == second.isJsonObject()
        && first.isJsonBoolean() == second.isJsonBoolean()
        && first.isJsonNumber() == second.isJsonNumber()
        && first.isJsonNull() == second.isJsonNull()
        && first.isJsonString() == second.isJsonString();
  }

  protected void jsonString2UserJsonString(JsonElement json) {
    if (json.isJsonObject()) {
      for (String key : json.getAsJsonObject().getMemberNames()) {
        JsonElement value = json.getAsJsonObject().getMember(key);
        if (value.isJsonString()) {
          json.getAsJsonObject()
              .putMember(key, new UserJsonString(value.getAsJsonString().getValue()));
        }
        else if (value.isJsonObject() || value.isJsonArray()) {
          jsonString2UserJsonString(value);
        }
      }
    }
    else if (json.isJsonArray()) {
      for (int i = 0; i < json.getAsJsonArray().getValues().size(); ++i) {
        JsonElement value = json.getAsJsonArray().getValues().get(i);
        if (value.isJsonString()) {
          json.getAsJsonArray().getValues().set(i, new UserJsonString(value.getAsJsonString().getValue()));
        }
        else if (value.isJsonObject() || value.isJsonArray()) {
          jsonString2UserJsonString(value);
        }
      }
    }
  }

  protected Optional<Path> getHWC(MCPath targetPath, String hwFile) {
    Optional<URL> hwFilePath = targetPath.find(hwFile);
    if (hwFilePath.isPresent()) {
      Optional<Path> hwPath = MCPath.toPath(hwFilePath.get());
      if (hwPath.isPresent()) {
        Reporting.reportUseHandwrittenCodeFile(hwPath.get(), Paths.get(hwFile));
        Reporting.reportHWCExistenceCheck(targetPath, Paths.get(hwFile), hwFilePath);
      }
      return hwPath;
    }
    return Optional.empty();
  }

}
