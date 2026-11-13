package umlp._cocos;

import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDMember;
import de.monticore.cdbasis._cocos.CDBasisASTCDClassCoCo;
import de.monticore.symbols.oosymbols.types3.OOSymbolsSymTypeRelations;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mccollectiontypes.types3.MCCollectionSymTypeRelations;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.logging.Log;
import umlp.generator.util.TagLookUp;

public class UMLPCheckParameterOfCommand implements CDBasisASTCDClassCoCo {

  public static final String ERROR_CODE = "0x40251";

  public static final String ERROR_MSG_ENUM = " The singleton class %s must not have any enums as parameters of its method %s";
  public static final String ERROR_MSG_ARRAY = " The singleton class %s must not have any array as parameters of its method %s, use List";
  public static final String ERROR_MSG_MAP = " The singleton class %s must not have any maps as parameters of its method %s";

  public static final String ERROR_SUFFIX = "\n Support for this type of parameter is not yet implemented";


  @Override
  public void check(ASTCDClass clazz) {
    if (TagLookUp.isSingleton(clazz)) {

      for (ASTCDMember member : clazz.getCDMemberList()) {
        if (member.isMethod()) {
          ASTCDMethod method = (ASTCDMethod) member;

          for (ASTCDParameter parameter : method.getCDParameterList()) {
            SymTypeExpression expression = TypeCheck3.symTypeFromAST(parameter.getMCType());
            if (OOSymbolsSymTypeRelations.isEnum(expression)) {
              Log.error(ERROR_CODE + String.format(ERROR_MSG_ENUM, clazz.getName(), method.getName()) + ERROR_SUFFIX, parameter.get_SourcePositionStart());
            } else if (expression.isArrayType()) {
              Log.error(ERROR_CODE + String.format(ERROR_MSG_ARRAY, clazz.getName(), method.getName()) + ERROR_SUFFIX, parameter.get_SourcePositionStart());
            } else if (MCCollectionSymTypeRelations.isMap(expression)) {
              Log.error(ERROR_CODE + String.format(ERROR_MSG_MAP, clazz.getName(), method.getName()) + ERROR_SUFFIX, parameter.get_SourcePositionStart());
            }
          }
        }
      }
    }
  }
}
