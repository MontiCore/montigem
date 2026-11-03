/* (c) https://github.com/MontiCore/monticore */

package umlp._lsp.language_access;

import de.monticore.cd4code._symboltable.CD4CodeSymbolTableCompleter;
import de.monticore.cd4code.types3.CD4CodeTypeCheck3;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.io.paths.MCPath;
import de.se_rwth.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umlp._symboltable.IUMLPArtifactScope;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class UMLPScopeManager extends UMLPScopeManagerTOP {
  private static final Logger logger = LoggerFactory.getLogger(UMLPScopeManager.class);

  @Override
  public void initGlobalScope(MCPath modelPath) {
    super.initGlobalScope(modelPath);
  }

  @Override
  public UMLPArtifactScopeWithFindings createArtifactScope(ASTCDCompilationUnit ast, IUMLPArtifactScope oldArtifactScope) {
    UMLPArtifactScopeWithFindings res = super.createArtifactScope(ast, oldArtifactScope);
    try{
      Log.getFindings().clear();
      CD4CodeTypeCheck3.init();
      res.originalAst.accept(new CD4CodeSymbolTableCompleter(ast).getTraverser());
      res.findings.addAll(Log.getFindings());
    } catch (Throwable e){
      logger.warn("Error while creating completing symbol table", e);
    }
    return res;
  }

  @Override
  public Map<ASTCDCompilationUnit, UMLPArtifactScopeWithFindings> createAllArtifactScopes(Collection<ASTCDCompilationUnit> astNodes) {
    final Map<ASTCDCompilationUnit, UMLPArtifactScopeWithFindings> res = new LinkedHashMap<>();
    syncAccessGlobalScope(gs -> {
      clearGlobalScope();
      if(supportsIterativeScopeAppending()){
        for(ASTCDCompilationUnit node : astNodes){
          try {
            Log.getFindings().clear();
            // Use super.createArtifactScope, so that the completer is not used
            res.put(node, super.createArtifactScope(node, null));
          }catch (Throwable e){
            logger.warn("Error while creating artifact scope", e);
          }
        }

        // Phase 2: complete symbol table when base structure is finished
        for(ASTCDCompilationUnit ast : astNodes){
          try {
            Log.getFindings().clear();
            CD4CodeTypeCheck3.init();
            ast.accept(new CD4CodeSymbolTableCompleter(ast).getTraverser());
            res.get(ast).findings.addAll(Log.getFindings());
          } catch (Throwable e){
            logger.warn("Error while creating completing symbol table", e);
          }
        }
      }
    });
    return res;
  }
}
