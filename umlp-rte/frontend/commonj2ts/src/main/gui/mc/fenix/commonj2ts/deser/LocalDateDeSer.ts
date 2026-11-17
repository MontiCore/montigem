/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";
import {Temporal} from "../types/Temporal";

/**
 * java.time.LocalDate as Temporal.PlainDate, in the ISO format of the backend (e.g. 2026-11-17), as JSON string
 */
export class LocalDateDeSer implements DeSer<Temporal.PlainDate> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: Temporal.PlainDate): Promise<string> {
    return Promise.resolve('"' + obj.toString() + '"');
  }

  deserialize(str: string): Promise<Temporal.PlainDate> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(Temporal.PlainDate.from(iso));
  }
}
