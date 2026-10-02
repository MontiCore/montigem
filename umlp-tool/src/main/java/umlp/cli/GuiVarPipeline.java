package umlp.cli;

import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._prettyprint.GUIDSLFullPrettyPrinter;
import de.monticore.io.FileReaderWriter;
import de.monticore.prettyprint.IndentPrinter;
import de.se_rwth.commons.logging.Log;
import umlp.UMLPToolOptions;
import umlp.GUIDSLReports;
import umlp.MillConf;
import umlp.UMLPTool;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;
import umlp.generator.gui.guivariability.GUIVariabilityHandler;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static umlp.UMLPToolOptions.INPUT;

public class GuiVarPipeline extends Pipeline<UMLPPipelineData> {

  public final IOperation<UMLPPipelineData> init;
  public final IOperation<UMLPPipelineData> processModels;
  public final Pipeline<DataWithUnprocessedModels> modelPreprocessingPipeline;
  public final Pipeline<DataWithModels> modelPipeline;

  public GuiVarPipeline(IOperation<UMLPPipelineData> setupUMLPReports) {
    register("setupUMLPReports", setupUMLPReports);
    register("init", init = data -> {
      GUIDSLReports reports =
          new GUIDSLReports(data.outDir.getPath(), data.reportDir.getPath(), data.reportPathOutput,
              data.hwcPath);
      reports.setReportLevel(UMLPToolOptions.reportLevel(data.cmd));
      data.guidsl_reportManagerFactory = reports;
      Reporting.init(data.outDir.getPath(), data.reportDir.getPath(),
          data.guidsl_reportManagerFactory);
      MillConf.switchTo(MillConf.GUIDSL);
      // GUIDSLMill.init();
      return true;
    });

    modelPreprocessingPipeline = createParsePipeline();
    modelPipeline = createModelPipeline();

    register("processModels",
        IOperation.withDependantPipelines(processModels = data -> {
          List<Path> inputs = Arrays.stream(data.cmd.getOptionValues(INPUT.getCMD())).map(Path::of)
              .collect(Collectors.toList());
          LinkedList<ASTGUICompilationUnit> baseASTs = new LinkedList<>();
          LinkedList<ASTGUICompilationUnit> variationASTs = new LinkedList<>();

          for (Path model : inputs) {
            modelPreprocessingPipeline.doWork(
                new PreprocessingData(data, model, baseASTs, variationASTs));
          }
          modelPipeline.doWork(new Data(data, baseASTs, variationASTs));

          return true;
        }, List.of(modelPreprocessingPipeline, modelPipeline)));
  }

  Pipeline<DataWithUnprocessedModels> createParsePipeline() {
    Pipeline<DataWithUnprocessedModels> pipe = new Pipeline<>();
    pipe.register("init_reporting", init_reporting);
    pipe.register("parse_model", parse_model);
    pipe.register("categorize_model", categorize_model);
    return pipe;
  }

  protected final String getReportingOnModelName(Path modelPath) {
    return modelPath.getFileName().toString().replace(".", "__");
  }

  public final IOperation.ISimpleOperation<DataWithUnprocessedModels> init_reporting = data -> {
    Reporting.on(getReportingOnModelName(data.getModelPath()));
  };

  public final IOperation.ISimpleOperation<DataWithUnprocessedModels> parse_model = data -> {
    ASTGUICompilationUnit parsedModel = UMLPTool.parseGUI(data.getModelPath().toString());
    Reporting.reportParseInputFile(data.getModelPath().toAbsolutePath(),
        getReportingOnModelName(data.getModelPath()));
    data.setParsedAST(parsedModel);
  };

  public final IOperation.ISimpleOperation<DataWithUnprocessedModels> categorize_model = data -> {
    if (data.getData().tool.isGUIVariabilityFile(data.getParsedAST())) {
      data.getVariationASTs().add(data.getParsedAST());
    }
    else {
      data.getBaseASTs().add(data.getParsedAST());
    }
  };

  Pipeline<DataWithModels> createModelPipeline() {
    Pipeline<DataWithModels> pipe = new Pipeline<>();
    pipe.register("process_variation_asts", process_variation_asts);
    pipe.register("process_product_asts", process_product_asts);
    return pipe;
  }

  public final IOperation.ISimpleOperation<DataWithModels> process_variation_asts = data -> {
    for (ASTGUICompilationUnit variationAST : data.getVariationASTs()) {
      List<ASTGUICompilationUnit> adaptedAST = data.getBaseASTs().stream().filter(
              ast -> ast.getComponentName().equals(
                  variationAST.getGUIComponentDeclaration().getGUIComponentAdaptation().getTarget()))
          .collect(Collectors.toList());
      if (adaptedAST.size() != 1) {
        Log.error(
            "expected exactly one matching file to adapt with GUIVariability, but found multiple or none");
      }
      GUIVariabilityHandler deltaHandler = new GUIVariabilityHandler(adaptedAST.get(0));
      variationAST.accept(deltaHandler.getTraverser());
    }
  };

  public final IOperation.ISimpleOperation<DataWithModels> process_product_asts = data -> {
    for (ASTGUICompilationUnit productAST : data.getBaseASTs()) {
      if (data.getVariationASTs().stream().anyMatch(ast ->
          ast.getGUIComponentDeclaration().isPresentGUIComponentAdaptation()
              && ast.getGUIComponentDeclaration().getGUIComponentAdaptation().getTarget().equals(productAST.getComponentName())
      )) {
        Reporting.on(productAST.getComponentName() + "__gui");
        Path outputPath = data.getData().outDir.toPath()
            .resolve(        //file structure needs to reflect package declarations
                productAST.getMCPackageDeclaration().getMCQualifiedName().toString().replace(".", "/")
                    .concat("/" + productAST.getComponentName() + ".gui"));
        GUIDSLFullPrettyPrinter pp = new GUIDSLFullPrettyPrinter(new IndentPrinter(), true);
        FileReaderWriter.storeInFile(outputPath, pp.prettyprint(productAST));

        Reporting.reportFileCreation(outputPath.toString());
        Reporting.flush(productAST);
      }
    }
  };

  public static class PreprocessingData implements DataWithUnprocessedModels {

    final UMLPPipelineData data;
    final Path modelPath;
    final List<ASTGUICompilationUnit> baseASTs;
    final List<ASTGUICompilationUnit> variationASTs;
    ASTGUICompilationUnit parsedAST;

    public PreprocessingData(UMLPPipelineData data, Path modelPath,
        List<ASTGUICompilationUnit> baseASTs, List<ASTGUICompilationUnit> variationASTs) {
      this.data = data;
      this.modelPath = modelPath;
      this.baseASTs = baseASTs;
      this.variationASTs = variationASTs;
    }

    @Override
    public Path getModelPath() {
      return modelPath;
    }

    @Override
    public UMLPPipelineData getData() {
      return data;
    }

    @Override
    public List<ASTGUICompilationUnit> getBaseASTs() {
      return baseASTs;
    }

    @Override
    public List<ASTGUICompilationUnit> getVariationASTs() {
      return variationASTs;
    }

    @Override
    public ASTGUICompilationUnit getParsedAST() {
      return parsedAST;
    }

    public void setParsedAST(ASTGUICompilationUnit parsedAST) {
      this.parsedAST = parsedAST;
    }
  }

  public static class Data implements DataWithModels {

    final UMLPPipelineData data;
    final List<ASTGUICompilationUnit> baseASTs;
    final List<ASTGUICompilationUnit> variationASTs;

    public Data(UMLPPipelineData data, List<ASTGUICompilationUnit> baseASTs,
        List<ASTGUICompilationUnit> variationASTs) {
      this.data = data;
      this.baseASTs = baseASTs;
      this.variationASTs = variationASTs;
    }

    @Override
    public UMLPPipelineData getData() {
      return data;
    }

    @Override
    public List<ASTGUICompilationUnit> getBaseASTs() {
      return baseASTs;
    }

    @Override
    public List<ASTGUICompilationUnit> getVariationASTs() {
      return variationASTs;
    }
  }

  public interface DataWithModels {

    UMLPPipelineData getData();

    List<ASTGUICompilationUnit> getBaseASTs();

    List<ASTGUICompilationUnit> getVariationASTs();
  }

  public interface DataWithUnprocessedModels extends DataWithModels {

    Path getModelPath();

    ASTGUICompilationUnit getParsedAST();

    void setParsedAST(ASTGUICompilationUnit parsedAST);
  }
}
