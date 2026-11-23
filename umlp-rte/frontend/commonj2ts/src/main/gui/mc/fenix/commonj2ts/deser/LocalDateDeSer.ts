/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "../types/Temporal";
import {IsoStringDeSer} from "./IsoStringDeSer";

/** java.time.LocalDate as Temporal.PlainDate, in the ISO format of the backend (e.g. 2026-11-17) */
export class LocalDateDeSer extends IsoStringDeSer<Temporal.PlainDate> {

  protected toIso(obj: Temporal.PlainDate): string {
    return obj.toString();
  }

  protected fromIso(iso: string): Temporal.PlainDate {
    return Temporal.PlainDate.from(iso);
  }
}
