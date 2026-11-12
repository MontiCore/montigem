/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDCardinality;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * A bidirectional qualified association, by an attribute ({@code [[attr]]}) or a type ({@code [Type]}), needs the
 * cardinality of the qualified side (the owner), which tells in how many owners' maps a value may be:
 * {@code [1]} or {@code [0..1]} (one owner, a put moves the value) or {@code [*]} (several owners); the values then
 * navigate to their owner(s). That the association is navigable from the owner is checked by
 * {@link UMLPAssociationQualifierNavigability}.
 */
public class UMLPAssociationQualifierOwnerCardinality implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6D";

  public static final String ERROR_MSG_FORMAT = " The bidirectional association qualified by %s " +
      "needs the cardinality [1], [0..1] or [*] of the qualified class %s: in how many maps a value may be.";

  @Override
  public void check(ASTCDAssociation assoc) {
    boolean bidirectional = assoc.getCDAssocDir().isDefinitiveNavigableLeft()
        && assoc.getCDAssocDir().isDefinitiveNavigableRight();
    if (!bidirectional) {
      return;
    }
    if (UMLPAssociationQualifierNavigability.isQualified(assoc.getLeft())) {
      check(assoc.getLeft());
    }
    if (UMLPAssociationQualifierNavigability.isQualified(assoc.getRight())) {
      check(assoc.getRight());
    }
  }

  protected void check(ASTCDAssocSide qualifiedSide) {
    if (!hasOwnerCardinality(qualifiedSide)) {
      logError(qualifiedSide.getCDQualifier(),
          UMLPAssociationQualifierNavigability.printQualifier(qualifiedSide.getCDQualifier()),
          qualifiedSide.getMCQualifiedType().getMCQualifiedName().getQName());
    }
  }

  /**
   * whether the qualified side has the cardinality [1], [0..1] or [*], see the class comment
   */
  public static boolean hasOwnerCardinality(ASTCDAssocSide qualifiedSide) {
    if (!qualifiedSide.isPresentCDCardinality()) {
      return false;
    }
    ASTCDCardinality cardinality = qualifiedSide.getCDCardinality();
    return cardinality.isOne() || cardinality.isOpt() || cardinality.isMult();
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
