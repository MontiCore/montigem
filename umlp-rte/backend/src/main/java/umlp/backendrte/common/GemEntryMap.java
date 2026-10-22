// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Keys and values are domain objects, both are stored by their gemIds.
 *
 * Attribute qualified associations derive the key of a value from the value itself
 * (see {@link #deriveKeyID}). Values added by id only (e.g. when loading) are keyed
 * on first access, which loads them in one batch.
 */
public abstract class GemEntryMap<K extends GemIdentifiable, V extends GemIdentifiable>
    extends AbstractMap<K, V>
    implements GemBuildable {

  // Lokaler speicher für die gemIds der enthaltenen Objekte. Enthält alle IDs der Qualifizierten Assoziation.
  protected HashMap<Long, Long> idMap;

  // ids of values whose key is not known yet, keyed by ensureKeyed()
  protected List<Long> unkeyedValueIDs = new ArrayList<>();

  // live views keySet, values, entrySet
  protected transient GemMapViews<Long, K, V> views = new GemMapViews<Long, K, V>() {
    @Override
    protected Collection<Long> currentKeys() {
      return getKeyIDs();
    }

    @Override
    protected Long lookupID(Long storedKey) {
      return GemEntryMap.this.lookupID(storedKey);
    }

    @Override
    protected Long toStoredKey(Object key) {
      return key instanceof GemIdentifiable ? ((GemIdentifiable) key).getGemId() : null;
    }

    @Override
    protected Map<Long, K> keyObjects(List<Long> storedKeys) {
      return loadKeysByID(storedKeys);
    }

    @Override
    protected Map<Long, V> valueObjects(List<Long> gemIDs) {
      return loadValuesByID(gemIDs);
    }

    @Override
    protected boolean containsValueID(long gemID) {
      return GemEntryMap.this.containsValueID(gemID);
    }

    @Override
    protected void removeValueID(long gemID) {
      _internal_removeValueIDAndSync(gemID);
    }

    @Override
    protected int size() {
      return GemEntryMap.this.size();
    }

    @Override
    protected void clear() {
      GemEntryMap.this.clear();
    }
  };

  public GemEntryMap(Map<Long, Long> idMap) {
    this.idMap = new LinkedHashMap<>(idMap);
  }

  public GemEntryMap() {
    this(Collections.emptyMap());
  }

  @Override
  public Optional<? extends GemEntryMap<K,V>> build() {
    return Optional.of((GemEntryMap<K,V>) this);
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

  public K getKey(Long gemID) {

    LoadResult<K> lr = loadSingleKeyObject(gemID);
    if (lr.isPresent()) {
      return lr.get();
    }
    else {
      Log.error("Can not load Object with id " + gemID);
      return null;
    }
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

  /**
   * live view, see {@link GemMapViews}
   */
  @Override
  public Set<K> keySet() {
    return views.keySet();
  }

  /**
   * live view, see {@link GemMapViews}
   */
  @Override
  public Collection<V> values() {
    return views.values();
  }

  /**
   * live view, see {@link GemMapViews}
   */
  @Override
  public Set<Entry<K,V>> entrySet() {
    return views.entrySet();
  }

  /**
   * @return the values with the gemIDs, loaded in one batch
   */
  protected Map<Long, V> loadValuesByID(List<Long> gemIDs) {
    Map<Long, V> result = new HashMap<>();
    if (gemIDs.isEmpty()) {
      return result;
    }
    List<LoadResult<V>> loaded = loadValueObjects(gemIDs);
    for (int i = 0; i < loaded.size(); i++) {
      if (loaded.get(i).isPresent()) {
        result.put(gemIDs.get(i), loaded.get(i).get());
      } else {
        Log.error("Can not load Object with id " + gemIDs.get(i));
      }
    }
    return result;
  }

  /**
   * @return the keys with the gemIDs, loaded in one batch
   */
  protected Map<Long, K> loadKeysByID(List<Long> gemIDs) {
    Map<Long, K> result = new HashMap<>();
    if (gemIDs.isEmpty()) {
      return result;
    }
    List<LoadResult<K>> loaded = loadKeyObjects(gemIDs);
    for (int i = 0; i < loaded.size(); i++) {
      if (loaded.get(i).isPresent()) {
        result.put(gemIDs.get(i), loaded.get(i).get());
      } else {
        Log.error("Can not load Object with id " + gemIDs.get(i));
      }
    }
    return result;
  }

  @Override
  public boolean containsKey(Object key) {
    if(key instanceof GemIdentifiable){
      return containsKeyID(((GemIdentifiable) key).getGemId());
    }else{
      Log.error("Key is not GemIdentifiable");
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    if(value instanceof GemIdentifiable){
      return containsValueID(((GemIdentifiable) value).getGemId());
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
  public boolean containsValueID(long gemID) {
    return idMap.containsValue(gemID) || unkeyedValueIDs.contains(gemID);
  }

  /**
   * identical to {@link Map#containsKey(Object)} but taking a gemID
   *
   * @param gemID the id of the object that may be contained in this Map
   * @return whether the object corresponding to gemID is contained in this Map
   */
  public boolean containsKeyID(long gemID) {
    ensureKeyed();
    return idMap.containsKey(gemID);
  }

  /**
   * @param gemID the id of a key
   * @return the gemID of the value stored for that key, or null
   */
  public Long getID(Long gemID) {
    ensureKeyed();
    return idMap.get(gemID);
  }

  /**
   * provides the key objects referenced by the gemIDs if possible
   *
   * @param gemIDs reference the key objects to get
   * @return the results of trying to load the objects
   */
  protected abstract List<LoadResult<K>> loadKeyObjects(List<Long> gemIDs);

  /**
   * provides the value objects referenced by the gemIDs if possible
   *
   * @param gemIDs reference the value objects to get
   * @return the results of trying to load the objects
   */
  protected abstract List<LoadResult<V>> loadValueObjects(List<Long> gemIDs);

  @Override
  public V put(K key, V value) {
    if (isKeyDerived()) {
      // the key of a value is derived from the value, it cannot be chosen
      long derived = deriveKeyID(value);
      if (key.getGemId() != derived) {
        Log.error("0xA7315 Key with id " + key.getGemId() + " does not match the key with id " + derived
            + " derived from the object with id " + value.getGemId());
        return null;
      }
      final V oldElem = get(key);
      _internal_putValueAndSync(value);
      return oldElem;
    }
    final V oldElem = get(key);
    _internal_putAndSync(key.getGemId(), value.getGemId());
    return oldElem;
  }

  @Override
  public V get(Object key) {
    if(key instanceof GemIdentifiable){
      Long valGemID = getID(((GemIdentifiable) key).getGemId());
      return valGemID == null ? null : getValue(valGemID);
    }else{
      Log.error("Key is not GemIdentifiable");
    }
    return null;
  }


  /**
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_putAndSync(long keyGemID, long valueGemID) {
    _internal_put(keyGemID, valueGemID);
  }

  public void _internal_put(long keyGemID, long valueGemID) {
    Long old = idMap.put(keyGemID, valueGemID);
    if (old != null && old != valueGemID) {
      onValueRemoved(old, null);
    }
  }


  @Override
  public V remove(Object key) {
    if (!containsKey(key)) {
      return null;
    }
    final V oldElem = get(key);
    _internal_removeAndSync(((GemIdentifiable) key).getGemId());
    return oldElem;
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_removeAndSync(Long keyGemID) {
    _internal_remove(keyGemID);
  }

  public void _internal_remove(long keyGemID) {
    Long old = idMap.remove(keyGemID);
    if (old != null) {
      onValueRemoved(old, null);
    }
  }


  /**
   * derives the gemID of the key of a value,
   * only supported by maps of attribute qualified associations
   */
  public long deriveKeyID(V value) {
    throw new UnsupportedOperationException("0xA7311 The keys of " + getClass().getName() + " are not derived from its values");
  }

  /**
   * whether the key of a value is derived from the value (attribute qualified associations),
   * see {@link #deriveKeyID}; then a value cannot be put under an arbitrary key
   */
  public boolean isKeyDerived() {
    return false;
  }

  /**
   * puts a value under its derived key;
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_putValueAndSync(V value) {
    _internal_putValue(value);
  }

  /**
   * removes the entry holding the value with gemID;
   * to be overridden by a subclass to add synchronization
   */
  protected void _internal_removeValueIDAndSync(long gemID) {
    _internal_removeValueID(gemID);
  }

  /**
   * stores a value under its derived key, without synchronization
   * (only for maps of attribute qualified associations)
   */
  public void _internal_putValue(V value) {
    _internal_put(deriveKeyID(value), value.getGemId());
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
    for (Entry<Long, Long> e : idMap.entrySet()) {
      if (e.getValue() == gemID) {
        idMap.remove(e.getKey());
        onValueRemoved(gemID, null);
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
        Log.error("0xA7312 Can not load Object with id " + gemIDs.get(i) + " to derive its key");
      }
    }
  }

  protected void keyLoadedValue(V value) {
    long keyID = deriveKeyID(value);
    Long present = lookupID(keyID);
    if (present != null && present != value.getGemId()) {
      Log.error("0xA7313 Key " + keyID + " is not unique: it is derived from the objects with ids "
          + present + " and " + value.getGemId());
      return;
    }
    _internal_put(keyID, value.getGemId());
    onValueKeyed(value);
  }

  /**
   * @return the id stored for the key with keyGemID, without deriving pending keys
   */
  protected Long lookupID(long keyGemID) {
    return idMap.get(keyGemID);
  }

  /**
   * hook, called whenever the key of a value has been derived;
   * used to observe the value to rekey it when the qualifying attribute changes
   */
  protected void onValueKeyed(V value) {
  }

  /**
   * hook, called whenever a value leaves the map: removed, or replaced by another value for its key;
   * used to stop observing it
   *
   * @param value the value, if it is at hand (e.g. an unbuilt builder), otherwise null
   */
  protected void onValueRemoved(long valueGemID, V value) {
  }

  /**
   * moves a value from oldKeyGemID to newKeyGemID, e.g. after its qualifying attribute changed.
   * Keys are derived locally, so this is neither synchronized nor persisted.
   */
  public void _internal_rekey(long oldKeyGemID, long newKeyGemID, long valueGemID) {
    if (!Objects.equals(lookupID(oldKeyGemID), valueGemID) || oldKeyGemID == newKeyGemID) {
      // not (or no longer) contained under oldKeyGemID
      return;
    }
    if (lookupID(newKeyGemID) != null) {
      Log.error("0xA7314 Key " + newKeyGemID + " is not unique: it is derived from the objects with ids "
          + lookupID(newKeyGemID) + " and " + valueGemID);
      return;
    }
    moveEntry(oldKeyGemID, newKeyGemID);
  }

  protected void moveEntry(long oldKeyGemID, long newKeyGemID) {
    idMap.put(newKeyGemID, idMap.remove(oldKeyGemID));
  }


  /**
   * returns if the element is available
   * e.g. one is authorized to access it
   * this is NOT a range check
   */
  // TODO: needed?
  public boolean isPresent(Long gemID) {
    Long valGemID = getID(gemID);
    return valGemID != null && loadSingleValueObject(valGemID).isPresent();
  }


  public LoadResult<V> loadSingleValueObject(long id) {
    List<LoadResult<V>> res = loadValueObjects(Collections.singletonList(id));
    if (res.isEmpty()) {
      return new LoadResult<>(LoadingState.FAILED_TO_LOAD);
    }
    return res.get(0);
  }

  public LoadResult<K> loadSingleKeyObject(long id) {
    List<LoadResult<K>> res = loadKeyObjects(Collections.singletonList(id));
      if (res.isEmpty()) {
        return new LoadResult<>(LoadingState.FAILED_TO_LOAD);
      }

    return res.get(0);
  }

  public Set<Long> getKeyIDs() {
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

  public Map<Long,Long> getIDMap() {
    ensureKeyed();
    return idMap;
  }


  /**
   * removes all entries, like removing them one by one (including synchronization)
   */
  @Override
  public void clear(){
    for (long gemID : getValueIDs()) {
      _internal_removeValueIDAndSync(gemID);
    }
  }

}
