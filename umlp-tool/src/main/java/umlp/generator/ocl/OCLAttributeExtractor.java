package umlp.generator.ocl;

import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.expressions.expressionsbasis._visitor.ExpressionsBasisVisitor2;
import de.monticore.ocl.ocl._ast.ASTOCLContextDefinition;
import de.monticore.ocl.ocl._ast.ASTOCLInvariant;
import de.monticore.symbols.basicsymbols._symboltable.IBasicSymbolsScope;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symboltable.ISymbol;
import de.monticore.symboltable.modifiers.AccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.TypeCheck3;
import de.monticore.types3.util.OOWithinTypeBasicSymbolsResolver;
import de.monticore.types3.util.WithinScopeBasicSymbolsResolver;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import oclwitherrormessages._visitor.OCLWithErrorMessagesTraverser;
import oclwitherrormessages._visitor.OCLWithErrorMessagesVisitor2;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class OCLAttributeExtractor implements CommonExpressionsVisitor2, ExpressionsBasisVisitor2, OCLWithErrorMessagesVisitor2 {

  protected Set<String> usedAttributes = new HashSet<>();
  protected String contextParamName;
  protected SymTypeExpression contextType;
  protected boolean hasErrorMessages = false;

  public OCLAttributeExtractor(String contextParamName, SymTypeExpression contextType) {
    this.contextParamName = contextParamName;
    this.contextType = contextType;
  }

  public static Result extract(ASTOCLInvariant invariant, String contextParamName) {
    OCLAttributeExtractor extractor = new OCLAttributeExtractor(
        contextParamName,
        resolveContextType(invariant, contextParamName)
    );
    OCLWithErrorMessagesTraverser traverser = OCLWithErrorMessagesMill.traverser();
    traverser.add4CommonExpressions(extractor);
    traverser.add4ExpressionsBasis(extractor);
    traverser.add4OCLWithErrorMessages(extractor);
    invariant.accept(traverser);
    return new Result(extractor.usedAttributes, extractor.hasErrorMessages);
  }

  @Override
  public void visit(ASTFieldAccessExpression node) {
    Optional<SymTypeExpression> ownerType = resolveExpressionType(node.getExpression());
    if (ownerType.filter(this::isContextType).isPresent()) {
      usedAttributes.add(node.getName());
    }
  }

  protected Optional<SymTypeExpression> resolveExpressionType(ASTExpression expression) {
    Optional<String> qualifiedName = getQualifiedName(expression);
    if (qualifiedName.isPresent()
        && expression.getEnclosingScope() instanceof IBasicSymbolsScope enclosingScope) {
      Optional<SymTypeExpression> expressionType = WithinScopeBasicSymbolsResolver.resolveNameAsExpr(
          enclosingScope,
          qualifiedName.get()
      );
      if (expressionType.isPresent()) {
        return expressionType;
      }
      if (WithinScopeBasicSymbolsResolver.resolveType(
          enclosingScope,
          qualifiedName.get()
      ).isPresent()) {
        return Optional.empty();
      }
    }
    return Optional.of(TypeCheck3.typeOf(expression));
  }

  protected Optional<String> getQualifiedName(ASTExpression expression) {
    if (expression instanceof ASTNameExpression name) {
      return Optional.of(name.getName());
    }
    if (expression instanceof ASTFieldAccessExpression fieldAccess) {
      return getQualifiedName(fieldAccess.getExpression())
          .map(qualifier -> qualifier + "." + fieldAccess.getName());
    }
    return Optional.empty();
  }

  @Override
  public void visit(ASTNameExpression node) {
    Optional<ISymbol> definingSymbol = node.getDefiningSymbol();
    if (definingSymbol.isPresent()
        && isContextField(node.getName(), definingSymbol.get())) {
      usedAttributes.add(node.getName());
    }
  }

  @Override
  public void visit(oclwitherrormessages._ast.ASTOCLInvariant node) {
    if (node.isPresentInvErrorMessages()) {
      hasErrorMessages = true;
    }
  }

  protected static SymTypeExpression resolveContextType(ASTOCLInvariant invariant, String contextParamName) {
    for (ASTOCLContextDefinition context : invariant.getOCLContextDefinitionList()) {
      if (context.isPresentMCType() && "this".equals(contextParamName)) {
        return TypeCheck3.symTypeFromAST(context.getMCType());
      }
      if (context.isPresentOCLParamDeclaration()
          && contextParamName.equals(context.getOCLParamDeclaration().getName())) {
        return TypeCheck3.symTypeFromAST(context.getOCLParamDeclaration().getMCType());
      }
    }
    return null;
  }

  protected boolean isContextType(SymTypeExpression type) {
    if (contextType == null || type == null) {
      return false;
    }
    if (type.deepEquals(contextType)) {
      return true;
    }
    return type.isIntersectionType()
        && type.asIntersectionType().getIntersectedTypeSet().stream().anyMatch(this::isContextType);
  }

  protected boolean isContextField(String name, ISymbol symbol) {
    if (!(symbol instanceof VariableSymbol) || contextType == null) {
      return false;
    }
    return OOWithinTypeBasicSymbolsResolver.resolveVariable(contextType, name, AccessModifier.ALL_INCLUSION, variableSymbol -> true).stream()
        .anyMatch(field -> field.equals(symbol));
  }

  public static class Result {
    public final Set<String> usedAttributes;
    public final boolean hasErrorMessages;

    public Result(Set<String> usedAttributes, boolean hasErrorMessages) {
      this.usedAttributes = usedAttributes;
      this.hasErrorMessages = hasErrorMessages;
    }
  }
}
