/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";
import {Temporal} from "../types/Temporal";

/**
 * java.time.LocalDateTime as Temporal.PlainDateTime, in the ISO format of the backend (e.g. 2026-11-17T10:30:15.123), as JSON string
 */
export class LocalDateTimeDeSer implements DeSer<Temporal.PlainDateTime> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: Temporal.PlainDateTime): Promise<string> {
    return Promise.resolve('"' + obj.toString() + '"');
  }

  deserialize(str: string): Promise<Temporal.PlainDateTime> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(Temporal.PlainDateTime.from(iso));
  }
}
