/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;

import java.util.Optional;

import static de.monticore.types.check.SymTypePrimitive.box;

/**
 * The key of the GemMap generated for the target side of an attribute qualified association
 * {@code Owner [[attr]] -> Target}: the key of a target object is the value of its attribute attr.
 * <ul>
 *   <li>attr of a primitive type or String: the GemMap is a GemValueMap keyed by the attribute value</li>
 *   <li>attr of a domain class (turned into a to-one association of Target by UMLAttrToAssocTrafo):
 *   the GemMap is a GemEntryMap keyed by the referenced object</li>
 * </ul>
 */
public class QualifierKey {

  protected final String attrName;

  protected final String targetName;

  protected final boolean isEntryKey;

  // the key type: boxed attribute type, or the referenced domain class
  protected final String keyType;

  // the type of the old and new value in Target's notifySet<Attr> observer method
  protected final String notifyType;

  // the getter of target reading the key: getAttr()/isAttr(), or getAttrGem() for references
  protected final String getterName;

  // the class declaring attr (Target or a supertype): its observer is notified when attr changes
  protected final CDTypeSymbol declaringType;
  // reads a stored key from a JsonElement, see getStoredKeyJson
  protected final String storedKeyJson;

  protected QualifierKey(String attrName, String targetName, boolean isEntryKey, String keyType,
                         String notifyType, String getterName, CDTypeSymbol declaringType, String storedKeyJson) {
    this.declaringType = declaringType;
    this.storedKeyJson = storedKeyJson;
    this.getterName = getterName;
    this.attrName = attrName;
    this.targetName = targetName;
    this.isEntryKey = isEntryKey;
    this.keyType = keyType;
    this.notifyType = notifyType;
  }

  /**
   * @param assoc the target side of an attribute qualified association, see {@link GenService#isMap}
   */
  public static QualifierKey of(ASTCDAssocSide assoc) {
    String attrName = GenService.getQualifierAttributeName(assoc).get();
    CDTypeSymbol targetSymbol = (CDTypeSymbol) assoc.getSymbol().getType().getTypeInfo();
    ASTCDType target = targetSymbol.getAstNode();

    Optional<ASTCDAttribute> attr = CDSymbolTables.getAttributesInHierarchy(target).stream()
        .filter(a -> a.getName().equals(attrName)).findFirst();
    if (attr.isPresent()) {
      SymTypeExpression type = attr.get().getSymbol().getType();
      String print = new CdUtilsPrinter().printType(GenService.createType(attr.get()));
      String keyType = type.isPrimitive() ? box(type.printFullName()) : print;
      CDTypeSymbol declaringType = (CDTypeSymbol) attr.get().getSymbol().getEnclosingScope().getSpanningSymbol();
      return new QualifierKey(attrName, targetSymbol.getName(), false, keyType, print,
          GenService.getGetterName(attrName, type), declaringType, GenService.calculateJSon(type));
    }

    // domain typed attributes have become to-one associations of the target
    Optional<ASTCDAssocSide> role = CDSymbolTables.getAssociationsInHierarchy(target).stream()
        .filter(a -> a.getName().equals(attrName)).findFirst();
    if (role.isPresent()) {
      String keyType = role.get().getSymbol().getType().getTypeInfo().getName();
      CDTypeSymbol declaringType = (CDTypeSymbol) role.get().getSymbol().getOtherSide().getType().getTypeInfo();
      return new QualifierKey(attrName, targetSymbol.getName(), true, keyType, "long",
          "get" + StringTransformations.capitalize(attrName) + "Gem", declaringType,
          "getAsJsonNumber().getNumberAsLong()");
    }

    Log.error("0xA7320 The qualifier " + attrName + " of the association to " + targetSymbol.getName()
        + " is neither an attribute nor a to-one association of " + targetSymbol.getName(), assoc.get_SourcePositionStart());
    return new QualifierKey(attrName, targetSymbol.getName(), false, "Object", "Object", "get" + attrName, targetSymbol, "");
  }

  public String getAttrName() {
    return attrName;
  }

  public String getTargetName() {
    return targetName;
  }

  /**
   * @return true: GemEntryMap keyed by a domain object; false: GemValueMap keyed by a value
   */
  public boolean isEntryKey() {
    return isEntryKey;
  }

  public CDTypeSymbol getDeclaringType() {
    return declaringType;
  }

  /**
   * @return the key type: the boxed attribute type, or the referenced domain class (GemEntryMap)
   */
  public String getKeyType() {
    return keyType;
  }

  public String getNotifyType() {
    return notifyType;
  }

  /**
   * @return the type a key is stored and synchronized as: the key itself (boxed) for GemValueMaps,
   * the gemId of the key object for GemEntryMaps
   */
  public String getStoredKeyType() {
    return isEntryKey ? "Long" : keyType;
  }

  /**
   * @return the call reading a stored key (see {@link #getStoredKeyType}) from a JsonElement,
   * e.g. getAsJsonString().getValue(); empty if the key type has no JSON representation
   */
  public String getStoredKeyJson() {
    return storedKeyJson;
  }

  /**
   * @return GemValueMap or GemEntryMap
   */
  public String getMapType() {
    return isEntryKey ? "GemEntryMap" : "GemValueMap";
  }

  /**
   * @return the expression deriving the key from a target object in variable valueVar:
   * the attribute value, or the gemId of the referenced object
   */
  public String getKeyExpression(String valueVar) {
    return valueVar + "." + getterName + "()" + (isEntryKey ? ".getGemId()" : "");
  }
}
