/* (c) https://github.com/MontiCore/monticore */

/**
 * java.time operations on the browser representation of LocalDate and LocalDateTime.
 *
 * A LocalDate or LocalDateTime is a JS Date whose local calendar fields carry the value
 * (`new Date(y, m - 1, d)` and `new Date(y, m - 1, d, h, min, s, ms)`), as created by
 * LocalDateDeSer and LocalDateTimeDeSer. All operations read the local fields and do calendar
 * arithmetic on them, so they behave like java.time independently of the browser's time zone and
 * of DST changes. Results are built with the local Date constructor.
 *
 * Known limit: a LocalDateTime inside the hour skipped at the start of DST has no local Date; the
 * Date constructor moves it forward by the DST offset.
 */
export class LocalDateTimeHelper {

  static readonly DAYS_OF_WEEK = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

  static getDayOfWeek(date: Date): string {
    return this.DAYS_OF_WEEK[date.getDay()];
  }

  /** The date with the given year and month (0-11, may be out of range), day clamped as in java.time. */
  private static withYearMonth(date: Date, year: number, month: number): Date {
    const total = year * 12 + month;
    const y = Math.floor(total / 12);
    const m = total - y * 12;
    const day = Math.min(date.getDate(), new Date(y, m + 1, 0).getDate());
    return new Date(y, m, day, date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  // === Plus Methods ===

  static plusYears(date: Date, n: number): Date {
    return this.withYearMonth(date, date.getFullYear() + n, date.getMonth());
  }

  static plusMonths(date: Date, n: number): Date {
    return this.withYearMonth(date, date.getFullYear(), date.getMonth() + n);
  }

  static plusWeeks(date: Date, n: number): Date {
    return this.plusDays(date, n * 7);
  }

  static plusDays(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + n,
      date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static plusHours(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(),
      date.getHours() + n, date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static plusMinutes(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(),
      date.getHours(), date.getMinutes() + n, date.getSeconds(), date.getMilliseconds());
  }

  static plusSeconds(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(),
      date.getHours(), date.getMinutes(), date.getSeconds() + n, date.getMilliseconds());
  }

  // === Minus Methods (delegating to Plus with -n) ===

  static minusYears(date: Date, n: number): Date {
    return this.plusYears(date, -n);
  }

  static minusMonths(date: Date, n: number): Date {
    return this.plusMonths(date, -n);
  }

  static minusWeeks(date: Date, n: number): Date {
    return this.plusWeeks(date, -n);
  }

  static minusDays(date: Date, n: number): Date {
    return this.plusDays(date, -n);
  }

  static minusHours(date: Date, n: number): Date {
    return this.plusHours(date, -n);
  }

  static minusMinutes(date: Date, n: number): Date {
    return this.plusMinutes(date, -n);
  }

  static minusSeconds(date: Date, n: number): Date {
    return this.plusSeconds(date, -n);
  }

  static setHours(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), n, date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static setMinutes(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), n, date.getSeconds(), date.getMilliseconds());
  }

  static setSeconds(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), n, date.getMilliseconds());
  }

  static setMilliseconds(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds(), n);
  }

  static atStartOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  static atEndOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
  }

  static withYear(date: Date, n: number): Date {
    return this.withYearMonth(date, n, date.getMonth());
  }

  /** @param n month 1-12, as in java.time */
  static withMonth(date: Date, n: number): Date {
    return this.withYearMonth(date, date.getFullYear(), n - 1);
  }

  static withDayOfYear(date: Date, n: number): Date {
    return new Date(date.getFullYear(), 0, n, date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }


  static withDayOfMonth(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), n, date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }


  static withHour(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), n, date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static withMinute(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), n, date.getSeconds(), date.getMilliseconds());
  }

  static withSecond(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), n, date.getMilliseconds());
  }

}
