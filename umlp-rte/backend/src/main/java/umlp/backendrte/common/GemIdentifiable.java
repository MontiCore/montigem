// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import umlp.backendrte.command.JsonDeSer;

public interface GemIdentifiable {

  /**
   * The gemId is valid from construction on, also for builders before build().
   * build() only registers the object with its manager; it never assigns the id.
   */
  long getGemId();

  /**
   * The DeSer for the concrete runtime type of this object. Generated domain
   * classes override this; serialization uses it to keep the concrete type
   * across the wire (monticore#5062). Handwritten implementors may keep the
   * default; callers must handle null by falling back to the declared type.
   */
  default JsonDeSer<? extends GemIdentifiable> getDeSer() {
    return null;
  }

}
