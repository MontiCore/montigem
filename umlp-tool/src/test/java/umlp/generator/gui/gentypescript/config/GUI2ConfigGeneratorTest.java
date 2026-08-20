package umlp.generator.gui.gentypescript.config;

import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.io.paths.MCPath;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIGeneratorUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GUI2ConfigGeneratorTest extends DomainGUITest {

  private ASTGUICompilationUnit dummy;
  private ASTGUICompilationUnit dummy2;
  private ASTGUICompilationUnit dummy3;
  private ASTGUICompilationUnit dummy4;
  private ASTGUICompilationUnit dummy5;
  private ASTGUICompilationUnit dummy6;
  private ASTGUICompilationUnit dummy7;
  private ASTGUICompilationUnit dashboard;
  private ASTGUICompilationUnit update;

  @Test
  public void testAddExtraInfo() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.addExtraInfo(routes);

    for (PackageRoute r : routes) {
      if (r.equals(conf_overridden)) {
        assertTrue(r.existsHW);
      }
      else {
        assertFalse(r.existsHW);
      }
    }

    assertAll(
        () -> assertTrue(conf_route.getChildren().isEmpty()),
        () -> assertTrue(conf_routelonger.getChildren().isEmpty()),
        () -> assertTrue(conf_nested_route.getChildren().isEmpty()),
        () -> assertEquals(2, conf_nested.getChildren().size()),
        () -> assertTrue(conf_nested.getChildren().containsAll(List.of(
            conf_nested_route, conf_nested_deep_deep_deep
        ))),
        () -> assertTrue(conf_nested_deep_deep_deep.getChildren().isEmpty()),
        () -> assertTrue(other_deep_nested.getChildren().isEmpty()),
        () -> assertEquals(4, conf.getChildren().size()),
        () -> assertTrue(conf.getChildren().containsAll(List.of(
            conf_route, conf_routelonger, conf_nested, conf_overridden
        ))),
        () -> assertTrue(conf_overridden.getChildren().isEmpty())
    );
  }

  @Test
  public void testGetPackageRoutes() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.addExtraInfo(routes);

    List<ComponentRoute> conf_overridden_comps = configGenerator.getPackageRoutes(conf_overridden);

    assertEquals(1, conf_overridden_comps.size());
    ComponentRoute update_cr = conf_overridden_comps.get(0);
    assertAll(
        () -> assertTrue(update_cr.getHasIds()),
        () -> assertEquals("Update", update_cr.getImportedSymbolName()),
        () -> assertEquals("overridden/Update/:conf_overridden_Update_person_RoutingId", update_cr.getUrl()),
        () -> assertEquals("overridden", update_cr.getUrlPrefix()),
        // from build folder to resources folder
        () -> assertEquals(
            "conf/overridden/Update",
            update_cr.getImportPathNoExt()
        )
    );

    List<ComponentRoute> conf_nested_deep_deep_deep_comps = configGenerator.getPackageRoutes(conf_nested_deep_deep_deep);

    assertEquals(1, conf_nested_deep_deep_deep_comps.size());
    ComponentRoute dummy5_cr = conf_nested_deep_deep_deep_comps.get(0);
    assertAll(
        () -> assertFalse(dummy5_cr.getHasIds()),
        () -> assertEquals("Dummy5", dummy5_cr.getImportedSymbolName()),
        () -> assertEquals("nested/deep/deep/deep/Dummy5", dummy5_cr.getUrl()),
        () -> assertEquals("nested/deep/deep/deep", dummy5_cr.getUrlPrefix()),
        () -> assertEquals("./Dummy5Component", dummy5_cr.getImportPathNoExt())
    );

    List<ComponentRoute> other_deep_nested_comps = configGenerator.getPackageRoutes(other_deep_nested);

    assertEquals(1, other_deep_nested_comps.size());
    ComponentRoute dummy6_cr = other_deep_nested_comps.get(0);
    assertAll(
        () -> assertFalse(dummy6_cr.getHasIds()),
        () -> assertEquals("Dummy6", dummy6_cr.getImportedSymbolName()),
        () -> assertEquals("deep/nested/Dummy6", dummy6_cr.getUrl()),
        () -> assertEquals("deep/nested", dummy6_cr.getUrlPrefix()),
        () -> assertEquals("./Dummy6Component", dummy6_cr.getImportPathNoExt())
    );
  }

  @Test
  public void testGetDefaultRoute() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.addExtraInfo(routes);
    List<ComponentRoute> conf_overridden_comps = configGenerator.getPackageRoutes(conf_overridden);
    Optional<ComponentRoute> conf_overridden_default = configGenerator.getDefaultRoute(conf_overridden_comps);

    assertTrue(conf_overridden_default.isEmpty());

    List<ComponentRoute> conf_nested_deep_deep_deep_comps = configGenerator.getPackageRoutes(conf_nested_deep_deep_deep);
    Optional<ComponentRoute> conf_nested_deep_deep_deep_default = configGenerator.getDefaultRoute(conf_nested_deep_deep_deep_comps);

    assertTrue(conf_nested_deep_deep_deep_default.isPresent());

    List<ComponentRoute> other_deep_nested_comps = configGenerator.getPackageRoutes(other_deep_nested);
    Optional<ComponentRoute> other_deep_nested_default = configGenerator.getDefaultRoute(other_deep_nested_comps);

    assertTrue(other_deep_nested_default.isPresent());
  }

  @Test
  public void testFindTopMostRoutes() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute overridden = ast2PackageRoute(dummy7);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        overridden,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.addExtraInfo(routes);
    List<PackageRoute> topRoutes = configGenerator.findTopMostRoutes(routes);

    assertEquals(3, topRoutes.size());
    PackageRoute pr1 = topRoutes.stream().filter(pr -> pr.getURLSegment().equals("conf")).findFirst().get();
    PackageRoute pr2 = topRoutes.stream().filter(pr -> pr.getURLSegment().equals("other")).findFirst().get();
    PackageRoute pr3 = topRoutes.stream().filter(pr -> pr.getURLSegment().equals("overridden")).findFirst().get();

    assertEquals(conf, pr1);
    assertEquals(other_deep_nested, pr2);
    assertEquals(overridden, pr3);
  }

  @Test
  public void testGetAppPath2RouteMap() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute overridden = ast2PackageRoute(dummy7);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        overridden,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.addExtraInfo(routes);
    List<PackageRoute> topRoutes = configGenerator.findTopMostRoutes(routes);
    Map<String, PackageRoute> path2RouteMap = configGenerator.getAppPath2RouteMap(topRoutes);

    assertEquals(3, path2RouteMap.size());

    String p1 = "./conf/ConfGemRouting";
    String p2 = "./other/deep/nested/NestedGemRouting";
    String p3 = "overridden/OverriddenGemRouting";

    assertTrue(path2RouteMap.containsKey(p1));
    assertTrue(path2RouteMap.containsKey(p2));
    assertTrue(path2RouteMap.containsKey(p3));

    PackageRoute pr1 = path2RouteMap.get(p1);
    PackageRoute pr2 = path2RouteMap.get(p2);
    PackageRoute pr3 = path2RouteMap.get(p3);

    assertEquals(conf, pr1);
    assertEquals(other_deep_nested, pr2);
    assertEquals(overridden, pr3);
  }

  @Test
  public void testGenerate() {
    prepareModels();
    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());

    PackageRoute conf_route = ast2PackageRoute(dummy);
    PackageRoute conf_routelonger = ast2PackageRoute(dummy2);
    PackageRoute conf_nested_route = ast2PackageRoute(dummy3);
    PackageRoute conf_nested = ast2PackageRoute(dummy4);
    PackageRoute conf_nested_deep_deep_deep = ast2PackageRoute(dummy5);
    PackageRoute other_deep_nested = ast2PackageRoute(dummy6);
    PackageRoute overridden = ast2PackageRoute(dummy7);
    PackageRoute conf = ast2PackageRoute(dashboard);
    PackageRoute conf_overridden = ast2PackageRoute(update);

    List<PackageRoute> routes = List.of(
        conf_route,
        conf_routelonger,
        conf_nested_route,
        conf_nested,
        conf_nested_deep_deep_deep,
        other_deep_nested,
        overridden,
        conf,
        conf_overridden
    );

    GUI2ConfigGenerator configGenerator = createGenerator();
    configGenerator.generateConfig(routes);

    assertEquals(0, Log.getFindingsCount());
  }

  protected GUI2ConfigGenerator createGenerator() {
    GeneratorSetup genSetup = new GeneratorSetup();
    File outDir = new File(BUILD_FOLDER + "/generated-test-sources/gui");
    MCPath hwcPath = new MCPath();
    hwcPath.addEntry(Paths.get("src/test/resources/gui"));

    genSetup.setOutputDirectory(outDir);
    genSetup.setHandcodedPath(hwcPath);
    GlobalExtensionManagement glex = new GlobalExtensionManagement();
    genSetup.setGlex(glex);
    genSetup.getGlex().setGlobalValue("genHelper", GUIGeneratorUtil.getInstance());

    return new GUI2ConfigGenerator(genSetup, Optional.empty());
  }

  protected void prepareModels() {
    dummy = processGUIModel("gui/conf/route/Dummy.gui", "src/test/resources/gui");
    dummy2 = processGUIModel("gui/conf/routelonger/Dummy2.gui", "src/test/resources/gui");
    dummy3 = processGUIModel("gui/conf/nested/route/Dummy3.gui", "src/test/resources/gui");
    dummy4 = processGUIModel("gui/conf/nested/Dummy4.gui", "src/test/resources/gui");
    dummy5 = processGUIModel("gui/conf/nested/deep/deep/deep/Dummy5.gui", "src/test/resources/gui");
    dummy6 = processGUIModel("gui/other/deep/nested/Dummy6.gui", "src/test/resources/gui");
    dummy7 = processGUIModel("gui/overridden/Dummy7.gui", "src/test/resources/gui");
    dashboard = processGUIModel("gui/conf/Dashboard.gui", "src/test/resources/gui");
    update = processGUIModel("gui/conf/overridden/Update.gui", "src/test/resources/gui");
  }

  protected PackageRoute ast2PackageRoute(ASTGUICompilationUnit ast) {
    PackageRoute pr = new PackageRoute(
        ast.getMCPackageDeclaration().getMCQualifiedName().getPartsList()
    );
    pr.add(ast);
    return pr;
  }

}
