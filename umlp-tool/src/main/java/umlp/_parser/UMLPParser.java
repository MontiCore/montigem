/* (c) https://github.com/MontiCore/monticore */
package umlp._parser;

import de.monticore.cd4analysis._parser.CD4AnalysisParser;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;

import java.util.Optional;

public  class UMLPParser extends UMLPParserTOP  {

  boolean _checkFileAndPackageName = true;

  @Override
  public Optional<ASTCDCompilationUnit> parse(String fileName) {
    final Optional<ASTCDCompilationUnit> parse = super.parse(fileName);
    if (_checkFileAndPackageName) {
      parse.ifPresent(p -> CD4AnalysisParser.checkFileAndPackageName(fileName, p));
    }
    return parse;
  }

}
