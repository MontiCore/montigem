package gui.management;

import de.se_rwth.commons.logging.Log;
import kitchencompanion.Receipt;
import kitchencompanion.Offer;
import kitchencompanion.Tally;
import kitchencompanion.User;
import umlp.guiutil.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static def.dom.Globals.clearInterval;
import static def.dom.Globals.setInterval;

public class Subtotal extends SubtotalTOP {

  @Override
  public void init() {
    super.init();
  }

  @Override
  public String sumOfTallies(User user) {
    double sum = 0.0;
    for (Tally t : user.getTalliesList()) {
      sum += t.getCount();
    }
    for (Tally t : user.getPrevTalliesList()) {
      sum += t.getCount();
    }
    return "" + sum;
  }

  @Override
  public String priceSum(User user) {
    return user.getSum() - user.getBalance() + " €";
  }

  @Override
  public String getUserBalance(User user) {
    double temp = user.getBalance();
    return temp + " €";
  }

  @Override
  public String getAmount(Offer offer, User user) {
    int sum = 0;
    for (Tally t : user.getTalliesList()) {
      if (offer.equals(t.getOffer())) {
        sum += t.getCount();
      }
    }
    for (Tally t : user.getPrevTalliesList()) {
      if (offer.equals(t.getOffer())) {
        sum += t.getCount();
      }
    }
    return "" + sum;
  }

  @Override
  public List<Offer> getOfferList(User user) {
    return Tally.offersOf(user.getPrevTalliesList());
  }

  @Override
  public String priceOrdered(User u) {
    return u.getSum() + " €";
  }

  @Override
  public void destroy() {
    super.destroy();
  }}
