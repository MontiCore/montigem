package gui;/* (c) https://github.com/MontiCore/monticore */

import app.AppRouter;
import sehub.SehubSecurityManager;
import umlp.guiutil.Router;
import umlp.jsweet.extension.annotation.Component;

@Component
public class Start extends StartTOP {

  @Override
  public void init() {
    if (!SehubSecurityManager.isSignedIn()) {
      AppRouter.navigateToGuiLoginLoginPage();
    }
    super.init();
  }
}


