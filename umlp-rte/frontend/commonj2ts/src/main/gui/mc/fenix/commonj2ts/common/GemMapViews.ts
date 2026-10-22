/* (c) https://github.com/MontiCore/monticore */
import {GemIdentifiable} from "./GemIdentifiable";
import {AsyncSet} from "../javautil/AsyncSet";
import {AsyncStream} from "../javautil/AsyncStream";
import {Collection} from "../javautil/Collection";
import {JavaUtilUtils} from "../javautil/JavaUtilUtils";
import {AbstractAsyncMap} from "../javautil/AbstractAsyncMap";

/**
 * Live views (keySet, values, entrySet) of a GemValueMap or GemEntryMap, like the backend GemMapViews:
 * - reading through a view always reflects the current content of the map
 * - removing through a view removes from the map, including synchronization;
 *   adding is not supported, as keys are derived from the values
 * - iteration is weakly consistent: it iterates the keys present when it started and skips the ones
 *   removed in the meantime; values (and key objects of GemEntryMaps) are loaded in one batch per iteration
 * - entries are [key, value] tuples that are not backed by the map: Map.Entry#setValue is not supported
 *
 * I: the type the map stores its keys as: the key itself (GemValueMap) or its gemId (GemEntryMap)
 */
export abstract class GemMapViews<I, K, V extends GemIdentifiable> {

  /** the stored keys, after deriving pending keys */
  abstract currentKeys(): Promise<Array<I>>;

  /** the gemId of the value stored for the stored key, or null */
  abstract lookupID(storedKey: I): number;

  /** the stored key of a key given to a view method, or null if it cannot be a key */
  abstract toStoredKey(key: any): I;

  /** the key objects of the stored keys (loaded in one batch for GemEntryMaps) */
  abstract keyObjects(storedKeys: Array<I>): Promise<Map<I, K>>;

  /** the values with the gemIds (loaded in one batch) */
  abstract valueObjects(gemIDs: Array<number>): Promise<Map<number, V>>;

  /** removes the entry holding the value with gemID, including synchronization */
  abstract removeValueID(gemID: number): Promise<void>;

  abstract containsValueID(gemID: number): Promise<boolean>;

  abstract size(): Promise<number>;

  abstract clear(): Promise<void>;

  containsStoredKey(storedKey: I): boolean {
    return this.lookupID(storedKey) != null;
  }

  keySet(): AsyncSet<K> {
    return AsyncSet.__viewOf(new GemMapView<I, K, V, K>(this, "keys"));
  }

  values(): Collection<V> {
    return new GemMapView<I, K, V, V>(this, "values");
  }

  entrySet(): AsyncSet<[K, V]> {
    return AsyncSet.__viewOf(new GemMapView<I, K, V, [K, V]>(this, "entries"));
  }

  /** the gemId of the value if o is an entry contained in the map, otherwise null */
  gemIDOfEntry(o: any): number {
    if (!Array.isArray(o) || o.length !== 2 || o[1] == null || typeof o[1].getGemId !== "function") {
      return null;
    }
    const storedKey: I = this.toStoredKey(o[0]);
    const gemID: number = storedKey == null ? null : this.lookupID(storedKey);
    return gemID != null && gemID === o[1].getGemId() ? gemID : null;
  }
}

/**
 * one view (keySet, values or entrySet) of GemMapViews
 */
class GemMapView<I, K, V extends GemIdentifiable, E> implements Collection<E> {

  constructor(/*private readonly*/ readonly _views: GemMapViews<I, K, V>,
              /*private readonly*/ readonly _kind: "keys" | "values" | "entries") {
  }

  // ─── AsyncIterable implementation ─────────────────────────────────────────

  /** weakly consistent: the keys present at the start, skipping the ones removed in the meantime */
  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    const keys: Array<I> = [...await this._views.currentKeys()];
    // loaded on first use, in one batch
    let values: Map<number, V> = null;
    let keyObjects: Map<I, K> = null;
    for (let idx = 0; idx < keys.length; idx++) {
      const storedKey = keys[idx];
      const gemID = this._views.lookupID(storedKey);
      if (gemID == null) {
        continue;
      }
      let key: K = null;
      if (this._kind !== "values") {
        if (keyObjects == null) {
          keyObjects = new Map(await this._views.keyObjects(keys.slice(idx)));
        }
        if (!keyObjects.has(storedKey)) {
          (await this._views.keyObjects([storedKey])).forEach((k, i) => keyObjects.set(i, k));
        }
        key = keyObjects.get(storedKey);
      }
      let value: V = null;
      if (this._kind !== "keys") {
        if (values == null) {
          // all values of the remaining keys in one batch
          const gemIDs: Array<number> = [];
          for (let i = idx; i < keys.length; i++) {
            const id = this._views.lookupID(keys[i]);
            if (id != null) {
              gemIDs.push(id);
            }
          }
          values = new Map(await this._views.valueObjects(gemIDs));
        }
        if (!values.has(gemID)) {
          // rekeyed or replaced after the batch was loaded
          (await this._views.valueObjects([gemID])).forEach((v, id) => values.set(id, v));
        }
        value = values.get(gemID);
      }
      switch (this._kind) {
        case "keys":
          yield key as unknown as E;
          break;
        case "values":
          yield value as unknown as E;
          break;
        case "entries":
          yield [key, value] as unknown as E;
          break;
      }
    }
  }

  // ─── Reading ──────────────────────────────────────────────────────────────

  async size(): Promise<number> {
    return this._views.size();
  }

  async isEmpty(): Promise<boolean> {
    return (await this.size()) === 0;
  }

  async contains(o: any): Promise<boolean> {
    switch (this._kind) {
      case "keys": {
        const storedKey: I = this._views.toStoredKey(o);
        return storedKey != null && this._views.containsStoredKey(storedKey);
      }
      case "values":
        return o != null && typeof o.getGemId === "function" && this._views.containsValueID(o.getGemId());
      case "entries":
        return this._views.gemIDOfEntry(o) != null;
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

  /** Set equality for keySet and entrySet; values() is only equal to itself, like in java.util.AbstractCollection */
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

  // ─── Removing (from the map, including synchronization) ───────────────────

  async removeElement(o: any): Promise<boolean> {
    let gemID: number = null;
    switch (this._kind) {
      case "keys": {
        const storedKey: I = this._views.toStoredKey(o);
        gemID = storedKey == null ? null : this._views.lookupID(storedKey);
        break;
      }
      case "values":
        gemID = (await this.contains(o)) ? o.getGemId() : null;
        break;
      case "entries":
        gemID = this._views.gemIDOfEntry(o);
        break;
    }
    if (gemID == null) {
      return false;
    }
    await this._views.removeValueID(gemID);
    return true;
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
    const toRemove: Array<any> = [];
    for await (const e of this) {
      if (await predicate(e)) {
        toRemove.push(e);
      }
    }
    let changed = false;
    for (const e of toRemove) {
      changed = (await this.removeElement(e)) || changed;
    }
    return changed;
  }

  async clear(): Promise<void> {
    return this._views.clear();
  }

  // ─── Unsupported ──────────────────────────────────────────────────────────

  /** @throws UnsupportedOperationException — the keys of a GemMap are derived from its values */
  add(_indexOrElement: number | E, _element?: E): Promise<boolean> {
    throw JavaUtilUtils.unsupportedOperation("add() is not supported by the views of a GemMap. Use Map#put() instead.");
  }

  /** @throws UnsupportedOperationException — the keys of a GemMap are derived from its values */
  addAll(_idxOrElems: number | Collection<E> | Array<E>, _elems?: Collection<E>): Promise<boolean> {
    throw JavaUtilUtils.unsupportedOperation("addAll() is not supported by the views of a GemMap. Use Map#put() instead.");
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
