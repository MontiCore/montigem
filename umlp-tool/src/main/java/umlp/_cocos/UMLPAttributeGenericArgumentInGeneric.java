package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.logging.Log;

public class UMLPAttributeGenericArgumentInGeneric implements CDBasisASTCDAttributeCoCo {

  public static final String ERROR_CODE = "0x70249";
  public static final String ERROR_MSG_FORMAT = """
       Invalid argument: %s %s
      Multi dimensional Generic types are not supported
      """;

  @Override
  public void check(ASTCDAttribute node) {
    SymTypeExpression symTypeExpression = TypeCheck3.symTypeFromAST(node.getMCType());

    if (symTypeExpression.isGenericType() && symTypeExpression.asGenericType().getArgumentList().stream().anyMatch(SymTypeExpression::isGenericType)) {
      Log.error(ERROR_CODE + String.format(ERROR_MSG_FORMAT, node.getName(), symTypeExpression.print()), node.get_SourcePositionStart());
    }
  }
}
