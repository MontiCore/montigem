package kitchencompanion;

import de.se_rwth.commons.logging.Log;
import jsweet.lang.Erased;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Verifies the admin password against a BCrypt hash.
 */
@Erased
public class AdminPasswordService {
  
  public static boolean matches(String password) {
    if (password == null || password.isEmpty()) {
      return false;
    }
    String hash = System.getenv().getOrDefault("adminPwHash", "");
    if (hash.isEmpty()) {
      if (DeploymentService.isDeployed()) {
        Log.error("0xKC001 No admin password configured. Set the environment variable 'adminPwHash'.");
        return false;
      }
    }
    return new BCryptPasswordEncoder().matches(password, hash);
  }
  
}
