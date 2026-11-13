/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from '../command/DeSer';

export class StringDeSer implements DeSer<string> {

  public isPojoDeSer(): boolean {
    return false;
  }
  constructor() {
  }

  public async __StringDeSerConstructor(): Promise<StringDeSer> {
    return this;
  }

  public async serialize$java_lang_String(obj: string): Promise<string> {
    return "\"" + obj + "\"";
  }

  /**
   *
   * @param {string} obj
   * @return {string}
   */
  public async serialize(obj?: any): Promise<string> {
    if (((typeof obj === 'string') || obj === null)) {
      return <any>await this.serialize$java_lang_String(obj);
    } else throw new Error('invalid overload');
  }

  /**
   * Simply return the string as in Common
   * @param {string} str
   * @return {Optional}
   */
  public async deserialize(str: string): Promise<string> {
    return (v => {
      if (v == null) throw new Error('value is null');
      return v;
    })(str);
  }
}

StringDeSer["__class"] = "umlp.backendrte.common.StringDeSer";
StringDeSer["__interfaces"] = ["umlp.backendrte.command.DeSer"];




