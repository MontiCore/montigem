package gui.kitchen;

import app.AppRouter;
import kitchencompanion.KitchenCompanionRole;
import kitchencompanion.User;
import kitchencompanion.UserFilter;

import java.util.function.Consumer;

public class UserPanel extends UserPanelTOP {

  @Override
  public Boolean search(String searchLetter, User user, KitchenCompanionRole role) {
    return UserFilter.matchesUser(searchLetter, user, role);
  }

  @Override
  public Consumer<Void> navigateToUser(User user, Boolean adminPage) {
    return $ -> {
      if (adminPage) {
        AppRouter.navigateToGuiAdminUserEdit(user.getGemId());
      } else {
        AppRouter.navigateToGuiManagementOfferOverview(user.getGemId());
      }
    };
  }
}
