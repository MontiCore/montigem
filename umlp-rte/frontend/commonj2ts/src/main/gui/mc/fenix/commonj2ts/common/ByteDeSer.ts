/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from '../command/DeSer';

export class ByteDeSer implements DeSer<number> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __ByteDeSerConstructor(): Promise<ByteDeSer> {
    return this;
  }

  public async serialize$java_lang_Byte(obj: number): Promise<string> {
    return "" + obj;
  }

  /**
   *
   * @param {number} obj
   * @return {string}
   */
  public async serialize(obj?: any): Promise<string> {
    if (((typeof obj === 'number') || obj === null)) {
      return <any>await this.serialize$java_lang_Byte(obj);
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

ByteDeSer["__class"] = "umlp.backendrte.common.ByteDeSer";
ByteDeSer["__interfaces"] = ["umlp.backendrte.command.DeSer"];




