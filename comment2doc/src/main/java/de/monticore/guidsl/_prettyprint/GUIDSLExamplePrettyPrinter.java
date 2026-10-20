package de.monticore.guidsl._prettyprint;

import de.monticore.ast.ASTNode;
import de.monticore.ast.Comment;
import de.monticore.expressions.commonexpressions._prettyprint.CommonExpressionsPrettyPrinter;
import de.monticore.expressions.expressionsbasis._prettyprint.ExpressionsBasisPrettyPrinter;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIAdapt;
import de.monticore.guidsl._ast.ASTGUIBlock;
import de.monticore.guidsl._ast.ASTGUIBlockContent;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentAdaptation;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._ast.ASTGUINamedArg;
import de.monticore.guidsl._ast.ASTGUINamedArgs;
import de.monticore.guidsl._ast.ASTGUIParam;
import de.monticore.guidsl._ast.ASTGUIRangeIterateControl;
import de.monticore.guidsl._ast.ASTGUIRemove;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.guidsl._ast.ASTGUIVariableDeclaration;
import de.monticore.literals.mccommonliterals._prettyprint.MCCommonLiteralsPrettyPrinter;
import de.monticore.literals.mcliteralsbasis._prettyprint.MCLiteralsBasisPrettyPrinter;
import de.monticore.mcbasics._prettyprint.MCBasicsPrettyPrinter;
import de.monticore.ocl.setexpressions._ast.ASTSetCollectionItem;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._ast.ASTSetValueItem;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsHandler;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsTraverser;
import de.monticore.prettyprint.CommentPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symbols.basicsymbols._prettyprint.BasicSymbolsPrettyPrinter;
import de.monticore.symbols.oosymbols._prettyprint.OOSymbolsPrettyPrinter;
import de.monticore.types.mcbasictypes._prettyprint.MCBasicTypesPrettyPrinter;
import de.monticore.types.mccollectiontypes._prettyprint.MCCollectionTypesPrettyPrinter;
import de.monticore.types.mcsimplegenerictypes._prettyprint.MCSimpleGenericTypesPrettyPrinter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A concrete pretty printer for the GUIDSL language that extends {@link GUIDSLPrettyPrinter}.
 **/
public class GUIDSLExamplePrettyPrinter extends GUIDSLPrettyPrinter implements SetExpressionsHandler {

  private final List<Comment> comments = new ArrayList<>();

  public List<Comment> getComments() {
    return comments;
  }

  @Override
  public void setTraverser(SetExpressionsTraverser setExpressionsTraverser) {
    // do not throw
  }

  /**
   * Constructs a GUIDSLExamplePrettyPrinter with the given printer and comment printing flag.
   *
   * @param printer       The {@link IndentPrinter} used to format the output.
   * @param printComments Whether comments should be printed in the output.
   */
  public GUIDSLExamplePrettyPrinter(IndentPrinter printer, boolean printComments) {
    super(printer, printComments);
    setTraverser(GUIDSLMill.traverser());
    initializeTraverser(printComments);
  }

  /**
   * Handles the pretty printing of {@link ASTSetEnumeration} nodes.
   * Supports formatting either as a compact list or as a multiline structure
   * based on presence of components, nested lists, guards or iterators.
   *
   * @param node The GUI list AST node to be printed.
   */
  @Override
  public void handle(ASTSetEnumeration node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    List<ASTSetCollectionItem> values = node.getSetCollectionItemList();
    boolean hasComponents = values.stream().anyMatch(v ->
        v instanceof ASTSetValueItem valueItem &&
            valueItem.getExpression() instanceof ASTGUIComponent
    );
    boolean hasLists = values.stream().anyMatch(v ->
        v instanceof ASTSetValueItem valueItem &&
            valueItem.getExpression() instanceof ASTSetEnumeration
    );
    boolean hasIterator = values.stream().anyMatch(v ->
        v instanceof ASTSetValueItem valueItem &&
            valueItem.getExpression() instanceof ASTGUIIterate
    );
    boolean hasGuards = values.stream().anyMatch(v ->
        v instanceof ASTSetValueItem valueItem &&
            valueItem.getExpression() instanceof ASTGUIGuard
    );

    this.getPrinter().print("[");

    if (hasComponents || hasLists || hasIterator || hasGuards) {
      this.getPrinter().println();
      this.getPrinter().indent();

      Iterator<ASTSetCollectionItem> iter = values.iterator();
      while (iter.hasNext()) {
        ASTSetCollectionItem val = iter.next();
        this.getPrinter().stripTrailing();
        val.accept(this.getTraverser());

        if (iter.hasNext()) {
          this.getPrinter().print(",");
        }
        this.getPrinter().println();
      }

      this.getPrinter().unindent();
    }
    else {
      // Compact list on one line
      Iterator<ASTSetCollectionItem> iter = values.iterator();
      while (iter.hasNext()) {
        ASTSetCollectionItem val = iter.next();
        val.accept(this.getTraverser());
        if (iter.hasNext()) {
          this.getPrinter().stripTrailing();
          this.getPrinter().print(", ");
        }
      }
    }
    this.getPrinter().stripTrailing();
    this.getPrinter().print("]");

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }
  }

  /**
   * Handles the pretty printing of {@link ASTGUIComponent} nodes.
   * Formats the component with its name and arguments, and handles indentation
   * based on number of arguments.
   *
   * @param node The GUI component AST node to be printed.
   */
  @Override
  public void handle(ASTGUIComponent node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isPresentId()) {
      node.getId().accept(this.getTraverser());
    }

    this.getPrinter().print("@");
    this.getPrinter().print(node.getName());

    List<ASTGUINamedArg> args = node.getGUINamedArgs().getGUINamedArgList();
    if (args.size() > 1) {
      this.getPrinter().print("(");
      this.getPrinter().println();
      this.getPrinter().indent();

      for (Iterator<ASTGUINamedArg> iter = args.iterator(); iter.hasNext(); ) {
        ASTGUINamedArg arg = iter.next();
        this.getPrinter().stripTrailing();
        arg.accept(this.getTraverser());
        if (iter.hasNext()) {
          this.getPrinter().stripTrailing();
          this.getPrinter().print(",");
        }
        this.getPrinter().println();
      }

      this.getPrinter().unindent();
      this.getPrinter().stripTrailing();
      this.getPrinter().print(")");
    }
    else {
      node.getGUINamedArgs().accept(this.getTraverser());
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }
  }

  /**
   * Handles the pretty printing of {@link ASTGUINamedArg} nodes.
   * Formats a named argument with its value.
   * Adds additional spacing around =.
   *
   * @param node The named argument node to be printed.
   */
  @Override
  public void handle(ASTGUINamedArg node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    this.getPrinter().print(node.getName() + " ");
    this.getPrinter().stripTrailing();
    this.getPrinter().print(" = ");
    node.getExpression().accept(this.getTraverser());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  /**
   * Handles the pretty printing of {@link ASTGUIParam} nodes.
   * Prints parameter name, type, and optional default value.
   * Adds additional spacing around =.
   *
   * @param node The GUI parameter node to be printed.
   */
  @Override
  public void handle(ASTGUIParam node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isReferenceParam()) {
      this.getPrinter().print("?");
    }

    node.getMCType().accept(this.getTraverser());
    this.getPrinter().print(node.getName());
    if (node.isPresentDefault()) {
      this.getPrinter().stripTrailing();
      this.getPrinter().print(" = ");
      node.getDefault().accept(this.getTraverser());
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  /**
   * Handles the pretty printing of {@link ASTGUIVariableDeclaration} nodes.
   *
   * @param node The GUI variable declaration node to be printed.
   */
  @Override
  public void handle(ASTGUIVariableDeclaration node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    this.getPrinter().print(node.getName());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  /**
   * Handles the pretty printing of {@link ASTGUIBlock} nodes.
   * Adds an extra line between consecutive block elements (except before the first).
   *
   * @param node The GUI block node to be printed.
   */
  @Override
  public void handle(ASTGUIBlock node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    this.getPrinter().println(" {");
    this.getPrinter().indent();
    List<ASTGUIBlockContent> contents = node.getGUIBlockContentList();
    for (int i = 0; i < contents.size(); i++) {
      if (i > 0) {
        this.getPrinter().println(); // Add extra line before all except the first
      }
      contents.get(i).accept(this.getTraverser());
    }
    this.getPrinter().unindent();
    this.getPrinter().println("}");
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  /**
   * Handles the pretty printing of {@link ASTGUICompilationUnit} nodes.
   * Prints the package, imports, and the main component declaration.
   *
   * @param node The GUI compilation unit node to be printed.
   */
  @Override
  public void handle(ASTGUICompilationUnit node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    node.getMCPackageDeclaration().accept(this.getTraverser());
    this.getPrinter().println();
    node.getMCImportStatementList().forEach((n) -> n.accept(this.getTraverser()));
    this.getPrinter().println();
    node.getGUIComponentDeclaration().accept(this.getTraverser());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIComponentDeclaration node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    Iterator<ASTGUIParam> iter_gUIParam = node.getGUIParamList().iterator();
    if (node.isComponent() || node.isPage()) {
      if (node.isComponent()) {
        this.getPrinter().print("component ");
      }
      else if (node.isPage()) {
        this.getPrinter().print("page ");
      }

      this.getPrinter().print(node.getName() + " ");
      this.getPrinter().stripTrailing();
      this.getPrinter().print("(");
      if (iter_gUIParam.hasNext()) {
        ((ASTGUIParam) iter_gUIParam.next()).accept(this.getTraverser());

        while (iter_gUIParam.hasNext()) {
          this.getPrinter().stripTrailing();
          this.getPrinter().print(",");
          ((ASTGUIParam) iter_gUIParam.next()).accept(this.getTraverser());
        }
      }

      this.getPrinter().stripTrailing();
      this.getPrinter().print(")");
      if (node.isPresentGUIComponentAdaptation()) {
        node.getGUIComponentAdaptation().accept(this.getTraverser());
      }

      if (node.isPresentGUIBlock()) {
        node.getGUIBlock().accept(this.getTraverser());
      }
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIComponentAdaptation node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    this.getPrinter().print("adapts ");
    this.getPrinter().print(node.getTarget() + " ");
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUINamedArgs node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    Iterator<ASTGUINamedArg> iter_gUINamedArg = node.getGUINamedArgList().iterator();
    this.getPrinter().stripTrailing();
    this.getPrinter().print("(");
    if (iter_gUINamedArg.hasNext()) {
      iter_gUINamedArg.next().accept(this.getTraverser());

      while (iter_gUINamedArg.hasNext()) {
        this.getPrinter().stripTrailing();
        this.getPrinter().print(",");
        iter_gUINamedArg.next().accept(this.getTraverser());
      }
    }

    this.getPrinter().stripTrailing();
    this.getPrinter().print(")");
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIBlockContent node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isPresentExpression()) {
      node.getExpression().accept(this.getTraverser());
      this.getPrinter().stripTrailing();
      this.getPrinter().println(";");
    }
    else if (node.isPresentGUIGuard()) {
      node.getGUIGuard().accept(this.getTraverser());
    }
    else if (node.isPresentGUIIterate()) {
      node.getGUIIterate().accept(this.getTraverser());
    }
    else if (node.isPresentGUIAdapt()) {
      node.getGUIAdapt().accept(this.getTraverser());
      this.getPrinter().stripTrailing();
      this.getPrinter().println(";");
    }
    else if (node.isPresentGUIRemove()) {
      node.getGUIRemove().accept(this.getTraverser());
      this.getPrinter().stripTrailing();
      this.getPrinter().println(";");
    }
    else if (node.isPresentSemicolon()) {
      this.getPrinter().stripTrailing();
      this.getPrinter().println(";");
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIAdapt node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.getPosition() == 2 || node.getPosition() == 1 || node.getPosition() == 6) {
      if (node.getPosition() == 2) {
        this.getPrinter().print("before ");
      }
      else if (node.getPosition() == 1) {
        this.getPrinter().print("after ");
      }
      else if (node.getPosition() == 6) {
        this.getPrinter().print("replace ");
      }

      this.getPrinter().print(node.getName() + " ");
      this.getPrinter().stripTrailing();
      this.getPrinter().print(":");
      node.getExpression().accept(this.getTraverser());
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIRemove node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    this.getPrinter().print("remove ");
    this.getPrinter().print(node.getName() + " ");
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIGuard node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isPresentId()) {
      node.getId().accept(this.getTraverser());
    }

    this.getPrinter().print("@guard ");
    this.getPrinter().stripTrailing();
    this.getPrinter().print("(");
    node.getCondition().accept(this.getTraverser());
    this.getPrinter().stripTrailing();
    this.getPrinter().print(")");
    node.getThenStatement().accept(this.getTraverser());
    if (node.isPresentElseStatement()) {
      this.getPrinter().print("else ");
      node.getElseStatement().accept(this.getTraverser());
    }

    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIIterate node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isPresentId()) {
      node.getId().accept(this.getTraverser());
    }

    this.getPrinter().print("@iterate ");
    this.getPrinter().stripTrailing();
    this.getPrinter().print("(");
    node.getIterateControl().accept(this.getTraverser());
    this.getPrinter().stripTrailing();
    this.getPrinter().print(")");
    node.getGUIBlock().accept(this.getTraverser());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIRangeIterateControl node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    node.getIndex().accept(this.getTraverser());
    this.getPrinter().print("from ");
    node.getStart().accept(this.getTraverser());
    this.getPrinter().print("to ");
    node.getEnd().accept(this.getTraverser());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  public void handle(ASTGUIShortIterateControl node) {
    this.collectComments(node);
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPreComments(node, this.getPrinter());
    }

    if (node.isPresentMCType()) {
      node.getMCType().accept(this.getTraverser());
    }

    node.getItem().accept(this.getTraverser());
    this.getPrinter().stripTrailing();
    this.getPrinter().print(":");
    node.getIterable().accept(this.getTraverser());
    if (this.isPrintComments()) {
      CommentPrettyPrinter.printPostComments(node, this.getPrinter());
    }

  }

  /**
   * Pretty prints the given AST node into a formatted string.
   * Clears the buffer and traversed elements before printing.
   *
   * @param node The AST node to be pretty printed.
   * @return The formatted string.
   */
  public String prettyprint(ASTNode node) {
    this.getPrinter().clearBuffer();
    this.getTraverser().clearTraversedElements();
    node.accept(this.getTraverser());
    this.getPrinter().stripTrailing();
    return this.getPrinter().getContent();
  }

  /**
   * Initializes the traverser with all necessary pretty printer handlers
   * for sublanguages and symbol sets.
   *
   * @param printComments Whether to enable comment printing.
   */
  protected void initializeTraverser(boolean printComments) {
    this.getTraverser().setGUIDSLHandler(this);
    this.getTraverser().add4GUIDSL(this);
    OOSymbolsPrettyPrinter oOSymbols = new OOSymbolsPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setOOSymbolsHandler(oOSymbols);
    this.getTraverser().add4OOSymbols(oOSymbols);
    MCCommonLiteralsPrettyPrinter mCCommonLiterals = new MCCommonLiteralsPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCCommonLiteralsHandler(mCCommonLiterals);
    this.getTraverser().add4MCCommonLiterals(mCCommonLiterals);
    MCSimpleGenericTypesPrettyPrinter mCSimpleGenericTypes = new MCSimpleGenericTypesPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCSimpleGenericTypesHandler(mCSimpleGenericTypes);
    this.getTraverser().add4MCSimpleGenericTypes(mCSimpleGenericTypes);
    CommonExpressionsPrettyPrinter commonExpressions = new CommonExpressionsPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setCommonExpressionsHandler(commonExpressions);
    this.getTraverser().add4CommonExpressions(commonExpressions);
    BasicSymbolsPrettyPrinter basicSymbols = new BasicSymbolsPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setBasicSymbolsHandler(basicSymbols);
    this.getTraverser().add4BasicSymbols(basicSymbols);
    MCBasicsPrettyPrinter mCBasics = new MCBasicsPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCBasicsHandler(mCBasics);
    this.getTraverser().add4MCBasics(mCBasics);
    MCLiteralsBasisPrettyPrinter mCLiteralsBasis = new MCLiteralsBasisPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCLiteralsBasisHandler(mCLiteralsBasis);
    this.getTraverser().add4MCLiteralsBasis(mCLiteralsBasis);
    MCCollectionTypesPrettyPrinter mCCollectionTypes = new MCCollectionTypesPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCCollectionTypesHandler(mCCollectionTypes);
    this.getTraverser().add4MCCollectionTypes(mCCollectionTypes);
    MCBasicTypesPrettyPrinter mCBasicTypes = new MCBasicTypesPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setMCBasicTypesHandler(mCBasicTypes);
    this.getTraverser().add4MCBasicTypes(mCBasicTypes);
    ExpressionsBasisPrettyPrinter expressionsBasis = new ExpressionsBasisPrettyPrinter(this.getPrinter(), printComments);
    this.getTraverser().setExpressionsBasisHandler(expressionsBasis);
    this.getTraverser().add4ExpressionsBasis(expressionsBasis);
  }

  /**
   * Updates the print comment flag for all sub-pretty-printers and handlers
   * associated with the traverser.
   *
   * @param printComments Whether to enable comment printing.
   */
  public void setPrintComments(boolean printComments) {
    ((GUIDSLPrettyPrinter) this.getTraverser().getGUIDSLHandler().get()).setPrintComments(printComments);
    ((OOSymbolsPrettyPrinter) this.getTraverser().getOOSymbolsHandler().get()).setPrintComments(printComments);
    ((MCCommonLiteralsPrettyPrinter) this.getTraverser().getMCCommonLiteralsHandler().get()).setPrintComments(printComments);
    ((MCSimpleGenericTypesPrettyPrinter) this.getTraverser().getMCSimpleGenericTypesHandler().get()).setPrintComments(printComments);
    ((CommonExpressionsPrettyPrinter) this.getTraverser().getCommonExpressionsHandler().get()).setPrintComments(printComments);
    ((BasicSymbolsPrettyPrinter) this.getTraverser().getBasicSymbolsHandler().get()).setPrintComments(printComments);
    ((MCBasicsPrettyPrinter) this.getTraverser().getMCBasicsHandler().get()).setPrintComments(printComments);
    ((MCLiteralsBasisPrettyPrinter) this.getTraverser().getMCLiteralsBasisHandler().get()).setPrintComments(printComments);
    ((MCCollectionTypesPrettyPrinter) this.getTraverser().getMCCollectionTypesHandler().get()).setPrintComments(printComments);
    ((MCBasicTypesPrettyPrinter) this.getTraverser().getMCBasicTypesHandler().get()).setPrintComments(printComments);
    ((ExpressionsBasisPrettyPrinter) this.getTraverser().getExpressionsBasisHandler().get()).setPrintComments(printComments);
  }

  private void collectComments(ASTNode node) {
    if (node.get_PreCommentList() != null) {
      comments.addAll(node.get_PreCommentList());
    }
    if (node.get_PostCommentList() != null) {
      comments.addAll(node.get_PostCommentList());
    }
  }

  public List<Comment> collectAllComments(ASTNode root) {
    this.getTraverser().clearTraversedElements();

    // Traverse the AST using the shared traverser
    root.accept(this.getTraverser());

    return getComments();
  }
}
