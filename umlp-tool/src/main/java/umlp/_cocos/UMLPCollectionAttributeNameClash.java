/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import umlp.generator.domain.decorator.GenService;

/**
 * A {@code java.util.List} attribute {@code x} gets the getter {@code getXList()}, a {@code java.util.Map} attribute
 * {@code getXMap()} (with simple and domain class elements alike), so no other attribute of the class may be named
 * {@code xList} or {@code xMap}: its getter would have the same name.
 */
public class UMLPCollectionAttributeNameClash implements CDBasisASTCDAttributeCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC70";

  public static final String ERROR_MSG_FORMAT = " The attribute %s and the attribute %s would both have the getter "
      + "%s; rename one of them.";

  @Override
  public void check(ASTCDAttribute attr) {
    if (!attr.isPresentSymbol()) {
      return;
    }
    String suffix;
    if (GenService.isList(attr.getSymbol().getType())) {
      suffix = "List";
    } else if (GenService.isMap(attr.getSymbol().getType())) {
      suffix = "Map";
    } else {
      return;
    }
    String clashing = attr.getName() + suffix;
    boolean clash = attr.getSymbol().getEnclosingScope().getLocalFieldSymbols().stream()
        .anyMatch(f -> f.getName().equals(clashing));
    if (clash) {
      String getter = "get" + Character.toUpperCase(clashing.charAt(0)) + clashing.substring(1) + "()";
      logError(attr, attr.getName(), clashing, getter);
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
