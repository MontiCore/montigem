/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.types3.TypeCheck3;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.symboltable.modifiers.BasicAccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.util.WithinTypeBasicSymbolsResolver;

import java.util.Optional;

/**
 * The qualifier of an attribute qualified association names the key of a target object: a single valued attribute or
 * association role of the target class. An optional ([0..1], Optional) or multi-valued ([*], [1..*], List, ...)
 * qualifier would give a target no key or several keys, which GemMaps do not support.
 */
public class UMLPAssociationQualifierKeyCardinality implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6B";

  public static final String ERROR_MSG_FORMAT = " Qualifier %s of a qualified association must be single valued " +
      "([1]) in class %s; optional and multi-valued attributes or roles are not supported as keys.";

  private final WithinTypeBasicSymbolsResolver resolver = new WithinTypeBasicSymbolsResolver();

  @Override
  public void check(ASTCDAssociation assoc) {
    if (isAttributeQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getRight());
    }
    if (isAttributeQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getLeft());
    }
  }

  protected void check(ASTCDAssocSide qualifiedSide, ASTCDAssocSide targetSide) {
    String qualifier = qualifiedSide.getCDQualifier().getByAttributeName();
    SymTypeExpression targetType = TypeCheck3.symTypeFromAST(targetSide.getMCQualifiedType());
    // a role of an association navigable from the target class
    Optional<ASTCDAssocSide> role = UMLPAssociationQualifierByAttributeName.findRole(targetType, qualifier);
    if (role.isPresent()) {
      if (role.get().isPresentCDCardinality() && !role.get().getCDCardinality().isOne()) {
        logError(qualifiedSide.getCDQualifier(), qualifier, targetSide.getMCQualifiedType().getMCQualifiedName());
      }
      return;
    }
    // an attribute of the target class (unknown names are reported by UMLPAssociationQualifierByAttributeName)
    Optional<SymTypeExpression> attributeType = resolver.resolveVariable(targetType, qualifier,
        BasicAccessModifier.ALL_INCLUSION, x -> true);
    if (attributeType.isPresent() && isOptionalOrMultiValued(attributeType.get())) {
      logError(qualifiedSide.getCDQualifier(), qualifier, targetSide.getMCQualifiedType().getMCQualifiedName());
    }
  }

  /**
   * Optional, collection or array types of attributes: no key or several keys per target object
   */
  public static boolean isOptionalOrMultiValued(SymTypeExpression type) {
    return type.isGenericType() || type.isArrayType();
  }

  protected static boolean isAttributeQualified(ASTCDAssocSide side) {
    return side.isPresentCDQualifier() && side.getCDQualifier().isPresentByAttributeName();
  }

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

  @Override
  public String getErrorMessageFormat() {
    return ERROR_MSG_FORMAT;
  }
}
