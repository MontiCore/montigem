/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;

/**
 * Attribute qualified associations are generated as GemMaps of the qualified side,
 * which is only supported if the association is navigable from the qualified side only.
 */
public class UMLPAssociationQualifierUnidirectional implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6A";

  public static final String ERROR_MSG_FORMAT = " Association qualified by attribute %s must be " +
      "navigable from the qualified class %s only; bidirectional qualified associations are not supported yet.";

  @Override
  public void check(ASTCDAssociation assoc) {
    if (isAttributeQualified(assoc.getLeft())) {
      check(assoc, assoc.getLeft(), assoc.getCDAssocDir().isDefinitiveNavigableRight()
          && !assoc.getCDAssocDir().isDefinitiveNavigableLeft());
    }
    if (isAttributeQualified(assoc.getRight())) {
      check(assoc, assoc.getRight(), assoc.getCDAssocDir().isDefinitiveNavigableLeft()
          && !assoc.getCDAssocDir().isDefinitiveNavigableRight());
    }
  }

  protected void check(ASTCDAssociation assoc, ASTCDAssocSide qualifiedSide, boolean onlyFromQualifiedSide) {
    if (!onlyFromQualifiedSide) {
      logError(qualifiedSide.getCDQualifier(), qualifiedSide.getCDQualifier().getByAttributeName(),
          qualifiedSide.getMCQualifiedType().getMCQualifiedName().getQName());
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
