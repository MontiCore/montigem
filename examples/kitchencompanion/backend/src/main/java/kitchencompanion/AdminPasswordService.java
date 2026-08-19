package kitchencompanion;

import de.se_rwth.commons.logging.Log;
import jsweet.lang.Erased;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Verifies the admin password against a BCrypt hash.
 */
@Erased
public class AdminPasswordService {

  /** BCrypt hash of the dev password ("admin1234"). Used only when adminPwHash is unset. */
  private static final String DEV_HASH = "$2a$10$Uc0EK4gzphzDDqvfkxwvpO6KRnPCwp/40s0AVqkN6U9nbrd8ghOaS";

  public static boolean matches(String password) {
    if (password == null || password.isEmpty()) {
      return false;
    }
    String hash = System.getenv().getOrDefault("adminPwHash", "");
    if (hash.isEmpty()) {
      if ("true".equalsIgnoreCase(System.getenv("deploy"))) {
        Log.error("0xKC001 No admin password configured. Set the environment variable 'adminPwHash'.");
        return false;
      }
      hash = DEV_HASH;
    }
    return new BCryptPasswordEncoder().matches(password, hash);
  }
}
