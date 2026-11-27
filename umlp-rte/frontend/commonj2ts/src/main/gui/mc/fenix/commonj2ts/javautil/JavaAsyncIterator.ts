/**
 * The asynchronous counterpart of java.util.Iterator, returned by JavaAsyncIterable#iterator.
 * (Named JavaAsyncIterator as TypeScript's AsyncIterator is a different, built-in interface.)
 */
export interface JavaAsyncIterator<E> {

  // java.util.Iterator#hasNext
  hasNext(): Promise<boolean>;

  // java.util.Iterator#next, rejects if there is no next element (NoSuchElementException)
  next(): Promise<E>;

}

/**
 * A JavaAsyncIterator over a TypeScript AsyncIterator (e.g. [Symbol.asyncIterator]() of a collection):
 * hasNext fetches the next element in advance, next returns it.
 */
export class JavaAsyncIteratorAdapter<E> implements JavaAsyncIterator<E> {

  private fetched?: IteratorResult<E>;

  constructor(private readonly iterator: AsyncIterator<E>) {
  }

  async hasNext(): Promise<boolean> {
    return !(await this.fetch()).done;
  }

  async next(): Promise<E> {
    const result = await this.fetch();
    if (result.done) {
      throw new Error("NoSuchElementException: the iteration has no more elements");
    }
    this.fetched = undefined;
    return result.value;
  }

  private async fetch(): Promise<IteratorResult<E>> {
    if (!this.fetched) {
      this.fetched = await this.iterator.next();
    }
    return this.fetched;
  }

}
