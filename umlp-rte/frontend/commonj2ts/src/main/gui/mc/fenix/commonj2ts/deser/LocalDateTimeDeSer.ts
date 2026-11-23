/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "../types/Temporal";
import {IsoStringDeSer} from "./IsoStringDeSer";

/** java.time.LocalDateTime as Temporal.PlainDateTime, in the ISO format of the backend (e.g. 2026-11-17T10:30:15) */
export class LocalDateTimeDeSer extends IsoStringDeSer<Temporal.PlainDateTime> {

  protected toIso(obj: Temporal.PlainDateTime): string {
    return obj.toString();
  }

  protected fromIso(iso: string): Temporal.PlainDateTime {
    return Temporal.PlainDateTime.from(iso);
  }
}
