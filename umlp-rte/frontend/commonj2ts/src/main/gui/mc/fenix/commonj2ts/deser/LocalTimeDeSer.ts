/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "../types/Temporal";
import {IsoStringDeSer} from "./IsoStringDeSer";

/** java.time.LocalTime as Temporal.PlainTime, in the ISO format of the backend (e.g. 10:30:15.123) */
export class LocalTimeDeSer extends IsoStringDeSer<Temporal.PlainTime> {

  protected toIso(obj: Temporal.PlainTime): string {
    return obj.toString();
  }

  protected fromIso(iso: string): Temporal.PlainTime {
    return Temporal.PlainTime.from(iso);
  }
}
