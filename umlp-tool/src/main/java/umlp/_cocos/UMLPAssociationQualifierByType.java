/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.ast.ASTCNode;
import de.monticore.types3.TypeCheck3;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types3.SymTypeRelations;

/**
 * The type of a type qualified association ({@code Owner [Type] -> Target}) is the key type of its GemMap: the
 * integral types byte, short, int and long (not float, double or char, see
 * {@link UMLPAssociationQualifierByAttributeName#isUnsupportedKey}) and String are keys of a GemValueMap, domain
 * classes keys of a GemEntryMap.
 */
public class UMLPAssociationQualifierByType implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC63";
  public static final String ERROR_MSG_FORMAT = " Qualified association with type %s is invalid, " +
      "qualified associations only support byte, short, int, long (not float, double or char: Java and the " +
      "browser would disagree on their keys), String or domain class type qualifiers.";
  @Override
  public void check(ASTCDAssociation a) {
    if (a.getLeft().isPresentCDQualifier() && a.getLeft().getCDQualifier().isPresentByType()) {
      checkKeyType(a.getLeft().getCDQualifier().getByType());
    } else if (a.getRight().isPresentCDQualifier() && a.getRight().getCDQualifier().isPresentByType()) {
      checkKeyType(a.getRight().getCDQualifier().getByType());
    }
  }
  private void checkKeyType(ASTMCType type) {
    SymTypeExpression ste = TypeCheck3.symTypeFromAST(type);
    if (!isTypeKey(ste)) {
      logError((ASTCNode) type, ste.getTypeInfo().getName());
    }
  }

  /**
   * whether the type can be the key type of a type qualified association (also of a {@code Map} attribute, see
   * {@link UMLPMapAttributeKeyType}), see the class comment
   */
  public static boolean isTypeKey(SymTypeExpression type) {
    boolean integral = SymTypeRelations.isNumericType(type)
        && !UMLPAssociationQualifierByAttributeName.isUnsupportedKey(type);
    return integral || SymTypeRelations.isString(type) || UMLPAssociationQualifierByAttributeName.isDomainKey(type);
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
