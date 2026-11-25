package gui.admin;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admin login: the login button stays disabled until a password has been typed in.
 */
public class LoginPageTest extends KitchenCompanionMockTestSetup {

  public LoginPageTest() {
    super(ClientSetup.of(1));
  }

  private LoginPage openLoginPage() {
    getClients(0).activate();
    // init() is skipped on purpose: in a dev environment it redirects straight to the admin page
    return new LoginPage();
  }

  @Test
  public void loginButtonIsDisabledWithoutPassword() {
    LoginPage page = openLoginPage();

    assertTrue(page.getLoginBtnDisabled());

    page.setPasswordEntry("");
    assertTrue(page.getLoginBtnDisabled());
  }

  @Test
  public void loginButtonIsEnabledAsSoonAsAPasswordIsTyped() {
    LoginPage page = openLoginPage();

    page.setPasswordEntry("s");
    assertFalse(page.getLoginBtnDisabled());

    // deleting the password again disables the button again
    page.setPasswordEntry("");
    assertTrue(page.getLoginBtnDisabled());
  }
}
