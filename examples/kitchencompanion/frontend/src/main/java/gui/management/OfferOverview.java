package gui.management;

import kitchencompanion.Offer;
import kitchencompanion.Tally;
import kitchencompanion.User;
import kitchencompanion.UserFilter;

import java.util.List;
import java.util.function.Consumer;

public class OfferOverview extends OfferOverviewTOP {

  @Override
  public void init() {
    super.init();
    getUser().fillTallies();
    getUser().endPurchase();
    getUser().sortPrevTallies();
  }

  @Override
  public Boolean search(Offer offer) {
    return UserFilter.matchesOffer(getSearchLetter(), offer);
  }
  
  @Override
  public Consumer<String> selectLetter() {
    return letter -> setSearchLetter(letter);
  }

  @Override
  public List<Offer> getOfferList(User user) {
    if (user.hasOrderedTallies() || user.getReceiptsList().isEmpty()) {
      return Tally.offersOf(user.getPrevTalliesList());
    }
    return Tally.offersOf(user.getReceiptsList().get(0).getPastTalliesList());
  }

  @Override
  public String getTargetAddress(User user) {
    return "/gui/management/Subtotal/" + user.getGemId();
  }

  public Boolean isInStock(Offer offer) {
    return offer.isInStock();
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public String getTallyCount(Offer offer) {
    return String.valueOf(Tally.countFor(getUser().getPrevTalliesList(), offer));
  }
}
