package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import java.time.LocalDate;
import java.util.List;

import static kitchencompanion.KitchenCompanionTestData.offer;
import static kitchencompanion.KitchenCompanionTestData.stock;
import static kitchencompanion.KitchenCompanionTestData.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Monthly billing ({@link User#endMonth()}): when a receipt is created, how the balance is used
 * and how unpaid amounts are carried over to the next month.
 */
public class UserEndMonthTest extends KitchenCompanionMockTestSetup {

  private static final double EPS = 1e-9;

  private Offer coffee;
  private Offer mate;

  public UserEndMonthTest() {
    super(ClientSetup.of(0));
  }

  @Override
  public void _init() throws Exception {
    super._init();
    coffee = offer("Kaffee", 0.25, true, ProductType.DRINK);
    mate = offer("Mate", 1.0, true, ProductType.DRINK);
  }

  private User userWithBalance(double balance) {
    User u = user("Tom", "Hanks", "THa", "hanks@example.com", balance, KitchenCompanionRole.USER);
    stock(u, coffee, mate);
    return u;
  }

  private static void order(User u, Offer offer, int times) {
    for (int i = 0; i < times; i++) {
      u.addTally(offer);
    }
  }

  @Test
  public void noReceiptWithoutOrdersOrDebt() {
    User u = userWithBalance(5.0);

    u.endMonth();

    assertTrue(u.getReceiptsList().isEmpty());
    assertEquals(5.0, u.getBalance(), EPS);
  }

  @Test
  public void ordersCoveredByTheBalanceArePaidFromIt() {
    User u = userWithBalance(10.0);
    order(u, coffee, 2);
    order(u, mate, 1);

    u.endMonth();

    assertEquals(1, u.getReceiptsList().size());
    Receipt r = u.getReceipts(0);
    assertEquals(1.5, r.getSum(), EPS);
    assertEquals(10.0, r.getPastBalance(), EPS);
    assertEquals(0.0, r.getDebt(), EPS);
    assertTrue(r.isPaid());
    assertTrue(r.isConfirmed());
    assertEquals(-8.5, r.getAmountToPay(), EPS);
    // the whole balance is used to pay; whatever is not needed stays on the user's balance
    assertEquals(10.0, r.getAmountPaid(), EPS);
    assertEquals(8.5, u.getBalance(), EPS);
    assertTrue(u.isPaid());
  }

  @Test
  public void ordersExactlyMatchingTheBalanceArePaid() {
    User u = userWithBalance(1.5);
    order(u, coffee, 2);
    order(u, mate, 1);

    u.endMonth();

    assertTrue(u.getReceipts(0).isPaid());
    assertEquals(0.0, u.getBalance(), EPS);
  }

  @Test
  public void ordersExceedingTheBalanceLeaveTheReceiptUnpaid() {
    User u = userWithBalance(1.0);
    order(u, mate, 3);

    u.endMonth();

    Receipt r = u.getReceipts(0);
    assertEquals(3.0, r.getSum(), EPS);
    assertEquals(1.0, r.getPastBalance(), EPS);
    assertFalse(r.isPaid());
    assertFalse(r.isConfirmed());
    assertEquals(0.0, r.getAmountPaid(), EPS);
    assertEquals(2.0, r.getAmountToPay(), EPS);
    assertEquals(0.0, u.getBalance(), EPS);
    assertFalse(u.isPaid());
  }

  @Test
  public void unpaidAmountIsCarriedOverAsDebtEvenWithoutNewOrders() {
    User u = userWithBalance(1.0);
    order(u, mate, 3);
    u.endMonth();

    u.endMonth();

    assertEquals(2, u.getReceiptsList().size());
    Receipt r = u.getReceipts(0);
    assertEquals(0.0, r.getSum(), EPS);
    assertEquals(2.0, r.getDebt(), EPS);
    assertFalse(r.isPaid());
    assertEquals(2.0, r.getAmountToPay(), EPS);
  }

  @Test
  public void debtAndNewOrdersArePaidOnceTheBalanceIsToppedUp() {
    User u = userWithBalance(1.0);
    order(u, mate, 3);
    u.endMonth();

    u.setBalance(5.0);
    order(u, coffee, 4);
    u.endMonth();

    Receipt r = u.getReceipts(0);
    assertEquals(1.0, r.getSum(), EPS);
    assertEquals(2.0, r.getDebt(), EPS);
    assertTrue(r.isPaid());
    assertEquals(5.0 - 1.0 - 2.0, u.getBalance(), EPS);

    // nothing left over, so the following month creates no receipt
    u.endMonth();
    assertEquals(2, u.getReceiptsList().size());
  }

  @Test
  public void receiptSnapshotsTheOrdersAndResetsTheTallies() {
    User u = userWithBalance(10.0);
    order(u, coffee, 1);
    order(u, mate, 2);

    u.endMonth();

    Receipt r = u.getReceipts(0);
    // most ordered first
    assertEquals(List.of(mate, coffee), Tally.offersOf(r.getPastTalliesList()));
    assertEquals(2, Tally.countFor(r.getPastTalliesList(), mate));
    assertEquals(1, Tally.countFor(r.getPastTalliesList(), coffee));

    for (Tally t : u.getTalliesList()) {
      assertEquals(0, t.getCount());
    }
    for (Tally t : u.getPrevTalliesList()) {
      assertEquals(0, t.getCount());
    }
    assertEquals(0.0, u.getSum(), EPS);
    assertFalse(u.hasOrderedTallies());
  }

  @Test
  public void receiptIsDatedToThePreviousMonth() {
    User u = userWithBalance(10.0);
    order(u, coffee, 1);

    u.endMonth();

    LocalDate expected = LocalDate.now().minusMonths(1);
    LocalDate actual = u.getReceipts(0).getDate();
    assertEquals(expected.getYear(), actual.getYear());
    assertEquals(expected.getMonth(), actual.getMonth());
  }

  @Test
  public void newestReceiptComesFirst() {
    User u = userWithBalance(10.0);
    order(u, coffee, 1);
    u.endMonth();
    order(u, mate, 1);
    u.endMonth();

    assertEquals(1.0, u.getReceipts(0).getSum(), EPS);
    assertEquals(0.25, u.getReceipts(1).getSum(), EPS);
    assertEquals(1.0, u.getPastSum(), EPS);
  }
}
