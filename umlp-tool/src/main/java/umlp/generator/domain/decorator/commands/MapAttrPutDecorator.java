/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import de.monticore.generating.templateengine.GlobalExtensionManagement;

import java.util.Set;

/**
 * Generates the command {@code <Class>_put<Attr>} of a map attribute with simple values, the counterpart of
 * {@code _add<Attr>} of list attributes: puts a value under a key.
 */
public class MapAttrPutDecorator extends MapAttrCommandDecorator {

  public static final String PUT_SUFFIX = "_put";

  public MapAttrPutDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex, supportedDeSers);
  }

  @Override
  protected String getSuffix() {
    return PUT_SUFFIX;
  }

  /**
   * the key and the value
   */
  @Override
  protected boolean hasValue() {
    return true;
  }

  /**
   * like the add of list attributes
   */
  @Override
  protected String getPermission() {
    return "Create";
  }
}
