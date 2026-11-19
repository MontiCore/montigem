package kitchencompanion;

/**
 * Small factory for the domain objects the KitchenCompanion tests need. Every method builds through
 * the generated builders of {@link KitchenCompanionManager}, so it must be called in an active
 * context of a {@code KitchenCompanionMockTestSetup}.
 */
public final class KitchenCompanionTestData {

  private KitchenCompanionTestData() {
  }

  public static Offer offer(String name, double price) {
    return offer(name, price, true, ProductType.DRINK);
  }

  public static Offer offer(String name, double price, boolean inStock, ProductType type) {
    return KitchenCompanionManager.offerBuilder()
        .name(name)
        .price(price)
        .imgSource("assets/images/" + name + ".png")
        .inStock(inStock)
        .productType(type)
        .build()
        .get();
  }

  public static User user(String name, String lastName, String nickname, KitchenCompanionRole role) {
    return user(name, lastName, nickname, nickname.toLowerCase() + "@example.com", 0.0, role);
  }

  public static User user(String name, String lastName, String nickname, String email,
                          double balance, KitchenCompanionRole role) {
    return KitchenCompanionManager.userBuilder()
        .name(name)
        .lastName(lastName)
        .nickname(nickname)
        .email(email)
        .balance(balance)
        .role(role)
        .build()
        .get();
  }

  public static Tally tally(Offer offer, int count) {
    return KitchenCompanionManager.tallyBuilder().offer(offer).count(count).build().get();
  }

  /**
   * Gives the user a zero-count current and previous tally for every offer, as the server does on
   * startup and {@link User#fillTallies()} does for newly added offers.
   */
  public static void stock(User user, Offer... offers) {
    for (Offer o : offers) {
      user.addTallies(tally(o, 0));
      user.addPrevTallies(tally(o, 0));
    }
  }
}
