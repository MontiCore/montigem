/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from '../command/DeSer';

export class IntegerDeSer implements DeSer<number> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __IntegerDeSerConstructor(): Promise<IntegerDeSer> {
    return this;
  }

  public async serialize$java_lang_Integer(obj: number): Promise<string> {
    return "" + obj;
  }

  /**
   *
   * @param {number} obj
   * @return {string}
   */
  public async serialize(obj?: any): Promise<string> {
    if (((typeof obj === 'number') || obj === null)) {
      return <any>await this.serialize$java_lang_Integer(obj);
    } else throw new Error('invalid overload');
  }

  /**
   *
   * @param {string} str
   * @return {Optional}
   */
  public async deserialize(str: string): Promise<number> {
    const e: any = JSON.parse(str);
    if (typeof e != 'number') {
      return null;
    }
    return (v => {
      if (v == null) throw new Error('value is null');
      return v;
    })(e);
  }
}

IntegerDeSer["__class"] = "umlp.backendrte.common.IntegerDeSer";
IntegerDeSer["__interfaces"] = ["umlp.backendrte.command.DeSer"];




