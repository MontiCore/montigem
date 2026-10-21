/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genjava.decorator;

import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIComponentStat;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.genjava.GUI2CDTrafo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class GUI2JavaClassDecoratorTest extends DomainGUITest {

  GlobalExtensionManagement glex;

  GeneratorSetup genSetup;

  @BeforeEach
  public void genSetup() {
    genSetup = new GeneratorSetup();
    // set output directory
    String genFolder = BUILD_FOLDER + "/guigentests";
    genSetup.setOutputDirectory(new File(genFolder));
    // set hwc directories
    // genSetup.setHandcodedPath(hwcPath);
    // set template path
    glex = createGlex();
    genSetup.setGlex(glex);
    genSetup.getGlex().setGlobalValue("cdPrinter", new CdUtilsPrinter());
    CD4C.init(genSetup);
  }

  @Test
  public void testAmounts() {
    ASTGUICompilationUnit gui;
    ASTCDCompilationUnit cd;

    gui = processGUIModel(
        "gui/genjava/decorator/GUI2JavaClassDecorator.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"), List.of("genjava.GUI2JavaClassDecorator"));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    assertEquals(1, cd.getCDDefinition().getCDClassesList().size());

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);
    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);

    assertAll(
        () -> assertEquals(0, Log.getErrorCount()),
        () -> assertEquals(27, clazz.getCDAttributeList().size()),
        () -> assertEquals(136, clazz.getCDMethodList().size()),
        () -> assertEquals(1, clazz.getCDConstructorList().size()),
        () -> assertEquals(21, imports.size())
    );
  }

  @Test
  public void testEmptyClass() {
    ASTGUICompilationUnit gui;
    ASTCDCompilationUnit cd;

    gui = processGUIModel(
        "gui/genjava/decorator/GUI2JavaClassDecorator.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"), List.of("genjava.GUI2JavaClassDecorator"));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);
    assertEquals("GUI2JavaClassDecorator", clazz.getName());

    List<ASTCDMethod> methods = clazz.getCDMethodList();
    ASTCDMethod init = getMethod(methods, "init");

    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);
    ASTMCImportStatement directiveImport = getImport(imports, "umlp.jsweet.extension.annotation.Directive");

    assertAll(
        () -> assertNotNull(init),
        () -> assertEquals("void", init.getMCReturnType().printType()),
        () -> assertEquals(0, init.getCDParameterList().size()),
        () -> assertNotNull(directiveImport)
    );
  }

  @Test
  public void testAutomaticTestTextInputValidityGetter() {
    ASTGUICompilationUnit gui = processGUIModel(
        "gui/genjava/decorator/TestTextInputAutoValidity.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    ASTCDCompilationUnit cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"), List.of("genjava.decorator.TestTextInputAutoValidity"));
    ASTGUIComponent auto = GUIComponentStat.getInstance()
        .getCompById(GUIComponentStat.getInstance().getComponents(gui.getGUIComponentDeclaration()), "auto");
    assertTrue(GUIGeneratorUtil.getInstance().shouldGenerateAutomaticDomainValidity(auto));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);
    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);

    assertAll(
        () -> assertNotNull(getMethod(clazz.getCDMethodList(), "getAutoValidity")),
        () -> assertNotNull(getImport(imports, "mc.fenix.input.geminputbasetypes.ValidationError")),
        () -> assertNotNull(getImport(imports, "java.util.Optional"))
    );
  }

  @Test
  public void testExplicitTestTextInputValidityOverridesAutomaticGetter() {
    ASTGUICompilationUnit gui = processGUIModel(
        "gui/genjava/decorator/TestTextInputExplicitValidity.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    ASTCDCompilationUnit cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"), List.of("genjava.decorator.TestTextInputExplicitValidity"));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);
    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);

    assertAll(
        () -> assertNotNull(getMethod(clazz.getCDMethodList(), "getExplicitValidity")),
        () -> assertNull(getImport(imports, "mc.fenix.input.geminputbasetypes.ValidationError")),
        () -> assertNull(getImport(imports, "java.util.Optional"))
    );
  }

  @Test
  public void testVisitGUIParam() {
    ASTGUICompilationUnit gui;
    ASTCDCompilationUnit cd;

    gui = processGUIModel(
        "gui/genjava/decorator/GUI2JavaClassDecorator.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"),
        List.of("genjava.decorator.GUI2JavaClassDecorator")
    );

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);

    List<Executable> assertions = new ArrayList<>();
    List<ASTCDAttribute> attrs = clazz.getCDAttributeList();

    ASTCDAttribute _person = getAttr(attrs, "_person");
    ASTCDAttribute _$per$son = getAttr(attrs, "_$per$son");
    ASTCDAttribute _status = getAttr(attrs, "_status");
    ASTCDAttribute _la = getAttr(attrs, "_la");
    ASTCDAttribute _lla = getAttr(attrs, "_lla");
    ASTCDAttribute _s = getAttr(attrs, "_s");
    ASTCDAttribute _b = getAttr(attrs, "_b");
    ASTCDAttribute _i = getAttr(attrs, "_i");
    ASTCDAttribute _e = getAttr(attrs, "_e");

    assertions.add(() -> assertNotNull(_person));
    assertions.add(() -> assertNotNull(_$per$son));
    assertions.add(() -> assertNotNull(_status));
    assertions.add(() -> assertNotNull(_lla));
    assertions.add(() -> assertNotNull(_la));
    assertions.add(() -> assertNotNull(_s));
    assertions.add(() -> assertNotNull(_b));
    assertions.add(() -> assertNotNull(_i));
    assertions.add(() -> assertNotNull(_e));

    assertions.add(() -> assertEquals("Person", _person.getMCType().printType()));
    assertions.add(() -> assertEquals("Person", _$per$son.getMCType().printType()));
    assertions.add(() -> assertEquals("MaritalStatus", _status.getMCType().printType()));
    assertions.add(() -> assertEquals("List<packaged.domain.Address>", _la.getMCType().printType()));
    assertions.add(() -> assertEquals("List<List<packaged.domain.Address>>", _lla.getMCType().printType()));
    assertions.add(() -> assertTrue(
        "java.lang.String".equals(_s.getMCType().printType())
        || "String".equals(_s.getMCType().printType())
    ));
    assertions.add(() -> assertTrue(
        "java.lang.Boolean".equals(_b.getMCType().printType())
            || "Boolean".equals(_b.getMCType().printType())
    ));
    assertions.add(() -> assertTrue(
        "java.lang.Integer".equals(_i.getMCType().printType())
        || "Integer".equals(_i.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("Consumer<java.lang.Void>", _e.getMCType().printType()));

    List<ASTCDMethod> methods = clazz.getCDMethodList();

    ASTCDMethod getPerson = getMethod(methods, "getPerson");
    ASTCDMethod setPerson = getMethod(methods, "setPerson");
    ASTCDMethod initPerson = getMethod(methods, "initPerson");
    ASTCDMethod get$per$son = getMethod(methods, "get$per$son");
    ASTCDMethod set$per$son = getMethod(methods, "set$per$son");
    ASTCDMethod init$per$son = getMethod(methods, "init$per$son");
    ASTCDMethod getStatus = getMethod(methods, "getStatus");
    ASTCDMethod setStatus = getMethod(methods, "setStatus");
    ASTCDMethod initStatus = getMethod(methods, "initStatus");
    ASTCDMethod getLa = getMethod(methods, "getLa");
    ASTCDMethod setLa = getMethod(methods, "setLa");
    ASTCDMethod initLa = getMethod(methods, "initLa");
    ASTCDMethod getLla = getMethod(methods, "getLla");
    ASTCDMethod setLla = getMethod(methods, "setLla");
    ASTCDMethod initLla = getMethod(methods, "initLla");
    ASTCDMethod getS = getMethod(methods, "getS");
    ASTCDMethod setS = getMethod(methods, "setS");
    ASTCDMethod initS = getMethod(methods, "initS");
    ASTCDMethod isB = getMethod(methods, "isB");
    ASTCDMethod setB = getMethod(methods, "setB");
    ASTCDMethod initB = getMethod(methods, "initB");
    ASTCDMethod getI = getMethod(methods, "getI");
    ASTCDMethod setI = getMethod(methods, "setI");
    ASTCDMethod initI = getMethod(methods, "initI");
    ASTCDMethod getE = getMethod(methods, "getE");
    ASTCDMethod setE = getMethod(methods, "setE");
    ASTCDMethod initE = getMethod(methods, "initE");

    assertions.add(() -> assertNotNull(getPerson));
    assertions.add(() -> assertNotNull(setPerson));
    assertions.add(() -> assertNotNull(initPerson));
    assertions.add(() -> assertNotNull(get$per$son));
    assertions.add(() -> assertNotNull(set$per$son));
    assertions.add(() -> assertNotNull(init$per$son));
    assertions.add(() -> assertNotNull(getStatus));
    assertions.add(() -> assertNotNull(setStatus));
    assertions.add(() -> assertNotNull(initStatus));
    assertions.add(() -> assertNotNull(getLa));
    assertions.add(() -> assertNotNull(setLa));
    assertions.add(() -> assertNotNull(initLa));
    assertions.add(() -> assertNotNull(getLla));
    assertions.add(() -> assertNotNull(setLla));
    assertions.add(() -> assertNotNull(initLla));
    assertions.add(() -> assertNotNull(getS));
    assertions.add(() -> assertNotNull(setS));
    assertions.add(() -> assertNotNull(initS));
    assertions.add(() -> assertNotNull(isB));
    assertions.add(() -> assertNotNull(setB));
    assertions.add(() -> assertNotNull(initB));
    assertions.add(() -> assertNotNull(getI));
    assertions.add(() -> assertNotNull(setI));
    assertions.add(() -> assertNotNull(initI));
    assertions.add(() -> assertNotNull(getE));
    assertions.add(() -> assertNotNull(setE));
    assertions.add(() -> assertNotNull(initE));

    Collection<ASTMCImportStatement> imports = CD4C.getInstance().getImportList(clazz);

    ASTMCImportStatement personImport = getImport(imports, "packaged.domain.Person");
    ASTMCImportStatement maritalStatusImport = getImport(imports, "packaged.domain.MaritalStatus");
    ASTMCImportStatement listImport = getImport(imports, "java.util.List");
    ASTMCImportStatement addressImport = getImport(imports, "packaged.domain.Address");
    ASTMCImportStatement consumerImport = getImport(imports, "java.util.function.Consumer");
    // optional imports, byproduct of type handling
    ASTMCImportStatement stringImport = getImport(imports, "java.lang.String");
    ASTMCImportStatement voidImport = getImport(imports, "java.lang.Void");

    assertions.add(() -> assertNotNull(personImport));
    assertions.add(() -> assertNotNull(maritalStatusImport));
    assertions.add(() -> assertNotNull(listImport));
    assertions.add(() -> assertNotNull(addressImport));
    assertions.add(() -> assertNotNull(consumerImport));
    assertions.add(() -> assertNotNull(stringImport));
    assertions.add(() -> assertNotNull(voidImport));

    assertAll(assertions);
  }

  @Test
  public void testVisitGUINamedArg() {
    ASTGUICompilationUnit gui;
    ASTCDCompilationUnit cd;

    gui = processGUIModel(
        "gui/genjava/decorator/GUI2JavaClassDecorator.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"),
        List.of("genjava.decorator.GUI2JavaClassDecorator")
    );

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    ASTCDClass clazz = cd.getCDDefinition().getCDClassesList().get(0);

    List<Executable> assertions = new ArrayList<>();

    List<ASTCDMethod> methods = clazz.getCDMethodList();

    ASTCDMethod getAP = getMethod(methods, "getAP");
    ASTCDMethod getAM = getMethod(methods, "getAM");
    ASTCDMethod getAS = getMethod(methods, "getAS");
    ASTCDMethod getAB = getMethod(methods, "getAB");
    ASTCDMethod getAA = getMethod(methods, "getAA");
    ASTCDMethod getCP = getMethod(methods, "getCP");
    ASTCDMethod getCS = getMethod(methods, "getCS");
    ASTCDMethod calcStr = getMethod(methods, "calcStr");
    ASTCDMethod getCA = getMethod(methods, "getCA");
    ASTCDMethod getCA_1 = getMethod(methods, "getCA_1");
    ASTCDMethod initCA_1 = getMethod(methods, "initCA_1");
    ASTCDMethod getDI = getMethod(methods, "getDI");
    ASTCDMethod setDI = getMethod(methods, "setDI");
    ASTCDMethod getFI = getMethod(methods, "getFI");
    ASTCDMethod setFI = getMethod(methods, "setFI");
    ASTCDMethod hE = getMethod(methods, "hE");
    ASTCDMethod jE = getMethod(methods, "jE");
    ASTCDMethod action = getMethod(methods, "action");
    ASTCDMethod getKA = getMethod(methods, "getKA");
    ASTCDMethod getKS = getMethod(methods, "getKS");
    ASTCDMethod getKS_1 = getMethod(methods, "getKS_3");
    ASTCDMethod initKS_1 = getMethod(methods, "initKS_3");
    ASTCDMethod getKM = getMethod(methods, "getKM");
    ASTCDMethod getKM_2 = getMethod(methods, "getKM_4");
    ASTCDMethod initKM_2 = getMethod(methods, "getKM_4");
    ASTCDMethod getLIterable = getMethod(methods, "getLIterable");
    ASTCDMethod getMCondition = getMethod(methods, "getMCondition");
    ASTCDMethod getNP = getMethod(methods, "getNP");
    ASTCDMethod getPIterable = getMethod(methods, "getPIterable");

    assertions.add(() -> assertNotNull(getAP));
    assertions.add(() -> assertNotNull(getAM));
    assertions.add(() -> assertNotNull(getAS));
    assertions.add(() -> assertNotNull(getAB));
    assertions.add(() -> assertNotNull(getAA));
    assertions.add(() -> assertNotNull(getCP));
    assertions.add(() -> assertNotNull(getCS));
    assertions.add(() -> assertNotNull(calcStr));
    assertions.add(() -> assertNotNull(getCA));
    assertions.add(() -> assertNotNull(getCA_1));
    assertions.add(() -> assertNotNull(initCA_1));
    assertions.add(() -> assertNotNull(getDI));
    assertions.add(() -> assertNotNull(setDI));
    assertions.add(() -> assertNotNull(getFI));
    assertions.add(() -> assertNotNull(setFI));
    assertions.add(() -> assertNotNull(hE));
    assertions.add(() -> assertNotNull(jE));
    assertions.add(() -> assertNotNull(action));
    assertions.add(() -> assertNotNull(getKA));
    assertions.add(() -> assertNotNull(getKS));
    assertions.add(() -> assertNotNull(getKS_1));
    assertions.add(() -> assertNotNull(initKS_1));
    assertions.add(() -> assertNotNull(getKM));
    assertions.add(() -> assertNotNull(getKM_2));
    assertions.add(() -> assertNotNull(initKM_2));
    assertions.add(() -> assertNotNull(getLIterable));
    assertions.add(() -> assertNotNull(getMCondition));
    assertions.add(() -> assertNotNull(getNP));
    assertions.add(() -> assertNotNull(getPIterable));

    assertAll(assertions);
  }

  @Test
  public void testAddInitMethods() {
    ASTGUICompilationUnit gui;
    ASTCDCompilationUnit cd;

    gui = processGUIModel(
        "gui/genjava/decorator/GUI2JavaClassDecorator.gui",
        "src/test/resources/gui",
        "packaged/Domain.umlp"
    );
    ASTGUIComponentDeclaration decl = gui.getGUIComponentDeclaration();
    cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Domain"), List.of("genjava.GUI2JavaClassDecorator"));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    // check what init methods will be added and their dependencies
    Optional<InitDependency> root = InitDependencyTree.getInstance()
        .getDependency(decl);

    assertTrue(root.isPresent());

    GUIComponentStat stat = GUIComponentStat.getInstance();

    List<ASTGUIComponent> components;
    List<ASTGUIGuard> guards;
    List<ASTGUIIterate> iterates;

    components = stat.getComponents(decl);
    guards = stat.getGuards(decl);
    iterates = stat.getIterates(decl);

    ASTGUIIterate lNode = stat.getIterById(iterates, "l");
    ASTGUIIterate oNode = stat.getIterById(iterates, "o");
    ASTGUIIterate pNode = stat.getIterById(iterates, "p");
    ASTGUIComponent qNode = stat.getCompById(components, "q");


    List<Executable> assertions = new ArrayList<>();

    // declaration block does not have explicit init
    assertions.add(() -> assertFalse(root.get().getInitMethod().isPresent()));
    // most GUIParam have init
    InitDependency person = root.get().getDependentList().get(0);
    assertions.add(() -> assertTrue(person.getInitMethod().isPresent()));
    InitDependency $per$son = person.getDependentList().get(0);
    assertions.add(() -> assertTrue($per$son.getInitMethod().isPresent()));
    InitDependency statusDefault = $per$son.getDependentList().get(0);
    assertions.add(() -> assertFalse(statusDefault.getInitMethod().isPresent()));
    InitDependency status = statusDefault.getDependentList().get(0);
    assertions.add(() -> assertTrue(status.getInitMethod().isPresent()));
    // param that has default value depends on the value
    InitDependency la = status.getDependentList().get(0);
    assertions.add(() -> assertTrue(la.getInitMethod().isPresent()));
    InitDependency lla = la.getDependentList().get(0);
    assertions.add(() -> assertTrue(lla.getInitMethod().isPresent()));
    InitDependency s = lla.getDependentList().get(0);
    assertions.add(() -> assertTrue(s.getInitMethod().isPresent()));
    InitDependency b = s.getDependentList().get(0);
    assertions.add(() -> assertTrue(b.getInitMethod().isPresent()));
    InitDependency i = b.getDependentList().get(0);
    assertions.add(() -> assertTrue(i.getInitMethod().isPresent()));
    InitDependency v = i.getDependentList().get(0);
    // GUIViewElement has no init
    assertions.add(() -> assertFalse(v.getInitMethod().isPresent()));
    InitDependency e = v.getDependentList().get(0);
    assertions.add(() -> assertTrue(e.getInitMethod().isPresent()));

    InitDependency declBlock = e.getDependentList().get(0);
    assertions.add(() -> assertFalse(declBlock.getInitMethod().isPresent()));

    InitDependency aA = declBlock.getDependentList().get(0);
    InitDependency cS = aA.getDependentList().get(0);
    InitDependency cA = cS.getDependentList().get(0);
    InitDependency fI0 = cA.getDependentList().get(0);
    InitDependency gLgve = fI0.getDependentList().get(0).getDependentList().get(0);
    InitDependency jE = gLgve.getDependentList().get(0);
    InitDependency kS = jE.getDependentList().get(0);
    InitDependency kM0 = kS.getDependentList().get(0);
    InitDependency kM1 = kM0.getDependentList().get(0);
    InitDependency kM = kM1.getDependentList().get(0);

    assertions.add(() -> assertTrue(aA.getInitMethod().isPresent()));
    assertions.add(() -> assertFalse(cS.getInitMethod().isPresent()));
    assertions.add(() -> assertTrue(cA.getInitMethod().isPresent()));
    assertions.add(() -> assertFalse(gLgve.getInitMethod().isPresent()));
    assertions.add(() -> assertFalse(jE.getInitMethod().isPresent()));
    assertions.add(() -> assertTrue(kS.getInitMethod().isPresent()));
    assertions.add(() -> assertFalse(kM0.getInitMethod().isPresent()));
    assertions.add(() -> assertFalse(kM1.getInitMethod().isPresent()));
    assertions.add(() -> assertTrue(kM.getInitMethod().isPresent()));

    InitDependency l = kM.getDependentList().get(0);
    InitDependency lCall = l.getDependentList().get(0);
    InitDependency lBlock = lCall.getDependentList().get(0);
    InitDependency mCondition = lBlock.getDependentList().get(0);
    InitDependency mBlock = mCondition.getDependentList().get(0);

    InitDependency o = l.getDependentList().get(1);

    assertions.add(() -> assertTrue(l.getInitMethod().isPresent()));
    assertions.add(() -> assertTrue(o.getInitMethod().isPresent()));

    InitDependency oIterable = o.getDependentList().get(0);
    assertions.add(() -> assertTrue(oIterable.getInitMethod().isPresent()));
    InitDependency oBlock = oIterable.getDependentList().get(0);

    InitDependency p = oBlock.getDependentList().get(0);
    InitDependency pIterable = p.getDependentList().get(0);
    assertions.add(() -> assertTrue(pIterable.getInitMethod().isPresent()));
    InitDependency pBlock = pIterable.getDependentList().get(0);
    assertions.add(() -> assertFalse(pBlock.getInitMethod().isPresent()));

    InitDependency qConstructor = pBlock.getDependentList().get(0);
    assertions.add(() -> assertTrue(qConstructor.getInitMethod().isPresent()));
    InitDependency qInnerList = qConstructor.getDependentList().get(0);
    assertions.add(() -> assertTrue(qInnerList.getInitMethod().isPresent()));
    InitDependency qCall = qInnerList.getDependentList().get(0);
    assertions.add(() -> assertTrue(qCall.getInitMethod().isPresent()));
    InitDependency qList = qCall.getDependentList().get(0);
    assertions.add(() -> assertTrue(qList.getInitMethod().isPresent()));

    InitDependency rList = qList.getDependentList().get(0);
    assertions.add(() -> assertTrue(rList.getInitMethod().isPresent()));

    assertAll(assertions);
  }

  @Test
  public void testStringToTypeConversionOfTextInputs() {
    ASTGUICompilationUnit gui = processGUIModel(
        "gui/genjava/decorator/TextInputConversion.gui",
        "src/test/resources/gui",
        "packaged/Inputs.umlp"
    );
    ASTCDCompilationUnit cd = GUI2CDTrafo.createCDCompilationUnit(gui);

    GUIGeneratorUtil.getInstance().init(
        List.of("packaged.Inputs"), List.of("genjava.decorator.TextInputConversion"));

    GUI2JavaClassDecorator decorator = new GUI2JavaClassDecorator(glex);
    decorator.transform(gui, cd);

    List<ASTCDMethod> methods = cd.getCDDefinition().getCDClassesList().get(0).getCDMethodList();
    String in = "conv_val_$";
    String emptyCheck = in + " == null || " + in + ".isEmpty() ? Optional.empty() : ";

    assertAll(
        () -> assertEquals(0, Log.getErrorCount()),
        // a cleared input of an optional attribute empties it
        () -> assertEquals("return " + emptyCheck + "Optional.of(LocalDate.parse(" + in + "));",
            printBody(getMethod(methods, "convertOptDayEntry"))),
        () -> assertEquals("return " + emptyCheck + "Optional.of(LocalDateTime.parse(" + in + "));",
            printBody(getMethod(methods, "convertOptTimeEntry"))),
        () -> assertEquals("return " + emptyCheck + "Optional.of(Integer.valueOf(" + in + "));",
            printBody(getMethod(methods, "convertOptNumEntry"))),
        () -> assertEquals("return " + emptyCheck + "Optional.of(Letter.valueOf(" + in + "));",
            printBody(getMethod(methods, "convertOptLetterEntry"))),
        // an empty string is a value of an optional string
        () -> assertEquals("return Optional.of(" + in + ");",
            printBody(getMethod(methods, "convertOptTextEntry"))),
        // mandatory attributes are converted unchanged
        () -> assertEquals("return LocalDate.parse(" + in + ");",
            printBody(getMethod(methods, "convertDayEntry"))),
        () -> assertEquals("return LocalDateTime.parse(" + in + ");",
            printBody(getMethod(methods, "convertTimeEntry"))),
        () -> assertEquals("return Integer.valueOf(" + in + ");",
            printBody(getMethod(methods, "convertNumEntry"))),
        () -> assertEquals("return Letter.valueOf(" + in + ");",
            printBody(getMethod(methods, "convertLetterEntry")))
    );
  }

  /**
   * @return the generated body of the method, without comments and with whitespace sequences
   * replaced by single spaces
   */
  protected String printBody(ASTCDMethod method) {
    assertNotNull(method);
    // CD4C attaches the body to the empty body template of the method
    String body = new GeneratorEngine(genSetup)
        .generate(CD4C.getInstance().getEmptyBodyTemplate(), method).toString();
    return body.replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", " ").trim();
  }

  protected ASTCDAttribute getAttr(Collection<ASTCDAttribute> attrs, String name) {
    for (ASTCDAttribute attr : attrs) {
      if (attr.getName().equals(name)) {
        return attr;
      }
    }
    return null;
  }

  protected ASTCDMethod getMethod(Collection<ASTCDMethod> methods, String name) {
    for (ASTCDMethod method : methods) {
      if (method.getName().equals(name)) {
        return method;
      }
    }
    return null;
  }

  protected int methodCount(Collection<ASTCDMethod> methods, String name) {
    int count = 0;
    for (ASTCDMethod method : methods) {
      if (method.getName().equals(name)) {
        count++;
      }
    }
    return count;
  }

  protected ASTMCImportStatement getImport(Collection<ASTMCImportStatement> imports, String name) {
    for (ASTMCImportStatement imp : imports) {
      if (imp.getQName().equals(name)) {
        return imp;
      }
    }
    return null;
  }

}
