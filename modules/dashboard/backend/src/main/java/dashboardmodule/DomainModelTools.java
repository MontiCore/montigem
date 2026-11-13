package dashboardmodule;

import dev.langchain4j.agent.tool.Tool;
import jsweet.lang.Erased;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads the application's packaged domain model. */
@Erased
public final class DomainModelTools {
  private final ClassLoader resources;
  private final String resource;

  public DomainModelTools(String resource) {
    this(DomainModelTools.class.getClassLoader(), resource);
  }

  public DomainModelTools(ClassLoader resources, String resource) {
    this.resources = resources;
    this.resource = resource;
  }

  @Tool("Read the application's UMLP class diagram, including domain classes, fields, "
      + "associations and enums. Call *once* before writing scripts that use application data. "
      + "The class diagram will not change within one conversation."
      + "An error means the schema is unavailable; do not guess its contents.")
  public String getDomainModel() {
    try (InputStream stream = resources.getResourceAsStream(resource)) {
      if (stream == null) {
        return "Error: missing UMLP class diagram resource: " + resource;
      }
      String model = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      if (model.isBlank()) {
        return "Error: empty UMLP class diagram resource: " + resource;
      }
      return model;
    } catch (IOException e) {
      return "Error: could not read UMLP class diagram resource " + resource + ": " + e.getMessage();
    }
  }
}
