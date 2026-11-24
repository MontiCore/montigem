package umlp.backendrte.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// GemMap that has to be used while objects are created, as in this state it cannot be
// synchronized with other participants yet.
public abstract class GemValueMapBuilder<K, V extends GemIdentifiable & GemBuildable> extends GemValueMap<K,V> {
  protected transient boolean buildFinished = false;

  // Other builders can be added to a builder; the manager does not know them yet and cannot load them
  // by id, so they are kept here locally. build() clears this storage, it is not used afterwards.
  // LinkedHashMap, so that the order is the insertion order, like in a JS Map.
  protected Map<K, IDOrBuilder<V>> builderElemStorage = new LinkedHashMap<>();

  // The values put as objects (built values or builders) before build(): the managers observe the values of the
  // owners they know, an owner that is a builder is not known yet, so build() derives their keys again
  // (values put by id, e.g. as received from another participant, keep their keys)
  protected transient Map<Long, V> valuesPutBeforeBuild = new LinkedHashMap<>();

  /**
   * an empty map, e.g. the map of a new owner
   */
  public GemValueMapBuilder() {
  }

  /**
   * @param initialIds the gemIds of the values per key, e.g. as loaded from the database
   */
  public GemValueMapBuilder(Map<K,Long> initialIds) {
    if (initialIds != null) {
      for (Entry<K,Long> idMapEntry : initialIds.entrySet()) {
        this.builderElemStorage.put(idMapEntry.getKey(), new IDOrBuilder<>(idMapEntry.getValue()));
      }
    }
  }

  @Override
  public Optional<GemValueMap<K,V>> build(){
    if(buildFinished){
      return Optional.of(this);
    }

    rederiveKeys();
    idMap.clear();
    for(Entry<K, IDOrBuilder<V>> element : builderElemStorage.entrySet()) {
      idMap.put(element.getKey(), element.getValue().getIDAndMaybeBuild());
    }

    buildFinished = true;

    // from now on, all elements are loaded by the manager
    builderElemStorage.clear();

    return Optional.of(this);
  }

  /**
   * moves the values put as objects before build() to the keys they derive now, e.g. after their qualifying
   * attribute changed (a key used by another value is reported and keeps its value, see _internal_rekey)
   */
  protected void rederiveKeys() {
    if (isKeyDerived()) {
      for (V value : valuesPutBeforeBuild.values()) {
        for (Entry<K, IDOrBuilder<V>> e : new ArrayList<>(builderElemStorage.entrySet())) {
          if (e.getValue().getGemId() == value.getGemId()) {
            K derived = deriveKey(value);
            if (!Objects.equals(e.getKey(), derived)) {
              _internal_rekey(e.getKey(), derived, value.getGemId());
            }
            break;
          }
        }
      }
    }
    valuesPutBeforeBuild.clear();
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
  public V get(Object key) {
    if(buildFinished) {
      return super.get(key);
    } else {
      ensureKeyed();
      IDOrBuilder<V> value = builderElemStorage.get(key);
      return value == null ? null : resolveValue(value);
    }
  }

  @Override
  public V remove(Object key) {
    if(buildFinished) {
      return super.remove(key);
    } else {
      ensureKeyed();
      IDOrBuilder<V> old = builderElemStorage.remove(key);
      valueLeft(old, -1);
      return old == null ? null : resolveValue(old);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    if(buildFinished) {
      return super.containsKey(key);
    } else {
      ensureKeyed();
      return builderElemStorage.containsKey(key);
    }
  }

  @Override
  public boolean containsValueID(long gemID) {
    if(buildFinished) {
      return super.containsValueID(gemID);
    } else {
      return unkeyedValueIDs.contains(gemID)
          || builderElemStorage.values().stream().anyMatch(v -> v.getGemId() == gemID);
    }
  }

  @Override
  public void _internal_put(K key, long valueGemID) {
    if(buildFinished) {
      super._internal_put(key, valueGemID);
    } else {
      valueLeft(builderElemStorage.put(key, new IDOrBuilder<>(valueGemID)), valueGemID);
    }
  }

  @Override
  public void _internal_putValue(V value) {
    if(buildFinished) {
      super._internal_putValue(value);
    } else {
      K key = deriveKey(value);
      if (refusesKey(key, value.getGemId())) {
        return;
      }
      // stored under one key only, see _internal_putValueID
      builderElemStorage.entrySet().removeIf(e -> e.getValue().getGemId() == value.getGemId()
          && !Objects.equals(e.getKey(), key));
      // keep the element itself, it may be an unbuilt builder
      valueLeft(builderElemStorage.put(key, new IDOrBuilder<>(value)), value.getGemId());
      valuesPutBeforeBuild.put(value.getGemId(), value);
      onValueKeyed(value);
    }
  }

  @Override
  public void _internal_putKeyValue(K key, V value) {
    if(buildFinished) {
      super._internal_putKeyValue(key, value);
    } else {
      if (refusesKey(key, value.getGemId())) {
        return;
      }
      ensureKeyed();
      // stored under one key only, see _internal_putValueID
      builderElemStorage.entrySet().removeIf(e -> e.getValue().getGemId() == value.getGemId()
          && !Objects.equals(e.getKey(), key));
      // Map might contain builders, which a normal GemMap will not
      // Thus, the elements themselves are stored, not only their ids
      valueLeft(builderElemStorage.put(key, new IDOrBuilder<>(value)), value.getGemId());
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
    for (Entry<K, IDOrBuilder<V>> e : builderElemStorage.entrySet()) {
      if (e.getValue().getGemId() == gemID) {
        valueLeft(builderElemStorage.remove(e.getKey()), -1);
        return true;
      }
    }
    return false;
  }

  @Override
  public void _internal_remove(K key) {
    if(buildFinished) {
      super._internal_remove(key);
    } else {
      valueLeft(builderElemStorage.remove(key), -1);
    }
  }

  @Override
  public void clear() {
    if(buildFinished) {
      super.clear();
    } else {
      List<IDOrBuilder<V>> removed = new ArrayList<>(builderElemStorage.values());
      builderElemStorage.clear();
      unkeyedValueIDs.clear();
      removed.forEach(old -> valueLeft(old, -1));
    }
  }

  @Override
  public Map<K,Long> getIDMap() {
    if(buildFinished) {
      return super.getIDMap();
    } else {
      ensureKeyed();
      // insertion order, like the map
      Map<K,Long> ids = new LinkedHashMap<>();
      for(Entry<K, IDOrBuilder<V>> e : builderElemStorage.entrySet()) {
        if(e.getValue().isPresentID()) {
          ids.put(e.getKey(), e.getValue().getID());
        } else {
          //todo decide on error handling
        }
      }
      return ids;
    }
  }

  @Override
  public Long getID(Object key) {
    if(buildFinished) {
      return super.getID(key);
    } else {
      ensureKeyed();
      return lookupID(key);
    }
  }

  @Override
  public Collection<Long> getValueIDs() {
    if(buildFinished) {
      return super.getValueIDs();
    } else {
      List<Long> ids = new ArrayList<>();
      builderElemStorage.values().forEach(v -> ids.add(v.getGemId()));
      ids.addAll(unkeyedValueIDs);
      return ids;
    }
  }

  @Override
  protected Long lookupID(Object key) {
    if(buildFinished) {
      return super.lookupID(key);
    } else {
      IDOrBuilder<V> value = builderElemStorage.get(key);
      return value == null ? null : value.getGemId();
    }
  }

  @Override
  protected void moveEntry(K oldKey, K newKey) {
    if(buildFinished) {
      super.moveEntry(oldKey, newKey);
    } else {
      builderElemStorage.put(newKey, builderElemStorage.remove(oldKey));
    }
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
  public Set<K> getKeys() {
    if(buildFinished) {
      return super.getKeys();
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
      IDOrBuilder<V> stored = builderElemStorage.values().stream()
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

  protected V resolveValue(IDOrBuilder<V> value) {
    return value.isPresentBuilder() ? value.getBuilder() : getValue(value.getID());
  }

}
