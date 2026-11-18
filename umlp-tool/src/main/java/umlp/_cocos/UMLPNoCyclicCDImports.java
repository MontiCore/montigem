/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._cocos.CDBasisASTCDCompilationUnitCoCo;
import de.monticore.symbols.basicsymbols._symboltable.DiagramSymbol;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Importing from another class diagram extends it. The generated managers statically delegate to the
 * managers of the extended diagrams, which requires the import graph to be acyclic.
 */
public class UMLPNoCyclicCDImports implements CDBasisASTCDCompilationUnitCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0x70881";

  public static final String ERROR_MSG_FORMAT = "The class diagram %s is part of an import cycle: %s.";

  @Override
  public void check(ASTCDCompilationUnit node) {
    String diagramName = node.getCDDefinition().getSymbol().getFullName();
    List<String> path = new ArrayList<>(List.of(diagramName));
    if (findPathTo(diagramName, node, path, new HashSet<>())) {
      logError(node.getCDDefinition(), diagramName, String.join(" -> ", path));
    }
  }

  protected boolean findPathTo(String target, ASTCDCompilationUnit current, List<String> path, Set<String> visited) {
    for (DiagramSymbol imported : UMLPSymbolTables.getExtendedCDs(current)) {
      String importedName = imported.getFullName();
      path.add(importedName);
      if (importedName.equals(target)) {
        return true;
      }
      if (visited.add(importedName) && findPathTo(target,
          UMLPSymbolTables.getDiagramFor((ASTCDDefinition) imported.getAstNode()), path, visited)) {
        return true;
      }
      path.remove(path.size() - 1);
    }
    return false;
  }

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

  @Override
  public String getErrorMessageFormat() {
    return ERROR_MSG_FORMAT;
  }
}
