package gui.admin;

import app.AppRouter;
import umlp.guiutil.Router;

import java.util.function.Consumer;

public class OfferEdit extends OfferEditTOP{
  @Override
  public Consumer<Void> navigateBack(){
    return $ -> {
      AppRouter.navigateToGuiAdminOfferAdmin();
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
