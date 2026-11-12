/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.types3.TypeCheck3;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.symboltable.modifiers.BasicAccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.util.WithinTypeBasicSymbolsResolver;

import umlp.generator.domain.decorator.GenService;

import java.util.Optional;


/**
 * The attribute that qualifies an attribute qualified association ({@code Owner [[attr]] -> Value}) has a type that
 * can be a key: a primitive or String (GemValueMap) or a domain class (GemEntryMap). Optional, collection and array
 * types are rejected as well, they would give a value object no key or several keys. That the attribute exists is
 * checked by {@link UMLPAssociationQualifierAttributeExists}, the cardinality of a role that is used as qualifier by
 * {@link UMLPAssociationQualifierKeyCardinality}.
 */
public class UMLPAssociationQualifierByAttributeName implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String TYPE_ERROR_CODE = "0xCDC68";

  public static final String TYPE_ERROR_MSG_FORMAT = " Qualified association with attribute %s " +
      "has incompatible type, qualified associations only support primitive (not float, double or char: Java and " +
      "the browser would disagree on their keys), String or domain class attributes as qualifiers.";

  private final WithinTypeBasicSymbolsResolver resolver;

  public UMLPAssociationQualifierByAttributeName(){
    this.resolver = new WithinTypeBasicSymbolsResolver();
  }

  @Override
  public void check(ASTCDAssociation assoc) {
    if (assoc.getLeft().isPresentCDQualifier() && assoc.getLeft().getCDQualifier()
        .isPresentByAttributeName()) {
      checkType(assoc.getLeft(), assoc.getRight());

    }else if (assoc.getRight().isPresentCDQualifier() && assoc.getRight().getCDQualifier()
        .isPresentByAttributeName()) {
      checkType(assoc.getRight(), assoc.getLeft());
    }
  }

  private void checkType(ASTCDAssocSide qSide, ASTCDAssocSide attrSide) {
    SymTypeExpression ste = TypeCheck3.symTypeFromAST(attrSide.getMCQualifiedType());
    final String attributeName = qSide.getCDQualifier().getByAttributeName();
    Optional<SymTypeExpression> expr = this.resolver.resolveVariable(ste, attributeName,
        BasicAccessModifier.ALL_INCLUSION, x -> true);
    if (expr.isPresent() && !isValueKey(expr.get()) && !isDomainKey(expr.get())) {
      logError(qSide.getCDQualifier().get_SourcePositionStart(),
               TYPE_ERROR_CODE,
               TYPE_ERROR_MSG_FORMAT,
               attributeName);
    }
    // a qualifier that is no attribute is a role (a domain object key) or does not exist
    // (UMLPAssociationQualifierAttributeExists)
  }

  /**
   * primitive (MontiCore typecheck, String included) qualifiers are keys of a GemValueMap, except the ones the
   * participants would not agree on (see {@link #isUnsupportedKey})
   */
  public static boolean isValueKey(SymTypeExpression type) {
    return (type.isPrimitive() || SymTypeRelations.isNumericType(type)
        || SymTypeRelations.isBoolean(type) || SymTypeRelations.isString(type)) && !isUnsupportedKey(type);
  }

  /**
   * float, double and char (also boxed) are no keys, as the participants would disagree on keys: a computed floating
   * point key rarely equals the one stored, -0.0 and 0.0 are two keys in Java but one in the browser, and the browser
   * does not round float values; a char is a number in Java but a string in the browser
   */
  public static boolean isUnsupportedKey(SymTypeExpression type) {
    return SymTypeRelations.isFloat(type) || SymTypeRelations.isDouble(type) || SymTypeRelations.isChar(type);
  }

  /**
   * domain class qualifiers are keys of a GemEntryMap
   */
  public static boolean isDomainKey(SymTypeExpression type) {
    return type.hasTypeInfo() && GenService.isDomainType(type.getTypeInfo()) && !GenService.isCDEnum(type);
  }

  @Override
  public String getErrorCode() {
    return TYPE_ERROR_CODE;
  }

  @Override
  public String getErrorMessageFormat() {
    return TYPE_ERROR_MSG_FORMAT;
  }
}
