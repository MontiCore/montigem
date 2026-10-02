/* (c) https://github.com/MontiCore/monticore */
package umlp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public enum UMLPToolOptions {
  // without args
  HELP("h", "help", "Prints this help dialog"),
  VERSION("v", "version", "Prints version information"),
  DEBUG("d", "debug", "Produces more detailed log"),

  CD2GUI("cdt", "cdtogui", "runs cd2gui to generate gui files from the input"),

  GUIRTETypes("rte", "rteTypes", "creates simple Java Types from GUI RTE types"),

  GUIVariability("gv", "guivariability", "Alterates guiFiles from the gp options with the guivariability files there and writes them to the outDir"),

  TEST_INFRASTRUCTURE("ti", "testInfrastructure", "Generate infrastructure for unit- and e2e-tests"),

  // with one arg
  INPUT("i", "input", "Reads the source file (mandatory) and parses the contents", "file"),
  OCL(Option.builder("ocl").longOpt("ocl").hasArgs().desc("Reads ocl file(s) (optional) and parses the contents").argName("file").build()),
  SYMTAB("s", "symboltable", "Serializes the symbol table of the given artifact.", "file", true),
  OUTPUT("o", "out", "Path of the output files.", "dir"),
  REPORTS("r", "report", "Prints reports of the artifact to the specified directory.", "dir", true),
  REPORT_BASE("rb", "report_base", "Base Directory used for relative paths in Reports.", "dir", true),
  REPORT_LEVEL("rl", "reportLevel", "all (default) writes all reports, incgen only the report that the incremental"
      + " Gradle build reads.", "level"),

  CFGTMPL("ct", "configTemplate", "Executes this template at the beginning of code generation to customize it. (template path may have to be set)", "file"),
  PRETTYPRINT("pp", "prettyprint", "Prints the AST to stdout or the specified file (optional)", "file", true),

  FOR_CLIENT("4c", "forclient", "forces UMLP generator to produce TypeScript code for client"),
  GEN_SYMBOLS_ONLY("so", "gensymbolsonly", "forces UMLP generator to only generate symbols for the cd"),

  //with multiple args
  PATH(Option.builder("path").hasArgs()
      .desc("Sets the artifact path for imported symbols, space separated.").build()),
  HWC_PATH(Option.builder("hwc").hasArgs()
      .desc("Sets the artifact paths for handwritten code, space separated.").build()),
  TMPL_PATH(Option.builder("fp").longOpt("templatePath").hasArgs()
      .desc("Directories and jars for handwritten templates, space separated.").build()),

  DOMAIN(Option.builder("dm").longOpt("domainModels").hasArgs()
      .desc("Fully qualified names of the domain models, space separated").build()),
  MODEl_PATH(Option.builder("mp").longOpt("modelPath").hasArgs()
      .desc("Sets the artifact path for imported models, space separated").build()),
  IMPORTS_PATH(Option.builder("ip").longOpt("importsPath").hasArgs()
      .desc("Sets the artifact path for files that are imported by the generated artifacts, space separated").build()),
  MODULE_CONFIG(Option.builder("m").longOpt("module-config").hasArgs()
      .desc("Fully qualified name(s) of config template(s) for added modules").build()),
  MERGE(Option.builder("mrg").longOpt("merge").hasArgs().desc("Parses the files as additional CDs" +
      " and merges them with the input CD (iff semantically sound). The result is stored in memory.")
      .build()),

  /**
   * RQ: GUI has no strategy to differentiate create and update pages, but we should only generate
   * one component per model. Adding a flag allows to explicitly state which models are for object
   * creation, otherwise pages for update are generated. Normally the flag is set by the CD2GUI,
   * but in the presence of hw models the flag is necessary.
   * Example: -guic stanley.PersonForm
   */
  GUI_CREATE(Option.builder("guic").longOpt("guiCreateModels").hasArgs()
      .desc("Fully qualified names of the gui models for object creation, space separated").build()),
  GUI_GEN_TS_FUNC(Option.builder("guits").longOpt("guiGenTSFuncionality").hasArgs()
      .desc("Flag which determines whether TS functionality should directly be generated from GUI models").build()),
  GUI_GEN_JSWEET(Option.builder("jsweet").longOpt("guiGenJsweet").hasArgs()
      .desc("Flag which determines whether TS code should be transpiled instead of generated from GUI models directly").build()),
  GUI_GEN_FOR_TS_LIB(Option.builder("tslib").argName("library_name").hasArg()
      .desc("Name of a TS library if generation schema is adjusted for the TS library").build()),
  USE_SPRING(Option.builder("use_spring").desc("If the backend is generated, generate additional spring classes").build()),
  USE_SHELL(Option.builder("use_shell").desc("If the backend is generated, generate additional spring shell classes").build()),
  USE_PLAYWRIGHT(Option.builder("use_playwright").desc("If spring classes are generated, generate additional playwright e2e infrastructure").build()),
  GEN_NEO4J(Option.builder("gen_neo4j").desc("Generate database access infrastructure for a Neo4j / Cypher databases.").build()),
  USE_NEO4J(Option.builder("use_neo4j").desc("Make Neo4j the default database access for the MontiGem app.").build()),
  GEN_MONITOR(Option.builder("gen_monitor").desc("Generate Monitor subclasses for all domain classes.").build()),
  GEN_IMPORT_MAP(Option.builder("gen_import_map").desc("Generate Import Map for GUI files.").build());

  UMLPToolOptions(Option option) {
    this.option = option;
  }

  UMLPToolOptions(String opt, String longOpt, String desc) {
    this.option = Option.builder(opt)
        .longOpt(longOpt)
        .desc(desc)
        .build();
  }

  UMLPToolOptions(String opt, String longOpt, String desc, String argName) {
    this(opt, longOpt, desc, argName, false);
  }

  UMLPToolOptions(String opt, String longOpt, String desc, String argName, boolean isArgOptional) {
    this.option = Option.builder(opt)
        .longOpt(longOpt)
        .argName(argName)
        .optionalArg(isArgOptional)
        .hasArg()
        .desc(desc)
        .build();
  }

  private Option option;

  public Option getOption() {
    return option;
  }

  public String getCMD() {
    return option.getOpt();
  }

  public boolean isSelected(CommandLine cmd) {
    return cmd.hasOption(option.getOpt());
  }

  public String getValue(CommandLine cmd) {
    if (!option.hasArg() || !isSelected(cmd)) {
      return "";
    }
    return cmd.getOptionValue(option.getOpt());
  }

  /**
   * The report level of the command line, {@link ReportLevel#ALL} if not given.
   */
  public static ReportLevel reportLevel(CommandLine cmd) {
    return ReportLevel.of(REPORT_LEVEL.getValue(cmd, ReportLevel.ALL.name()));
  }

  public String getValue(CommandLine cmd, String defaultValue) {
    if (!option.hasArg()) {
      return "";
    }
    if (!isSelected(cmd)) {
      return defaultValue;
    }
    return cmd.getOptionValue(option.getOpt());
  }
}
