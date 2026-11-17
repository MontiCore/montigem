/* (c) https://github.com/MontiCore/monticore */
import {JavaUtilUtils} from "./JavaUtilUtils";

/**
 * Finds the stored key of a native Map with the same value as a java.time key: like in Java, java.time keys are equal
 * by value, while a native Map compares objects by identity. The Temporal keys are indexed by their ISO string; the
 * owner of the map reports the keys it stores and removes (stored, removed), a map changed otherwise is indexed anew
 * when its size differs (or after reset).
 */
export class TemporalKeys<K> {

  private index: Map<string, K> = null;

  private indexedSize = -1;

  /** @param keys the map whose keys are indexed */
  constructor(private readonly keys: () => Map<K, any>) {
  }

  /** the stored key with the same value as key, or key itself (also for a key that is no java.time value) */
  find(key: any): any {
    const map: Map<K, any> = this.keys();
    if (!JavaUtilUtils.isTemporal(key) || map.has(key)) {
      return key;
    }
    if (this.index == null || this.indexedSize !== map.size) {
      this.index = new Map<string, K>();
      for (const k of map.keys()) {
        if (JavaUtilUtils.isTemporal(k)) {
          this.index.set(k.toString(), k);
        }
      }
      this.indexedSize = map.size;
    }
    const stored: K = this.index.get(key.toString());
    return stored !== undefined && map.has(stored) ? stored : key;
  }

  /** to be called after key has been stored in the map */
  stored(key: K): void {
    if (this.index != null && JavaUtilUtils.isTemporal(key)) {
      this.index.set(key.toString(), key);
    }
    this.indexedSize = this.keys().size;
  }

  /** to be called after key has been removed from the map */
  removed(key: K): void {
    if (this.index != null && JavaUtilUtils.isTemporal(key)) {
      this.index.delete(key.toString());
    }
    this.indexedSize = this.keys().size;
  }

  /** to be called after the map has been replaced or changed as a whole */
  reset(): void {
    this.index = null;
  }
}
