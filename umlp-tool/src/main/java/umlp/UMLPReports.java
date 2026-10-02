/* (c) https://github.com/MontiCore/monticore */
package umlp;

import de.monticore.generating.templateengine.reporting.commons.AReporter;
import de.monticore.generating.templateengine.reporting.commons.ASTNodeIdentHelper;
import de.monticore.generating.templateengine.reporting.commons.ReportManager;
import de.monticore.generating.templateengine.reporting.commons.ReportingRepository;
import de.monticore.generating.templateengine.reporting.reporter.*;
import de.monticore.io.paths.MCPath;
import de.monticore.symboltable.serialization.json.JsonElement;
import umlp._visitor.UMLPTraverser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Initializes and provides the set of reports desired for UMLP to the
 * reporting framework.
 */

public class UMLPReports implements ReportManager.ReportManagerFactory {


  protected final JsonElement jsonElement;
  protected String outputDirectory;

  protected String reportDirectory;

  protected MCPath handwrittenPath;
  protected Function<Path, Path> reportPathOutput;
  protected List<AReporter> reporters = new ArrayList<>();

  protected ReportLevel reportLevel = ReportLevel.ALL;

  // protected MCPath templatePath;

  /**
   * Constructor for de.monticore.MontiCoreReports
   */
  public UMLPReports(
      String outputDirectory,
      String reportDirectory,
      Function<Path, Path> reportPathOutput,
      MCPath handwrittenPath, JsonElement jsonElement) {
    this.outputDirectory = outputDirectory;
    this.reportDirectory = reportDirectory;
    this.handwrittenPath = handwrittenPath;
    this.reportPathOutput = reportPathOutput;
    this.jsonElement = jsonElement;
    // this.templatePath = templatePath;
  }

  public void setReportLevel(ReportLevel reportLevel) {
    this.reportLevel = reportLevel;
  }

  /**
   * @see ReportManager.ReportManagerFactory#provide(String)
   */
  @Override
  public ReportManager provide(String modelName) {
    String lowerCaseName = modelName.toLowerCase();

    ReportingRepository repository = new ReportingRepository(new ASTNodeIdentHelper());


    ReportManager reports = new ReportManager(this.reportDirectory);

    UMLPTraverser traverserSummary = UMLPMill.inheritanceTraverser();
    SummaryReporter summaryReporter = new SummaryReporter(this.reportDirectory, lowerCaseName, repository,
        traverserSummary);
    GeneratedFilesReporter generatedFilesReporter = new GeneratedFilesReporter(this.reportDirectory, lowerCaseName,
        repository);
    HandWrittenCodeReporter handWrittenCodeReporter = new HandWrittenCodeReporter(this.reportDirectory, lowerCaseName,
        repository);
    TemplatesReporter templatesReporter = new TemplatesReporter(this.reportDirectory, lowerCaseName, repository);
    HookPointReporter hookPointReporter = new HookPointReporter(this.reportDirectory, lowerCaseName, repository);
    InstantiationsReporter instantiationsReporter = new InstantiationsReporter(this.reportDirectory, lowerCaseName);
    VariablesReporter variablesReporter = new VariablesReporter(this.reportDirectory, lowerCaseName);
    DetailedReporter detailedReporter = new DetailedReporter(this.reportDirectory, lowerCaseName, repository);
    TemplateTreeReporter templateTreeReporter = new TemplateTreeReporter(this.reportDirectory, lowerCaseName);
    InvolvedFilesReporter involvedFilesReporter = new InvolvedFilesReporter(this.reportDirectory);

    UMLPTraverser traverserNodeTree = UMLPMill.inheritanceTraverser();
    NodeTreeReporter nodeTreeReporter = new NodeTreeReporter(this.reportDirectory, lowerCaseName, repository,
        traverserNodeTree);
    NodeTreeDecoratedReporter nodeTreeDecoratedReporter = new NodeTreeDecoratedReporter(this.reportDirectory,
        lowerCaseName, repository, traverserNodeTree);
    UMLPTraverser traverserNodeType = UMLPMill.inheritanceTraverser();
    NodeTypesReporter nodeTypesReporter = new NodeTypesReporter(this.reportDirectory, lowerCaseName, traverserNodeType);
    TransformationReporter transformationReporter = new TransformationReporter(this.reportDirectory, lowerCaseName,
        repository);
    ArtifactGmlReporter artifactGmlReporter = new ArtifactGmlReporter(this.reportDirectory, lowerCaseName);
    ArtifactGVReporter artifactGVReporter = new ArtifactGVReporter(this.reportDirectory, lowerCaseName);
    SuccessfulReporter successfulReporter = new SuccessfulReporter(this.reportDirectory, lowerCaseName);
    IncGenGradleReporter incGenGradleReporter = new IncGenGradleReporter(this.reportDirectory, reportPathOutput,
        lowerCaseName);
    StatisticsReporter statisticsReporter = new StatisticsReporter(jsonElement, "UMLP_JAR_JSON", this.reportDirectory, modelName, repository, traverserNodeTree);

    if (reportLevel == ReportLevel.INCGEN) {
      reports.addReportEventHandler(incGenGradleReporter);
      reporters.add(incGenGradleReporter);
      return reports;
    }

    reports.addReportEventHandler(summaryReporter);
    reports.addReportEventHandler(generatedFilesReporter);
    reports.addReportEventHandler(handWrittenCodeReporter);
    reports.addReportEventHandler(templatesReporter);
    reports.addReportEventHandler(hookPointReporter);
    reports.addReportEventHandler(instantiationsReporter);
    reports.addReportEventHandler(variablesReporter);
    reports.addReportEventHandler(detailedReporter);
    reports.addReportEventHandler(templateTreeReporter);
    reports.addReportEventHandler(involvedFilesReporter);
    reports.addReportEventHandler(nodeTreeReporter);
    reports.addReportEventHandler(nodeTreeDecoratedReporter);
    reports.addReportEventHandler(nodeTypesReporter);
    reports.addReportEventHandler(transformationReporter);
    reports.addReportEventHandler(artifactGmlReporter);
    reports.addReportEventHandler(artifactGVReporter);
    //reports.addReportEventHandler(new ODReporter(this.reportDirectory, lowerCaseName, repository));
    reports.addReportEventHandler(successfulReporter);
    reports.addReportEventHandler(incGenGradleReporter);
    reports.addReportEventHandler(statisticsReporter);

    reporters.add(summaryReporter);
    reporters.add(generatedFilesReporter);
    reporters.add(handWrittenCodeReporter);
    reporters.add(templatesReporter);
    reporters.add(hookPointReporter);
    reporters.add(instantiationsReporter);
    reporters.add(variablesReporter);
    reporters.add(detailedReporter);
    reporters.add(templateTreeReporter);
    reporters.add(involvedFilesReporter);
    reporters.add(nodeTreeReporter);
    reporters.add(nodeTreeDecoratedReporter);
    reporters.add(nodeTypesReporter);
    reporters.add(transformationReporter);
    reporters.add(artifactGmlReporter);
    reporters.add(artifactGVReporter);
    //reporters.add(new ODReporter(this.reportDirectory, lowerCaseName, repository));
    reporters.add(successfulReporter);
    reporters.add(incGenGradleReporter);
    reporters.add(statisticsReporter);

    return reports;
  }

  public void close() {
    for (AReporter reporter : reporters) {
      reporter.closeFile();
    }
  }
}
