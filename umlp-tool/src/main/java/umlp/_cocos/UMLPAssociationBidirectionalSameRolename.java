package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.se_rwth.commons.logging.Log;

import java.util.Objects;

public class UMLPAssociationBidirectionalSameRolename implements CDAssociationASTCDAssociationCoCo {

  public static final String ERROR_CODE = "0xAFa2D";
  public static final String ERROR_MSG_FORMAT = " Roles of bidirectional associations must not be the same";

  @Override
  public void check(ASTCDAssociation node) {

    if (Objects.equals(node.getLeft().getCDRole().getName(), node.getRight().getCDRole().getName())) {
      Log.error(ERROR_CODE + String.format(ERROR_MSG_FORMAT), node.get_SourcePositionStart());
    }

  }
}
