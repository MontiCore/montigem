package umlp.generator.gui.genjava.prettyprinter;

import de.monticore.expressions.commonexpressions._ast.ASTArrayAccessExpression;
import de.monticore.expressions.commonexpressions._ast.ASTBooleanAndOpExpression;
import de.monticore.expressions.commonexpressions._ast.ASTBooleanNotExpression;
import de.monticore.expressions.commonexpressions._ast.ASTBooleanOrOpExpression;
import de.monticore.expressions.commonexpressions._ast.ASTBracketExpression;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTConditionalExpression;
import de.monticore.expressions.commonexpressions._ast.ASTDivideExpression;
import de.monticore.expressions.commonexpressions._ast.ASTEqualsExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._ast.ASTGreaterEqualExpression;
import de.monticore.expressions.commonexpressions._ast.ASTGreaterThanExpression;
import de.monticore.expressions.commonexpressions._ast.ASTLessEqualExpression;
import de.monticore.expressions.commonexpressions._ast.ASTLessThanExpression;
import de.monticore.expressions.commonexpressions._ast.ASTLogicalNotExpression;
import de.monticore.expressions.commonexpressions._ast.ASTMinusExpression;
import de.monticore.expressions.commonexpressions._ast.ASTMinusPrefixExpression;
import de.monticore.expressions.commonexpressions._ast.ASTModuloExpression;
import de.monticore.expressions.commonexpressions._ast.ASTMultExpression;
import de.monticore.expressions.commonexpressions._ast.ASTNotEqualsExpression;
import de.monticore.expressions.commonexpressions._ast.ASTPlusExpression;
import de.monticore.expressions.commonexpressions._ast.ASTPlusPrefixExpression;
import de.monticore.expressions.commonexpressions._ast.ASTStaticFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.prettyprint.CommentPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.gui.GUIGeneratorUtil;

import java.util.Collection;

import static umlp.generator.gui.genjava.prettyprinter.Expression2SetterPrinter.SETTER_PARAM;

public class FieldAccess2MutatorConverter extends FieldAccess2AccessorConverter {
  protected SharedBoolean lastFieldAccess;

  public FieldAccess2MutatorConverter(
      IndentPrinter printer, boolean printComments,
      Collection<ASTExpression> ignore, SharedBoolean lastFieldAccess
  ) {
    super(printer, printComments, ignore);
    this.lastFieldAccess = lastFieldAccess;
  }

  @Override
  public void handle(ASTFieldAccessExpression node) {
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    boolean printSetter = lastFieldAccess.getValue();
    lastFieldAccess.setValue(false);

    node.getExpression().accept(getTraverser());

    getPrinter().stripTrailing();
    getPrinter().print(".");

    if (GUIGeneratorUtil.getInstance().isEnumMember(node)) {
      Log.error("0x0000B Expected a mutable value, but got "
          + GUIGeneratorUtil.getInstance().print(node));
    }
    if (ignore.contains(node)) {
      getPrinter().print(node.getName() + " ");
    }
    else {
      if (printSetter) {
        getPrinter().print(attr2Setter(node) + " ");
      }
      else {
        getPrinter().print(attr2GetterCall(node) + " ");
      }
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }

  @Override
  public void handle(ASTStaticFieldAccessExpression node) {
    // e.g., enum constants
    Log.error("0x0000B Expected a mutable value, but got "
        + GUIGeneratorUtil.getInstance().print(node));
    super.handle(node);
  }

  protected String attr2Setter(ASTFieldAccessExpression node) {
    return "set" + StringTransformations.capitalize(node.getName()) + "(" + SETTER_PARAM + ")";
  }

  @Override
  public void handle(ASTCallExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTPlusPrefixExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTMinusPrefixExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTBooleanNotExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTLogicalNotExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTMultExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTDivideExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTModuloExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTPlusExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTMinusExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTLessEqualExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTGreaterEqualExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTLessThanExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTGreaterThanExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTEqualsExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTNotEqualsExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTBooleanAndOpExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTBooleanOrOpExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTConditionalExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTBracketExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  @Override
  public void handle(ASTArrayAccessExpression node) {
    errorIfExpressionIsFirst(node);
    super.handle(node);
  }

  protected void errorIfExpressionIsFirst(ASTExpression node) {
    if (lastFieldAccess.getValue()) {
      Log.error("0x0000D Expected FieldAccessExpression but got '"
          + GUIGeneratorUtil.getInstance().print(node)
          + "'. " + this.getClass().getSimpleName() + " cannot produce setter.");
    }
  }

}
