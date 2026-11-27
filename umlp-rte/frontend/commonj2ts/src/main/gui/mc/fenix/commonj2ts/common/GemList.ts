/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {GemListAsyncIterator} from "../common/GemListAsyncIterator";
import {GemIdentifiable} from "../common/GemIdentifiable";
import {GemBuildable} from "../common/GemBuildable";
import {LoadResult} from "../common/LoadResult";
import {LoadingState} from "../common/LoadingState";
import {IUMLPList} from "../common/IUMLPList";
import {AsyncList} from "../javautil/AsyncList";
import {AsyncSet} from "../javautil/AsyncSet";
import {Collection} from "../javautil/Collection";
import {JavaAsyncIterator} from "../javautil/JavaAsyncIterator";
import {AsyncStream} from "../javautil";

export abstract class GemList<E extends GemIdentifiable> implements GemBuildable, IUMLPList<E> {
  protected ids: Array<number> = [];
  protected asArray: Array<E> = null;

  // the state of loading this list
  protected state: LoadingState = LoadingState.NOT_YET_LOADED;

  /**
   * Notify about a change to the GemList (as seen by the toArray)
   */
  protected updateIds() {
    // Pass the toArray to all observers (but call toArray lazy)
    let lazy = undefined;
    // this.subject.next(this.toArray());
  }


  public constructor() {}

  public async __GemListConstructor(ids?: Array<number> | Collection<number>) : Promise<GemList<E>> {
    this.state = LoadingState.NOT_YET_LOADED;
    if (ids instanceof Array) {
      this.ids = ids;
    } else if (typeof ids !== 'undefined' && ids != null) {
      // ids.toArray().then(numbers => this.ids = [...numbers]);
      this.ids = [...await ids.toArrayDeepSync()];
    } else if (typeof ids === 'undefined') {
      this.ids = [];
    }
    return this;
  }

  // Be notified about subscription, so we can load on-demand
  protected _onSub() {
    if (this.state == LoadingState.NOT_YET_LOADED) {
      // if this was the first subscription, load the data
      this.loadAll();
    }
  }

  protected loadAll() {
    this.state = LoadingState.LOADING;
    new AsyncList<number>().__AsyncListConstructor(this.ids).then(x => this.loadObjects(x).then(value => {
      this.state = LoadingState.LOADED;
      this.asArray = [];
      value.forEach(element => {
        if (element.getLoadingState() == LoadingState.LOADED) {
          this.asArray!.push(element.get());
        }
      });

      this.updateIds();
    }))
  }


  /* Default method injected from umlp.backendrte.common.GemBuildable */
  isBuildFinished(): boolean {
    return true;
  }

  /**
   *
   * @return {Optional}
   */
  public build(): Promise<GemList<E>> {
    return Promise.resolve((v => {
      if (v == null) throw new Error('value is null');
      return v;
    })(<GemList<E>>this));
  }

  /**
   *
   * @return {number}
   */
  public async size(): Promise<number> {
    return this.ids.length;
  }

  /**
   *
   * @param {number} idx
   * @return {*}
   */
  public async get(idx: number): Promise<E> {
    if (await this.isNotIdxInInclusiveBounds(idx)) {
      await this.logIndexError("0xAC729", "get", idx);
    }
    const id: number = await this.getID$int(idx);
    const lr: LoadResult<E> = await this.loadSingleObject(id);
    if (lr.isPresent()) {
      return lr.get();
    } else {
      return null;
    }
  }

  /**
   * identical to {@link java.util.List#contains(Object)} but taking a gemID
   *
   * @param {number} gemID the id of the object that may be contained in this list
   * @return {boolean} whether the object corresponding to gemID is contained in this list
   */
  public contains(gemID: number | E): Promise<boolean> {
    if (typeof gemID === 'number') {
      return Promise.resolve(this.ids.includes(gemID));
    } else {
      return Promise.resolve(this.ids.includes(gemID.getGemId()));
    }
  }

  public async containsAll(o: Collection<E>): Promise<boolean> {
    if (await this.size() === 0) {
      return new Promise((resolve) => resolve(true));
    }
    for await (const e of o) {
      if (!this.contains(e)) {
        return false;
      }
    }
    return true;
  }

  public async equals(o: any): Promise<boolean> {
    if (!(o instanceof GemList)) {
      return false;
    }
    if (await this.size() !== await o.size()) {
      return false;
    }
    for (let i = 0; i < await this.size(); i++) {
      if ((await this.get(i)).getGemId() !== (await o.get(i)).getGemId()) {
        return false;
      }
    }
    return true;
  }

  public async hashCode(): Promise<number> {
    let hashCode: number = 1;
    for await (const element of this) {
      hashCode = 31 * hashCode + (!element ? 0 : element.getGemId());
    }
    return hashCode;
  }

  /**
   * provides the objects referenced by the gemIDs if possible
   *
   * @param {number[]} gemIDs reference the objects to get
   * @return {LoadResult[]} the results of trying to load the objects
   */
  abstract loadObjects(gemIDs: IUMLPList<number>): Promise<IUMLPList<LoadResult<E>>>;

  /**
   *
   * @param {number} idx
   * @param {*} elem
   * @return {*}
   */
  public async set(idx: number, elem: E): Promise<E> {
    if (await this.isNotIdxInInclusiveBounds(idx)) {
      await this.logIndexError("0xAC725", "set", idx);
    }
    const oldElem: E = await this.get(idx);
    await this._internal_setAndSync(idx, elem.getGemId());
    return oldElem;
  }

  /**
   * to be overridden by a subclass to add synchronization
   * @param {number} idx
   * @param {number} gemID
   */
  abstract _internal_setAndSync(idx: number, gemID: number);

  public async _internal_set(idx: number, gemID: number) {
    if (await this.isNotIdxInInclusiveBounds(idx)) {
      await this.logIndexError("0xAC726", "_internal_set", idx);
    }
    this.ids[idx] = gemID;

    if (this.asArray != null) {
      this.asArray[idx] = (await this.loadSingleObject(gemID)).get();
    }
    this.updateIds();
  }

  public async addFirst(element : E): Promise<boolean> {
    return await this.add(0, element);
  }

  public async addLast(element : E): Promise<boolean> {
    return await this.add(await this.size(), element);
  }

  public async add(idxOrElem: number | E, elem?: E): Promise<boolean> {
    if ((elem === undefined || elem === null) && (<E>idxOrElem).getGemId) {
      await this._internal_addAndSync(await this.size(), await new AsyncList<number>().__AsyncListConstructor([(<E>idxOrElem).getGemId()]));
    } else if (typeof idxOrElem === 'number') {
      await this._internal_addAndSync(idxOrElem, await new AsyncList<number>().__AsyncListConstructor([elem.getGemId()]));
    }
    return true;
  }

  public async addAll(idxOrElems: number | Collection<E>, elems?: Collection<E>): Promise<boolean> {
    if (elems === undefined || elems === null && Symbol.asyncIterator in Object(idxOrElems)) {
      var gemIds = await (await new AsyncList().__AsyncListConstructor(<Collection<E>>idxOrElems)).stream().map((elem: E) => elem.getGemId()).toList();
      await this._internal_addAndSync(0, await new AsyncList<number>().__AsyncListConstructor(gemIds));
    } else {
      await this._internal_addAndSync(<number>idxOrElems, await (await new AsyncList().__AsyncListConstructor(<Collection<E>>elems)).stream().map((elem: E) => elem.getGemId()).toList());
    }
    return true;
  }

  /**
   * to be overridden by a subclass to add synchronization
   * @param {number} idx
   * @param {number[]} gemIDs
   */
  abstract _internal_addAndSync(idx: number, gemIDs: IUMLPList<number>);

  // TODO: TSAddToListOfDomain.ftl with concrete object reference?
  // right now we have to load the object twice
  public async _internal_add(idx: number, gemIDs: IUMLPList<number>) {
    if (await this.isNotIdxInExclusiveBounds(idx)) {
      await this.logIndexError("0xAC727", "_internal_add", idx);
    }
    this.ids.splice(idx, 0, ...(await gemIDs.toArrayDeepSync()));

    if (this.asArray != null) {
      if (await(gemIDs.size()) != 1) {
        throw Error("Did not expect an internal_add with size of" + await gemIDs.size() + gemIDs);
      }
      const loadResult = await this.loadSingleObject(await gemIDs.get(0));
      if (loadResult.isPresent())
        this.asArray.splice(idx, 0, loadResult.get());
      else
        throw Error("Failed to load single object " + await gemIDs.get(0));
    }
    this.updateIds();
  }

  public async clear(): Promise<void> {
    const size = await this.size();
    if(size > 0) {
      await this.removeRange(0, await this.size());
    }
  }

  /**
   *
   * @param {*} element
   * @return {boolean}
   */
  public async removeElement(element: E): Promise<boolean> {
    let index = await this.indexOf(element);
    if (index > 0) {
      await this._internal_removeAndSync(await new AsyncSet<number>().__AsyncSetConstructor([index]));
      return true;
    } else {
      return false;
    }
  }

  public async removeIndex(index: number): Promise<E> {
    if (index >= 0 && index < await this.size()) {
      const elem: E = await this.get(index);
      await this._internal_removeAndSync(await new AsyncSet<number>().__AsyncSetConstructor([index]));
      return elem;
    } else {
      return Promise.resolve(undefined);
    }
  }

  /* toIndex is exclusive!! */
  public async removeRange(fromIndex: number, toIndex: number) {
    if (toIndex <= fromIndex) {
      throw new Error("Invalid range: toIndex is less equal to fromIndex");
    }
    const indicesToRemove: Array<number> = Array.from({length: toIndex - fromIndex}, (_, i) => i + fromIndex);
    await this._internal_removeAndSync(await new AsyncSet<number>().__AsyncSetConstructor(indicesToRemove));
  }

  // TODO this looks very wrong to me -> removeAll gemIds?
  // Old version: await this._internal_removeAndSync(await gemIDs.map(e => e.getGemId()));
  public async removeAll(elems: Collection<E>): Promise<boolean> {
    for await(const element of elems) {
      await this.removeElement(element)
    }
    return true;
  }

  /**
   * to be overridden by a subclass to add synchronization
   * @param {number[]} idxs
   */
  abstract _internal_removeAndSync(idxs: AsyncSet<number> | Array<number>);

  public async _internal_remove(idxs: AsyncSet<number> | Array<number>) {
    const toSort: Array<number> = [];
    for await (let idx of idxs) {
      toSort.push(idx);
    }
    /* reverse */
    toSort.reverse();
    for (let idx of toSort) {
      /* remove */
      this.ids.splice((<number>idx | 0), 1)[0];
    }

    if (this.asArray != null) {
      for (let idx of toSort) {
        /* remove */
        this.asArray.splice((<number>idx | 0), 1)[0];
      }
    }
    this.updateIds();
  }

  public async isEmpty(): Promise<boolean> {
    return (await this.size()) === 0;
  }

  /**
   * returns if the element is available
   * e.g. one is authorized to access it
   * this is NOT a range check
   * @param {number} idx
   * @return {boolean}
   */
  public async isPresent(idx: number): Promise<boolean> {
    if (await this.isNotIdxInInclusiveBounds(idx)) {
      await this.logIndexError("oxAC728", "isPresent", idx);
    }
    return (await this.loadSingleObject(await this.getID$int(idx))).isPresent();
  }

  public setIDs(changes: any) {
    {
      let array = /* entrySet */((m) => {
        if (m.entries == null) m.entries = [];
        return m.entries;
      })(<any>changes);
      for (let index = 0; index < array.length; index++) {
        let e = array[index];
        {
          /* set */
          (this.ids[e.getKey()] = e.getValue());
        }
      }
    }
  }

  async loadSingleObject(id: number): Promise<LoadResult<E>> {
    const res: IUMLPList<LoadResult<E>> = await this.loadObjects(await new AsyncList<number>().__AsyncListConstructor([id]));
    if ((await res.size()) == 0) {
      return await <any>(new LoadResult<any>().__LoadResultConstructor(LoadingState.FAILED_TO_LOAD));
    }
    return await res.get(0);
  }

  async isNotIdxInInclusiveBounds(idx: number): Promise<boolean> {
    return idx < 0 || idx >= await this.size();
  }

  async isNotIdxInExclusiveBounds(idx: number): Promise<boolean> {
    return idx < 0 || idx > await this.size();
  }

  async logIndexError(errorCode: string, methodName: string, idx: number): Promise<never> {
    Log.error(
      errorCode +
      " GemList::" + methodName + " index out of bounds" +
      ", size: " + await this.size() +
      ", idx: " + idx
    );
    throw Object.defineProperty(new Error(), '__classes', {
      configurable: true,
      value: ['java.lang.Throwable', 'java.lang.IndexOutOfBoundsException', 'java.lang.Object', 'java.lang.RuntimeException', 'java.lang.Exception']
    });
  }

  async assertExclusiveIdxInBounds(idx: number): Promise<void> {
    if (idx < 0 || idx > await this.size()) {
      throw Object.defineProperty(new Error(), '__classes', {
        configurable: true,
        value: ['java.lang.Throwable', 'java.lang.IndexOutOfBoundsException', 'java.lang.Object', 'java.lang.RuntimeException', 'java.lang.Exception']
      });
    }
  }

  public async getID$int(idx: number): Promise<number> {
    return (await this.getIDs$int$int(idx, idx + 1)).get(0);
  }

  public async getIDs$int$int(startIdx: number, endIdx: number): Promise<IUMLPList<number>> {
    return new AsyncList<number>().__AsyncListConstructor(this.ids.slice(startIdx, endIdx));
  }

  public async getIDs(startIdx?: any, endIdx?: any): Promise<IUMLPList<number>> {
    if (((typeof startIdx === 'number') || startIdx === null) && ((typeof endIdx === 'number') || endIdx === null)) {
      return await this.getIDs$int$int(startIdx, endIdx);
    } else if (startIdx === undefined && endIdx === undefined) {
      return await this.getIDs$();
    } else throw new Error('invalid overload');
  }

  public getIDs$(): Promise<IUMLPList<number>> {
    return new AsyncList<number>().__AsyncListConstructor(this.ids);
  }

  public getID$umlp_backendrte_common_GemIdentifiable(elem: E): number {
    return elem.getGemId();
  }

  /**
   * returns the gemID of a given element
   * @param {*} elem
   * @return {number}
   */
  public async getID(elem?: any): Promise<number> {
    if (((typeof elem === 'number') || elem === null)) {
      return await this.getID$int(elem);
    } else if (((elem != null) || elem === null)) {
      return this.getID$umlp_backendrte_common_GemIdentifiable(elem);
    } else throw new Error('invalid overload');
  }

  /**
   * Removes the given gemId from the idStorage.
   * Can only be called if the idStorage contains the gemId, otherwise the size will become invalid.
   * The gemId is not required to be loaded in the idStorage, thus the result of idStorage.removeId must not be returned
   * @param {number} gemId
   */
  public async removeIdSure(gemId: number) {
    const indexToBeRemoved = this.ids.indexOf(gemId);
    await this._internal_removeAndSync(await new AsyncSet<number>().__AsyncSetConstructor([indexToBeRemoved]));
  }

  public indexOfId(gemId: number): number {
    return this.ids.indexOf(gemId);
  }

  public lastIndexOfId(gemId: number): number {
    return this.ids.lastIndexOf(gemId);
  }

  /* toIndex is exclusive; batch-loads the range and returns a snapshot */
  public async subList(fromIndex: number, toIndex: number): Promise<IUMLPList<E>> {
    await this.subListRangeCheck(fromIndex, toIndex);
    const result: E[] = [];
    if (toIndex > fromIndex) {
      await this.loadObjects(await this.getIDs(fromIndex, toIndex));
      for (let i = fromIndex; i < toIndex; i++) {
        result.push(await this.get(i));
      }
    }
    return <IUMLPList<E>> await new AsyncList<E>().__AsyncListConstructor(result);
  }

  async* [Symbol.asyncIterator](): AsyncIterator<E> {
    const iterator = this.iterator();
    while (await iterator.hasNext()) {
      yield await iterator.next();
    }
  }

  public async indexOf(o: any): Promise<number> {
    let i = 0;
    for await (const e of this) {
      if(e === o) {
        return i;
      }
      i++;
    }
    return -1; // TODO
  }

  iterator(): JavaAsyncIterator<E> {
    return new GemListAsyncIterator(this);
  }

  public async lastIndexOf(o: any): Promise<number> {
    let i = 0;
    let lastFind = -1;
    for await (const e of this) {
      if(e === o) {
        lastFind = i;
      }
      i++;
    }
    return lastFind;
  }

  listIterator(from?: any): AsyncIterator<E> {
    return this[Symbol.asyncIterator]();
  }

  public async retainAll(c: Collection<E>): Promise<boolean> {
    throw Error("NoImplementedException for retainAll on GemLists");
    return false; // TODO
  }

  asArrayPromise: Promise<Array<E>> = null;

  async toArrayDeepSync(startIndex?: number, endIndex?: number): Promise<Array<any>> {
    if (this.asArrayPromise == null) {
      this.asArrayPromise = new Promise(async resolve => {
        // TODO Beachtung von startIndex und endIndex
        if (this.asArray === null) {
          this.asArray = [];
          const arr = [];
          await this.loadObjects(await new AsyncList<number>().__AsyncListConstructor(this.ids));
          for await (let gemListElement of this) {
            arr.push(gemListElement);
          }
          this.asArray = arr;
        }
        resolve(this.asArray);
      });
    }
    // this.asArrayPromise.then(v => console.log("Before: ", v));
    this.asArrayPromise = this.asArrayPromise.then(() => this.asArray);
    // this.asArrayPromise.then(v => console.log("After: ", + v));
    return this.asArrayPromise;
  }

  async forEach(consumer: (element: E) => void): Promise<void> {
    for await (const e of this) {
      consumer(e);
    }
  }

  stream(): AsyncStream<E> {
    return AsyncStream.of(this);
  }

  public async sort(comp?: any): Promise<void> {
    const listAsArray = await this.toArrayDeepSync()
    listAsArray.sort(comp)
    // Inspired by java.util.Collection#sort
    for (const [index, element] of listAsArray.entries()) {
      await this.set(index, element)
    }
  }

  private async subListRangeCheck(fromIndex: number, toIndex: number): Promise<void> {
    if (fromIndex < 0 || toIndex > await this.size() || fromIndex > toIndex) {
      throw new Error("Invalid range");
    }
  }

  __internalArray() : Array<E> {
    throw Error("__internalArray is not allowed, since GemLists are always asynchronous. " +
      "Please use GemListUtil#toArraySync if you need a synchronus array.");
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
}

GemList["__class"] = "umlp.backendrte.common.GemList";
GemList["__interfaces"] = ["umlp.backendrte.common.GemBuildable"];


