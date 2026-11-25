package kitchencompanion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The parts of {@link AdminPasswordService} that do not depend on the {@code adminPwHash}
 * environment variable of the machine running the tests.
 */
public class AdminPasswordServiceTest {

  @Test
  public void missingPasswordIsRejected() {
    assertFalse(AdminPasswordService.matches(null));
    assertFalse(AdminPasswordService.matches(""));
  }

  @Test
  public void wrongPasswordIsRejected() {
    assertFalse(AdminPasswordService.matches("definitely-not-the-admin-password"));
  }
}
