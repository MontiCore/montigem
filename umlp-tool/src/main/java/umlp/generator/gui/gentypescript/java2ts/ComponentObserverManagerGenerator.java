/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript.java2ts;

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.symbols.basicsymbols._ast.ASTDiagram;
import de.monticore.symbols.basicsymbols._symboltable.DiagramSymbol;
import umlp.generator.AbstractDecorator;
import umlp.generator.Filter;
import umlp.generator.util.UMLPSymbolTables;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ComponentObserverManagerGenerator {

  protected GeneratorSetup generatorSetup;
  protected GeneratorEngine generatorEngine;

  public ComponentObserverManagerGenerator(GeneratorSetup generatorSetup) {
    this.generatorSetup = generatorSetup;
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    generatorSetup.setTracing(false);
  }

  public void generate(ASTCDCompilationUnit ast) {
    boolean reportingWasEnabled = Reporting.isEnabled();
    String reportedModel = Reporting.off();
    String cdName = ast.getCDDefinition().getName();
    Path targetPath = Paths.get(cdName + "ComponentObserverManager.ts");
   LinkedHashMap<String, String> domainClassNamesToPackageMap = new LinkedHashMap<String, String>();

    getConcreteDomainClasses(ast.getCDDefinition()).forEach(domainClass ->
        domainClassNamesToPackageMap.put(domainClass.getName(), getCorrectPackage(domainClass)));

    var extensionDiamond = UMLPSymbolTables.getExtensionDiamond(ast.getCDDefinition().getSymbol());
    LinkedHashSet<DiagramSymbol> allParentDiagrams = extensionDiamond.values().stream().reduce(new LinkedHashSet<>(),
        (acc, set) -> {
          acc.addAll(set); return acc;
    });

    allParentDiagrams.stream().map(parentD ->  {
      // Get all concrete Domain Classes of all parent CDs
      ASTDiagram astDiagram = parentD.getAstNode();
      if(astDiagram instanceof ASTCDDefinition) {
        return getConcreteDomainClasses((ASTCDDefinition)astDiagram);
      } else {
        return new ArrayList<ASTCDClass>();
      }
    }).forEach(l -> l.forEach(
        domainClassOfParentDiag -> {
          domainClassNamesToPackageMap.put(domainClassOfParentDiag.getName(),
              getCorrectPackage(domainClassOfParentDiag));
        }
    ));

    List<String> names = new ArrayList<>(new LinkedHashSet<>(domainClassNamesToPackageMap.values()));

    generatorEngine.generateNoA("umlp.gui2ts.TSComponentObserverManager", targetPath, ast,
        cdName, names, domainClassNamesToPackageMap);

    // resume the pipeline's report, the Gradle task reads its IncGenGradleCheck.txt
    if (reportingWasEnabled) {
      Reporting.on(reportedModel);
    }
    Reporting.reportFileCreation(generatorSetup.getOutputDirectory().toPath().resolve(targetPath).toString());
  }

  protected List<ASTCDClass> getConcreteDomainClasses(ASTCDDefinition cd) {
    return getDomainClasses(cd, Filter.DERIVED.negate());
  }

  protected String getCorrectPackage(ASTCDType type) {
    return UMLPSymbolTables.getDiagramFor(type).getCDDefinition().getName();
  }

  protected List<ASTCDClass> getDomainClasses(
      ASTCDDefinition cd, Predicate<Filter.Wrapper> filter) {
    return cd.getCDClassesList()
        .stream()
        .filter(c -> filter.test(Filter.Wrapper.wrap(c)))
        .collect(Collectors.toList());
  }
}
