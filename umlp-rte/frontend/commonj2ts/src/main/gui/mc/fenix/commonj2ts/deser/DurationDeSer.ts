/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "../types/Temporal";
import {DurationHelper} from "../types/DurationHelper";
import {IsoStringDeSer} from "./IsoStringDeSer";

/** java.time.Duration as Temporal.Duration, in the ISO format of java.time (e.g. PT1H30M, PT-0.5S) */
export class DurationDeSer extends IsoStringDeSer<Temporal.Duration> {

  protected toIso(obj: Temporal.Duration): string {
    return DurationHelper.toString(obj);
  }

  protected fromIso(iso: string): Temporal.Duration {
    return DurationHelper.parse(iso);
  }
}
