import {IUMLPList} from "../common/IUMLPList";
import {GemList} from "../common/GemList";
import {Collection} from "../javautil/Collection";
import {JavaAsyncIterator, JavaAsyncIteratorAdapter} from "./JavaAsyncIterator";
import {AsyncStream} from "../javautil/AsyncStream";

export class AsyncList<E> implements IUMLPList<E> {
  /*private readonly*/
  _data: E[];
  /*private readonly*/
  _delegate?: IUMLPList<E>;

  constructor() {
  }

  __AsyncListConstructor(data: E[] | Collection<E>): Promise<AsyncList<E>>;
  __AsyncListConstructor(...items: E[]): Promise<AsyncList<E>>;
  async __AsyncListConstructor(first: E[] | Collection<E> | E,...rest: E[]) : Promise<AsyncList<E>> {
    if(first === undefined || first === null) {
      this._data = [];
    } else if (Array.isArray(first) && rest.length === 0) {
      this._data = first;
    } else if (first && typeof (first as any)[Symbol.asyncIterator] === "function" && rest.length === 0) {
      this._data = [];
      if(typeof (first as any).indexOf === "function") { // AsyncList or GemList Case
        this._delegate = first as IUMLPList<E>;
      } else { // Collection
        for await (const e of first as any) {
          this._data.push(e);
        }
      }
    } else {
      this._data = [first as E, ...rest];
    }
    return this;
  }

  // TODO is this only needed for testing?
  public toString() : string {
    return "["+ this._data.toString()+"]";
  }

  static async from<E>(iterable: IUMLPList<E>): Promise<AsyncList<E>> {
    return new AsyncList<E>().__AsyncListConstructor(iterable);
  }

  async toArrayDeepSync(startIndex?: number, endIndex?: number): Promise<Array<any>> {
    if (this._delegate) {
      return this._delegate.toArrayDeepSync(startIndex, endIndex);
    }
    if (this._data.length == 0) {
      return this._data; // TODO copied from UMLPArrayList
    } else if (this._data[0] instanceof GemList || this._data[0] instanceof AsyncList) {
      const result: any[] = [];

      for (const v of this._data) {
        result.push(await (v as unknown as IUMLPList<any>).toArrayDeepSync());
      }

      return result;
    } else {
      // TODO copied from UMLPArrayList
      return this._data;
    }
  }

  // ─── Static factory methods ───────────────────────────────────────────────

  static of<E>(...elements: E[]): Promise<AsyncList<E>> {
    return new AsyncList<E>().__AsyncListConstructor(elements);
  }

  static async copyOf<E>(collection: AsyncIterable<E>): Promise<AsyncList<E>> {
    const data: E[] = [];
    for await (const item of collection) {
      data.push(item);
    }
    return new AsyncList<E>().__AsyncListConstructor(data);
  }

  // ─── AsyncIterable implementation ─────────────────────────────────────────

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    if (this._delegate) {
      const iterator = this._delegate.iterator();
      while (await iterator.hasNext()) {
        yield await iterator.next();
      }
    } else {
      for (const item of this._data) {
        yield item;
      }
    }
  }

  // ─── Methods returning boolean ────────────────────────────────────────────

  /**
   * java.util.List#add(E) → boolean  /  java.util.List#add(int, E) → void
   *
   * When E is number the two overloads are ambiguous at the call site —
   * a single numeric argument is always treated as the element variant.
   */
  add(element: E): Promise<boolean>;
  add(index: number, element: E): Promise<boolean>;
  add(indexOrElement: number | E, element?: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.add(indexOrElement as any, element as any);
    }
    if (element !== undefined) {
      this._data.splice(indexOrElement as number, 0, element);
      return Promise.resolve(true);
    }
    this._data.push(indexOrElement as E);
    return Promise.resolve(true);
  }

  public async addFirst(element : E): Promise<boolean> {
    return await this.add(0, element);
  }

  public async addLast(element : E): Promise<boolean> {
    return await this.add(await this.size(), element);
  }

  /**
   * java.util.List#addAll(Collection<? extends E>) → boolean
   * java.util.List#addAll(int, Collection<? extends E>) → boolean
   */
  addAll(c: Collection<E>): Promise<boolean>;
  addAll(index: number, c: Collection<E>): Promise<boolean>;
  addAll(indexOrC: number | Collection<E>, c?: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.addAll(indexOrC as any, c as any);
    }
    if (typeof indexOrC === 'number') {
      const idx = indexOrC;
      const source = c!;
      return (async () => {
        const items: E[] = [];
        for await (const item of source) {
          items.push(item);
        }
        if (items.length === 0) return false;
        this._data.splice(idx, 0, ...items);
        return true;
      })();
    }
    const source = indexOrC;
    return (async () => {
      let changed = false;
      for await (const item of source) {
        this._data.push(item);
        changed = true;
      }
      return changed;
    })();
  }

  /** java.util.List#contains(Object) → boolean */
  public async contains(element: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.contains(element);
    }
    return this._data.includes(element);
  }

  /** java.util.List#containsAll(Collection<?>) → boolean */
  public async containsAll(c: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.containsAll(c);
    }
    for await (const target of c) {
      if (!this._data.includes(target)) return false;
    }
    return true;
  }

  /** java.util.List#equals(Object) → boolean — element equality uses === */
  async equals(o: unknown): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.equals(o);
    }
    if (this === o) return true;
    if (!(o instanceof AsyncList)) return false;
    if (this._data.length !== o._data.length) return false;
    for (let i = 0; i < this._data.length; i++) {
      if (this._data[i] !== o._data[i]) return false;
    }
    return true;
  }

  /** java.util.List#isEmpty() → boolean */
  async isEmpty(): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.isEmpty();
    }
    return this._data.length === 0;
  }

  /**
   * java.util.List#remove(int) → E
   * java.util.List#remove(Object) → boolean
   *
   * When E is number, a numeric argument is always treated as an index.
   */
  removeIndex(idx: number): Promise<E> {
    if (this._delegate) {
      return this._delegate.removeIndex(idx);
    }
    if (idx < 0 || idx >= this._data.length) {
      return Promise.reject(new RangeError(`RemoveIndex: Index out of bounds: ${idx}`));
    }
    return Promise.resolve(this._data.splice(idx, 1)[0]);
  }

  removeElement(elem: E): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.removeElement(elem);
    }
    const i = this._data.indexOf(elem);
    if (i === -1) {
      return Promise.resolve(false);
    }
    this._data.splice(i, 1);
    return Promise.resolve(true);
  }

  /** java.util.List#removeAll(Collection<?>) → boolean */
  async removeAll(c: Collection<E>): Promise<boolean> {
    if (this._delegate) {
      return this._delegate.removeAll(c as IUMLPList<E>);
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

  /** java.util.List#retainAll(Collection<?>) → boolean */
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
   * java.util.List#hashCode() → int
   * Follows AbstractList.hashCode(): hash = 31 * hash + elementHash, seed 1.
   */
  async hashCode(): Promise<number> {
    let hash = 1;
    if (this._delegate) {
      for await (const item of this._delegate) {
        hash = (Math.imul(31, hash) + this._elementHash(item)) | 0;
      }
      return hash;
    }
    for (const item of this._data) {
      hash = (Math.imul(31, hash) + this._elementHash(item)) | 0;
    }
    return hash;
  }

  /** java.util.List#indexOf(Object) → int — returns -1 if not present */
  async indexOf(element: E): Promise<number> {
    if (this._delegate) {
      return this._delegate.indexOf(element);
    }
    return this._data.indexOf(element);
  }

  /** java.util.List#lastIndexOf(Object) → int — returns -1 if not present */
  async lastIndexOf(element: E): Promise<number> {
    if (this._delegate) {
      return this._delegate.lastIndexOf(element);
    }
    return this._data.lastIndexOf(element);
  }

  /** java.util.List#size() → int */
  async size(): Promise<number> {
    if (this._delegate) {
      return this._delegate.size();
    }
    return this._data.length;
  }

  // ─── Methods returning void, E, or List<E> ───────────────────────────────

  /** java.util.List#clear() → void */
  async clear(): Promise<void> {
    if (this._delegate) {
      return this._delegate.clear();
    }
    this._data.length = 0;
  }

  /** java.util.List#get(int) → E */
  async get(index: number): Promise<E> {
    if (this._delegate) {
      return this._delegate.get(index);
    }
    if (index < 0 || index >= this._data.length) {
      throw new RangeError(`Get: Index out of bounds: ${index}`);
    }
    return this._data[index];
  }

  /** java.util.List#set(int, E) → E */
  async set(index: number, element: E): Promise<E> {
    if (this._delegate) {
      return this._delegate.set(index, element);
    }
    if (index < 0 || index >= this._data.length) {
      throw new RangeError(`Set: Index out of bounds: ${index}`);
    }
    const previous = this._data[index];
    this._data[index] = element;
    return previous;
  }

  /** java.util.List#replaceAll(UnaryOperator<E>) → void */
  async replaceAll(operator: (e: E) => E): Promise<void> {
    if (this._delegate) {
      const size = await this._delegate.size();
      for (let i = 0; i < size; i++) {
        await this._delegate.set(i, operator(await this._delegate.get(i)));
      }
      return;
    }
    for (let i = 0; i < this._data.length; i++) {
      this._data[i] = operator(this._data[i]);
    }
  }

  /** java.util.List#sort(Comparator<E>) → void */
  async sort(comparator?: any): Promise<void> {
    // TODO inplace is a problem for delegate semantics
    if (this._delegate) {
      return this._delegate.sort(comparator);
    }
    this._data.sort(comparator);
  }

  /** java.util.List#subList(int, int) → List<E> */
  async subList(fromIndex: number, toIndex: number): Promise<IUMLPList<E>> {
    if (this._delegate) {
      return this._delegate.subList(fromIndex, toIndex);
    }
    const result: E[] = this._data.slice(fromIndex, toIndex);
    return <IUMLPList<E>> await new AsyncList<E>().__AsyncListConstructor(result);
  }

  /** java.lang.Iterable#forEach(Consumer<E>) → void */
  async forEach(action: (e: E) => void | Promise<void>): Promise<void> {
    if (this._delegate) {
      return this._delegate.forEach(action);
    }
    for (const item of this._data) {
      await action(item);
    }
  }

  stream(): AsyncStream<E> {
    return AsyncStream.of(this);
  }

  // ─── Still unsupported (return complex non-primitive types) ──────────────

  iterator(): JavaAsyncIterator<E> {
    return new JavaAsyncIteratorAdapter(this[Symbol.asyncIterator]());
  }

  /** @throws — use [Symbol.asyncIterator]() instead */
  listIterator(_index?: number): AsyncIterator<E> {
    throw new Error('listIterator() returns ListIterator<E>. Use [Symbol.asyncIterator]() instead.');
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

  __internalArray() : Array<E> {
    if(this._delegate) {
      return this._delegate.__internalArray();
    }
    return this._data;
  }
}
