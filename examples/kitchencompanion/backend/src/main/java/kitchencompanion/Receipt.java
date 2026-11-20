package kitchencompanion;

import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public class Receipt extends ReceiptTOP {
  public double getAmountToPay() {
    return getSum() + getDebt() - getPastBalance();
  }

  public boolean isPaidFromBalance() {
    return getSum() + getDebt() <= getPastBalance();
  }
}
