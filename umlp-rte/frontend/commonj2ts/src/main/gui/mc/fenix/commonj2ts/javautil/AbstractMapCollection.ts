/* (c) https://github.com/MontiCore/monticore */
import {AsyncStream} from "./AsyncStream";
import {Collection} from "./Collection";
import {JavaUtilUtils} from "./JavaUtilUtils";
import {AbstractAsyncMap} from "./AbstractAsyncMap";

/**
 * Collection methods shared by the entry sets of the maps (AsyncMap, GemValueMap, GemEntryMap) and their
 * projections keySet and values. Internal, not exported from the package.
 */
export abstract class AbstractMapCollection<E> implements Collection<E> {

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
