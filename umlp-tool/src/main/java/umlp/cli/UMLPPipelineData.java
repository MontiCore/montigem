package umlp.cli;

import de.monticore.io.paths.MCPath;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import umlp.GUIDSLReports;
import umlp.UMLPReports;
import umlp.UMLPTool;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;


/**
 * A data class for holding the state of a tool run
 */
public class UMLPPipelineData {
  // final, initial values
  final CommandLine cmd;
  final Options cmdOptions;
  final UMLPTool tool;

  public UMLPPipelineData(CommandLine cmd, Options cmdOptions, UMLPTool tool) {
    this.cmd = cmd;
    this.cmdOptions = cmdOptions;
    this.tool = tool;
  }

  // values computed during a run
  File inputFile, outDir, reportDir;
  Function<Path, Path> reportPathOutput;

  MCPath importedSymbolPath = new MCPath(),
      modelPath = new MCPath(),
      hwcPath = new MCPath(),
      fpPath = new MCPath(),
      importsPath = new MCPath();

  public UMLPReports umlp_reportManagerFactory = null;
  public GUIDSLReports guidsl_reportManagerFactory = null;

  public CommandLine getCmd() {
    return cmd;
  }

  public Options getCmdOptions() {
    return cmdOptions;
  }

  /**
   * An MCPath keeps a class loader per entry, which holds a jar of the path open until it is closed.
   * The tool's class loader stays open between builds, so the paths must be closed after each run.
   */
  public void closePaths() {
    importedSymbolPath.close();
    modelPath.close();
    hwcPath.close();
    fpPath.close();
    importsPath.close();
  }

  // At some point: allow further extensibility, cf. Gradle Extensible

}
