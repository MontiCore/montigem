package aas.utils;

import de.monticore.ast.ASTNode;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;

import java.util.ArrayList;
import java.util.List;

public class ClassFinder implements CDBasisVisitor2 {
  String name;

  public ASTNode getFoundClass() {
    return foundClass;
  }

  public List<ASTNode> getFoundClassesList() {
    return foundClassesList;
  }

  ASTNode foundClass;
  List<ASTNode> foundClassesList = new ArrayList<>();

  public ClassFinder(String name) {
    this.name = name;
  }

  @Override
  public void visit(ASTCDClass node) {

    if (node.getName().equals(name)) {
      if (foundClass == null) {
        foundClass = node;
      }
      foundClassesList.add(node);
    }

  }
}
