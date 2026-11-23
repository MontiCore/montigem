/* (c) https://github.com/MontiCore/monticore */

/** the lengths of the time units and the arithmetic shared by the java.time helpers (LocalDateTimeHelper, ...) */
export class TimeUnits {

  static readonly SECONDS_PER_MINUTE = 60;
  static readonly SECONDS_PER_HOUR = 3600;
  static readonly SECONDS_PER_DAY = 86400;
  static readonly MILLIS_PER_SECOND = 1000;
  static readonly NANOS_PER_MICRO = 1000;
  static readonly NANOS_PER_MILLI = 1000000;
  static readonly NANOS_PER_SECOND = 1000000000;

  /** the seconds of nanos, rounded down like Math.floorDiv */
  static secondsOf(nanos: bigint): bigint {
    const nanosPerSecond = BigInt(TimeUnits.NANOS_PER_SECOND);
    const seconds = nanos / nanosPerSecond;
    return nanos % nanosPerSecond < 0n ? seconds - 1n : seconds;
  }

  /** the nanoseconds within the second of nanos, 0 to 999,999,999 */
  static nanoOf(nanos: bigint): bigint {
    const nanosPerSecond = BigInt(TimeUnits.NANOS_PER_SECOND);
    const nano = nanos % nanosPerSecond;
    return nano < 0n ? nano + nanosPerSecond : nano;
  }

  /** n with leading zeros to digits digits, e.g. 07 */
  static pad(n: number, digits = 2): string {
    return String(n).padStart(digits, "0");
  }
}
