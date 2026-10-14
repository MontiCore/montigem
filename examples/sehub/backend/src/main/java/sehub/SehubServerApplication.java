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
    
    // More data for the generated pages. It deliberately mixes valid objects with ones that
    // violate the rules in models/ocl, so that every constraint card on the overview pages
    // shows both: rules that hold and rules with violating objects to click on.
    Project tutorial = createProject("montigem3-tutorial", "MontiGem3 Tutorial",
        "https://git.rwth-aachen.de/monticore/umlp-anwendungen/montigem3-tutorial.git");
    // violates FullNameContainsName, GitUrlIsHttps and ProjectHasMembers
    Project dt = createProject("digital-twin", "Digital Twin",
        "git@git.rwth-aachen.de:monticore/umlp-anwendungen/digital-twin.git");

    createDummyUsers(project, tutorial);
    createDummyPosts(project);
    createDummyIssues(project, tutorial, dt);
  }

  private Project getFirstProject() {
    GemList<Project> projects = SehubManager.getProjectList();
    if (projects.isEmpty()) {
      Log.warn("No Project was extracted, users and issues stay unassigned.");
      return null;
    }
    return projects.get(0);
  }

  private void createDummyUsers(Project p1, Project p2) {
    PasswordEncoder encoder = new BCryptPasswordEncoder();

    createUser("MyUser", encoder.encode("MyPasswort"), "my.user@sehub.de", SehubRole.DEVELOPER, p1);
    createUser("admin", encoder.encode("pwd"), "admin@sehub.de", SehubRole.OWNER, p1);
    
    String password = encoder.encode("sehub");
    
    // a user belongs to exactly one project
    createUser("lena.becker", password, "lena.becker@sehub.de", SehubRole.MAINTAINER, p2);
    createUser("jonas.wolf", password, "jonas.wolf@se-rwth.de", SehubRole.DEVELOPER, p2);
    createUser("sarah.klein", password, "sarah.klein@sehub.de", SehubRole.REPORTER, p2);
    
    // violates UsernameWithoutSpaces
    createUser("Max Mustermann", password, "max.mustermann@sehub.de", SehubRole.GUEST, p1);
    // violates ValidEmail, and every post of this user violates AuthorHasEmail
    createUser("tim.hoffmann", password, "tim.hoffmann(at)sehub", SehubRole.DEVELOPER, p1);
    // violates ValidEmail: no domain
    createUser("anna.schulz", password, "anna.schulz@localhost", SehubRole.REPORTER, p1);
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
        .title(" [ 15:57 ]: Können Wasserkocher fliegen?")
        .text("Nein")
        .responses(Collections.emptySet())
        .build();

    Optional<Post> p2 = SehubManager.postBuilder()
        .author(author)
        .title(" [ 16:34 ]: Können Wasserkocher fliegen?")
        .text("Ja, während eines FoWos")
        .responses(Collections.emptySet())
        .build();

    Set<Post> responses = new java.util.HashSet<>(Collections.emptySet());
    responses.add(p1.get());
    responses.add(p2.get());

    Optional<Post> p3 = SehubManager.postBuilder()
        .author(author)
        .title("Können Wasserkocher fliegen?")
        .text("Wir sollten diese Frage vor dem FoWo klären! Ggf. sollten Wasserkocher für eine Testreihe organisiert werden!")
        .responses(responses)
        .build();

    createIssue("Die Sache mit dem Wasserkocher",
        "Hier kann eine Beschreibung stehen...",
        IssueState.IN_PROGRESS, IssueType.TEST, false, 1, Set.of(p3.get()), project);
  }

  private void createDummyIssues(Project p1, Project p2, Project p3) {
    createIssue("Verfuegbare Konten sollen beim Bearbeiten von Dienstreisen auswaehlbar sein",
        "Wenn eine bestehende Dienstreise bearbeitet wird, kann kein neues Konto ausgewaehlt werden. " +
            "Zuerst muss die zu bearbeitende Dienstreise geloescht, gespeichert und erneut bearbeitet werden. " +
            "Erst dann werden die Kontooptionen angezeigt. " +
            "Es sollte moeglich sein, direkt ein neues Konto auszuwaehlen.",
        IssueState.DONE, IssueType.BUG, true, 5, Collections.emptySet(), p1);

    createIssue("Aenderungsdatum Erweiterung",
        "Das Aenderungsdatum fuer Buchungen, Mittelabrufe, Rechnungen, Mittelzuweisungen und Stellenzuweisungen " +
            "ist aktuell nur auf der Seite Finanzen/Zahlungen sichtbar. Das Aenderungsdatum soll aber auch fuer die " +
            "einzelnen Elemente auf den Kontenseiten in einem Konto selbst sichtbar sein.",
        IssueState.IN_PROGRESS, IssueType.FEATURE, false, 3, Collections.emptySet(), p1);

    createIssue("Finanzen: Umfang einer Zuweisung zu Organigrammstufe im Konto beschraenken",
        "Bei der Zuweisung einer Organigrammstufe in einem Konto kann ein Umfang hinterlegt werden. " +
            "Standardmaessig ist der Umfang mit 100% vorausgefuellt. Der Umfang kann allerdings bearbeitet werden und " +
            "auf mehr als 100% angepasst werden. Eine Zuweisung von mehr als 100% ergibt jedoch keinen Sinn, weshalb " +
            "der maximal zulaessige Umfang auf 100% beschraenkt werden sollte.",
        IssueState.UNASSIGNED, IssueType.BUG, false, 2, Collections.emptySet(), p1);
    
    User lena = findUser("lena.becker");
    User jonas = findUser("jonas.wolf");
    User sarah = findUser("sarah.klein");
    User max = findUser("Max Mustermann");
    User tim = findUser("tim.hoffmann");
    
    // A thread with two levels of responses, all marked with "Re: "
    Post buildFixConfirmed = createPost(jonas, "Re: Re: Build schlaegt unter Windows fehl",
        "Danke, mit 7.9.0-SNAPSHOT laeuft der Build wieder durch.", Set.of());
    Post buildFix = createPost(lena, "Re: Build schlaegt unter Windows fehl",
        "Das liegt an der MontiCore-Version von cd2gui. Setz sie in den gradle.properties auf "
            + "7.9.0-SNAPSHOT.", Set.of(buildFixConfirmed));
    Post buildQuestion = createPost(jonas, "Build schlaegt unter Windows fehl",
        "Beim Aufruf von gradle build bricht jede cd2java-Task mit 0xFD777 ab.", Set.of(buildFix));
    createIssue("Build schlaegt unter Windows fehl",
        "Seit dem letzten Update von cd2gui laesst sich das Tutorial unter Windows nicht mehr bauen.",
        IssueState.DONE, IssueType.BUILD, true, 8, Set.of(buildQuestion), p2);
    
    // violates ResponseIsMarked
    Post screenshotAnswer = createPost(lena, "Aktualisiere ich morgen",
        "Ich mache morgen neue Screenshots mit dem aktuellen Layout.", Set.of());
    // violates PostHasText and, through its author, AuthorHasEmail
    Post screenshotEmpty = createPost(tim, "Re: Screenshots in Kapitel 3", "", Set.of());
    Post screenshotQuestion = createPost(sarah, "Screenshots in Kapitel 3",
        "Die Screenshots zeigen noch die alte Navigationsleiste.",
        Set.of(screenshotAnswer, screenshotEmpty));
    createIssue("Kapitel 3: Screenshots veraltet",
        "Die Screenshots in Kapitel 3 passen nicht mehr zur aktuellen Oberflaeche.",
        IssueState.IN_PROGRESS, IssueType.DOCUMENTATION, false, 1, Set.of(screenshotQuestion), p2);
    
    // violates IssueHasDescription
    createIssue("Tippfehler in der Einleitung", "", IssueState.UNASSIGNED, IssueType.DOCUMENTATION, false, 1, Set.of(), p2);
    
    // violates IssueTitleShort; its post violates TitleNotPadded
    Post loadTime = createPost(max, " Ladezeit messen",
        "Ab wie vielen Projekten wird das Dashboard spuerbar langsam?", Set.of());
    createIssue("Dashboard laedt bei vielen Projekten sehr langsam und blockiert die Oberflaeche",
        "Mit mehr als 50 Projekten dauert das Laden des Dashboards mehrere Sekunden.",
        IssueState.UNASSIGNED, IssueType.PERFORMANCE, true, 13, Set.of(loadTime), p1);
    
    // violates IssueHasDescription
    createIssue("Suche nach Artefakten", "", IssueState.UNASSIGNED, IssueType.FEATURE, false, 8, Set.of(), p1);
    
    createIssue("Dark Mode ausprobieren",
        "Ein dunkles Farbschema fuer alle Seiten, umschaltbar in der Navigationsleiste.",
        IssueState.UNASSIGNED, IssueType.FEATURE, false, 5, Set.of(), p3);
  }

  private Project createProject(String name, String fullName, String git) {
    return SehubManager.projectBuilder()
        .name(name)
        .fullName(fullName)
        .git(git)
        .build()
        .orElse(null);
  }

  private Post createPost(User author, String title, String text, Set<Post> responses) {
    return SehubManager.postBuilder()
        .author(author)
        .title(title)
        .text(text)
        .responses(new java.util.HashSet<>(responses))
        .build()
        .orElse(null);
  }

  private User findUser(String username) {
    for (User user : SehubManager.getUserList()) {
      if (user.getUsername().equals(username)) {
        return user;
      }
    }
    throw new IllegalStateException("Dummy user " + username + " was not created");
  }

  /**
   * @param storyPoints estimated effort, following the Fibonacci numbers 1, 2, 3, 5, 8, 13
   */
  private Optional<Issue> createIssue(String title, String description, IssueState state, IssueType type,
                                      boolean blocker, int storyPoints, Set<Post> posts, Project project) {
    Optional<Issue> issue = SehubManager.issueBuilder()
        .title(title)
        .description(description)
        .state(state)
        .type(type)
        .blocker(blocker)
        .storyPoints(storyPoints)
        .posts(posts)
        .build();

    if (issue.isPresent() && project != null) {
      project.addIssues(issue.get());
    }
    return issue;
  }
}
