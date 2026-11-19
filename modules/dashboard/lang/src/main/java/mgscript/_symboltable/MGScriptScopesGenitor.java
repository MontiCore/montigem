/* (c) https://github.com/MontiCore/monticore */
package mgscript._symboltable;

import de.monticore.symboltable.ImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import mgscript._ast.ASTInterpreterCompilationUnit;

import java.util.ArrayList;
import java.util.List;

public class MGScriptScopesGenitor
    extends MGScriptScopesGenitorTOP {

  @Override
  public IMGScriptArtifactScope createFromAST(
      ASTInterpreterCompilationUnit node
  ) {
    IMGScriptArtifactScope result = super.createFromAST(node);
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
