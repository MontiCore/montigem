/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "./Temporal";
import {TimeUnits} from "./TimeUnits";

const NANOS_PER_SECOND = BigInt(TimeUnits.NANOS_PER_SECOND);
const SECONDS_PER_MINUTE = BigInt(TimeUnits.SECONDS_PER_MINUTE);
const SECONDS_PER_HOUR = BigInt(TimeUnits.SECONDS_PER_HOUR);
const SECONDS_PER_DAY = BigInt(TimeUnits.SECONDS_PER_DAY);

/** the estimated length of a year like Java (365.2425 days), in seconds */
const SECONDS_PER_ESTIMATED_YEAR = 31556952n;

/** the nanoseconds of the ChronoUnits of exact length (Duration.of, plus(amount, unit) on times) */
const UNIT_NANOS: { [unit: string]: bigint } = {
  NANOS: 1n, MICROS: BigInt(TimeUnits.NANOS_PER_MICRO), MILLIS: BigInt(TimeUnits.NANOS_PER_MILLI),
  SECONDS: NANOS_PER_SECOND, MINUTES: SECONDS_PER_MINUTE * NANOS_PER_SECOND, HOURS: SECONDS_PER_HOUR * NANOS_PER_SECOND,
  HALF_DAYS: SECONDS_PER_DAY / 2n * NANOS_PER_SECOND, DAYS: SECONDS_PER_DAY * NANOS_PER_SECOND
};

/** the Temporal rounding of truncatedTo(unit): the smallest unit and its increment */
const TRUNCATION_UNITS: { [unit: string]: [string, number] } = {
  NANOS: ["nanosecond", 1], MICROS: ["microsecond", 1], MILLIS: ["millisecond", 1], SECONDS: ["second", 1],
  MINUTES: ["minute", 1], HOURS: ["hour", 1], HALF_DAYS: ["hour", 12], DAYS: ["day", 1]
};

/** ChronoUnit.getDuration(): the estimated lengths of the date-based units like Java, in seconds */
const ESTIMATED_SECONDS: { [unit: string]: bigint } = {
  WEEKS: 7n * SECONDS_PER_DAY, MONTHS: SECONDS_PER_ESTIMATED_YEAR / 12n, YEARS: SECONDS_PER_ESTIMATED_YEAR,
  DECADES: 10n * SECONDS_PER_ESTIMATED_YEAR, CENTURIES: 100n * SECONDS_PER_ESTIMATED_YEAR,
  MILLENNIA: 1000n * SECONDS_PER_ESTIMATED_YEAR
};

/** the Temporal field and factor of the date-based ChronoUnits */
const CALENDAR_UNITS: { [unit: string]: [string, number] } = {
  DAYS: ["days", 1], WEEKS: ["weeks", 1], MONTHS: ["months", 1], YEARS: ["years", 1], DECADES: ["years", 10],
  CENTURIES: ["years", 100], MILLENNIA: ["years", 1000]
};

/**
 * java.time.Duration on Temporal.Duration, used by the transpiled code (see JavaTimeAdapter): a duration is kept
 * balanced from hours to nanoseconds, so that equal durations have equal fields, and computed exactly in nanoseconds
 * like Java. Also the ChronoUnit methods: plus(amount, unit), minus(amount, unit) and unit.between(start, end).
 */
export class DurationHelper {

  static readonly ZERO: Temporal.Duration = new Temporal.Duration();

  /** the duration of nanos nanoseconds, balanced from hours to nanoseconds */
  static ofNanos(nanos: number | bigint): Temporal.Duration {
    let rest = BigInt(nanos);
    const sign = rest < 0n ? -1n : 1n;
    rest *= sign;
    const fields: Array<number> = [];
    for (const size of [1000n, 1000n, 1000n, 60n, 60n]) {
      fields.push(Number(sign * (rest % size)));
      rest /= size;
    }
    const [ns, us, ms, s, min] = fields;
    return new Temporal.Duration(0, 0, 0, 0, Number(sign * rest), min, s, ms, us, ns);
  }

  /** toNanos(): the total nanoseconds (without years, months and weeks), exact within 2^53, about 104 days */
  static toNanos(duration: Temporal.Duration): number {
    return Number(DurationHelper.nanosOf(duration));
  }

  static ofDays(days: number): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(days) * UNIT_NANOS["DAYS"]);
  }

  static ofHours(hours: number): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(hours) * UNIT_NANOS["HOURS"]);
  }

  static ofMinutes(minutes: number): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(minutes) * UNIT_NANOS["MINUTES"]);
  }

  static ofSeconds(seconds: number, nanoAdjustment = 0): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(seconds) * NANOS_PER_SECOND + BigInt(nanoAdjustment));
  }

  static ofMillis(millis: number): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(millis) * UNIT_NANOS["MILLIS"]);
  }

  /** Duration.of(amount, unit), for the units of exact length up to DAYS */
  static of(amount: number, unit: string): Temporal.Duration {
    return DurationHelper.ofNanos(BigInt(amount) * DurationHelper.unitNanos(unit));
  }

  /** Duration.between(start, end) of two LocalDateTimes, LocalTimes or Instants */
  static between(start: any, end: any): Temporal.Duration {
    return DurationHelper.ofNanos(DurationHelper.nanosBetween(start, end));
  }

  /** Duration.parse(text): the ISO format of java.time, e.g. PT1H30M, -PT0.5S, PT-1M-30S or P2DT3H */
  static parse(text: string): Temporal.Duration {
    const m = /^([-+]?)P(?:([-+]?\d+)D)?(T(?:([-+]?\d+)H)?(?:([-+]?\d+)M)?(?:([-+]?\d+)(?:[.,](\d{0,9}))?S)?)?$/i
      .exec(text);
    if (m == null || (m[2] === undefined && m[3] === undefined)
      || (m[3] !== undefined && m[4] === undefined && m[5] === undefined && m[6] === undefined)) {
      throw new RangeError("Text cannot be parsed to a Duration: " + text);
    }
    const part = (value: string, unit: bigint) => value === undefined ? 0n : BigInt(value) * unit;
    let nanos = part(m[2], UNIT_NANOS["DAYS"]) + part(m[4], UNIT_NANOS["HOURS"]) + part(m[5], UNIT_NANOS["MINUTES"])
      + part(m[6], NANOS_PER_SECOND);
    if (m[7] !== undefined && m[7].length > 0) {
      // the fraction has the sign of the seconds
      const fraction = BigInt(m[7].padEnd(9, "0"));
      nanos += m[6].startsWith("-") ? -fraction : fraction;
    }
    return DurationHelper.ofNanos(m[1] === "-" ? -nanos : nanos);
  }

  static plus(duration: Temporal.Duration, other: Temporal.Duration): Temporal.Duration {
    return DurationHelper.ofNanos(DurationHelper.nanosOf(duration) + DurationHelper.nanosOf(other));
  }

  static minus(duration: Temporal.Duration, other: Temporal.Duration): Temporal.Duration {
    return DurationHelper.ofNanos(DurationHelper.nanosOf(duration) - DurationHelper.nanosOf(other));
  }

  static plusDays(duration: Temporal.Duration, days: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofDays(days));
  }

  static plusHours(duration: Temporal.Duration, hours: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofHours(hours));
  }

  static plusMinutes(duration: Temporal.Duration, minutes: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofMinutes(minutes));
  }

  static plusSeconds(duration: Temporal.Duration, seconds: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofSeconds(seconds));
  }

  static plusMillis(duration: Temporal.Duration, millis: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofMillis(millis));
  }

  static plusNanos(duration: Temporal.Duration, nanos: number): Temporal.Duration {
    return DurationHelper.plus(duration, DurationHelper.ofNanos(nanos));
  }

  static minusDays(duration: Temporal.Duration, days: number): Temporal.Duration {
    return DurationHelper.plusDays(duration, -days);
  }

  static minusHours(duration: Temporal.Duration, hours: number): Temporal.Duration {
    return DurationHelper.plusHours(duration, -hours);
  }

  static minusMinutes(duration: Temporal.Duration, minutes: number): Temporal.Duration {
    return DurationHelper.plusMinutes(duration, -minutes);
  }

  static minusSeconds(duration: Temporal.Duration, seconds: number): Temporal.Duration {
    return DurationHelper.plusSeconds(duration, -seconds);
  }

  static minusMillis(duration: Temporal.Duration, millis: number): Temporal.Duration {
    return DurationHelper.plusMillis(duration, -millis);
  }

  static minusNanos(duration: Temporal.Duration, nanos: number): Temporal.Duration {
    return DurationHelper.plusNanos(duration, -nanos);
  }

  static multipliedBy(duration: Temporal.Duration, factor: number): Temporal.Duration {
    return DurationHelper.ofNanos(DurationHelper.nanosOf(duration) * BigInt(factor));
  }

  /** dividedBy(divisor) and dividedBy(Duration), truncated like Java */
  static dividedBy(duration: Temporal.Duration, divisor: number | Temporal.Duration): any {
    if (divisor instanceof Temporal.Duration) {
      return Number(DurationHelper.nanosOf(duration) / DurationHelper.nanosOf(divisor));
    }
    return DurationHelper.ofNanos(DurationHelper.nanosOf(duration) / BigInt(divisor));
  }

  static negated(duration: Temporal.Duration): Temporal.Duration {
    return DurationHelper.ofNanos(-DurationHelper.nanosOf(duration));
  }

  static abs(duration: Temporal.Duration): Temporal.Duration {
    return duration.sign < 0 ? DurationHelper.negated(duration) : duration;
  }

  static isPositive(duration: Temporal.Duration): boolean {
    return duration.sign > 0;
  }

  static isZero(duration: Temporal.Duration): boolean {
    return duration.sign === 0;
  }

  static isNegative(duration: Temporal.Duration): boolean {
    return duration.sign < 0;
  }

  /** getSeconds(): the seconds rounded down, like Java (the nanoseconds are never negative) */
  static getSeconds(duration: Temporal.Duration): number {
    return Number(TimeUnits.secondsOf(DurationHelper.nanosOf(duration)));
  }

  /** getNano(): the nanoseconds within the second, 0 to 999,999,999 */
  static getNano(duration: Temporal.Duration): number {
    return Number(TimeUnits.nanoOf(DurationHelper.nanosOf(duration)));
  }

  static toDays(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / SECONDS_PER_DAY);
  }

  static toHours(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / SECONDS_PER_HOUR);
  }

  static toMinutes(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / SECONDS_PER_MINUTE);
  }

  static toSeconds(duration: Temporal.Duration): number {
    return DurationHelper.getSeconds(duration);
  }

  /** toDaysPart() to toNanosPart(), like Java: from the seconds rounded down and the nanoseconds of the second */
  static toDaysPart(duration: Temporal.Duration): number {
    return DurationHelper.toDays(duration);
  }

  static toHoursPart(duration: Temporal.Duration): number {
    return DurationHelper.toHours(duration) % 24;
  }

  static toMinutesPart(duration: Temporal.Duration): number {
    return DurationHelper.toMinutes(duration) % 60;
  }

  static toSecondsPart(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) % SECONDS_PER_MINUTE);
  }

  static toMillisPart(duration: Temporal.Duration): number {
    return Math.floor(DurationHelper.getNano(duration) / TimeUnits.NANOS_PER_MILLI);
  }

  static toNanosPart(duration: Temporal.Duration): number {
    return DurationHelper.getNano(duration);
  }

  /** toMillis(): the whole milliseconds, truncated towards zero like Java */
  static toMillis(duration: Temporal.Duration): number {
    return Number(DurationHelper.nanosOf(duration) / UNIT_NANOS["MILLIS"]);
  }

  static compareTo(duration: Temporal.Duration, other: Temporal.Duration): number {
    const difference = DurationHelper.nanosOf(duration) - DurationHelper.nanosOf(other);
    return difference < 0n ? -1 : difference > 0n ? 1 : 0;
  }

  /** toString() like java.time, e.g. PT1H30M, PT0.5S, PT-0.5S, PT-1M-30S, PT0S */
  static toString(duration: Temporal.Duration): string {
    if (duration == null) {
      return null;
    }
    const total = DurationHelper.nanosOf(duration);
    const seconds = TimeUnits.secondsOf(total);
    const nanos = TimeUnits.nanoOf(total);
    if (total === 0n) {
      return "PT0S";
    }
    const effectiveSeconds = seconds < 0n && nanos > 0n ? seconds + 1n : seconds;
    const hours = effectiveSeconds / SECONDS_PER_HOUR;
    const minutes = (effectiveSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
    const secs = effectiveSeconds % SECONDS_PER_MINUTE;
    let s = "PT" + (hours !== 0n ? hours + "H" : "") + (minutes !== 0n ? minutes + "M" : "");
    if (secs === 0n && nanos === 0n && s.length > 2) {
      return s;
    }
    s += seconds < 0n && nanos > 0n && secs === 0n ? "-0" : String(secs);
    if (nanos > 0n) {
      const fraction = String(seconds < 0n ? 2n * NANOS_PER_SECOND - nanos : nanos + NANOS_PER_SECOND);
      s += "." + fraction.substring(1).replace(/0+$/, "");
    }
    return s + "S";
  }

  /** plus(amount, unit) of a LocalDate, LocalDateTime, LocalTime, Instant or Duration */
  static plusUnits<T>(value: T, amount: number, unit: string): T {
    const v: any = value;
    if (v instanceof Temporal.Duration) {
      return DurationHelper.plus(v, DurationHelper.of(amount, unit)) as any;
    }
    DurationHelper.checkTimeUnit(v, unit);
    const nanos = UNIT_NANOS[unit];
    if (v instanceof Temporal.Instant || (nanos !== undefined && nanos < UNIT_NANOS["DAYS"]
      && !(v instanceof Temporal.PlainDate))) {
      return v.add(DurationHelper.ofNanos(BigInt(amount) * DurationHelper.unitNanos(unit)));
    }
    const [field, factor] = DurationHelper.calendarUnit(unit);
    return v.add({[field]: amount * factor});
  }

  /** minus(amount, unit), see plusUnits */
  static minusUnits<T>(value: T, amount: number, unit: string): T {
    return DurationHelper.plusUnits(value, -amount, unit);
  }

  /**
   * truncatedTo(unit) of a LocalDateTime, LocalTime, Instant, ZonedDateTime or Duration: the smaller fields set to
   * zero, for units up to DAYS like Java (an Instant towards the past, a Duration towards zero within the day)
   */
  static truncatedTo<T>(value: T, unit: string): T {
    const v: any = value;
    const truncation = TRUNCATION_UNITS[unit];
    if (truncation === undefined) {
      throw new RangeError("Unit is too large to be used for truncation: " + unit);
    }
    if (v instanceof Temporal.Duration) {
      // like Duration.truncatedTo: the nanoseconds of the day, truncated to the unit
      const total = DurationHelper.nanosOf(v);
      const nanoOfDay = (TimeUnits.secondsOf(total) % SECONDS_PER_DAY) * NANOS_PER_SECOND + TimeUnits.nanoOf(total);
      const unitNanos = DurationHelper.unitNanos(unit);
      return DurationHelper.ofNanos(total + nanoOfDay / unitNanos * unitNanos - nanoOfDay) as any;
    }
    const [smallestUnit, increment] = truncation;
    if (unit === "DAYS" && v instanceof Temporal.PlainTime) {
      return new Temporal.PlainTime() as any;
    }
    if (unit === "DAYS" && v instanceof Temporal.Instant) {
      return v.round({smallestUnit: "hour", roundingIncrement: 24, roundingMode: "floor"}) as any;
    }
    return v.round({smallestUnit, roundingIncrement: increment,
      roundingMode: v instanceof Temporal.Instant ? "floor" : "trunc"});
  }

  /** ChronoUnit.getDuration(): exact for the units up to DAYS, estimated for the longer ones like Java */
  static getDuration(unit: string): Temporal.Duration {
    const estimated = ESTIMATED_SECONDS[unit];
    return estimated !== undefined ? DurationHelper.ofNanos(estimated * NANOS_PER_SECOND)
      : DurationHelper.ofNanos(DurationHelper.unitNanos(unit));
  }

  /** ChronoUnit.toString(): the display name like Java, e.g. "Days" or "HalfDays" for HALF_DAYS */
  static unitToString(unit: string): string {
    if (unit == null) {
      return null;
    }
    return unit.split("_").map(part => part.charAt(0) + part.substring(1).toLowerCase()).join("");
  }

  /** ChronoUnit.valueOf(name): the name of a constant */
  static unitOf(name: string): string {
    if (UNIT_NANOS[name] === undefined && CALENDAR_UNITS[name] === undefined) {
      throw new RangeError("No enum constant java.time.temporal.ChronoUnit." + name);
    }
    return name;
  }

  /**
   * unit.between(start, end): the whole units from start to end, truncated like Java; the date-based units of a
   * date-time count its local date-time (a ZonedDateTime in the zone of start, a day across a DST change is a day)
   */
  static unitsBetween(unit: string, start: any, end: any): number {
    DurationHelper.checkTimeUnit(start, unit);
    const nanos = UNIT_NANOS[unit];
    const zoned = start instanceof Temporal.ZonedDateTime;
    if (nanos !== undefined && !(start instanceof Temporal.PlainDate) && !(zoned && unit === "DAYS")) {
      return Number(DurationHelper.nanosBetween(start, end) / nanos);
    }
    if (start instanceof Temporal.PlainDate) {
      return DurationHelper.dateUnitsBetween(unit, start, end);
    }
    if (start instanceof Temporal.Instant) {
      throw new RangeError("Unsupported unit: " + unit);
    }
    const from: Temporal.PlainDateTime = zoned ? start.toPlainDateTime() : start;
    const to: Temporal.PlainDateTime = zoned ? end.withTimeZone(start.timeZoneId).toPlainDateTime() : end;
    // like LocalDateTime.until: the end date counts only if its time is reached
    let endDate = to.toPlainDate();
    const byDate = Temporal.PlainDate.compare(endDate, from.toPlainDate());
    const byTime = Temporal.PlainTime.compare(to.toPlainTime(), from.toPlainTime());
    if (byDate > 0 && byTime < 0) {
      endDate = endDate.subtract({days: 1});
    } else if (byDate < 0 && byTime > 0) {
      endDate = endDate.add({days: 1});
    }
    return DurationHelper.dateUnitsBetween(unit, from.toPlainDate(), endDate);
  }

  /** the date-based units between two dates like LocalDate.until: a month counts once its day of month is reached */
  private static dateUnitsBetween(unit: string, start: Temporal.PlainDate, end: Temporal.PlainDate): number {
    const [field, factor] = DurationHelper.calendarUnit(unit);
    if (field === "days" || field === "weeks") {
      const days = start.until(end, {largestUnit: "days"}).days;
      return Math.trunc(days / (field === "weeks" ? 7 : 1) / factor);
    }
    const packed = (date: Temporal.PlainDate) => ((date.year * 12 + date.month) * 32 + date.day);
    const months = Math.trunc((packed(end) - packed(start)) / 32);
    return Math.trunc(months / (field === "years" ? 12 * factor : factor));
  }

  private static nanosBetween(start: any, end: any): bigint {
    if (start instanceof Temporal.Instant) {
      return end.epochNanoseconds - start.epochNanoseconds;
    }
    return DurationHelper.nanosOf(start.until(end, {largestUnit: "hours"}));
  }

  private static nanosOf(duration: Temporal.Duration): bigint {
    return ((((((BigInt(duration.days) * 24n + BigInt(duration.hours)) * 60n + BigInt(duration.minutes)) * 60n
      + BigInt(duration.seconds)) * 1000n + BigInt(duration.milliseconds)) * 1000n
      + BigInt(duration.microseconds)) * 1000n) + BigInt(duration.nanoseconds);
  }

  private static unitNanos(unit: string): bigint {
    const nanos = UNIT_NANOS[unit];
    if (nanos === undefined) {
      throw new RangeError("Unit must not have an estimated duration: " + unit);
    }
    return nanos;
  }

  /** a LocalTime supports the units up to HALF_DAYS only, like Java (Temporal ignores days on a PlainTime) */
  private static checkTimeUnit(value: any, unit: string): void {
    const nanos = UNIT_NANOS[unit];
    if (value instanceof Temporal.PlainTime && (nanos === undefined || nanos >= UNIT_NANOS["DAYS"])) {
      throw new RangeError("Unsupported unit: " + unit);
    }
  }

  private static calendarUnit(unit: string): [string, number] {
    const calendarUnit = CALENDAR_UNITS[unit];
    if (calendarUnit === undefined) {
      throw new RangeError("Unsupported unit: " + unit);
    }
    return calendarUnit;
  }
}
