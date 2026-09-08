package gui;/* (c) https://github.com/MontiCore/monticore */

import app.AppRouter;
import jsweet.lang.Replace;
import sehub.SehubSecurityManager;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component()
public class Navigation extends NavigationTOP {

  @Override
  public Boolean isSignedIn() {
    return SehubSecurityManager.isSignedIn();
  }

  @Override
  public String getCurrentUserName() {
    return SehubSecurityManager.getCurrentUserName();
  }

  @Override
  public Consumer<Void> signOut() {
    return $ -> {
      logoutOnBackend();
      SehubSecurityManager.signOut();
      AppRouter.navigateToGuiLoginLoginPage();
    };
  }

  @Replace("fetch('/umlp/api/logout');")
  private void logoutOnBackend() {
  }
}
