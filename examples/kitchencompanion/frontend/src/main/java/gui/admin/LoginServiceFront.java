package gui.admin;

import kitchencompanion.commands.CheckAdminPassword;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandResult;

public abstract class LoginServiceFront {

  private static boolean loggedIn = false;

  public static void setLoggedIn(boolean loggedIn) {
    LoginServiceFront.loggedIn = loggedIn;
  }

  public static boolean getLoggedIn() {
    return loggedIn;
  }
  
  public static boolean login(String password) {
    CommandResult<Boolean> result = CommandManager.executeCommand(new CheckAdminPassword(password));
    boolean canLogin = result.isSuccessful() && Boolean.TRUE.equals(result.asSimpleResult().get());
    if (canLogin) {
      setLoggedIn(true);
    }
    return canLogin;
  }
}
