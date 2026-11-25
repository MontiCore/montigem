package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import static kitchencompanion.KitchenCompanionTestData.offer;
import static kitchencompanion.KitchenCompanionTestData.user;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filters behind the alphabet search of the kitchen and offer pages.
 */
public class UserFilterTest extends KitchenCompanionMockTestSetup {

  public UserFilterTest() {
    super(ClientSetup.of(0));
  }

  @Test
  public void emptyOrMissingLetterMatchesEveryUserOfTheRole() {
    User hanks = user("Tom", "Hanks", "THa", KitchenCompanionRole.VIP);

    assertTrue(UserFilter.matchesUser("", hanks, KitchenCompanionRole.VIP));
    assertTrue(UserFilter.matchesUser(null, hanks, KitchenCompanionRole.VIP));
    assertFalse(UserFilter.matchesUser("", hanks, KitchenCompanionRole.USER));
  }

  @Test
  public void letterMatchesNicknameOrLastNameIgnoringCase() {
    User hanks = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);

    assertTrue(UserFilter.matchesUser("T", hanks, KitchenCompanionRole.USER));
    assertTrue(UserFilter.matchesUser("h", hanks, KitchenCompanionRole.USER));
    assertFalse(UserFilter.matchesUser("O", hanks, KitchenCompanionRole.USER));
  }

  @Test
  public void userPanelAlsoListsAdmins() {
    User admin = user("Denzel", "Washington", "DWa", KitchenCompanionRole.ADMIN);

    assertTrue(UserFilter.matchesUser("", admin, KitchenCompanionRole.USER));
    assertTrue(UserFilter.matchesUser("", admin, KitchenCompanionRole.ADMIN));
  }

  @Test
  public void otherRolesOnlyMatchThemselves() {
    User vip = user("Meryl", "Streep", "MSt", KitchenCompanionRole.VIP);
    User hiwi = user("Florence", "Pugh", "FPu", KitchenCompanionRole.HIWI);
    User office = user("", "Office", "Office", KitchenCompanionRole.EXTRA);

    assertFalse(UserFilter.matchesUser("", vip, KitchenCompanionRole.USER));
    assertFalse(UserFilter.matchesUser("", vip, KitchenCompanionRole.HIWI));
    assertTrue(UserFilter.matchesUser("", hiwi, KitchenCompanionRole.HIWI));
    assertFalse(UserFilter.matchesUser("", hiwi, KitchenCompanionRole.USER));
    assertTrue(UserFilter.matchesUser("O", office, KitchenCompanionRole.EXTRA));
    assertFalse(UserFilter.matchesUser("O", office, KitchenCompanionRole.VIP));
  }

  @Test
  public void matchesOfferByNamePrefix() {
    Offer coffee = offer("Kaffee", 0.25);

    assertTrue(UserFilter.matchesOffer("", coffee));
    assertTrue(UserFilter.matchesOffer(null, coffee));
    assertTrue(UserFilter.matchesOffer("k", coffee));
    assertFalse(UserFilter.matchesOffer("W", coffee));
  }

  @Test
  public void noUserForChecksFirstAndLastNamesOfAllUsers() {
    user("Tom", "Hanks", "THa", KitchenCompanionRole.VIP);
    user("Meryl", "Streep", "MSt", KitchenCompanionRole.USER);

    assertFalse(UserFilter.noUserFor("T"));
    assertFalse(UserFilter.noUserFor("h"));
    assertFalse(UserFilter.noUserFor("S"));
    assertTrue(UserFilter.noUserFor("Q"));
  }

  @Test
  public void noOfferForIgnoresOffersOutOfStock() {
    offer("Kaffee", 0.25, true, ProductType.DRINK);
    offer("Mate", 1.0, false, ProductType.DRINK);

    assertFalse(UserFilter.noOfferFor("K"));
    assertTrue(UserFilter.noOfferFor("M"));
    assertTrue(UserFilter.noOfferFor("Z"));
  }
}
