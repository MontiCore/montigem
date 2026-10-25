package mgscript;

import de.monticore.ast.ASTNode;
import de.monticore.interpreter.calculations.MICalculation;
import de.monticore.interpreter.util.InterpreterDataForBasicSymbols;
import de.monticore.statements.interpreter.StatementsInterpreter;
import de.monticore.values.MCValue;
import de.monticore.visitor.ITraverser;

public class StatementInterpreterWrapper extends StatementsInterpreter {


  public StatementInterpreterWrapper(ITraverser traverser, InterpreterDataForBasicSymbols iData) {
    super(traverser, iData);
  }

  @Override
  public MICalculation getCalculation(ASTNode node) {
    return super.getCalculation(node);
  }

  @Override
  public MCValue interpretNode(ASTNode node) {
    return super.interpretNode(node);
  }
}
