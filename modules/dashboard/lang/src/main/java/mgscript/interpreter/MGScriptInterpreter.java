// (c) https://github.com/MontiCore/monticore
package mgscript.interpreter;

import com.google.common.base.Preconditions;
import de.monticore.interpreter.calculations.MICalculation;
import de.monticore.interpreter.calculations.MICalculationVoid;
import de.monticore.interpreter.util.InterpreterDataForBasicSymbols;
import de.monticore.statements.mcstatementsbasis._ast.ASTMCBlockStatement;
import mgscript._ast.ASTInterpreterCompilationUnit;
import mgscript._visitor.MGScriptInheritanceHandler;

public class MGScriptInterpreter
    extends MGScriptInheritanceHandler {

  protected InterpreterDataForBasicSymbols iData;

  public MGScriptInterpreter(InterpreterDataForBasicSymbols iData) {
    this.iData = Preconditions.checkNotNull(iData);
  }

  @Override
  public void traverse(ASTInterpreterCompilationUnit node) {
    MICalculation fullCalc;
    MICalculationVoid stmtChainCalc = MICalculationVoid.NOOP_CALC;
    for (ASTMCBlockStatement stmt : node.getMCBlockStatementList()) {
      stmt.accept(getTraverser());
      MICalculationVoid stmtCalc = iData.popCalculation().asCalculationVoid();
      stmtChainCalc = stmtChainCalc.getChainedBefore(stmtCalc);
    }
    fullCalc = stmtChainCalc;
    iData.putCalculation(fullCalc);
    System.out.println(fullCalc);
  }

}
