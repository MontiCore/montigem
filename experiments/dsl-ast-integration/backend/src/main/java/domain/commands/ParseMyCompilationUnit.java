package domain.commands;

import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import domain.ASTMyCompilationUnit;
import domain.ASTMyCompilationUnitBuilder;
import domain.ParseResultASTMyCompilationUnit;
import dsl2cd.DSL2Domain;
import mydsl.mydsl.MyDSLMill;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.common.LoadingState;

import java.util.Optional;
import java.util.stream.Collectors;

public class ParseMyCompilationUnit extends ParseMyCompilationUnitTOP {
  public ParseMyCompilationUnit(String input) {
    super(input);
  }

  @Override
  public SimpleResult<ParseResultASTMyCompilationUnit> doAction() {
    Log.clearFindings();
    var retB = domain.DomainManager.parseResultASTMyCompilationUnitBuilder();

    MyDSLMill.init();

    var parser = MyDSLMill.parser();

    Optional<mydsl.mydsl._ast.ASTMyCompilationUnit> opt = null;
    opt = parser.parse_StringMyCompilationUnit(getInput());

    if (parser.hasErrors() || !opt.isPresent()) {

      retB.errors(Log.getFindings().stream().map(Finding::buildMsg).collect(Collectors.toList()));

      return this.replySuccess(retB.build().get());
    }

    var traverser = MyDSLMill.inheritanceTraverser();
//    var handler = new MyDSL2Domain();
    var handler = new DSL2Domain();

    traverser.setMyDSLHandler(handler);
//    traverser.setMCBasicsHandler(handler);
    traverser.setMCLiteralsBasisHandler(handler);
    traverser.setMCBasicTypesHandler(handler);
//    traverser.setOOSymbolsHandler(handler);
    traverser.setUMLStereotypeHandler(handler);
    traverser.setUMLModifierHandler(handler);
    traverser.setCommonExpressionsHandler(handler);
    traverser.setBasicSymbolsHandler(handler);
    traverser.setMCCommonLiteralsHandler(handler);
    traverser.setExpressionsBasisHandler(handler);

    opt.get().accept(traverser);

    ASTMyCompilationUnitBuilder r = (ASTMyCompilationUnitBuilder) handler.stack.pop();
    r.build();

    retB.ast(r);

    retB.errors(Log.getFindings().stream().map(Finding::buildMsg).collect(Collectors.toList()));

    retB.build();
    return this.replySuccess(retB);
  }
}
