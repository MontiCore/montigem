package umlp.generator.ocl.javagen;

import de.monticore.codegen.CodeGenSymTypeExpressionConverter;
import de.monticore.codegen.javagen.JavaGenSymTypeRelations;
import de.monticore.codegen.javagen.JavaGenVisitorState;
import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.ocl.oclexpressions._ast.ASTInDeclaration;
import de.monticore.ocl.oclexpressions._ast.ASTInDeclarationVariable;
import de.monticore.ocl.oclexpressions._ast.ASTTypeIfExpression;
import de.monticore.ocl.oclexpressions.codegen.javagen.OCLExpressionsJavaGenVisitor;
import de.monticore.symbols.basicsymbols._symboltable.IBasicSymbolsScope;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.TypeCheck3;
import de.monticore.types3.util.WithinScopeBasicSymbolsResolver;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.logging.Log;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.ocl.TypeUtil;

import java.util.Optional;

public class OCLExpressionsUMLPJavaGenVisitor extends OCLExpressionsJavaGenVisitor {
  protected final OCLTypeNarrowingContext typeNarrowingContext;

  public OCLExpressionsUMLPJavaGenVisitor(JavaGenVisitorState state, OCLTypeNarrowingContext typeNarrowingContext) {
    super(state);
    this.typeNarrowingContext = typeNarrowingContext;
  }

  @Override
  public void traverse(ASTTypeIfExpression node) {
    SymTypeExpression typeIfType = SymTypeRelations.normalize(TypeCheck3.typeOf(node));
    SymTypeExpression narrowedType = SymTypeRelations.normalize(TypeCheck3.symTypeFromAST(node.getMCType()));
    String varName = getVarName(node);

    if (!JavaGenSymTypeRelations.generatesToJavaRuntimeIdentifiableType(narrowedType)) {
      Log.error(
          "0x6785C typeif cannot check type " + narrowedType.printFullName()
              + " at runtime",
          node.get_SourcePositionStart(), node.get_SourcePositionEnd());
    }

    state.printExpressionBeginLambda(typeIfType);
    getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(typeIfType));
    getPrinter().print(" ");
    getPrinter().print(varName);
    state.endStatement();

    getPrinter().print("if (");
    getPrinter().print(node.getName());
    getPrinter().print(" instanceof ");
    getPrinter().print(SymTypeExpression2JavaConverter.getTypeErasedJavaTypePrint(narrowedType));
    getPrinter().println(") {");
    getPrinter().indent();
    getPrinter().print(varName);
    getPrinter().print(" = ");
    typeNarrowingContext.push(node.getName(), narrowedType);
    CodeGenSymTypeExpressionConverter.printConverted(
        getPrinter(),
        typeIfType,
        TypeCheck3.typeOf(node.getThenExpression().getExpression()),
        printer -> node.getThenExpression().accept(getTraverser()));
    typeNarrowingContext.pop(node.getName());
    state.endStatement();
    getPrinter().unindent();

    getPrinter().println("} else {");
    getPrinter().indent();
    getPrinter().print(varName);
    getPrinter().print(" = ");
    CodeGenSymTypeExpressionConverter.printConverted(
        getPrinter(),
        typeIfType,
        TypeCheck3.typeOf(node.getElseExpression()),
        printer -> node.getElseExpression().accept(getTraverser()));
    state.endStatement();
    getPrinter().unindent();
    getPrinter().println("}");

    getPrinter().print("return ");
    getPrinter().print(varName);
    state.endStatement();
    state.printExpressionEndLambda();
  }

  @Override
  public void traverse(ASTInDeclaration node) {
    Optional<SymTypeExpression> domainType = getDomainTypeIfNoVar(node);
    Optional<SymTypeExpression> innerType = getInnerType(node);
    if (domainType.isEmpty() || innerType.isEmpty()) {
      super.traverse(node);
      return;
    }

    String innerTypeStr = SymTypeExpression2JavaConverter.getJavaTypePrint(innerType.get());
    String fullName = domainType.get().getTypeInfo().getFullName();
    String cdQName = Names.getQualifier(fullName);

    String allInstances = cdQName.toLowerCase()
        + "." + ManagerDecorator.getClassName(Names.getSimpleName(cdQName))
        + ".get" + Names.getSimpleName(fullName) + "List()";

    for (ASTInDeclarationVariable var : node.getInDeclarationVariableList()) {
      getPrinter().print("for (");
      getPrinter().print(innerTypeStr);
      getPrinter().print(" ");
      getPrinter().print(var.getName());
      getPrinter().print(" : ");
      getPrinter().print(allInstances);
      getPrinter().println(") {");
      getPrinter().indent();
    }
  }

  @Override
  protected Optional<SymTypeExpression> getInnerType(ASTInDeclaration node) {
    if (!node.isPresentMCType()) {
      Optional<SymTypeExpression> domainType = getDomainTypeIfNoVar(node);
      if (domainType.isPresent()) {
        return domainType;
      }
    }
    return super.getInnerType(node);
  }

  protected Optional<SymTypeExpression> getDomainTypeIfNoVar(ASTInDeclaration node) {
    if (!node.isPresentExpression()) {
      return Optional.empty();
    }
    ASTExpression expr = node.getExpression();
    if (!(expr.getEnclosingScope() instanceof IBasicSymbolsScope enclosingScope)) {
      return Optional.empty();
    }
    Optional<String> name = OCLStaticMemberJavaGenHelper.getQualifiedName(expr);
    if (name.isEmpty()
        || WithinScopeBasicSymbolsResolver.resolveNameAsExpr(enclosingScope, name.get()).isPresent()) {
      // variables take precedence over types
      return Optional.empty();
    }
    return WithinScopeBasicSymbolsResolver.resolveType(enclosingScope, name.get())
        .map(SymTypeRelations::normalize)
        .filter(type -> type.isObjectType() && TypeUtil.getInstance().isDomainType(type));
  }
}
