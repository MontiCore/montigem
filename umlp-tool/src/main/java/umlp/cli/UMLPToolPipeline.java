package umlp.cli;

import de.monticore.generating.templateengine.reporting.Reporting;
import de.se_rwth.commons.logging.Log;
import org.apache.commons.cli.CommandLine;
import umlp.UMLPToolOptions;
import umlp.UMLPReports;
import umlp.UMLPTool;
import umlp.cli.common.IOperation;
import umlp.cli.common.Pipeline;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static umlp.UMLPToolOptions.CD2GUI;
import static umlp.UMLPToolOptions.DEBUG;
import static umlp.UMLPToolOptions.GUIRTETypes;
import static umlp.UMLPToolOptions.GUIVariability;
import static umlp.UMLPToolOptions.HELP;
import static umlp.UMLPToolOptions.HWC_PATH;
import static umlp.UMLPToolOptions.IMPORTS_PATH;
import static umlp.UMLPToolOptions.INPUT;
import static umlp.UMLPToolOptions.MODEl_PATH;
import static umlp.UMLPToolOptions.OUTPUT;
import static umlp.UMLPToolOptions.PATH;
import static umlp.UMLPToolOptions.REPORTS;
import static umlp.UMLPToolOptions.REPORT_BASE;
import static umlp.UMLPToolOptions.TEST_INFRASTRUCTURE;
import static umlp.UMLPToolOptions.TMPL_PATH;

/**
 * Set-up for the tool-pipeline.
 * The options CD2GUI, GuiVar, UMLP (from a .umlp file), and GUI are moved into separate pipelines
 * (each defined in their own files)
 */
public class UMLPToolPipeline extends Pipeline<UMLPPipelineData> {

  // A reuseable operation, showing the help
  public IOperation<UMLPPipelineData> showHelp;

  // a reuseable operation, setting up the UMLPReports
  public IOperation<UMLPPipelineData> setupUMLPReports;

  public final IOperation<UMLPPipelineData> isUMLPFileCheck = data -> {
    if (!data.inputFile.getName().endsWith(".umlp")) {
      Log.warn("option -cd2gui used without a classdiagram umlp file as input");
      return false;
    }
    return true;
  };

  // Next, the various operations are stored, such that they can be modified
  protected IOperation<UMLPPipelineData> initLog;
  protected IOperation<UMLPPipelineData> getInputFile;
  protected IOperation<UMLPPipelineData> loadPath;
  protected IOperation<UMLPPipelineData> importsPath;
  protected IOperation<UMLPPipelineData> loadModelPath;
  protected IOperation<UMLPPipelineData> loadHWCPath;
  protected IOperation<UMLPPipelineData> loadTmplPath;

  // Alternative pipeline actions
  protected RTEDataPipeline rteDataPipeline = null;
  protected CD2GUIPipeline cd2GUIPipeline = null;
  protected Pipeline<UMLPPipelineData> testInfrastructurePipeline = null;
  protected GuiVarPipeline guiVarPipeline = null;
  protected UMLPFilePipeline umlpFilePipeline = null;
  protected GuiPipeline guiPipeline = null;

  public UMLPToolPipeline(CommandLine cmd) {
    setupUMLPReports = data -> {
      UMLPReports reports = new UMLPReports(data.outDir.getPath(), data.reportDir.getPath(), data.reportPathOutput, data.hwcPath, data.tool.buildJsonData(data.cmd));
      reports.setReportLevel(UMLPToolOptions.reportLevel(data.cmd));
      data.umlp_reportManagerFactory = reports;
      Reporting.init(data.outDir.getPath(), data.reportDir.getPath(), data.umlp_reportManagerFactory);
      String report_on = data.inputFile.getName().replace(".", "__");   // TODO: Is this name unique? Should it be unique?
      Reporting.on(report_on);
      return true;
    };

    // A reuseable operation, showing the help and exiting
    if(cmd == null || !cmd.getArgList().isEmpty() || HELP.isSelected(cmd)) {
      this.register("showHelp", showHelp = data -> {
        org.apache.commons.cli.HelpFormatter formatter = new org.apache.commons.cli.HelpFormatter();
        formatter.setWidth(80);
        formatter.printHelp("UMLPTool", data.getCmdOptions());
        return false; // abort
      });
    }

    this.register("initLog", initLog = data -> {
      // set desired log level without modifying the "failQuick" option
      boolean enableFailQuick = Log.isFailQuickEnabled();
      if (DEBUG.isSelected(data.getCmd())) {
        Log.initDEBUG();
      } else {
//        Log.initWARN(); // TODO: modify, not init! (ErrorHook/System#exit behavior) - monticore/monticore#4317
      }
      Log.enableFailQuick(enableFailQuick);
      return true;
    });

    this.register("getInputFile", getInputFile = data -> {
      if (!INPUT.isSelected(data.getCmd())) {
        Log.error("0xE0000 Indicating an input file with the option 'i' is mandatory");
      }
      data.inputFile = new File(INPUT.getValue(data.getCmd()));
      data.outDir = new File(OUTPUT.getValue(data.cmd, UMLPTool.DEFAULT_OUT));

      data.reportDir = new File(REPORTS.getValue(data.cmd, UMLPTool.DEFAULT_REPORT_OUT));
      String report_base = REPORT_BASE.getValue(data.cmd);
      if (!report_base.isEmpty()) {
        Path base_dir = Paths.get(report_base).toAbsolutePath();
        // Only relativize this path if both share a root
        data.reportPathOutput = p -> Objects.equals(p.getRoot(), base_dir.getRoot()) ? base_dir.relativize(p.toAbsolutePath()) : p.toAbsolutePath();
      } else {
        data.reportPathOutput = Path::toAbsolutePath;
      }
      return true;
    });

    if (PATH.isSelected(cmd)) {
      this.register("loadPath", loadPath = data -> {
        for (String v : data.cmd.getOptionValues(PATH.getCMD())) {
          data.importedSymbolPath.addEntry(Paths.get(v));
        }
        return true;
      });
    }

    if (IMPORTS_PATH.isSelected(cmd)) {
      this.register("loadImportsPath", importsPath = data -> {
        for (String v : data.cmd.getOptionValues(IMPORTS_PATH.getCMD())) {
          data.importsPath.addEntry(Paths.get(v));
        }
        return true;
      });
    }

    if (MODEl_PATH.isSelected(cmd)) {
      this.register("loadModelPath", loadModelPath = data -> {
        for (String v : data.cmd.getOptionValues(MODEl_PATH.getCMD())) {
          data.modelPath.addEntry(Paths.get(v));
          data.importedSymbolPath.addEntry(Paths.get(v));
        }
        return true;
      });
    }

    if (HWC_PATH.isSelected(cmd)) {
      this.register("loadHWCPath", loadHWCPath = data -> {
        for (String v : data.cmd.getOptionValues(HWC_PATH.getCMD())) {
          data.hwcPath.addEntry(Paths.get(v));
        }
        return true;
      });
    }

    if (TMPL_PATH.isSelected(cmd)) {
      this.register("loadTemplatePath", loadTmplPath = data -> {
        for (String v : data.cmd.getOptionValues(TMPL_PATH.getCMD())) {
          data.fpPath.addEntry(Paths.get(v));
        }
        return true;
      });
    }

    // Register the alternatives
    String inputValue = INPUT.getValue(cmd);

    var inputFile = new File(inputValue);
    if (GUIRTETypes.isSelected(cmd)) {
      rteDataPipeline = new RTEDataPipeline(setupUMLPReports);
      this.register(GUIRTETypes.name(), rteDataPipeline);
    }else if (CD2GUI.isSelected(cmd)) {
      cd2GUIPipeline = new CD2GUIPipeline(setupUMLPReports, isUMLPFileCheck);
      this.register(CD2GUI.name(), cd2GUIPipeline);
    } else if (TEST_INFRASTRUCTURE.isSelected(cmd)) {
      // Is configured via the config template UMLP2JavaTestInfrastructure
      testInfrastructurePipeline = new Pipeline<>();
      this.register(TEST_INFRASTRUCTURE.name(), testInfrastructurePipeline);
    } else if (GUIVariability.isSelected(cmd)) {
      guiVarPipeline = new GuiVarPipeline(setupUMLPReports);
      this.register(GUIVariability.name(), guiVarPipeline);
    } else if (inputFile.getName().endsWith(".umlp")) {
      umlpFilePipeline = new UMLPFilePipeline(setupUMLPReports);
      this.register(umlpFilePipeline);
    } else if (inputFile.getName().endsWith(".gui")) {
      guiPipeline = new GuiPipeline(setupUMLPReports);
      this.register(guiPipeline);
    } else {
      if(inputValue.isBlank()){
        Log.error("Option " + INPUT.getCMD() + " is not set!");
      }else{
        Log.error("Unhandled input file: " + inputFile);
      }
    }

    // Finally (for every option), close report managers
    this.addFinally(data -> {
      if (data.umlp_reportManagerFactory != null) {
        data.umlp_reportManagerFactory.close();
      }
    });
    this.addFinally(data -> {
      if (data.guidsl_reportManagerFactory != null) {
        data.guidsl_reportManagerFactory.close();
      }
    });

  }

  public Optional<CD2GUIPipeline> getCd2GUIPipeline() {
    return Optional.ofNullable(cd2GUIPipeline);
  }

  public Optional<GuiPipeline> getGuiPipeline() {
    return Optional.ofNullable(guiPipeline);
  }

  public Optional<GuiVarPipeline> getGuiVarPipeline() {
    return Optional.ofNullable(guiVarPipeline);
  }

  public Optional<RTEDataPipeline> getRteDataPipeline() {
    return Optional.ofNullable(rteDataPipeline);
  }

  public Optional<Pipeline<UMLPPipelineData>> getTestInfrastructurePipeline() {
    return Optional.ofNullable(testInfrastructurePipeline);
  }

  public Optional<UMLPFilePipeline> getUmlpFilePipeline() {
    return Optional.ofNullable(umlpFilePipeline);
  }

  public void warn(String msg) {
    Log.warn(msg);
  }
}
