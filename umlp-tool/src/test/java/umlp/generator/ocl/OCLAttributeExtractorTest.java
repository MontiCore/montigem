package umlp.generator.ocl;

import de.monticore.ocl.ocl.OCLMill;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OCLAttributeExtractorTest {

  @BeforeEach
  void initTypeCheck() {
    LogStub.init();
    Log.enableFailQuick(false);
    OCLMill.reset();
    OCLMill.init();
    NoRegexOCLTypeCheck3.init();
  }

  @AfterEach
  void resetTypeCheck() {
    NoRegexOCLTypeCheck3.reset();
  }

  @Test
  void ignoresTypeQualifierWithoutTypeCheckingItAsExpression() {
    TypeSymbol context = addType("Person");
    TypeSymbol localDate = addType("LocalDate");
    var qualifier = OCLMill.nameExpressionBuilder().setName("LocalDate").build();
    qualifier.setEnclosingScope(OCLMill.globalScope());
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast().setTypeOfTypeIdentifierForName(
        qualifier,
        SymTypeExpressionFactory.createTypeObject(localDate)
    );
    var staticAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(qualifier)
        .setName("now")
        .build();

    OCLAttributeExtractor extractor = new OCLAttributeExtractor(
        "person",
        SymTypeExpressionFactory.createTypeObject(context)
    );

    assertDoesNotThrow(() -> extractor.visit(staticAccess));
    assertTrue(extractor.usedAttributes.isEmpty());
  }

  @Test
  void extractsFieldAccessOnContextExpression() {
    TypeSymbol context = addType("Person");
    var contextType = SymTypeExpressionFactory.createTypeObject(context);
    VariableSymbol person = new VariableSymbol("person");
    person.setType(contextType);
    OCLMill.globalScope().add(person);
    var qualifier = OCLMill.nameExpressionBuilder().setName("person").build();
    qualifier.setEnclosingScope(OCLMill.globalScope());
    var fieldAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(qualifier)
        .setName("age")
        .build();

    OCLAttributeExtractor extractor = new OCLAttributeExtractor("person", contextType);

    extractor.visit(fieldAccess);

    assertTrue(extractor.usedAttributes.contains("age"));
  }

  @Test
  void extractsFieldAccessOnCompoundContextExpression() {
    TypeSymbol context = addType("Person");
    var contextType = SymTypeExpressionFactory.createTypeObject(context);
    var person = OCLMill.nameExpressionBuilder().setName("person").build();
    var relatedPerson = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(person)
        .setName("relatedPerson")
        .build();
    relatedPerson.setEnclosingScope(OCLMill.globalScope());
    NoRegexOCLTypeCheck3.getDelegate().getType4Ast().setTypeOfExpression(
        relatedPerson,
        contextType
    );
    var fieldAccess = OCLMill.fieldAccessExpressionBuilder()
        .setExpression(relatedPerson)
        .setName("age")
        .build();

    OCLAttributeExtractor extractor = new OCLAttributeExtractor("person", contextType);

    extractor.visit(fieldAccess);

    assertTrue(extractor.usedAttributes.contains("age"));
  }

  private TypeSymbol addType(String name) {
    TypeSymbol type = new TypeSymbol(name);
    type.setSpannedScope(OCLMill.scope());
    OCLMill.globalScope().add(type);
    return type;
  }
}
