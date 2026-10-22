// (c) https://github.com/MontiCore/monticore
package umlp.backendrte.common;

import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleImmutableEntry;
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
 * Live views (keySet, values, entrySet) of a GemValueMap or GemEntryMap.
 * <ul>
 *   <li>Reading through a view always reflects the current content of the map.</li>
 *   <li>Removing through a view (also through its iterators) removes from the map,
 *   including synchronization; adding is not supported, as keys are derived from the values.</li>
 *   <li>Iterators are weakly consistent: they iterate the keys present when they were created,
 *   skip entries removed in the meantime and never throw ConcurrentModificationException.
 *   Values (and key objects of GemEntryMaps) are loaded in one batch per iterator.</li>
 * </ul>
 *
 * @param <I> the type the map stores its keys as: the key itself (GemValueMap) or its gemId (GemEntryMap)
 * @param <K> the key type
 * @param <V> the value type
 */
abstract class GemMapViews<I, K, V extends GemIdentifiable> {

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

  /**
   * removes the entry holding the value with gemID, including synchronization
   */
  protected abstract void removeValueID(long gemID);

  protected abstract int size();

  protected abstract void clear();

  protected boolean containsStoredKey(I storedKey) {
    return lookupID(storedKey) != null;
  }

  protected boolean containsValueID(long gemID) {
    for (I key : currentKeys()) {
      if (Objects.equals(lookupID(key), gemID)) {
        return true;
      }
    }
    return false;
  }

  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        return new ViewIterator<K>() {
          @Override
          protected K element(I storedKey, long gemID) {
            return keyOf(storedKey);
          }
        };
      }

      @Override
      public int size() {
        return GemMapViews.this.size();
      }

      @Override
      public boolean contains(Object o) {
        I storedKey = toStoredKey(o);
        return storedKey != null && containsStoredKey(storedKey);
      }

      @Override
      public boolean remove(Object o) {
        I storedKey = toStoredKey(o);
        Long gemID = storedKey == null ? null : lookupID(storedKey);
        if (gemID == null) {
          return false;
        }
        removeValueID(gemID);
        return true;
      }

      @Override
      public void clear() {
        GemMapViews.this.clear();
      }
    };
  }

  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return new ViewIterator<V>() {
          @Override
          protected V element(I storedKey, long gemID) {
            return valueOf(gemID);
          }
        };
      }

      @Override
      public int size() {
        return GemMapViews.this.size();
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
        GemMapViews.this.clear();
      }
    };
  }

  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new ViewIterator<Entry<K, V>>() {
          @Override
          protected Entry<K, V> element(I storedKey, long gemID) {
            // immutable: setValue is not supported, the key is derived from the value
            return new SimpleImmutableEntry<>(keyOf(storedKey), valueOf(gemID));
          }
        };
      }

      @Override
      public int size() {
        return GemMapViews.this.size();
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
        GemMapViews.this.clear();
      }
    };
  }

  /**
   * @return the gemId of the value if o is an entry contained in the map, otherwise null
   */
  protected Long gemIDOfEntry(Object o) {
    if (!(o instanceof Entry) || !(((Entry<?, ?>) o).getValue() instanceof GemIdentifiable)) {
      return null;
    }
    Entry<?, ?> entry = (Entry<?, ?>) o;
    I storedKey = toStoredKey(entry.getKey());
    Long gemID = storedKey == null ? null : lookupID(storedKey);
    return gemID != null && gemID == ((GemIdentifiable) entry.getValue()).getGemId() ? gemID : null;
  }

  /**
   * Weakly consistent iterator over the keys present at its creation
   */
  protected abstract class ViewIterator<T> implements Iterator<T> {

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

    protected ViewIterator() {
      this.keys = new ArrayList<>(currentKeys());
    }

    protected abstract T element(I storedKey, long gemID);

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
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      hasPending = false;
      hasLast = true;
      lastKey = pendingKey;
      lastID = pendingID;
      return element(lastKey, lastID);
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
        for (int i = idx - 1; i < keys.size(); i++) {
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
        keyObjects = new HashMap<>(keyObjects(keys.subList(idx - 1, keys.size())));
      }
      if (!keyObjects.containsKey(storedKey)) {
        keyObjects.putAll(GemMapViews.this.keyObjects(List.of(storedKey)));
      }
      return keyObjects.get(storedKey);
    }
  }
}
