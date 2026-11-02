package aas.utils;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._visitor.CDAssociationVisitor2;

import java.util.ArrayList;
import java.util.List;

public class AssociationFinder implements CDAssociationVisitor2 {
  String contains;

  public List<ASTCDAssociation> getAssociations() {
    return associations;
  }

  List<ASTCDAssociation> associations = new ArrayList<>();
  public AssociationFinder(String contains) {
    this.contains = contains;
  }

  @Override
  public void visit(ASTCDAssociation node) {

    if (node.getPrintableName().contains(contains)) {
      associations.add(node);
    }

  }

}
