/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "./Temporal";
import {DurationHelper} from "./DurationHelper";
import {ZonedDateTimeHelper} from "./ZonedDateTimeHelper";
import {JavaUtilUtils} from "../javautil/JavaUtilUtils";

/** the ISO formats of java.time (DateTimeFormatter.ISO_LOCAL_DATE, ISO_LOCAL_TIME); Temporal's from takes more */
const ISO_DATE = "(?:\\d{4}|[+-]\\d{4,10})-\\d{2}-\\d{2}";
const ISO_TIME = "\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?";

type DateOrDateTime = Temporal.PlainDate | Temporal.PlainDateTime;
type TimeOrDateTime = Temporal.PlainTime | Temporal.PlainDateTime;
// also a ZonedDateTime, for the getters
type WithDate = DateOrDateTime | Temporal.ZonedDateTime;
type WithTime = TimeOrDateTime | Temporal.ZonedDateTime;

/**
 * java.time methods of LocalDate, LocalDateTime, LocalTime and Instant that have no direct counterpart in the Temporal
 * API, used by the transpiled code (see JavaTimeAdapter)
 */
export class LocalDateTimeHelper {

  static readonly DAYS_OF_WEEK = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

  private static readonly EPOCH = new Temporal.PlainDate(1970, 1, 1);

  static readonly MONTHS = ["JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER",
    "OCTOBER", "NOVEMBER", "DECEMBER"];

  /** LocalDateTime.of(year, month, day, hour, minute[, second[, nanoOfSecond]]) */
  static ofDateTime(year: number, month: number, day: number, hour: number, minute: number, second = 0,
                    nanoOfSecond = 0): Temporal.PlainDateTime {
    return new Temporal.PlainDateTime(year, month, day, hour, minute, second, ...LocalDateTimeHelper.split(nanoOfSecond));
  }

  /** LocalTime.of(hour, minute[, second[, nanoOfSecond]]) */
  static ofTime(hour: number, minute: number, second = 0, nanoOfSecond = 0): Temporal.PlainTime {
    return new Temporal.PlainTime(hour, minute, second, ...LocalDateTimeHelper.split(nanoOfSecond));
  }

  /** LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset) */
  static ofEpochSecond(epochSecond: number, nanoOfSecond = 0, offset = "Z"): Temporal.PlainDateTime {
    const nanos = BigInt(epochSecond) * 1000000000n + BigInt(nanoOfSecond);
    return Temporal.Instant.fromEpochNanoseconds(nanos).toZonedDateTimeISO(ZonedDateTimeHelper.tz(offset))
      .toPlainDateTime();
  }

  /** Instant.ofEpochSecond(epochSecond[, nanoAdjustment]) */
  static instantOfEpochSecond(epochSecond: number, nanoAdjustment = 0): Temporal.Instant {
    return Temporal.Instant.fromEpochNanoseconds(BigInt(epochSecond) * 1000000000n + BigInt(nanoAdjustment));
  }

  /** Instant.getEpochSecond() */
  static getEpochSecond(instant: Temporal.Instant): number {
    return Math.floor(instant.epochMilliseconds / 1000);
  }

  /** LocalDateTime.toEpochSecond(offset), ZonedDateTime.toEpochSecond() */
  static toEpochSecond(dateTime: Temporal.PlainDateTime | Temporal.ZonedDateTime, offset = "Z"): number {
    const zoned = dateTime instanceof Temporal.ZonedDateTime ? dateTime
      : dateTime.toZonedDateTime(ZonedDateTimeHelper.tz(offset));
    return Math.floor(zoned.epochMilliseconds / 1000);
  }

  /** getNano() */
  static getNano(time: WithTime | Temporal.Instant): number {
    if (time instanceof Temporal.Instant) {
      return Number(((time.epochNanoseconds % 1000000000n) + 1000000000n) % 1000000000n);
    }
    return time.millisecond * 1000000 + time.microsecond * 1000 + time.nanosecond;
  }

  /** getDayOfWeek(), the name of the DayOfWeek */
  static getDayOfWeek(date: WithDate): string {
    return LocalDateTimeHelper.DAYS_OF_WEEK[date.dayOfWeek % 7];
  }

  /** getMonth(), the name of the Month */
  static getMonth(date: WithDate): string {
    return LocalDateTimeHelper.MONTHS[date.month - 1];
  }

  /** LocalDate.parse(text): the format of java.time only (Temporal also takes e.g. a date-time or 20240101) */
  static parseDate(text: string): Temporal.PlainDate {
    return Temporal.PlainDate.from(LocalDateTimeHelper.checkFormat(text, ISO_DATE));
  }

  /** LocalDateTime.parse(text), see parseDate */
  static parseDateTime(text: string): Temporal.PlainDateTime {
    return Temporal.PlainDateTime.from(LocalDateTimeHelper.checkFormat(text, ISO_DATE + "[Tt]" + ISO_TIME));
  }

  /** LocalTime.parse(text), see parseDate */
  static parseTime(text: string): Temporal.PlainTime {
    return Temporal.PlainTime.from(LocalDateTimeHelper.checkFormat(text, ISO_TIME));
  }

  private static checkFormat(text: string, format: string): string {
    if (text == null || !new RegExp("^" + format + "$").test(text)) {
      throw new RangeError("Text '" + text + "' could not be parsed");
    }
    return text;
  }

  /**
   * String(value) like Java for a value of a static type that may be a java.time value (Object, a type parameter):
   * a java.time value in the format of java.time, as Temporal values refuse the conversion to a string
   */
  static toJavaString(value: any): string {
    return JavaUtilUtils.isTemporal(value) ? LocalDateTimeHelper.toString(value) : String(value);
  }

  /** withNano(nanoOfSecond) of a LocalDateTime, LocalTime or ZonedDateTime; a value out of range is rejected */
  static withNano<T extends WithTime>(time: T, nanoOfSecond: number): T {
    if (nanoOfSecond < 0 || nanoOfSecond > 999999999) {
      throw new RangeError("Invalid value for NanoOfSecond: " + nanoOfSecond);
    }
    const [millisecond, microsecond, nanosecond] = LocalDateTimeHelper.split(nanoOfSecond);
    return (time as any).with({millisecond, microsecond, nanosecond});
  }

  /** LocalTime.toSecondOfDay() */
  static toSecondOfDay(time: Temporal.PlainTime): number {
    return time.hour * 3600 + time.minute * 60 + time.second;
  }

  /** LocalTime.ofSecondOfDay(secondOfDay) */
  static ofSecondOfDay(secondOfDay: number): Temporal.PlainTime {
    if (secondOfDay < 0 || secondOfDay >= 86400) {
      throw new RangeError("Invalid value for SecondOfDay: " + secondOfDay);
    }
    return new Temporal.PlainTime(Math.floor(secondOfDay / 3600), Math.floor(secondOfDay / 60) % 60, secondOfDay % 60);
  }

  /** LocalTime.toNanoOfDay() */
  static toNanoOfDay(time: Temporal.PlainTime): number {
    return LocalDateTimeHelper.toSecondOfDay(time) * 1000000000 + LocalDateTimeHelper.getNano(time);
  }

  /** LocalTime.ofNanoOfDay(nanoOfDay) */
  static ofNanoOfDay(nanoOfDay: number): Temporal.PlainTime {
    const nanos = BigInt(nanoOfDay);
    if (nanos < 0n || nanos >= 86400n * 1000000000n) {
      throw new RangeError("Invalid value for NanoOfDay: " + nanoOfDay);
    }
    const second = Number(nanos / 1000000000n);
    return LocalDateTimeHelper.ofTime(Math.floor(second / 3600), Math.floor(second / 60) % 60, second % 60,
      Number(nanos % 1000000000n));
  }

  /** LocalDate.toEpochDay(): the days since 1970-01-01 */
  static toEpochDay(date: Temporal.PlainDate): number {
    return LocalDateTimeHelper.EPOCH.until(date).days;
  }

  /** LocalDate.ofEpochDay(epochDay) */
  static ofEpochDay(epochDay: number): Temporal.PlainDate {
    return LocalDateTimeHelper.EPOCH.add({days: epochDay});
  }

  /** withDayOfYear(dayOfYear); a day beyond the year is rejected like in java.time */
  static withDayOfYear<T extends WithDate>(date: T, dayOfYear: number): T {
    if (dayOfYear < 1 || dayOfYear > date.daysInYear) {
      throw new RangeError("Invalid day of year " + dayOfYear + " for the year " + date.year);
    }
    return date.with({month: 1, day: 1}).add({days: dayOfYear - 1}) as T;
  }

  /**
   * toString() like java.time: seconds only if not 0 (always for Instant, in UTC with Z), the fraction in groups of 3
   * digits; null for null, which string concatenation prints as "null" like Java
   */
  static toString(value: Temporal.PlainDate | WithTime | Temporal.Instant | Temporal.Duration): string {
    if (value == null) {
      return null;
    }
    if (value instanceof Temporal.ZonedDateTime) {
      // the local date-time, the offset and, if it is no offset, the zone, e.g. 2026-11-17T09:00+01:00[Europe/Berlin]
      const offset = ZonedDateTimeHelper.getOffset(value);
      const zone = ZonedDateTimeHelper.getZone(value);
      return LocalDateTimeHelper.toString(value.toPlainDateTime()) + offset + (zone !== offset ? "[" + zone + "]" : "");
    }
    if (value instanceof Temporal.Duration) {
      return DurationHelper.toString(value);
    }
    if (value instanceof Temporal.PlainDate) {
      return value.toString();
    }
    if (value instanceof Temporal.Instant) {
      const dateTime = value.toZonedDateTimeISO("UTC").toPlainDateTime();
      return dateTime.toPlainDate().toString() + "T" + LocalDateTimeHelper.timeString(dateTime.toPlainTime(), true)
        + "Z";
    }
    const time = value instanceof Temporal.PlainDateTime ? value.toPlainTime() : value;
    const s = LocalDateTimeHelper.timeString(time, false);
    return value instanceof Temporal.PlainDateTime ? value.toPlainDate().toString() + "T" + s : s;
  }

  /** milliseconds since 1970 of a wall time taken as UTC, for libraries that work with Date (e.g. charts) */
  static toEpochMilliseconds(value: DateOrDateTime): number {
    const dateTime = value instanceof Temporal.PlainDate ? value.toPlainDateTime() : value;
    return dateTime.toZonedDateTime("UTC").epochMilliseconds;
  }

  /** a Date whose local fields show the wall time, for libraries that work with Date (e.g. date pickers) */
  static toDate(value: DateOrDateTime): Date {
    const t = value instanceof Temporal.PlainDate ? value.toPlainDateTime() : value;
    return new Date(t.year, t.month - 1, t.day, t.hour, t.minute, t.second, t.millisecond);
  }

  /** the wall time shown by the local fields of a Date, see toDate */
  static fromDate(date: Date): Temporal.PlainDateTime {
    return new Temporal.PlainDateTime(date.getFullYear(), date.getMonth() + 1, date.getDate(), date.getHours(),
      date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  /** HH:mm[:ss[.fraction]] like java.time, the seconds always if withSeconds */
  private static timeString(time: Temporal.PlainTime, withSeconds: boolean): string {
    const pad = (n: number, digits = 2) => String(n).padStart(digits, "0");
    let s = pad(time.hour) + ":" + pad(time.minute);
    const nano = LocalDateTimeHelper.getNano(time);
    if (withSeconds || time.second > 0 || nano > 0) {
      s += ":" + pad(time.second);
      if (nano > 0) {
        s += "." + (nano % 1000000 === 0 ? pad(nano / 1000000, 3)
          : nano % 1000 === 0 ? pad(nano / 1000, 6) : pad(nano, 9));
      }
    }
    return s;
  }

  /** nanoOfSecond as millisecond, microsecond, nanosecond */
  private static split(nanoOfSecond: number): [number, number, number] {
    return [Math.floor(nanoOfSecond / 1000000), Math.floor(nanoOfSecond / 1000) % 1000, nanoOfSecond % 1000];
  }
}
