import {GemList} from "../common/GemList";
import {GemIdentifiable} from "../common/GemIdentifiable";
import {CommandManager} from "../command/CommandManager";
import {JavaAsyncIterator} from "../javautil/JavaAsyncIterator";

export class GemListAsyncIterator<E extends GemIdentifiable> implements JavaAsyncIterator<E> {
  private index: number;
  private prefetchedUpTo: number;
  // in-flight background load of the next window; resolves to the new prefetchedUpTo
  private nextPrefetch: Promise<number> | null;

  constructor(private gemList: GemList<E>, private batchSize: number = CommandManager.loadBatchSize) {
    this.index = 0;
    this.prefetchedUpTo = 0;
    this.nextPrefetch = null;
  }

  private startPrefetch(from: number): Promise<number> {
    return (async () => {
      const end = Math.min(from + this.batchSize, await this.gemList.size());
      if (end > from) {
        await this.gemList.loadObjects(await this.gemList.getIDs(from, end));
      }
      return end;
      // on failure keep the old window; the per-index get() will surface the error
    })().catch(() => from);
  }

  async hasNext(): Promise<boolean> {
    return this.index < await this.gemList.size();
  }

  async next(): Promise<E> {
    if (!await this.hasNext()) {
      throw new Error("NoSuchElementException: the iteration has no more elements");
    }
    // cursor left the prefetched window: wait for the in-flight load or start one
    if (this.index >= this.prefetchedUpTo) {
      if (this.nextPrefetch == null) {
        this.nextPrefetch = this.startPrefetch(this.index);
      }
      this.prefetchedUpTo = Math.max(this.prefetchedUpTo, await this.nextPrefetch);
      this.nextPrefetch = null;
    }
    // read-ahead: start loading the next window in the background while iteration continues
    if (this.nextPrefetch == null && this.prefetchedUpTo - this.index <= this.batchSize / 2
        && this.prefetchedUpTo < await this.gemList.size()) {
      this.nextPrefetch = this.startPrefetch(this.prefetchedUpTo);
    }
    const value = await this.gemList.get(this.index);
    ++this.index;
    return value;
  }
}
