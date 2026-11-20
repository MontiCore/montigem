package umlp.backendrte.ocl;

import de.monticore.expressions.commonexpressions._ast.ASTEqualsExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._ast.ASTQualifiedNameExpression;
import de.monticore.expressions.commonexpressions._prettyprint.CommonExpressionsPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;

import java.util.List;

public class CommonExpressions2SQLPrinter extends CommonExpressionsPrettyPrinter {

  public CommonExpressions2SQLPrinter(IndentPrinter printer, boolean printComments) {
    super(printer, printComments);
  }

  @Override
  public void handle(ASTEqualsExpression node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    node.getLeft().accept(getTraverser());
    getPrinter().stripTrailing();

    // adjusted part
    getPrinter().print("=");

    node.getRight().accept(getTraverser());
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }

  @Override
  public void handle(ASTFieldAccessExpression node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }

    node.getExpression().accept(getTraverser());

    getPrinter().stripTrailing();
    getPrinter().print(".");
    getPrinter().print("\"" + node.getName() + "\" ");

    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }

  /**
   * qualified names, like `p.age`, are a variable followed by field accesses
   */
  @Override
  public void handle(ASTQualifiedNameExpression node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }

    List<String> parts = node.getMCQualifiedName().getPartsList();
    getPrinter().print(parts.get(0));
    for (String part : parts.subList(1, parts.size())) {
      getPrinter().print(".\"" + part + "\"");
    }
    getPrinter().print(" ");

    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }
}
