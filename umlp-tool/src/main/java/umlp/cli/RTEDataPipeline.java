package umlp.cli;

import de.monticore.cd._symboltable.BuiltInTypes;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.tagging.tags.TagsMill;
import umlp.MillConf;
import umlp.TagRelated;
import umlp.UMLPMill;
import umlp.UMLPTool;
import umlp.UMLPToolOptions;
import umlp._symboltable.IUMLPArtifactScope;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.gui.gentypescript.config.ImportMapGenerator;
import umlp.generator.rte.data.CDGenerator4RTE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static umlp.UMLPToolOptions.INPUT;

public class RTEDataPipeline extends Pipeline<UMLPPipelineData> {

  public final IOperation<UMLPPipelineData> init;
  public final IOperation.ISimpleOperation2<UMLPPipelineData> addBuiltInTypes;
  public final Pipeline<DataWithAstAndGen> modelPipeline;
  public final IOperation<UMLPPipelineData> generateRTE;

  public RTEDataPipeline(IOperation<UMLPPipelineData> setupUMLPReports) {
    register("setupUMLPReports", setupUMLPReports);
    register("init", init = data -> {
      MillConf.switchTo(MillConf.TAGS, MillConf.UMLP);
      return true;
    });
    register("addBuiltInTypes", addBuiltInTypes = this::addBuiltInTypes);

    modelPipeline = createModelPipeline();
    register("generateRTE",
        IOperation.withDependantPipelines(generateRTE = data -> {
          List<Path> paths = Arrays.stream(data.cmd.getOptionValues(INPUT.getCMD())).map(Paths::get)
              .collect(Collectors.toList());
          for (Path cdPath : paths) {
            modelPipeline.doWork(new Data(data, cdPath));
          }
          return true;
        }, Collections.singletonList(modelPipeline)));
  }

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_parse = data -> {
    ASTCDCompilationUnit ast = data.getTool().parse(data.getModelPath().toString());
    data.setAst(ast);
  };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_after_parse_trafos =
      data -> data.getTool().runAfterParseTrafo(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_create_symbol_table =
      data -> data.getTool().createSymbolTable(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_default_trafos =
      data -> data.getTool().runDefaultTrafos(data.getAst());

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_recreate_symbol_table =
      data -> {
    IUMLPArtifactScope symTab = data.getTool().recreateSymbolTable(data.getAst());
    data.setSymbolTable(symTab);
  };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> storeTags =  data ->
    TagRelated.doStoreTagStereos(data.getAst(), data.getData().tool, data.getSymbolTable(), data.getData().cmd);

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_init_gen_setup = data -> {
    GeneratorSetup genSetup = new GeneratorSetup();
    // set output directory
    genSetup.setOutputDirectory(data.getData().outDir);
    // set hwc directories
    genSetup.setHandcodedPath(data.getData().hwcPath);
    // set template path
    genSetup.setAdditionalTemplatePaths(
        data.getData().fpPath.getEntries().stream().map(p -> p.toFile())
            .collect(Collectors.toList()));
    data.setGenSetup(genSetup);
  };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_init_glex = data -> {
    GlobalExtensionManagement glex =
        data.getTool().createGlex(data.getTool().getModuleConfigTemplates(data.getData().cmd));
    data.getGenSetup().setGlex(glex);
  };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_init_cd4c =
      data -> {
        CD4C.init(data.getGenSetup());
        CD4C.getInstance().setDeferredTemplateEvaluation(
            UMLPToolOptions.templateSourceMapping(data.getData().cmd).isEnabled());
      };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_init_gen_config =
      data -> {
        // Get all ASTGUIComponents
        data.getGenSetup().getGlex().setGlobalValue("UMLPGeneratorConfig",
            UMLPGeneratorConfig.getInstance().initFromCmd(data.getData().getCmd()));
      };

  public final IOperation.ISimpleOperation<DataWithAstAndGen> model_pipe_generate = data -> {
    Optional<String> tslib = UMLPGeneratorConfig.getInstance().getTSLib();
    if (tslib.isPresent()) {
      MillConf.switchTo(MillConf.GUIDSL, MillConf.UMLP);
      ImportMapGenerator importMapGenerator = new ImportMapGenerator(data.getGenSetup());
      importMapGenerator.generate(tslib.get(), data.getAst());
    }

    MillConf.switchTo(MillConf.TAGS, MillConf.UMLP);
    // Get all ASTGUIComponents
    CDGenerator4RTE javaGenerator =
        new CDGenerator4RTE(data.getGenSetup(), UMLPGeneratorConfig.getInstance());
    String javaConfigTemplate =
        data.getData().cmd.getOptionValue("ct", UMLPTool.DEFAULT_CONFIGTEMPLATE_RTE2JAVA);
    javaGenerator.generate(data.getAst(), javaConfigTemplate);
  };

  Pipeline<DataWithAstAndGen> createModelPipeline() {
    Pipeline<DataWithAstAndGen> pipe = new Pipeline<>();
    pipe.register("model_pipe_parse", model_pipe_parse);
    pipe.register("model_pipe_after_parse_trafos", model_pipe_after_parse_trafos);
    pipe.register("model_pipe_create_symbol_table", model_pipe_create_symbol_table);
    pipe.register("model_pipe_default_trafos", model_pipe_default_trafos);
    pipe.register("model_pipe_recreate_symbol_table", model_pipe_recreate_symbol_table);
    pipe.register("storeTags", storeTags);
    pipe.register("model_pipe_init_gen_setup", model_pipe_init_gen_setup);
    pipe.register("model_pipe_init_glex", model_pipe_init_glex);
    pipe.register("model_pipe_init_cd4c", model_pipe_init_cd4c);
    pipe.register("model_pipe_init_gen_config", model_pipe_init_gen_config);
    pipe.register("model_pipe_generate", model_pipe_generate);

    pipe.addFinally(data -> Reporting.flush(data.getAst()));
    return pipe;
  }

  public static class Data implements DataWithAstAndGen {

    final UMLPPipelineData data;
    final Path modelPath;
    ASTCDCompilationUnit ast;
    GeneratorSetup genSetup;
    IUMLPArtifactScope symbolTable;

    public Data(UMLPPipelineData data, Path modelPath) {
      this.data = data;
      this.modelPath = modelPath;
    }

    @Override
    public UMLPPipelineData getData() {
      return data;
    }

    @Override
    public Path getModelPath() {
      return modelPath;
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
    public GeneratorSetup getGenSetup() {
      return genSetup;
    }

    @Override
    public void setGenSetup(GeneratorSetup genSetup) {
      this.genSetup = genSetup;
    }

    @Override
    public IUMLPArtifactScope getSymbolTable() {
      return symbolTable;
    }

    @Override
    public void setSymbolTable(IUMLPArtifactScope symbolTable) {this.symbolTable = symbolTable;}
  }

  public interface DataWithAstAndGen {

    UMLPPipelineData getData();

    Path getModelPath();

    UMLPTool getTool();

    ASTCDCompilationUnit getAst();

    void setAst(ASTCDCompilationUnit ast);

    GeneratorSetup getGenSetup();

    void setGenSetup(GeneratorSetup genSetup);

    IUMLPArtifactScope getSymbolTable();

    void setSymbolTable(IUMLPArtifactScope symbolTable);
  }

  void addBuiltInTypes() {
    BuiltInTypes.addBuiltInTypes(UMLPMill.globalScope(), false);
  }
}
