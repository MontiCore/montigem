package umlp.generator.ocl.javagen;

import de.monticore.codegen.javagen.JavaGenVisitorState;
import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symbols.oosymbols._symboltable.FieldSymbol;
import de.monticore.symbols.oosymbols._symboltable.IOOSymbolsScope;
import de.monticore.symbols.oosymbols._symboltable.MethodSymbol;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.generator.ocl.NoRegexOCLTypeCheck3;
import umlp.generator.ocl.TypeUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonExpressionsOCLJavaGenVisitorTest {

  @BeforeEach
  void initTypeCheck() {
    LogStub.init();
    Log.enableFailQuick(false);
    OCLMill.reset();
    OCLMill.init();
    NoRegexOCLTypeCheck3.init();
    SymTypeExpression2UMLPJavaConverter.init();
    TypeUtil.getInstance().init(List.of());
  }

  @AfterEach
  void resetTypeCheck() {
    NoRegexOCLTypeCheck3.reset();
    SymTypeExpression2JavaConverter.reset();
  }

  @Test
  void printsStaticFieldFromOutermostExpressionType() {
    TypeUtil.getInstance().init(List.of("Zeiterfassung"));
    TypeSymbol owner = mockOwnerType("Zeiterfassung.Eintragstyp");
    IOOSymbolsScope ownerScope = mockOwnerScope(owner);
    FieldSymbol field = mock(FieldSymbol.class);
    when(field.isIsStatic()).thenReturn(true);
    when(field.getEnclosingScope()).thenReturn(ownerScope);

    var qualifier = OCLMill.nameExpressionBuilder().setName("Eintragstyp").build();
    var staticAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(qualifier)
        .setName("PAUSE")
        .build();
    var memberType = SymTypeExpressionFactory.createTypeObject(owner);
    memberType.getSourceInfo().setSourceSymbol(field);
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast()
        .setTypeOfExpression(staticAccess, memberType);

    assertEquals("zeiterfassung.Eintragstyp.PAUSE", print(staticAccess));
  }

  @Test
  void printsStaticMethodWithQualifiedOwnerFromOutermostExpressionType() {
    TypeSymbol owner = mockOwnerType("java.time.LocalDateTime");
    IOOSymbolsScope ownerScope = mockOwnerScope(owner);
    MethodSymbol method = mock(MethodSymbol.class);
    when(method.isIsStatic()).thenReturn(true);
    when(method.getEnclosingScope()).thenReturn(ownerScope);

    var java = OCLMill.nameExpressionBuilder().setName("java").build();
    var javaTime = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(java)
        .setName("time")
        .build();
    var localDateTime = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(javaTime)
        .setName("LocalDateTime")
        .build();
    var staticAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(localDateTime)
        .setName("now")
        .build();
    var memberType = SymTypeExpressionFactory.createFunction(
        SymTypeExpressionFactory.createTypeObject(owner));
    memberType.getSourceInfo().setSourceSymbol(method);
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast()
        .setTypeOfExpression(staticAccess, memberType);

    assertEquals("java.time.LocalDateTime.now", print(staticAccess));
  }

  @Test
  void resolvesStaticOwnerWithoutTypeCheckingQualifierAsExpression() {
    TypeSymbol owner = new TypeSymbol("Eintragstyp");
    owner.setSpannedScope(OCLMill.scope());
    OCLMill.globalScope().add(owner);

    var qualifier = OCLMill.nameExpressionBuilder().setName("Eintragstyp").build();
    qualifier.setEnclosingScope(OCLMill.globalScope());
    var staticAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(qualifier)
        .setName("PAUSE")
        .build();
    staticAccess.setEnclosingScope(OCLMill.globalScope());
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast().setTypeOfExpression(
        staticAccess, SymTypeExpressionFactory.createTypeObject(owner));

    assertEquals("Eintragstyp.PAUSE", print(staticAccess));
  }

  @Test
  void collectsFlattenedListWithToList() {
    assertEquals(
        "items.stream().map(it -> it.getValue()).toList()",
        printFlattenedFieldAccess("List"));
  }

  @Test
  void wrapsFlattenedSetListInHashSet() {
    assertEquals(
        "new java.util.HashSet<>(items.stream().map(it -> it.getValue()).toList())",
        printFlattenedFieldAccess("Set"));
  }

  @Test
  void collectsFlattenedCollectionWithToList() {
    assertEquals(
        "items.stream().map(it -> it.getValue()).toList()",
        printFlattenedFieldAccess("Collection"));
  }

  @Test
  void printsCalleeAsMethodWhenItsTypeIsNoFunction() {
    // the type of an overloaded method like String.startsWith is not always a function type
    var receiver = OCLMill.nameExpressionBuilder().setName("title").build();
    var callee = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(receiver)
        .setName("startsWith")
        .build();
    var call = OCLMill.callExpressionBuilder()
        .setExpression(callee)
        .setArguments(OCLMill.argumentsBuilder()
            .addExpression(OCLMill.nameExpressionBuilder().setName("prefix").build())
            .build())
        .build();
    var stringType = SymTypeExpressionFactory.createTypeObject(new TypeSymbol("String"));
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast().setTypeOfExpression(receiver, stringType);
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast()
        .setTypeOfExpression(callee, SymTypeExpressionFactory.createObscureType());

    IndentPrinter printer = new IndentPrinter();
    var generator = new OCL2UMLPJavaCodeGenerator(printer);
    call.accept(generator.getTraverser());

    assertEquals("(title.startsWith(prefix))", printer.getContent());
  }

  private TypeSymbol mockOwnerType(String fullName) {
    TypeSymbol owner = mock(TypeSymbol.class);
    when(owner.getFullName()).thenReturn(fullName);
    return owner;
  }

  private IOOSymbolsScope mockOwnerScope(TypeSymbol owner) {
    IOOSymbolsScope ownerScope = mock(IOOSymbolsScope.class);
    when(ownerScope.isPresentSpanningSymbol()).thenReturn(true);
    when(ownerScope.getSpanningSymbol()).thenReturn(owner);
    return ownerScope;
  }

  private String print(ASTFieldAccessExpression node) {
    IndentPrinter printer = new IndentPrinter();
    var visitor = new CommonExpressionsOCLJavaGenVisitor(new JavaGenVisitorState(printer));
    visitor.handle(node);
    return printer.getContent();
  }

  private String printFlattenedFieldAccess(String collectionName) {
    var itemType = SymTypeExpressionFactory.createTypeObject(new TypeSymbol("Item"));
    var collectionType = SymTypeExpressionFactory.createGenerics(
        new TypeSymbol(collectionName), List.of(itemType));
    var field = new VariableSymbol("value");
    field.setType(itemType);
    collectionType.getSourceInfo().setSourceSymbol(field);

    var receiver = OCLMill.nameExpressionBuilder().setName("items").build();
    var fieldAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(receiver)
        .setName("value")
        .build();
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast()
        .setTypeOfExpression(receiver, collectionType);
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast()
        .setTypeOfExpression(fieldAccess, collectionType);

    IndentPrinter printer = new IndentPrinter();
    var generator = new OCL2UMLPJavaCodeGenerator(printer);
    fieldAccess.accept(generator.getTraverser());
    return printer.getContent();
  }
}
