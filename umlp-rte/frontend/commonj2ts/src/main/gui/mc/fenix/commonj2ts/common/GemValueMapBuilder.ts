/* (c) https://github.com/MontiCore/monticore */
import {GemValueMap} from "./GemValueMap";
import {GemIdentifiable} from "./GemIdentifiable";
import {GemBuildable} from "./GemBuildable";
import {IDOrBuilder} from "./IDOrBuilder";
import {IUMLPList} from "./IUMLPList";
import {IUMLPMap} from "./IUMLPMap";
import {AsyncList} from "../javautil/AsyncList";
import {AsyncMap} from "../javautil/AsyncMap";
import {AsyncSet} from "../javautil/AsyncSet";

// GemMap, welche bei der Erstellung von Objekten benutzt werden muss, da sie in diesem Zustand
// noch nicht mit anderen Teilnehmen synchronisiert werden kann.
export abstract class GemValueMapBuilder<K, V extends GemIdentifiable & GemBuildable> extends GemValueMap<K, V> {
  protected buildFinished: boolean = false;

  // Da einem in einem Builder andere Builder hinzugefügt werden können und der FlensburgManager diese
  // nicht kennt, also auch nicht per id laden kann, muss hier eine lokale Verwaltung stattfinden.
  // Nach build() wird diese geleert und nicht weiter verwendet.
  // Map, damit die Reihenfolge der Einfügereihenfolge entspricht.
  protected builderElemStorage: Map<K, IDOrBuilder<V>> = new Map();

  public constructor() {
    super();
  }

  public async __GemValueMapBuilderConstructor(initialIds: IUMLPMap<K, number> | Map<K, number>): Promise<GemValueMapBuilder<K, V>> {
    await super.__GemValueMapConstructor();
    if (initialIds instanceof Map) {
      initialIds.forEach((id, key) => this.builderElemStorage.set(key, new IDOrBuilder<V>(id)));
    } else if (initialIds != null) {
      for await (const [key, id] of await initialIds.entrySet()) {
        this.builderElemStorage.set(key, new IDOrBuilder<V>(id));
      }
    }
    return this;
  }

  public override async build(): Promise<GemValueMap<K, V>> {
    if (this.buildFinished) {
      return this;
    }

    this.idMap.clear();
    for (const [key, element] of Array.from(this.builderElemStorage.entries())) {
      this.idMap.set(key, await element.getIDAndMaybeBuild());
    }

    this.buildFinished = true;

    // Von hier an werden alle Element vom FlensburgManager geladen
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
      const old: IDOrBuilder<V> = this.storageGet(key);
      this.builderElemStorage.set(key, new IDOrBuilder<V>(value));
      await this.valueLeft(old, value.getGemId());
      return old == null ? null : this.resolveValue(old);
    }
  }

  override async get(key: any): Promise<V> {
    if (this.buildFinished) {
      return super.get(key);
    } else {
      await this.ensureKeyed();
      const value: IDOrBuilder<V> = this.storageGet(key);
      return value == null ? null : this.resolveValue(value);
    }
  }

  protected override async removeKey(key: any): Promise<V> {
    if (this.buildFinished) {
      return super.removeKey(key);
    } else {
      await this.ensureKeyed();
      const old: IDOrBuilder<V> = this.storageGet(key);
      this.builderElemStorage.delete(key);
      await this.valueLeft(old, -1);
      return old == null ? null : this.resolveValue(old);
    }
  }

  override async containsKey(key: any): Promise<boolean> {
    if (this.buildFinished) {
      return super.containsKey(key);
    } else {
      await this.ensureKeyed();
      return this.builderElemStorage.has(key);
    }
  }

  public override async containsValueID(gemID: number): Promise<boolean> {
    if (this.buildFinished) {
      return super.containsValueID(gemID);
    } else {
      return this.unkeyedValueIDs.includes(gemID)
        || Array.from(this.builderElemStorage.values()).some(v => v.getGemId() === gemID);
    }
  }

  public override async _internal_put(key: K, valueGemID: number): Promise<void> {
    if (this.buildFinished) {
      await super._internal_put(key, valueGemID);
    } else {
      const old: IDOrBuilder<V> = this.storageGet(key);
      this.builderElemStorage.set(key, new IDOrBuilder<V>(valueGemID));
      await this.valueLeft(old, valueGemID);
    }
  }

  public override async _internal_putValue(value: V): Promise<void> {
    if (this.buildFinished) {
      await super._internal_putValue(value);
    } else {
      // keep the element itself, it may be an unbuilt builder
      const key: K = await this.deriveKey(value);
      const old: IDOrBuilder<V> = this.storageGet(key);
      this.builderElemStorage.set(key, new IDOrBuilder<V>(value));
      await this.valueLeft(old, value.getGemId());
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
    for (const [key, value] of Array.from(this.builderElemStorage.entries())) {
      if (value.getGemId() === gemID) {
        this.builderElemStorage.delete(key);
        await this.valueLeft(value, -1);
        return true;
      }
    }
    return false;
  }

  public override async _internal_remove(key: K): Promise<void> {
    if (this.buildFinished) {
      await super._internal_remove(key);
    } else {
      const old: IDOrBuilder<V> = this.storageGet(key);
      this.builderElemStorage.delete(key);
      await this.valueLeft(old, -1);
    }
  }

  override async clear(): Promise<void> {
    if (this.buildFinished) {
      await super.clear();
    } else {
      const removed: Array<IDOrBuilder<V>> = Array.from(this.builderElemStorage.values());
      this.builderElemStorage.clear();
      this.unkeyedValueIDs.length = 0;
      for (const old of removed) {
        await this.valueLeft(old, -1);
      }
    }
  }

  public override async getIDMap(): Promise<IUMLPMap<K, number>> {
    if (this.buildFinished) {
      return super.getIDMap();
    } else {
      await this.ensureKeyed();
      const ids: Map<K, number> = new Map();
      for (const [key, value] of Array.from(this.builderElemStorage.entries())) {
        if (value.isPresentID()) {
          ids.set(key, value.getID());
        } else {
          //todo decide on error handling
        }
      }
      return AsyncMap.__viewOf(ids);
    }
  }

  public override async getID(key: any): Promise<number> {
    if (this.buildFinished) {
      return super.getID(key);
    } else {
      await this.ensureKeyed();
      return this.lookupID(key);
    }
  }

  public override async getValueIDs(): Promise<IUMLPList<number>> {
    if (this.buildFinished) {
      return super.getValueIDs();
    } else {
      const ids: Array<number> = Array.from(this.builderElemStorage.values()).map(v => v.getGemId());
      ids.push(...this.unkeyedValueIDs);
      return await new AsyncList<number>().__AsyncListConstructor(ids);
    }
  }

  override lookupID(key: any): number {
    if (this.buildFinished) {
      return super.lookupID(key);
    } else {
      const value: IDOrBuilder<V> = this.storageGet(key);
      return value == null ? null : value.getGemId();
    }
  }

  override moveEntry(oldKey: K, newKey: K): void {
    if (this.buildFinished) {
      super.moveEntry(oldKey, newKey);
    } else {
      const value: IDOrBuilder<V> = this.storageGet(oldKey);
      this.builderElemStorage.delete(oldKey);
      this.builderElemStorage.set(newKey, value);
    }
  }

  /**
   * calls {@link #onValueRemoved} if old left the map, i.e. was not replaced by itself
   */
  async valueLeft(old: IDOrBuilder<V>, newValueGemID: number): Promise<void> {
    if (old != null && old.getGemId() !== newValueGemID) {
      await this.onValueRemoved(old.getGemId(), old.isPresentBuilder() ? old.getBuilder() : null);
    }
  }

  public override async getKeys(): Promise<AsyncSet<K>> {
    if (this.buildFinished) {
      return super.getKeys();
    } else {
      return await new AsyncSet<K>().__AsyncSetConstructor(await this.currentKeys());
    }
  }

  override async currentKeys(): Promise<Array<K>> {
    if (this.buildFinished) {
      return super.currentKeys();
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
      const stored: IDOrBuilder<V> = Array.from(this.builderElemStorage.values())
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

  async resolveValue(value: IDOrBuilder<V>): Promise<V> {
    return value.isPresentBuilder() ? value.getBuilder() : this.getValue(value.getID());
  }

  /*private*/ storageGet(key: any): IDOrBuilder<V> {
    return this.builderElemStorage.has(key) ? this.builderElemStorage.get(key) : null;
  }
}

GemValueMapBuilder["__class"] = "umlp.backendrte.common.GemValueMapBuilder";
GemValueMapBuilder["__interfaces"] = ["umlp.backendrte.common.GemBuildable"];
