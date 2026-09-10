package umlp.generator.ocl;

import de.monticore.class2mc.Class2MCResolver;
import de.monticore.gradle.common.GradleLog;
import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.ocl._symboltable.IOCLGlobalScope;
import de.monticore.ocl.ocl._symboltable.OCLSymbolTableCompleter;
import de.monticore.ocl.oclexpressions.symboltable.OCLExpressionsSymbolTableCompleter;
import de.monticore.ocl.setexpressions.symboltable.SetExpressionsSymbolTableCompleter;
import de.monticore.ocl.types.check.types3wrapper.TypeCheck3AsOCLDeriver;
import de.monticore.ocl.types.check.types3wrapper.TypeCheck3AsOCLSynthesizer;
import de.monticore.ocl.util.SymbolTableUtil;
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.se_rwth.commons.logging.Log;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import oclwitherrormessages._symboltable.OCLWithErrorMessagesScopesGenitorDelegator;
import oclwitherrormessages._visitor.OCLWithErrorMessagesTraverser;
import org.apache.commons.cli.*;
import umlp._symboltable.CDRoleSymbol2VariableSymbolAdapterDeSer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OCL2JavaGeneratorTool {

  public static void gradleMain(String[] args){
    GradleLog.init(); // by default, don't use SLF4J log
    main(args);
  }

  public static void main(String[] args) {
    Options options = new Options();

    Option oclOption = Option.builder("ocl")
        .required(true)
        .hasArgs()
        .valueSeparator(',')
        .desc("Comma-separated list of OCL files")
        .build();

    Option symbolsOption = Option.builder("symbols")
        .required(true)
        .hasArgs()
        .valueSeparator(',')
        .desc("Comma-separated list of symbol paths")
        .build();

    Option outputOption = Option.builder("out")
        .required(true)
        .hasArg()
        .desc("Output directory")
        .build();

    Option domainOption = Option.builder("domain")
        .required(false)
        .hasArgs()
        .valueSeparator(',')
        .desc("Comma-separated list of domain models")
        .build();

    Option prefixPackageOption = Option.builder("prefixPackage")
        .required(false)
        .hasArg()
        .desc("Prefix package for the generated code")
        .build();

    options.addOption(oclOption);
    options.addOption(symbolsOption);
    options.addOption(outputOption);
    options.addOption(domainOption);
    options.addOption(prefixPackageOption);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      formatter.printHelp("GeneratorApp", options);
      System.exit(1);
    }

    List<Path> oclFiles = Arrays.stream(cmd.getOptionValues("ocl"))
        .map(Paths::get)
        .collect(Collectors.toList());

    List<Path> symbolPaths = Arrays.stream(cmd.getOptionValues("symbols"))
        .map(Paths::get)
        .collect(Collectors.toList());

    Path outputDir = Paths.get(cmd.getOptionValue("out"));

    List<String> domainModels = cmd.hasOption("domain")
        ? List.of(cmd.getOptionValues("domain"))
        : List.of();

    String prefixPackage = cmd.getOptionValue("prefixPackage");

    OCL2JavaGeneratorTool tool = new OCL2JavaGeneratorTool();
    tool.generate(oclFiles, symbolPaths, outputDir, domainModels, prefixPackage);
  }

  public void generate(List<Path> oclFiles, List<Path> symbolPaths, Path outputDir, List<String> domainModels, String prefixPackage) {
    initSymtab(symbolPaths, domainModels);
    for (Path oclFile : oclFiles) {
      Log.info("Processing " + oclFile, "ocl2java");
      try {
        Optional<ASTOCLCompilationUnit> astOpt = OCLWithErrorMessagesMill.parser().parse(oclFile.toString());
        if (astOpt.isPresent()) {
          ASTOCLCompilationUnit ast = astOpt.get();

          String fullPackage = (prefixPackage != null && !prefixPackage.isEmpty())
              ? (ast.isPresentPackage() && !ast.getPackage().isEmpty() ? prefixPackage + "." + ast.getPackage() : prefixPackage)
              : ast.getPackage();

          Path outputPath = outputDir.resolve(fullPackage.replace(".", "/")).resolve(ast.getOCLArtifact().getName() + ".java");
          createSymtab(ast);
          outputPath.toFile().getParentFile().mkdirs();
          OCL2UMLPJavaGenerator.generate(ast, fullPackage, outputPath.toString());
        } else {
          Log.error("Can not parse from ocl " + oclFile);
        }
      } catch (IOException | NullPointerException e) {
        Log.error("Can not generate from ocl " + oclFile, e);
      }
    }
  }

  @SuppressWarnings("removal")
  public static void createSymtab(ASTOCLCompilationUnit ocl) {
    OCLWithErrorMessagesScopesGenitorDelegator genitor = OCLWithErrorMessagesMill.scopesGenitorDelegator();
    SymbolTableUtil.addDefaultImports(genitor.createFromAST(ocl));


    OCLSymbolTableCompleter stCompleter =
        new OCLSymbolTableCompleter(ocl.getMCImportStatementList(), ocl.getPackage());
    stCompleter.setSynthesizer(new TypeCheck3AsOCLSynthesizer());
    OCLExpressionsSymbolTableCompleter stCompleter2 = new OCLExpressionsSymbolTableCompleter();
    stCompleter2.setDeriver(new TypeCheck3AsOCLDeriver());
    stCompleter2.setSynthesizer(new TypeCheck3AsOCLSynthesizer());
    SetExpressionsSymbolTableCompleter stCompleter3 = new SetExpressionsSymbolTableCompleter();
    stCompleter3.setDeriver(new TypeCheck3AsOCLDeriver());
    stCompleter3.setSynthesizer(new TypeCheck3AsOCLSynthesizer());

    OCLWithErrorMessagesTraverser t = OCLWithErrorMessagesMill.inheritanceTraverser();
    t.add4BasicSymbols(stCompleter);
    t.add4OCL(stCompleter);
    t.setOCLHandler(stCompleter);
    t.setOCLExpressionsHandler(stCompleter2);
    t.setSetExpressionsHandler(stCompleter3);
    t.add4BasicSymbols(stCompleter2);
    t.add4OCLExpressions(stCompleter2);
    t.add4BasicSymbols(stCompleter3);
    t.add4SetExpressions(stCompleter3);
    ocl.accept(t);
  }

  public static void initSymtab(List<Path> symbolPaths, List<String> domainModels) {
    OCLWithErrorMessagesMill.reset();
    OCLWithErrorMessagesMill.init();
    OCLWithErrorMessagesMill.globalScope().clear();
    BasicSymbolsMill.initializePrimitives();

    Class2MCResolver resolver = new Class2MCResolver();
    OCLWithErrorMessagesMill.globalScope().addAdaptedTypeSymbolResolver(resolver);
    SymbolTableUtil.addOclpLibrary();

    NoRegexOCLTypeCheck3.init();

    TypeUtil.getInstance().init(domainModels);

    IOCLGlobalScope gs = OCLWithErrorMessagesMill.globalScope();

    gs.putTypeSymbolDeSer("de.monticore.cdbasis._symboltable.CDTypeSymbol");
    gs.putFunctionSymbolDeSer("de.monticore.cd4codebasis._symboltable.CDMethodSignatureSymbol");
    gs.putVariableSymbolDeSer("de.monticore.symbols.oosymbols._symboltable.FieldSymbol");
    gs.putSymbolDeSer("de.monticore.cdassociation._symboltable.CDRoleSymbol",
        new CDRoleSymbol2VariableSymbolAdapterDeSer());

    gs.setSymbolPath(new MCPath(symbolPaths));
  }
}
