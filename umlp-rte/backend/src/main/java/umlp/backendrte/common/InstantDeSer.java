/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class InstantDeSer implements umlp.backendrte.command.DeSer<Instant> {
  private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

  @Override
  public String serialize(Instant obj) {
    return '"' + formatter.format(obj) + '"';
  }

  @Override
  public Optional<Instant> deserialize(String str) {
    if(!str.matches("\".*\"")){
      Log.warn("Instant must be deserialized from json string");
      return Optional.empty();
    }

    String content = str.substring(1, str.length() - 1);
    return Optional.of(Instant.from(formatter.parse(content)));
  }
}
