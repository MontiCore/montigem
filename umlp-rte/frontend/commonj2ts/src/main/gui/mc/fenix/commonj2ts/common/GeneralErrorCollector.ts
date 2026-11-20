/* (c) https://github.com/MontiCore/monticore */
// import { ServerError } from '../command/ServerError';
import {AsyncList} from '../javautil/AsyncList';
import {IUMLPList} from '../common/IUMLPList';
import {GeneralErrorObserver} from './GeneralErrorObserver';

export class GeneralErrorCollector {
  static instance: GeneralErrorCollector = null;

  observers: IUMLPList<GeneralErrorObserver>;

  constructor() {
  }

  public async __GeneralErrorCollectorConstructor(): Promise<GeneralErrorCollector> {
    this.observers = <any>(await new AsyncList().__AsyncListConstructor());
    return this;
  }

  public static async init() {
    GeneralErrorCollector.instance = new GeneralErrorCollector();
    // Since this class is not transpiled but HW (why?) with a parallel java class,
    // the async constructors of JSweet have to be called as well :( (see monticore/monticore#5271)
    await GeneralErrorCollector.instance.__GeneralErrorCollectorConstructor();
  }

  public static async getInstance(): Promise<GeneralErrorCollector> {
    if (GeneralErrorCollector.instance == null) {
      await GeneralErrorCollector.init();
    }
    return GeneralErrorCollector.instance;
  }

  public static async addErrorObserver(observer: GeneralErrorObserver) {
    await (await GeneralErrorCollector.getInstance())._addErrorObserver(observer);
  }

  public static initMe(generalErrorCollector: GeneralErrorCollector) {
    GeneralErrorCollector.instance = generalErrorCollector;
  }

  public static async containsErrorObserver(observer: GeneralErrorObserver): Promise<boolean> {
    return await (await GeneralErrorCollector.getInstance())._containsErrorObserver(observer);
  }

  public static async removeErrorObserver(observer: GeneralErrorObserver): Promise<boolean> {
    return await (await GeneralErrorCollector.getInstance())._removeErrorObserver(observer);
  }

  public static async reportConnectionError() {
    (await GeneralErrorCollector.getInstance())._reportConnectionError();
  }

  public static async reportServerError(t: Error) {
    (await GeneralErrorCollector.getInstance())._reportServerError(t);
  }

  public static async reportSyncError() {
    (await GeneralErrorCollector.getInstance())._reportSyncError();
  }

  async _addErrorObserver(observer: GeneralErrorObserver) {
    await this.observers.add(observer);
  }

  async _containsErrorObserver(observer: GeneralErrorObserver): Promise<boolean> {
    return await this.observers.contains(observer);
  }

  async _removeErrorObserver(observer: GeneralErrorObserver): Promise<boolean> {
    return await this.observers.removeElement(observer);
  }

  _reportConnectionError() {
    const msg: string = this._buildConnectionErrorMsg();
    this.observers.forEach(((msg) => {
      return (obs) => obs.notifyConnectionError(msg)
    })(msg));
  }

  _buildConnectionErrorMsg(): string {
    return "Connection error!";
  }

  _reportServerError(t: Error) {
    // CommandManager.sendCommandToSubscribers<any>(new ServerError(t));
    const msg: string = this._buildServerErrorMsg(t);
    this.observers.forEach(((msg) => {
      return (obs) => obs.notifyServerError(msg)
    })(msg));
  }

  _buildServerErrorMsg(t: Error): string {
    return "Server error: " + t.message;
  }

  _reportSyncError() {
    // CommandManager.sendCommandToSubscribers<any>(new SyncError());
    const msg: string = this._buildSyncErrorMsg();
    this.observers.forEach(((msg) => {
      return (obs) => obs.notifySyncError(msg)
    })(msg));
  }

  _buildSyncErrorMsg(): string {
    return "Sync error!";
  }
}

GeneralErrorCollector["__class"] = "umlp.backendrte.common.GeneralErrorCollector";


