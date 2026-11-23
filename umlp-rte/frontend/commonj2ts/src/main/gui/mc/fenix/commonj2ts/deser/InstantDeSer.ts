/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "../types/Temporal";
import {IsoStringDeSer} from "./IsoStringDeSer";

/** java.time.Instant as Temporal.Instant, in the ISO format of the backend (e.g. 2026-11-17T09:30:15.123Z) */
export class InstantDeSer extends IsoStringDeSer<Temporal.Instant> {

  protected toIso(obj: Temporal.Instant): string {
    return obj.toString();
  }

  protected fromIso(iso: string): Temporal.Instant {
    return Temporal.Instant.from(iso);
  }
}
