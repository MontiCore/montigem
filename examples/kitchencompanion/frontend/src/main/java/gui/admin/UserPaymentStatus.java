package gui.admin;

import kitchencompanion.Receipt;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;
@Component
public class UserPaymentStatus extends UserPaymentStatusTOP {

  @Override
  public String getInvoiceAmount(Receipt receipt) {
    if (receipt.isPaidFromBalance()) {
      return (receipt.getSum() + receipt.getDebt()) + " €";
    }
    return receipt.getAmountToPay() + " €";
  }

  @Override
  public void setPaidChecked(Boolean paid) {
    Receipt r = getReceipt();
    r.setAmountPaid(0.);
    getReceipt().setConfirmed(false);
    super.setPaidChecked(paid);
  }

  @Override
  public Consumer<Void> confirm() {
    return $ -> {
      if (!getReceipt().isConfirmed() && getReceipt().isPaid()) {
        if (getReceipt().getAmountPaid() >= getReceipt().getAmountToPay()) {
          double addedBalance = getReceipt().getAmountPaid() - getReceipt().getAmountToPay();
          getUser().setBalance(getUser().getBalance() + addedBalance);
        } else if (getReceipt().getAmountPaid() == 0) {
          getReceipt().setAmountPaid(getReceipt().getAmountToPay());
        }
        getReceipt().setConfirmed(true);
      }
    };
  }

  @Override
  public Boolean canConfirm(Receipt receipt) {
    return !receipt.isConfirmed();
  }

  @Override
  public Boolean canReset(Receipt receipt) {
    return receipt.isConfirmed() && !receipt.isPaidFromBalance();
  }

  @Override
  public Consumer<Void> reset() {
    return $ -> {
      Receipt r = getReceipt();
      if (!r.isPaid() || !canReset(r)) {
        return;
      }
      if (r.getAmountPaid() >= r.getAmountToPay()) {
        getUser().setBalance(getUser().getBalance() - (r.getAmountPaid() - r.getAmountToPay()));
        setPaidChecked(false);
      }
    };
  }
}
