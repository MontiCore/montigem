import {AsyncList, AsyncSet} from "../javautil";

export class AsyncStream<T> implements AsyncIterable<T> {
  private readonly iterable: AsyncIterable<T>;

  private constructor(iterable: AsyncIterable<T>) {
    this.iterable = iterable;
  }

  public [Symbol.asyncIterator](): AsyncIterator<T> {
    return this.iterable[Symbol.asyncIterator]();
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private static async _mergeSort<T>(arr: T[], cmp: (a: T, b: T) => number | Promise<number>): Promise<T[]> {
    if (arr.length <= 1) return arr;
    const mid = Math.floor(arr.length / 2);
    const left = await AsyncStream._mergeSort(arr.slice(0, mid), cmp);
    const right = await AsyncStream._mergeSort(arr.slice(mid), cmp);
    const result: T[] = [];
    let i = 0, j = 0;
    while (i < left.length && j < right.length) {
      if (await cmp(left[i], right[j]) <= 0) {
        result.push(left[i++]);
      } else {
        result.push(right[j++]);
      }
    }
    while (i < left.length) result.push(left[i++]);
    while (j < right.length) result.push(right[j++]);
    return result;
  }

  private static _fromArray<T>(arr: T[]): AsyncStream<T> {
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        for (const item of arr) {
          yield item;
        }
      }
    });
  }

  private static _fromSyncIterable<T>(iterable: Iterable<T>): AsyncStream<T> {
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        for (const item of iterable) {
          yield item;
        }
      }
    });
  }

  // ─── Static sources ───────────────────────────────────────────────────────

  public static of<T>(iterable: AsyncIterable<T> | Iterable<T>): AsyncStream<T>;
  public static of<T>(array: T[]): AsyncStream<T>;
  public static of<T>(...values: T[]): AsyncStream<T>;
  public static of<T>(...args: any[]): AsyncStream<T> {
    if (args.length === 1) {
      const arg = args[0];
      if (arg != null && typeof arg[Symbol.asyncIterator] === 'function') {
        return new AsyncStream<T>(arg);
      }
      if (Array.isArray(arg)) {
        return AsyncStream._fromArray<T>(arg);
      }
      if (arg != null && typeof arg[Symbol.iterator] === 'function') {
        return AsyncStream._fromSyncIterable<T>(arg);
      }
    }
    return AsyncStream._fromArray<T>(args as T[]);
  }

  public static empty<T>(): AsyncStream<T> {
    return AsyncStream._fromArray<T>([]);
  }

  // ─── Intermediate operations (lazy — no elements are pulled until a terminal operation runs) ───

  public map<R>(mapper: (value: T) => R | Promise<R>): AsyncStream<R> {
    const source = this.iterable;
    return new AsyncStream<R>({
      async *[Symbol.asyncIterator]() {
        for await (const item of source) {
          yield await mapper(item);
        }
      }
    });
  }

  public filter(predicate: (value: T) => boolean | Promise<boolean>): AsyncStream<T> {
    const source = this.iterable;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        for await (const item of source) {
          if (await predicate(item)) yield item;
        }
      }
    });
  }

  public flatMap<R>(mapper: (value: T) => AsyncIterable<R> | Iterable<R> | R | Promise<AsyncIterable<R> | Iterable<R> | R>): AsyncStream<R> {
    const source = this.iterable;
    return new AsyncStream<R>({
      async *[Symbol.asyncIterator]() {
        for await (const item of source) {
          const mapped = await mapper(item);
          if (mapped instanceof AsyncStream) {
            yield* mapped;
          } else if (mapped != null && typeof (mapped as any)[Symbol.asyncIterator] === 'function') {
            yield* mapped as AsyncIterable<R>;
          } else if (Array.isArray(mapped)) {
            yield* mapped;
          } else if (mapped !== null && mapped !== undefined) {
            yield mapped as R;
          }
        }
      }
    });
  }

  public distinct(): AsyncStream<T> {
    const source = this.iterable;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        const seen = new Set<T>();
        for await (const item of source) {
          if (!seen.has(item)) {
            seen.add(item);
            yield item;
          }
        }
      }
    });
  }

  public limit(maxSize: number): AsyncStream<T> {
    const source = this.iterable;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        if (maxSize <= 0) return;
        let count = 0;
        for await (const item of source) {
          yield item;
          if (++count >= maxSize) break;
        }
      }
    });
  }

  public skip(n: number): AsyncStream<T> {
    const source = this.iterable;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        let count = 0;
        for await (const item of source) {
          if (count < n) {
            count++;
          } else {
            yield item;
          }
        }
      }
    });
  }

  public peek(action: (value: T) => any | Promise<any>): AsyncStream<T> {
    const source = this.iterable;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        for await (const item of source) {
          await action(item);
          yield item;
        }
      }
    });
  }

  /** Collects all elements eagerly on first iteration, then sorts. Stays lazy until consumed. */
  public sorted(comparator?: (a: T, b: T) => number | Promise<number>): AsyncStream<T> {
    const source = this.iterable;
    let sortedArr: T[] | null = null;
    return new AsyncStream<T>({
      async *[Symbol.asyncIterator]() {
        if (sortedArr === null) {
          const arr: T[] = [];
          for await (const item of source) {
            arr.push(item);
          }
          sortedArr = comparator
            ? await AsyncStream._mergeSort(arr, comparator)
            : arr.sort();
        }
        yield* sortedArr;
      }
    });
  }

  // ─── Terminal operations (consuming — drive the lazy pipeline and produce a result) ──────────
  // Terminal operations:
  //   toList, toArray, forEach, forEachOrdered, count,
  //   anyMatch, allMatch, noneMatch, min, max,
  //   findFirst, findAny, reduce, collect

  // terminal
  public async toList(): Promise<AsyncList<T>> {
    const result: AsyncList<T> = await new AsyncList<T>().__AsyncListConstructor();
    for await (const item of this.iterable) {
      await result.add(item);
    }
    return result;
  }

  // terminal
  public toArray(): never {
    throw new Error("toArray() not supported use GemListUtil#toArraySync if you need JavaScript arrays.")
  }

  // terminal
  public async forEach(action: (value: T) => any | Promise<any>): Promise<void> {
    for await (const item of this.iterable) {
      await action(item);
    }
  }

  // terminal
  public async forEachOrdered(action: (value: T) => void | Promise<void>): Promise<void> {
    return this.forEach(action);
  }

  // terminal
  public async count(): Promise<number> {
    let n = 0;
    for await (const _ of this.iterable) {
      n++;
    }
    return n;
  }

  // terminal — short-circuits on first match
  public async anyMatch(predicate: (value: T) => boolean | Promise<boolean>): Promise<boolean> {
    for await (const item of this.iterable) {
      if (await predicate(item)) return true;
    }
    return false;
  }

  // terminal — short-circuits on first non-match
  public async allMatch(predicate: (value: T) => boolean | Promise<boolean>): Promise<boolean> {
    for await (const item of this.iterable) {
      if (!await predicate(item)) return false;
    }
    return true;
  }

  // terminal — short-circuits on first match
  public async noneMatch(predicate: (value: T) => boolean | Promise<boolean>): Promise<boolean> {
    for await (const item of this.iterable) {
      if (await predicate(item)) return false;
    }
    return true;
  }

  // terminal
  public async min(comparator: (a: T, b: T) => number | Promise<number>): Promise<T | null> {
    let min: T | null = null;
    let found = false;
    for await (const item of this.iterable) {
      if (!found) {
        min = item; found = true;
      } else if (await comparator(item, min!) < 0) {
        min = item;
      }
    }
    return min;
  }

  // terminal
  public async max(comparator: (a: T, b: T) => number | Promise<number>): Promise<T | null> {
    let max: T | null = null;
    let found = false;
    for await (const item of this.iterable) {
      if (!found) {
        max = item; found = true;
      } else if (await comparator(item, max!) > 0) {
        max = item;
      }
    }
    return max;
  }

  // terminal — short-circuits after the first element
  public async findFirst(): Promise<T | null> {
    for await (const item of this.iterable) {
      return item;
    }
    return null;
  }

  // terminal — short-circuits after the first element
  public async findAny(): Promise<T | null> {
    return this.findFirst();
  }

  // terminal
  // Arg3 is never needed because of sequential implementation?
  public reduce(accumulator: (acc: T, curr: T) => T | Promise<T>): Promise<T | null>;
  public reduce<U>(identity: U, accumulator: (acc: U, curr: T) => U | Promise<U>): Promise<U>;
  public reduce<U>(identity: U, accumulator: (acc: U, curr: T) => U | Promise<U>, combiner: (left: U, right: U) => U | Promise<U>): Promise<U>;
  public async reduce(arg1: any, arg2?: any, arg3?: any): Promise<any> {
    if (arg2 === undefined) {
      let acc: T | null = null;
      let first = true;
      for await (const item of this.iterable) {
        if (first) { acc = item; first = false; }
        else { acc = await arg1(acc, item); }
      }
      return acc;
    } else {
      let acc = arg1;
      for await (const item of this.iterable) {
        acc = await arg2(acc, item);
      }
      return acc;
    }
  }

  // terminal
  public async collect<R, A>(collector: any): Promise<R> {
    if (collector && collector.name === 'toList') {
      return (await this.toList()) as unknown as R;
    }
    if (collector && collector.name === 'toSet') {
      const set = await new AsyncSet<T>().__AsyncSetConstructor();
      for await (const item of this.iterable) {
        set.add(item);
      }
      return set as unknown as R;
    }
    if (collector && collector.name === 'joining') {
      const parts: string[] = [];
      for await (const item of this.iterable) {
        parts.push(String(item));
      }
      return parts.join(collector.delimiter || '') as unknown as R;
    }
    if (collector && collector.name === 'counting') {
      return (await this.count()) as unknown as R;
    }
    return (await this.toList()) as unknown as R;
  }


  // Stream of Optional has to be treated differently
  public static ofOptional<T>(arg: T | null): AsyncStream<T> {
    return AsyncStream._fromArray(arg? [arg] : []);
  }
}

export class Collectors {
  public static toList<T>() {
    return { name: 'toList' };
  }

  public static toSet<T>() {
    return { name: 'toSet' };
  }

  public static joining(delimiter: string = '') {
    return { name: 'joining', delimiter: delimiter };
  }

  public static counting<T>() {
    return { name: 'counting' };
  }
}
