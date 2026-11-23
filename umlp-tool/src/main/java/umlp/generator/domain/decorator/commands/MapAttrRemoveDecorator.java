/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import de.monticore.generating.templateengine.GlobalExtensionManagement;

import java.util.Set;

/**
 * Generates the command {@code <Class>_remove<Attr>} of a map attribute with simple values, the counterpart of
 * {@code _remove<Attr>} of list attributes: removes the entry of a key.
 */
public class MapAttrRemoveDecorator extends MapAttrCommandDecorator {

  public static final String REMOVE_SUFFIX = "_remove";

  public MapAttrRemoveDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex, supportedDeSers);
  }

  @Override
  protected String getSuffix() {
    return REMOVE_SUFFIX;
  }

  /**
   * the key only
   */
  @Override
  protected boolean hasValue() {
    return false;
  }

  /**
   * like the remove of list attributes
   */
  @Override
  protected String getPermission() {
    return "Delete";
  }
}
