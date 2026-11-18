/* (c) https://github.com/MontiCore/monticore */
import {DeSer} from "../command/DeSer";
import {Temporal} from "../types/Temporal";
import {DurationHelper} from "../types/DurationHelper";

/**
 * java.time.Duration as Temporal.Duration, in the ISO format of java.time (e.g. PT1H30M, PT-0.5S), as JSON string
 */
export class DurationDeSer implements DeSer<Temporal.Duration> {

  public isPojoDeSer(): boolean {
    return false;
  }

  serialize(obj: Temporal.Duration): Promise<string> {
    return Promise.resolve('"' + DurationHelper.toString(obj) + '"');
  }

  deserialize(str: string): Promise<Temporal.Duration> {
    const iso = str.startsWith('"') ? str.slice(1, str.length - 1) : str;
    return Promise.resolve(DurationHelper.parse(iso));
  }
}
