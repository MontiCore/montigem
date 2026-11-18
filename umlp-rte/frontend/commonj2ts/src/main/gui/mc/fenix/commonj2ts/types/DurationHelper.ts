/* (c) https://github.com/MontiCore/monticore */
import {Temporal} from "./Temporal";

const NANOS_PER_SECOND = 1000000000n;

/** the nanoseconds of the ChronoUnits of exact length (Duration.of, plus(amount, unit) on times) */
const UNIT_NANOS: { [unit: string]: bigint } = {
  NANOS: 1n, MICROS: 1000n, MILLIS: 1000000n, SECONDS: NANOS_PER_SECOND, MINUTES: 60n * NANOS_PER_SECOND,
  HOURS: 3600n * NANOS_PER_SECOND, HALF_DAYS: 43200n * NANOS_PER_SECOND, DAYS: 86400n * NANOS_PER_SECOND
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

  static isZero(duration: Temporal.Duration): boolean {
    return duration.sign === 0;
  }

  static isNegative(duration: Temporal.Duration): boolean {
    return duration.sign < 0;
  }

  /** getSeconds(): the seconds rounded down, like Java (the nanoseconds are never negative) */
  static getSeconds(duration: Temporal.Duration): number {
    return Number(DurationHelper.secondsOf(DurationHelper.nanosOf(duration)));
  }

  /** getNano(): the nanoseconds within the second, 0 to 999,999,999 */
  static getNano(duration: Temporal.Duration): number {
    return Number(DurationHelper.nanoOf(DurationHelper.nanosOf(duration)));
  }

  static toDays(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / 86400n);
  }

  static toHours(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / 3600n);
  }

  static toMinutes(duration: Temporal.Duration): number {
    return Number(BigInt(DurationHelper.getSeconds(duration)) / 60n);
  }

  static toSeconds(duration: Temporal.Duration): number {
    return DurationHelper.getSeconds(duration);
  }

  static toMillis(duration: Temporal.Duration): number {
    const nanos = DurationHelper.nanosOf(duration);
    return Number(DurationHelper.secondsOf(nanos) * 1000n + DurationHelper.nanoOf(nanos) / 1000000n);
  }

  static compareTo(duration: Temporal.Duration, other: Temporal.Duration): number {
    const difference = DurationHelper.nanosOf(duration) - DurationHelper.nanosOf(other);
    return difference < 0n ? -1 : difference > 0n ? 1 : 0;
  }

  static equals(duration: Temporal.Duration, other: any): boolean {
    return other instanceof Temporal.Duration && DurationHelper.compareTo(duration, other) === 0;
  }

  /** toString() like java.time, e.g. PT1H30M, PT0.5S, PT-0.5S, PT-1M-30S, PT0S */
  static toString(duration: Temporal.Duration): string {
    if (duration == null) {
      return null;
    }
    const total = DurationHelper.nanosOf(duration);
    const seconds = DurationHelper.secondsOf(total);
    const nanos = DurationHelper.nanoOf(total);
    if (total === 0n) {
      return "PT0S";
    }
    const effectiveSeconds = seconds < 0n && nanos > 0n ? seconds + 1n : seconds;
    const hours = effectiveSeconds / 3600n;
    const minutes = (effectiveSeconds % 3600n) / 60n;
    const secs = effectiveSeconds % 60n;
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

  /** unit.between(start, end): the whole units from start to end, truncated like Java */
  static unitsBetween(unit: string, start: any, end: any): number {
    const nanos = UNIT_NANOS[unit];
    if (nanos !== undefined && !(start instanceof Temporal.PlainDate)) {
      return Number(DurationHelper.nanosBetween(start, end) / nanos);
    }
    const [field, factor] = DurationHelper.calendarUnit(unit);
    const difference = start.until(end, {largestUnit: field, smallestUnit: field, roundingMode: "trunc"});
    return Math.trunc(difference[field] / factor);
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

  /** the seconds of nanos rounded down */
  private static secondsOf(nanos: bigint): bigint {
    const seconds = nanos / NANOS_PER_SECOND;
    return nanos % NANOS_PER_SECOND < 0n ? seconds - 1n : seconds;
  }

  /** the nanoseconds within the second, 0 to 999,999,999 */
  private static nanoOf(nanos: bigint): bigint {
    const nano = nanos % NANOS_PER_SECOND;
    return nano < 0n ? nano + NANOS_PER_SECOND : nano;
  }

  private static unitNanos(unit: string): bigint {
    const nanos = UNIT_NANOS[unit];
    if (nanos === undefined) {
      throw new RangeError("Unit must not have an estimated duration: " + unit);
    }
    return nanos;
  }

  private static calendarUnit(unit: string): [string, number] {
    const calendarUnit = CALENDAR_UNITS[unit];
    if (calendarUnit === undefined) {
      throw new RangeError("Unsupported unit: " + unit);
    }
    return calendarUnit;
  }
}
