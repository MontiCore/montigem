package umlp.cli;

import de.monticore.cd._symboltable.BuiltInTypes;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdmerge.CDMerge;
import de.monticore.cdmerge.config.MergeParameter;
import de.monticore.class2mc.Class2MCResolver;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.util.SymbolTableUtil;
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import umlp.MCPathUtil;
import umlp.MillConf;
import umlp.TagRelated;
import umlp.UMLPMill;
import umlp.UMLPTool;
import umlp._symboltable.IUMLPArtifactScope;
import umlp._symboltable.UMLPSymbols2Json;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.UMLPGenerator;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.gui.gentypescript.config.ImportMapGenerator;
import umlp.generator.gui.gentypescript.java2ts.ComponentObserverManagerGenerator;
import umlp.generator.gui.gentypescript.printer.TSImportMap;
import umlp.generator.util.UMLPSymbolTables;
import umlp.generator.ocl.OCL2JavaGeneratorTool;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static umlp.UMLPToolOptions.DOMAIN;
import static umlp.UMLPToolOptions.FOR_CLIENT;
import static umlp.UMLPToolOptions.GEN_SYMBOLS_ONLY;
import static umlp.UMLPToolOptions.GUI_GEN_JSWEET;
import static umlp.UMLPToolOptions.MERGE;
import static umlp.UMLPToolOptions.OCL;
import static umlp.UMLPToolOptions.PRETTYPRINT;
import static umlp.UMLPToolOptions.SYMTAB;

public class UMLPFilePipeline extends Pipeline<UMLPPipelineData> {


  public final IOperation<UMLPPipelineData> init;
  public final IOperation.ISimpleOperation2<UMLPPipelineData> addBuiltInTypes;

  public Pipeline<ModelPathData> loadModelPathPipe;
  public final IOperation<UMLPPipelineData> loadModelpath;
  public Pipeline<Data> astPipe;
  public final IOperation.ISimpleOperation<UMLPPipelineData> loadAST;

  public final IOperation<Data> mergeAST;
  public final IOperation.ISimpleOperation<Data> storeSymbols;

  public final IOperation.ISimpleOperation<Data> writeSymbolsToSTD;
  public final IOperation<Data> loadOCL;

  public final IOperation.ISimpleOperation<Data> prettyPrint;
  public final IOperation.ISimpleOperation<Data> storeTags;
  public final IOperation.ISimpleOperation<Data> setupGenerator;
  public final IOperation.ISimpleOperation<Data> generate;

  UMLPFilePipeline(IOperation<UMLPPipelineData> setupUMLPReports) {
    register("setupUMLPReports", setupUMLPReports);
    register("init", init = data -> {
      // If we were to with to use concrete syntax, we would have to use the UMLPTagDefinitionMill
      MillConf.switchTo(MillConf.TAGS, MillConf.GUIDSL, MillConf.UMLP);
      if (SYMTAB.isSelected(data.cmd)) {
        TagRelated.initLookup(data.importedSymbolPath);
      }
      UMLPMill.globalScope().setSymbolPath(data.importedSymbolPath);
      TSImportMap.INSTANCE.init(data.importedSymbolPath);
      return true;
    });
    register("addBuiltInTypes", addBuiltInTypes = this::addBuiltInTypes);

    // Pipeline dealing with how to load models into the model path
    loadModelPathPipe = createPipe(ModelPathData.class);
    register("loadModelpath", IOperation.withDependantPipelines(loadModelpath = data -> {
      String report_on = data.inputFile.getName().replace(".", "__");   // TODO: Is this name unique? Should it be unique?

      List<URL> models = MCPathUtil.getAllModels(data.modelPath, ".*[.]umlp");
      for (URL modelUrl : models) {
        try (Reader reader = new InputStreamReader(modelUrl.openStream())) {
          ASTCDCompilationUnit ast = UMLPMill.parser().parse(reader).orElseThrow();
          Reporting.reportParseInputFile(data.inputFile.toPath().toAbsolutePath(), report_on);
          loadModelPathPipe.doWork(new ModelPathData(data, ast));
        } catch (IOException e) {
          e.printStackTrace();
          Log.error("Can not load model for url " + modelUrl, e);
          return false;
        }
      }
      return true;
    }, Collections.singletonList(loadModelPathPipe)));


    // Pipeline for dealing with parsed ASTs (will be called next)
    astPipe = createPipe(Data.class);
    // Next, we load the item into the AST
    register("loadAST", IOperation.withDependantPipelines(loadAST = data -> {
      String report_on = data.inputFile.getName().replace(".", "__");   // TODO: Is this name unique? Should it be unique?

      // parse, create symbol table, check all cocos
      ASTCDCompilationUnit ast = data.tool.parse(data.inputFile.getPath());
      Reporting.reportParseInputFile(data.inputFile.toPath().toAbsolutePath(), report_on); // TODO: causes NullPointerException
      astPipe.doWork(new Data(data, ast));
    }, Collections.singletonList(astPipe)));
    // Insert merge operation at the front of the ast pipeline
    astPipe.registerFirst("mergeAST", mergeAST = data -> {
      // First step of an AST: If --merge was provided, override AST with merged AST
      if (MERGE.isSelected(data.data.cmd)){
        List<ASTCDCompilationUnit> mergeSet = new ArrayList<>();
        mergeSet.add(data.data.tool.parse(data.data.inputFile.getPath()));
        // Parse and report parsing of merge-inputs
        for (String mergeFile : data.data.cmd.getOptionValues("mrg")) {
          File inputMergeFile = new File(mergeFile);
          mergeSet.add(data.data.tool.parse(inputMergeFile.getAbsolutePath()));
          // Note: Do NOT use reportParseInputFile, as it resets it. Instead, use reportOpenInputFile
          Reporting.reportOpenInputFile(inputMergeFile.toPath().toAbsolutePath().toString());
        }
        // Construct a name for the merged file
        String cdName = "Merge";
        if (PRETTYPRINT.isSelected(data.data.cmd)
            && PRETTYPRINT.getValue(data.data.cmd) != null ){
          cdName = Path.of(PRETTYPRINT.getValue(data.data.cmd)).getFileName().toString();
          if (PRETTYPRINT.getValue(data.data.cmd).endsWith(".cd")){
            cdName = cdName.substring(0, cdName.length() - 3);
          } else if (PRETTYPRINT.getValue(data.data.cmd).endsWith(".umlp")) {
            cdName = cdName.substring(0, cdName.length() - 5);
          } else {
            cdName = "Merge";
          }
        }
        ASTCDCompilationUnit merge = CDMerge.merge(mergeSet, cdName,
            Set.of(MergeParameter.FAIL_AMBIGUOUS));
        if (merge != null){
          // Continue with the merged AST
          data.setAst(merge);
        } else {
          // Abort (return false) and report error on unsuccessful merge
          Log.error("Merge was unsuccessful");
          return false;
        }
      }
      return true;
    });
    astPipe.register("storeSymbols", storeSymbols = data -> {
      if (SYMTAB.isSelected(data.data.cmd) && SYMTAB.getValue(data.data.cmd) != null) {
        String s = SYMTAB.getValue(data.data.cmd);
        // store symbol table to passed file
        data.symbolPath = Paths.get(s + File.separator + Names.getPathFromPackage(data.symbolTable.getPackageName()) + File.separator + data.symbolTable.getName() + ".umlpsym");
        JsonPrinter.disableIndentation();
        // Due to our broken-on-purpose-language aggregation (Tags and UMLP initialized separately)
        // we can simply store the UMLP symbol table (and ignore any tags in this regard)
        data.data.tool.storeSymbols(data.symbolTable, data.symbolPath.toString());
      }
    });
    astPipe.register("writeSymbolsToSTD", writeSymbolsToSTD = data -> {
      if (SYMTAB.isSelected(data.data.cmd) && SYMTAB.getValue(data.data.cmd) == null) {
        //print (formatted!) symboltable to console
        UMLPSymbols2Json s2j = new UMLPSymbols2Json();
        JsonPrinter.enableIndentation();
        data.symbolTable.accept(s2j.getTraverser());
        System.out.println(s2j.getSerializedString());
      }
    });

    astPipe.register("loadOCL", loadOCL = data -> {
      if (OCL.isSelected(data.data.cmd)) {
        MCPath oclModelPath = new MCPath();
        for (String v : data.data.cmd.getOptionValues(OCL.getCMD())) {
          oclModelPath.addEntry(Paths.get(v));
        }
        List<URL> oclFiles = MCPathUtil.getAllModels(oclModelPath, ".*[.]ocl");
        if (!oclFiles.isEmpty()) {
          List<Path> symbolPaths = new ArrayList<>(data.data.importedSymbolPath.getEntries());
          List<String> domainModels = DOMAIN.isSelected(data.data.cmd) ? List.of(data.data.cmd.getOptionValues(DOMAIN.getCMD())) : List.of();

          // One symbol table for all OCL files: the generator later type-checks the invariants of every
          // file, so a per-file reset would leave the earlier files in a stale global scope (order-dependent)
          OCL2JavaGeneratorTool.initSymtab(symbolPaths, domainModels);
          if (data.symbolPath != null && data.symbolPath.toFile().exists()) {
            SymbolTableUtil.loadSymbolFile(data.symbolPath.toString());
          }

          for (URL modelUrl : oclFiles) {
            try {
              File oclFile = new File(modelUrl.toURI());
              // Use OCLWithErrorMessagesMill to parse so we don't lose error messages
              Optional<ASTOCLCompilationUnit> oclCompilationUnit;
              try {
                oclCompilationUnit = OCLWithErrorMessagesMill.parser().parse(oclFile.getAbsolutePath());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              if (oclCompilationUnit.isEmpty()) {
                Log.error("0xE0001 Could not parse OCL file");
                return false;
              }
              ASTOCLCompilationUnit oCLAst = oclCompilationUnit.get();
              data.oclCompilationUnits.add(oCLAst);
              OCL2JavaGeneratorTool.createSymtab(oCLAst);
            } catch (URISyntaxException e) {
              throw new RuntimeException(e);
            }
          }
          AbstractDecorator.hasOCL = true;
        }
      }
      return true;
    });

    astPipe.register("prettyPrint", prettyPrint = data -> {
      // pretty print model and either output on stdout or store to file
      if (PRETTYPRINT.isSelected(data.data.cmd)) {
        data.data.tool.prettyPrint(data.ast, PRETTYPRINT.getValue(data.data.cmd));
      }
    });
    astPipe.register("storeTags", storeTags = data -> {
      TagRelated.doStoreTagStereos(data.ast, data.data.tool, data.symbolTable, data.data.cmd);
    });

    //
    astPipe.register("setupGenerator", setupGenerator = data -> {
      data.genSetup = new GeneratorSetup();
      data.genSetup.setGlex(UMLPTool.createGlex(data.data.tool.getModuleConfigTemplates(data.data.cmd)));
      // set output directory
      data.genSetup.setOutputDirectory(data.data.outDir);

      // set hwc directories
      data.genSetup.setHandcodedPath(data.data.hwcPath);

      // set template path
      data.genSetup.setAdditionalTemplatePaths(data.data.fpPath.getEntries().stream().map(Path::toFile).collect(Collectors.toList()));
      CD4C.init(data.genSetup);
    });

    // Finally, generate
    astPipe.register("generate", generate = data -> {
      UMLPGeneratorConfig config = UMLPGeneratorConfig.getInstance().initFromCmd(data.data.cmd);
      if (FOR_CLIENT.isSelected(data.data.cmd)) {
        Optional<String> tslib = UMLPGeneratorConfig.getInstance().getTSLib();
        if (tslib.isPresent()) {
          ImportMapGenerator importMapGenerator = new ImportMapGenerator(data.genSetup);
          importMapGenerator.generate(tslib.get(), data.ast);
        }
      } else if (GUI_GEN_JSWEET.isSelected(data.data.cmd)) {
        ComponentObserverManagerGenerator componentObserverManagerGenerator = new ComponentObserverManagerGenerator(data.genSetup);
        componentObserverManagerGenerator.generate(data.ast);
      } else if (!GEN_SYMBOLS_ONLY.isSelected(data.data.cmd)) {
        // execute generator
        UMLPGenerator generator = new UMLPGenerator(data.genSetup, config);
        String configTemplate = data.data.cmd.getOptionValue("ct", UMLPTool.DEFAULT_CONFIGTEMPLATE);
        generator.generate(data.ast, configTemplate, data.oclCompilationUnits);
      }
    });

    astPipe.addFinally(data -> {
      // write reports
      Reporting.flush(data.ast);
    });

  }

  public final IOperation.ISimpleOperation<DataWithCDASTAndTool> create_pipe_afterParseTrafo = data -> {
    data.getTool().runAfterParseTrafo(data.getAST());
  };
  public final IOperation.ISimpleOperation<DataWithCDASTAndTool> create_pipe_create_st = data -> {
    data.getTool().createSymbolTable(data.getAST());
  };
  public final IOperation.ISimpleOperation<DataWithCDASTAndTool> create_pipe_run_def_coco = data -> {
    data.getTool().runDefaultCoCos(data.getAST());
  };
  public final IOperation.ISimpleOperation<DataWithCDASTAndTool> create_pipe_run_def_trafos = data -> {
    data.getTool().runDefaultTrafos(data.getAST());
  };
  public final IOperation.ISimpleOperation<DataWithCDASTAndTool> create_pipe_recreate_st = data -> {
    data.setSymbolTable(data.getTool().recreateSymbolTable(data.getAST()));
  };

  <T extends DataWithCDASTAndTool> Pipeline<T> createPipe(Class<T> clazz) {
    Pipeline<DataWithCDASTAndTool> pipe = new Pipeline<>();
    pipe.register("create_pipe_afterParseTrafo", create_pipe_afterParseTrafo);
    pipe.register("create_pipe_create_st", create_pipe_create_st);
    pipe.register("create_pipe_run_def_coco", create_pipe_run_def_coco);
    pipe.register("create_pipe_run_def_trafos", create_pipe_run_def_trafos);
    pipe.register("create_pipe_recreate_st", create_pipe_recreate_st);
    return (Pipeline<T>) pipe;
  }

  public static class ModelPathData implements DataWithCDASTAndTool {
    final UMLPPipelineData data;
    final ASTCDCompilationUnit ast;

    public ModelPathData(UMLPPipelineData data, ASTCDCompilationUnit ast) {
      this.data = data;
      this.ast = ast;
    }

    @Override
    public ASTCDCompilationUnit getAST() {
      return this.ast;
    }

    @Override
    public UMLPTool getTool() {
      return this.data.tool;
    }

    @Override
    public void setSymbolTable(IUMLPArtifactScope symbolTable) {
      // do nothing
    }
  }

  public static class Data implements DataWithCDASTAndTool {
    final UMLPPipelineData data;
    ASTCDCompilationUnit ast;
    List<ASTOCLCompilationUnit> oclCompilationUnits = new ArrayList<>();

    public Data(UMLPPipelineData data, ASTCDCompilationUnit ast) {
      this.data = data;
      this.ast = ast;
    }

    IUMLPArtifactScope symbolTable;
    Path symbolPath;
    GeneratorSetup genSetup;

    @Override
    public ASTCDCompilationUnit getAST() {
      return this.ast;
    }

    @Override
    public UMLPTool getTool() {
      return this.data.tool;
    }

    public void setAst(ASTCDCompilationUnit ast) {
      this.ast = ast;
    }

    @Override
    public void setSymbolTable(IUMLPArtifactScope symbolTable) {
      this.symbolTable = symbolTable;
    }
  }

  void addBuiltInTypes() {
    BuiltInTypes.addBuiltInTypes(UMLPMill.globalScope(), false);
  }


  public interface DataWithCDAST {
    ASTCDCompilationUnit getAST();
    void setSymbolTable(IUMLPArtifactScope symbolTable);
  }

  public interface DataWithTool {
    UMLPTool getTool();
  }

  public interface DataWithCDASTAndTool extends DataWithCDAST, DataWithTool {
  }

  public Pipeline<Data> getAstPipe() {
    return astPipe;
  }
}
