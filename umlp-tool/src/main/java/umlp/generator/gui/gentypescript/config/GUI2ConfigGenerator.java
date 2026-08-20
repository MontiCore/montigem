/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript.config;

import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.io.paths.MCPath;
import umlp.generator.gui.GUIGeneratorUtil;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GUI2ConfigGenerator {

  public final static String APP_FILE_PREFIX = "App";
  public final static String ROUTE_FILE_SUFFIX = "GemRouting";
  public final static String MODULE_FILE_SUFFIX = "GemModule";
  public final static String CONFIG_FILE_SUFFIX = "GemConfig";
  public final static String TS_FILE_EXTENSION = "ts";
  public final static String HTML_FILE_EXTENSION = "html";

  public final static String APP_ROUTE_FILE = APP_FILE_PREFIX + ROUTE_FILE_SUFFIX + "." + TS_FILE_EXTENSION;
  public final static String APP_ROUTE_TOP_FILE = APP_FILE_PREFIX + ROUTE_FILE_SUFFIX + "TOP" + "." + TS_FILE_EXTENSION;

  public final static String APP_CONFIG_FILE = APP_FILE_PREFIX + CONFIG_FILE_SUFFIX + "." + TS_FILE_EXTENSION;
  public final static String APP_CONFIG_TOP_FILE = APP_FILE_PREFIX + CONFIG_FILE_SUFFIX + "TOP" + "." + TS_FILE_EXTENSION;

  public final static String APP_COMPONENT = APP_FILE_PREFIX+"Component";
  public final static String APP_TS = APP_COMPONENT+"."+TS_FILE_EXTENSION;
  public final static String APP_HTML = APP_COMPONENT+"."+HTML_FILE_EXTENSION;

  protected static final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();
  protected final GeneratorEngine generatorEngine;
  protected GeneratorSetup setup;
  protected MCPath hwcPath;
  protected final Optional<String> domainName;

  public GUI2ConfigGenerator(GeneratorSetup generatorSetup, Optional<String> domainName) {
    this.generatorEngine = new GeneratorEngine(generatorSetup);
    this.setup = generatorSetup;
    this.hwcPath = generatorSetup.getHandcodedPath();
    this.domainName = domainName;
  }

  public List<String> generateConfig(Collection<PackageRoute> allRoutes) {
    // fill route objects with extra info
    addExtraInfo(allRoutes);

    List<String> res = new ArrayList<>();
    // generate config for each route and add info to the route objects for the next step
    for (PackageRoute route : allRoutes) {
      res.add(generateRouteFiles(route));
    }
    // generate global config
    res.addAll(generateAppConfigFiles(allRoutes));

    // generate app component
    if(domainName.isPresent()) {
      res.addAll(generateAppComponent(domainName.get()));
    }
    return res;
  }

  /**
   * Fill PackageRoute objects with extra info, such as if handwritten route exists and which
   * routes are subroutes of other routes.
   *
   * @param allRoutes All available routes.
   */
  protected void addExtraInfo(Collection<PackageRoute> allRoutes) {
    // preparation
    for (PackageRoute r1 : allRoutes) {
      // set if HW file exists
      String dir = r1.getImportDir();
      String name = r1.getRoutingFileNameBase();
      boolean existHWRoute = util.existsHandwrittenFile(
          hwcPath, dir + "/" + name + ROUTE_FILE_SUFFIX + "." + TS_FILE_EXTENSION);
      r1.setExistsHW(existHWRoute);

      // set subroutes for each route
      for (PackageRoute r2 : allRoutes) {
        if (r1.isParentRouteOf(r2)) {
          r1.addIfSubroute(r2);
        }
      }
    }
  }

  protected String generateRouteFiles(PackageRoute packageRoute) {
    Path generatedRoutingFile = packageRoute.getGenPath();
    List<ComponentRoute> componentRoutes = getPackageRoutes(packageRoute);
    Optional<ComponentRoute> defaultRoute = getDefaultRoute(componentRoutes);
    packageRoute.setDefaultRoute(defaultRoute);

    GeneratorEngine genEngine = new GeneratorEngine(setup);
    genEngine.generateNoA("umlp.gui2ts.TSPackageRoutes", generatedRoutingFile,
        packageRoute, componentRoutes, defaultRoute);

    return generatedRoutingFile.toString();
  }

  protected List<String> generateAppConfigFiles(Collection<PackageRoute> allRoutes) {
    GeneratorEngine genEngine = new GeneratorEngine(setup);

    Optional<String> appRoutePath = util.getRelativeImportPathForAppConfigFile(
        hwcPath, setup.getOutputDirectory().toPath(),
        APP_FILE_PREFIX + ROUTE_FILE_SUFFIX, TS_FILE_EXTENSION);
    boolean existHWRoute = appRoutePath.isPresent();

    List<PackageRoute> routes = findTopMostRoutes(allRoutes);
    String defaultRouting = "";
    for (PackageRoute r : routes) {
      if (r.getDefaultRoute().isPresent()) {
        defaultRouting = r.getURLSegment();
      }
    }
    Path genAppRoutePath = Paths.get(existHWRoute ? APP_ROUTE_TOP_FILE : APP_ROUTE_FILE);
    genEngine.generateNoA("umlp.gui2ts.TSAppRoutes",
        genAppRoutePath, getAppPath2RouteMap(routes), defaultRouting);

    Optional<String> appConfigPath = util.getRelativeImportPathForAppConfigFile(
        hwcPath, setup.getOutputDirectory().toPath(),
        APP_FILE_PREFIX + CONFIG_FILE_SUFFIX, TS_FILE_EXTENSION);
    boolean existHWConfig = appConfigPath.isPresent();

    Path genAppConfigsPath = Paths.get(existHWConfig ? APP_CONFIG_TOP_FILE : APP_CONFIG_FILE);
    genEngine.generateNoA("umlp.gui2ts.TSAppConfig", genAppConfigsPath);

    Path generatedModuleFiles = Paths.get(APP_FILE_PREFIX + MODULE_FILE_SUFFIX + "." + TS_FILE_EXTENSION);

    return Arrays.asList(genAppRoutePath.toString(), generatedModuleFiles.toString());
  }

  protected List<String> generateAppComponent(String domainName) {
    List<String> generatedFiles = new ArrayList<>();
    GeneratorEngine genEngine = new GeneratorEngine(setup);

    // TODO this is only used for existence checking
    Optional<String> appPath = util.getRelativeImportPathForAppConfigFile(
        hwcPath, setup.getOutputDirectory().toPath(),
        APP_COMPONENT, TS_FILE_EXTENSION);
    boolean existHWApp = appPath.isPresent();

    String pathAppTs = "./" + APP_TS;
    String htmlPath = "./" + APP_HTML;
    List<String> scssPaths = new ArrayList<>();

    Optional<String> hwHtmlPath = util.getRelativeImportPath(
        hwcPath, setup.getOutputDirectory().toPath(), APP_COMPONENT, "html");
    if (hwHtmlPath.isPresent()) {
      htmlPath = hwHtmlPath.get();
    } else {
      Path htmlPathAsPath = Paths.get(htmlPath);
      genEngine.generateNoA("umlp.gui2html.AppComponent", htmlPathAsPath);
      generatedFiles.add(htmlPathAsPath.toString());
    }
    Optional<String> hwScssPath = util.getRelativeImportPath(
        hwcPath, setup.getOutputDirectory().toPath(), APP_COMPONENT,"scss");
    if (hwScssPath.isPresent()) {
      scssPaths.add(hwScssPath.get());
    } else {
      Path scssPath = Paths.get("./"+APP_COMPONENT+".scss");
      genEngine.generateNoA("umlp.gui2html.AppScss", scssPath);
      generatedFiles.add(scssPath.toString());
    }
    if (!existHWApp) {
      Path pathAppTsAsPath = Paths.get(pathAppTs);
      genEngine.generateNoA("umlp.gui2ts.TSApp", pathAppTsAsPath, domainName, htmlPath, scssPaths);
      generatedFiles.add(pathAppTsAsPath.toString());
    }

    return generatedFiles;
  }

  /**
   * Collect route configurations for a specific package. For example, given pages
   * "foo/bar/MyPage.gui" and "foo/OtherPage.gui", package "foo.bar" gets routing information of
   * "MyPage.gui", package "foo" gets routing information of "OtherPage.gui".
   *
   * @param route - routing information of a package.
   * @return list of component routing information belonging to the package.
   */
  protected List<ComponentRoute> getPackageRoutes(PackageRoute route) {
    List<ComponentRoute> packageRoutes = new ArrayList<>();

    for (ASTGUICompilationUnit component : route.getComponents()) {
      Optional<String> filePath = util.getRelativeImportPathForClass(
          hwcPath, setup.getOutputDirectory().toPath(),
          component.getComponentQName()
      );
      boolean existHWC = filePath.isPresent();
      String componentFilePath;
      if (existHWC) {
        componentFilePath = component.getComponentQName().replace(".","/");
      }
      else {
        componentFilePath = "./" + component.getComponentName() + "Component";
      }
      List<String> idStrings = util.getRoutingIds(component);

      ComponentRoute componentRouting = new ComponentRoute(
          component,
          existHWC,
          idStrings,
          componentFilePath,
          route.getPathStringNoExt()
      );
      packageRoutes.add(componentRouting);
    }

    return packageRoutes;
  }

  /**
   * Select a default component to navigate to for a package.
   *
   * @param routes - a list of component routing information
   * @return default component for routing (navigation) when URL is underspecified
   */
  protected Optional<ComponentRoute> getDefaultRoute(List<ComponentRoute> routes) {
    for (ComponentRoute route : routes) {
      if (!route.getHasIds()) {
        return Optional.of(route);
      }
    }
    return Optional.empty();
  }

  protected List<PackageRoute> findTopMostRoutes(Collection<PackageRoute> allRoutes) {
    List<PackageRoute> routes = new ArrayList<>();

    for (PackageRoute r : allRoutes) {
      if (r.getParent().isEmpty()) {
        routes.add(r);
      }
    }
    return routes;
  }

  protected Map<String, PackageRoute> getAppPath2RouteMap(Collection<PackageRoute> routes) {
    Map<String, PackageRoute> routeMap = new LinkedHashMap<>();
    for (PackageRoute route : routes) {
      String relativePath = "./" + route.getPathFromRoot();
      Path appRouteFolder = setup.getOutputDirectory().toPath();
      String hwFile = route.getPathFromRoot();

      Optional<URL> hwFilePath = hwcPath.find(hwFile);
      Optional<String> res = Optional.empty();
      if (hwFilePath.isPresent()) {
        Optional<Path> hwPath = MCPath.toPath(hwFilePath.get());
        if (hwPath.isPresent()) {
          res = Optional.of(route.getPathFromRoot());

          Reporting.reportUseHandwrittenCodeFile(hwPath.get(), Paths.get(hwFile));
        }
      }
      Reporting.reportHWCExistenceCheck(hwcPath, Paths.get(hwFile), hwFilePath);

      if (res.isPresent()) {
        relativePath = res.get();
      }
      routeMap.put(util.removeExtension(relativePath), route);
    }
    return routeMap;
  }

}
