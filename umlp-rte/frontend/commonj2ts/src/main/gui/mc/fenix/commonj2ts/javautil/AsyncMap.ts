import {IUMLPMap} from "../common/IUMLPMap";
import {AbstractAsyncMap} from "./AbstractAsyncMap";
import {AsyncSet} from "./AsyncSet";
import {AsyncMapEntrySet} from "./AsyncMapEntrySet";
import {Collection} from "./Collection";

/**
 * java.util.HashMap, LinkedHashMap, TreeMap, ... in transpiled code: wraps a native Map, i.e. keeps the
 * insertion order and compares keys like ===.
 */
export class AsyncMap<K, V> extends AbstractAsyncMap<K, V> {
  /*private readonly*/
  _data: Map<K, V>;

  constructor() {
    super();
  }

  /**
   * new HashMap<>(), new HashMap<>(int), new HashMap<>(int, float): an empty map
   * new HashMap<>(Map): a copy of the map
   *
   * A native Map is wrapped, not copied (like AsyncList wraps an array), an array of [key, value] tuples is copied.
   */
  async __AsyncMapConstructor(data?: number | Map<K, V> | IUMLPMap<K, V> | Array<[K, V]>, _loadFactor?: number): Promise<AsyncMap<K, V>> {
    if (data instanceof Map) {
      this._data = data;
      return this;
    }
    this._data = new Map<K, V>();
    if (Array.isArray(data)) {
      for (const [k, v] of data) {
        this._data.set(k, v);
      }
    } else if (data != null && typeof (data as any).entrySet === "function") {
      await this.putAll(data as IUMLPMap<K, V>);
    }
    return this;
  }

  /** A live view of a native Map, created synchronously (e.g. for BaseClassObjectMapper#getBaseDeSers) */
  static __viewOf<K, V>(data: Map<K, V>): AsyncMap<K, V> {
    const view = new AsyncMap<K, V>();
    view._data = data;
    return view;
  }

  public toString(): string {
    return "{" + Array.from(this._data.entries()).map(([k, v]) => String(k) + "=" + String(v)).join(", ") + "}";
  }

  // ─── Core methods ─────────────────────────────────────────────────────────

  async size(): Promise<number> {
    return this._data.size;
  }

  async containsKey(key: any): Promise<boolean> {
    return this._data.has(key);
  }

  async get(key: any): Promise<V> {
    return this._data.has(key) ? this._data.get(key) : null;
  }

  async put(key: K, value: V): Promise<V> {
    const oldValue = this._data.has(key) ? this._data.get(key) : null;
    this._data.set(key, value);
    return oldValue;
  }

  protected async removeKey(key: any): Promise<V> {
    if (!this._data.has(key)) {
      return null;
    }
    const oldValue = this._data.get(key);
    this._data.delete(key);
    return oldValue;
  }

  async clear(): Promise<void> {
    this._data.clear();
  }

  /** live view, a projection of entrySet(), see AsyncMapEntrySet */
  async keySet(): Promise<AsyncSet<K>> {
    return new AsyncMapEntrySet<K, V>(this).keySet();
  }

  /** live view, a projection of entrySet(), see AsyncMapEntrySet */
  async values(): Promise<Collection<V>> {
    return new AsyncMapEntrySet<K, V>(this).values();
  }

  /** live view, see AsyncMapEntrySet (created per call: no field, so the map holds no cyclic reference) */
  async entrySet(): Promise<AsyncSet<[K, V]>> {
    return AsyncSet.__viewOf(new AsyncMapEntrySet<K, V>(this));
  }

  __internalMap(): Map<K, V> {
    return this._data;
  }
}
