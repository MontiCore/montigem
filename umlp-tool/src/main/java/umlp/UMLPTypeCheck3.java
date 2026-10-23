/* (c) https://github.com/MontiCore/monticore */
package umlp;

import de.monticore.types3.TypeCheck3;
import de.monticore.types3.util.MapBasedTypeCheck3;

/**
 * Access to the active TypeCheck3 delegate (set by the last initialized mill).
 */
public abstract class UMLPTypeCheck3 extends TypeCheck3 {

  /**
   * Drops all cached types. Cached types of an AST keep their symbols, so they point into the old
   * symbol table after the AST got a new one, e.g. roles are then added to the old CD types.
   * Call before completing a symbol table.
   */
  public static void resetTypeCache() {
    if (delegate instanceof MapBasedTypeCheck3 mapBased) {
      mapBased.getType4Ast().reset();
    }
  }

}
