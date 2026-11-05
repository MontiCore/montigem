/* (c) https://github.com/MontiCore/monticore */
package umlp._parser;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;

import java.util.Optional;

// Workaround for monticore/monticore#3651
public class CD4CodeParserForUMLP extends CD4CodeParserForUMLPTOP {

  @Override
  public Optional<ASTCDCompilationUnit> parseCDCompilationUnit(java.io.Reader reader) {
    return umlp.UMLPMill.parser().parseCDCompilationUnit(reader);
  }

  @Override
  public Optional<ASTCDCompilationUnit> parseCDCompilationUnit(String fileName) {
    return umlp.UMLPMill.parser().parseCDCompilationUnit(fileName);
  }

  @Override
  public Optional<ASTCDDefinition> parseCDDefinition(java.io.Reader reader) {
    return umlp.UMLPMill.parser().parseCDDefinition(reader);
  }

  @Override
  public Optional<ASTCDDefinition> parseCDDefinition(String fileName) {
    return umlp.UMLPMill.parser().parseCDDefinition(fileName);
  }
}
