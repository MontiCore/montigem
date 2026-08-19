package gui.admin;

import app.AppRouter;
import kitchencompanion.Receipt;
import kitchencompanion.User;

import java.time.LocalDate;

public class PaymentStatus extends PaymentStatusTOP {

  @Override
  public Boolean hasReceipt(User user) {
    if (!user.getReceiptsList().isEmpty()) {
      LocalDate date = LocalDate.now().minusMonths(1);
      return user.getReceipts(0).getDate().getMonth() == date.getMonth() && user.getReceipts(0).getDate().getYear() == date.getYear();
    }
    return false;
  }

  @Override
  public Receipt getReceipt(User user) {
    return user.getReceipts(0);
  }
  @Override
  public Boolean hasAny1Receipt() {
    for (User u : getUsers()) {
      if (hasReceipt(u)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void init() {
    super.init();
    if (!LoginServiceFront.getLoggedIn()) {
      AppRouter.navigateToGuiAdminLoginPage();
    }
  }

  @Override
  protected void initUsers() {
    super.initUsers();
    setUsers(User.getUsersList());
  }

  @Override
  public void destroy() {
    super.destroy();
  }
}
