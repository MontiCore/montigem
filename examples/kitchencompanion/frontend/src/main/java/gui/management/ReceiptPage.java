package gui.management;

import kitchencompanion.Offer;
import kitchencompanion.Tally;
import kitchencompanion.Receipt;
import kitchencompanion.User;

import java.util.ArrayList;
import java.util.List;

public class ReceiptPage extends ReceiptPageTOP {

  @Override
  public List<Offer> getOfferList(Receipt receipt) {
    return Tally.offersOf(receipt.getPastTalliesList());
  }

  @Override
  public String getAmount(Offer offer, Receipt receipt) {
    for (Tally t : receipt.getPastTalliesList()) {
      if (t.getOffer() == offer) {
        return t.getCount() + "";
      }
    }
    return "0";
  }

  @Override
  public String priceOrdered(Receipt receipt) {
    return receipt.getSum() + " €";
  }

  @Override
  public String priceSum(Receipt receipt) {
    return receipt.getSum() + receipt.getDebt() - receipt.getPastBalance() + " €";
  }

  @Override
  public String sumOfTallies(Receipt receipt) {
    int sum = 0;
    for (Tally t : receipt.getPastTalliesList()) {
      sum += t.getCount();
    }
    return String.valueOf(sum);
  }

  @Override
  public String pricePaid(Receipt receipt) {
    return receipt.getAmountPaid() + " €";
  }
}
