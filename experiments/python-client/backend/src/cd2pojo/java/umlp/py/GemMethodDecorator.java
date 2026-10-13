package umlp.py;

import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._visitor.CD4CodeBasisVisitor2;
import de.monticore.cdbasis._ast.ASTCDClass;

public class GemMethodDecorator extends AbstractDecorator<AbstractDecorator.NoData> implements
    CD4CodeBasisVisitor2 {


  @Override
  public void visit(ASTCDMethod origNode) {
    var decNode = decoratorData.getAsDecorated(origNode);

    ASTCDClass origClazz = (ASTCDClass) decoratorData.getParent(origNode).get();

    // Commands are not yet supported
    decoratorData.getAsDecorated(origClazz).removeCDMember(decNode);
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CD4CodeBasis(this);
  }


}
