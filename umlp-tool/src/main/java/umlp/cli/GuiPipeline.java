package umlp.cli;

import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._symboltable.GUIDSLSymbols2Json;
import de.monticore.guidsl._symboltable.IGUIDSLArtifactScope;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;
import org.apache.commons.cli.CommandLine;
import umlp.UMLPToolOptions;
import umlp.GUIDSLReports;
import umlp.MillConf;
import umlp.TagRelated;
import umlp.UMLPTool;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.genhtml.GUI2HtmlGenerator;
import umlp.generator.gui.genjava.AppRouterGenerator;
import umlp.generator.gui.genjava.GUI2JavaGenerator;
import umlp.generator.gui.gentypescript.GUI2TSGenerator;
import umlp.generator.gui.gentypescript.config.GUI2ConfigGenerator;
import umlp.generator.gui.gentypescript.config.ImportMapGenerator;
import umlp.generator.gui.gentypescript.config.PackageRoute;
import umlp.generator.gui.gentypescript.printer.TSImportMap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static umlp.UMLPToolOptions.DOMAIN;
import static umlp.UMLPToolOptions.GUI_CREATE;
import static umlp.UMLPToolOptions.INPUT;
import static umlp.UMLPToolOptions.SYMTAB;

public class GuiPipeline extends Pipeline<UMLPPipelineData> {

  public final IOperation<UMLPPipelineData> init;
  public final IOperation<UMLPPipelineData> loadModelPath;
  public final Pipeline<DataWithGenSetupAndModel> modelPipeline;
  public final Pipeline<DataWithGenSetupAndRouting> configurationPipeline;

  public GuiPipeline(IOperation<UMLPPipelineData> setupUMLPReports) {
    register("setupUMLPReports", setupUMLPReports);
    register("init", init = data -> {
      // Prepare the SingletonLookup
      MillConf.switchTo(MillConf.TAGS, MillConf.UMLP, MillConf.GUIDSL);
      TagRelated.initLookup(data.importedSymbolPath);
      UMLPTool.initGUIGlobalScope(data.importedSymbolPath);
      TSImportMap.INSTANCE.init(data.importedSymbolPath);
      return true;
    });

    modelPipeline = createModelPipeline();
    configurationPipeline = createConfigurationPipeline();

    register("processModels",
        IOperation.withDependantPipelines(loadModelPath = data -> {
          String[] modelPathStrings = data.cmd.getOptionValues(INPUT.getCMD());
          List<Path> modelPaths =
              Arrays.stream(modelPathStrings).map(Paths::get).collect(Collectors.toList());
          Map<String, PackageRoute> routeMap = new HashMap<>();

          List<ASTGUICompilationUnit> asts = new ArrayList<>();
          for (Path modelPath : modelPaths) {
            DataWithGenSetupAndModel modelData = new Data(data, modelPath);
            boolean modelPipeResult = modelPipeline.doWork(modelData);

            if (modelPipeResult && modelData.getAst() != null) {
              ASTGUICompilationUnit ast = modelData.getAst();
              asts.add(ast);
              if (ast.getGUIComponentDeclaration().isPage()) {
                if (!routeMap.containsKey(ast.getPackage())) {
                  routeMap.put(ast.getPackage(), new PackageRoute(
                      ast.getMCPackageDeclaration().getMCQualifiedName().getPartsList()));
                }
                routeMap.get(ast.getPackage()).add(ast);
              }
            }
          }
          configurationPipeline.doWork(new GenConfigData(data, routeMap, asts));
          return true;
        }, List.of(modelPipeline, configurationPipeline)));

    addFinally(data -> {
      TagRelated.postInfo();
      Reporting.flush(new ASTGUICompilationUnit());
    });
  }

  protected final String getReportingOnModelName(Path modelPath) {
    return modelPath.getFileName().toString().replace(".", "__");
  }

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_init_reporting =
      data -> {
        GUIDSLReports reports =
            new GUIDSLReports(data.getData().outDir.getPath(), data.getData().reportDir.getPath(),
                data.getData().reportPathOutput, data.getData().hwcPath);
        reports.setReportLevel(UMLPToolOptions.reportLevel(data.getData().cmd));
        data.getData().guidsl_reportManagerFactory = reports;
        Reporting.init(data.getData().outDir.getPath(), data.getData().reportDir.getPath(),
            data.getData().guidsl_reportManagerFactory);
        Reporting.on(getReportingOnModelName(data.getPath()));
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_load_ast_with_st =
      data -> {
        Path modelPath = data.getPath();
        Reporting.reportParseInputFile(modelPath.toAbsolutePath(),
            getReportingOnModelName(modelPath));
        var e = data.getTool().createAstWithSymtab(modelPath);
        ASTGUICompilationUnit astGui = e.getKey();
        IGUIDSLArtifactScope symbolTable = e.getValue();
        data.setAst(astGui);
        data.setSymbolTable(symbolTable);
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_store_st = data -> {
    // print or store symbol table
    if (SYMTAB.isSelected(data.getCMD())) {
      String s = SYMTAB.getValue(data.getCMD());
      IGUIDSLArtifactScope symbolTable = data.getSymbolTable();
      if (null != s) {
        // store symbol table to passed file
        Path symbolPath = Paths.get(
            s + File.separator + Names.getPathFromPackage(symbolTable.getPackageName())
                + File.separator + symbolTable.getName() + ".guisym");
        JsonPrinter.disableIndentation();
        data.getTool().storeSymbols(symbolTable, symbolPath.toString());
      }
      else {
        //print (formatted!) symboltable to console
        GUIDSLSymbols2Json s2j = new GUIDSLSymbols2Json();
        JsonPrinter.enableIndentation();
        System.out.println(s2j.serialize(symbolTable));
      }
    }
  };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel>
      model_pipe_create_run_def_coco = data -> {
    data.getTool().runDefaultCoCos(data.getAst());
  };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_setup_generator =
      data -> {
        GeneratorSetup genSetup = new GeneratorSetup();
        // set output directory
        genSetup.setOutputDirectory(data.getData().outDir);
        // set hwc directories
        genSetup.setHandcodedPath(data.getData().hwcPath);
        // set template path
        genSetup.setAdditionalTemplatePaths(
            data.getData().fpPath.getEntries().stream().map(Path::toFile)
                .collect(Collectors.toList()));
        GlobalExtensionManagement glex =
            data.getTool().createGlex(data.getTool().getModuleConfigTemplates(data.getCMD()));
        genSetup.setGlex(glex);
        CD4C.init(genSetup);

        data.setGenSetup(genSetup);
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel>
      model_pipe_init_generator_util = data -> {
    List<String> domainModels = new ArrayList<>();
    if (DOMAIN.isSelected(data.getCMD())) {
      domainModels.addAll(Arrays.asList(data.getCMD().getOptionValues(DOMAIN.getCMD())));
    }

    List<String> createModels = new ArrayList<>();
    if (GUI_CREATE.isSelected(data.getCMD())) {
      createModels.addAll(Arrays.asList(data.getCMD().getOptionValues(GUI_CREATE.getCMD())));
    }

    GUIGeneratorUtil.getInstance().init(domainModels, createModels);
  };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_setup_glex_vars =
      data -> {
        // Get all ASTGUIComponents
        data.getGenSetup().getGlex().setGlobalValue("UMLPGeneratorConfig",
            UMLPGeneratorConfig.getInstance().initFromCmd(data.getCMD()));
        data.getGenSetup().getGlex().setGlobalValue("genHelper", GUIGeneratorUtil.getInstance());
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_java_generator =
      data -> {
        GUI2JavaGenerator javaGenerator = new GUI2JavaGenerator(data.getGenSetup());
        String javaConfigTemplate =
            data.getCMD().getOptionValue("ct_java", UMLPTool.DEFAULT_CONFIGTEMPLATE_GUI2JAVA);
        javaGenerator.generate(data.getAst(), javaConfigTemplate);
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_ts_generator =
      data -> {
        GUI2TSGenerator tsGenerator =
            new GUI2TSGenerator(data.getGenSetup(), data.getData().importsPath);
        tsGenerator.generate(data.getAst(), UMLPTool.DEFAULT_CONFIGTEMPLATE_GUI2TS);
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndModel> model_pipe_html_generator =
      data -> {
        GUI2HtmlGenerator htmlGenerator = new GUI2HtmlGenerator(data.getGenSetup());
        String htmlConfigTemplate =
            data.getCMD().getOptionValue("ct_html", UMLPTool.DEFAULT_CONFIGTEMPLATE_GUI2HTML);
        htmlGenerator.generate(data.getAst(), htmlConfigTemplate);
      };

  Pipeline<DataWithGenSetupAndModel> createModelPipeline() {
    Pipeline<DataWithGenSetupAndModel> pipe = new Pipeline<>();
    pipe.register("model_pipe_init_reporting", model_pipe_init_reporting);
    pipe.register("model_pipe_load_ast_with_st", model_pipe_load_ast_with_st);
    pipe.register("model_pipe_create_run_def_coco", model_pipe_create_run_def_coco);
    pipe.register("model_pipe_store_st", model_pipe_store_st);
    pipe.register("model_pipe_setup_generator", model_pipe_setup_generator);
    pipe.register("model_pipe_init_generator_util", model_pipe_init_generator_util);
    pipe.register("model_pipe_setup_glex_vars", model_pipe_setup_glex_vars);
    pipe.register("model_pipe_java_generator", model_pipe_java_generator);
    pipe.register("model_pipe_ts_generator", model_pipe_ts_generator);
    pipe.register("model_pipe_html_generator", model_pipe_html_generator);

    pipe.addFinally(data -> {
      // write reports
      Reporting.flush(data.getAst());
    });
    return pipe;
  }

  public final IOperation.ISimpleOperation<DataWithGenSetupAndRouting> config_pipe_setup_generator =
      data -> {
        //create routes file for every package and
        GeneratorSetup genSetup = new GeneratorSetup();
        // set output directory
        genSetup.setOutputDirectory(data.getData().outDir);
        // set hwc directories
        genSetup.setHandcodedPath(data.getData().hwcPath);
        // set template path
        genSetup.setAdditionalTemplatePaths(
            data.getData().fpPath.getEntries().stream().map(p -> p.toFile())
                .collect(Collectors.toList()));
        GlobalExtensionManagement glex =
            data.getTool().createGlex(data.getTool().getModuleConfigTemplates(data.getCMD()));
        genSetup.setGlex(glex);
        genSetup.getGlex().setGlobalValue("genHelper", GUIGeneratorUtil.getInstance());
        genSetup.setTracing(false);

        data.setGenSetup(genSetup);
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndRouting> config_pipe_init_modules =
      data -> {
        // Report InitModules
        data.getGenSetup().getNewTemplateController("umlp.generator.hook.InitModules")
            .includeArgs("umlp.generator.hook.InitModules",
                GUIDSLMill.stringLiteralBuilder().setSource("GuiPipelineSetup#configs").build(),
                Collections.singletonList(data.getGenSetup().getGlex()));
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndRouting> config_pipe_generate =
      data -> {
        Optional<String> domainName = UMLPGeneratorConfig.getInstance().getDomain();
        GUI2ConfigGenerator configGenerator = new GUI2ConfigGenerator(data.getGenSetup(), domainName);
        List<String> outputs = configGenerator.generateConfig(data.getRouting().values());
        for (String output : outputs) {
          Reporting.reportFileCreation(
              Paths.get(data.getData().outDir.getPath(), output).toString());
        }
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndRouting> import_map_pipe_generate =
      data -> {
        Optional<String> tslib = UMLPGeneratorConfig.getInstance().getTSLib();
        if (tslib.isPresent() && UMLPGeneratorConfig.getInstance().isGenerateImportMap()) {
          ImportMapGenerator importMapGenerator = new ImportMapGenerator(data.getGenSetup());
          importMapGenerator.generate(tslib.get(), data.getASTs());
        }
      };

  public final IOperation.ISimpleOperation<DataWithGenSetupAndRouting> model_pipe_router_generator =
      data -> {
        AppRouterGenerator routerGenerator = new AppRouterGenerator(data.getGenSetup());
        routerGenerator.generate(data.getASTs());
      };

  Pipeline<DataWithGenSetupAndRouting> createConfigurationPipeline() {
    Pipeline<DataWithGenSetupAndRouting> pipe = new Pipeline<>();
    pipe.register("config_pipe_setup_generator", config_pipe_setup_generator);
    pipe.register("config_pipe_init_modules", config_pipe_init_modules);
    pipe.register("config_pipe_generate", config_pipe_generate);
    pipe.register("import_map_pipe_generate", import_map_pipe_generate);
    pipe.register("model_pipe_router_generator", model_pipe_router_generator);
    return pipe;
  }

  /**
   * Check if a collection of paths has files with same names and report them as an error.
   * Models of MC languages generally require that the symbol declaration closest to a global scope
   * has the same name as the file. This means that files with the same name have symbols with the
   * same name. This can cause problems if the symbols represent GUI components, as current
   * generator implementation does not allow the same name for components. E.g. foo.bar.MyPage and
   * other.MyPage may result in routing collisions or component usage collisions, depending on the
   * situation.
   *
   * @param paths Paths to check for duplicates.
   */
  protected void checkSameFilenames(Collection<Path> paths) {
    Map<String, List<Path>> filenameMap = new HashMap<>();
    for (Path path : paths) {
      String filename = path.getFileName().toString();
      int lastDotIndex = filename.lastIndexOf('.');
      String filenameNoExt = (lastDotIndex == -1) ? filename : filename.substring(0, lastDotIndex);
      filenameMap.computeIfAbsent(filenameNoExt, x -> new ArrayList<>()).add(path);
    }

    StringBuilder sb = new StringBuilder(
        "0x00023 Found files with the same names," + " which is likely to cause issues:\n");
    boolean error = false;
    for (Map.Entry<String, List<Path>> entry : filenameMap.entrySet()) {
      if (entry.getValue().size() > 1) {
        error = true;
        for (Path p : entry.getValue()) {
          sb.append(p.toString());
          sb.append("\n");
        }
        sb.append("\n");
      }
    }
    if (error) {
      sb.append("Please check if files were named properly, rename the files and"
          + " set matching component names.");
      Log.error(sb.toString());
    }
  }

  public static class GenConfigData implements DataWithGenSetupAndRouting {

    final UMLPPipelineData data;
    final Map<String, PackageRoute> routing;
    final List<ASTGUICompilationUnit> asts;
    GeneratorSetup genSetup;

    public GenConfigData(UMLPPipelineData data, Map<String, PackageRoute> routing, List<ASTGUICompilationUnit> asts) {
      this.data = data;
      this.routing = routing;
      this.asts = asts;
    }

    public UMLPTool getTool() {
      return this.data.tool;
    }

    public CommandLine getCMD() {
      return this.data.cmd;
    }

    public UMLPPipelineData getData() {
      return data;
    }

    public Map<String, PackageRoute> getRouting() {
      return routing;
    }

    public List<ASTGUICompilationUnit> getASTs() {
      return asts;
    }

    public GeneratorSetup getGenSetup() {
      return genSetup;
    }

    public void setGenSetup(GeneratorSetup genSetup) {
      this.genSetup = genSetup;
    }
  }

  public static class Data implements DataWithGenSetupAndModel {

    final UMLPPipelineData data;
    final Path path;
    ASTGUICompilationUnit ast;
    IGUIDSLArtifactScope symbolTable;
    GeneratorSetup genSetup;

    public Data(UMLPPipelineData data, Path path) {
      this.data = data;
      this.path = path;
    }

    public UMLPTool getTool() {
      return this.data.tool;
    }

    public CommandLine getCMD() {
      return this.data.cmd;
    }

    public UMLPPipelineData getData() {
      return data;
    }

    public Path getPath() {
      return path;
    }

    public IGUIDSLArtifactScope getSymbolTable() {
      return symbolTable;
    }

    public ASTGUICompilationUnit getAst() {
      return ast;
    }

    public GeneratorSetup getGenSetup() {
      return genSetup;
    }

    public void setAst(ASTGUICompilationUnit ast) {
      this.ast = ast;
    }

    public void setSymbolTable(IGUIDSLArtifactScope symbolTable) {
      this.symbolTable = symbolTable;
    }

    public void setGenSetup(GeneratorSetup genSetup) {
      this.genSetup = genSetup;
    }
  }

  public interface DataWithGenSetupAndModel extends DataWithGenSetup {

    Path getPath();

    ASTGUICompilationUnit getAst();

    void setAst(ASTGUICompilationUnit ast);

    IGUIDSLArtifactScope getSymbolTable();

    void setSymbolTable(IGUIDSLArtifactScope symbolTable);
  }

  public interface DataWithGenSetupAndRouting extends DataWithGenSetup {

    Map<String, PackageRoute> getRouting();

    List<ASTGUICompilationUnit> getASTs();
  }

  public interface DataWithGenSetup {

    UMLPPipelineData getData();

    UMLPTool getTool();

    CommandLine getCMD();

    GeneratorSetup getGenSetup();

    void setGenSetup(GeneratorSetup genSetup);
  }

}
