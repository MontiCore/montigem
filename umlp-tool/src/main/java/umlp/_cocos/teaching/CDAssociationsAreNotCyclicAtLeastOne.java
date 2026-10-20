/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos.teaching;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDBiDir;
import de.monticore.cdassociation._ast.ASTCDCardOne;
import de.monticore.cdassociation._ast.ASTCDCardAtLeastOne;
import de.monticore.cdassociation._ast.ASTCDLeftToRightDir;
import de.monticore.cdassociation._ast.ASTCDRightToLeftDir;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._cocos.CDBasisASTCDDefinitionCoCo;
import umlp._cocos.CoCoTestable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CDAssociationsAreNotCyclicAtLeastOne implements CDBasisASTCDDefinitionCoCo, CoCoTestable {

  Map<String, List<String>> graph = new LinkedHashMap<>();

  @Override
  public void check(ASTCDDefinition node) {

    for (ASTCDAssociation assoc: node.getCDAssociationsList()) {
      if (assoc.getCDAssocDir() instanceof ASTCDRightToLeftDir ||
          assoc.getCDAssocDir() instanceof ASTCDBiDir) {
        if (assoc.getLeft().isPresentCDCardinality() &&
            (assoc.getLeft().getCDCardinality() instanceof ASTCDCardOne ||
                assoc.getLeft().getCDCardinality() instanceof ASTCDCardAtLeastOne)) {
          List<String> adj = graph.getOrDefault(assoc.getRight().getMCQualifiedType().getMCQualifiedName().getQName(), new ArrayList<>());
          adj.add(assoc.getLeft().getMCQualifiedType().getMCQualifiedName().getQName());
          graph.put(assoc.getRight().getMCQualifiedType().getMCQualifiedName().getQName(), adj);
        }
      }
      if (assoc.getCDAssocDir() instanceof ASTCDLeftToRightDir || assoc.getCDAssocDir() instanceof ASTCDBiDir) {
        if (assoc.getRight().isPresentCDCardinality() &&
            (assoc.getRight().getCDCardinality() instanceof ASTCDCardOne ||
                assoc.getRight().getCDCardinality() instanceof ASTCDCardAtLeastOne)) {
          List<String> adj = graph.getOrDefault(assoc.getLeft().getMCQualifiedType().getMCQualifiedName().getQName(), new ArrayList<>());
          adj.add(assoc.getRight().getMCQualifiedType().getMCQualifiedName().getQName());
          graph.put(assoc.getLeft().getMCQualifiedType().getMCQualifiedName().getQName(), adj);
        }
      }

      if (isCyclic()) {
        logError(node);
      }
    }
  }

  protected boolean isCyclic() {
    List<String> visited = new ArrayList<>();
    List<String> stack = new ArrayList<>();

    for (String node: graph.keySet()) {
      if (isCyclic(node, visited, stack)) {
        return true;
      }
    }
    return false;
  }

  protected boolean isCyclic(String node, List<String> visited, List<String> stack) {
    if (stack.contains(node)) {
      removeFromGraph(stack);
      return true;
    }
    if (visited.contains(node)) {
      return false;
    }
    visited.add(node);
    stack.add(node);
    List<String> adj = graph.getOrDefault(node, new ArrayList<>());
    for (String s: adj) {
      if (isCyclic(s, visited, stack)) {
        removeFromGraph(stack);
        return true;
      }
    }
    stack.remove(node);
    return false;
  }

  protected void removeFromGraph(List<String> list) {
    for (int i = 0; i < list.size()-1; i++) {
      graph.get(list.get(i)).remove(list.get(i+1));
    }
  }

  @Override
  public String getErrorCode() {
    return "0x52567";
  }

  @Override
  public String getErrorMessageFormat() {
    return "Associations must not form a dependency loop";
  }
}
