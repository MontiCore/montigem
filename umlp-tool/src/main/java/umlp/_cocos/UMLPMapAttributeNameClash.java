/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._cocos.CDBasisASTCDAttributeCoCo;
import umlp.generator.domain.decorator.GenService;

/**
 * A {@code java.util.Map} attribute {@code x} gets the getter {@code getXMap()} (map attributes and GemMaps alike), so
 * no other attribute of the class may be named {@code xMap}: its getter would have the same name.
 */
public class UMLPMapAttributeNameClash implements CDBasisASTCDAttributeCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC70";

  public static final String ERROR_MSG_FORMAT = " The Map attribute %s and the attribute %s would both have the getter "
      + "%s; rename one of them.";

  @Override
  public void check(ASTCDAttribute attr) {
    if (!attr.isPresentSymbol() || !GenService.isMap(attr.getSymbol().getType())) {
      return;
    }
    String clashing = attr.getName() + "Map";
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
