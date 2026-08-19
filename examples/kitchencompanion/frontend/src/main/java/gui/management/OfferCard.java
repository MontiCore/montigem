package gui.management;

import kitchencompanion.Offer;
import kitchencompanion.Tally;

import java.util.function.Consumer;

public class OfferCard extends OfferCardTOP {
  @Override
  public Consumer<Void> addTally(Offer offer) {
    return $ -> {
      getUser().addTally(offer);
      setTallyCount(offer);
      setCanRemove();
    };
  }


  public void setCanRemove() {
    this.setCanRemoveTally(getUser().canRemoveTally(isAdminRights(),getOffer()));
  }

  @Override
  public Consumer<Void> removeTally(Offer offer) {
    return $ -> {
      getUser().removeTally(isAdminRights(), offer);
      setTallyCount(offer);
      setCanRemove();
    };
  }

  public void setTallyCount(Offer offer) {
    int tallyCount = Tally.countFor(getUser().getTalliesList(), offer);
    String prevTalliesCount = getPrevTallies(offer);
    if (prevTalliesCount.compareTo("0") == 0 && tallyCount == 0) {
      setCount("" + 0);
    } else if (prevTalliesCount.compareTo("0") == 0 && tallyCount > 0) {
      setCount("" + tallyCount);
    } else if (prevTalliesCount.compareTo("0") != 0 && tallyCount == 0) {
      setCount(prevTalliesCount);
    } else {
      setCount(prevTalliesCount + " " + "+" + "" + tallyCount);
    }
  }

  private String getPrevTallies(Offer offer) {
    return "" + Tally.countFor(getUser().getPrevTalliesList(), offer);
  }
}
