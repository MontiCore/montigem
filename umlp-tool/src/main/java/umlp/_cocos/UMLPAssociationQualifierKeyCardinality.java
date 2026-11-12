/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.types3.TypeCheck3;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.types.check.SymTypeExpression;

import java.util.Optional;

/**
 * The qualifier of an attribute qualified association may name an association role of the target class instead of an
 * attribute. Such a role has cardinality [1]: an optional ([0..1]) or multi-valued ([*], [1..*]) role would give a
 * target no key or several keys, which GemMaps do not support. Optional and multi-valued attribute types are reported
 * by {@link UMLPAssociationQualifierByAttributeName}.
 */
public class UMLPAssociationQualifierKeyCardinality implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6B";

  public static final String ERROR_MSG_FORMAT = " Association role %s that qualifies a qualified association must " +
      "have cardinality [1] in class %s; optional and multi-valued roles are not supported as keys.";

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
    // a role of an association navigable from the target class; attributes have no cardinality
    Optional<ASTCDAssocSide> role = UMLPAssociationQualifierAttributeExists.findRole(targetType, qualifier);
    if (role.isPresent() && role.get().isPresentCDCardinality() && !role.get().getCDCardinality().isOne()) {
      logError(qualifiedSide.getCDQualifier(), qualifier, targetSide.getMCQualifiedType().getMCQualifiedName());
    }
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
