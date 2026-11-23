/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "./Temporal";
import {TimeUnits} from "./TimeUnits";

const OFFSET = /^([+-])(\d{1,2})(?::?(\d{2}))?(?::?(\d{2}))?$/;

/** the largest offset like in Java, 18 hours */
const MAX_OFFSET_SECONDS = 18 * TimeUnits.SECONDS_PER_HOUR;

/**
 * java.time.ZoneId, ZoneOffset and the parts of ZonedDateTime that differ from the Temporal API, used by the transpiled
 * code (see JavaTimeAdapter): a ZoneId is its id like in Java ("Europe/Berlin", "UTC", "+02:00", "Z" for the offset
 * zero), a ZonedDateTime a Temporal.ZonedDateTime
 */
export class ZonedDateTimeHelper {

  /** the Temporal time zone of a zone id of Java ("Z" is the offset zero) */
  static tz(zone: string): string {
    return zone === "Z" ? "+00:00" : zone;
  }

  /** ZoneId.of(id): an offset is normalized like ZoneOffset.of, a region is checked */
  static zoneOf(id: string): string {
    if (id === "Z" || OFFSET.test(id)) {
      return ZonedDateTimeHelper.offsetOf(id);
    }
    // throws a RangeError for an unknown zone, like ZoneId.of
    new Temporal.ZonedDateTime(0n, id);
    return id;
  }

  /** ZoneId.normalized(): a region without any offset change becomes its offset, like Java (UTC is Z) */
  static normalized(zone: string): string {
    if (zone === "Z" || OFFSET.test(zone)) {
      return zone;
    }
    const start = new Temporal.ZonedDateTime(0n, zone);
    if (start.getTimeZoneTransition("next") !== null || start.getTimeZoneTransition("previous") !== null) {
      return zone;
    }
    return ZonedDateTimeHelper.ofTotalSeconds(start.offsetNanoseconds / TimeUnits.NANOS_PER_SECOND);
  }

  /** ZoneId.systemDefault(): the zone of the browser */
  static systemDefault(): string {
    return Temporal.Now.timeZoneId();
  }

  /** ZoneOffset.of(id), e.g. "+02:00", "-5", "+0530" */
  static offsetOf(id: string): string {
    if (id === "Z") {
      return "Z";
    }
    const m = OFFSET.exec(id);
    if (m == null) {
      throw new RangeError("Invalid ID for ZoneOffset: " + id);
    }
    return ZonedDateTimeHelper.ofTotalSeconds(ZonedDateTimeHelper.secondsOf(m));
  }

  static ofHours(hours: number): string {
    return ZonedDateTimeHelper.ofTotalSeconds(hours * TimeUnits.SECONDS_PER_HOUR);
  }

  static ofHoursMinutes(hours: number, minutes: number): string {
    return ZonedDateTimeHelper.ofTotalSeconds(hours * TimeUnits.SECONDS_PER_HOUR
      + minutes * TimeUnits.SECONDS_PER_MINUTE);
  }

  static ofHoursMinutesSeconds(hours: number, minutes: number, seconds: number): string {
    return ZonedDateTimeHelper.ofTotalSeconds(hours * TimeUnits.SECONDS_PER_HOUR
      + minutes * TimeUnits.SECONDS_PER_MINUTE + seconds);
  }

  /** ZoneOffset.ofTotalSeconds(seconds): "Z" for zero, otherwise +hh:mm[:ss] like Java */
  static ofTotalSeconds(seconds: number): string {
    if (Math.abs(seconds) > MAX_OFFSET_SECONDS) {
      throw new RangeError("Zone offset not in valid range: -18:00 to +18:00");
    }
    if (seconds === 0) {
      return "Z";
    }
    const abs = Math.abs(seconds);
    const minutes = Math.floor(abs % TimeUnits.SECONDS_PER_HOUR / TimeUnits.SECONDS_PER_MINUTE);
    const rest = abs % TimeUnits.SECONDS_PER_MINUTE;
    return (seconds < 0 ? "-" : "+") + TimeUnits.pad(Math.floor(abs / TimeUnits.SECONDS_PER_HOUR)) + ":"
      + TimeUnits.pad(minutes) + (rest !== 0 ? ":" + TimeUnits.pad(rest) : "");
  }

  /** ZoneOffset.getTotalSeconds() */
  static getTotalSeconds(offset: string): number {
    if (offset === "Z") {
      return 0;
    }
    return ZonedDateTimeHelper.secondsOf(OFFSET.exec(offset));
  }

  /** the total seconds of an offset matched by OFFSET */
  private static secondsOf(m: RegExpExecArray): number {
    const seconds = Number(m[2]) * TimeUnits.SECONDS_PER_HOUR + Number(m[3] ?? 0) * TimeUnits.SECONDS_PER_MINUTE
      + Number(m[4] ?? 0);
    return m[1] === "-" ? -seconds : seconds;
  }

  /** ZonedDateTime.getZone(): the zone id like in Java */
  static getZone(dateTime: Temporal.ZonedDateTime): string {
    const zone = dateTime.timeZoneId;
    return OFFSET.test(zone) ? ZonedDateTimeHelper.offsetOf(zone) : zone;
  }

  /** ZonedDateTime.getOffset(): the offset like in Java */
  static getOffset(dateTime: Temporal.ZonedDateTime): string {
    return ZonedDateTimeHelper.offsetOf(dateTime.offset);
  }

  /** ZonedDateTime.parse(text): also without a zone in brackets, e.g. 2026-11-17T09:00+01:00 or 2026-11-17T08:00Z */
  static parse(text: string): Temporal.ZonedDateTime {
    if (text.includes("[")) {
      // an offset that does not fit the zone keeps the instant, like Java
      return Temporal.ZonedDateTime.from(text, {offset: "use"});
    }
    const offset = /(Z|[+-]\d{2}:\d{2}(?::\d{2})?)$/i.exec(text);
    if (offset == null) {
      throw new RangeError("Text cannot be parsed to a ZonedDateTime: " + text);
    }
    return Temporal.ZonedDateTime.from(text + "[" + ZonedDateTimeHelper.tz(offset[1].toUpperCase()) + "]");
  }

  /** ZonedDateTime.withZoneSameLocal(zone): the local date-time in zone, keeping the offset where it is valid */
  static withZoneSameLocal(dateTime: Temporal.ZonedDateTime, zone: string): Temporal.ZonedDateTime {
    const local = dateTime.toPlainDateTime();
    return Temporal.ZonedDateTime.from({
      year: local.year, month: local.month, day: local.day, hour: local.hour, minute: local.minute,
      second: local.second, millisecond: local.millisecond, microsecond: local.microsecond,
      nanosecond: local.nanosecond, timeZone: ZonedDateTimeHelper.tz(zone), offset: dateTime.offset
    }, {offset: "prefer"});
  }

  /** compareTo like Java: by the instant, then the local date-time, then the zone id */
  static compareTo(a: Temporal.ZonedDateTime, b: Temporal.ZonedDateTime): number {
    const byInstant = Temporal.ZonedDateTime.compare(a, b);
    if (byInstant !== 0) {
      return byInstant;
    }
    const byLocal = Temporal.PlainDateTime.compare(a.toPlainDateTime(), b.toPlainDateTime());
    if (byLocal !== 0) {
      return byLocal;
    }
    const zoneA = ZonedDateTimeHelper.getZone(a);
    const zoneB = ZonedDateTimeHelper.getZone(b);
    return zoneA < zoneB ? -1 : zoneA > zoneB ? 1 : 0;
  }
}
