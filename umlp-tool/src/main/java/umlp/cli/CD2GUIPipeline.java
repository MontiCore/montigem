package umlp.cli;

import cd2gui.CD2GUITool;
import de.monticore.cd._symboltable.BuiltInTypes;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.util.SymbolTableUtil;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import umlp.*;
import umlp._symboltable.IUMLPArtifactScope;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.UMLAddGemIdTrafo;
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

import static umlp.UMLPToolOptions.DOMAIN;
import static umlp.UMLPToolOptions.OCL;

public class CD2GUIPipeline extends Pipeline<UMLPPipelineData> {

  public final IOperation<UMLPPipelineData> init;
  public final IOperation.ISimpleOperation2<UMLPPipelineData> addBuiltInTypes;
  public final IOperation<UMLPPipelineData> generateGui;
  public final Pipeline<DataWithAst> modelPipeline;
  public final Pipeline<DataWithAst> loadModelPathPipe;
  
  public CD2GUIPipeline(IOperation<UMLPPipelineData> setupUMLPReports, IOperation<UMLPPipelineData> isUMLPFileCheck) {
    register("setupUMLPReports", setupUMLPReports);
    register("check_umlp_filetype", isUMLPFileCheck);
    register("init", init = data -> {
      MillConf.switchTo(MillConf.TAGS, MillConf.UMLP, MillConf.GUIDSL);
      UMLPMill.globalScope().setSymbolPath(data.importedSymbolPath);
      return true;
    });
    register("addBuiltInTypes", addBuiltInTypes = this::addBuiltInTypes);
    
    // Pipeline dealing with how to load models into the model path
    loadModelPathPipe = createLoadModelPathPipe();
    register("loadModelpath", IOperation.withDependantPipelines(data -> {
      String report_on = data.inputFile.getName().replace(".", "__");   // TODO: Is this name unique? Should it be unique?
      
      List<URL> models = MCPathUtil.getAllModels(data.modelPath, ".*[.]umlp");
      for (URL modelUrl : models) {
        try (Reader reader = new InputStreamReader(modelUrl.openStream())) {
          ASTCDCompilationUnit ast = UMLPMill.parser().parse(reader).orElseThrow();
          Reporting.reportParseInputFile(data.inputFile.toPath().toAbsolutePath(), report_on);
          Data dataWithAst = new Data(data);
          dataWithAst.setAst(ast);
          loadModelPathPipe.doWork(dataWithAst);
        } catch (IOException e) {
          e.printStackTrace();
          Log.error("Can not load model for url " + modelUrl, e);
          return false;
        }
      }
      return true;
    }, Collections.singletonList(loadModelPathPipe)));
    
    modelPipeline = createModelPipeline();
    register("generateGui",
        IOperation.withDependantPipelines(generateGui = data -> {
          modelPipeline.doWork(new Data(data));
          return true;
        }, Collections.singletonList(modelPipeline)));
  }

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_parse = data -> {
    ASTCDCompilationUnit ast = data.getTool().parse(data.getData().inputFile.getPath());
    Reporting.reportParseInputFile(data.getData().inputFile.toPath().toAbsolutePath(),
        getReportingOnModelName(
            data.getData().inputFile.toPath())); // TODO: causes NullPointerException
    data.setAst(ast);
  };

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_after_parse_trafos =
      data -> data.getTool().runAfterParseTrafo(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_symbol_table =
      data -> data.getTool().createSymbolTable(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_default_cocos =
      data -> data.getTool().runDefaultCoCos(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_default_trafos =
      data -> data.getTool().runDefaultTrafos(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_set_cd2gui_options = data -> {
    // Do not gen Forms for singletons
    for (String singletonClass : data.getTool().getSingletonClasses(data.getAst())) {
      data.addExtraOption("ExcludeForm", singletonClass);
      data.addExtraOption("NoOverview", singletonClass);
    }
  };

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_cd2gui_tool = data -> {
    CD2GUITool guiTool =
        new CD2GUITool(data.getAst(), data.getOclCompilationUnits(), data.getData().outDir, data.getData().hwcPath,
            data.getExtraOptions().toArray(new String[0]));
    guiTool.generateGUI();
  };

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_add_gem_id_trafo = data -> {
    new UMLAddGemIdTrafo(UMLPMill.inheritanceTraverser()).transform(data.getAst());
  };

  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_recreate_and_store_symbol_table =
      data -> {
        // TODO (MB): Ist diese Lösung okay?
        // Create a customized symbol table for the generated GUI-models
        IUMLPArtifactScope symbolTable = data.getTool().recreateSymbolTable(data.getAst());
        Path symbolPath = Paths.get(
            data.getData().outDir.getPath() + File.separator + "symbols" + File.separator
                + Names.getPathFromPackage(symbolTable.getPackageName()) + File.separator
                + symbolTable.getName() + ".umlpsym");
        data.setSymbolPath(symbolPath);
        JsonPrinter.disableIndentation();
        data.getTool().storeSymbols(symbolTable, symbolPath.toString());
        data.setSymbolTable(symbolTable);
      };
  
  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_store_tags =  data -> {
    // Create a customized tags file for the generated GUI-models
    IUMLPArtifactScope symbolTable = data.getTool().recreateSymbolTable(data.getAst());
    Path tagPath = Paths.get(
        data.getData().outDir.getPath() + File.separator + "symbols" + File.separator
            + Names.getPathFromPackage(symbolTable.getPackageName()) + File.separator
            + symbolTable.getName() + ".tags");
    TagRelated.doStoreTagStereos(data.getAst(), data.getData().tool, data.getSymbolTable(), data.getData().cmd, tagPath);
    TagRelated.initLookup(new MCPath(tagPath));
  };
  
  public final IOperation.ISimpleOperation<DataWithAst> model_pipe_load_ocl =  data -> {
    if (OCL.isSelected(data.getData().cmd)) {
      MCPath oclModelPath = new MCPath();
      for (String v : data.getData().cmd.getOptionValues(OCL.getCMD())) {
        oclModelPath.addEntry(Paths.get(v));
      }
      List<URL> oclFiles = MCPathUtil.getAllModels(oclModelPath, ".*[.]ocl");
      if (!oclFiles.isEmpty()) {
        List<Path> symbolPaths = new ArrayList<>(data.getData().importedSymbolPath.getEntries());
        List<String> domainModels = DOMAIN.isSelected(data.getData().cmd) ? List.of(data.getData().cmd.getOptionValues(DOMAIN.getCMD())) : List.of();

        // One symbol table for all OCL files, see UMLPFilePipeline#loadOCL
        OCL2JavaGeneratorTool.initSymtab(symbolPaths, domainModels);
        if (data.getSymbolPath() != null && data.getSymbolPath().toFile().exists()) {
          SymbolTableUtil.loadSymbolFile(data.getSymbolPath().toString());
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
            }
            ASTOCLCompilationUnit oCLAst = oclCompilationUnit.get();
            data.addOclCompilationUnit(oCLAst);
            OCL2JavaGeneratorTool.createSymtab(oCLAst);
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }
        }
        AbstractDecorator.hasOCL = true;
      }
    }
  };

  Pipeline<DataWithAst> createModelPipeline() {
    Pipeline<DataWithAst> pipe = new Pipeline<>();
    pipe.register("model_pipe_parse", model_pipe_parse);
    pipe.register("model_pipe_after_parse_trafos", model_pipe_after_parse_trafos);
    pipe.register("model_pipe_symbol_table", model_pipe_symbol_table);
    pipe.register("model_pipe_default_cocos", model_pipe_default_cocos);
    pipe.register("model_pipe_default_trafos", model_pipe_default_trafos);
    pipe.register("model_pipe_recreate_and_store_symbol_table",
        model_pipe_recreate_and_store_symbol_table);
    pipe.register("model_pipe_load_ocl", model_pipe_load_ocl);
    pipe.register("model_pipe_set_cd2gui_options", model_pipe_set_cd2gui_options);
    pipe.register("model_pipe_cd2gui_tool", model_pipe_cd2gui_tool);
    pipe.register("model_pipe_add_gem_id_trafo", model_pipe_add_gem_id_trafo);
    pipe.register("model_pipe_recreate_and_store_symbol_table",
        model_pipe_recreate_and_store_symbol_table);
    pipe.register("model_pipe_store_tags", model_pipe_store_tags);
    pipe.addFinally(data -> Reporting.flush(data.getAst()));
    return pipe;
  }
  
  public final IOperation.ISimpleOperation<DataWithAst> create_pipe_afterParseTrafo = data -> {
    data.getTool().runAfterParseTrafo(data.getAst());
  };
  public final IOperation.ISimpleOperation<DataWithAst> create_pipe_create_st = data -> {
    data.getTool().createSymbolTable(data.getAst());
  };
  public final IOperation.ISimpleOperation<DataWithAst> create_pipe_run_def_coco = data -> {
    data.getTool().runDefaultCoCos(data.getAst());
  };
  public final IOperation.ISimpleOperation<DataWithAst> create_pipe_run_def_trafos = data -> {
    data.getTool().runDefaultTrafos(data.getAst());
  };
  
  <T extends DataWithAst> Pipeline<T> createLoadModelPathPipe() {
    Pipeline<DataWithAst> pipe = new Pipeline<>();
    pipe.register("create_pipe_afterParseTrafo", create_pipe_afterParseTrafo);
    pipe.register("create_pipe_create_st", create_pipe_create_st);
    pipe.register("create_pipe_run_def_coco", create_pipe_run_def_coco);
    pipe.register("create_pipe_run_def_trafos", create_pipe_run_def_trafos);
    return (Pipeline<T>) pipe;
  }

  protected final String getReportingOnModelName(Path modelPath) {
    return modelPath.getFileName().toString()
        .replace(".", "__"); // TODO: Is this name unique? Should it be unique?
  }

  public static class Data implements DataWithAst {

    final UMLPPipelineData data;
    ASTCDCompilationUnit ast;
    List<ASTOCLCompilationUnit> oclCompilationUnits = new ArrayList<>();
    List<String> extraOptions;
    IUMLPArtifactScope symbolTable;
    Path symbolPath;
    
    public Data(UMLPPipelineData data) {
      this.data = data;
      this.extraOptions = new ArrayList<>();
    }

    @Override
    public UMLPPipelineData getData() {
      return data;
    }

    @Override
    public UMLPTool getTool() {
      return data.tool;
    }

    @Override
    public ASTCDCompilationUnit getAst() {
      return ast;
    }

    @Override
    public void setAst(ASTCDCompilationUnit ast) {
      this.ast = ast;
    }
    
    @Override
    public List<ASTOCLCompilationUnit> getOclCompilationUnits() {
      return oclCompilationUnits;
    }
    
    @Override
    public void addOclCompilationUnit(ASTOCLCompilationUnit oclCompUnit) {
      oclCompilationUnits.add(oclCompUnit);
    }
    
    @Override
    public List<String> getExtraOptions() {
      return extraOptions;
    }

    @Override
    public void addExtraOption(String key, String value) {
      extraOptions.add("-");
      extraOptions.add(key);
      extraOptions.add(value);
    }
    
    @Override
    public void setSymbolPath(Path path) {
      symbolPath = path;
    }
    
    @Override
    public Path getSymbolPath() {
      return symbolPath;
    }
    
    @Override
    public IUMLPArtifactScope getSymbolTable() {
      return symbolTable;
    }
    
    @Override
    public void setSymbolTable(IUMLPArtifactScope symbolTable) {this.symbolTable = symbolTable;}
    
  }

  public interface DataWithAst {

    UMLPPipelineData getData();

    UMLPTool getTool();

    ASTCDCompilationUnit getAst();

    void setAst(ASTCDCompilationUnit ast);
    
    List<ASTOCLCompilationUnit> getOclCompilationUnits();
    
    void addOclCompilationUnit(ASTOCLCompilationUnit oclCompUnit);

    List<String> getExtraOptions();

    void addExtraOption(String key, String value);
    
    Path getSymbolPath();
    
    void setSymbolPath(Path path);
    
    IUMLPArtifactScope getSymbolTable();
    
    void setSymbolTable(IUMLPArtifactScope symbolTable);
  }

  void addBuiltInTypes() {
    BuiltInTypes.addBuiltInTypes(UMLPMill.globalScope(), false);
  }

}
