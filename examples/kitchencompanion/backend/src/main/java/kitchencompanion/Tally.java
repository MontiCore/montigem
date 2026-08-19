package kitchencompanion;

import java.util.ArrayList;
import java.util.List;

public class Tally extends TallyTOP {

  /**
   * @return the offers of the given tallies, in list order
   */
  public static List<Offer> offersOf(List<Tally> tallies) {
    List<Offer> offers = new ArrayList<>();
    for (Tally t : tallies) {
      offers.add(t.getOffer());
    }
    return offers;
  }

  /**
   * @return the count tallied for the offer, or 0 when the list has no entry for it
   */
  public static int countFor(List<Tally> tallies, Offer offer) {
    for (Tally t : tallies) {
      if (t.getOffer().getName().equals(offer.getName())) {
        return t.getCount();
      }
    }
    return 0;
  }

  /**
   * Highest count first, then by product type, then by offer name.
   */
  public static int compareForDisplay(Tally a, Tally b) {
    if (a.getCount() != b.getCount()) {
      return b.getCount() - a.getCount();
    }
    int byType = a.getOffer().getProductType().name()
        .compareToIgnoreCase(b.getOffer().getProductType().name());
    if (byType != 0) {
      return byType;
    }
    return a.getOffer().getName().compareToIgnoreCase(b.getOffer().getName());
  }
}
