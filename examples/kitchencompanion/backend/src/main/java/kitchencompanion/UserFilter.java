package kitchencompanion;

/**
 * Filter predicates for the alphabet search.
 */
public class UserFilter {
  
  public static Boolean matchesUser(String letter, User user, KitchenCompanionRole role) {
    if (!hasRole(user, role)) {
      return false;
    }
    String prefix = normalize(letter);
    return prefix.isEmpty()
        || startsWith(user.getNickname(), prefix)
        || startsWith(user.getLastName(), prefix);
  }

  public static Boolean matchesOffer(String letter, Offer offer) {
    String prefix = normalize(letter);
    return prefix.isEmpty() || startsWith(offer.getName(), prefix);
  }
  
  public static Boolean noUserFor(String letter) {
    for (User user : KitchenCompanionManager.getUserList()) {
      if (startsWith(user.getName(), normalize(letter))
          || startsWith(user.getLastName(), normalize(letter))) {
        return false;
      }
    }
    return true;
  }
  
  public static Boolean noOfferFor(String letter) {
    for (Offer offer : KitchenCompanionManager.getOfferList()) {
      if (offer.isInStock() && startsWith(offer.getName(), normalize(letter))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasRole(User user, KitchenCompanionRole role) {
    if (role == KitchenCompanionRole.USER) {
      return user.getRole() == KitchenCompanionRole.USER
          || user.getRole() == KitchenCompanionRole.ADMIN;
    }
    return user.getRole() == role;
  }

  private static String normalize(String letter) {
    if (letter == null) {
      return "";
    }
    return letter.toLowerCase();
  }

  private static boolean startsWith(String value, String lowerCasePrefix) {
    return value != null && value.toLowerCase().startsWith(lowerCasePrefix);
  }
}
