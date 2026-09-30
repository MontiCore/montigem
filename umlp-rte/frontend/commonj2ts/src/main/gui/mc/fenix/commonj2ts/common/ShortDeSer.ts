/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from '../command/DeSer';

export class ShortDeSer implements DeSer<number> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __ShortDeSerConstructor(): Promise<ShortDeSer> {
    return this;
  }

  public async serialize$java_lang_Short(obj: number): Promise<string> {
    return "" + obj;
  }

  /**
   *
   * @param {number} obj
   * @return {string}
   */
  public async serialize(obj?: any): Promise<string> {
    if (((typeof obj === 'number') || obj === null)) {
      return <any>await this.serialize$java_lang_Short(obj);
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

ShortDeSer["__class"] = "umlp.backendrte.common.ShortDeSer";
ShortDeSer["__interfaces"] = ["umlp.backendrte.command.DeSer"];




