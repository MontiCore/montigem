/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genhtml;

import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIBlock;
import de.monticore.guidsl._ast.ASTGUIBlockContent;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * Stores information on nested nodes for HTML generator.
 * The nested nodes in GUI model must be flattened using ng-template node. Example:
 * <pre>
 * {@code
 * ```gui
 * @GemRow(components = [foo@GemText()]);
 * ```
 * ```html
 * <gem-row "components"=[foo]>
 * <gem-row>
 * <ng-template #foo>
 *   <gem-text>
 *   </gem-text>
 * </ng-template>
 * ```
 * }
 * </pre>
 */
public class HTMLHelper implements GUIDSLVisitor2 {

  protected GUIDSLTraverser traverser;

  protected Map<ASTGUIBlock, Set<ASTGUIBlockContent>> ngTemplates = new LinkedHashMap();
  protected Stack<ASTGUIBlock> holderStack = new Stack<>();

  protected Optional<ASTExpression> currentComp = Optional.empty();
  protected Optional<ASTGUIGuard> currentGuard = Optional.empty();
  protected Optional<ASTGUIIterate> currentIterate = Optional.empty();
  protected Set<ASTExpression> currentBlocks = new LinkedHashSet<>();

  public HTMLHelper(ASTGUICompilationUnit ast) {
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
    if (ast.getGUIComponentDeclaration().isPresentGUIBlock()) {
      ast.accept(getTraverser());
    }
  }

  public GUIDSLTraverser getTraverser() {
    return traverser;
  }

  public Set<ASTGUIBlockContent> getNgTemplates(ASTGUIBlock node) {
    if (ngTemplates.containsKey(node)) {
      return ngTemplates.get(node);
    }
    return Collections.emptySet();
  }

  @Override
  public void visit(ASTGUIComponentDeclaration node) {
    holderStack.push(node.getGUIBlock());
  }

  @Override
  public void endVisit(ASTGUIComponentDeclaration node) {
    holderStack.pop();
  }

  @Override
  public void visit(ASTGUIComponent node) {
    if (currentComp.isEmpty()) {
      currentComp = Optional.of(node);
    }
  }

  @Override
  public void endVisit(ASTGUIComponent node) {
    if (currentComp.isPresent() && currentComp.get().equals(node)) {
      currentComp = Optional.empty();
    }
    // ignore if the component is directly in blocks (between `{` and `}`) of
    // `@guard(...) { } else { }` or `@iterate(...) { }`
    else if (!currentBlocks.contains(node)) {
      ASTGUIBlockContent content = GUIDSLMill.gUIBlockContentBuilder()
          .setExpression(node)
          .build();
      addNgTemplate(content);
    }
  }

  @Override
  public void visit(ASTGUIGuard node) {
    if (currentGuard.isEmpty()) {
      currentGuard = Optional.of(node);
    }
    addGuardBlocks(node);
  }

  @Override
  public void endVisit(ASTGUIGuard node) {
    if (currentGuard.isPresent() && currentGuard.get().equals(node)) {
      currentGuard = Optional.empty();
    }
    if (currentComp.isPresent() && !currentBlocks.contains(node)) {
      ASTGUIBlockContent content = GUIDSLMill.gUIBlockContentBuilder()
          .setGUIGuard(node)
          .build();
      addNgTemplate(content);
    }
    removeGuardBlocks(node);
  }

  @Override
  public void visit(ASTGUIIterate node) {
    if (currentIterate.isEmpty()) {
      currentIterate = Optional.of(node);
    }
    holderStack.push(node.getGUIBlock());
    addIterateBlocks(node);
  }

  @Override
  public void endVisit(ASTGUIIterate node) {
    holderStack.pop();
    if (currentIterate.isPresent() && currentIterate.get().equals(node)) {
      currentIterate = Optional.empty();
    }
    if (currentComp.isPresent() && !currentBlocks.contains(node)) {
      ASTGUIBlockContent content = GUIDSLMill.gUIBlockContentBuilder()
          .setGUIIterate(node)
          .build();
      addNgTemplate(content);
    }
    removeIterateBlocks(node);
  }

  protected void addNgTemplate(ASTGUIBlockContent content) {
    if (!holderStack.empty()) {
      ASTGUIBlock currentBlock = holderStack.peek();
      if (!ngTemplates.containsKey(currentBlock)) {
        ngTemplates.put(currentBlock, new LinkedHashSet<>());
      }
      ngTemplates.get(currentBlock).add(content);
    }
  }

  protected void addToCurrentBlocks(ASTGUIBlockContent block) {
    if (block.isPresentExpression() && block.getExpression() instanceof ASTGUIComponent) {
      currentBlocks.add(block.getExpression());
    }
    if (block.isPresentGUIGuard()) {
      currentBlocks.add(block.getGUIGuard());
    }
    if (block.isPresentGUIIterate()) {
      currentBlocks.add(block.getGUIIterate());
    }
  }

  protected void removeFromCurrentBlocks(ASTGUIBlockContent block) {
    if (block.isPresentExpression() && block.getExpression() instanceof ASTGUIComponent) {
      currentBlocks.remove(block.getExpression());
    }
    if (block.isPresentGUIGuard()) {
      currentBlocks.remove(block.getGUIGuard());
    }
    if (block.isPresentGUIIterate()) {
      currentBlocks.remove(block.getGUIIterate());
    }
  }

  protected void addGuardBlocks(ASTGUIGuard node) {
    node.getThenStatement().getGUIBlockContentList().forEach(e -> addToCurrentBlocks(e));
    if (node.isPresentElseStatement()) {
      node.getElseStatement().getGUIBlockContentList().forEach(e -> addToCurrentBlocks(e));
    }
  }

  protected void removeGuardBlocks(ASTGUIGuard node) {
    node.getThenStatement().getGUIBlockContentList().forEach(e -> removeFromCurrentBlocks(e));
    if (node.isPresentElseStatement()) {
      node.getElseStatement().getGUIBlockContentList().forEach(e -> removeFromCurrentBlocks(e));
    }
  }

  protected void addIterateBlocks(ASTGUIIterate node) {
    node.getGUIBlock().getGUIBlockContentList().forEach(e -> addToCurrentBlocks(e));
  }

  protected void removeIterateBlocks(ASTGUIIterate node) {
    node.getGUIBlock().getGUIBlockContentList().forEach(e -> removeFromCurrentBlocks(e));
  }

}
