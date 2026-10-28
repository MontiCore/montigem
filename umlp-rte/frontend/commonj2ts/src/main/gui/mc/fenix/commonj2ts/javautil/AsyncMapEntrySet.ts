/* (c) https://github.com/MontiCore/monticore */
import {AsyncSet} from "./AsyncSet";
import {Collection} from "./Collection";
import {JavaUtilUtils} from "./JavaUtilUtils";
import {MapEntryHelper} from "./MapEntryHelper";
import {AbstractMapCollection} from "./AbstractMapCollection";
import {AsyncMap} from "./AsyncMap";

/**
 * Live entry set of an AsyncMap, like the GemMapEntrySet of the GemMaps. The keySet and values of the map are
 * projections of it (see keySet(), values()):
 * - reading always reflects the current content of the map
 * - removing (also through the projections) removes from the map, adding is not supported
 * - iteration is weakly consistent: it iterates the keys present when it started and skips the ones removed in the meantime
 * - entries are [key, value] tuples backed by the map, see MapEntryHelper#setValue
 */
export class AsyncMapEntrySet<K, V> extends AbstractMapCollection<[K, V]> {

  constructor(/*private readonly*/ readonly _map: AsyncMap<K, V>) {
    super();
  }

  async* [Symbol.asyncIterator](): AsyncIterator<[K, V]> {
    for (const key of this.__keys()) {
      yield this.__entry(key);
    }
  }

  async size(): Promise<number> {
    return this._map.size();
  }

  async isEmpty(): Promise<boolean> {
    return this._map.isEmpty();
  }

  async contains(o: any): Promise<boolean> {
    return Array.isArray(o) && o.length === 2 && await this._map.containsKey(o[0])
      && await JavaUtilUtils.equals(await this._map.get(o[0]), o[1]);
  }

  async removeElement(o: any): Promise<boolean> {
    if (!(await this.contains(o))) {
      return false;
    }
    await this._map.remove(o[0]);
    return true;
  }

  /** java.util.Collection#removeIf(Predicate<E>) → boolean */
  async removeIf(predicate: (e: [K, V]) => boolean | Promise<boolean>): Promise<boolean> {
    return this.__removeWhere(async (key) => predicate(this.__entry(key)), false);
  }

  async clear(): Promise<void> {
    return this._map.clear();
  }

  /** the keys of the map: a projection of the entries */
  keySet(): AsyncSet<K> {
    return AsyncSet.__viewOf(new AsyncMapEntrySetProjection<K, V, K>(this, "keys"));
  }

  /** the values of the map: a projection of the entries */
  values(): Collection<V> {
    return new AsyncMapEntrySetProjection<K, V, V>(this, "values");
  }

  /** the keys present at the start, skipping the ones removed in the meantime (weakly consistent) */
  * __keys(): Iterable<K> {
    for (const key of Array.from(this._map._data.keys())) {
      if (this._map._data.has(key)) {
        yield key;
      }
    }
  }

  /** the entry of the key, backed by the map */
  __entry(key: K): [K, V] {
    return MapEntryHelper.__backedBy<K, V>([key, this._map._data.get(key)], this._map);
  }

  /** removes the entries whose keys match, from the keys present at the start; only the first one if firstOnly */
  async __removeWhere(matches: (key: K) => Promise<boolean>, firstOnly: boolean): Promise<boolean> {
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
}

/**
 * keySet or values of an AsyncMap: a projection of its AsyncMapEntrySet
 */
class AsyncMapEntrySetProjection<K, V, E> extends AbstractMapCollection<E> {

  constructor(/*private readonly*/ readonly _entries: AsyncMapEntrySet<K, V>,
              /*private readonly*/ readonly _kind: "keys" | "values") {
    super();
  }

  isSet(): boolean {
    return this._kind === "keys";
  }

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    for (const key of this._entries.__keys()) {
      yield this._element(key);
    }
  }

  async size(): Promise<number> {
    return this._entries.size();
  }

  async isEmpty(): Promise<boolean> {
    return this._entries.isEmpty();
  }

  async contains(o: any): Promise<boolean> {
    return this._kind === "keys" ? this._entries._map.containsKey(o) : this._entries._map.containsValue(o);
  }

  async removeElement(o: any): Promise<boolean> {
    if (this._kind === "keys") {
      if (!(await this._entries._map.containsKey(o))) {
        return false;
      }
      await this._entries._map.remove(o);
      return true;
    }
    // the first key with the value
    return this._entries.__removeWhere(async (key) => JavaUtilUtils.equals(this._entries._map._data.get(key), o), true);
  }

  /** java.util.Collection#removeIf(Predicate<E>) → boolean */
  async removeIf(predicate: (e: E) => boolean | Promise<boolean>): Promise<boolean> {
    return this._entries.__removeWhere(async (key) => predicate(this._element(key)), false);
  }

  async clear(): Promise<void> {
    return this._entries.clear();
  }

  /*private*/ _element(key: K): E {
    return (this._kind === "keys" ? key : this._entries._map._data.get(key)) as unknown as E;
  }
}
