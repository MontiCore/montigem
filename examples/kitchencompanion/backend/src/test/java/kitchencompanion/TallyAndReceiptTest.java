package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import java.time.LocalDate;
import java.util.List;

import static kitchencompanion.KitchenCompanionTestData.offer;
import static kitchencompanion.KitchenCompanionTestData.tally;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static helpers of {@link Tally} and the amount calculation of {@link Receipt}.
 */
public class TallyAndReceiptTest extends KitchenCompanionMockTestSetup {

  private static final double EPS = 1e-9;

  public TallyAndReceiptTest() {
    super(ClientSetup.of(0));
  }

  @Test
  public void offersOfKeepsListOrder() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer water = offer("Wasser", 0.75);

    assertEquals(List.of(water, coffee), Tally.offersOf(List.of(tally(water, 0), tally(coffee, 3))));
    assertTrue(Tally.offersOf(List.of()).isEmpty());
  }

  @Test
  public void countForMatchesTheOfferByName() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer water = offer("Wasser", 0.75);
    Offer mate = offer("Mate", 1.0);
    List<Tally> tallies = List.of(tally(coffee, 3), tally(water, 1));

    assertEquals(3, Tally.countFor(tallies, coffee));
    assertEquals(1, Tally.countFor(tallies, water));
    assertEquals(0, Tally.countFor(tallies, mate));
  }

  @Test
  public void compareForDisplay() {
    Offer coffee = offer("Kaffee", 0.25, true, ProductType.DRINK);
    Offer water = offer("Wasser", 0.75, true, ProductType.DRINK);
    Offer peanuts = offer("Erdnüsse", 1.0, true, ProductType.SNACK);

    // higher count first
    assertTrue(Tally.compareForDisplay(tally(water, 5), tally(coffee, 1)) < 0);
    assertTrue(Tally.compareForDisplay(tally(coffee, 1), tally(water, 5)) > 0);
    // same count: drinks before snacks
    assertTrue(Tally.compareForDisplay(tally(water, 1), tally(peanuts, 1)) < 0);
    // same count and type: by name, ignoring case
    assertTrue(Tally.compareForDisplay(tally(coffee, 1), tally(water, 1)) < 0);
    assertEquals(0, Tally.compareForDisplay(tally(coffee, 2), tally(coffee, 2)));
  }

  @Test
  public void amountToPayIsSumPlusDebtMinusBalance() {
    assertEquals(4.5, receipt(3.0, 2.5, 1.0).getAmountToPay(), EPS);
    assertEquals(3.0, receipt(3.0, 0.0, 0.0).getAmountToPay(), EPS);
  }

  @Test
  public void amountToPayIsNegativeWhenTheBalanceExceedsTheBill() {
    assertEquals(-4.0, receipt(1.0, 0.0, 5.0).getAmountToPay(), EPS);
    assertEquals(0.0, receipt(0.0, 0.0, 0.0).getAmountToPay(), EPS);
  }

  @Test
  public void paidFromBalanceWhenTheBalanceCoversTheWholeBill() {
    assertTrue(receipt(1.5, 0.0, 10.0).isPaidFromBalance());
    assertTrue(receipt(1.5, 0.0, 1.5).isPaidFromBalance());
    assertTrue(receipt(1.0, 2.0, 3.0).isPaidFromBalance());
    assertFalse(receipt(3.0, 0.0, 1.0).isPaidFromBalance());
    assertFalse(receipt(1.0, 2.0, 2.5).isPaidFromBalance());
  }

  @Test
  public void endMonthMarksExactlyTheReceiptsItPaidFromTheBalance() {
    Offer mate = offer("Mate", 1.0);
    User rich = KitchenCompanionTestData.user("Tom", "Hanks", "THa", "hanks@example.com", 10.0,
        KitchenCompanionRole.USER);
    User poor = KitchenCompanionTestData.user("Meryl", "Streep", "MSt", "streep@example.com", 1.0,
        KitchenCompanionRole.USER);
    for (User u : List.of(rich, poor)) {
      KitchenCompanionTestData.stock(u, mate);
      u.addTally(mate);
      u.addTally(mate);
      u.endMonth();
    }

    assertTrue(rich.getReceipts(0).isPaid());
    assertTrue(rich.getReceipts(0).isPaidFromBalance());
    assertFalse(poor.getReceipts(0).isPaid());
    assertFalse(poor.getReceipts(0).isPaidFromBalance());
  }

  private static Receipt receipt(double sum, double debt, double pastBalance) {
    return KitchenCompanionManager.receiptBuilder()
        .sum(sum)
        .debt(debt)
        .pastBalance(pastBalance)
        .amountPaid(0.0)
        .confirmed(false)
        .paid(false)
        .date(LocalDate.of(2026, 1, 1))
        .build()
        .get();
  }
}
