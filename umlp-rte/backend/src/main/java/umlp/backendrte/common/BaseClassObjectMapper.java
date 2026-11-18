/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.common;

// RTE-P

import de.se_rwth.commons.logging.Log;
import umlp.backendrte.command.DeSer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * (De)serializer for base classes from java.lang
 * Since this uses the static delegate pattern, additional base classes can be added via subclassing.
 * (See FlensburgBaseClassObjectMapper)
 */
public class BaseClassObjectMapper {
  protected static BaseClassObjectMapper instance = new BaseClassObjectMapper();

  public static BaseClassObjectMapper getInstance() {
    return instance;
  }

  protected Map<String, DeSer> baseDeSers = new HashMap<>();

  public BaseClassObjectMapper(){
    baseDeSers.put("java.lang.Boolean", new BooleanDeSer());
    baseDeSers.put("java.lang.Short", new ShortDeSer());
    baseDeSers.put("java.lang.Character", new CharacterDeSer());
    baseDeSers.put("java.lang.Long", new LongDeSer());
    baseDeSers.put("java.lang.Integer", new IntegerDeSer());
    baseDeSers.put("java.lang.Byte", new ByteDeSer());
    baseDeSers.put("java.lang.Float", new FloatDeSer());
    baseDeSers.put("java.lang.Double", new DoubleDeSer());
    baseDeSers.put("java.lang.String", new StringDeSer());

    baseDeSers.put("java.time.LocalDate", new LocalDateDeSer());
    baseDeSers.put("java.time.LocalDateTime", new LocalDateTimeDeSer());
    baseDeSers.put("java.time.LocalTime", new LocalTimeDeSer());
    baseDeSers.put("java.time.Instant", new InstantDeSer());
  }

  public static void addDeSers(Map<String, ? extends DeSer> addedDeSers) {
    addedDeSers.entrySet().forEach(deser -> {
      getInstance().baseDeSers.put(deser.getKey(), deser.getValue());
    });
  }

  public Map<String, DeSer> getBaseDeSers() {
    return baseDeSers;
  }

  public static Optional<String> serialize(Object o, String className) {
    return getInstance()._serialize(o, className);
  }

  public Optional<String> _serialize(Object o, String className){
    if(!baseDeSers.containsKey(className)){
      return Optional.empty();
    }

    return Optional.ofNullable(baseDeSers.get(className).serialize(o));
  }

  public static <T> Optional<T> deserialize(String s, String className) {
    return getInstance()._deserialize(s, className);
  }

  public <T> Optional<T> _deserialize(String s, String className){
    if(!baseDeSers.containsKey(className)){
      return Optional.empty();
    }
    // TODO prüfen
    try {
      return (Optional<T>) baseDeSers.get(className).deserialize(s);
    } catch (ClassCastException ex) {
      Log.error("Wrong return type of base class deser: '" + className + "'");
      return Optional.empty();
    }
  }

  public static boolean canDeSer(String className){
    return getInstance()._canDeSer(className);
  }

  public boolean _canDeSer(String className){
    return baseDeSers.containsKey(className);
  }
}
