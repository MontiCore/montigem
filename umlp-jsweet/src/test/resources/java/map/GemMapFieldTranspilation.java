package map;

import umlp.backendrte.common.GemIdentifiable;
import umlp.backendrte.common.GemValueMap;

import java.util.Map;

public class GemMapFieldTranspilation<V extends GemIdentifiable> {

  GemValueMap<String, V> gemMap;

  Map<String, V> map;

  public int sizes() {
    return gemMap.size() + map.size();
  }
}
