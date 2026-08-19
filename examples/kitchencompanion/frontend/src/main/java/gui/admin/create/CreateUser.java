package gui.admin.create;

import app.AppRouter;
import kitchencompanion.KitchenCompanionManager;
import gui.admin.LoginServiceFront;
import kitchencompanion.Offer;
import kitchencompanion.User;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component()
public class CreateUser extends CreateUserTOP {
  @Override
  public Consumer<Void> createAndNavigate(User user) {
    return $ -> {
      if (user.isValid()) {
        user.setBalance(0.);
        user.build();
        for (Offer o : KitchenCompanionManager.getOfferList()) {
          if (o.isInStock()) {
            user.addTallies(KitchenCompanionManager.tallyBuilder().count(0).offer(o).build().get());
            user.addPrevTallies(KitchenCompanionManager.tallyBuilder().count(0).offer(o).build().get());
          }
        }
        AppRouter.navigateToGuiAdminAdmin();
      }
    };
  }
  @Override
  public void init() {
    super.init();
    if(!LoginServiceFront.getLoggedIn()){
      AppRouter.navigateToGuiAdminLoginPage();
    }
  }
}
