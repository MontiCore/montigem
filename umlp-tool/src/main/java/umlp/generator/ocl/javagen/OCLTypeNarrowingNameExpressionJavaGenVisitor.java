package umlp.generator.ocl.javagen;

import de.monticore.codegen.javagen.JavaGenVisitorState;
import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.expressions.expressionsbasis.codegen.javagen.ExpressionsBasisJavaGenVisitor;
import de.monticore.symbols.basicsymbols._symboltable.IBasicSymbolsScope;
import de.monticore.symbols.oosymbols._symboltable.FieldSymbol;
import de.monticore.symbols.oosymbols._symboltable.MethodSymbol;
import de.monticore.symboltable.IScopeSpanningSymbol;
import de.monticore.symboltable.ISymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeOfFunction;
import de.monticore.types3.util.WithinScopeBasicSymbolsResolver;
import de.se_rwth.commons.logging.Log;

import java.util.Optional;

import static de.monticore.types3.SymTypeRelations.normalize;

public class OCLTypeNarrowingNameExpressionJavaGenVisitor extends ExpressionsBasisJavaGenVisitor {
  protected final OCLTypeNarrowingContext typeNarrowingContext;

  public OCLTypeNarrowingNameExpressionJavaGenVisitor(JavaGenVisitorState state, OCLTypeNarrowingContext typeNarrowingContext) {
    super(state);
    this.typeNarrowingContext = typeNarrowingContext;
  }

  @Override
  public void traverse(ASTNameExpression node) {
    Optional<SymTypeExpression> exprType = resolveExpressionType(node);
    Optional<ISymbol> sourceSymbol =
        exprType.flatMap(type -> type.getSourceInfo().getSourceSymbol());

    // static field
    if (sourceSymbol.isPresent() &&
        sourceSymbol.get() instanceof FieldSymbol fieldSymbol &&
        fieldSymbol.isIsStatic()) {
      getPrinter().print(fieldSymbol.getFullName());
    }

    // function references
    else if (exprType.isPresent()
        && exprType.get().isFunctionType()
        && exprType.get().asFunctionType().hasSymbol()) {
      SymTypeExpression resolvedExprType = exprType.get();
      SymTypeOfFunction funcType = resolvedExprType.asFunctionType();
      String funcName = funcType.getSymbol().getName();

      getPrinter().print("((");
      getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(resolvedExprType));
      getPrinter().print(") ");
      if (funcType.getSymbol() instanceof MethodSymbol) {
        MethodSymbol methodSym = (MethodSymbol) funcType.getSymbol();
        if (!methodSym.getSpannedScope().isPresentSpanningSymbol()) {
          Log.error("0xFD227 internal error: "
                  + "method symbol has no enclosing (OO)type: "
                  + methodSym.getFullName(),
              methodSym.getSourcePosition()
          );
          return;
        }
        IScopeSpanningSymbol owner =
            methodSym.getEnclosingScope().getSpanningSymbol();
        if (methodSym.isIsConstructor()) {
          getPrinter().print(owner.getFullName());
          getPrinter().print("::new");
        }
        else if (methodSym.isIsStatic()) {
          getPrinter().print(owner.getFullName());
          getPrinter().print("::");
          getPrinter().print(funcName);
        }
        else {
          // m() non-static can only be of this
          getPrinter().print("this");
          getPrinter().print("::");
          getPrinter().print(funcName);
        }
      }
      else {
        String funcFullName = funcType.getSymbol().getFullName();
        // assumed to always have a "."
        String javaFuncName =
            funcName.substring(0, funcFullName.lastIndexOf("."))
                + "::"
                + funcName.substring(funcFullName.lastIndexOf(".") + 1);
        getPrinter().print(javaFuncName);
      }
      getPrinter().print(")");
    } else {
      Optional<SymTypeExpression> narrowedType = typeNarrowingContext.get(node.getName());
      if(narrowedType.isPresent()){
        getPrinter().print("((");
        getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(narrowedType.get()));
        getPrinter().print(")");
      }

      if ("this".equals(node.getName())) {
        getPrinter().print("_this");
      } else {
        // variables
        getPrinter().print(node.getName());
      }

      if(narrowedType.isPresent()){
        getPrinter().print(")");
      }
    }
  }

  protected Optional<SymTypeExpression> resolveExpressionType(ASTNameExpression node) {
    if (!(node.getEnclosingScope() instanceof IBasicSymbolsScope enclosingScope)) {
      return Optional.empty();
    }
    return WithinScopeBasicSymbolsResolver.resolveNameAsExpr(enclosingScope, node.getName())
        .map(type -> normalize(type));
  }
}
