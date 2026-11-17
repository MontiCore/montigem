/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";
import {Temporal} from "../types/Temporal";

/**
 * java.time.LocalTime as Temporal.PlainTime, in the ISO format of the backend (e.g. 10:30:15.123), as JSON string
 */
export class LocalTimeDeSer implements DeSer<Temporal.PlainTime> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: Temporal.PlainTime): Promise<string> {
    return Promise.resolve('"' + obj.toString() + '"');
  }

  deserialize(str: string): Promise<Temporal.PlainTime> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(Temporal.PlainTime.from(iso));
  }
}
