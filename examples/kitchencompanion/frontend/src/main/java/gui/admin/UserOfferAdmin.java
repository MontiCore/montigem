package gui.admin;

import kitchencompanion.Offer;
import kitchencompanion.Tally;
import kitchencompanion.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class UserOfferAdmin extends UserOfferAdminTOP {
  @Override
  public String getTallyCount(Offer offer) {
    return String.valueOf(Tally.countFor(getUser().getPrevTalliesList(), offer));
  }
}
