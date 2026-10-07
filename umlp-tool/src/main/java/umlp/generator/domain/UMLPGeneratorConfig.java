/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain;

import org.apache.commons.cli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static umlp.UMLPToolOptions.DOMAIN;
import static umlp.UMLPToolOptions.GEN_IMPORT_MAP;
import static umlp.UMLPToolOptions.GEN_MONITOR;
import static umlp.UMLPToolOptions.GEN_NEO4J;
import static umlp.UMLPToolOptions.GUI_GEN_FOR_TS_LIB;
import static umlp.UMLPToolOptions.GUI_TS_OUTPUT;
import static umlp.UMLPToolOptions.USE_NEO4J;
import static umlp.UMLPToolOptions.USE_PLAYWRIGHT;
import static umlp.UMLPToolOptions.USE_SHELL;
import static umlp.UMLPToolOptions.USE_SPRING;

public class UMLPGeneratorConfig {

  private boolean useSpring;
  private boolean usePlaywright;
  private boolean useShell;
  private boolean genNeo4j;
  private boolean useNeo4j;
  private boolean generateMonitor;
  private boolean generateImportMap;

  private String tslib;
  private String tsOutput;
  private String domain;

  private UMLPGeneratorConfig() {
  }

  private static UMLPGeneratorConfig instance = null;
  public static UMLPGeneratorConfig getInstance() {
    if (instance == null) {
      instance = new UMLPGeneratorConfig();
    }
    return instance;
  }

  public static void reset() {
    instance = null;
  }

  public boolean isUseSpring() {
    return useSpring;
  }

  public UMLPGeneratorConfig setUseSpring(boolean useSpring) {
    this.useSpring = useSpring;
    return this;
  }

  public boolean isGenNeo4j() {
    return genNeo4j;
  }

  public UMLPGeneratorConfig setGenNeo4j(boolean genNeo4j){
    this.genNeo4j = genNeo4j;
    return this;
  }

  public boolean isUseNeo4j() {
    return useNeo4j;
  }

  public UMLPGeneratorConfig setUseNeo4j(boolean useNeo4j){
    this.useNeo4j = useNeo4j;
    return this;
  }

  public boolean isUsePlaywright() {
    return usePlaywright;
  }

  public UMLPGeneratorConfig setUsePlaywright(boolean usePlaywright){
    this.usePlaywright = usePlaywright;
    return this;
  }

  public boolean isUseShell() {
    return useShell;
  }

  public UMLPGeneratorConfig setUseShell(boolean useShell) {
    this.useShell = useShell;
    return this;
  }

  public boolean is4TSLib() {
    return Optional.ofNullable(tslib).isPresent();
  }

  public Optional<String> getTSLib() {
    return Optional.ofNullable(tslib);
  }

  public UMLPGeneratorConfig setTSLib(String tslib) {
    this.tslib = tslib;
    return this;
  }

  /**
   * @return the folder the generated GUI files are copied to, if it is not the output folder
   */
  public Optional<Path> getTSOutput() {
    return Optional.ofNullable(tsOutput).map(Paths::get);
  }

  public UMLPGeneratorConfig setTSOutput(String tsOutput) {
    this.tsOutput = tsOutput;
    return this;
  }

  public boolean is4Domain() {
    return Optional.ofNullable(domain).isPresent();
  }

  public Optional<String> getDomain() {
    return Optional.ofNullable(domain);
  }

  public UMLPGeneratorConfig setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public boolean isGenerateImportMap() {
    return this.generateImportMap;
  }

  public UMLPGeneratorConfig setGenerateImportMap(boolean genenerateImportMap) {
    this.generateImportMap = genenerateImportMap;
    return this;
  }

  public boolean isGenerateMonitor() {
    return generateMonitor;
  }

  public UMLPGeneratorConfig setGenerateMonitor(boolean generateMonitor) {
    this.generateMonitor = generateMonitor;
    return this;
  }

  public UMLPGeneratorConfig initFromCmd(CommandLine cmd) {
    if (GUI_GEN_FOR_TS_LIB.isSelected(cmd)) {
      this.setTSLib(GUI_GEN_FOR_TS_LIB.getValue(cmd));
    }
    if (GUI_TS_OUTPUT.isSelected(cmd)) {
      this.setTSOutput(GUI_TS_OUTPUT.getValue(cmd));
    }
    if(DOMAIN.isSelected(cmd)) {
      this.setDomain(DOMAIN.getValue(cmd));
    }
    return this.setUseSpring(USE_SPRING.isSelected(cmd))
        .setUseShell(USE_SHELL.isSelected(cmd))
        .setUsePlaywright(USE_PLAYWRIGHT.isSelected(cmd))
        .setGenNeo4j(GEN_NEO4J.isSelected(cmd))
        .setUseNeo4j(USE_NEO4J.isSelected(cmd))
        .setGenerateMonitor(GEN_MONITOR.isSelected(cmd))
        .setGenerateImportMap(GEN_IMPORT_MAP.isSelected(cmd));
  }
}
