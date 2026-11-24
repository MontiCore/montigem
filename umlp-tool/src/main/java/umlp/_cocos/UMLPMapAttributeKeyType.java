/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.types.check.SymTypeExpression;
import umlp.generator.domain.decorator.GenService;

import java.util.Optional;

/**
 * The key type K of a {@code java.util.Map<K, V>} attribute is the type qualifier of the association it becomes
 * ({@code Owner [K] -> (attr) V [1]}, UMLAttrToAssocTrafo), so it follows the rules of type qualifiers
 * ({@link UMLPAssociationQualifierByType#isTypeKey}): byte, short, int, long, String or a domain class. The check runs
 * on the attribute, before the transformation, so the error points to the attribute. The value type is checked by
 * {@link UMLPMapAttributeValueType}.
 */
public class UMLPMapAttributeKeyType implements CDBasisASTCDAttributeCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6F";

  public static final String ERROR_MSG_FORMAT = " The key type %s of the Map attribute %s is invalid: a Map attribute "
      + "becomes a type qualified association, whose keys are byte, short, int, long (not float, double or char: Java "
      + "and the browser would disagree on their keys), String or a domain class.";

  @Override
  public void check(ASTCDAttribute attr) {
    if (!attr.isPresentSymbol()) {
      return;
    }
    SymTypeExpression type = attr.getSymbol().getType();
    if (!GenService.isMap(type)) {
      return;
    }
    Optional<SymTypeExpression> key = GenService.getMapTypeArgument(type, 0);
    if (key.isPresent() && !UMLPAssociationQualifierByType.isTypeKey(key.get())) {
      logError(attr, key.get().print(), attr.getName());
    }
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
