/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from '../command/DeSer';

export class BooleanDeSer implements DeSer<boolean> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __BooleanDeSerConstructor(): Promise<BooleanDeSer> {
    return this;
  }

  public async serialize$java_lang_Boolean(obj: boolean): Promise<string> {
    return "" + obj;
  }

  /**
   *
   * @param {boolean} obj
   * @return {string}
   */
  public async serialize(obj?: any): Promise<string> {
    if (((typeof obj === 'boolean') || obj === null)) {
      return <any>await this.serialize$java_lang_Boolean(obj);
    } else throw new Error('invalid overload');
  }

  /**
   *
   * @param {string} str
   * @return {Optional}
   */
  public async deserialize(str: string): Promise<boolean> {
    const e: any = JSON.parse(str);
    if (typeof e != 'boolean') {
      return null;
    }
    return (v => {
      if (v == null) throw new Error('value is null');
      return v;
    })(e);
  }
}

BooleanDeSer["__class"] = "umlp.backendrte.common.BooleanDeSer";
BooleanDeSer["__interfaces"] = ["umlp.backendrte.command.DeSer"];




