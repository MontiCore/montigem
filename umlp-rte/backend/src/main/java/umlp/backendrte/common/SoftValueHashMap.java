package umlp.backendrte.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SoftValueHashMap<K,V> extends AbstractMap<K,V> {
  private class SoftValueRef<K, V> extends SoftReference<V> {
    public  K key;

    private SoftValueRef(K key, V val, ReferenceQueue q)
    {
      super(val, q);
      this.key = key;
    }

    private SoftValueHashMap<K, V>.SoftValueRef<K, V> create(K key, V val, ReferenceQueue q) {
      if (val == null) return null;
      else return (SoftValueHashMap<K, V>.SoftValueRef<K, V>) new SoftValueRef<K, V>(key, val, q);
    }

  }
  public Set<Entry<K, V>> entrySet() {
    processQueue();
    HashMap<K,V> entryMap = new HashMap<>();
    for (Entry<K, SoftValueRef<K,V>> entry : hash.entrySet()) {
      if(entry.getValue()!=null) {
        entryMap.put(entry.getKey(), entry.getValue().get());
      }else{
        entryMap.put(entry.getKey(), null);
      }
    }
    return entryMap.entrySet();
  }

  /* Hash table mapping SoftKeys to values */
  private Map<K,SoftValueRef<K,V>> hash;

  /* Reference queue for cleared SoftKeys */
  private ReferenceQueue queue = new ReferenceQueue();

  /* Remove all invalidated entries from the map, that is, remove all entries
     whose values have been discarded.
   */
  private void processQueue() {
    SoftValueRef<K,V> ref;
    while ((ref = (SoftValueRef<K,V>)queue.poll()) != null) {
      if (ref == hash.get(ref.key)) {
        // only remove if it is the *exact* same SoftValueRef
        hash.remove(ref.key);
      }
    }
  }


  /* -- Constructors -- */

  /**
   * Constructs a new, empty <code>SoftValueHashMap</code> with the given
   * initial capacity and the given load factor.
   *
   * @param  initialCapacity  The initial capacity of the
   *                          <code>SoftValueHashMap</code>
   *
   * @param  loadFactor       The load factor of the <code>SoftValueHashMap</code>
   *
   * @throws IllegalArgumentException  If the initial capacity is less than
   *                                   zero, or if the load factor is
   *                                   nonpositive
   */
  public SoftValueHashMap(int initialCapacity, float loadFactor)
  {
    hash = new HashMap<>(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty <code>SoftValueHashMap</code> with the given
   * initial capacity and the default load factor, which is
   * <code>0.75</code>.
   *
   * @param  initialCapacity  The initial capacity of the
   *                          <code>SoftValueHashMap</code>
   *
   * @throws IllegalArgumentException  If the initial capacity is less than
   *                                   zero
   */
  public SoftValueHashMap(int initialCapacity)
  {
    hash = new HashMap<>(initialCapacity);
  }

  /**
   * Constructs a new, empty <code>SoftValueHashMap</code> with the default
   * initial capacity and the default load factor, which is
   * <code>0.75</code>.
   */
  public SoftValueHashMap()
  {
    hash = new HashMap<>();
  }

  /**
   * Constructs a new <code>SoftValueHashMap</code> with the same mappings as the
   * specified <tt>Map</tt>.  The <code>SoftValueHashMap</code> is created with an
   * initial capacity of twice the number of mappings in the specified map
   * or 11 (whichever is greater), and a default load factor, which is
   * <tt>0.75</tt>.
   *
   * @param   t the map whose mappings are to be placed in this map.
   */
  public SoftValueHashMap(Map t) {
    this(Math.max(2*t.size(), 11), 0.75f);
    putAll(t);
  }

  /* -- Simple queries -- */

  /**
   * Returns the number of key-value mappings in this map.
   * <strong>Note:</strong> <em>In contrast with most implementations of the
   * <code>Map</code> interface, the time required by this operation is
   * linear in the size of the map.</em>
   */
  public int size()
  {
    processQueue();
    return hash.size();
  }

  /**
   * Returns <code>true</code> if this map contains no key-value mappings.
   */
  public boolean isEmpty()
  {
    processQueue();
    return hash.isEmpty();
  }

  /**
   * Returns <code>true</code> if this map contains a mapping for the
   * specified key.
   *
   * @param   key   The key whose presence in this map is to be tested
   */
  public boolean containsKey(Object key)
  {
    processQueue();
    return hash.containsKey(key);
  }

  /* -- Lookup and modification operations -- */

  /**
   * Returns the value to which this map maps the specified <code>key</code>.
   * If this map does not contain a value for this key, then return
   * <code>null</code>.
   *
   * @param  key  The key whose associated value, if any, is to be returned
   */
  public V get(Object key) {
    processQueue();
    SoftReference<V> ref = hash.get(key);
    if (ref != null) return ref.get();
    return null;
  }

  /**
   * Updates this map so that the given <code>key</code> maps to the given
   * <code>value</code>.  If the map previously contained a mapping for
   * <code>key</code> then that mapping is replaced and the previous value is
   * returned.
   *
   * @param  key    The key that is to be mapped to the given
   *                <code>value</code>
   * @param  value  The value to which the given <code>key</code> is to be
   *                mapped
   *
   * @return  The previous value to which this key was mapped, or
   *          <code>null</code> if there was no mapping for the key
   */
  public V put(K key, V value){
    processQueue();
    SoftValueRef<K, V> old = hash.put(key, value == null ? null : new SoftValueRef<K, V>(key, value, queue));
    return old == null ? null : old.get();
  }

  /**
   * Removes the mapping for the given <code>key</code> from this map, if
   * present.
   *
   * @param  key  The key whose mapping is to be removed
   *
   * @return  The value to which this key was mapped, or <code>null</code> if
   *          there was no mapping for the key
   */
  public V remove(Object key)
  {
    processQueue();
    SoftValueRef<K, V> ref = hash.remove(key);
    return ref == null ? null : ref.get();
  }

  /**
   * Removes all mappings from this map.
   */
  public void clear()
  {
    processQueue();
    hash.clear();
  }
}
