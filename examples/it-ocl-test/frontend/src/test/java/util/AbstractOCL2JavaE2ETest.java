package util;

import com.microsoft.playwright.Page;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import mydomain.test_infrastructure.AbstractMydomainE2ETest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import umlp.backendrte.common.DatabaseAccessManager;
import umlp.backendrte.common.GemIdentifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractOCL2JavaE2ETest<T extends GemIdentifiable> extends AbstractMydomainE2ETest implements IOCL2XTest<T> {
  protected final HashMap<String, String> constraints = new HashMap<>();
  protected final HashMap<String, String> constraintPageRoutes = new HashMap<>();
  protected final HashMap<String, List<String>> constraintButtonClasses = new HashMap<>();
  protected Page page;
  protected final String pageRoute;

  protected AbstractOCL2JavaE2ETest() {
    this(null);
  }

  protected AbstractOCL2JavaE2ETest(String pageRoute) {
    this.pageRoute = pageRoute;
  }

  @BeforeEach
  void init(Page page) {
    this.page = page;
    page.onConsoleMessage(cm -> System.out.println("[BROWSER] " + cm.text()));
  }

  @BeforeAll
  public void initConstraints(){
    _initConstraints();
  }

  @Override
  public List<Long> translateAndExecQuery(String constraint) {
    String route = pageRouteFor(constraint);
    navAndWait(page, route);
    String pageName = route.substring(route.lastIndexOf('/') + 1);
    waitInit(page, pageName);
    waitForAngular(page);

    Set<Long> gemIds = new LinkedHashSet<>();
    for (String buttonClass : buttonClassesFor(constraint)) {
      page.locator("." + buttonClass).click();
      waitForAngular(page);
      waitForBackend();
      waitForAngular(page);
      gemIds.addAll(readResultGemIds());
    }
    return new ArrayList<>(gemIds);
  }

  @Override
  public List<Long> translateAndExecQueryFromPath(String path) {
    return translateAndExecQuery(path);
  }

  @Override
  public List<Long> _translateAndExecQuery(ASTOCLCompilationUnit ocl) {
    throw new IllegalStateException("Should never be reached!");
  }

  protected void _initConstraints() {
  }

  protected void registerGeneratedOclPages(Collection<String> constraints) {
    constraints.forEach(this::registerGeneratedOclPage);
  }

  protected void registerGeneratedOclPage(String constraint) {
    registerConstraint(constraint, generatedOclPageRoute(constraint), generatedButtonClass(constraint));
  }

  protected void registerGeneratedOclPage(String constraint, int invariantCount) {
    if (invariantCount < 1) {
      throw new IllegalArgumentException("Expected at least one invariant button for " + constraint);
    }
    if (invariantCount == 1) {
      registerGeneratedOclPage(constraint);
      return;
    }

    List<String> buttonClasses = new ArrayList<>();
    String prefix = generatedButtonClass(constraint);
    for (int i = 0; i < invariantCount; i++) {
      buttonClasses.add(prefix + "Inv" + i);
    }
    registerConstraint(constraint, generatedOclPageRoute(constraint), buttonClasses);
  }

  protected void registerGeneratedOclPage(String constraint, String... buttonClasses) {
    registerConstraint(constraint, generatedOclPageRoute(constraint), Arrays.asList(buttonClasses));
  }

  protected void registerConstraint(String constraint, String pageRoute, String buttonClass) {
    registerConstraint(constraint, pageRoute, List.of(buttonClass));
  }

  protected void registerConstraint(String constraint, String pageRoute, List<String> buttonClasses) {
    if (buttonClasses.isEmpty()) {
      throw new IllegalArgumentException("Expected at least one button class for " + constraint);
    }
    constraints.put(constraint, buttonClasses.get(0));
    constraintPageRoutes.put(constraint, pageRoute);
    constraintButtonClasses.put(constraint, List.copyOf(buttonClasses));
  }

  protected String generatedOclPageRoute(String constraint) {
    return "/ocl2java/" + getResourceFolder().toLowerCase() + "/" + constraint + "Page";
  }

  protected String generatedButtonClass(String constraint) {
    if (constraint.isEmpty()) {
      return constraint;
    }
    return constraint.substring(0, 1).toLowerCase() + constraint.substring(1);
  }

  protected String pageRouteFor(String constraint) {
    String registeredRoute = constraintPageRoutes.get(constraint);
    if (registeredRoute != null) {
      return registeredRoute;
    }
    if (pageRoute != null && !pageRoute.isBlank()) {
      return pageRoute;
    }
    return generatedOclPageRoute(constraint);
  }

  protected List<String> buttonClassesFor(String constraint) {
    List<String> buttonClasses = constraintButtonClasses.get(constraint);
    if (buttonClasses != null) {
      return buttonClasses;
    }

    String buttonClass = constraints.get(constraint);
    if (buttonClass != null) {
      return List.of(buttonClass);
    }
    return List.of(generatedButtonClass(constraint));
  }

  protected List<Long> readResultGemIds() {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 5000) {
      String text = page.locator(".resultGemText").textContent().trim();
      if (!"Click a button to fill".equals(text)) {
        break;
      }
      page.waitForTimeout(50);
    }
    String gemIdStr = page.locator(".resultGemText").textContent().trim();
    if (gemIdStr.isEmpty() || "Click a button to fill".equals(gemIdStr)) {
      return List.of();
    }
    return Arrays.stream(gemIdStr.split(","))
        .map(String::trim)
        .filter(gemId -> !gemId.isEmpty())
        .map(Long::parseLong)
        .toList();
  }
}
