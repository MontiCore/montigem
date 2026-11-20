// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.ocl;

import de.monticore.expressions.commonexpressions._ast.ASTEqualsExpression;
import de.monticore.expressions.commonexpressions._ast.ASTGreaterEqualExpression;
import de.monticore.expressions.commonexpressions._ast.ASTGreaterThanExpression;
import de.monticore.expressions.commonexpressions._ast.ASTLessEqualExpression;
import de.monticore.expressions.commonexpressions._ast.ASTLessThanExpression;
import de.monticore.expressions.commonexpressions._ast.ASTNotEqualsExpression;
import de.monticore.expressions.commonexpressions._ast.ASTQualifiedNameExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.ocl.setexpressions._ast.ASTSetComprehension;
import de.monticore.ocl.setexpressions._ast.ASTSetComprehensionItem;
import de.monticore.ocl.setexpressions._cocos.SetExpressionsASTSetComprehensionCoCo;
import de.se_rwth.commons.logging.Log;

/**
 * Allows only simple set comprehensions, like `[ p | p in Person, p.age > 18 ]`
 */
public class OnlySimpleComprehensions implements SetExpressionsASTSetComprehensionCoCo {

  @Override
  public void check(ASTSetComprehension node) {
    // left side is only simple expression, like `p`, `foo`, ...
    if (node.getLeft().isPresentExpression()
        && isName(node.getLeft().getExpression())) {
      String left = ((ASTQualifiedNameExpression) node.getLeft().getExpression()).getMCQualifiedName().getQName();
      int generators = 0;
      for (ASTSetComprehensionItem right : node.getSetComprehensionItemList()) {
        // if right side has a generator declaration, then only simple, like `p in Person`
        if (right.isPresentGeneratorDeclaration()) {
          if (isName(right.getGeneratorDeclaration().getExpression())) {
            if (!right.getGeneratorDeclaration().getName().equals(left)) {
              Log.error("0xB0002 Unsupported format: Generator on the right side of SetComprehension must use"
                  + " the name from the left side but was\n"
                  + OCLMill.prettyPrint(right, false)
                  + ", try instead\n" + left + " in "
                  + OCLMill.prettyPrint(right.getGeneratorDeclaration(), false)
              );
            }
          }
          else {
            Log.error("0xB0003 Unsupported format: Generator on the right side of SetComprehension must use"
                + " domain class name describing a set, like or `Person` or `Car` but was\n"
                + OCLMill.prettyPrint(right.getGeneratorDeclaration().getExpression(), false));
          }
          generators++;
        }
        else if (right.isPresentExpression()) {
          if (!(right.getExpression() instanceof ASTLessThanExpression)
              && !(right.getExpression() instanceof ASTLessEqualExpression)
              && !(right.getExpression() instanceof ASTEqualsExpression)
              && !(right.getExpression() instanceof ASTNotEqualsExpression)
              && !(right.getExpression() instanceof ASTGreaterThanExpression)
              && !(right.getExpression() instanceof ASTGreaterEqualExpression)
          ) {
            Log.error("0xB0006 Unsupported format: Expression on the right side of SetComprehension"
                + " must be a comparison, like `person.age > 18` or `car.name = 'Mustang'`"
                + " but got\n" + OCLMill.prettyPrint(right, false)
            );
          }
        }
        else {
          Log.error("0xB0005 Unsupported format: expected a single generator and maybe multiple"
              + " expressions but got\n" + OCLMill.prettyPrint(right, false)
          );
        }
      }
      if (generators != 1) {
        Log.error("0xB0004 Unsupported format: expected a single generator but got " + generators);
      }
    }
    else {
      Log.error("0xB0001 Unsupported format: Left side of SetComprehension must be a name expression, like `person`"
          + " or `x` but was\n" + OCLMill.prettyPrint(node.getLeft(), false));
    }
  }

  /**
   * simple names, like `p`, are parsed as qualified names with a single part
   */
  protected boolean isName(ASTExpression expr) {
    return expr instanceof ASTQualifiedNameExpression qName
        && qName.getMCQualifiedName().sizeParts() == 1;
  }
}
