package gui.admin;

import kitchencompanion.KitchenCompanionManager;
import kitchencompanion.KitchenCompanionRole;
import kitchencompanion.Offer;
import kitchencompanion.ProductType;
import kitchencompanion.Receipt;
import kitchencompanion.User;
import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admin payment card of a user's latest receipt: confirming a payment and resetting it again.
 * The admin works on a simulated client; results are checked on the client and on the backend.
 */
public class UserPaymentStatusTest extends KitchenCompanionMockTestSetup {

  private static final double EPS = 1e-9;

  public UserPaymentStatusTest() {
    super(ClientSetup.of(1));
  }

  /**
   * Builds a user with the given balance on the backend, lets them order {@code coffees} coffees
   * at 0.25 € and closes the month.
   *
   * @return the gemId of the user
   */
  private long userAfterMonthEnd(double balance, int coffees) {
    getBackend().activate();
    Offer coffee = KitchenCompanionManager.offerBuilder()
        .name("Kaffee").price(0.25).imgSource("assets/images/Kaffeebohnen.png")
        .inStock(true).productType(ProductType.DRINK)
        .build().get();
    User u = KitchenCompanionManager.userBuilder()
        .name("Tom").lastName("Hanks").nickname("THa").email("hanks@example.com")
        .balance(balance).role(KitchenCompanionRole.USER)
        .build().get();
    u.addTallies(KitchenCompanionManager.tallyBuilder().offer(coffee).count(0).build().get());
    u.addPrevTallies(KitchenCompanionManager.tallyBuilder().offer(coffee).count(0).build().get());
    for (int i = 0; i < coffees; i++) {
      u.addTally(coffee);
    }
    u.endMonth();
    return u.getGemId();
  }

  /** Opens the payment card of the user's latest receipt on the admin's client. */
  private UserPaymentStatus openCard(long userId) {
    getClients(0).activate();
    User u = KitchenCompanionManager.getUserSure(userId);
    UserPaymentStatus card = new UserPaymentStatus();
    card.setUser(u);
    card.setReceipt(u.getReceipts(0));
    card.init();
    return card;
  }

  /**
   * Guards in UserPaymentStatus.gui: 3 = receipt.paid, 5 = receipt.confirmed (reset in the if
   * branch, confirm in the else branch), 6 = canReset(receipt).
   */
  private static boolean resetButtonShown(UserPaymentStatus card) {
    assertEquals("reset", card.getResetBtnLabel());
    return card.getGuard3Condition() && card.getGuard5Condition() && card.getGuard6Condition();
  }

  private static boolean confirmButtonShown(UserPaymentStatus card) {
    assertEquals("confirm", card.getConfirmBtnLabel());
    return card.getGuard3Condition() && !card.getGuard5Condition();
  }

  private User backendUser(long userId) {
    getBackend().activate();
    return KitchenCompanionManager.getUserSure(userId);
  }

  /** Asserts the state endMonth() leaves for a receipt paid from the balance. */
  private void assertStillPaidFromBalance(long userId, double balanceLeft, double pastBalance,
                                          double amountToPay) {
    User server = backendUser(userId);
    Receipt receipt = server.getReceipts(0);
    assertEquals(balanceLeft, server.getBalance(), EPS);
    assertTrue(receipt.isPaid());
    assertTrue(receipt.isConfirmed());
    assertEquals(pastBalance, receipt.getPastBalance(), EPS);
    assertEquals(pastBalance, receipt.getAmountPaid(), EPS);
    assertEquals(amountToPay, receipt.getAmountToPay(), EPS);
  }

  /**
   * Tom has 10 €, buys 1.50 € and the month end pays the bill from his balance (8.50 € left).
   * That payment is final: the card offers neither confirm nor reset, and triggering reset anyway
   * changes nothing. The amount to pay is negative: the balance exceeded the bill by 8.50 €.
   */
  @Test
  public void receiptPaidFromTheBalanceCannotBeReset() {
    long tom = userAfterMonthEnd(10.0, 6);
    UserPaymentStatus card = openCard(tom);
    assertStillPaidFromBalance(tom, 8.5, 10.0, -8.5);

    getClients(0).activate();
    assertEquals("-8.5 €", card.getGemText2Value());
    assertFalse(confirmButtonShown(card));
    assertFalse(resetButtonShown(card));
    card.resetBtnLeftClick(null);

    assertEquals(8.5, card.getUser().getBalance(), EPS);
    assertStillPaidFromBalance(tom, 8.5, 10.0, -8.5);
  }

  /** Same when the balance matched the bill exactly (1.50 € balance, 1.50 € bill). */
  @Test
  public void receiptPaidWithTheExactBalanceCannotBeReset() {
    long tom = userAfterMonthEnd(1.5, 6);
    UserPaymentStatus card = openCard(tom);
    assertStillPaidFromBalance(tom, 0.0, 1.5, 0.0);

    getClients(0).activate();
    assertFalse(confirmButtonShown(card));
    assertFalse(resetButtonShown(card));
    card.resetBtnLeftClick(null);

    assertStillPaidFromBalance(tom, 0.0, 1.5, 0.0);
  }

  /**
   * Tom has 1 € and buys 3 €, so 2 € are open. He pays 5 € in cash; the admin confirms that and
   * 3 € are credited. Resetting takes the 3 € back off again. Confirm is only offered while there
   * is something to confirm, reset only once something has been confirmed.
   */
  @Test
  public void confirmCreditsOverpaymentAndResetTakesItBack() {
    long tom = userAfterMonthEnd(1.0, 12);
    UserPaymentStatus card = openCard(tom);
    assertFalse(card.getReceipt().isPaid());
    assertEquals("2.0 €", card.getGemText2Value());
    // nothing ticked yet: neither button
    assertFalse(confirmButtonShown(card));
    assertFalse(resetButtonShown(card));

    card.setPaidChecked(true);
    card.setAmountPaidEntry("5");
    assertTrue(confirmButtonShown(card));
    assertFalse(resetButtonShown(card));
    card.confirmBtnLeftClick(null);

    User server = backendUser(tom);
    assertEquals(3.0, server.getBalance(), EPS);
    assertTrue(server.getReceipts(0).isPaid());
    assertTrue(server.getReceipts(0).isConfirmed());

    getClients(0).activate();
    assertFalse(confirmButtonShown(card));
    assertTrue(resetButtonShown(card));
    card.resetBtnLeftClick(null);

    server = backendUser(tom);
    assertEquals(0.0, server.getBalance(), EPS);
    assertFalse(server.getReceipts(0).isPaid());
    assertFalse(server.getReceipts(0).isConfirmed());
    assertEquals(0.0, server.getReceipts(0).getAmountPaid(), EPS);
    assertEquals(2.0, server.getReceipts(0).getAmountToPay(), EPS);

    getClients(0).activate();
    assertFalse(confirmButtonShown(card));
    assertFalse(resetButtonShown(card));
  }
}
