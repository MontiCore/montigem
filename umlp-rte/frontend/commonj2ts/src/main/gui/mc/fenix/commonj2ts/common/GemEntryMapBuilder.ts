/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {GemEntryMap} from "./GemEntryMap";
import {GemIdentifiable} from "./GemIdentifiable";
import {GemBuildable} from "./GemBuildable";
import {IDOrBuilder} from "./IDOrBuilder";
import {IUMLPList} from "./IUMLPList";
import {IUMLPMap} from "./IUMLPMap";
import {AsyncList} from "../javautil/AsyncList";
import {AsyncMap} from "../javautil/AsyncMap";
import {AsyncSet} from "../javautil/AsyncSet";

// GemMap that has to be used while objects are created, as in this state it cannot be
// synchronized with other participants yet.
export abstract class GemEntryMapBuilder<K extends GemIdentifiable & GemBuildable, V extends GemIdentifiable & GemBuildable> extends GemEntryMap<K, V> {
  protected buildFinished: boolean = false;

  // Other builders can be added to a builder; the manager does not know them yet and cannot load them
  // by id, so they are kept here locally. build() clears this storage, it is not used afterwards.
  // The key is the gemId of the key element: builders have their gemId from construction on.
  // a Map keeps the insertion order
  protected builderElemStorage: Map<number, [IDOrBuilder<K>, IDOrBuilder<V>]> = new Map();

  public constructor() {
    super();
  }

  public async __GemEntryMapBuilderConstructor(initialIds: IUMLPMap<number, number> | Map<number, number>): Promise<GemEntryMapBuilder<K, V>> {
    await super.__GemEntryMapConstructor();
    if (initialIds instanceof Map) {
      for (const [keyID, id] of Array.from(initialIds.entries())) {
        await this.storeEntry(new IDOrBuilder<K>(keyID), new IDOrBuilder<V>(id));
      }
    } else if (initialIds != null) {
      for await (const [keyID, id] of await initialIds.entrySet()) {
        await this.storeEntry(new IDOrBuilder<K>(keyID), new IDOrBuilder<V>(id));
      }
    }
    return this;
  }

  public override async build(): Promise<GemEntryMap<K, V>> {
    if (this.buildFinished) {
      return this;
    }

    this.idMap.clear();
    for (const [key, value] of Array.from(this.builderElemStorage.values())) {
      this.idMap.set(await key.getIDAndMaybeBuild(), await value.getIDAndMaybeBuild());
    }

    this.buildFinished = true;

    // from now on, all elements are loaded by the manager
    this.builderElemStorage.clear();

    return this;
  }

  public override isBuildFinished(): boolean {
    return this.buildFinished;
  }

  override async size(): Promise<number> {
    if (this.buildFinished) {
      return super.size();
    } else {
      await this.ensureKeyed();
      return this.builderElemStorage.size;
    }
  }

  override async isEmpty(): Promise<boolean> {
    if (this.buildFinished) {
      return super.isEmpty();
    } else {
      return this.builderElemStorage.size === 0 && this.unkeyedValueIDs.length === 0;
    }
  }

  override async put(key: K, value: V): Promise<V> {
    if (this.buildFinished || this.isKeyDerived()) {
      return super.put(key, value);
    } else {
      // Map might contain builders, which a normal GemMap will not
      // Thus, the elements themselves are stored, not only their ids
      await this.ensureKeyed();
      const old = await this.storeEntry(new IDOrBuilder<K>(key), new IDOrBuilder<V>(value));
      return old == null ? null : this.resolveValue(old[1]);
    }
  }

  override async get(key: any): Promise<V> {
    if (this.buildFinished) {
      return super.get(key);
    } else {
      if (key == null || typeof key.getGemId !== "function") {
        Log.error("Key is not GemIdentifiable");
        return null;
      }
      await this.ensureKeyed();
      const entry = this.storageGet(key.getGemId());
      return entry == null ? null : this.resolveValue(entry[1]);
    }
  }

  protected override async removeKey(key: any): Promise<V> {
    if (this.buildFinished) {
      return super.removeKey(key);
    } else {
      if (key == null || typeof key.getGemId !== "function") {
        Log.error("Key is not GemIdentifiable");
        return null;
      }
      await this.ensureKeyed();
      const old = this.storageGet(key.getGemId());
      this.builderElemStorage.delete(key.getGemId());
      await this.valueLeft(old == null ? null : old[1], -1);
      return old == null ? null : this.resolveValue(old[1]);
    }
  }

  public override async containsKeyID(gemID: number): Promise<boolean> {
    if (this.buildFinished) {
      return super.containsKeyID(gemID);
    } else {
      await this.ensureKeyed();
      return this.builderElemStorage.has(gemID);
    }
  }

  public override async containsValueID(gemID: number): Promise<boolean> {
    if (this.buildFinished) {
      return super.containsValueID(gemID);
    } else {
      return this.unkeyedValueIDs.includes(gemID)
        || Array.from(this.builderElemStorage.values()).some(e => e[1].getGemId() === gemID);
    }
  }

  public override async getID(gemID: number): Promise<number> {
    if (this.buildFinished) {
      return super.getID(gemID);
    } else {
      await this.ensureKeyed();
      return this.lookupID(gemID);
    }
  }

  public override async _internal_put(keyGemID: number, valueGemID: number): Promise<void> {
    if (this.buildFinished) {
      await super._internal_put(keyGemID, valueGemID);
    } else {
      await this.storeEntry(new IDOrBuilder<K>(keyGemID), new IDOrBuilder<V>(valueGemID));
    }
  }

  public override async _internal_putValue(value: V): Promise<void> {
    if (this.buildFinished) {
      await super._internal_putValue(value);
    } else {
      // keep the element itself, it may be an unbuilt builder; the key is referenced by id
      await this.storeEntry(new IDOrBuilder<K>(await this.deriveKeyID(value)), new IDOrBuilder<V>(value));
      await this.onValueKeyed(value);
    }
  }

  public override async _internal_removeValueID(gemID: number): Promise<boolean> {
    if (this.buildFinished) {
      return super._internal_removeValueID(gemID);
    }
    const idx = this.unkeyedValueIDs.indexOf(gemID);
    if (idx >= 0) {
      this.unkeyedValueIDs.splice(idx, 1);
      return true;
    }
    for (const [keyID, entry] of Array.from(this.builderElemStorage.entries())) {
      if (entry[1].getGemId() === gemID) {
        this.builderElemStorage.delete(keyID);
        await this.valueLeft(entry[1], -1);
        return true;
      }
    }
    return false;
  }

  public override async _internal_remove(keyGemID: number): Promise<void> {
    if (this.buildFinished) {
      await super._internal_remove(keyGemID);
    } else {
      const old = this.storageGet(keyGemID);
      this.builderElemStorage.delete(keyGemID);
      await this.valueLeft(old == null ? null : old[1], -1);
    }
  }

  override async clear(): Promise<void> {
    if (this.buildFinished) {
      await super.clear();
    } else {
      const removed = Array.from(this.builderElemStorage.values());
      this.builderElemStorage.clear();
      this.unkeyedValueIDs.length = 0;
      for (const old of removed) {
        await this.valueLeft(old[1], -1);
      }
    }
  }

  public override async getIDMap(): Promise<IUMLPMap<number, number>> {
    if (this.buildFinished) {
      return super.getIDMap();
    } else {
      await this.ensureKeyed();
      const ids: Map<number, number> = new Map();
      for (const [key, value] of Array.from(this.builderElemStorage.values())) {
        if (key.isPresentID() && value.isPresentID()) {
          ids.set(key.getID(), value.getID());
        } else {
          //todo decide on error handling
        }
      }
      return AsyncMap.__viewOf(ids);
    }
  }

  public override async getValueIDs(): Promise<IUMLPList<number>> {
    if (this.buildFinished) {
      return super.getValueIDs();
    } else {
      const ids: Array<number> = Array.from(this.builderElemStorage.values()).map(e => e[1].getGemId());
      ids.push(...this.unkeyedValueIDs);
      return await new AsyncList<number>().__AsyncListConstructor(ids);
    }
  }

  override lookupID(keyGemID: number): number {
    if (this.buildFinished) {
      return super.lookupID(keyGemID);
    } else {
      const entry = this.storageGet(keyGemID);
      return entry == null ? null : entry[1].getGemId();
    }
  }

  override moveEntry(oldKeyGemID: number, newKeyGemID: number): void {
    if (this.buildFinished) {
      super.moveEntry(oldKeyGemID, newKeyGemID);
    } else {
      const entry = this.storageGet(oldKeyGemID);
      this.builderElemStorage.delete(oldKeyGemID);
      // the value stays, so it does not leave the map
      this.builderElemStorage.set(newKeyGemID, [new IDOrBuilder<K>(newKeyGemID), entry[1]]);
    }
  }

  /**
   * stores the entry under the gemId of its key
   * @return the entry previously stored for that key, or null
   */
  async storeEntry(key: IDOrBuilder<K>, value: IDOrBuilder<V>): Promise<[IDOrBuilder<K>, IDOrBuilder<V>]> {
    const old = this.storageGet(key.getGemId());
    this.builderElemStorage.set(key.getGemId(), [key, value]);
    await this.valueLeft(old == null ? null : old[1], value.getGemId());
    return old;
  }

  async resolveKey(key: IDOrBuilder<K>): Promise<K> {
    return key.isPresentBuilder() ? key.getBuilder() : this.getKey(key.getID());
  }

  /**
   * calls {@link #onValueRemoved} if old left the map, i.e. was not replaced by itself
   */
  async valueLeft(old: IDOrBuilder<V>, newValueGemID: number): Promise<void> {
    if (old != null && old.getGemId() !== newValueGemID) {
      await this.onValueRemoved(old.getGemId(), old.isPresentBuilder() ? old.getBuilder() : null);
    }
  }

  public override async getKeyIDs(): Promise<AsyncSet<number>> {
    if (this.buildFinished) {
      return super.getKeyIDs();
    } else {
      return await new AsyncSet<number>().__AsyncSetConstructor(await this.currentKeyIDs());
    }
  }

  override async currentKeyIDs(): Promise<Array<number>> {
    if (this.buildFinished) {
      return super.currentKeyIDs();
    } else {
      await this.ensureKeyed();
      return Array.from(this.builderElemStorage.keys());
    }
  }

  /**
   * before build, the values may be unbuilt builders, which cannot be loaded
   */
  override async loadValuesByID(gemIDs: Array<number>): Promise<Map<number, V>> {
    if (this.buildFinished) {
      return super.loadValuesByID(gemIDs);
    }
    const result: Map<number, V> = new Map();
    const toLoad: Array<number> = [];
    for (const gemID of gemIDs) {
      const stored = Array.from(this.builderElemStorage.values()).map(e => e[1])
        .find(v => v.isPresentBuilder() && v.getGemId() === gemID);
      if (stored != null) {
        result.set(gemID, stored.getBuilder());
      } else {
        toLoad.push(gemID);
      }
    }
    (await super.loadValuesByID(toLoad)).forEach((value, gemID) => result.set(gemID, value));
    return result;
  }

  /**
   * before build, the keys may be unbuilt builders, which cannot be loaded
   */
  override async loadKeysByID(gemIDs: Array<number>): Promise<Map<number, K>> {
    if (this.buildFinished) {
      return super.loadKeysByID(gemIDs);
    }
    const result: Map<number, K> = new Map();
    const toLoad: Array<number> = [];
    for (const gemID of gemIDs) {
      const stored = this.storageGet(gemID);
      if (stored != null && stored[0].isPresentBuilder()) {
        result.set(gemID, stored[0].getBuilder());
      } else {
        toLoad.push(gemID);
      }
    }
    (await super.loadKeysByID(toLoad)).forEach((key, gemID) => result.set(gemID, key));
    return result;
  }

  async resolveValue(value: IDOrBuilder<V>): Promise<V> {
    return value.isPresentBuilder() ? value.getBuilder() : this.getValue(value.getID());
  }

  /*private*/ storageGet(keyGemID: number): [IDOrBuilder<K>, IDOrBuilder<V>] {
    return this.builderElemStorage.has(keyGemID) ? this.builderElemStorage.get(keyGemID) : null;
  }
}

GemEntryMapBuilder["__class"] = "umlp.backendrte.common.GemEntryMapBuilder";
GemEntryMapBuilder["__interfaces"] = ["umlp.backendrte.common.GemBuildable"];
