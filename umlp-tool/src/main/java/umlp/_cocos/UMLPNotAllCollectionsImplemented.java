/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.logging.Log;
import umlp.generator.domain.decorator.GenService;

public class UMLPNotAllCollectionsImplemented implements CDBasisASTCDAttributeCoCo {

  public static final String ERROR_CODE = "0x70876";

  public static final String ERROR_MSG_FORMAT = " Currently only the generic classes List, Optional and Map are supported, but attribute %s uses %s";

  @Override
  public void check(ASTCDAttribute attr) {
    SymTypeExpression type = attr.getSymbol().getType();

    if (type.isGenericType()) {
      // Map attributes become type qualified associations, checked by UMLPMapAttributeValueType and
      // UMLPMapAttributeKeyType
      if(!(GenService.isList(type) || GenService.isOptional(type) || GenService.isMap(type))) {
        Log.error(
            String.format(ERROR_CODE + ERROR_MSG_FORMAT, attr.getName(), type.print()),
            attr.get_SourcePositionStart()
        );
      }
    }
  }
}
