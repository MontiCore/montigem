/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.common;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.json.JsonObject;
import de.monticore.symboltable.serialization.json.JsonElement;
import de.se_rwth.commons.logging.Log;
import org.apache.commons.lang3.StringEscapeUtils;
import umlp.backendrte.command.DeSer;
import umlp.backendrte.command.PojoJsonDeSer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * State for a single POJO graph traversal. Definitions carry $id and repeated
 * references carry $ref. Objects are registered before their fields are read,
 * and builders are finished only after the complete graph has been populated.
 * No traversal state is shared between threads or concurrent browser requests.
 */
public class PojoGraphContext {
  private final List<Object> written = new ArrayList<>();
  private final Map<String, Object> read = new HashMap<>();
  private final Map<String, String> readTypes = new HashMap<>();
  private final List<Runnable> finishers = new ArrayList<>();
  private int writeDepth;
  private int readDepth;

  @SuppressWarnings("unchecked")
  public Optional<String> serialize(Object object, String className) {
    DeSer<?> deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (deser == null) {
      Log.error("No serializer registered for class: '" + className + "'");
      return Optional.empty();
    }
    if (deser.isPojoDeSer()) {
      return Optional.of(serializeObject(object, (PojoJsonDeSer<Object>) deser));
    }
    return BaseClassObjectMapper.serialize(object, className);
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> deserialize(String json, String className) {
    DeSer<?> deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (deser == null) {
      Log.error("No deserializer registered for class: '" + className + "'");
      return Optional.empty();
    }
    if (deser.isPojoDeSer()) {
      return (Optional<T>) deserializeObject(JsonParser.parse(json).getAsJsonObject(), (PojoJsonDeSer<?>) deser);
    }
    return BaseClassObjectMapper.deserialize(json, className);
  }

  @SuppressWarnings("unchecked")
  public Object deserializeValue(JsonElement value, String className) {
    DeSer<?> deser = BaseClassObjectMapper.getInstance().getBaseDeSers().get(className);
    if (deser == null) {
      String message = "No deserializer registered for class: '" + className + "'";
      Log.error(message);
      throw new IllegalArgumentException(message);
    }
    if (deser.isPojoDeSer()) {
      return deserializeObject(value.getAsJsonObject(), (PojoJsonDeSer<?>) deser).orElseThrow();
    }
    // Parsed JsonStrings print their unquoted value; scalar DeSers expect JSON.
    String json = value.isJsonString()
        ? '"' + StringEscapeUtils.escapeJson(value.getAsJsonString().getValue()) + '"'
        : value.print(new de.monticore.prettyprint.IndentPrinter());
    return BaseClassObjectMapper.deserialize(json, className).orElseThrow();
  }

  public String serializeObject(Object object, PojoJsonDeSer<?> deser) {
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    writeObject(object, printer, deser);
    printer.endObject();
    return printer.getContent();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void writeObject(Object object, JsonPrinter printer, PojoJsonDeSer deser) {
    if (object == null) {
      throw new IllegalArgumentException("Cannot serialize a null POJO");
    }
    writeDepth++;
    try {
      int existing = -1;
      // Reference equality also works for POJOs with value-based equals().
      for (int i = 0; i < written.size(); i++) {
        if (written.get(i) == object) {
          existing = i;
          break;
        }
      }
      if (existing >= 0) {
        printer.member("$ref", Integer.toString(existing));
      } else {
        String id = Integer.toString(written.size());
        written.add(object);
        printer.member("$id", id);
        deser.writeJson(object, printer, this);
      }
    } finally {
      writeDepth--;
      if (writeDepth == 0) {
        written.clear();
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public Optional<?> deserializeObject(JsonObject json, PojoJsonDeSer deser) {
    readDepth++;
    try {
      if (json.hasMember("$ref")) {
        String id = json.getStringMember("$ref");
        if (json.hasMember("$id") || !read.containsKey(id)
            || !deser.getTypeIdentifier().equals(readTypes.get(id))) {
          throw new IllegalArgumentException("Invalid POJO reference: " + id);
        }
        return Optional.of(read.get(id));
      }
      Object object = deser.createObject();
      if (json.hasMember("$id")) {
        String id = json.getStringMember("$id");
        if (read.containsKey(id)) {
          throw new IllegalArgumentException("Duplicate POJO identity: " + id);
        }
        read.put(id, object);
        readTypes.put(id, deser.getTypeIdentifier());
      }
      finishers.add(() -> {
        if (!deser.finishObject(object)) {
          throw new IllegalArgumentException("Invalid POJO: " + deser.getTypeIdentifier());
        }
      });
      deser.readJson(object, json, this);
      if (readDepth == 1) {
        for (Runnable finisher : finishers) {
          finisher.run();
        }
      }
      return Optional.of(object);
    } finally {
      readDepth--;
      if (readDepth == 0) {
        read.clear();
        readTypes.clear();
        finishers.clear();
      }
    }
  }
}
