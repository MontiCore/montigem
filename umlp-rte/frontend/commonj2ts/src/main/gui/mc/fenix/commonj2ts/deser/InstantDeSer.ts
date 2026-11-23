/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";
import {Temporal} from "../types/Temporal";

/**
 * java.time.Instant as Temporal.Instant, in the ISO format of the backend (e.g. 2026-11-17T09:30:15.123Z), as JSON
 * string
 */
export class InstantDeSer implements DeSer<Temporal.Instant> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: Temporal.Instant): Promise<string> {
    return Promise.resolve('"' + obj.toString() + '"');
  }

  deserialize(str: string): Promise<Temporal.Instant> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(Temporal.Instant.from(iso));
  }
}
