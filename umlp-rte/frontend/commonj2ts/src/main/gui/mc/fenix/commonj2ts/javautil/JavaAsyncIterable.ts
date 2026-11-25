import {JavaAsyncIterator} from "./JavaAsyncIterator";

/**
 * The asynchronous counterpart of java.lang.Iterable: the elements are iterated asynchronously,
 * via iterator() (java.lang.Iterable#iterator) or for await (TypeScript's AsyncIterable).
 */
export interface JavaAsyncIterable<E> extends AsyncIterable<E> {

  // java.lang.Iterable#iterator
  iterator(): JavaAsyncIterator<E>;

  // java.lang.Iterable#forEach
  forEach(consumer: (element: E) => void): Promise<void>;

  [Symbol.asyncIterator](): AsyncIterator<E>;

}
