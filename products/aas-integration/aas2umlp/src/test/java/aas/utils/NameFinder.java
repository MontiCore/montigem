package aas.utils;

import de.monticore.ast.ASTNode;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;

public class NameFinder implements CDBasisVisitor2 {
  String name;
  Boolean flag = false;

  public ASTNode getFoundNode() {
    return foundNode;
  }

  ASTNode foundNode;

  public NameFinder(String name) {
    this.name = name;
  }


  @Override
  public void visit(ASTCDClass node) {
    if (node.getName().equals(name) && !flag) {
      flag = true;
      foundNode = node;
    }
  }

  @Override
  public void visit(ASTCDAttribute node) {
    if (node.getName().equals(name) && !flag) {
      flag = true;
      foundNode = node;
    }
  }

}
