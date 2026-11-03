package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.se_rwth.commons.logging.Log;

public class UMLPAssociationNameLength implements CDAssociationASTCDAssociationCoCo {
  public static final String ERROR_CODE = "0x40252";
  public static final String ERROR_MSG = """
      The Association Name %s is too long, Postgres truncates table names after 64 characters.
      Set the name by hand or change the existing name.
      Nomenclature: association <AssociationName> [cardinality1] ClassName1 (roleName1) <-> (roleName2) ClassName2 [cardinality2];
      """;

  @Override
  public void check(ASTCDAssociation asso) {
    if (asso.getName().length() > 64) {
      Log.warn(ERROR_CODE + String.format(ERROR_MSG, asso.getName()), asso.get_SourcePositionStart());
    }
  }
}
