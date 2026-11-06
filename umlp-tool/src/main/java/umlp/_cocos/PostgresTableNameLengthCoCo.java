package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.cdbasis._cocos.CDBasisASTCDClassCoCo;
import de.monticore.cdbasis._cocos.CDBasisASTCDElementCoCo;
import de.se_rwth.commons.logging.Log;

public class PostgresTableNameLengthCoCo implements CDBasisASTCDElementCoCo {
  public static final String ERROR_CODE = "0x40252";
  public static final String ERROR_MSG = """
       The %s Name %s is too long, Postgres truncates table names after 64 characters.
      Set the name by hand or change the existing name.
      """;

  private String getName(ASTCDElement element) {
    try {
      return (String) element.getClass().getMethod("getName").invoke(element);
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public void check(ASTCDElement element) {
    String name = getName(element);
    System.out.println(element.toString() + " " + name);

    if (name.length() > 64) {
      Log.warn(ERROR_CODE + String.format(ERROR_MSG, element.getClass().getSimpleName().replaceFirst("ASTCD", ""), name), element.get_SourcePositionStart());
    }
  }
}
