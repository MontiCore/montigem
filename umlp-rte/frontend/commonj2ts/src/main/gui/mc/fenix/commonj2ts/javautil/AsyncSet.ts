import {GemList} from "../common/GemList";
import {Collection} from "../javautil/Collection";
import {AsyncStream} from "../javautil";

export class AsyncSet<E> implements Collection<E> {
  /*private readonly*/
  _data: E[];
  /*private readonly*/
  _delegate?: Collection<E>;

  constructor() {
  }

  async __AsyncSetConstructor(data?: E[] | Collection<E>): Promise<AsyncSet<E>> {
    if (Array.isArray(data)) {
      this._data = [];
      for (const item of data) {
        if (!this._data.includes(item)) {
          this._data.push(item);
        }
      }
    } else if (data && typeof (data as any)[Symbol.asyncIterator] === "function") {
      if(typeof (data as any).__AsyncSetConstructor === "function") { // AsyncSet Case
        this._delegate = data as AsyncSet<E>;
      } else { // Collection e.g. List or GemList
        for await (const e of data) {
          this._data.push(e);
        }
      }
      this._delegate = data as Collection<E>;
      this._data = [];
    } else if (data === undefined || data === null) {
      this._data = [];
    } else {
      console.error("AsyncSet Constructor was called with an argument that is not an array or an AsyncIterable")
      this._data = [];
    }
    return this;
  }

  // TODO is this only needed for testing?
  public toString() : string {
    return "["+ this._data.toString()+"]";
  }

  static async from<E>(iterable: Collection<E>): Promise<AsyncSet<E>> {
    return new AsyncSet<E>().__AsyncSetConstructor(iterable);
  }

  /** A live view: all calls are delegated to the collection (e.g. AsyncMap#keySet, AsyncMap#entrySet) */
  static __viewOf<E>(delegate: Collection<E>): AsyncSet<E> {
    const view = new AsyncSet<E>();
    view._data = [];
    view._delegate = delegate;
    return view;
  }

  // ─── Static factory methods ───────────────────────────────────────────────

  static of<E>(...elements: E[]): Promise<AsyncSet<E>> {
    return new AsyncSet<E>().__AsyncSetConstructor(elements);
  }

  static async copyOf<E>(collection: AsyncIterable<E>): Promise<AsyncSet<E>> {
    const data: E[] = [];
    for await (const item of collection) {
      if (!data.includes(item)) {
        data.push(item);
      }
    }
    return new AsyncSet<E>().__AsyncSetConstructor(data);
  }

  // ─── AsyncIterable implementation ─────────────────────────────────────────

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    if (this._delegate) {
      yield* this._delegate;
    } else {
      for (const item of this._data) {
        yield item;
      }
    }
  }

  // ─── Methods returning boolean ────────────────────────────────────────────

  /**
   * java.util.Set#add(E) → boolean
   *
   * Index argument is ignored for Set semantics. The element is only added if
   * not already present (reference equality ===).
   */
  add(indexOrElement: number | E, element?: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.add(indexOrElement as any, element as any);
    }
    const elem = (element !== undefined) ? element : indexOrElement as E;
    if (this._data.includes(elem)) {
      return Promise.resolve(false);
    }
    this._data.push(elem);
    return Promise.resolve(true);
  }

  /**
   * java.util.Set#addAll(Collection<? extends E>) → boolean
   *
   * Index argument is ignored for Set semantics. Only elements not already
   * present are added.
   */
  addAll(idxOrElems: number | Collection<E> | Array<E>, elems?: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.addAll(idxOrElems as any, elems as any);
    }
    const source: Collection<E> | Array<E> = (typeof idxOrElems === 'number') ? elems! : idxOrElems as Collection<E> | Array<E>;
    return (async () => {
      let changed = false;
      if (Array.isArray(source)) {
        for (const item of source) {
          if (!this._data.includes(item)) {
            this._data.push(item);
            changed = true;
          }
        }
      } else {
        for await (const item of source) {
          if (!this._data.includes(item)) {
            this._data.push(item);
            changed = true;
          }
        }
      }
      return changed;
    })();
  }

  /** java.util.Set#contains(Object) → boolean */
  public async contains(element: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.contains(element);
    }
    return this._data.includes(element);
  }

  /** java.util.Set#containsAll(Collection<?>) → boolean */
  public async containsAll(c: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.containsAll(c);
    }
    for await (const target of c) {
      if (!this._data.includes(target)) return false;
    }
    return true;
  }

  /** java.util.Set#equals(Object) → boolean — order-independent element equality uses === */
  async equals(o: unknown): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.equals(o);
    }
    if (this === o) return true;
    if (!(o instanceof AsyncSet)) return false;
    if (this._data.length !== o._data.length) return false;
    for (const item of this._data) {
      if (!o._data.includes(item)) return false;
    }
    return true;
  }

  /** java.util.Set#isEmpty() → boolean */
  async isEmpty(): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.isEmpty();
    }
    return this._data.length === 0;
  }

  /** @throws — java.util.Set has no index-based remove */
  removeIndex(_idx: number): Promise<E> {
    throw new Error('removeIndex() is not supported by Set. Use removeElement() instead.');
  }

  removeElement(elem: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.removeElement(elem);
    }
    return (async () => {
      const i = this._data.indexOf(elem);
      if (i !== -1) {
        this._data.splice(i, 1);
        return true;
      }
      return false;
    })();
  }

  /** java.util.Set#removeAll(Collection<?>) → boolean */
  async removeAll(c: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.removeAll(c);
    }
    const toRemove: E[] = [];
    for await (const item of c) {
      toRemove.push(item);
    }
    let changed = false;
    const kept: E[] = [];
    for (const item of this._data) {
      if (toRemove.includes(item)) {
        changed = true;
      } else {
        kept.push(item);
      }
    }
    this._data.length = 0;
    this._data.push(...kept);
    return changed;
  }

  /** java.util.Collection#removeIf(Predicate<E>) → boolean */
  async removeIf(predicate: (e: E) => boolean): Promise<boolean> {
    if (this._delegate) {
      const toRemove: E[] = [];
      for await (const item of this._delegate) {
        if (predicate(item)) {
          toRemove.push(item);
        }
      }
      let changed = false;
      for (const item of toRemove) {
        changed = (await this._delegate.removeElement(item)) || changed;
      }
      return changed;
    }
    let changed = false;
    const kept: E[] = [];
    for (const item of this._data) {
      if (predicate(item)) {
        changed = true;
      } else {
        kept.push(item);
      }
    }
    this._data.length = 0;
    this._data.push(...kept);
    return changed;
  }

  /** java.util.Set#retainAll(Collection<?>) → boolean */
  async retainAll(c: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.retainAll(c);
    }
    const toKeep: E[] = [];
    for await (const item of c) {
      toKeep.push(item);
    }
    let changed = false;
    const kept: E[] = [];
    for (const item of this._data) {
      if (toKeep.includes(item)) {
        kept.push(item);
      } else {
        changed = true;
      }
    }
    this._data.length = 0;
    this._data.push(...kept);
    return changed;
  }

  // ─── Methods returning int (number) ──────────────────────────────────────

  /**
   * java.util.Set#hashCode() → int
   * Follows AbstractSet.hashCode(): sum of element hashes (null elements contribute 0).
   */
  async hashCode(): Promise<number> {
    let hash = 0;
    if (this._delegate) {
      for await (const item of this._delegate) {
        hash = (hash + this._elementHash(item)) | 0;
      }
      return hash;
    }
    for (const item of this._data) {
      hash = (hash + this._elementHash(item)) | 0;
    }
    return hash;
  }

  /** java.util.Set#size() → int */
  async size(): Promise<number> {
    if (this._delegate) {
      return this._delegate.size();
    }
    return this._data.length;
  }

  // ─── Methods returning void ───────────────────────────────────────────────

  /** java.util.Set#clear() → void */
  async clear(): Promise<void> {
    if (this._delegate) {
      return this._delegate.clear();
    }
    this._data.length = 0;
  }

  /** java.lang.Iterable#forEach(Consumer<E>) → void */
  async forEach(action: (e: E) => void): Promise<void> {
    if (this._delegate) {
      return this._delegate.forEach(action);
    }
    for (const item of this._data) {
      action(item);
    }
  }

  stream(): AsyncStream<E> {
    return AsyncStream.of(this);
  }

  async toArrayDeepSync(startIndex?: number, endIndex?: number): Promise<Array<any>> {
    if (this._delegate) {
      return this._delegate.toArrayDeepSync(startIndex, endIndex);
    }
    if (this._data.length == 0) {
      return this._data;
    } else if (this._data[0] instanceof GemList || this._data[0] instanceof AsyncSet) {
      const result: any[] = [];

      for (const v of this._data) {
        result.push(await (v as unknown as Collection<any>).toArrayDeepSync());
      }

      return result;
    } else {
      return this._data;
    }
  }

  // ─── Still unsupported ────────────────────────────────────────────────────

  /** @throws — use [Symbol.asyncIterator]() instead */
  iterator(): AsyncIterator<E> {
    throw new Error('iterator() returns Iterator<E>. Use [Symbol.asyncIterator]() instead.');
  }

  /** @throws — java.util.Set has no listIterator */
  listIterator(_index?: number): AsyncIterator<E> {
    throw new Error('listIterator() is not supported by Set. Use [Symbol.asyncIterator]() instead.');
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
  toArray<T>(args?: any): never {
    throw new Error('toArray() not supported use GemListUtil#toArraySync if you need JavaScript arrays.');
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  /** Java-compatible per-element hash, mirroring Boolean/Number/String.hashCode() conventions. */
  /*private*/ _elementHash(e: E): number {
    if (e == null) return 0;
    if (typeof e === 'boolean') return e ? 1231 : 1237;
    if (typeof e === 'number') return e | 0;
    const s = String(e);
    let h = 0;
    for (let i = 0; i < s.length; i++) {
      h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    }
    return h;
  }
}
