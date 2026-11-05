/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDCardinality;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDQualifier;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * Qualified associations, by an attribute ({@code [[attr]]}) or a type ({@code [Type]}), are generated as GemMaps of
 * the qualified side (the owner), so they must be navigable from the qualified side (0xCDC6A). A bidirectional one
 * also needs the cardinality of the qualified side, which tells in how many owners' maps a value may be (0xCDC6D):
 * {@code [1]} or {@code [0..1]} (one owner, a put moves the value) or {@code [*]} (several owners); the values then
 * navigate to their owner(s).
 */
public class UMLPAssociationQualifierNavigability implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6A";

  public static final String ERROR_MSG_FORMAT = " Association qualified by %s must be " +
      "navigable from the qualified class %s.";

  public static final String OWNER_CARDINALITY_ERROR_CODE = "0xCDC6D";

  public static final String OWNER_CARDINALITY_ERROR_MSG_FORMAT = " The bidirectional association qualified by %s " +
      "needs the cardinality [1], [0..1] or [*] of the qualified class %s: in how many maps a value may be.";

  @Override
  public void check(ASTCDAssociation assoc) {
    if (isQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getCDAssocDir().isDefinitiveNavigableRight(),
          assoc.getCDAssocDir().isDefinitiveNavigableLeft());
    }
    if (isQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getCDAssocDir().isDefinitiveNavigableLeft(),
          assoc.getCDAssocDir().isDefinitiveNavigableRight());
    }
  }

  /**
   * @param fromQualifiedSide whether the association is navigable from the qualified side (to the values)
   * @param toQualifiedSide whether it is navigable to the qualified side (from the values: bidirectional)
   */
  protected void check(ASTCDAssocSide qualifiedSide, boolean fromQualifiedSide, boolean toQualifiedSide) {
    String qualifier = printQualifier(qualifiedSide.getCDQualifier());
    String owner = qualifiedSide.getMCQualifiedType().getMCQualifiedName().getQName();
    if (!fromQualifiedSide) {
      logError(qualifiedSide.getCDQualifier(), qualifier, owner);
    }
    else if (toQualifiedSide && !hasOwnerCardinality(qualifiedSide)) {
      logError(qualifiedSide.getCDQualifier().get_SourcePositionStart(), OWNER_CARDINALITY_ERROR_CODE,
          OWNER_CARDINALITY_ERROR_MSG_FORMAT, qualifier, owner);
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

  /**
   * whether the side is qualified by an attribute ({@code [[attr]]}) or a type ({@code [Type]}), i.e. a GemMap
   */
  public static boolean isQualified(ASTCDAssocSide side) {
    return side.isPresentCDQualifier()
        && (side.getCDQualifier().isPresentByAttributeName() || side.getCDQualifier().isPresentByType());
  }

  /**
   * the qualifier as written in the model, {@code [[attr]]} or {@code [Type]}
   */
  public static String printQualifier(ASTCDQualifier qualifier) {
    return qualifier.isPresentByAttributeName()
        ? "[[" + qualifier.getByAttributeName() + "]]"
        : "[" + qualifier.getByType().printType() + "]";
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
