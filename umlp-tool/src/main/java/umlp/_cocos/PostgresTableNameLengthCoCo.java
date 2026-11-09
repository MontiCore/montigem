package umlp._cocos;

import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDBasisNode;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.cdbasis._cocos.CDBasisASTCDClassCoCo;
import de.monticore.cdinterfaceandenum._ast.ASTCDEnum;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.cdinterfaceandenum._cocos.CDInterfaceAndEnumASTCDEnumCoCo;
import de.monticore.cdinterfaceandenum._cocos.CDInterfaceAndEnumASTCDInterfaceCoCo;
import de.se_rwth.commons.logging.Log;
import umlp.generator.db.sql.SQLSchemaGenerator;

public class PostgresTableNameLengthCoCo implements
    CDBasisASTCDClassCoCo,
    CDInterfaceAndEnumASTCDInterfaceCoCo,
    CDInterfaceAndEnumASTCDEnumCoCo,
    CDAssociationASTCDAssociationCoCo,
    CDBasisASTCDAttributeCoCo
{

  public static final String ERROR_CODE = "0x40252";
  public static final String ERROR_MSG = """
       The %s Name %s is too long, Postgres truncates table names after %s characters.
      The truncated name is: %s
      Set the name by hand or change the existing name.
      """;

  private void tooLongWarn(String name, String truncatedName, ASTCDBasisNode node, String className) {
    if (SQLSchemaGenerator.MAX_IDENTIFIER_BYTES < name.length()) {
      Log.warn(ERROR_CODE + String.format(
          ERROR_MSG,
          className,
          name,
          SQLSchemaGenerator.MAX_IDENTIFIER_BYTES,
          truncatedName
          ),
          node.get_SourcePositionStart());
    }
  }

  @Override
  public void check(ASTCDAssociation node) {
    String name = SQLSchemaGenerator.assocName(node.getName());
    String truncatedName = SQLSchemaGenerator.convertAssocName(node.getName());
    tooLongWarn(name, truncatedName, node, "Association");
  }

  @Override
  public void check(ASTCDClass node) {
    String name = SQLSchemaGenerator.className(node.getName());
    String truncatedName = SQLSchemaGenerator.convertClassName(node.getName(), SQLSchemaGenerator.AttributeType.PLAIN);
    tooLongWarn(name, truncatedName, node, "Class");
  }

  @Override
  public void check(ASTCDEnum node) {
    String name = node.getName();
    tooLongWarn(name, name, node, "Enum");
  }

  @Override
  public void check(ASTCDInterface node) {
    String name = node.getName();
    tooLongWarn(name, name, node, "Interface");
  }

  @Override
  public void check(ASTCDAttribute node) {
    String name = SQLSchemaGenerator.attributeName(node.getName());
    String truncatedName = SQLSchemaGenerator.convertAttributeName(node.getName(), SQLSchemaGenerator.AttributeType.PLAIN);
    tooLongWarn(name, truncatedName, node, "Attribute");
  }
}
