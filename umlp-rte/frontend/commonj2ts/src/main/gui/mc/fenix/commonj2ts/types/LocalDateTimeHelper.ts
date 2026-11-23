/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "./Temporal";

type DateOrDateTime = Temporal.PlainDate | Temporal.PlainDateTime;
type TimeOrDateTime = Temporal.PlainTime | Temporal.PlainDateTime;

/**
 * java.time methods of LocalDate, LocalDateTime and LocalTime that have no direct counterpart in the Temporal API,
 * used by the transpiled code (see JavaTimeAdapter)
 */
export class LocalDateTimeHelper {

  static readonly DAYS_OF_WEEK = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

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

  /** LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset), for the offset UTC */
  static ofEpochSecond(epochSecond: number, nanoOfSecond = 0): Temporal.PlainDateTime {
    const nanos = BigInt(epochSecond) * 1000000000n + BigInt(nanoOfSecond);
    return Temporal.Instant.fromEpochNanoseconds(nanos).toZonedDateTimeISO("UTC").toPlainDateTime();
  }

  /** LocalDateTime.toEpochSecond(offset), for the offset UTC */
  static toEpochSecond(dateTime: Temporal.PlainDateTime): number {
    return Math.floor(dateTime.toZonedDateTime("UTC").epochMilliseconds / 1000);
  }

  /** getNano() */
  static getNano(time: TimeOrDateTime): number {
    return time.millisecond * 1000000 + time.microsecond * 1000 + time.nanosecond;
  }

  /** getDayOfWeek(), the name of the DayOfWeek */
  static getDayOfWeek(date: DateOrDateTime): string {
    return LocalDateTimeHelper.DAYS_OF_WEEK[date.dayOfWeek % 7];
  }

  /** getMonth(), the name of the Month */
  static getMonth(date: DateOrDateTime): string {
    return LocalDateTimeHelper.MONTHS[date.month - 1];
  }

  /** withDayOfYear(dayOfYear) */
  static withDayOfYear<T extends DateOrDateTime>(date: T, dayOfYear: number): T {
    return date.with({month: 1, day: 1}).add({days: dayOfYear - 1}) as T;
  }

  /**
   * toString() like java.time: seconds only if not 0, the fraction in groups of 3 digits; null for null, which
   * string concatenation prints as "null" like Java
   */
  static toString(value: Temporal.PlainDate | TimeOrDateTime): string {
    if (value == null) {
      return null;
    }
    if (value instanceof Temporal.PlainDate) {
      return value.toString();
    }
    const time = value instanceof Temporal.PlainDateTime ? value.toPlainTime() : value;
    const pad = (n: number, digits = 2) => String(n).padStart(digits, "0");
    let s = pad(time.hour) + ":" + pad(time.minute);
    const nano = LocalDateTimeHelper.getNano(time);
    if (time.second > 0 || nano > 0) {
      s += ":" + pad(time.second);
      if (nano > 0) {
        s += "." + (nano % 1000000 === 0 ? pad(nano / 1000000, 3)
          : nano % 1000 === 0 ? pad(nano / 1000, 6) : pad(nano, 9));
      }
    }
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

  /** nanoOfSecond as millisecond, microsecond, nanosecond */
  private static split(nanoOfSecond: number): [number, number, number] {
    return [Math.floor(nanoOfSecond / 1000000), Math.floor(nanoOfSecond / 1000) % 1000, nanoOfSecond % 1000];
  }
}
