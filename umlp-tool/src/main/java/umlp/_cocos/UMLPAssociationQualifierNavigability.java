/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDQualifier;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * Qualified associations, by an attribute ({@code [[attr]]}) or a type ({@code [Type]}), are generated as GemMaps of
 * the qualified side (the owner), so they must be navigable from the qualified side. The cardinality the owner needs
 * in a bidirectional one is checked by {@link UMLPAssociationQualifierOwnerCardinality}.
 */
public class UMLPAssociationQualifierNavigability implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6A";

  public static final String ERROR_MSG_FORMAT = " Association qualified by %s must be " +
      "navigable from the qualified class %s.";

  @Override
  public void check(ASTCDAssociation assoc) {
    if (isQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getCDAssocDir().isDefinitiveNavigableRight());
    }
    if (isQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getCDAssocDir().isDefinitiveNavigableLeft());
    }
  }

  /**
   * @param fromQualifiedSide whether the association is navigable from the qualified side (to the values)
   */
  protected void check(ASTCDAssocSide qualifiedSide, boolean fromQualifiedSide) {
    if (!fromQualifiedSide) {
      logError(qualifiedSide.getCDQualifier(), printQualifier(qualifiedSide.getCDQualifier()),
          qualifiedSide.getMCQualifiedType().getMCQualifiedName().getQName());
    }
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
