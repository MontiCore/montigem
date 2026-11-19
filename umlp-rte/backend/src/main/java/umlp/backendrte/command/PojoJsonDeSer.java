/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.command;

import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.json.JsonObject;
import umlp.backendrte.common.PojoGraphContext;

/** Generated POJO serializers allocate objects before reading their links. */
public interface PojoJsonDeSer<T> extends JsonDeSer<T> {
  T createObject();
  void readJson(T object, JsonObject json, PojoGraphContext context);
  void writeJson(T object, JsonPrinter printer, PojoGraphContext context);
  boolean finishObject(T object);

  @Override
  default boolean isPojoDeSer() {
    return true;
  }
}
