package gui.admin;

import kitchencompanion.commands.CheckAdminPassword;
import kitchencompanion.commands.IsDevEnvironment;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandResult;

public abstract class LoginServiceFront {

  private static boolean loggedIn = false;

  /** Whether the backend runs without the environment variable "deploy" set to "true". */
  private static boolean devEnvironment = false;

  private static boolean devEnvironmentChecked = false;

  public static void setLoggedIn(boolean loggedIn) {
    LoginServiceFront.loggedIn = loggedIn;
  }

  public static boolean getLoggedIn() {
    // in a dev environment nobody has to log in
    return loggedIn || isDevEnvironment();
  }

  public static boolean login(String password) {
    if (isDevEnvironment()) {
      setLoggedIn(true);
      return true;
    }
    CommandResult<Boolean> result = CommandManager.executeCommand(new CheckAdminPassword(password));
    boolean canLogin = result.isSuccessful() && Boolean.TRUE.equals(result.asSimpleResult().get());
    if (canLogin) {
      setLoggedIn(true);
    }
    return canLogin;
  }

  public static boolean isDevEnvironment() {
    if (!devEnvironmentChecked) {
      CommandResult<Boolean> result = CommandManager.executeCommand(new IsDevEnvironment());
      if (result.isSuccessful()) {
        devEnvironment = Boolean.TRUE.equals(result.asSimpleResult().get());
        devEnvironmentChecked = true;
      }
    }
    return devEnvironment;
  }
}
