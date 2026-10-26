/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CreateNpmrcTaskTest {

  @Test
  void createsUnauthenticatedLocalPublishConfiguration() {
    Project project = ProjectBuilder.builder().build();

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, true, true);

    assertEquals("registry=http://127.0.0.1:4873/\n", npmrc);
    assertFalse(npmrc.contains("_auth"));
  }

  @Test
  void keepsNexusAsTheDefaultPublishRepository() {
    Project project = ProjectBuilder.builder().build();
    project.getExtensions().getExtraProperties().set("mavenUser", "user");
    project.getExtensions().getExtraProperties().set("mavenPassword", "password");

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, true, false);

    assertEquals(
        "registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "//nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/:_auth=\"dXNlcjpwYXNzd29yZA==\"",
        npmrc);
  }

  @Test
  void usesNexusForAllScopesByDefault() {
    Project project = ProjectBuilder.builder().build();
    project.getExtensions().getExtraProperties().set("mavenUser", "user");
    project.getExtensions().getExtraProperties().set("mavenPassword", "password");

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, false, false);

    assertEquals(
        "@monticore:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@umlp:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@montigem:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@monticore:_auth=\"dXNlcjpwYXNzd29yZA==\"\n"
            + "@umlp:_auth=\"dXNlcjpwYXNzd29yZA==\"\n"
            + "@montigem:_auth=\"dXNlcjpwYXNzd29yZA==\"",
        npmrc);
  }

  @Test
  void usesLocalRepositoryOnlyForUmlpDownloads() {
    Project project = ProjectBuilder.builder().build();
    project.getExtensions().getExtraProperties().set("mavenUser", "user");
    project.getExtensions().getExtraProperties().set("mavenPassword", "password");

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, false, true);

    assertEquals(
        "@monticore:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@umlp:registry=http://127.0.0.1:4873/\n"
            + "@montigem:registry=http://127.0.0.1:4873/\n"
            + "@monticore:_auth=\"dXNlcjpwYXNzd29yZA==\"",
        npmrc);
  }

  @Test
  void usesNexusAnonymouslyWithoutCredentials() {
    Project project = ProjectBuilder.builder().build();

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, false, false);

    assertEquals(
        "@monticore:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@umlp:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@montigem:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n",
        npmrc);
  }

  @Test
  void usesLocalRepositoryAnonymouslyWithoutCredentials() {
    Project project = ProjectBuilder.builder().build();

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, false, true);

    assertEquals(
        "@monticore:registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n"
            + "@umlp:registry=http://127.0.0.1:4873/\n"
            + "@montigem:registry=http://127.0.0.1:4873/\n",
        npmrc);
  }

  @Test
  void keepsNexusAsPublishRepositoryWithoutCredentials() {
    Project project = ProjectBuilder.builder().build();

    String npmrc = CreateNpmrcTask.getNpmrcContent(project, true, false);

    assertEquals(
        "registry=https://nexus.se.rwth-aachen.de/repository/montigem-frontendrte-snapshots/\n",
        npmrc);
  }
}
