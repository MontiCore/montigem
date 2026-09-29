package umlp.generator.ocl.javagen;

import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.symbols.basicsymbols._symboltable.IBasicSymbolsScope;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.oosymbols._symboltable.FieldSymbol;
import de.monticore.symbols.oosymbols._symboltable.MethodSymbol;
import de.monticore.symboltable.ISymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types3.util.WithinScopeBasicSymbolsResolver;

import java.util.Optional;

final class OCLStaticMemberJavaGenHelper {

  private OCLStaticMemberJavaGenHelper() {
  }

  static Optional<SymTypeExpression> getOwnerType(
      ASTFieldAccessExpression node,
      SymTypeExpression memberType
  ) {
    Optional<SymTypeExpression> sourceOwner = memberType.getSourceInfo().getSourceSymbol()
        .filter(OCLStaticMemberJavaGenHelper::isStaticMember)
        .flatMap(OCLStaticMemberJavaGenHelper::getOwnerType);
    if (sourceOwner.isPresent()) {
      return sourceOwner;
    }
    return resolveQualifierType(node);
  }

  private static boolean isStaticMember(ISymbol symbol) {
    return (symbol instanceof FieldSymbol field && field.isIsStatic())
        || (symbol instanceof MethodSymbol method && method.isIsStatic());
  }

  private static Optional<SymTypeExpression> getOwnerType(ISymbol member) {
    var enclosingScope = member.getEnclosingScope();
    if (!enclosingScope.isPresentSpanningSymbol()
        || !(enclosingScope.getSpanningSymbol() instanceof TypeSymbol owner)) {
      return Optional.empty();
    }
    return Optional.of(SymTypeExpressionFactory.createTypeObject(owner));
  }

  private static Optional<SymTypeExpression> resolveQualifierType(
      ASTFieldAccessExpression node
  ) {
    if (!(node.getEnclosingScope() instanceof IBasicSymbolsScope enclosingScope)) {
      return Optional.empty();
    }
    return getQualifiedName(node.getExpression())
        .flatMap(name -> WithinScopeBasicSymbolsResolver.resolveType(enclosingScope, name));
  }

  static Optional<String> getQualifiedName(ASTExpression expression) {
    if (expression instanceof ASTNameExpression name) {
      return Optional.of(name.getName());
    }
    if (expression instanceof ASTFieldAccessExpression fieldAccess) {
      return getQualifiedName(fieldAccess.getExpression())
          .map(qualifier -> qualifier + "." + fieldAccess.getName());
    }
    return Optional.empty();
  }
}
