/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.types.check.SymTypeExpression;
import umlp.generator.domain.decorator.GenService;

import java.util.Optional;

/**
 * A {@code java.util.Map<K, V>} attribute becomes the type qualified association {@code Owner [K] -> (attr) V [1]}
 * (UMLAttrToAssocTrafo), so its value type V is a domain class or interface. Other maps (raw, with primitive, String,
 * enum or collection values) are not supported as attribute types. The key type is checked by
 * {@link UMLPMapAttributeKeyType}.
 */
public class UMLPMapAttributeValueType implements CDBasisASTCDAttributeCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6E";

  public static final String ERROR_MSG_FORMAT = " The Map attribute %s needs a domain class as value type, found %s: "
      + "a Map<K, V> attribute becomes the type qualified association [K] -> V [1]; other maps are not supported as "
      + "attribute types.";

  @Override
  public void check(ASTCDAttribute attr) {
    if (!attr.isPresentSymbol()) {
      return;
    }
    SymTypeExpression type = attr.getSymbol().getType();
    if (!GenService.isMap(type)) {
      return;
    }
    Optional<SymTypeExpression> value = GenService.getMapTypeArgument(type, 1);
    if (value.isEmpty() || !GenService.isAssociationTarget(value.get())) {
      logError(attr, attr.getName(), value.map(SymTypeExpression::print).orElse("no type argument"));
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
