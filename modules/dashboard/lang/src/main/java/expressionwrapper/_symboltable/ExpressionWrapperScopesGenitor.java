/* (c) https://github.com/MontiCore/monticore */
package expressionwrapper._symboltable;

import de.monticore.symboltable.ImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import expressionwrapper._ast.ASTInterpreterCompilationUnit;

import java.util.ArrayList;
import java.util.List;

public class ExpressionWrapperScopesGenitor
    extends ExpressionWrapperScopesGenitorTOP {

  @Override
  public IExpressionWrapperArtifactScope createFromAST(
      ASTInterpreterCompilationUnit node
  ) {
    IExpressionWrapperArtifactScope result = super.createFromAST(node);
    result.setName("TestA");
    List<ImportStatement> imports = new ArrayList<>();
    for (ASTMCImportStatement importStmt : node.getMCImportStatementList()) {
      imports.add(
          new ImportStatement(importStmt.getQName(), importStmt.isStar())
      );
    }
    result.setImportsList(imports);
    return result;
  }

}
