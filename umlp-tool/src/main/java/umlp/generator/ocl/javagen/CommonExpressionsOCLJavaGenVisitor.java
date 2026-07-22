package umlp.generator.ocl.javagen;


import de.monticore.codegen.javagen.JavaGenVisitorState;
import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions.codegen.javagen.CommonExpressionsJavaGenVisitor;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.ocl.types3.OCLCollectionSymTypeRelations;
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symboltable.ISymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import umlp.generator.ocl.TypeUtil;
import umlp.generator.ocl.javagen.field_access.FieldAccessInfo;

import java.util.Optional;

public class CommonExpressionsOCLJavaGenVisitor extends CommonExpressionsJavaGenVisitor {
  public CommonExpressionsOCLJavaGenVisitor(JavaGenVisitorState state) {
    super(state);
  }

  @Override
  public void handle(ASTCallExpression node) {
    boolean isFieldAccess = OCLWithErrorMessagesMill.typeDispatcher().isCommonExpressionsASTFieldAccessExpression(node.getExpression());
    boolean handled = false;

    if (isFieldAccess) {
      ASTFieldAccessExpression fa = OCLWithErrorMessagesMill.typeDispatcher().asCommonExpressionsASTFieldAccessExpression(node.getExpression());
      handled = handleTranslatedListCall(fa, node);
    }

    if (!handled) {
      state.startParentheses();
      node.getExpression().accept(getTraverser());
      state.startParentheses();
      boolean first = true;
      for (ASTExpression arg : node.getArguments().getExpressionList()) {
        if (!first) {
          getPrinter().print(", ");
        }
        arg.accept(getTraverser());
        first = false;
      }
      state.endParentheses();
      state.endParentheses();
    }
  }

  private boolean handleTranslatedListCall(ASTFieldAccessExpression fa, ASTCallExpression node) {
    return switch (fa.getName()) {
      case "asSet" -> {
        String setName = nextListOperationVariable();
        String itemName = setName + "_item";
        printCollectionSupplierStart(node, setName, "java.util.HashSet");
        getPrinter().print("for (Object ");
        getPrinter().print(itemName);
        getPrinter().print(" : ");
        printReceiver(fa);
        getPrinter().println(") {");
        getPrinter().indent();
        getPrinter().print(setName);
        getPrinter().print(".add((");
        printCollectionElementType(node);
        getPrinter().print(") ");
        getPrinter().print(itemName);
        getPrinter().println(");");
        getPrinter().unindent();
        getPrinter().println("}");
        printListSupplierEnd(setName);
        yield true;
      }
      case "asList" -> {
        String listName = nextListOperationVariable();
        printCollectionSupplierStart(node, listName, "java.util.ArrayList");
        getPrinter().print(listName);
        getPrinter().println(".addAll(");
        printReceiver(fa);
        getPrinter().println(");");
        printListSupplierEnd(listName);
        yield true;
      }
      case "first" -> {
        printReceiverInParentheses(fa);
        getPrinter().print(".get(0)");
        yield true;
      }
      case "last" -> {
        printReceiverInParentheses(fa);
        getPrinter().print(".get(");
        printReceiverInParentheses(fa);
        getPrinter().print(".size() - 1)");
        yield true;
      }
      case "rest" -> {
        String listName = nextListOperationVariable();
        printListSupplierStart(node, listName);
        getPrinter().print(listName);
        getPrinter().println(".addAll(");
        printReceiver(fa);
        getPrinter().println(");");
        getPrinter().print("if (!");
        getPrinter().print(listName);
        getPrinter().println(".isEmpty()) {");
        getPrinter().indent();
        getPrinter().print(listName);
        getPrinter().println(".remove(0);");
        getPrinter().unindent();
        getPrinter().println("}");
        printListSupplierEnd(listName);
        yield true;
      }
      case "count" -> {
        if (node.getArguments().getExpressionList().size() != 1) {
          yield false;
        }
        String countName = nextListOperationVariable();
        String itemName = countName + "_item";
        getPrinter().println("((java.util.function.Supplier<Integer>)()->{");
        getPrinter().indent();
        getPrinter().print("int ");
        getPrinter().print(countName);
        getPrinter().println(" = 0;");
        getPrinter().print("for (Object ");
        getPrinter().print(itemName);
        getPrinter().print(" : ");
        printReceiver(fa);
        getPrinter().println(") {");
        getPrinter().indent();
        getPrinter().print("if (java.util.Objects.equals(");
        getPrinter().print(itemName);
        getPrinter().print(", ");
        printArgument(node, 0);
        getPrinter().println(")) {");
        getPrinter().indent();
        getPrinter().print(countName);
        getPrinter().println("++;");
        getPrinter().unindent();
        getPrinter().println("}");
        getPrinter().unindent();
        getPrinter().println("}");
        getPrinter().print("return ");
        getPrinter().print(countName);
        getPrinter().println(";");
        getPrinter().unindent();
        getPrinter().print("}).get()");
        yield true;
      }
      case "removeAtIndex" -> {
        if (node.getArguments().getExpressionList().size() != 1) {
          yield false;
        }
        String listName = nextListOperationVariable();
        printListSupplierStart(node, listName);
        getPrinter().print(listName);
        getPrinter().println(".addAll(");
        printReceiver(fa);
        getPrinter().println(");");
        getPrinter().print(listName);
        getPrinter().print(".remove((int) ");
        printArgument(node, 0);
        getPrinter().println(");");
        printListSupplierEnd(listName);
        yield true;
      }
      case "flatten" -> {
        String listName = nextListOperationVariable();
        String itemName = listName + "_item";
        printListSupplierStart(node, listName);
        getPrinter().print("for (java.util.Collection<?> ");
        getPrinter().print(itemName);
        getPrinter().print(" : ");
        printReceiver(fa);
        getPrinter().println(") {");
        getPrinter().indent();
        getPrinter().print(listName);
        getPrinter().print(".addAll(");
        getPrinter().print(itemName);
        getPrinter().println(");");
        getPrinter().unindent();
        getPrinter().println("}");
        printListSupplierEnd(listName);
        yield true;
      }
      default -> false;
    };
  }

  private int listOperationVariableCounter = 0;

  private String nextListOperationVariable() {
    return "__oclListOp" + listOperationVariableCounter++;
  }

  private void printListSupplierStart(ASTCallExpression node, String variableName) {
    printCollectionSupplierStart(node, variableName, "java.util.ArrayList");
  }

  private void printCollectionSupplierStart(ASTCallExpression node, String variableName, String implementationType) {
    getPrinter().print("((java.util.function.Supplier<");
    getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(TypeCheck3.typeOf(node)));
    getPrinter().println(">)()->{");
    getPrinter().indent();
    getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(TypeCheck3.typeOf(node)));
    getPrinter().print(" ");
    getPrinter().print(variableName);
    getPrinter().print(" = new ");
    getPrinter().print(implementationType);
    getPrinter().println("<>();");
  }

  private void printListSupplierEnd(String listName) {
    getPrinter().print("return ");
    getPrinter().print(listName);
    getPrinter().println(";");
    getPrinter().unindent();
    getPrinter().print("}).get()");
  }

  private void printReceiver(ASTFieldAccessExpression fa) {
    fa.getExpression().accept(getTraverser());
  }

  private void printReceiverInParentheses(ASTFieldAccessExpression fa) {
    state.startParentheses();
    printReceiver(fa);
    state.endParentheses();
  }

  private void printArgument(ASTCallExpression node, int index) {
    node.getArguments().getExpression(index).accept(getTraverser());
  }

  private void printCollectionElementType(ASTCallExpression node) {
    SymTypeExpression collectionType = TypeCheck3.typeOf(node);
    if (collectionType.isGenericType() && !collectionType.asGenericType().getArgumentList().isEmpty()) {
      getPrinter().print(SymTypeExpression2JavaConverter.getJavaTypePrint(
          collectionType.asGenericType().getArgument(0)));
    } else {
      getPrinter().print("Object");
    }
  }

  @Override
  public void handle(ASTFieldAccessExpression node) {
    SymTypeExpression nodeType = TypeCheck3.typeOf(node);
    // Qualifiers of static members can be package or type names without an expression type.
    // The outermost field access is an expression, so its source symbol or qualifier provides
    // the static owner without type-checking the qualifier as an expression.
    Optional<SymTypeExpression> staticMemberOwner =
        OCLStaticMemberJavaGenHelper.getOwnerType(node, nodeType);
    if (staticMemberOwner.isPresent()) {
      getPrinter().print(
          SymTypeExpression2JavaConverter.getJavaTypePrint(staticMemberOwner.get()));
      getPrinter().print(".");
      getPrinter().print(node.getName());
      return;
    }

    // functions should not be replaced by getter name conversion
    if (nodeType.isFunctionType()) {
      node.getExpression().accept(getTraverser());
      getPrinter().print(".");
      getPrinter().print(node.getName());
      return;
    }

    SymTypeExpression lhsType = TypeCheck3.typeOf(node.getExpression());
    SymTypeExpression fullType = nodeType;
    if (OCLCollectionSymTypeRelations.isOCLCollection(lhsType) && OCLCollectionSymTypeRelations.isOCLCollection(fullType)) {
      // flattening

      // check if source type
      boolean isCollectionType = false;
      boolean isDomainType = false;
      boolean isBoolean = false;
      SymTypeExpression varType = null;
      Optional<ISymbol> sourceSymbolOpt = fullType.getSourceInfo().getSourceSymbol();
      if (sourceSymbolOpt.isEmpty()) {
        Log.warn("Can not convert field access since source symbol is not available", node.get_SourcePositionStart(), node.get_SourcePositionEnd());
        return;
      }
      ISymbol sourceSymbol = sourceSymbolOpt.get();
      if (BasicSymbolsMill.typeDispatcher().isBasicSymbolsVariable(sourceSymbol)) {
        VariableSymbol varSymbol = BasicSymbolsMill.typeDispatcher().asBasicSymbolsVariable(sourceSymbol);
        varType = varSymbol.getType();
        isCollectionType = TypeUtil.getInstance().isCollectionType(varType);
        isDomainType = TypeUtil.getInstance().isDomainType(varType);
        isBoolean = SymTypeRelations.isBoolean(varType);
      }

      // - variable with collection simple type
      if (isCollectionType && !isDomainType) {
        Log.warn("Not translating flattening for attribute list of simple type");
        return;
      }

      printStreamCollectionStart(lhsType);
      node.getExpression().accept(getTraverser());

      // - variable with simple type
      if (!isCollectionType && !isDomainType) {
        printFlatteningMap(node.getName(), isBoolean ? "is" : "get", lhsType);
      }

      // - association with opt or one cardinality
      if (!isCollectionType && isDomainType) {
        // TODO: opt needs extra check
        printFlatteningMap(node.getName(), "get", lhsType);
      }

      // - association with opt mult cardinality
      if (isCollectionType && isDomainType) {
        getPrinter().print(".stream().flatMap(it -> it.");
        getPrinter().print("get");
        getPrinter().print(StringTransformations.capitalize(node.getName()));
        getPrinter().print("List().stream())");
        printStreamCollectionEnd(lhsType);
      }
    } else {
      // normal access
      node.getExpression().accept(getTraverser());
      FieldAccessInfo info = FieldAccessInfo.from(node, lhsType, fullType);
      printFieldAccess(info, node.getName());
    }
  }

  private void printFlatteningMap(String fieldName, String accessorPrefix, SymTypeExpression lhsType) {
    getPrinter().print(".stream().map(it -> it.");
    getPrinter().print(accessorPrefix);
    getPrinter().print(StringTransformations.capitalize(fieldName));
    getPrinter().print("())");
    printStreamCollectionEnd(lhsType);
  }

  private void printStreamCollectionStart(SymTypeExpression lhsType) {
    if (OCLCollectionSymTypeRelations.isSet(lhsType)) {
      getPrinter().print("new java.util.HashSet<>(");
    }
  }

  private void printStreamCollectionEnd(SymTypeExpression lhsType) {
    getPrinter().print(".toList()");
    if (OCLCollectionSymTypeRelations.isSet(lhsType)) {
      getPrinter().print(")");
    }
  }

  private void printFieldAccess(FieldAccessInfo info, String nodeName) {
    switch (info.getKind()) {
      case TO_ONE_DOMAIN ->
          getPrinter().print(".get" + StringTransformations.capitalize(nodeName) + "()");
      case TO_MANY_DOMAIN, COLLECTION_OF_SIMPLE_VALUE ->
          getPrinter().print(".get" + StringTransformations.capitalize(nodeName) + "List()");
      case SIMPLE_VALUE -> {
        if (SymTypeRelations.isBoolean(info.getFieldType())) {
          getPrinter().print(".is" + StringTransformations.capitalize(nodeName) + "()");
        } else {
          getPrinter().print(".get" + StringTransformations.capitalize(nodeName) + "()");
        }
      }
    }
  }
}
