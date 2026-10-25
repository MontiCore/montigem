import {IUMLPMap} from "../common/IUMLPMap";
import {AbstractAsyncMap} from "./AbstractAsyncMap";
import {AsyncSet} from "./AsyncSet";
import {AsyncStream} from "./AsyncStream";
import {Collection} from "./Collection";
import {JavaUtilUtils} from "./JavaUtilUtils";
import {MapEntryHelper} from "./MapEntryHelper";

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

  async keySet(): Promise<AsyncSet<K>> {
    return AsyncSet.__viewOf(new AsyncMapView<K, V, K>(this, "keys"));
  }

  async values(): Promise<Collection<V>> {
    return new AsyncMapView<K, V, V>(this, "values");
  }

  async entrySet(): Promise<AsyncSet<[K, V]>> {
    return AsyncSet.__viewOf(new AsyncMapView<K, V, [K, V]>(this, "entries"));
  }

  __internalMap(): Map<K, V> {
    return this._data;
  }
}

/**
 * Live view (keySet, values, entrySet) of an AsyncMap, like the views of the GemMaps (see GemMapEntrySet):
 * - reading through the view always reflects the current content of the map
 * - removing through the view removes from the map, adding is not supported
 * - iteration is weakly consistent: it iterates the keys present when it started and skips the ones removed in the meantime
 * - entries are [key, value] tuples backed by the map, see MapEntryHelper#setValue
 */
class AsyncMapView<K, V, E> implements Collection<E> {

  constructor(/*private readonly*/ readonly _map: AsyncMap<K, V>,
              /*private readonly*/ readonly _kind: "keys" | "values" | "entries") {
  }

  // ─── AsyncIterable implementation ─────────────────────────────────────────

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    for (const key of Array.from(this._map._data.keys())) {
      if (this._map._data.has(key)) {
        yield this._element(key);
      }
    }
  }

  // ─── Reading ──────────────────────────────────────────────────────────────

  async size(): Promise<number> {
    return this._map.size();
  }

  async isEmpty(): Promise<boolean> {
    return this._map.isEmpty();
  }

  async contains(o: any): Promise<boolean> {
    switch (this._kind) {
      case "keys":
        return this._map.containsKey(o);
      case "values":
        return this._map.containsValue(o);
      case "entries":
        return Array.isArray(o) && o.length === 2 && await this._map.containsKey(o[0])
          && await JavaUtilUtils.equals(await this._map.get(o[0]), o[1]);
    }
  }

  async containsAll(c: Collection<any>): Promise<boolean> {
    for await (const o of c) {
      if (!(await this.contains(o))) {
        return false;
      }
    }
    return true;
  }

  /** Set equality for keySet and entrySet; values() is only equal to itself, like in java.util.HashMap */
  async equals(o: any): Promise<boolean> {
    if (o === this) {
      return true;
    }
    if (this._kind === "values" || o == null || typeof o.size !== "function" || typeof o[Symbol.asyncIterator] !== "function") {
      return false;
    }
    return (await o.size()) === (await this.size()) && this.containsAll(o);
  }

  async hashCode(): Promise<number> {
    let hash = 0;
    for await (const e of this) {
      hash = (hash + (await AbstractAsyncMap.__hash(e))) | 0;
    }
    return hash;
  }

  async forEach(action: (e: E) => void | Promise<void>): Promise<void> {
    for await (const e of this) {
      await action(e);
    }
  }

  stream(): AsyncStream<E> {
    return AsyncStream.of(this);
  }

  async toArrayDeepSync(startIndex?: number, endIndex?: number): Promise<Array<any>> {
    const result: E[] = [];
    for await (const e of this) {
      result.push(e);
    }
    return result.slice(startIndex ?? 0, endIndex ?? result.length);
  }

  // ─── Removing (from the map) ──────────────────────────────────────────────

  async removeElement(o: any): Promise<boolean> {
    switch (this._kind) {
      case "keys":
        if (!(await this._map.containsKey(o))) {
          return false;
        }
        await this._map.remove(o);
        return true;
      case "values":
        // the first key with the value
        return this._removeWhere(async (key) => JavaUtilUtils.equals(this._map._data.get(key), o), true);
      case "entries":
        if (!(await this.contains(o))) {
          return false;
        }
        await this._map.remove(o[0]);
        return true;
    }
  }

  async removeAll(c: Collection<any>): Promise<boolean> {
    const others = await this._toArray(c);
    return this.removeIf(async (e) => this._containedIn(others, e));
  }

  async retainAll(c: Collection<any>): Promise<boolean> {
    const others = await this._toArray(c);
    return this.removeIf(async (e) => !(await this._containedIn(others, e)));
  }

  /** java.util.Collection#removeIf(Predicate<E>) → boolean */
  async removeIf(predicate: (e: E) => boolean | Promise<boolean>): Promise<boolean> {
    return this._removeWhere(async (key) => predicate(this._element(key)), false);
  }

  async clear(): Promise<void> {
    return this._map.clear();
  }

  // ─── Unsupported ──────────────────────────────────────────────────────────

  /** @throws UnsupportedOperationException — a map view does not support adding */
  add(_indexOrElement: number | E, _element?: E): Promise<boolean> {
    throw JavaUtilUtils.unsupportedOperation("add() is not supported by the views of a Map. Use Map#put() instead.");
  }

  /** @throws UnsupportedOperationException — a map view does not support adding */
  addAll(_idxOrElems: number | Collection<E> | Array<E>, _elems?: Collection<E>): Promise<boolean> {
    throw JavaUtilUtils.unsupportedOperation("addAll() is not supported by the views of a Map. Use Map#put() instead.");
  }

  /** @throws — the views of a map have no index-based remove */
  removeIndex(_index: number): Promise<E> {
    throw new Error('removeIndex() is not supported by the views of a Map. Use removeElement() instead.');
  }

  /** @throws — use [Symbol.asyncIterator]() instead */
  iterator(): AsyncIterator<E> {
    throw new Error('iterator() returns Iterator<E>. Use [Symbol.asyncIterator]() instead.');
  }

  /** @throws — the views of a map have no listIterator */
  listIterator(_index?: number): AsyncIterator<E> {
    throw new Error('listIterator() is not supported by the views of a Map. Use [Symbol.asyncIterator]() instead.');
  }

  /** @throws — returns Spliterator<E>, not supported */
  spliterator(): never {
    throw new Error('spliterator() returns Spliterator<E>. Not supported.');
  }

  /** @throws — returns Stream<E>, not supported */
  parallelStream(): never {
    throw new Error('parallelStream() returns Stream<E>. Not supported.');
  }

  /** @throws — returns Object[], not supported */
  toArray<T>(_args?: any): never {
    throw new Error('toArray() not supported use GemListUtil#toArraySync if you need JavaScript arrays.');
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  /*private*/ _element(key: K): E {
    switch (this._kind) {
      case "keys":
        return key as unknown as E;
      case "values":
        return this._map._data.get(key) as unknown as E;
      case "entries":
        return MapEntryHelper.__backedBy<K, V>([key, this._map._data.get(key)], this._map) as unknown as E;
    }
  }

  /** removes the entries whose keys match, from the keys present at the start; only the first one if firstOnly */
  /*private*/ async _removeWhere(matches: (key: K) => Promise<boolean>, firstOnly: boolean): Promise<boolean> {
    let changed = false;
    for (const key of Array.from(this._map._data.keys())) {
      if (this._map._data.has(key) && await matches(key)) {
        await this._map.remove(key);
        changed = true;
        if (firstOnly) {
          break;
        }
      }
    }
    return changed;
  }

  /*private*/ async _toArray(c: Collection<any>): Promise<any[]> {
    const result: any[] = [];
    for await (const o of c) {
      result.push(o);
    }
    return result;
  }

  /*private*/ async _containedIn(others: any[], e: E): Promise<boolean> {
    for (const o of others) {
      if (await JavaUtilUtils.equals(o, e)) {
        return true;
      }
    }
    return false;
  }
}
