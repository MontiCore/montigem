package umlp.generator.gui.genjava.decorator;

import de.monticore.ast.ASTNode;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._ast.ASTStaticFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.expressions.expressionsbasis._visitor.ExpressionsBasisVisitor2;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIBlock;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._ast.ASTGUIParam;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsVisitor2;
import de.se_rwth.commons.logging.Log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

// TODO AGe: currently has stricter dependencies than expected, e.g.,
//  [[person, contact, Person()], friend.friends] ->
//  friend.friends depends on [person, contact, Person()].
public class InitDependencyTree implements GUIDSLVisitor2, CommonExpressionsVisitor2, ExpressionsBasisVisitor2,
    SetExpressionsVisitor2 {

  protected GUIDSLTraverser traverser;

  protected static InitDependencyTree instance;

  protected Stack<InitDependency> depStack = new Stack<>();
  protected Map<ASTNode, InitDependency> nodeDepMap = new LinkedHashMap<>();
  protected Optional<ASTNode> jump = Optional.empty();

  public static InitDependencyTree getInstance() {
    if (instance == null) {
      instance = new InitDependencyTree();
    }
    return instance;
  }

  public GUIDSLTraverser getTraverser() {
    return traverser;
  }

  private InitDependencyTree() {
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
    traverser.add4CommonExpressions(this);
    traverser.add4ExpressionsBasis(this);
    traverser.add4SetExpressions(this);
  }

  public void init(ASTGUIComponentDeclaration node) {
    reset();
    node.accept(getTraverser());
  }

  public void reset() {
  }

  public Optional<InitDependency> getDependency(ASTNode node) {
    return Optional.ofNullable(nodeDepMap.get(node));
  }

  @Override
  public void visit(ASTGUIComponentDeclaration node) {
    putDependency(node);
  }

  @Override
  public void visit(ASTGUIBlock node) {
    putDependency(node);
  }

  @Override
  public void endVisit(ASTGUIBlock node) {
    InitDependency blockDep = nodeDepMap.get(node);
    if (blockDep != null) {
      while (!depStack.isEmpty() && !depStack.peek().equals(blockDep)) {
        depStack.pop();
      }
      if (!depStack.isEmpty() && depStack.peek().equals(blockDep)) {
        depStack.pop();
      }
    }
  }

  @Override
  public void visit(ASTGUIParam node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTGUIParam node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  @Override
  public void visit(ASTGUIIterate node) {
    if (!(node.getIterateControl() instanceof ASTGUIShortIterateControl)) {
      Log.error("0x00004 iterate construct " + node.getId().getName()
          + " was not processed: only short version, such as '(Type item : list)',"
          + " is supported so far");
    }
    /*
    ASTGUIShortIterateControl control = (ASTGUIShortIterateControl) node.getIterateControl();
    putDependency(control);
    */
  }

  @Override
  public void endVisit(ASTGUIIterate node) {
    if (node.getIterateControl() instanceof ASTGUIShortIterateControl) {
      InitDependency controlDep = nodeDepMap.get(node.getIterateControl());
      if (controlDep != null) {
        while (!depStack.isEmpty() && !depStack.peek().equals(controlDep)) {
          depStack.pop();
        }
        if (!depStack.isEmpty() && depStack.peek().equals(controlDep)) {
          depStack.pop();
        }
      }
    }
  }

  @Override
  public void visit(ASTGUIShortIterateControl node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTGUIShortIterateControl node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  @Override
  public void visit(ASTCallExpression node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTCallExpression node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  @Override
  public void visit(ASTFieldAccessExpression node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTFieldAccessExpression node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  @Override
  public void visit(ASTStaticFieldAccessExpression node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTStaticFieldAccessExpression node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  @Override
  public void visit(ASTSetEnumeration node) {
    rememberDepStackJump(node);
  }

  @Override
  public void endVisit(ASTSetEnumeration node) {
    putDependency(node);
    maybeJumpDepStack(node);
  }

  protected InitDependency putDependency(ASTNode node) {
    InitDependency current = new InitDependency();
    nodeDepMap.put(node, current);
    if (!depStack.isEmpty()) {
      InitDependency dependency = depStack.peek();
      dependency.addDependent(current);
    }
    depStack.push(current);
    return current;
  }

  public void rememberDepStackJump(ASTNode node) {
    if (jump.isEmpty()) {
      jump = Optional.of(node);
    }
  }

  public void maybeJumpDepStack(ASTNode node) {
    if (jump.isPresent() && jump.get().equals(node)) {
      while (!depStack.isEmpty()
          && !nodeDepMap.get(jump.get()).equals(depStack.peek())) {
        depStack.pop();
      }
      jump = Optional.empty();
    }
  }

  protected void popDependency() {
    if (depStack.isEmpty()) {
      Log.error("0x0001C Unable to derive initialization dependencies.");
    }
    depStack.pop();
  }

  public static void cleanUp() {
    instance = null;
  }

}
