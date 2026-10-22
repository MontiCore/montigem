package umlp.backendrte.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// GemList, welche bei der Erstellung von Objekten benutzt werden muss, da sie in diesem Zustand
// noch nicht mit anderen Teilnehmen synchronisiert werden kann.
public abstract class GemValueMapBuilder<K, V extends GemIdentifiable & GemBuildable> extends GemValueMap<K,V> {
  protected transient boolean buildFinished = false;

  // Da einem in einem Builder andere Builder hinzugefügt werden können und der FlensburgManager diese
  // nicht kennt, also auch nicht per id laden kann, muss hier eine lokale Verwaltung stattfinden.
  // Nach build() wird diese geleert und nicht weiter verwendet.
  // LinkedHashMap, damit die Reihenfolge wie bei einer JS-Map der Einfügereihenfolge entspricht.
  protected Map<K, IDOrBuilder<V>> builderElemStorage = new LinkedHashMap<>();

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

    idMap.clear();
    for(Entry<K, IDOrBuilder<V>> element : builderElemStorage.entrySet()) {
      idMap.put(element.getKey(), element.getValue().getIDAndMaybeBuild());
    }

    buildFinished = true;

    // Von hier an werden alle Element vom FlensburgManager geladen
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
      IDOrBuilder<V> old = builderElemStorage.put(key, new IDOrBuilder<>(value));
      valueLeft(old, value.getGemId());
      return old == null ? null : resolveValue(old);
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
  public Set<Entry<K,V>> entrySet() {
    if(buildFinished) {
      return super.entrySet();
    } else {
      ensureKeyed();
      Set<Entry<K, V>> result = new HashSet<>();
      for (Entry<K, IDOrBuilder<V>> e : builderElemStorage.entrySet()) {
        result.add(new SimpleEntry<>(e.getKey(), resolveValue(e.getValue())));
      }
      return result;
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
      // keep the element itself, it may be an unbuilt builder
      valueLeft(builderElemStorage.put(deriveKey(value), new IDOrBuilder<>(value)), value.getGemId());
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
      Map<K,Long> ids = new HashMap<>();
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

  protected V resolveValue(IDOrBuilder<V> value) {
    return value.isPresentBuilder() ? value.getBuilder() : getValue(value.getID());
  }

}
