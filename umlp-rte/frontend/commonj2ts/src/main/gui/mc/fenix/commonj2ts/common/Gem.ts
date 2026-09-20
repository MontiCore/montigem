/* (c) https://github.com/MontiCore/monticore */
import {Log} from '../logging/Log';
import {LoadResult} from './LoadResult';


import {LoadingState} from './LoadingState';


import {GemIdentifiable} from './GemIdentifiable';

export class Gem<E extends GemIdentifiable> implements GemIdentifiable {
  /**
   * The object stored
   * is not null iff the object has been loaded ({@link Gem#loadingState} == {@link LoadingState#LOADED})
   */
  object: E;

  /**
   * Contains the gemId of the object. If the object is loaded, gemId == object.getGemId() must hold.
   */
  gemId: number;

  /**
   * whether the object has been loaded or (why) not
   * the object can be {@link LoadingState#LOADED} or {@link LoadingState#NOT_YET_LOADED}
   * alternatively the state indicates that the attempt to load
   * failed for some reason (s. {@link LoadingState})
   */
  loadingState: LoadingState;

  loadFunction: () => LoadResult<E>;

  public constructor() {
    if (this.gemId === undefined) {
      this.gemId = 0;
    }
    if (this.loadingState === undefined) {
      this.loadingState = null;
    }
  }

  public async __GemConstructor(gemId?: any, supplier?: any): Promise<Gem<E>> {
    if (this.gemId === undefined) {
      this.gemId = 0;
    }
    if (this.loadingState === undefined) {
      this.loadingState = null;
    }
    if (((typeof gemId === 'number') || gemId === null) && ((typeof supplier === 'function' && (<any>supplier).length === 0) || supplier === null)) {
      let __args = arguments;
      this.object = null;
      this.loadFunction = <any>(((funcInst: any) => {
        if (funcInst == null || typeof funcInst == 'function') {
          return funcInst
        }
        return () => (funcInst['get'] ? funcInst['get'] : funcInst).call(funcInst)
      })(null));
      this.gemId = gemId;
      this.loadFunction = <any>(((funcInst: any) => {
        if (funcInst == null || typeof funcInst == 'function') {
          return funcInst
        }
        return () => (funcInst['get'] ? funcInst['get'] : funcInst).call(funcInst)
      })(/* requireNonNull */(obj => {
        if (obj == undefined || obj == null) {
          throw Object.defineProperty(new Error(), '__classes', {
            configurable: true,
            value: ['java.lang.Throwable', 'java.lang.Object', 'java.lang.RuntimeException', 'java.lang.NullPointerException', 'java.lang.Exception']
          })
        } else {
          return obj
        }
      })(supplier)));
      this.loadingState = LoadingState.NOT_YET_LOADED;
    } else if (((typeof gemId === 'number') || gemId === null) && ((typeof supplier === 'number') || supplier === null)) {
      let __args = arguments;
      let gemID: any = __args[0];
      let loadingState: any = __args[1];
      this.object = null;
      this.loadFunction = <any>(((funcInst: any) => {
        if (funcInst == null || typeof funcInst == 'function') {
          return funcInst
        }
        return () => (funcInst['get'] ? funcInst['get'] : funcInst).call(funcInst)
      })(null));
      this.gemId = gemID;
      if (loadingState === LoadingState.LOADED) {
        Log.error("0xE2921 Object created with unexpected state");
      }
      this.loadingState = loadingState;
    } else if (((gemId != null) || gemId === null) && supplier === undefined) {
      let __args = arguments;
      let object: any = __args[0];
      this.object = null;
      this.loadFunction = <any>(((funcInst: any) => {
        if (funcInst == null || typeof funcInst == 'function') {
          return funcInst
        }
        return () => (funcInst['get'] ? funcInst['get'] : funcInst).call(funcInst)
      })(null));
      this.object = object;
      this.gemId = object.getGemId();
      this.loadingState = LoadingState.LOADED;
    } else if (gemId === undefined && supplier === undefined) {
      let __args = arguments;
      this.object = null;
      this.loadFunction = <any>(((funcInst: any) => {
        if (funcInst == null || typeof funcInst == 'function') {
          return funcInst
        }
        return () => (funcInst['get'] ? funcInst['get'] : funcInst).call(funcInst)
      })(null));
      this.gemId = -1;
      this.loadFunction = () => {
        throw Object.defineProperty(new Error("Can not load empty Gem!"), '__classes', {
          configurable: true,
          value: ['java.lang.Throwable', 'java.lang.IllegalStateException', 'java.lang.Object', 'java.lang.RuntimeException', 'java.lang.Exception']
        });
      };
      this.loadingState = LoadingState.EMPTY;
    } else throw new Error('invalid overload');
    return this;
  }

  /**
   *
   * @return {LoadingState}
   */
  public getLoadingState(): LoadingState {
    return this.loadingState;
  }

  public isPresent(): Promise<boolean> {
    if (this.loadingState === LoadingState.EMPTY) {
      return Promise.resolve(false);
    }
    return this.loadIfRequired().then(_ => {
      return this.loadingState === LoadingState.LOADED && this.object != null
    });
  }

  public get(): Promise<E> {
    return this.loadIfRequired().then(_ => this.isPresent()).then((isPresent: boolean) => {
      if (!isPresent) {
        Log.error("0xE2921: Internal error. Object was not loaded in Gem with gemId "+this.gemId
          +"; instead LoadingState was " + LoadingState[this.loadingState]?.toString());
      }
      return this.object;
    });
  }

  /** Loads the object if required; Java Optional values are represented as a value or null. */
  public async asOpt(): Promise<E | null> {
    return await this.isPresent() ? this.object : null;
  }

  public set(object: E) {
    if (object != null) {
      this.object = object;
      this.gemId = object.getGemId();
      this.loadingState = LoadingState.LOADED;
    }
  }

  loadPromise: Promise<LoadResult<E>>;
  loadIfRequired(): Promise<LoadResult<E>> {
    if (this.loadPromise === undefined && this.object !== undefined && this.object !== null) {
      const lr = new LoadResult<E>();
      this.loadPromise = lr.__LoadResultConstructor(this.object).then(() => lr);
    }
    else if (this.loadingState === LoadingState.EMPTY) {
      const lr = new LoadResult<E>();
      this.loadPromise = lr.__LoadResultConstructor(LoadingState.EMPTY).then(() => lr);
    }
    else if (this.loadPromise === undefined && this.loadingState === LoadingState.NOT_YET_LOADED) {
      this.loadPromise = Promise.resolve(this.loadFunction()).then((result: LoadResult<E>) => {
        if (result.isPresent()) {
          this.set(result.get());
        } else {
          this.loadingState = result.loadingState;
        }
        return result;
      });
    }
    return this.loadPromise;
  }

  /**
   *
   * @return {number}
   */
  public getGemId(): number {
    return this.gemId;
  }
}

Gem["__class"] = "umlp.backendrte.common.Gem";
Gem["__interfaces"] = ["umlp.backendrte.common.GemIdentifiable"];


