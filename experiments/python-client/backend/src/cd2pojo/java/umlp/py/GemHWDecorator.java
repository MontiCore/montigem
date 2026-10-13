package umlp.py;

import com.google.common.collect.Iterables;
import de.monticore.cd.codegen.decorators.IDecorator;
import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.generating.templateengine.StringHookPoint;
import de.se_rwth.commons.logging.Log;

import java.util.List;

public class GemHWDecorator extends AbstractDecorator<AbstractDecorator.NoData> implements CDBasisVisitor2 {


  @Override
  public void visit(ASTCDClass originalNode) {
    for (var comment : originalNode.get_PostCommentList()) {
      if (comment.getText().trim().startsWith("// HW:")) {
        String content = comment.getText().replace("// HW:", "") + "\n";
        var decNode = decoratorData.getAsDecorated(originalNode);
        glexOpt.ifPresent(glex -> glex.addAfterTemplate("cd2python.Class::postBody", decNode, new StringHookPoint(content)));
      }

    }
  }

  @Override
  public void visit(ASTCDAttribute originalNode) {
    for (var comment : originalNode.get_PostCommentList()) {
      if (comment.getText().trim().startsWith("// HW:"))
        Log.error("Unhandled hw comment", originalNode.get_SourcePositionStart());
    }
  }


  @Override
  public Iterable<Class<? extends IDecorator>> getMustRunAfter() {
    return Iterables.concat(super.getMustRunAfter(), List.of(GemBaseDecorator.class));
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
  }


}
