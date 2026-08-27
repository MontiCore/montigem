package ma2fustrumo;

import arcbasis._ast.ASTArcComponentType;
import com.google.common.base.Preconditions;
import de.monticore.symbols.compsymbols._symboltable.ComponentTypeSymbol;
import de.monticore.tagging.tags.TagsMill;
import de.se_rwth.commons.logging.Log;
import ma2fustrumo.pojo.SimpleFunction;
import montiarc.MontiArcMill;
import montiarc.MontiArcTool;
import montiarc._ast.ASTMACompilationUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class MA2FuStruMoTool extends MontiArcTool {
  protected SimpleFunction result;

  public static void main(String[] args) {
    Preconditions.checkNotNull(args);
    MA2FuStruMoTool tool = new MA2FuStruMoTool();
    tool.init();
    tool.run(args);
  }

  @Override
  public void init() {
    TagsMill.reset();
    TagsMill.init();
    super.init();
  }

  @Override
  public Options addStandardOptions(Options options) {
    Preconditions.checkNotNull(options);
    options.addOption(org.apache.commons.cli.Option
                          .builder("main")
                          .longOpt("--main")
                          .hasArg()
                          .desc("Sets the main component of the montiarc model.")
                          .build());
    return super.addStandardOptions(options);
  }

  @Override
  protected void doRun(CommandLine commandLine) {
    Preconditions.checkArgument(!commandLine.hasOption("h"));
    Preconditions.checkArgument(!commandLine.hasOption("v"));
    Preconditions.checkArgument(commandLine.hasOption("i"));
    Preconditions.checkNotNull(commandLine);

    if (!commandLine.hasOption("main")) {
      Log.error("Main component not defined. Use '-main' to set the main component.");
      this.printHelp();
      return;
    }

    String[] inputModelPaths = commandLine.hasOption("i") ?
        splitPathEntries(commandLine.getOptionValues("i")) :
        new String[0];
    String[] symbolPaths = commandLine.hasOption("path") ?
        splitPathEntries(commandLine.getOptionValues("path")) :
        new String[0];
    String mainComponent = commandLine.getOptionValue("main");
    String prettyPrintPath = commandLine.hasOption("pp") ?
        Optional.of(commandLine.getOptionValue("pp")).orElse("") :
        null;
    String symbolOutputPath = commandLine.getOptionValue("s");
    String reportPath = commandLine.getOptionValue("r");
    boolean enableClass2Mc = commandLine.hasOption("c2mc");
    boolean disableVariableComponents = commandLine.hasOption("novar");

    this.doRun(inputModelPaths,
               symbolPaths,
               mainComponent,
               prettyPrintPath,
               symbolOutputPath,
               reportPath,
               enableClass2Mc,
               disableVariableComponents);
  }

  protected void doRun(String[] inputModelPaths,
                       String[] symbolPaths,
                       String mainComponent,
                       String prettyPrintPath,
                       String symbolOutputPath,
                       String reportPath,
                       boolean enableClass2Mc,
                       boolean disableVariableComponents) {
    Preconditions.checkNotNull(inputModelPaths);
    Preconditions.checkNotNull(mainComponent);
    Preconditions.checkNotNull(symbolPaths);
    Preconditions.checkArgument(inputModelPaths.length > 0);

    MontiArcMill.globalScope().clear();
    MontiArcMill.globalScope().init();
    this.initBuildInSymbols(enableClass2Mc);
    this.initGlobalScope(symbolPaths);
    String[] tagPathEntries = Stream
        .concat(Arrays.stream(inputModelPaths), Arrays.stream(symbolPaths))
        .distinct()
        .toArray(String[]::new);
    this.compileWithTags(inputModelPaths,
                         mainComponent,
                         prettyPrintPath,
                         symbolOutputPath,
                         reportPath,
                         enableClass2Mc,
                         disableVariableComponents,
                         tagPathEntries);
  }

  /**
   * Parses all MontiArc component models found in the specified input files
   * and directories and checks context-condition. Optionally pretty-prints
   * the models, serializes their symbol table, generates reports,
   * and translates the models to java code.
   * <p>
   * Class2MC (c2mc) can be enabled to import symbols from the Java runtime
   * environment (Java RTE). Context-condition checking for variable components
   * can be skipped (novar) to improve performance.
   *
   * @param inputModelPaths           Array of file and directory paths that form the model path,
   *                                  i.e., the paths that contain the MontiArc models to be
   *                                  compiled.
   *                                  At least one path must be provided.
   * @param mainComponent             Sets the main component of the MontiArc model.
   * @param prettyPrintPath           Path to the directory where pretty-printed models should be
   *                                 stored.
   *                                  If {@code null}, pretty-printing is disabled.
   *                                  If empty, models are printed to standard output.
   * @param symbolOutputPath          Path to the directory where the symbol table should be
   *                                  serialized.
   *                                  If {@code null}, symbol table serialization is disabled.
   * @param reportPath                Path to the directory where reports should be stored.
   *                                  If {@code null}, report generation is disabled.
   * @param enableClass2Mc            Enables importing of Java symbols via Class2MC.
   * @param disableVariableComponents Disables context-condition checking for variable
   *                                  components to improve performance.
   * @return A collection of the abstract syntax trees (ASTs) of the parsed
   * input models found in the given model paths.
   */
  public Set<ASTMACompilationUnit> compile(String[] inputModelPaths,
                                           String mainComponent,
                                           String prettyPrintPath,
                                           String symbolOutputPath,
                                           String reportPath,
                                           boolean enableClass2Mc,
                                           boolean disableVariableComponents) {
    return compileWithTags(inputModelPaths,
                           mainComponent,
                           prettyPrintPath,
                           symbolOutputPath,
                           reportPath,
                           enableClass2Mc,
                           disableVariableComponents,
                           inputModelPaths);
  }

  protected Set<ASTMACompilationUnit> compileWithTags(String[] inputModelPaths,
                                                      String mainComponent,
                                                      String prettyPrintPath,
                                                      String symbolOutputPath,
                                                      String reportPath,
                                                      boolean enableClass2Mc,
                                                      boolean disableVariableComponents,
                                                      String[] tagPathEntries) {
    Preconditions.checkNotNull(inputModelPaths);
    Preconditions.checkNotNull(mainComponent);
    Preconditions.checkArgument(inputModelPaths.length > 0);

    TagLoader.load(tagPathEntries);
    try {
      Set<ASTMACompilationUnit> compilationUnits = super.compile(inputModelPaths,
                                                                 prettyPrintPath,
                                                                 symbolOutputPath,
                                                                 reportPath,
                                                                 enableClass2Mc,
                                                                 disableVariableComponents);
      this.generate(mainComponent);
      return compilationUnits;
    } finally {
      TagLoader.clear();
    }
  }

  public void generate(String mainComponent) {
    Preconditions.checkNotNull(mainComponent);
    Optional<ComponentTypeSymbol> mainType = MontiArcMill
        .globalScope()
        .resolveComponentType(mainComponent);
    if (mainType.isEmpty()) {
      Log.error("Main component not found in model path.");
    } else if (mainType.get().isPresentAstNode()) {
      doTransform(mainType);
    } else {
      Log.error("AST node of main component is missing.");
    }
  }

  protected void doTransform(Optional<ComponentTypeSymbol> mainType) {
    MA2FuStruMoTransformer transformer = new MA2FuStruMoTransformer();
    result = transformer.transform((ASTArcComponentType) mainType.get().getAstNode());
  }

  public SimpleFunction getResult() {
    return result;
  }
}
