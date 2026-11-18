/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import de.monticore.types.check.SymTypeExpression;
import umlp.generator.domain.decorator.GenService;

import java.util.Optional;

/**
 * The value type V of a {@code java.util.Map<K, V>} attribute is a domain class or interface (the attribute becomes the
 * type qualified association {@code Owner [K] -> (attr) V [1]}, UMLAttrToAssocTrafo) or a simple type like the elements
 * of list attributes: a primitive or boxed type, String or an enum (a map attribute, persisted and synchronized per
 * entry). Raw maps and maps of collections, Optionals or maps are not supported. The key type is checked by
 * {@link UMLPMapAttributeKeyType}.
 */
public class UMLPMapAttributeValueType implements CDBasisASTCDAttributeCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC6E";

  public static final String ERROR_MSG_FORMAT = " The Map attribute %s needs a domain class or a simple type (a "
      + "primitive or boxed type, String, an enum, LocalDate, LocalDateTime, LocalTime, Instant or Duration) as value "
      + "type, found %s.";

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
    if (value.isEmpty()
        || !(GenService.isAssociationTarget(value.get()) || GenService.isSimpleMapValue(value.get()))) {
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
