/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * The side of an attribute qualified association whose class holds the qualifying attribute (the values) has the
 * cardinality [1]: the keys are derived from the values, so every key stands for exactly one value (a qualified access
 * reaches one object). The qualifier side may have any cardinality.
 */
public class UMLPAssociationQualifierTargetCardinality implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6C";

  public static final String ERROR_MSG_FORMAT = " The class %s, whose %s qualifies the association, must have the " +
      "cardinality [1]: every key is derived from exactly one object.";

  @Override
  public void check(ASTCDAssociation assoc) {
    if (UMLPAssociationQualifierKeyCardinality.isAttributeQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getRight());
    }
    if (UMLPAssociationQualifierKeyCardinality.isAttributeQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getLeft());
    }
  }

  protected void check(ASTCDAssocSide qualifiedSide, ASTCDAssocSide targetSide) {
    if (!targetSide.isPresentCDCardinality() || !targetSide.getCDCardinality().isOne()) {
      logError(qualifiedSide.getCDQualifier(), targetSide.getMCQualifiedType().getMCQualifiedName(),
          qualifiedSide.getCDQualifier().getByAttributeName());
    }
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
