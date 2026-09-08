/* (c) https://github.com/MontiCore/monticore */
package sehub;

import de.se_rwth.commons.logging.Log;
import dummy.DummyDataCreator;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import umlp.backendrte.common.GemList;

@SpringBootApplication(
    exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class},
    scanBasePackages={"umlp.backendrte.service.websocket", "umlp.backendrte.service.rest", "sehub.service", "security"}
)
public class SehubServerApplication extends sehub.SehubServerApplicationTOP {

  public  static  void main (String[] args){
    SpringApplication.run(SehubServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();

    // Only create DummyData if database is empty
    if(SehubManager.getUserList().isEmpty()){ // Is there a better way to check if dummy-data was already created?
      Log.println("Creating Dummy Data");
      createDummyData();
    } else {
      Log.println("_NOT_ Creating New Dummy Data. Data is already present.");
    }

    try {
      Server webServer = Server.createWebServer("-web", "-webAllowOthers").start();
      Log.info(String.format("H2 Web Server started at: http://localhost:%s", webServer.getPort()), SehubServerApplication.class.getName());
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  public void createDummyData(){
    // Projects, Directories and FileArtifacts are extracted from the source tree.
    // This runs first so that users, issues and posts can be attached to a real
    // Project - otherwise the association tables on the generated pages stay empty.
    new DummyDataCreator().createDummyData();
    Project project = getFirstProject();

    createDummyUsers(project);
    createDummyPosts(project);
    createDummyIssues(project);
  }

  private Project getFirstProject() {
    GemList<Project> projects = SehubManager.getProjectList();
    if (projects.isEmpty()) {
      Log.warn("No Project was extracted, users and issues stay unassigned.");
      return null;
    }
    return projects.get(0);
  }

  private void createDummyUsers(Project project) {
    PasswordEncoder encoder = new BCryptPasswordEncoder();

    createUser("MyUser", encoder.encode("MyPasswort"), "my.user@sehub.de", SehubRole.DEVELOPER, project);
    createUser("admin", encoder.encode("pwd"), "admin@sehub.de", SehubRole.OWNER, project);
  }

  private Optional<User> createUser(String name, String encodedPassword, String email,
                                    SehubRole role, Project project) {
    Optional<User> user = SehubManager.userBuilder()
        .username(name)
        .password(encodedPassword)
        .registrationDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .email(email)
        .role(role)
        .build();

    if (user.isPresent() && project != null) {
      project.addUsers(user.get());
    }
    return user;
  }

  private void createDummyPosts(Project project) {
    User author = SehubManager.getUserList().get(0);

    Optional<Post> p1 = SehubManager.postBuilder()
        .author(author)
        .title(" | 15:57 ]: Koennen Wasserkocher Fliegen ?")
        .text("Nein")
        .responses(Collections.emptySet())
        .build();

    Optional<Post> p2 = SehubManager.postBuilder()
        .author(author)
        .title(" | 16:34 ]: Koennen Wasserkocher Fliegen ?")
        .text("Ja, waehrend eines FoWos")
        .responses(Collections.emptySet())
        .build();

    Set<Post> responses = new java.util.HashSet<>(Collections.emptySet());
    responses.add(p1.get());
    responses.add(p2.get());

    Optional<Post> p3 = SehubManager.postBuilder()
        .author(author)
        .title("Koennen Wasserkocher Fliegen ?")
        .text("Wir sollten diese Frage vor dem FoWo klaeren! Ggf. sollten Wasserkocher fuer eine Testreihe organisiert werden!")
        .responses(responses)
        .build();

    createIssue("Die Sache mit dem Wasserkocher",
        "Hier kann eine Beschreibung stehen...",
        IssueState.IN_PROGRESS, Set.of(p3.get()), project);
  }

  private void createDummyIssues(Project project) {
    createIssue("Verfuegbare Konten sollen beim Bearbeiten von Dienstreisen auswaehlbar sein",
        "Wenn eine bestehende Dienstreise bearbeitet wird, kann kein neues Konto ausgewaehlt werden. " +
            "Zuerst muss die zu bearbeitende Dienstreise geloescht, gespeichert und erneut bearbeitet werden. " +
            "Erst dann werden die Kontooptionen angezeigt. " +
            "Es sollte moeglich sein, direkt ein neues Konto auszuwaehlen.",
        IssueState.DONE, Collections.emptySet(), project);

    createIssue("Aenderungsdatum Erweiterung",
        "Das Aenderungsdatum fuer Buchungen, Mittelabrufe, Rechnungen, Mittelzuweisungen und Stellenzuweisungen " +
            "ist aktuell nur auf der Seite Finanzen/Zahlungen sichtbar. Das Aenderungsdatum soll aber auch fuer die " +
            "einzelnen Elemente auf den Kontenseiten in einem Konto selbst sichtbar sein.",
        IssueState.IN_PROGRESS, Collections.emptySet(), project);

    createIssue("Finanzen: Umfang einer Zuweisung zu Organigrammstufe im Konto beschraenken",
        "Bei der Zuweisung einer Organigrammstufe in einem Konto kann ein Umfang hinterlegt werden. " +
            "Standardmaessig ist der Umfang mit 100% vorausgefuellt. Der Umfang kann allerdings bearbeitet werden und " +
            "auf mehr als 100% angepasst werden. Eine Zuweisung von mehr als 100% ergibt jedoch keinen Sinn, weshalb " +
            "der maximal zulaessige Umfang auf 100% beschraenkt werden sollte.",
        IssueState.UNASSIGNED, Collections.emptySet(), project);
  }

  private Optional<Issue> createIssue(String title, String description, IssueState state,
                                      Set<Post> posts, Project project) {
    Optional<Issue> issue = SehubManager.issueBuilder()
        .title(title)
        .description(description)
        .state(state)
        .posts(posts)
        .build();

    if (issue.isPresent() && project != null) {
      project.addIssues(issue.get());
    }
    return issue;
  }
}
