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

// GemMap, welche bei der Erstellung von Objekten benutzt werden muss, da sie in diesem Zustand
// noch nicht mit anderen Teilnehmen synchronisiert werden kann.
public abstract class GemEntryMapBuilder<K extends GemIdentifiable & GemBuildable, V extends GemIdentifiable & GemBuildable> extends GemEntryMap<K,V> {
  protected transient boolean buildFinished = false;

  // Da einem in einem Builder andere Builder hinzugefügt werden können und der FlensburgManager diese
  // nicht kennt, also auch nicht per id laden kann, muss hier eine lokale Verwaltung stattfinden.
  // Nach build() wird diese geleert und nicht weiter verwendet.
  // Schlüssel ist die gemId des Key-Elements: Builder haben ihre gemId bereits ab der Konstruktion
  // (siehe GemIdentifiable#getGemId), daher wird kein equals/hashCode auf IDOrBuilder benötigt.
  // LinkedHashMap, damit die Reihenfolge wie bei einer JS-Map der Einfügereihenfolge entspricht.
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
    if(buildFinished) {
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
      return old == null ? null : resolveValue(old.getValue());
    }
  }

  @Override
  public Set<Entry<K,V>> entrySet() {
    if(buildFinished) {
      return super.entrySet();
    } else {
      ensureKeyed();
      Set<Entry<K, V>> result = new HashSet<>();
      for (Entry<IDOrBuilder<K>, IDOrBuilder<V>> e : builderElemStorage.values()) {
        result.add(new SimpleEntry<>(resolveKey(e.getKey()), resolveValue(e.getValue())));
      }
      return result;
    }
  }

  @Override
  public boolean containsKey(Long gemID) {
    if(buildFinished) {
      return super.containsKey(gemID);
    } else {
      ensureKeyed();
      return builderElemStorage.containsKey(gemID);
    }
  }

  @Override
  public boolean containsValue(Long gemID) {
    if(buildFinished) {
      return super.containsValue(gemID);
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
        builderElemStorage.remove(e.getKey());
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
      builderElemStorage.remove(keyGemID);
    }
  }

  @Override
  public void clear() {
    if(buildFinished) {
      super.clear();
    } else {
      builderElemStorage.clear();
      unkeyedValueIDs.clear();
    }
  }

  @Override
  public Map<Long,Long> getIDMap() {
    if(buildFinished) {
      return super.getIDMap();
    } else {
      ensureKeyed();
      Map<Long,Long> ids = new HashMap<>();
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
    return builderElemStorage.put(key.getGemId(), new SimpleImmutableEntry<>(key, value));
  }

  protected K resolveKey(IDOrBuilder<K> key) {
    return key.isPresentBuilder() ? key.getBuilder() : getKey(key.getID());
  }

  protected V resolveValue(IDOrBuilder<V> value) {
    return value.isPresentBuilder() ? value.getBuilder() : getValue(value.getID());
  }

}
