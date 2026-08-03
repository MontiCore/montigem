package umlp.generator.ocl.javagen;

import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.ocl._ast.ASTOCLContextDefinition;
import de.monticore.ocl.ocl._ast.ASTOCLParamDeclaration;
import de.monticore.ocl.ocl._visitor.OCLVisitor2;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types3.TypeCheck3;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import oclwitherrormessages._visitor.OCLWithErrorMessagesTraverser;
import umlp.generator.ocl.TypeUtil;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Collects the Java imports required for types referenced by an OCL artifact. */
public class OCLJavaImportCollector implements CommonExpressionsVisitor2, OCLVisitor2 {
  protected final Set<String> imports = new TreeSet<>();

  public static Set<String> collect(ASTOCLCompilationUnit node) {
    OCLJavaImportCollector collector = new OCLJavaImportCollector();

    node.getMCImportStatementList().stream()
        .filter(importStatement -> !importStatement.isStar())
        .forEach(importStatement -> OCLWithErrorMessagesMill.globalScope()
            .resolveType(importStatement.getQName())
            .map(SymTypeExpressionFactory::createTypeObject)
            .filter(type -> !TypeUtil.getInstance().isDomainType(type))
            .ifPresent(collector::addImport));

    OCLWithErrorMessagesTraverser traverser = OCLWithErrorMessagesMill.traverser();
    traverser.add4CommonExpressions(collector);
    traverser.add4OCL(collector);
    node.accept(traverser);
    return collector.getImports();
  }

  @Override
  public void visit(ASTOCLParamDeclaration node) {
    addDomainType(TypeCheck3.symTypeFromAST(node.getMCType()));
  }

  @Override
  public void visit(ASTOCLContextDefinition node) {
    if (node.isPresentMCType()) {
      addDomainType(TypeCheck3.symTypeFromAST(node.getMCType()));
    }
  }

  @Override
  public void visit(ASTFieldAccessExpression node) {
    addDomainType(TypeCheck3.typeOf(node));
  }

  protected void addDomainType(SymTypeExpression type) {
    if (type == null) {
      return;
    }
    if (type.isGenericType()) {
      type.asGenericType().getArgumentList().forEach(this::addDomainType);
    } else if (type.isArrayType()) {
      addDomainType(type.asArrayType().getArgument());
    } else if (TypeUtil.getInstance().isDomainType(type)) {
      addImport(type);
    }
  }

  protected void addImport(SymTypeExpression type) {
    imports.add(SymTypeExpression2JavaConverter.getJavaTypeQName(type));
  }

  public Set<String> getImports() {
    return Collections.unmodifiableSet(imports);
  }
}
