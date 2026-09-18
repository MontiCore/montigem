/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

/** Creates and logs in the fixed development user at the local Verdaccio registry. */
public abstract class LocalNpmAdduserTask extends DefaultTask {

  private static final String USERNAME = "umlp";
  private static final String PASSWORD = "umlp-local";
  private static final String EMAIL = "umlp@localhost";
  private static final ReentrantLock PROCESS_LOCK = new ReentrantLock();

  /** Build-local npm configuration to receive the Verdaccio login token. */
  @Internal
  public abstract RegularFileProperty getNpmrcFile();

  @TaskAction
  public void adduser() throws IOException {
    URI registry = URI.create(CreateNpmrcTask.LOCAL_NPM_REPOSITORY);
    Path lockFile = Path.of(System.getProperty("java.io.tmpdir"), "umlp-local-npm-adduser.lock");
    PROCESS_LOCK.lock();
    try {
      try (FileChannel channel = FileChannel.open(lockFile,
          StandardOpenOption.CREATE, StandardOpenOption.WRITE);
           FileLock ignored = channel.lock()) {
        String currentToken = readAuthToken(registry, getNpmrcFile().get().getAsFile().toPath());
        if (currentToken != null && isValidToken(registry, currentToken)) {
          getLogger().lifecycle("Local npm user '{}' is already logged in.", USERNAME);
          return;
        }

        String token = createOrLogin(registry);
        writeAuthToken(registry, token, getNpmrcFile().get().getAsFile().toPath());
        getLogger().lifecycle("Created or logged in local npm user '{}'.", USERNAME);
      }
    }
    finally {
      PROCESS_LOCK.unlock();
    }
  }

  private String createOrLogin(URI registry) throws IOException {
    URI endpoint = registry.resolve("-/user/org.couchdb.user:" + USERNAME);
    JsonObject body = new JsonObject();
    body.addProperty("_id", "org.couchdb.user:" + USERNAME);
    body.addProperty("name", USERNAME);
    body.addProperty("password", PASSWORD);
    body.addProperty("email", EMAIL);
    body.addProperty("type", "user");
    body.add("roles", new JsonArray());
    body.addProperty("date", Instant.now().toString());

    HttpResponse<String> response = putJson(endpoint, body, null);
    if (response.statusCode() == 409) {
      response = loginExistingUser(endpoint, body);
    }
    if (response.statusCode() != 200 && response.statusCode() != 201) {
      throw new GradleException("Local npm user creation/login failed with HTTP "
          + response.statusCode() + ": " + response.body());
    }
    JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
    if (!responseBody.has("token")) {
      throw new GradleException("Local npm registry did not return an authentication token");
    }
    return responseBody.get("token").getAsString();
  }

  private HttpResponse<String> loginExistingUser(URI endpoint, JsonObject body) throws IOException {
    String basicAuth = Base64.getEncoder().encodeToString(
        (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    return putJson(endpoint, body, "Basic " + basicAuth);
  }

  private HttpResponse<String> putJson(URI endpoint, JsonObject body, String authorization)
      throws IOException {
    HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json");
    if (authorization != null) {
      request.header("Authorization", authorization);
    }
    return send(request.PUT(HttpRequest.BodyPublishers.ofString(
        body.toString(), StandardCharsets.UTF_8)).build());
  }

  private boolean isValidToken(URI registry, String token) throws IOException {
    HttpResponse<String> response = send(HttpRequest.newBuilder(registry.resolve("-/whoami"))
        .header("Authorization", "Bearer " + token)
        .GET()
        .build());
    return response.statusCode() == 200;
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException {
    try {
      return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GradleException("Interrupted while accessing the local npm registry", e);
    }
  }

  private String readAuthToken(URI registry, Path npmrc) throws IOException {
    if (!Files.exists(npmrc)) {
      return null;
    }
    String key = authTokenKey(registry);
    return Files.readAllLines(npmrc, StandardCharsets.UTF_8).stream()
        .filter(line -> line.startsWith(key))
        .map(line -> line.substring(key.length()).trim())
        .findFirst()
        .orElse(null);
  }

  private void writeAuthToken(URI registry, String token, Path npmrc) throws IOException {
    Files.createDirectories(npmrc.getParent());
    String key = authTokenKey(registry);
    Files.writeString(npmrc,
        "registry=" + registry + System.lineSeparator()
            + key + token + System.lineSeparator(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private String authTokenKey(URI registry) {
    String registryPath = registry.getRawPath();
    if (registryPath == null || registryPath.isEmpty()) {
      registryPath = "/";
    }
    if (!registryPath.endsWith("/")) {
      registryPath += "/";
    }
    return "//" + registry.getRawAuthority() + registryPath + ":_authToken=";
  }
}
