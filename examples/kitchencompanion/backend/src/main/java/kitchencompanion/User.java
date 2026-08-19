package kitchencompanion;


import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class User extends UserTOP {

  public Double calculateSum() {
    Double sum = 0.;
    for (Tally t : getTalliesList()) {
      sum += t.getCount() * t.getOffer().getPrice();
    }
    for (Tally t : getPrevTalliesList()) {
      sum += t.getCount() * t.getOffer().getPrice();
    }
    return sum;
  }

  @Override
  public double getSum() {
    return calculateSum();
  }

  /**
   * Highest count first, then by product type, then by offer name.
   */
  public void sortPrevTallies() {
    List<Tally> list = getPrevTalliesList();
    if (list == null || list.size() < 2) {
      return;
    }
    for (int i = 1; i < list.size(); i++) {
      Tally current = list.get(i);
      int j = i - 1;
      while (j >= 0 && Tally.compareForDisplay(list.get(j), current) > 0) {
        list.set(j + 1, list.get(j));
        j--;
      }
      list.set(j + 1, current);
    }
  }

  /**
   * Sorts users the way they are shown to the reader: prioritized entries first, then
   * alphabetically by last and first name. Single definition for the panels and the mails.
   */
  public static void sortForDisplay(List<User> users) {
    users.sort((a, b) -> compareForDisplay(a, b));
  }

  private static int compareForDisplay(User a, User b) {
    if (displayPriority(a) != displayPriority(b)) {
      return displayPriority(b) - displayPriority(a);
    }
    int byLastName = a.getLastName().compareToIgnoreCase(b.getLastName());
    if (byLastName != 0) {
      return byLastName;
    }
    return a.getName().compareToIgnoreCase(b.getName());
  }

  private static int displayPriority(User u) {
    if (u.getRole() == KitchenCompanionRole.VIP) {
      return 2;
    }
    return 0;
  }

  public boolean canRemoveTally(boolean adminRights, Offer offer) {
    Tally tally = tallyExists(offer);
    if (tally == null) {
      return false;
    }
    return tally.getCount() > 0 || adminRights;
  }

  public void removeTally(boolean adminRights, Offer offer) {
    Tally tally = tallyExists(offer);
    if (canRemoveTally(adminRights, offer)) {
      tally.setCount(tally.getCount() - 1);
    }
  }

  /**
   * Creates a monthly receipt with all relevant values;
   * only generated if orders and/or debt exists;
   * carries over unpaid debt from the previous month.
   */
  public void endMonth() {
    endPurchase();
    sortPrevTallies();
    if (hasOrderedTallies() || !isPaid()) {
      double sum = getSum();
      double balance = getBalance();
      double debt = 0.;
      double amountPaid = 0.;
      boolean paid = false;

      if (!isPaid()) {
        Receipt r = getReceiptsList().get(0);
        debt = r.getSum() + r.getDebt() - r.getPastBalance();
      }
      if (sum + debt <= getBalance()) {
        paid = true;
        setBalance(getBalance() - (sum + debt));
        amountPaid = balance;
      } else {
        setBalance(0.);
      }
      Receipt receipt = KitchenCompanionManager.receiptBuilder()
          .sum(sum)
          .pastBalance(balance)
          .debt(debt)
          .amountPaid(amountPaid)
          .confirmed(paid)
          .paid(paid)
          .date(LocalDate.now().minusMonths(1))
          .build()
          .get();

      for (Tally t : getPrevTalliesList()) {
        receipt.addPastTallies(
            KitchenCompanionManager.tallyBuilder()
                .count(t.getCount())
                .offer(t.getOffer())
                .build()
                .get()
        );
        t.setCount(0);
      }
      addReceipt(receipt);
    }
  }

  public Boolean hasOrderedTallies() {
    for (Tally t : getPrevTalliesList()) {
      if (t.getCount() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * This Method Fills The Tallies list with missing offers so newly added offers become visible
   */
  public void fillTallies() {
    for (Offer o : KitchenCompanionManager.getOfferList()) {
      if (o.isInStock()) {
        addOfferToList(getTalliesList(), o);
        addOfferToList(getPrevTalliesList(), o);
      }
    }
  }

  public void endPurchase() {
    for (Tally prev : getPrevTalliesList()) {
      for (Tally curr : getTalliesList()) {
        if (prev.getOffer().getName().compareToIgnoreCase(curr.getOffer().getName()) == 0) {
          prev.setCount(prev.getCount() + curr.getCount());
          curr.setCount(0);
        }
      }
    }
  }

  public void addTally(Offer offer) {
    Tally tally = tallyExists(offer);
    tally.setCount(tally.getCount() + 1);
  }

  public void addReceipt(Receipt receipt) {
    getReceiptsList().add(0, receipt);
  }

  public Double getPastSum() {
    if (getReceiptsList().isEmpty()) {
      return 0.;
    }
    return getReceiptsList().get(0).getSum();
  }


  public Boolean isValid() {
    if (!this.getName().isEmpty() &&
        !this.getLastName().isEmpty() &&
        !this.getNickname().isEmpty() &&
        isEmailValid() &&
        this.getRole() != null
    ) {
      return true;
    }
    return false;
  }

  public boolean isEmailValid() {
    if (_email.contains("@")) {
      String[] parts = _email.split("@");
      if (parts.length == 2) {
        String local = parts[0];
        String domain = parts[1];
        if (domain.contains(".") && !local.isEmpty() && !domain.startsWith(".") && !domain.endsWith(".")) {
          return true;
        }
      }
    }
    return false;
  }

  // Private Methods
  private void addOfferToList(List<Tally> list, Offer offer) {
    if (!offerExists(list, offer)) {
      list.add(KitchenCompanionManager.tallyBuilder().offer(offer).count(0).build().get());
    }
  }

  private boolean offerExists(List<Tally> tallyList, Offer offer) {
    for (Tally t : tallyList) {
      if (t.getOffer().getGemId() == offer.getGemId()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Will never be null because of the way we fill the tallies list,
   * but will return null if there is a different error
   *
   * @param offer
   * @return
   */
  private Tally tallyExists(Offer offer) {
    Tally found = null;
    for (Tally t : getTalliesList()) {
      if (t.getOffer().getGemId() == offer.getGemId()) {
        found = t;
        break;
      }
    }
    return found;
  }

  // static methods


  public static List<User> getUsersList() {
    return KitchenCompanionManager.getUserList();
  }

  public Boolean isPaid() {
    if (getReceiptsList() != null && !getReceiptsList().isEmpty()) {
      return getReceiptsList().get(0).isPaid();
    } else {
      return true;
    }
  }

  /**
   * Implements the derived attribute {@code formattedName} of the model, so that the GUI can
   * bind {@code user.formattedName} directly instead of every component defining a getName().
   */
  @Override
  public String getFormattedName() {
    if (this.getName().isEmpty() || this.getLastName().isEmpty()) {
      return this.getName() + this.getLastName();
    } else {
      return this.getLastName() + ", " + this.getName();
    }
  }
}
