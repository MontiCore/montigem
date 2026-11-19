package umlp.generator.gui.genjava.decorator;

import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUINamedArg;
import de.monticore.guidsl._ast.ASTGUIParam;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.guidsl._symboltable.BuiltInTypes;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.se_rwth.commons.logging.Log;
import umlp.generator.gui.GUIGeneratorUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CallTypeDeriver implements GUIDSLVisitor2 {

  protected GUIDSLTraverser traverser;

  protected final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();
  protected static CallTypeDeriver instance;

  protected Map<ASTExpression, SymTypeExpression> exp2Type = new LinkedHashMap<>();

  public static CallTypeDeriver getInstance() {
    if (instance == null) {
      instance = new CallTypeDeriver();
    }
    return instance;
  }

  public static void unsetInstance() {
    instance = null;
  }

  public GUIDSLTraverser getTraverser() {
    return traverser;
  }

  private CallTypeDeriver() {
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
  }

  public void init(ASTGUIComponentDeclaration node) {
    reset();
    node.accept(getTraverser());
  }

  public void reset() {
    exp2Type = new LinkedHashMap<>();
  }

  public boolean hasReturnType(ASTCallExpression node) {
    return exp2Type.containsKey(node);
  }

  public SymTypeExpression getReturnType(ASTCallExpression node) {
    return exp2Type.get(node);
  }

  @Override
  public void visit(ASTGUIParam node) {
    if (node.isPresentDefault()) {
      if (!node.isPresentSymbol()) {
        Log.error("0x0000E Unable to find symbol for '" + node.getName()
            + "' at " + node.get_SourcePositionStart().toString()
            + " - " + node.get_SourcePositionEnd().toString()
        );
      }
      exp2Type.put(node.getDefault(), node.getSymbol().getType());
    }
  }

  @Override
  public void visit(ASTGUINamedArg node) {
    if (!node.isPresentNameSymbol()) {
      Log.error("0x0000E Unable to find symbol for '" + node.getName()
          + "' at " + node.get_SourcePositionStart().toString()
          + " - " + node.get_SourcePositionEnd().toString()
      );
    }
    exp2Type.put(node.getExpression(), node.getNameSymbol().getType());
  }

  @Override
  public void visit(ASTGUIGuard node) {
    Optional<TypeSymbol> ts = GUIDSLMill.globalScope().resolveType(BuiltInTypes.BOOLEAN_BOXED.getQName());
    if (ts.isPresent()) {
      exp2Type.put(node.getCondition(), SymTypeExpressionFactory.createTypeObject(ts.get()));
    }
    else {
      Log.error("0x00017 Unable to resolve '" + BuiltInTypes.BOOLEAN_BOXED.getQName() + "' type");
    }
  }

  @Override
  public void visit(ASTGUIShortIterateControl node) {
    Optional<TypeSymbol> ts = GUIDSLMill.globalScope().resolveType(BuiltInTypes.LIST.getQName());
    if (ts.isPresent()) {
      if (node.isPresentMCType()) {
        if (!node.getItem().isPresentSymbol()) {
          Log.error("0x0000E Unable to find symbol for '" + node.getItem().getName()
              + "' at " + node.getItem().get_SourcePositionStart().toString()
              + " - " + node.getItem().get_SourcePositionEnd().toString()
          );
        }
        exp2Type.put(node.getIterable(), SymTypeExpressionFactory.createGenerics(
            ts.get(),
            node.getItem().getSymbol().getType()
        ));
      }
      else {
        exp2Type.put(node.getIterable(), SymTypeExpressionFactory.createTypeObject(ts.get()));
      }
    }
    else {
      Log.error("0x00017 Unable to resolve '" + BuiltInTypes.LIST.getQName() + "' type");
    }
  }

}
