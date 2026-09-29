package resources.exectests;

import util.JSweetTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * JavaTimeAdapter used to inline the argument of plusDays & co. into a synchronous arrow function
 * ({@code ((d) => { ... res.getUTCDate() + (<argument>) ... })(<target>)}). An argument that
 * transpiles to an {@code await} expression, e.g. an association navigation in generated OCL
 * code, then ended up in a non-async function. {@code CompletableFuture#join} stands in for such
 * an argument here.
 */
public class DateArithmeticAsyncArgumentRuntimeCheck {

  @JSweetTest
  public static boolean checkPlusDaysWithAsyncArgument() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.plusDays(CompletableFuture.completedFuture(1L).join())
        .equals(LocalDate.of(2024, 3, 1));
  }

  /** Shape of the OCL invariant: {@code tag.plusDays(if <async> then 14 - 1 else 8 * 7 - 1)}. */
  @JSweetTest
  public static boolean checkPlusDaysWithAsyncConditionInArgument() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.plusDays(CompletableFuture.completedFuture(true).join() ? (14 - 1) : (8 * 7 - 1))
        .equals(LocalDate.of(2024, 3, 13));
  }

  @JSweetTest
  public static boolean checkLocalDateArithmeticWithAsyncArguments() {
    LocalDate date = LocalDate.of(2024, 2, 29);
    return date.minusDays(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2024, 2, 28))
        && date.plusWeeks(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2024, 3, 7))
        && date.minusWeeks(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2024, 2, 22))
        && date.plusMonths(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2024, 3, 29))
        && date.minusMonths(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2024, 1, 29))
        && date.plusYears(CompletableFuture.completedFuture(1L).join())
            .equals(LocalDate.of(2025, 2, 28))
        && date.minusYears(CompletableFuture.completedFuture(4L).join())
            .equals(LocalDate.of(2020, 2, 29));
  }

  @JSweetTest
  public static boolean checkLocalDateTimeArithmeticWithAsyncArguments() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 23, 30, 0);
    return dateTime.plusHours(CompletableFuture.completedFuture(2L).join())
            .equals(LocalDateTime.of(2024, 3, 1, 1, 30, 0))
        && dateTime.minusHours(CompletableFuture.completedFuture(2L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 21, 30, 0))
        && dateTime.plusMinutes(CompletableFuture.completedFuture(45L).join())
            .equals(LocalDateTime.of(2024, 3, 1, 0, 15, 0))
        && dateTime.minusMinutes(CompletableFuture.completedFuture(30L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 23, 0, 0))
        && dateTime.plusSeconds(CompletableFuture.completedFuture(30L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 23, 30, 30))
        && dateTime.minusSeconds(CompletableFuture.completedFuture(30L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 23, 29, 30))
        && dateTime.plusNanos(CompletableFuture.completedFuture(5_000_000L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 23, 30, 0, 5_000_000))
        && dateTime.minusNanos(CompletableFuture.completedFuture(5_000_000L).join())
            .equals(LocalDateTime.of(2024, 2, 29, 23, 29, 59, 995_000_000));
  }

  /** The amount used to be printed without parentheses for months and years. */
  @JSweetTest
  public static boolean checkMinusMonthsWithCompoundArgument() {
    long one = 1;
    return LocalDate.of(2024, 5, 15).minusMonths(one + 1)
        .equals(LocalDate.of(2024, 3, 15));
  }
}
