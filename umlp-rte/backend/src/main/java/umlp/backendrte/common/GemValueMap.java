// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/*
 * A lazy Map for Keys/Values
 * Elements are loaded as they are required.
 * Useful if e.g. the elements are loaded over a network.
 *
 * The keys are values (numbers, Strings), the values are domain objects.
 *
 * Attribute qualified associations derive the key of a value from the value itself
 * (see {@link #deriveKey}). Values added by id only (e.g. when loading) are keyed
 * on first access, which loads them in one batch.
 */
public abstract class GemValueMap<K, V extends GemIdentifiable>
    extends AbstractMap<K, V>
    implements GemBuildable {

  // Lokaler speicher für die gemIds der enthaltenen Objekte. Enthält alle IDs der Qualifizierten Assoziation.
  protected HashMap<K, Long> idMap;

  // ids of values whose key is not known yet, keyed by ensureKeyed()
  protected List<Long> unkeyedValueIDs = new ArrayList<>();

  public GemValueMap(Map<K, Long> idMap) {
    this.idMap = new HashMap<>(idMap);
  }

  public GemValueMap() {
    this(Collections.emptyMap());
  }

  @Override
  public Optional<? extends GemValueMap<K,V>> build() {
    return Optional.of((GemValueMap<K,V>) this);
  }

  @Override
  public int size() {
    ensureKeyed();
    return idMap.size();
  }

  @Override
  public boolean isEmpty() {
    return idMap.isEmpty() && unkeyedValueIDs.isEmpty();
  }

  public V getValue(Long gemID) {
    LoadResult<V> lr = loadSingleValueObject(gemID);
    if (lr.isPresent()) {
      return lr.get();
    }
    else {
      Log.error("Can not load Object with id " + gemID);
      return null;
    }
  }

  /** DO NOT USE if avoidable
   * Loads all Map elements from database,
   * @return EntrySet<K,V> the Entry Set of the GemValueMap
   */
  @Override
  public Set<Entry<K,V>> entrySet() {
    ensureKeyed();
    Set<Entry<K, V>> result = new HashSet<>();
    for (Entry<K, Long> e : idMap.entrySet()) {
      Entry<K,V> entry = new SimpleEntry<>(e.getKey(), getValue(e.getValue()));
      result.add(entry);
    }
    return result;
  }

  @Override
  public boolean containsKey(Object key) {
    ensureKeyed();
    return idMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    if(value instanceof GemIdentifiable){
      return containsValue(((GemIdentifiable) value).getGemId());
    }else{
      Log.error("Value is not GemIdentifiable");
    }
    return false;
  }

  /**
   * identical to {@link Map#containsValue(Object)} but taking a gemID
   *
   * @param gemID the id of the object that may be contained in this Map
   * @return whether the object corresponding to gemID is contained in this Map
   */
  public boolean containsValue(Long gemID) {
    return idMap.containsValue(gemID) || unkeyedValueIDs.contains(gemID);
  }

  /**
   * @return the gemID of the value stored for key, or null
   */
  public Long getID(Object key) {
    ensureKeyed();
    return idMap.get(key);
  }

  /**
   * provides the value objects referenced by the gemIDs if possible
   *
   * @param gemIDs reference the value objects to get
   * @return the results of trying to load the objects
   */
  protected abstract List<LoadResult<V>> loadValueObjects(List<Long> gemIDs);

  @Override
  public V put(K key, V value) {
    final V oldElem = get(key);
    _internal_putAndSync(key, value.getGemId());
    return oldElem;
  }

  @Override
  public V get(Object key) {
    Long valGemID = getID(key);
    return valGemID == null ? null : getValue(valGemID);
  }


  /**
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_putAndSync(K key, long valueGemID) {
    _internal_put(key, valueGemID);
  }

  public void _internal_put(K key, long valueGemID) {
    idMap.put(key, valueGemID);
  }


  @Override
  public V remove(Object key) {
    if (!containsKey(key)) {
      return null;
    }
    final V oldElem = get(key);
    _internal_removeAndSync((K) key);
    return oldElem;
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_removeAndSync(K key) {
    _internal_remove(key);
  }

  public void _internal_remove(K key) {
    idMap.remove(key);
  }


  /**
   * derives the key of a value, only supported by maps of attribute qualified associations
   */
  public K deriveKey(V value) {
    throw new UnsupportedOperationException("0xA7301 The keys of " + getClass().getName() + " are not derived from its values");
  }

  /**
   * stores a value under its derived key, without synchronization
   * (only for maps of attribute qualified associations)
   */
  public void _internal_putValue(V value) {
    _internal_put(deriveKey(value), value.getGemId());
    onValueKeyed(value);
  }

  /**
   * removes the entry holding the value with gemID, without synchronization
   * @return whether such an entry existed
   */
  public boolean _internal_removeValueID(long gemID) {
    if (unkeyedValueIDs.remove(Long.valueOf(gemID))) {
      return true;
    }
    for (Entry<K, Long> e : idMap.entrySet()) {
      if (e.getValue() == gemID) {
        idMap.remove(e.getKey());
        return true;
      }
    }
    return false;
  }

  /**
   * adds values by their ids only; their keys are derived on first access
   * (only for maps of attribute qualified associations)
   */
  public void _internal_addValueIDs(Collection<Long> gemIDs) {
    unkeyedValueIDs.addAll(gemIDs);
  }

  /**
   * derives the keys of all values added by id only, loading them in one batch
   */
  protected void ensureKeyed() {
    if (unkeyedValueIDs.isEmpty()) {
      return;
    }
    List<Long> gemIDs = new ArrayList<>(unkeyedValueIDs);
    unkeyedValueIDs.clear();
    List<LoadResult<V>> loaded = loadValueObjects(gemIDs);
    for (int i = 0; i < loaded.size(); i++) {
      if (loaded.get(i).isPresent()) {
        keyLoadedValue(loaded.get(i).get());
      } else {
        Log.error("0xA7302 Can not load Object with id " + gemIDs.get(i) + " to derive its key");
      }
    }
  }

  protected void keyLoadedValue(V value) {
    K key = deriveKey(value);
    Long present = lookupID(key);
    if (present != null && present != value.getGemId()) {
      Log.error("0xA7303 Key " + key + " is not unique: it is derived from the objects with ids "
          + present + " and " + value.getGemId());
      return;
    }
    _internal_put(key, value.getGemId());
    onValueKeyed(value);
  }

  /**
   * @return the id stored for key, without deriving pending keys
   */
  protected Long lookupID(Object key) {
    return idMap.get(key);
  }

  /**
   * hook, called whenever the key of a value has been derived;
   * used to observe the value to rekey it when the qualifying attribute changes
   */
  protected void onValueKeyed(V value) {
  }

  /**
   * moves a value from oldKey to newKey, e.g. after its qualifying attribute changed.
   * Keys are derived locally, so this is neither synchronized nor persisted.
   */
  public void _internal_rekey(K oldKey, K newKey, long valueGemID) {
    if (!Objects.equals(lookupID(oldKey), valueGemID) || Objects.equals(oldKey, newKey)) {
      // not (or no longer) contained under oldKey
      return;
    }
    if (lookupID(newKey) != null) {
      Log.error("0xA7304 Key " + newKey + " is not unique: it is derived from the objects with ids "
          + lookupID(newKey) + " and " + valueGemID);
      return;
    }
    moveEntry(oldKey, newKey);
  }

  protected void moveEntry(K oldKey, K newKey) {
    idMap.put(newKey, idMap.remove(oldKey));
  }


  /**
   * returns if the element is available
   * e.g. one is authorized to access it
   * this is NOT a range check
   */
  // TODO: needed?
  public boolean isPresent(Object key) {
    Long valGemID = getID(key);
    return valGemID != null && loadSingleValueObject(valGemID).isPresent();
  }


  public LoadResult<V> loadSingleValueObject(long id) {
    List<LoadResult<V>> res = loadValueObjects(Collections.singletonList(id));
    if (res.isEmpty()) {
      return new LoadResult<>(LoadingState.FAILED_TO_LOAD);
    }
    return res.get(0);
  }

  public Set<K> getKeys() {
    ensureKeyed();
    return idMap.keySet();
  }

  /**
   * the ids of all values, without deriving keys
   */
  public Collection<Long> getValueIDs() {
    List<Long> result = new ArrayList<>(idMap.values());
    result.addAll(unkeyedValueIDs);
    return result;
  }

  public Map<K,Long> getIDMap() {
    ensureKeyed();
    return idMap;
  }


  public void clear(){
    idMap.clear();
    unkeyedValueIDs.clear();
  }

}
