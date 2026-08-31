/* (c) https://github.com/MontiCore/monticore */
package security;

import de.se_rwth.commons.logging.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sehub.SehubManager;
import sehub.User;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * "Forgot password" has to work before a session exists, so like signup it cannot go over
 * the websocket command channel - see {@link PublicSignupController} for why.
 */
@RestController
public class PublicPasswordResetController {

  /** Relative to the {@code /umlp/api} context path, i.e. {@code POST /umlp/api/public/reset_password}. */
  public static final String RESET_PASSWORD_PATH = "/public/reset_password";

  private static final Pattern EMAIL =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  /** No look-alike characters - the password is read off a log line and typed by hand. */
  private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private static final int GENERATED_PASSWORD_LENGTH = 12;

  private final SecureRandom random = new SecureRandom();

  private final PasswordEncoder passwordEncoder;

  public PublicPasswordResetController(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  public record ResetPasswordRequest(String email) {}

  @PostMapping(value = RESET_PASSWORD_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
    String email = request.email() == null ? "" : request.email().trim();

    if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
      return ResponseEntity.ok(Map.of("success", false, "reason", "Wrong format for Email address!"));
    }

    findByEmail(email).ifPresent(user -> {
      String generated = generatePassword();
      user.setPassword(passwordEncoder.encode(generated));
      // Stands in for the mail that a deployment with a configured mail sender would send.
      Log.info("New password for " + email + ": " + generated,
          PublicPasswordResetController.class.getName());
    });
    
    return ResponseEntity.ok(Map.of("success", true));
  }

  private Optional<User> findByEmail(String email) {
    for (User user : SehubManager.getUserList()) {
      if (email.equals(user.getEmail())) {
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  private String generatePassword() {
    StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
    for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
      password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return password.toString();
  }
}
