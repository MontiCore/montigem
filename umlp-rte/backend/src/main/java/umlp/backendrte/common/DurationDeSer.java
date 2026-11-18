/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;

import java.time.Duration;
import java.util.Optional;

public class DurationDeSer implements umlp.backendrte.command.DeSer<Duration> {
  @Override
  public String serialize(Duration obj) {
    return '"' + obj.toString() + '"';
  }

  @Override
  public Optional<Duration> deserialize(String str) {
    if(!str.matches("\".*\"")){
      Log.warn("Duration must be deserialized from json string");
      return Optional.empty();
    }

    String content = str.substring(1, str.length() - 1);
    return Optional.of(Duration.parse(content));
  }
}
