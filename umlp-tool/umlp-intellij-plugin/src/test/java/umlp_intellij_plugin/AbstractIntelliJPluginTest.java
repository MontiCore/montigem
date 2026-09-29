package umlp_intellij_plugin;

import de.mclsg.plugin.test.AbstractLanguageServerPluginTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractIntelliJPluginTest extends AbstractLanguageServerPluginTest {

  @Override
  protected void setupProjectFiles() {
    Path basePath = Path.of("src/test/montigem-template");
    assertTrue(Files.exists(basePath), "Can not find src/test/montigem-template. " +
        "Please clone https://git.rwth-aachen.de/monticore/umlp-anwendungen/montigem-templates/montigem-template into this directory.");
    copyGitWorkingCopyToProject(basePath);
  }
}
