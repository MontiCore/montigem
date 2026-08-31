/* (c) https://github.com/MontiCore/monticore */
package security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  @Bean
  public CustomUserDetailsService customUserDetailsService() {
    return new CustomUserDetailsService();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    // ToDo: insecure!!!!
    http.csrf(csrf -> csrf.disable());

    http.authorizeHttpRequests((requests) -> requests
        .requestMatchers(HttpMethod.POST, PublicSignupController.SIGNUP_PATH).permitAll()
        .requestMatchers(HttpMethod.POST, PublicPasswordResetController.RESET_PASSWORD_PATH).permitAll()
        .anyRequest().authenticated());
    http.formLogin(form -> form
        .failureHandler((request, response, exception) -> {
          Map<String, Object> data = new HashMap<>();
          data.put("success", false);
          data.put("reason", exception.getMessage());
          sendJSONResponse(response, data);
        })
        .successHandler((request, response, authentication) -> {
          Map<String, Object> data = new HashMap<>();
          data.put("success", true);
          sendJSONResponse(response, data);
        }));
    http.httpBasic(httpBasic -> {});
    return http.build();
  }

  protected void sendJSONResponse(HttpServletResponse response, Map<String, Object> data) throws IOException {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    PrintWriter out = response.getWriter();
    new ObjectMapper().writeValue(out, data);
    out.flush();
  }

}
