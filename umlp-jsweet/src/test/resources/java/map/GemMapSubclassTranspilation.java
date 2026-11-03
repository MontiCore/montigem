package map;

import umlp.backendrte.common.GemBuildable;
import umlp.backendrte.common.GemIdentifiable;
import umlp.backendrte.common.GemValueMapBuilder;

/**
 * like a generated GemMap of an attribute qualified association: overrides the RTE hooks with synchronous bodies
 */
public abstract class GemMapSubclassTranspilation<V extends GemIdentifiable & GemBuildable> extends GemValueMapBuilder<String, V> {

  public GemMapSubclassTranspilation() {
    super(null);
  }

  @Override
  public String deriveKey(V value) {
    return String.valueOf(value.getGemId());
  }

  @Override
  public boolean isKeyDerived() {
    return true;
  }

  @Override
  protected void onValueKeyed(V value) {
  }

  public int sizeAfterRemove(long gemID) {
    _internal_removeValueID(gemID);
    return size();
  }
}
