/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * The value side of a qualified association, by an attribute ({@code [[attr]]}) or a type ({@code [Type]}), has the
 * cardinality [1]: every key stands for exactly one value (a qualified access reaches one object), whether the key
 * is derived from the value or chosen on put. The qualifier side may have any cardinality.
 */
public class UMLPAssociationQualifierTargetCardinality implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6C";

  public static final String ERROR_MSG_FORMAT = " The class %s of the association qualified by %s must have the " +
      "cardinality [1]: every key stands for exactly one object.";

  @Override
  public void check(ASTCDAssociation assoc) {
    if (UMLPAssociationQualifierUnidirectional.isQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getRight());
    }
    if (UMLPAssociationQualifierUnidirectional.isQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getLeft());
    }
  }

  protected void check(ASTCDAssocSide qualifiedSide, ASTCDAssocSide targetSide) {
    if (!targetSide.isPresentCDCardinality() || !targetSide.getCDCardinality().isOne()) {
      logError(qualifiedSide.getCDQualifier(), targetSide.getMCQualifiedType().getMCQualifiedName(),
          UMLPAssociationQualifierUnidirectional.printQualifier(qualifiedSide.getCDQualifier()));
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
