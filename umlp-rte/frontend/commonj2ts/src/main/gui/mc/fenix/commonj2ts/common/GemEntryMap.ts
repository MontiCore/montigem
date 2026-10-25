/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {GemIdentifiable} from "./GemIdentifiable";
import {GemBuildable} from "./GemBuildable";
import {LoadResult} from "./LoadResult";
import {LoadingState} from "./LoadingState";
import {IUMLPList} from "./IUMLPList";
import {IUMLPMap} from "./IUMLPMap";
import {GemEntrySet} from "./GemEntrySet";
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
 * Keys and values are domain objects, both are stored by their gemIds.
 *
 * Attribute qualified associations derive the key of a value from the value itself
 * (see {@link #deriveKeyID}). Values added by id only (e.g. when loading) are keyed
 * on first access, which loads them in one batch.
 */
export abstract class GemEntryMap<K extends GemIdentifiable, V extends GemIdentifiable> extends AbstractAsyncMap<K, V> implements GemBuildable {

  // local storage of the gemIds of the contained objects: all ids of the qualified association
  protected idMap: Map<number, number> = new Map();

  // ids of values whose key is not known yet, keyed by ensureKeyed()
  protected unkeyedValueIDs: Array<number> = [];

  // the running ensureKeyed(), so that concurrent calls wait for its keys
  protected keying: Promise<void> = null;

  // live entry set; keySet and values are projections of it
  protected entries: GemEntrySet<number, K, V> = new GemEntryMapEntrySet<K, V>(this);

  public constructor() {
    super();
  }

  public async __GemEntryMapConstructor(idMap?: IUMLPMap<number, number> | Map<number, number>): Promise<GemEntryMap<K, V>> {
    this.idMap = new Map();
    this.unkeyedValueIDs = [];
    if (idMap instanceof Map) {
      idMap.forEach((id, keyID) => this.idMap.set(keyID, id));
    } else if (idMap != null) {
      for await (const [keyID, id] of await idMap.entrySet()) {
        this.idMap.set(keyID, id);
      }
    }
    return this;
  }

  /* Default method injected from umlp.backendrte.common.GemBuildable */
  isBuildFinished(): boolean {
    return true;
  }

  public async build(): Promise<GemEntryMap<K, V>> {
    return this;
  }

  async size(): Promise<number> {
    await this.ensureKeyed();
    return this.idMap.size;
  }

  async isEmpty(): Promise<boolean> {
    return this.idMap.size === 0 && this.unkeyedValueIDs.length === 0;
  }

  public async getKey(gemID: number): Promise<K> {
    const lr: LoadResult<K> = await this.loadSingleKeyObject(gemID);
    if (lr.isPresent()) {
      return lr.get();
    } else {
      Log.error("Can not load Object with id " + gemID);
      return null;
    }
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

  /** live view, a projection of entrySet(), see GemEntrySet */
  async keySet(): Promise<AsyncSet<K>> {
    return this.entries.keySet();
  }

  /** live view, a projection of entrySet(), see GemEntrySet */
  async values(): Promise<Collection<V>> {
    return this.entries.values();
  }

  /** live view, see GemEntrySet */
  async entrySet(): Promise<AsyncSet<[K, V]>> {
    return AsyncSet.__viewOf(this.entries);
  }

  /**
   * @return the values with the gemIDs, loaded in one batch
   */
  async loadValuesByID(gemIDs: Array<number>): Promise<Map<number, V>> {
    return this.loadByID(gemIDs, ids => this.loadValueObjects(ids));
  }

  /**
   * @return the keys with the gemIDs, loaded in one batch
   */
  async loadKeysByID(gemIDs: Array<number>): Promise<Map<number, K>> {
    return this.loadByID(gemIDs, ids => this.loadKeyObjects(ids));
  }

  /*private*/ async loadByID<T>(gemIDs: Array<number>,
                                load: (gemIDs: IUMLPList<number>) => Promise<IUMLPList<LoadResult<T>>>): Promise<Map<number, T>> {
    const result: Map<number, T> = new Map();
    if (gemIDs.length === 0) {
      return result;
    }
    const loaded: IUMLPList<LoadResult<T>> = await load(await new AsyncList<number>().__AsyncListConstructor([...gemIDs]));
    const size = await loaded.size();
    for (let i = 0; i < size; i++) {
      const lr: LoadResult<T> = await loaded.get(i);
      if (lr.isPresent()) {
        result.set(gemIDs[i], lr.get());
      } else {
        Log.error("Can not load Object with id " + gemIDs[i]);
      }
    }
    return result;
  }

  async containsKey(key: any): Promise<boolean> {
    if (key != null && typeof key.getGemId === "function") {
      return this.containsKeyID(key.getGemId());
    } else {
      Log.error("Key is not GemIdentifiable");
    }
    return false;
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
   * identical to {@link Map#containsKey(Object)} but taking a gemID
   *
   * @param gemID the id of the object that may be contained in this Map
   * @return whether the object corresponding to gemID is contained in this Map
   */
  public async containsKeyID(gemID: number): Promise<boolean> {
    await this.ensureKeyed();
    return this.idMap.has(gemID);
  }

  /**
   * @param gemID the id of a key
   * @return the gemID of the value stored for that key, or null
   */
  public async getID(gemID: number): Promise<number> {
    await this.ensureKeyed();
    return this.lookupID(gemID);
  }

  /**
   * provides the key objects referenced by the gemIDs if possible
   *
   * @param gemIDs reference the key objects to get
   * @return the results of trying to load the objects
   */
  abstract loadKeyObjects(gemIDs: IUMLPList<number>): Promise<IUMLPList<LoadResult<K>>>;

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
      const derived: number = await this.deriveKeyID(value);
      if (key.getGemId() !== derived) {
        Log.error("0xA7315 Key with id " + key.getGemId() + " does not match the key with id " + derived
          + " derived from the object with id " + value.getGemId());
        return null;
      }
      const oldElem: V = await this.get(key);
      await this._internal_putValueAndSync(value);
      return oldElem;
    }
    const oldElem: V = await this.get(key);
    await this._internal_putAndSync(key.getGemId(), value.getGemId());
    return oldElem;
  }

  async get(key: any): Promise<V> {
    if (key != null && typeof key.getGemId === "function") {
      const valGemID: number = await this.getID(key.getGemId());
      return valGemID == null ? null : this.getValue(valGemID);
    } else {
      Log.error("Key is not GemIdentifiable");
    }
    return null;
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  async _internal_putAndSync(keyGemID: number, valueGemID: number): Promise<void> {
    await this._internal_put(keyGemID, valueGemID);
  }

  public async _internal_put(keyGemID: number, valueGemID: number): Promise<void> {
    const old: number = this.lookupID(keyGemID);
    this.idMap.set(keyGemID, valueGemID);
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
    await this._internal_removeAndSync(key.getGemId());
    return oldElem;
  }

  /**
   * to be overridden by a subclass to add synchronization
   */
  async _internal_removeAndSync(keyGemID: number): Promise<void> {
    await this._internal_remove(keyGemID);
  }

  public async _internal_remove(keyGemID: number): Promise<void> {
    const old: number = this.lookupID(keyGemID);
    this.idMap.delete(keyGemID);
    if (old != null) {
      await this.onValueRemoved(old, null);
    }
  }

  /**
   * derives the gemID of the key of a value,
   * only supported by maps of attribute qualified associations
   */
  public async deriveKeyID(value: V): Promise<number> {
    throw JavaUtilUtils.unsupportedOperation("0xA7311 The keys of " + this.constructor.name + " are not derived from its values");
  }

  /**
   * whether the key of a value is derived from the value (attribute qualified associations),
   * see {@link #deriveKeyID}; then a value cannot be put under an arbitrary key
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
    await this._internal_put(await this.deriveKeyID(value), value.getGemId());
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
    for (const [keyID, id] of Array.from(this.idMap.entries())) {
      if (id === gemID) {
        this.idMap.delete(keyID);
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
          Log.error("0xA7312 Can not load Object with id " + gemIDs[i] + " to derive its key");
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
    const keyID: number = await this.deriveKeyID(value);
    const present: number = this.lookupID(keyID);
    if (present != null && present !== value.getGemId()) {
      Log.error("0xA7313 Key " + keyID + " is not unique: it is derived from the objects with ids "
        + present + " and " + value.getGemId());
      return;
    }
    await this._internal_put(keyID, value.getGemId());
    await this.onValueKeyed(value);
  }

  /**
   * @return the id stored for the key with keyGemID, without deriving pending keys
   */
  lookupID(keyGemID: number): number {
    return this.idMap.has(keyGemID) ? this.idMap.get(keyGemID) : null;
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
   * moves a value from oldKeyGemID to newKeyGemID, e.g. after its qualifying attribute changed.
   * Keys are derived locally, so this is neither synchronized nor persisted.
   */
  public async _internal_rekey(oldKeyGemID: number, newKeyGemID: number, valueGemID: number): Promise<void> {
    if (this.lookupID(oldKeyGemID) !== valueGemID || oldKeyGemID === newKeyGemID) {
      // not (or no longer) contained under oldKeyGemID
      return;
    }
    if (this.lookupID(newKeyGemID) != null) {
      Log.error("0xA7314 Key " + newKeyGemID + " is not unique: it is derived from the objects with ids "
        + this.lookupID(newKeyGemID) + " and " + valueGemID);
      return;
    }
    this.moveEntry(oldKeyGemID, newKeyGemID);
  }

  moveEntry(oldKeyGemID: number, newKeyGemID: number): void {
    const id = this.idMap.get(oldKeyGemID);
    this.idMap.delete(oldKeyGemID);
    this.idMap.set(newKeyGemID, id);
  }

  /**
   * returns if the element is available
   * e.g. one is authorized to access it
   * this is NOT a range check
   */
  public async isPresent(gemID: number): Promise<boolean> {
    const valGemID: number = await this.getID(gemID);
    return valGemID != null && (await this.loadSingleValueObject(valGemID)).isPresent();
  }

  public async loadSingleValueObject(id: number): Promise<LoadResult<V>> {
    const res: IUMLPList<LoadResult<V>> = await this.loadValueObjects(await new AsyncList<number>().__AsyncListConstructor([id]));
    if ((await res.size()) == 0) {
      return await <any>(new LoadResult<any>().__LoadResultConstructor(LoadingState.FAILED_TO_LOAD));
    }
    return await res.get(0);
  }

  public async loadSingleKeyObject(id: number): Promise<LoadResult<K>> {
    const res: IUMLPList<LoadResult<K>> = await this.loadKeyObjects(await new AsyncList<number>().__AsyncListConstructor([id]));
    if ((await res.size()) == 0) {
      return await <any>(new LoadResult<any>().__LoadResultConstructor(LoadingState.FAILED_TO_LOAD));
    }
    return await res.get(0);
  }

  public async getKeyIDs(): Promise<AsyncSet<number>> {
    return await new AsyncSet<number>().__AsyncSetConstructor(await this.currentKeyIDs());
  }

  /** the gemIds of the keys, after deriving pending keys (see GemEntrySet#currentKeys) */
  async currentKeyIDs(): Promise<Array<number>> {
    await this.ensureKeyed();
    return Array.from(this.idMap.keys());
  }

  /**
   * the ids of all values, without deriving keys
   */
  public async getValueIDs(): Promise<IUMLPList<number>> {
    return await new AsyncList<number>().__AsyncListConstructor([...this.idMap.values(), ...this.unkeyedValueIDs]);
  }

  public async getIDMap(): Promise<IUMLPMap<number, number>> {
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

/** the entry set of a GemEntryMap: the keys are stored as their gemIds */
class GemEntryMapEntrySet<K extends GemIdentifiable, V extends GemIdentifiable> extends GemEntrySet<number, K, V> {

  constructor(/*private readonly*/ readonly _map: GemEntryMap<K, V>) {
    super();
  }

  map(): AbstractAsyncMap<K, V> {
    return this._map;
  }

  currentKeys(): Promise<Array<number>> {
    return this._map.currentKeyIDs();
  }

  lookupID(storedKey: number): number {
    return this._map.lookupID(storedKey);
  }

  toStoredKey(key: any): number {
    return key != null && typeof key.getGemId === "function" ? key.getGemId() : null;
  }

  keyObjects(storedKeys: Array<number>): Promise<Map<number, K>> {
    return this._map.loadKeysByID(storedKeys);
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

GemEntryMap["__class"] = "umlp.backendrte.common.GemEntryMap";
GemEntryMap["__interfaces"] = ["umlp.backendrte.common.GemBuildable"];
