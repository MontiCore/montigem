/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {GemIdentifiable} from "./GemIdentifiable";
import {GemBuildable} from "./GemBuildable";
import {LoadResult} from "./LoadResult";
import {LoadingState} from "./LoadingState";
import {IUMLPList} from "./IUMLPList";
import {IUMLPMap} from "./IUMLPMap";
import {GemMapEntrySet} from "./GemMapEntrySet";
import {AbstractAsyncMap} from "../javautil/AbstractAsyncMap";
import {AsyncList} from "../javautil/AsyncList";
import {AsyncMap} from "../javautil/AsyncMap";
import {AsyncSet} from "../javautil/AsyncSet";
import {Collection} from "../javautil/Collection";
import {JavaUtilUtils} from "../javautil/JavaUtilUtils";

/*
 * A lazy Map for Keys/Values
 * Elements are loaded as they are required.
 * Useful if e.g. the elements are loaded over a network.
 *
 * The keys are values (numbers, Strings), the values are domain objects. Keys are compared like ===.
 *
 * Attribute qualified associations derive the key of a value from the value itself
 * (see {@link #deriveKey}). Values added by id only (e.g. when loading) are keyed
 * on first access, which loads them in one batch.
 */
export abstract class GemValueMap<K, V extends GemIdentifiable> extends AbstractAsyncMap<K, V> implements GemBuildable {

  // local storage of the gemIds of the contained objects: all ids of the qualified association
  protected idMap: Map<K, number> = new Map();

  // ids of values whose key is not known yet, keyed by ensureKeyed()
  protected unkeyedValueIDs: Array<number> = [];

  // the running ensureKeyed(), so that concurrent calls wait for its keys
  protected keying: Promise<void> = null;

  // live entry set; keySet and values are projections of it
  protected entries: GemMapEntrySet<K, K, V> = new GemValueMapEntrySet<K, V>(this);

  public constructor() {
    super();
  }

  public async __GemValueMapConstructor(idMap?: IUMLPMap<K, number> | Map<K, number>): Promise<GemValueMap<K, V>> {
    this.idMap = new Map();
    this.unkeyedValueIDs = [];
    if (idMap instanceof Map) {
      idMap.forEach((id, key) => this.idMap.set(key, id));
    } else if (idMap != null) {
      for await (const [key, id] of await idMap.entrySet()) {
        this.idMap.set(key, id);
      }
    }
    return this;
  }

  /* Default method injected from umlp.backendrte.common.GemBuildable */
  isBuildFinished(): boolean {
    return true;
  }

  public async build(): Promise<GemValueMap<K, V>> {
    return this;
  }

  async size(): Promise<number> {
    await this.ensureKeyed();
    return this.idMap.size;
  }

  async isEmpty(): Promise<boolean> {
    return this.idMap.size === 0 && this.unkeyedValueIDs.length === 0;
  }

  public async getValue(gemID: number): Promise<V> {
    const lr: LoadResult<V> = await this.loadSingleValueObject(gemID);
    if (lr.isPresent()) {
      return lr.get();
    } else {
      Log.error("Can not load Object with id " + gemID);
      return null;
    }
  }

  /** live view, a projection of entrySet(), see GemMapEntrySet */
  async keySet(): Promise<AsyncSet<K>> {
    return this.entries.keySet();
  }

  /** live view, a projection of entrySet(), see GemMapEntrySet */
  async values(): Promise<Collection<V>> {
    return this.entries.values();
  }

  /** live view, see GemMapEntrySet */
  async entrySet(): Promise<AsyncSet<[K, V]>> {
    return AsyncSet.__viewOf(this.entries);
  }

  /**
   * @return the values with the gemIDs, loaded in one batch
   */
  async loadValuesByID(gemIDs: Array<number>): Promise<Map<number, V>> {
    const result: Map<number, V> = new Map();
    if (gemIDs.length === 0) {
      return result;
    }
    const loaded: IUMLPList<LoadResult<V>> = await this.loadValueObjects(await new AsyncList<number>().__AsyncListConstructor([...gemIDs]));
    const size = await loaded.size();
    for (let i = 0; i < size; i++) {
      const lr: LoadResult<V> = await loaded.get(i);
      if (lr.isPresent()) {
        result.set(gemIDs[i], lr.get());
      } else {
        Log.error("Can not load Object with id " + gemIDs[i]);
      }
    }
    return result;
  }

  async containsKey(key: any): Promise<boolean> {
    await this.ensureKeyed();
    return this.idMap.has(key);
  }

  async containsValue(value: any): Promise<boolean> {
    if (value != null && typeof value.getGemId === "function") {
      return this.containsValueID(value.getGemId());
    } else {
      Log.error("Value is not GemIdentifiable");
    }
    return false;
  }

  /**
   * identical to {@link Map#containsValue(Object)} but taking a gemID
   *
   * @param gemID the id of the object that may be contained in this Map
   * @return whether the object corresponding to gemID is contained in this Map
   */
  public async containsValueID(gemID: number): Promise<boolean> {
    return Array.from(this.idMap.values()).includes(gemID) || this.unkeyedValueIDs.includes(gemID);
  }

  /**
   * @return the gemID of the value stored for key, or null
   */
  public async getID(key: any): Promise<number> {
    await this.ensureKeyed();
    return this.lookupID(key);
  }

  /**
   * provides the value objects referenced by the gemIDs if possible
   *
   * @param gemIDs reference the value objects to get
   * @return the results of trying to load the objects
   */
  abstract loadValueObjects(gemIDs: IUMLPList<number>): Promise<IUMLPList<LoadResult<V>>>;

  async put(key: K, value: V): Promise<V> {
    if (this.isKeyDerived()) {
      // the key of a value is derived from the value, it cannot be chosen
      const derived: K = await this.deriveKey(value);
      if (key !== derived) {
        Log.error("0xA7305 Key " + key + " does not match the key " + derived
          + " derived from the object with id " + value.getGemId());
        return null;
      }
      const oldElem: V = await this.get(key);
      await this._internal_putValueAndSync(value);
      return oldElem;
    }
    const oldElem: V = await this.get(key);
    await this._internal_putAndSync(key, value.getGemId());
    return oldElem;
  }

  async get(key: any): Promise<V> {
    const valGemID: number = await this.getID(key);
    return valGemID == null ? null : this.getValue(valGemID);
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  async _internal_putAndSync(key: K, valueGemID: number): Promise<void> {
    await this._internal_put(key, valueGemID);
  }

  public async _internal_put(key: K, valueGemID: number): Promise<void> {
    const old: number = this.lookupID(key);
    this.idMap.set(key, valueGemID);
    if (old != null && old !== valueGemID) {
      await this.onValueRemoved(old, null);
    }
  }

  /** java.util.Map#remove(Object) */
  protected async removeKey(key: any): Promise<V> {
    if (!(await this.containsKey(key))) {
      return null;
    }
    const oldElem: V = await this.get(key);
    await this._internal_removeAndSync(key);
    return oldElem;
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  async _internal_removeAndSync(key: K): Promise<void> {
    await this._internal_remove(key);
  }

  public async _internal_remove(key: K): Promise<void> {
    const old: number = this.lookupID(key);
    this.idMap.delete(key);
    if (old != null) {
      await this.onValueRemoved(old, null);
    }
  }

  /**
   * derives the key of a value, only supported by maps of attribute qualified associations
   */
  public async deriveKey(value: V): Promise<K> {
    throw JavaUtilUtils.unsupportedOperation("0xA7301 The keys of " + this.constructor.name + " are not derived from its values");
  }

  /**
   * whether the key of a value is derived from the value (attribute qualified associations),
   * see {@link #deriveKey}; then a value cannot be put under an arbitrary key
   */
  public isKeyDerived(): boolean {
    return false;
  }

  /**
   * puts a value under its derived key;
   * to be overridden by a subclass to add synchronization
   */
  async _internal_putValueAndSync(value: V): Promise<void> {
    await this._internal_putValue(value);
  }

  /**
   * removes the entry holding the value with gemID;
   * to be overridden by a subclass to add synchronization
   */
  async _internal_removeValueIDAndSync(gemID: number): Promise<void> {
    await this._internal_removeValueID(gemID);
  }

  /**
   * stores a value under its derived key, without synchronization
   * (only for maps of attribute qualified associations)
   */
  public async _internal_putValue(value: V): Promise<void> {
    await this._internal_put(await this.deriveKey(value), value.getGemId());
    await this.onValueKeyed(value);
  }

  /**
   * removes the entry holding the value with gemID, without synchronization
   * @return whether such an entry existed
   */
  public async _internal_removeValueID(gemID: number): Promise<boolean> {
    const idx = this.unkeyedValueIDs.indexOf(gemID);
    if (idx >= 0) {
      this.unkeyedValueIDs.splice(idx, 1);
      return true;
    }
    for (const [key, id] of Array.from(this.idMap.entries())) {
      if (id === gemID) {
        this.idMap.delete(key);
        await this.onValueRemoved(gemID, null);
        return true;
      }
    }
    return false;
  }

  /**
   * adds values by their ids only; their keys are derived on first access
   * (only for maps of attribute qualified associations)
   */
  public async _internal_addValueIDs(gemIDs: Collection<number> | Array<number>): Promise<void> {
    if (Array.isArray(gemIDs)) {
      this.unkeyedValueIDs.push(...gemIDs);
    } else {
      for await (const gemID of gemIDs) {
        this.unkeyedValueIDs.push(gemID);
      }
    }
  }

  /**
   * stores values under their keys by id, without loading them, e.g. as received from another participant
   * (keys in insertion order)
   */
  public async _internal_putIDs(ids: IUMLPMap<K, number> | Map<K, number>): Promise<void> {
    if (ids instanceof Map) {
      for (const [key, id] of Array.from(ids.entries())) {
        await this._internal_put(key, id);
      }
    } else {
      for await (const [key, id] of await ids.entrySet()) {
        await this._internal_put(key, id);
      }
    }
  }

  /**
   * derives the keys of all values added by id only, loading them in one batch
   */
  async ensureKeyed(): Promise<void> {
    while (this.keying != null) {
      await this.keying;
    }
    if (this.unkeyedValueIDs.length === 0) {
      return;
    }
    const gemIDs: Array<number> = [...this.unkeyedValueIDs];
    this.unkeyedValueIDs.length = 0;
    this.keying = (async () => {
      const loaded: IUMLPList<LoadResult<V>> = await this.loadValueObjects(await new AsyncList<number>().__AsyncListConstructor(gemIDs));
      const size = await loaded.size();
      for (let i = 0; i < size; i++) {
        const lr: LoadResult<V> = await loaded.get(i);
        if (lr.isPresent()) {
          await this.keyLoadedValue(lr.get());
        } else {
          Log.error("0xA7302 Can not load Object with id " + gemIDs[i] + " to derive its key");
        }
      }
    })();
    try {
      await this.keying;
    } finally {
      this.keying = null;
    }
  }

  async keyLoadedValue(value: V): Promise<void> {
    const key: K = await this.deriveKey(value);
    const present: number = this.lookupID(key);
    if (present != null && present !== value.getGemId()) {
      Log.error("0xA7303 Key " + key + " is not unique: it is derived from the objects with ids "
        + present + " and " + value.getGemId());
      return;
    }
    await this._internal_put(key, value.getGemId());
    await this.onValueKeyed(value);
  }

  /**
   * @return the id stored for key, without deriving pending keys
   */
  lookupID(key: any): number {
    return this.idMap.has(key) ? this.idMap.get(key) : null;
  }

  /**
   * hook, called whenever the key of a value has been derived;
   * used to observe the value to rekey it when the qualifying attribute changes
   */
  async onValueKeyed(value: V): Promise<void> {
  }

  /**
   * hook, called whenever a value leaves the map: removed, or replaced by another value for its key;
   * used to stop observing it
   *
   * @param value the value, if it is at hand (e.g. an unbuilt builder), otherwise null
   */
  async onValueRemoved(valueGemID: number, value: V): Promise<void> {
  }

  /**
   * moves a value from oldKey to newKey, e.g. after its qualifying attribute changed,
   * without synchronization (see _internal_rekeyAndSync)
   *
   * @return whether the value was moved; false if it is not (or no longer) stored under oldKey
   */
  public async _internal_rekey(oldKey: K, newKey: K, valueGemID: number): Promise<boolean> {
    if (this.lookupID(oldKey) !== valueGemID || oldKey === newKey) {
      // not (or no longer) contained under oldKey
      return false;
    }
    if (this.lookupID(newKey) != null) {
      Log.error("0xA7304 Key " + newKey + " is not unique: it is derived from the objects with ids "
        + this.lookupID(newKey) + " and " + valueGemID);
      return false;
    }
    this.moveEntry(oldKey, newKey);
    return true;
  }

  /**
   * moves a value from oldKey to newKey after its qualifying attribute changed (see _internal_rekey);
   * to be overridden by a subclass to add synchronization
   */
  public async _internal_rekeyAndSync(oldKey: K, newKey: K, valueGemID: number): Promise<void> {
    await this._internal_rekey(oldKey, newKey, valueGemID);
  }

  moveEntry(oldKey: K, newKey: K): void {
    const id = this.idMap.get(oldKey);
    this.idMap.delete(oldKey);
    this.idMap.set(newKey, id);
  }

  /**
   * returns if the element is available
   * e.g. one is authorized to access it
   * this is NOT a range check
   */
  public async isPresent(key: any): Promise<boolean> {
    const valGemID: number = await this.getID(key);
    return valGemID != null && (await this.loadSingleValueObject(valGemID)).isPresent();
  }

  public async loadSingleValueObject(id: number): Promise<LoadResult<V>> {
    const res: IUMLPList<LoadResult<V>> = await this.loadValueObjects(await new AsyncList<number>().__AsyncListConstructor([id]));
    if ((await res.size()) == 0) {
      return await <any>(new LoadResult<any>().__LoadResultConstructor(LoadingState.FAILED_TO_LOAD));
    }
    return await res.get(0);
  }

  public async getKeys(): Promise<AsyncSet<K>> {
    return await new AsyncSet<K>().__AsyncSetConstructor(await this.currentKeys());
  }

  /** the keys, after deriving pending keys (see GemMapEntrySet#currentKeys) */
  async currentKeys(): Promise<Array<K>> {
    await this.ensureKeyed();
    return Array.from(this.idMap.keys());
  }

  /**
   * the ids of all values, without deriving keys
   */
  public async getValueIDs(): Promise<IUMLPList<number>> {
    return await new AsyncList<number>().__AsyncListConstructor([...this.idMap.values(), ...this.unkeyedValueIDs]);
  }

  public async getIDMap(): Promise<IUMLPMap<K, number>> {
    await this.ensureKeyed();
    return AsyncMap.__viewOf(this.idMap);
  }

  /**
   * removes all entries, like removing them one by one (including synchronization)
   */
  async clear(): Promise<void> {
    for await (const gemID of await this.getValueIDs()) {
      await this._internal_removeValueIDAndSync(gemID);
    }
  }

  __internalMap(): Map<K, V> {
    throw Error("__internalMap is not allowed, since GemMaps are always asynchronous.");
  }
}

/** the entry set of a GemValueMap: the keys are stored as they are */
class GemValueMapEntrySet<K, V extends GemIdentifiable> extends GemMapEntrySet<K, K, V> {

  constructor(/*private readonly*/ readonly _map: GemValueMap<K, V>) {
    super();
  }

  map(): AbstractAsyncMap<K, V> {
    return this._map;
  }

  currentKeys(): Promise<Array<K>> {
    return this._map.currentKeys();
  }

  lookupID(storedKey: K): number {
    return this._map.lookupID(storedKey);
  }

  toStoredKey(key: any): K {
    return key;
  }

  async keyObjects(storedKeys: Array<K>): Promise<Map<K, K>> {
    const result: Map<K, K> = new Map();
    storedKeys.forEach(k => result.set(k, k));
    return result;
  }

  valueObjects(gemIDs: Array<number>): Promise<Map<number, V>> {
    return this._map.loadValuesByID(gemIDs);
  }

  removeValueID(gemID: number): Promise<void> {
    return this._map._internal_removeValueIDAndSync(gemID);
  }

  containsValueID(gemID: number): Promise<boolean> {
    return this._map.containsValueID(gemID);
  }
}

GemValueMap["__class"] = "umlp.backendrte.common.GemValueMap";
GemValueMap["__interfaces"] = ["umlp.backendrte.common.GemBuildable"];
