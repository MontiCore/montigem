import {IUMLPMap} from "../common/IUMLPMap";
import {Collection} from "./Collection";
import {AsyncSet} from "./AsyncSet";
import {JavaUtilUtils} from "./JavaUtilUtils";
import {MapEntryHelper} from "./MapEntryHelper";

/**
 * Base of all IUMLPMaps, the counterpart of java.util.AbstractMap: implements the default methods of
 * java.util.Map once on top of the core methods, which subclasses implement. Lambdas may be async, they are awaited.
 *
 * Like the backend GemMaps (which extend java.util.AbstractMap), the TS GemMaps inherit these methods and fail
 * where the backend fails: replaceAll needs Map.Entry#setValue, which their entries do not support.
 */
export abstract class AbstractAsyncMap<K, V> implements IUMLPMap<K, V> {

  // ─── Core methods, implemented by subclasses ──────────────────────────────

  /** java.util.Map#size() → int */
  abstract size(): Promise<number>;

  /** java.util.Map#containsKey(Object) → boolean */
  abstract containsKey(key: any): Promise<boolean>;

  /** java.util.Map#get(Object) → V, null if there is no value for the key */
  abstract get(key: any): Promise<V>;

  /** java.util.Map#put(K, V) → V, the previous value or null */
  abstract put(key: K, value: V): Promise<V>;

  /** java.util.Map#remove(Object) → V, the previous value or null */
  protected abstract removeKey(key: any): Promise<V>;

  /** java.util.Map#clear() → void */
  abstract clear(): Promise<void>;

  /** java.util.Map#keySet() → Set<K> */
  abstract keySet(): Promise<AsyncSet<K>>;

  /** java.util.Map#values() → Collection<V> */
  abstract values(): Promise<Collection<V>>;

  /** java.util.Map#entrySet() → Set<Map.Entry<K, V>> */
  abstract entrySet(): Promise<AsyncSet<[K, V]>>;

  abstract __internalMap(): Map<K, V>;

  // ─── Default methods ──────────────────────────────────────────────────────

  /** java.util.Map#isEmpty() → boolean */
  async isEmpty(): Promise<boolean> {
    return (await this.size()) === 0;
  }

  /** java.util.Map#containsValue(Object) → boolean */
  async containsValue(value: any): Promise<boolean> {
    for await (const v of await this.values()) {
      if (await JavaUtilUtils.equals(v, value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * java.util.Map#remove(Object) → V
   * java.util.Map#remove(Object, Object) → boolean
   */
  remove(key: any): Promise<V>;
  remove(key: any, value: any): Promise<boolean>;
  async remove(key: any, value?: any): Promise<V | boolean> {
    if (arguments.length < 2) {
      return this.removeKey(key);
    }
    if (!(await this.containsMapping(key, value))) {
      return false;
    }
    await this.removeKey(key);
    return true;
  }

  /** java.util.Map#putAll(Map<? extends K, ? extends V>) → void */
  async putAll(m: IUMLPMap<K, V> | Map<K, V>): Promise<void> {
    if (m instanceof Map) {
      for (const [k, v] of Array.from(m.entries())) {
        await this.put(k, v);
      }
      return;
    }
    for await (const [k, v] of await m.entrySet()) {
      await this.put(k, v);
    }
  }

  /** java.util.Map#equals(Object) → boolean, like java.util.AbstractMap */
  async equals(o: any): Promise<boolean> {
    if (o === this) {
      return true;
    }
    if (!(o instanceof AbstractAsyncMap)) {
      return false;
    }
    if ((await o.size()) !== (await this.size())) {
      return false;
    }
    for await (const [k, v] of await this.entrySet()) {
      if (v == null) {
        if (!((await o.get(k)) == null && await o.containsKey(k))) {
          return false;
        }
      } else if (!(await JavaUtilUtils.equals(v, await o.get(k)))) {
        return false;
      }
    }
    return true;
  }

  /** java.util.Map#hashCode() → int, like java.util.AbstractMap: the sum of the entry hashes */
  async hashCode(): Promise<number> {
    let hash = 0;
    for await (const [k, v] of await this.entrySet()) {
      hash = (hash + ((await AbstractAsyncMap.__hash(k)) ^ (await AbstractAsyncMap.__hash(v)))) | 0;
    }
    return hash;
  }

  /** java.util.Map#getOrDefault(Object, V) → V */
  async getOrDefault(key: any, defaultValue: V): Promise<V> {
    const v = await this.get(key);
    return (v != null || await this.containsKey(key)) ? v : defaultValue;
  }

  /** java.util.Map#forEach(BiConsumer<K, V>) → void */
  async forEach(action: (key: K, value: V) => unknown): Promise<void> {
    for await (const [k, v] of await this.entrySet()) {
      await action(k, v);
    }
  }

  /** java.util.Map#replaceAll(BiFunction<K, V, V>) → void, sets the values through the entries like java.util.Map */
  async replaceAll(fn: (key: K, value: V) => V | Promise<V>): Promise<void> {
    for await (const entry of await this.entrySet()) {
      await MapEntryHelper.setValue(entry, await fn(entry[0], entry[1]));
    }
  }

  /** java.util.Map#putIfAbsent(K, V) → V */
  async putIfAbsent(key: K, value: V): Promise<V> {
    let v = await this.get(key);
    if (v == null) {
      v = await this.put(key, value);
    }
    return v;
  }

  /**
   * java.util.Map#replace(K, V) → V
   * java.util.Map#replace(K, V, V) → boolean
   */
  replace(key: K, value: V): Promise<V>;
  replace(key: K, oldValue: V, newValue: V): Promise<boolean>;
  async replace(key: K, valueOrOldValue: V, newValue?: V): Promise<V | boolean> {
    if (arguments.length < 3) {
      if ((await this.get(key)) != null || await this.containsKey(key)) {
        return this.put(key, valueOrOldValue);
      }
      return null;
    }
    if (!(await this.containsMapping(key, valueOrOldValue))) {
      return false;
    }
    await this.put(key, newValue);
    return true;
  }

  /** java.util.Map#computeIfAbsent(K, Function<K, V>) → V */
  async computeIfAbsent(key: K, fn: (key: K) => V | Promise<V>): Promise<V> {
    const v = await this.get(key);
    if (v == null) {
      const newValue = await fn(key);
      if (newValue != null) {
        await this.put(key, newValue);
        return newValue;
      }
    }
    return v;
  }

  /** java.util.Map#computeIfPresent(K, BiFunction<K, V, V>) → V */
  async computeIfPresent(key: K, fn: (key: K, value: V) => V | Promise<V>): Promise<V> {
    const oldValue = await this.get(key);
    if (oldValue == null) {
      return null;
    }
    const newValue = await fn(key, oldValue);
    if (newValue == null) {
      await this.removeKey(key);
      return null;
    }
    await this.put(key, newValue);
    return newValue;
  }

  /** java.util.Map#compute(K, BiFunction<K, V, V>) → V */
  async compute(key: K, fn: (key: K, value: V) => V | Promise<V>): Promise<V> {
    const oldValue = await this.get(key);
    const newValue = await fn(key, oldValue);
    if (newValue == null) {
      if (oldValue != null || await this.containsKey(key)) {
        await this.removeKey(key);
      }
      return null;
    }
    await this.put(key, newValue);
    return newValue;
  }

  /** java.util.Map#merge(K, V, BiFunction<V, V, V>) → V */
  async merge(key: K, value: V, fn: (oldValue: V, value: V) => V | Promise<V>): Promise<V> {
    JavaUtilUtils.requireNonNull(value);
    const oldValue = await this.get(key);
    const newValue = (oldValue == null) ? value : await fn(oldValue, value);
    if (newValue == null) {
      await this.removeKey(key);
      return null;
    }
    await this.put(key, newValue);
    return newValue;
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  /** whether the map contains the key with the value, like the check of java.util.Map#remove(Object, Object) */
  protected async containsMapping(key: any, value: any): Promise<boolean> {
    const current = await this.get(key);
    if (!(await JavaUtilUtils.equals(current, value))) {
      return false;
    }
    return current != null || await this.containsKey(key);
  }

  /** Java-compatible hash of a key, value or entry, mirroring Boolean/Number/String.hashCode() conventions */
  static async __hash(e: any): Promise<number> {
    if (e == null) return 0;
    if (typeof e === 'boolean') return e ? 1231 : 1237;
    if (typeof e === 'number') return e | 0;
    if (Array.isArray(e) && e.length === 2) { // Map.Entry
      return (await AbstractAsyncMap.__hash(e[0])) ^ (await AbstractAsyncMap.__hash(e[1]));
    }
    if (typeof e.hashCode === 'function') return (await e.hashCode()) | 0;
    const s = String(e);
    let h = 0;
    for (let i = 0; i < s.length; i++) {
      h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    }
    return h;
  }
}
