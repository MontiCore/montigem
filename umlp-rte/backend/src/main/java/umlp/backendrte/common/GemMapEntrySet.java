// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Live entry set of a GemValueMap or GemEntryMap. The keySet and values of the map are projections of it
 * (see {@link #keySet()}, {@link #values()}), like java.util.AbstractMap derives them from its entrySet.
 * <ul>
 *   <li>Reading always reflects the current content of the map.</li>
 *   <li>Removing (also through the iterators and the projections) removes from the map, including
 *   synchronization, by gemId; adding is not supported, as keys are derived from the values.</li>
 *   <li>Iterators are weakly consistent: they iterate the keys present when they were created,
 *   skip entries removed in the meantime and never throw ConcurrentModificationException.</li>
 *   <li>Entries are lazy: an entry holds the stored key and the gemId of its value. Its key object (GemEntryMap)
 *   and its value are loaded on first access, together with those of the remaining entries of the iterator
 *   (one batch per iterator). Entries are immutable (setValue is not supported); equals and hashCode follow
 *   {@link Entry} and therefore load the value.</li>
 * </ul>
 *
 * @param <I> the type the map stores its keys as: the key itself (GemValueMap) or its gemId (GemEntryMap)
 * @param <K> the key type
 * @param <V> the value type
 */
abstract class GemMapEntrySet<I, K, V extends GemIdentifiable> extends AbstractSet<Entry<K, V>> {

  /**
   * @return the map this is the entry set of
   */
  protected abstract Map<K, V> map();

  /**
   * @return the stored keys, after deriving pending keys
   */
  protected abstract Collection<I> currentKeys();

  /**
   * @return the gemId of the value stored for the stored key, or null
   */
  protected abstract Long lookupID(I storedKey);

  /**
   * @return the stored key of a key given to a view method, or null if it cannot be a key
   */
  protected abstract I toStoredKey(Object key);

  /**
   * @return the key objects of the stored keys (loaded in one batch for GemEntryMaps)
   */
  protected abstract Map<I, K> keyObjects(List<I> storedKeys);

  /**
   * @return the values with the gemIds (loaded in one batch)
   */
  protected abstract Map<Long, V> valueObjects(List<Long> gemIDs);

  protected abstract boolean containsValueID(long gemID);

  /**
   * removes the entry holding the value with gemID, including synchronization
   */
  protected abstract void removeValueID(long gemID);

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new EntryIterator();
  }

  @Override
  public int size() {
    return map().size();
  }

  @Override
  public boolean isEmpty() {
    return map().isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return gemIDOfEntry(o) != null;
  }

  @Override
  public boolean remove(Object o) {
    Long gemID = gemIDOfEntry(o);
    if (gemID == null) {
      return false;
    }
    removeValueID(gemID);
    return true;
  }

  @Override
  public void clear() {
    map().clear();
  }

  /**
   * the keys of the map: a projection of the entries that loads no values
   */
  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        return new Projection<K>() {
          @Override
          public K next() {
            return entries.next().getKey();
          }
        };
      }

      @Override
      public int size() {
        return map().size();
      }

      @Override
      public boolean isEmpty() {
        return map().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
        return gemIDOfKey(o) != null;
      }

      @Override
      public boolean remove(Object o) {
        Long gemID = gemIDOfKey(o);
        if (gemID == null) {
          return false;
        }
        removeValueID(gemID);
        return true;
      }

      @Override
      public void clear() {
        map().clear();
      }
    };
  }

  /**
   * the values of the map: a projection of the entries; contains and remove work by gemId
   */
  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return new Projection<V>() {
          @Override
          public V next() {
            return entries.next().getValue();
          }
        };
      }

      @Override
      public int size() {
        return map().size();
      }

      @Override
      public boolean isEmpty() {
        return map().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
        return o instanceof GemIdentifiable && containsValueID(((GemIdentifiable) o).getGemId());
      }

      @Override
      public boolean remove(Object o) {
        if (!contains(o)) {
          return false;
        }
        removeValueID(((GemIdentifiable) o).getGemId());
        return true;
      }

      @Override
      public void clear() {
        map().clear();
      }
    };
  }

  /**
   * @return the gemId of the value stored for the key, or null
   */
  protected Long gemIDOfKey(Object key) {
    // derive pending keys first, a loaded map knows its values by id only
    currentKeys();
    I storedKey = toStoredKey(key);
    return storedKey == null ? null : lookupID(storedKey);
  }

  /**
   * @return the gemId of the value if o is an entry contained in the map, otherwise null
   */
  protected Long gemIDOfEntry(Object o) {
    if (o instanceof GemMapEntrySet.GemMapEntry && ((GemMapEntrySet<?, ?, ?>.GemMapEntry) o).entrySet() == this) {
      // an entry of this set: compare by gemId, without loading
      GemMapEntry entry = (GemMapEntry) o;
      currentKeys();
      return Objects.equals(lookupID(entry.storedKey), entry.gemID) ? entry.gemID : null;
    }
    if (!(o instanceof Entry) || !(((Entry<?, ?>) o).getValue() instanceof GemIdentifiable)) {
      return null;
    }
    Entry<?, ?> entry = (Entry<?, ?>) o;
    Long gemID = gemIDOfKey(entry.getKey());
    return gemID != null && gemID == ((GemIdentifiable) entry.getValue()).getGemId() ? gemID : null;
  }

  /**
   * An entry of the map, holding the stored key and the gemId of the value; loads both on first access
   */
  protected class GemMapEntry implements Entry<K, V> {

    protected final I storedKey;

    protected final long gemID;

    // loads key objects and values in batches
    protected final EntryIterator loader;

    protected GemMapEntry(I storedKey, long gemID, EntryIterator loader) {
      this.storedKey = storedKey;
      this.gemID = gemID;
      this.loader = loader;
    }

    protected GemMapEntrySet<I, K, V> entrySet() {
      return GemMapEntrySet.this;
    }

    @Override
    public K getKey() {
      return loader.keyOf(storedKey);
    }

    @Override
    public V getValue() {
      return loader.valueOf(gemID);
    }

    /**
     * not supported: the key is derived from the value
     */
    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      return Objects.equals(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
    }

    @Override
    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  /**
   * Weakly consistent iterator over the keys present at its creation
   */
  protected class EntryIterator implements Iterator<Entry<K, V>> {

    protected final List<I> keys;

    protected int idx = 0;

    protected boolean hasPending = false;

    protected I pendingKey;

    protected long pendingID;

    protected boolean hasLast = false;

    protected I lastKey;

    protected long lastID;

    // loaded on first use, in one batch
    protected Map<Long, V> values;

    protected Map<I, K> keyObjects;

    protected EntryIterator() {
      this.keys = new ArrayList<>(currentKeys());
    }

    @Override
    public boolean hasNext() {
      while (!hasPending && idx < keys.size()) {
        I key = keys.get(idx++);
        Long gemID = lookupID(key);
        if (gemID != null) {
          hasPending = true;
          pendingKey = key;
          pendingID = gemID;
        }
      }
      return hasPending;
    }

    @Override
    public Entry<K, V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      hasPending = false;
      hasLast = true;
      lastKey = pendingKey;
      lastID = pendingID;
      return new GemMapEntry(lastKey, lastID, this);
    }

    @Override
    public void remove() {
      if (!hasLast) {
        throw new IllegalStateException();
      }
      hasLast = false;
      // only if the entry is still there
      if (Objects.equals(lookupID(lastKey), lastID)) {
        removeValueID(lastID);
      }
    }

    protected V valueOf(long gemID) {
      if (values == null) {
        // all values of the remaining keys in one batch
        List<Long> gemIDs = new ArrayList<>();
        for (int i = Math.max(idx - 1, 0); i < keys.size(); i++) {
          Long id = lookupID(keys.get(i));
          if (id != null) {
            gemIDs.add(id);
          }
        }
        values = new HashMap<>(valueObjects(gemIDs));
      }
      if (!values.containsKey(gemID)) {
        // rekeyed or replaced after the batch was loaded
        values.putAll(valueObjects(List.of(gemID)));
      }
      return values.get(gemID);
    }

    protected K keyOf(I storedKey) {
      if (keyObjects == null) {
        keyObjects = new HashMap<>(GemMapEntrySet.this.keyObjects(keys.subList(Math.max(idx - 1, 0), keys.size())));
      }
      if (!keyObjects.containsKey(storedKey)) {
        keyObjects.putAll(GemMapEntrySet.this.keyObjects(List.of(storedKey)));
      }
      return keyObjects.get(storedKey);
    }
  }

  /**
   * iterator of a projection (keys, values): maps the entries of an EntryIterator
   */
  protected abstract class Projection<T> implements Iterator<T> {

    protected final EntryIterator entries = new EntryIterator();

    @Override
    public boolean hasNext() {
      return entries.hasNext();
    }

    @Override
    public void remove() {
      entries.remove();
    }
  }
}
