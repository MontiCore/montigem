package util;

import org.jsweet.transpiler.EcmaScriptComplianceLevel;
import org.jsweet.transpiler.JSweetFactory;
import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.ModuleKind;
import org.jsweet.transpiler.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class TranspilerSetupUtil {

  public static final String domainConfig = "src/test/resources/jsweetconfigs/domainConfig.json";
  public static final String defaultDomainOutput = "build/transpile/domainout";

  public static final String guiConfig = "src/test/resources/jsweetconfigs/guiConfig.json";
  public static final String defaultGUIOutput = "build/transpile/guiout";

  public static JSweetTranspiler getGUITranspiler() {
    return getGUITranspiler(new File(defaultGUIOutput));
  }

  public static JSweetTranspiler getDomainTranspiler() {
    return getDomainTranspiler(new File(defaultDomainOutput));
  }

  public static JSweetTranspiler transpilerModuleUMLPAdapter() {
    return getTranspilerFromConfigFile(
        new File("src/test/resources/jsweetconfigs/ModuleUMLPAdapter.json"),
        new File("build/transpile/umlpmodule")
    );
  }

  public static JSweetTranspiler getGUITranspiler(File outputDir) {
    return getTranspilerFromConfigFile( new File(guiConfig), outputDir);
  }

  public static JSweetTranspiler getDomainTranspiler(File outputDir) {
    return getTranspilerFromConfigFile(new File(domainConfig), outputDir);
  }

  public static JSweetTranspiler getTranspilerFromConfigFile(File configuration, File outputDir) {
    // Load node dir from props(see build.gradle)
    Properties nodeDirProps = new Properties();
    try {
      nodeDirProps.load(TranspilerSetupUtil.class.getResourceAsStream("/nodeDir.properties"));
    } catch (IOException e) {
      throw new RuntimeException("Can not load node path from properties", e);
    }
    String nodePath = Objects.requireNonNull(nodeDirProps.getProperty("node_dir"));
    File f = new File(nodePath);
    if(!f.exists()) {
      System.out.println("Error node could not be found in path "+f.getAbsolutePath());
    }

    ProcessUtil.addExtraPath(nodePath);

    JSweetFactory factory = new JSweetFactory();
    JSweetTranspiler transpiler = null;
    transpiler = new JSweetTranspiler(
        new File("build/transpile"),
        configuration,
        factory,
        null,
        outputDir,
        null,
        null,
        System.getProperty("java.class.path")
    );

    // Set according to our JSweet Plugin
    transpiler.setTscWatchMode(false);
    transpiler.setGenerateJsFiles(false);
    transpiler.setModuleKind(ModuleKind.commonjs);
    transpiler.setAutoPropagateAsyncAwaits(true);
    transpiler.setVerbose(true);
    transpiler.setEncoding("UTF-8");
    transpiler.setGenerateSourceMaps(true);
    transpiler.setEcmaTargetVersion(EcmaScriptComplianceLevel.ES6);

    return transpiler;
  }
}
