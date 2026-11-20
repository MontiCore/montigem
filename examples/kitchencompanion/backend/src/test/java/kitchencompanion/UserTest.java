package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import java.util.ArrayList;
import java.util.List;

import static kitchencompanion.KitchenCompanionTestData.offer;
import static kitchencompanion.KitchenCompanionTestData.stock;
import static kitchencompanion.KitchenCompanionTestData.tally;
import static kitchencompanion.KitchenCompanionTestData.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tally bookkeeping, validation and display helpers of {@link User}. The monthly billing is
 * covered separately in {@link UserEndMonthTest}.
 */
public class UserTest extends KitchenCompanionMockTestSetup {

  private static final double EPS = 1e-9;

  public UserTest() {
    super(ClientSetup.of(0));
  }

  // --- tallies ---------------------------------------------------------------------------------

  @Test
  public void addTallyIncrementsTheCurrentTallyOfThatOffer() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer water = offer("Wasser", 0.75);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.VIP);
    stock(u, coffee, water);

    u.addTally(coffee);
    u.addTally(coffee);

    assertEquals(2, Tally.countFor(u.getTalliesList(), coffee));
    assertEquals(0, Tally.countFor(u.getTalliesList(), water));
    assertEquals(0, Tally.countFor(u.getPrevTalliesList(), coffee));
  }

  @Test
  public void sumCoversCurrentAndPreviousTallies() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer mate = offer("Mate", 1.0);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.VIP);
    u.addTallies(tally(coffee, 2));
    u.addPrevTallies(tally(coffee, 4));
    u.addPrevTallies(tally(mate, 1));

    assertEquals(0.5 + 1.0 + 1.0, u.getSum(), EPS);
    assertEquals(u.calculateSum(), u.getSum(), EPS);
  }

  @Test
  public void userCanOnlyRemoveTalliesAboveZero() {
    Offer coffee = offer("Kaffee", 0.25);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    stock(u, coffee);

    assertFalse(u.canRemoveTally(false, coffee));
    u.removeTally(false, coffee);
    assertEquals(0, Tally.countFor(u.getTalliesList(), coffee));

    u.addTally(coffee);
    assertTrue(u.canRemoveTally(false, coffee));
    u.removeTally(false, coffee);
    assertEquals(0, Tally.countFor(u.getTalliesList(), coffee));
  }

  @Test
  public void adminCanRemoveTallyBelowZeroToCorrectAlreadyBookedOrders() {
    Offer coffee = offer("Kaffee", 0.25);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    stock(u, coffee);

    assertTrue(u.canRemoveTally(true, coffee));
    u.removeTally(true, coffee);

    assertEquals(-1, Tally.countFor(u.getTalliesList(), coffee));
  }

  @Test
  public void tallyCannotBeRemovedForAnOfferTheUserHasNoTallyFor() {
    Offer coffee = offer("Kaffee", 0.25);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);

    assertFalse(u.canRemoveTally(true, coffee));
    assertFalse(u.canRemoveTally(false, coffee));
  }

  @Test
  public void endPurchaseMovesCurrentTalliesIntoPreviousTallies() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer water = offer("Wasser", 0.75);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addTallies(tally(coffee, 2));
    u.addTallies(tally(water, 1));
    u.addPrevTallies(tally(coffee, 3));
    u.addPrevTallies(tally(water, 0));

    u.endPurchase();

    assertEquals(5, Tally.countFor(u.getPrevTalliesList(), coffee));
    assertEquals(1, Tally.countFor(u.getPrevTalliesList(), water));
    assertEquals(0, Tally.countFor(u.getTalliesList(), coffee));
    assertEquals(0, Tally.countFor(u.getTalliesList(), water));
  }

  @Test
  public void endPurchaseDoesNotChangeTheSum() {
    Offer coffee = offer("Kaffee", 0.25);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addTallies(tally(coffee, 2));
    u.addPrevTallies(tally(coffee, 3));
    double before = u.getSum();

    u.endPurchase();

    assertEquals(before, u.getSum(), EPS);
  }

  @Test
  public void fillTalliesAddsMissingInStockOffersOnce() {
    Offer coffee = offer("Kaffee", 0.25);
    Offer peanuts = offer("Erdnüsse", 1.0, true, ProductType.SNACK);
    Offer soldOut = offer("Mate", 1.0, false, ProductType.DRINK);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addTallies(tally(coffee, 2));
    u.addPrevTallies(tally(coffee, 3));

    u.fillTallies();
    u.fillTallies();

    assertEquals(2, u.getTalliesList().size());
    assertEquals(2, u.getPrevTalliesList().size());
    // existing counts are kept
    assertEquals(2, Tally.countFor(u.getTalliesList(), coffee));
    assertEquals(3, Tally.countFor(u.getPrevTalliesList(), coffee));
    // the new offer starts at zero, the sold out one is not added
    assertEquals(0, Tally.countFor(u.getTalliesList(), peanuts));
    assertFalse(Tally.offersOf(u.getTalliesList()).contains(soldOut));
    assertFalse(Tally.offersOf(u.getPrevTalliesList()).contains(soldOut));
  }

  @Test
  public void hasOrderedTalliesOnlyLooksAtPreviousTallies() {
    Offer coffee = offer("Kaffee", 0.25);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    stock(u, coffee);
    assertFalse(u.hasOrderedTallies());

    u.addTally(coffee);
    assertFalse(u.hasOrderedTallies());

    u.endPurchase();
    assertTrue(u.hasOrderedTallies());
  }

  @Test
  public void sortPrevTalliesOrdersByCountThenTypeThenName() {
    Offer water = offer("Wasser", 0.75, true, ProductType.DRINK);
    Offer coffee = offer("Kaffee", 0.25, true, ProductType.DRINK);
    Offer noodles = offer("Instant Nudeln", 0.4, true, ProductType.SNACK);
    Offer peanuts = offer("Erdnüsse", 1.0, true, ProductType.SNACK);
    Offer mate = offer("Mate", 1.0, true, ProductType.DRINK);
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addPrevTallies(tally(noodles, 0));
    u.addPrevTallies(tally(water, 0));
    u.addPrevTallies(tally(peanuts, 3));
    u.addPrevTallies(tally(coffee, 0));
    u.addPrevTallies(tally(mate, 3));

    u.sortPrevTallies();

    assertEquals(List.of(mate, peanuts, coffee, water, noodles),
        Tally.offersOf(u.getPrevTalliesList()));
  }

  // --- receipts --------------------------------------------------------------------------------

  @Test
  public void userWithoutReceiptsCountsAsPaid() {
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);

    assertTrue(u.isPaid());
    assertEquals(0.0, u.getPastSum(), EPS);
  }

  @Test
  public void newestReceiptDecidesPaymentStatusAndPastSum() {
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addReceipt(receipt(2.0, true));
    u.addReceipt(receipt(3.5, false));

    assertEquals(2, u.getReceiptsList().size());
    assertFalse(u.isPaid());
    assertEquals(3.5, u.getPastSum(), EPS);
  }

  private static Receipt receipt(double sum, boolean paid) {
    return KitchenCompanionManager.receiptBuilder()
        .sum(sum)
        .pastBalance(0.0)
        .debt(0.0)
        .amountPaid(0.0)
        .confirmed(paid)
        .paid(paid)
        .date(java.time.LocalDate.of(2026, 1, 1))
        .build()
        .get();
  }

  // --- validation ------------------------------------------------------------------------------

  @Test
  public void validUser() {
    User u = user("Tom", "Hanks", "THa", "hanks@example.com", 0.0, KitchenCompanionRole.USER);

    assertTrue(u.isEmailValid());
    assertTrue(u.isValid());
  }

  @Test
  public void userWithMissingNamesIsInvalid() {
    User u = user("Tom", "Hanks", "THa", "hanks@example.com", 0.0, KitchenCompanionRole.USER);

    u.setName("");
    assertFalse(u.isValid());
    u.setName("Tom");

    u.setLastName("");
    assertFalse(u.isValid());
    u.setLastName("Hanks");

    u.setNickname("");
    assertFalse(u.isValid());
  }

  @Test
  public void userWithInvalidEmailIsInvalid() {
    User u = user("Tom", "Hanks", "THa", "hanks@example.com", 0.0, KitchenCompanionRole.USER);
    u.setEmail("hanks.example.com");

    assertFalse(u.isValid());
  }

  @Test
  public void emailValidation() {
    User u = user("Tom", "Hanks", "THa", "hanks@example.com", 0.0, KitchenCompanionRole.USER);
    for (String valid : List.of("a@b.c", "tom.hanks@mail.example.com")) {
      u.setEmail(valid);
      assertTrue(u.isEmailValid(), valid);
    }
    for (String invalid : List.of("", "hanks", "hanks@", "@example.com", "hanks@example",
        "hanks@.example.com", "hanks@example.com.", "a@b@example.com")) {
      u.setEmail(invalid);
      assertFalse(u.isEmailValid(), invalid);
    }
  }

  // --- display ---------------------------------------------------------------------------------

  @Test
  public void formattedNameIsLastNameCommaFirstName() {
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);

    assertEquals("Hanks, Tom", u.getFormattedName());
  }

  @Test
  public void formattedNameOmitsTheCommaWhenOneNameIsEmpty() {
    User office = user("", "Office", "Office", KitchenCompanionRole.EXTRA);

    assertEquals("Office", office.getFormattedName());
  }

  @Test
  public void sortForDisplayPutsVipsFirstThenSortsByLastAndFirstName() {
    User evans = user("Chris", "Evans", "CEv", KitchenCompanionRole.USER);
    User streep = user("Meryl", "Streep", "MSt", KitchenCompanionRole.VIP);
    User emmaStone = user("Emma", "Stone", "ESt", KitchenCompanionRole.ADMIN);
    User hanks = user("Tom", "Hanks", "THa", KitchenCompanionRole.VIP);
    User alanStone = user("alan", "stone", "ASt", KitchenCompanionRole.USER);
    List<User> users = new ArrayList<>(List.of(evans, streep, emmaStone, hanks, alanStone));

    User.sortForDisplay(users);

    assertEquals(List.of(hanks, streep, evans, alanStone, emmaStone), users);
  }
}
