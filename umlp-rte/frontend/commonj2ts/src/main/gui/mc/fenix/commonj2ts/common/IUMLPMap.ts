import {Collection} from "../javautil/Collection";
import {AsyncSet} from "../javautil/AsyncSet";

/**
 * java.util.Map in transpiled code (like IUMLPList for java.util.List). All methods are async, as
 * implementations may have to load keys or values first (GemValueMap, GemEntryMap).
 *
 * Entries are tuples [key, value]; Map.Entry#setValue is realized by MapEntryHelper#setValue.
 * Implementations extend AbstractAsyncMap, which implements the default methods of java.util.Map.
 */
export interface IUMLPMap<K, V> {

  size(): Promise<number>;

  isEmpty(): Promise<boolean>;

  containsKey(key: any): Promise<boolean>;

  containsValue(value: any): Promise<boolean>;

  get(key: any): Promise<V>;

  put(key: K, value: V): Promise<V>;

  // java.util.Map#remove(Object) and java.util.Map#remove(Object, Object), told apart by the number of arguments
  remove(key: any): Promise<V>;

  remove(key: any, value: any): Promise<boolean>;

  putAll(m: IUMLPMap<K, V> | Map<K, V>): Promise<void>;

  clear(): Promise<void>;

  /** live view, removing from it removes from the map, adding is not supported */
  keySet(): Promise<AsyncSet<K>>;

  /** live view, removing from it removes from the map, adding is not supported */
  values(): Promise<Collection<V>>;

  /** live view, removing from it removes from the map, adding is not supported */
  entrySet(): Promise<AsyncSet<[K, V]>>;

  equals(o: any): Promise<boolean>;

  hashCode(): Promise<number>;

  getOrDefault(key: any, defaultValue: V): Promise<V>;

  forEach(action: (key: K, value: V) => void | Promise<void>): Promise<void>;

  replaceAll(fn: (key: K, value: V) => V | Promise<V>): Promise<void>;

  putIfAbsent(key: K, value: V): Promise<V>;

  // java.util.Map#replace(K, V) and java.util.Map#replace(K, V, V), told apart by the number of arguments
  replace(key: K, value: V): Promise<V>;

  replace(key: K, oldValue: V, newValue: V): Promise<boolean>;

  computeIfAbsent(key: K, fn: (key: K) => V | Promise<V>): Promise<V>;

  computeIfPresent(key: K, fn: (key: K, value: V) => V | Promise<V>): Promise<V>;

  compute(key: K, fn: (key: K, value: V) => V | Promise<V>): Promise<V>;

  merge(key: K, value: V, fn: (oldValue: V, value: V) => V | Promise<V>): Promise<V>;

  /**
   * This method must only be used in circumstances where you know that the IUMLPMap is just a wrapper for a synchronous map.
   * It should NEVER be used in transpiled code.
   */
  __internalMap(): Map<K, V>;

}
