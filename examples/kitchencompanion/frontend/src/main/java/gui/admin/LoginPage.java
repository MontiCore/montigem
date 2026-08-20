package gui.admin;
import app.AppRouter;
import mc.fenix.input.gemtextinputtypes.GemTextInputValidity;

import java.util.function.Consumer;
public class LoginPage extends LoginPageTOP {
  
  @Override
  public void init() {
    super.init();
    if (LoginServiceFront.getLoggedIn()){
      AppRouter.navigateToGuiAdminAdmin();
    }
  }
  
  @Override
  public Consumer<Void> login(String pw) {
    return $ -> {
      if(LoginServiceFront.login(pw)){
        validity = new GemTextInputValidity("Success", "");
        getPasswordValidity();
        AppRouter.navigateToGuiAdminAdmin();
      } else{
        validity = new GemTextInputValidity("Error", "Wrong password");
        getPasswordValidity();
      }
    };
  }
  
  private GemTextInputValidity validity = new GemTextInputValidity("Success","validity hasnt been checked yet");
  
  @Override
  public GemTextInputValidity getPasswordValidity() {
    return validity;
  }

  
}
