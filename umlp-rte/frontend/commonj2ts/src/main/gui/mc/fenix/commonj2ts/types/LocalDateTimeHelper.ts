/* (c) https://github.com/MontiCore/monticore */

export class LocalDateTimeHelper {

  static readonly DAYS_OF_WEEK = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

  static getDayOfWeek(date: Date): string {
    return this.DAYS_OF_WEEK[date.getUTCDay()];
  }

  // === Plus Methods ===

  static plusYears(date: Date, n: number): Date {
    return new Date(
      date.getFullYear() + n,
      date.getMonth(),
      date.getDate(),
      date.getHours(),
      date.getMinutes(),
      date.getSeconds(),
      date.getMilliseconds()
    );
  }

  static plusMonths(date: Date, n: number): Date {
    return new Date(
      date.getFullYear(),
      date.getMonth() + n,
      date.getDate(),
      date.getHours(),
      date.getMinutes(),
      date.getSeconds(),
      date.getMilliseconds()
    );
  }

  static plusWeeks(date: Date, n: number): Date {
    return new Date(date.getTime() + n * 7 * 24 * 60 * 60 * 1000);
  }

  static plusDays(date: Date, n: number): Date {
    return new Date(date.getTime() + n * 24 * 60 * 60 * 1000);
  }

  static plusHours(date: Date, n: number): Date {
    return new Date(date.getTime() + n * 60 * 60 * 1000);
  }

  static plusMinutes(date: Date, n: number): Date {
    return new Date(date.getTime() + n * 60 * 1000);
  }

  static plusSeconds(date: Date, n: number): Date {
    return new Date(date.getTime() + n * 1000);
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
    return new Date(n, date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static withMonth(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static withDayOfYear(date: Date, n: number): Date {
    return new Date(date.getFullYear(), 0, n, date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }


  static withDayOfMonth(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), n, date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }


  static withHour(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDay(), n, date.getMinutes(), date.getSeconds(), date.getMilliseconds());
  }

  static withMinute(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDay(), date.getHours(), n, date.getSeconds(), date.getMilliseconds());
  }

  static withSecond(date: Date, n: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes(), n, date.getMilliseconds());
  }

}
