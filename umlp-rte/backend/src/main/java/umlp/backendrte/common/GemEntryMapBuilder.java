package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// GemMap that has to be used while objects are created, as in this state it cannot be
// synchronized with other participants yet.
public abstract class GemEntryMapBuilder<K extends GemIdentifiable & GemBuildable, V extends GemIdentifiable & GemBuildable> extends GemEntryMap<K,V> {
  protected transient boolean buildFinished = false;

  // Other builders can be added to a builder; the manager does not know them yet and cannot load them
  // by id, so they are kept here locally. build() clears this storage, it is not used afterwards.
  // The key is the gemId of the key element: builders have their gemId from construction on
  // (see GemIdentifiable#getGemId), so IDOrBuilder needs no equals/hashCode.
  // LinkedHashMap, so that the order is the insertion order, like in a JS Map.
  protected Map<Long, Entry<IDOrBuilder<K>, IDOrBuilder<V>>> builderElemStorage = new LinkedHashMap<>();

  public GemEntryMapBuilder(Map<Long,Long> initialIds) {
    if (initialIds != null) {
      for (Entry<Long,Long> gemIDEntry : initialIds.entrySet()) {
        storeEntry(new IDOrBuilder<>(gemIDEntry.getKey()), new IDOrBuilder<>(gemIDEntry.getValue()));
      }
    }
  }

  @Override
  public Optional<GemEntryMap<K,V>> build(){
    if(buildFinished){
      return Optional.of(this);
    }

    idMap.clear();
    for(Entry<IDOrBuilder<K>, IDOrBuilder<V>> element : builderElemStorage.values()) {
      idMap.put(element.getKey().getIDAndMaybeBuild(), element.getValue().getIDAndMaybeBuild());
    }

    buildFinished = true;

    // from now on, all elements are loaded by the manager
    builderElemStorage.clear();

    return Optional.of(this);
  }

  @Override
  public boolean isBuildFinished() {
    return buildFinished;
  }

  @Override
  public int size() {
    if(buildFinished){
      return super.size();
    } else {
      ensureKeyed();
      return builderElemStorage.size();
    }
  }

  @Override
  public boolean isEmpty() {
    if(buildFinished){
      return super.isEmpty();
    } else {
      return builderElemStorage.isEmpty() && unkeyedValueIDs.isEmpty();
    }
  }

  @Override
  public V put(K key, V value) {
    if(buildFinished || isKeyDerived()) {
      return super.put(key, value);
    } else {
      // Map might contain builders, which a normal GemMap will not
      // Thus, the elements themselves are stored, not only their ids
      ensureKeyed();
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> old = storeEntry(new IDOrBuilder<>(key), new IDOrBuilder<>(value));
      return old == null ? null : resolveValue(old.getValue());
    }
  }

  @Override
  public V get(Object key) {
    if(buildFinished) {
      return super.get(key);
    } else {
      if(!(key instanceof GemIdentifiable)) {
        Log.error("Key is not GemIdentifiable");
        return null;
      }
      ensureKeyed();
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> entry = builderElemStorage.get(((GemIdentifiable) key).getGemId());
      return entry == null ? null : resolveValue(entry.getValue());
    }
  }

  @Override
  public V remove(Object key) {
    if(buildFinished) {
      return super.remove(key);
    } else {
      if(!(key instanceof GemIdentifiable)) {
        Log.error("Key is not GemIdentifiable");
        return null;
      }
      ensureKeyed();
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> old = builderElemStorage.remove(((GemIdentifiable) key).getGemId());
      valueLeft(old == null ? null : old.getValue(), -1);
      return old == null ? null : resolveValue(old.getValue());
    }
  }

  @Override
  public boolean containsKeyID(long gemID) {
    if(buildFinished) {
      return super.containsKeyID(gemID);
    } else {
      ensureKeyed();
      return builderElemStorage.containsKey(gemID);
    }
  }

  @Override
  public boolean containsValueID(long gemID) {
    if(buildFinished) {
      return super.containsValueID(gemID);
    } else {
      return unkeyedValueIDs.contains(gemID)
          || builderElemStorage.values().stream().anyMatch(e -> e.getValue().getGemId() == gemID);
    }
  }

  @Override
  public Long getID(Long gemID) {
    if(buildFinished) {
      return super.getID(gemID);
    } else {
      ensureKeyed();
      return lookupID(gemID);
    }
  }

  @Override
  public void _internal_put(long keyGemID, long valueGemID) {
    if(buildFinished) {
      super._internal_put(keyGemID, valueGemID);
    } else {
      storeEntry(new IDOrBuilder<>(keyGemID), new IDOrBuilder<>(valueGemID));
    }
  }

  @Override
  public void _internal_putValue(V value) {
    if(buildFinished) {
      super._internal_putValue(value);
    } else {
      // keep the element itself, it may be an unbuilt builder; the key is referenced by id
      storeEntry(new IDOrBuilder<>(deriveKeyID(value)), new IDOrBuilder<>(value));
      onValueKeyed(value);
    }
  }

  @Override
  public boolean _internal_removeValueID(long gemID) {
    if(buildFinished) {
      return super._internal_removeValueID(gemID);
    }
    if (unkeyedValueIDs.remove(Long.valueOf(gemID))) {
      return true;
    }
    for (Entry<Long, Entry<IDOrBuilder<K>, IDOrBuilder<V>>> e : builderElemStorage.entrySet()) {
      if (e.getValue().getValue().getGemId() == gemID) {
        valueLeft(builderElemStorage.remove(e.getKey()).getValue(), -1);
        return true;
      }
    }
    return false;
  }

  @Override
  public void _internal_remove(long keyGemID) {
    if(buildFinished) {
      super._internal_remove(keyGemID);
    } else {
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> old = builderElemStorage.remove(keyGemID);
      valueLeft(old == null ? null : old.getValue(), -1);
    }
  }

  @Override
  public void clear() {
    if(buildFinished) {
      super.clear();
    } else {
      List<Entry<IDOrBuilder<K>, IDOrBuilder<V>>> removed = new ArrayList<>(builderElemStorage.values());
      builderElemStorage.clear();
      unkeyedValueIDs.clear();
      removed.forEach(old -> valueLeft(old.getValue(), -1));
    }
  }

  @Override
  public Map<Long,Long> getIDMap() {
    if(buildFinished) {
      return super.getIDMap();
    } else {
      ensureKeyed();
      // insertion order, like the map
      Map<Long,Long> ids = new LinkedHashMap<>();
      for(Entry<IDOrBuilder<K>, IDOrBuilder<V>> e : builderElemStorage.values()) {
        if(e.getKey().isPresentID() && e.getValue().isPresentID()) {
          ids.put(e.getKey().getID(), e.getValue().getID());
        } else {
          //todo decide on error handling
        }
      }
      return ids;
    }
  }

  @Override
  public Collection<Long> getValueIDs() {
    if(buildFinished) {
      return super.getValueIDs();
    } else {
      List<Long> ids = new ArrayList<>();
      builderElemStorage.values().forEach(e -> ids.add(e.getValue().getGemId()));
      ids.addAll(unkeyedValueIDs);
      return ids;
    }
  }

  @Override
  protected Long lookupID(long keyGemID) {
    if(buildFinished) {
      return super.lookupID(keyGemID);
    } else {
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> entry = builderElemStorage.get(keyGemID);
      return entry == null ? null : entry.getValue().getGemId();
    }
  }

  @Override
  protected void moveEntry(long oldKeyGemID, long newKeyGemID) {
    if(buildFinished) {
      super.moveEntry(oldKeyGemID, newKeyGemID);
    } else {
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> entry = builderElemStorage.remove(oldKeyGemID);
      storeEntry(new IDOrBuilder<>(newKeyGemID), entry.getValue());
    }
  }

  /**
   * stores the entry under the gemId of its key
   * @return the entry previously stored for that key, or null
   */
  protected Entry<IDOrBuilder<K>, IDOrBuilder<V>> storeEntry(IDOrBuilder<K> key, IDOrBuilder<V> value) {
    Entry<IDOrBuilder<K>, IDOrBuilder<V>> old = builderElemStorage.put(key.getGemId(), new SimpleImmutableEntry<>(key, value));
    valueLeft(old == null ? null : old.getValue(), value.getGemId());
    return old;
  }

  protected K resolveKey(IDOrBuilder<K> key) {
    return key.isPresentBuilder() ? key.getBuilder() : getKey(key.getID());
  }

  /**
   * calls {@link #onValueRemoved} if old left the map, i.e. was not replaced by itself
   */
  protected void valueLeft(IDOrBuilder<V> old, long newValueGemID) {
    if (old != null && old.getGemId() != newValueGemID) {
      onValueRemoved(old.getGemId(), old.isPresentBuilder() ? old.getBuilder() : null);
    }
  }

  @Override
  public Set<Long> getKeyIDs() {
    if(buildFinished) {
      return super.getKeyIDs();
    } else {
      ensureKeyed();
      return builderElemStorage.keySet();
    }
  }

  /**
   * before build, the values may be unbuilt builders, which cannot be loaded
   */
  @Override
  protected Map<Long, V> loadValuesByID(List<Long> gemIDs) {
    if(buildFinished) {
      return super.loadValuesByID(gemIDs);
    }
    Map<Long, V> result = new HashMap<>();
    List<Long> toLoad = new ArrayList<>();
    for (long gemID : gemIDs) {
      IDOrBuilder<V> stored = builderElemStorage.values().stream().map(Entry::getValue)
          .filter(v -> v.isPresentBuilder() && v.getGemId() == gemID).findFirst().orElse(null);
      if (stored != null) {
        result.put(gemID, stored.getBuilder());
      } else {
        toLoad.add(gemID);
      }
    }
    result.putAll(super.loadValuesByID(toLoad));
    return result;
  }

  /**
   * before build, the keys may be unbuilt builders, which cannot be loaded
   */
  @Override
  protected Map<Long, K> loadKeysByID(List<Long> gemIDs) {
    if(buildFinished) {
      return super.loadKeysByID(gemIDs);
    }
    Map<Long, K> result = new HashMap<>();
    List<Long> toLoad = new ArrayList<>();
    for (long gemID : gemIDs) {
      Entry<IDOrBuilder<K>, IDOrBuilder<V>> stored = builderElemStorage.get(gemID);
      if (stored != null && stored.getKey().isPresentBuilder()) {
        result.put(gemID, stored.getKey().getBuilder());
      } else {
        toLoad.add(gemID);
      }
    }
    result.putAll(super.loadKeysByID(toLoad));
    return result;
  }

  protected V resolveValue(IDOrBuilder<V> value) {
    return value.isPresentBuilder() ? value.getBuilder() : getValue(value.getID());
  }

}
