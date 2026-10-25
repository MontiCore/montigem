/* (c) https://github.com/MontiCore/monticore */
import {GemIdentifiable} from "./GemIdentifiable";
import {AsyncSet} from "../javautil/AsyncSet";
import {AsyncStream} from "../javautil/AsyncStream";
import {Collection} from "../javautil/Collection";
import {JavaUtilUtils} from "../javautil/JavaUtilUtils";
import {AbstractAsyncMap} from "../javautil/AbstractAsyncMap";

/**
 * Collection methods shared by the entry set and its projections
 */
abstract class GemMapCollection<E> implements Collection<E> {

  abstract [Symbol.asyncIterator](): AsyncIterator<E>;

  abstract size(): Promise<number>;

  abstract isEmpty(): Promise<boolean>;

  abstract contains(o: any): Promise<boolean>;

  abstract removeElement(o: any): Promise<boolean>;

  abstract clear(): Promise<void>;

  /** whether this is a set (keySet, entrySet), used by equals */
  isSet(): boolean {
    return true;
  }

  // ─── Reading ──────────────────────────────────────────────────────────────

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
    if (!this.isSet() || o == null || typeof o.size !== "function" || typeof o[Symbol.asyncIterator] !== "function") {
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

/**
 * Live entry set of a GemValueMap or GemEntryMap, like the backend GemMapEntrySet. The keySet and values of the map
 * are projections of it (see keySet(), values()):
 * - reading always reflects the current content of the map
 * - removing (also through the projections) removes from the map, including synchronization, by gemId;
 *   adding is not supported, as keys are derived from the values
 * - iteration is weakly consistent: it iterates the keys present when it started and skips the ones
 *   removed in the meantime
 * - entries are [key, value] tuples, so an iterated entry has its value loaded: values (and key objects of
 *   GemEntryMaps) are loaded in one batch per iteration; the keySet projection loads no values.
 *   Entries are not backed by the map: Map.Entry#setValue is not supported
 *
 * I: the type the map stores its keys as: the key itself (GemValueMap) or its gemId (GemEntryMap)
 */
export abstract class GemMapEntrySet<I, K, V extends GemIdentifiable> extends GemMapCollection<[K, V]> {

  /** the map this is the entry set of */
  abstract map(): AbstractAsyncMap<K, V>;

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

  abstract containsValueID(gemID: number): Promise<boolean>;

  /** removes the entry holding the value with gemID, including synchronization */
  abstract removeValueID(gemID: number): Promise<void>;

  async* [Symbol.asyncIterator](): AsyncIterator<[K, V]> {
    for await (const e of this.__entries(true, true)) {
      yield [e.key, e.value];
    }
  }

  async size(): Promise<number> {
    return this.map().size();
  }

  async isEmpty(): Promise<boolean> {
    return this.map().isEmpty();
  }

  async contains(o: any): Promise<boolean> {
    return (await this.gemIDOfEntry(o)) != null;
  }

  async removeElement(o: any): Promise<boolean> {
    const gemID = await this.gemIDOfEntry(o);
    if (gemID == null) {
      return false;
    }
    await this.removeValueID(gemID);
    return true;
  }

  async clear(): Promise<void> {
    return this.map().clear();
  }

  /** the keys of the map: a projection of the entries that loads no values */
  keySet(): AsyncSet<K> {
    return AsyncSet.__viewOf(new GemMapEntrySetProjection<I, K, V, K>(this, "keys"));
  }

  /** the values of the map: a projection of the entries; contains and remove work by gemId */
  values(): Collection<V> {
    return new GemMapEntrySetProjection<I, K, V, V>(this, "values");
  }

  /** the gemId of the value stored for the key, or null */
  async gemIDOfKey(key: any): Promise<number> {
    // derive pending keys first, a loaded map knows its values by id only
    await this.currentKeys();
    const storedKey: I = this.toStoredKey(key);
    return storedKey == null ? null : this.lookupID(storedKey);
  }

  /** the gemId of the value if o is an entry contained in the map, otherwise null */
  async gemIDOfEntry(o: any): Promise<number> {
    if (!Array.isArray(o) || o.length !== 2 || o[1] == null || typeof o[1].getGemId !== "function") {
      return null;
    }
    const gemID = await this.gemIDOfKey(o[0]);
    return gemID != null && gemID === o[1].getGemId() ? gemID : null;
  }

  /**
   * weakly consistent iteration over the keys present at the start, skipping the ones removed in the meantime;
   * loads the key objects (withKeys) and the values (withValues) of the remaining entries in one batch on first use
   */
  async* __entries(withKeys: boolean, withValues: boolean): AsyncGenerator<{ key: K, value: V }> {
    const keys: Array<I> = [...await this.currentKeys()];
    // loaded on first use, in one batch
    let values: Map<number, V> = null;
    let keyObjects: Map<I, K> = null;
    for (let idx = 0; idx < keys.length; idx++) {
      const storedKey = keys[idx];
      const gemID = this.lookupID(storedKey);
      if (gemID == null) {
        continue;
      }
      let key: K = null;
      if (withKeys) {
        if (keyObjects == null) {
          keyObjects = new Map(await this.keyObjects(keys.slice(idx)));
        }
        if (!keyObjects.has(storedKey)) {
          (await this.keyObjects([storedKey])).forEach((k, i) => keyObjects.set(i, k));
        }
        key = keyObjects.get(storedKey);
      }
      let value: V = null;
      if (withValues) {
        if (values == null) {
          // all values of the remaining keys in one batch
          const gemIDs: Array<number> = [];
          for (let i = idx; i < keys.length; i++) {
            const id = this.lookupID(keys[i]);
            if (id != null) {
              gemIDs.push(id);
            }
          }
          values = new Map(await this.valueObjects(gemIDs));
        }
        if (!values.has(gemID)) {
          // rekeyed or replaced after the batch was loaded
          (await this.valueObjects([gemID])).forEach((v, id) => values.set(id, v));
        }
        value = values.get(gemID);
      }
      yield {key, value};
    }
  }
}

/**
 * keySet or values of a GemMap: a projection of its GemMapEntrySet
 */
class GemMapEntrySetProjection<I, K, V extends GemIdentifiable, E> extends GemMapCollection<E> {

  constructor(/*private readonly*/ readonly _entries: GemMapEntrySet<I, K, V>,
              /*private readonly*/ readonly _kind: "keys" | "values") {
    super();
  }

  isSet(): boolean {
    return this._kind === "keys";
  }

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    const keys = this._kind === "keys";
    for await (const e of this._entries.__entries(keys, !keys)) {
      yield (keys ? e.key : e.value) as unknown as E;
    }
  }

  async size(): Promise<number> {
    return this._entries.size();
  }

  async isEmpty(): Promise<boolean> {
    return this._entries.isEmpty();
  }

  async contains(o: any): Promise<boolean> {
    if (this._kind === "keys") {
      return (await this._entries.gemIDOfKey(o)) != null;
    }
    return o != null && typeof o.getGemId === "function" && this._entries.containsValueID(o.getGemId());
  }

  async removeElement(o: any): Promise<boolean> {
    let gemID: number = null;
    if (this._kind === "keys") {
      gemID = await this._entries.gemIDOfKey(o);
    } else if (await this.contains(o)) {
      gemID = o.getGemId();
    }
    if (gemID == null) {
      return false;
    }
    await this._entries.removeValueID(gemID);
    return true;
  }

  async clear(): Promise<void> {
    return this._entries.clear();
  }
}
