/* (c) https://github.com/MontiCore/monticore */
package security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sehub.SehubManager;
import sehub.SehubRole;
import sehub.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Registration has to work before a session exists, so it cannot go over the websocket
 * command channel: {@link WebSecurityConfig} rejects that with 401 until the user is
 * signed in, and the client-side command promise would simply never settle.
 * <p>
 * This is the one endpoint that is reachable unauthenticated. It therefore does the
 * validation itself instead of trusting the browser, and it never hands the user list
 * out to the client.
 */
@RestController
public class PublicSignupController {

  /** Relative to the {@code /umlp/api} context path, i.e. {@code POST /umlp/api/public/signup}. */
  public static final String SIGNUP_PATH = "/public/signup";

  private static final Pattern EMAIL =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  private final PasswordEncoder passwordEncoder;

  public PublicSignupController(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  public record SignupRequest(String username, String password, String email) {}

  @PostMapping(value = SIGNUP_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> signUp(@RequestBody SignupRequest request) {
    String username = trimmed(request.username());
    String email = trimmed(request.email());
    String password = request.password();

    if (username.isEmpty() || email.isEmpty() || password == null || password.isEmpty()) {
      return failure("Please fill in account name, password and e-mail address.");
    }
    if (!EMAIL.matcher(email).matches()) {
      return failure("Wrong format for Email address!");
    }

    for (User existing : SehubManager.getUserList()) {
      if (username.equals(existing.getUsername())) {
        return failure("The account \"" + username + "\" is already in use.");
      }
      if (email.equals(existing.getEmail())) {
        return failure("The e-mail address \"" + email + "\" is already in use.");
      }
    }
    
    Optional<User> created = SehubManager.userBuilder()
        .username(username)
        .password(passwordEncoder.encode(password))
        .email(email)
        .registrationDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .role(SehubRole.DEVELOPER)
        .build();

    if (created.isEmpty()) {
      return failure("The account could not be created.");
    }
    return ResponseEntity.ok(Map.of("success", true));
  }

  private static String trimmed(String value) {
    return value == null ? "" : value.trim();
  }
  
  private static ResponseEntity<Map<String, Object>> failure(String reason) {
    return ResponseEntity.ok(Map.of("success", false, "reason", reason));
  }
}
