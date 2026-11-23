/* (c) https://github.com/MontiCore/monticore */

/**
 * The Temporal API of transpiled java.time values: LocalDate is a Temporal.PlainDate, LocalDateTime a
 * Temporal.PlainDateTime, LocalTime a Temporal.PlainTime, Instant a Temporal.Instant, Duration a Temporal.Duration,
 * ZonedDateTime a Temporal.ZonedDateTime. From the polyfill, as not every browser has Temporal yet.
 */
export {Temporal} from "@js-temporal/polyfill";
