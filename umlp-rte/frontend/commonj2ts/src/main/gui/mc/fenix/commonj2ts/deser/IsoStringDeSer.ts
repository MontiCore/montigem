/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";

/**
 * a value as JSON string in an ISO format, e.g. the java.time values (see LocalDateDeSer): the subclasses convert the
 * value to and from the string
 */
export abstract class IsoStringDeSer<T> implements DeSer<T> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: T): Promise<string> {
    return Promise.resolve('"' + this.toIso(obj) + '"');
  }

  deserialize(str: string): Promise<T> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(this.fromIso(iso));
  }

  protected abstract toIso(obj: T): string;

  protected abstract fromIso(iso: string): T;
}
