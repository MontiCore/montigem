/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genjava.decorator;

import de.monticore.ast.ASTNode;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._ast.ASTSetValueItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIComponentStat;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.TestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("unused")
public class StaticObjectsCollectorTest extends DomainGUITest {

  @Test
  public void testGetNode2Attr() {
    ASTGUIComponentDeclaration decl;
    GUIComponentStat stat = GUIComponentStat.getInstance();

    List<ASTGUIComponent> components;
    List<ASTGUIGuard> guards;
    List<ASTGUIIterate> iterates;

    decl = processGUIModel(
        "gui/genjava/decorator/StaticObjectsCollector.gui",
        "src/test/resources/gui"
    ).getGUIComponentDeclaration();

    GUIGeneratorUtil.getInstance().init(List.of("Domain"), Collections.emptyList());
    StaticObjectsCollector.getInstance().init(decl);

    components = stat.getComponents(decl);
    guards = stat.getGuards(decl);
    iterates = stat.getIterates(decl);

    ASTGUIComponent a = stat.getCompById(components, "a");
    ASTGUIComponent b = stat.getCompById(components, "b");
    ASTGUIComponent c = stat.getCompById(components, "c");
    ASTGUIComponent d = stat.getCompById(components, "d");
    ASTGUIGuard e = stat.getGuardById(guards, "e");
    ASTGUIIterate f = stat.getIterById(iterates, "f");
    ASTGUIComponent g = stat.getCompById(components, "g");
    ASTGUIComponent h = stat.getCompById(components, "h");
    ASTGUIComponent i = stat.getCompById(components, "i");
    ASTGUIComponent j = stat.getCompById(components, "j");

    List<Executable> assertions = new ArrayList<>();

    Map<ASTNode, Attribute> node2Attr = StaticObjectsCollector.getInstance().getNode2Attr();

    assertions.add(() -> assertEquals(24, node2Attr.size()));

    Attribute bP0Wrapper = node2Attr.get(
        stat.getArgByName(b, "p").getExpression());
    ASTCDAttribute bP0 = bP0Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Person", bP0.getMCType().printType()));
    assertions.add(() -> assertEquals("bP_0", bP0.getName()));
    assertions.add(() -> assertEquals(0, bP0Wrapper.getParams().size()));

    Attribute cA1Wrapper = node2Attr.get(
        stat.getArgByName(c, "a").getExpression());
    ASTCDAttribute cA1 = cA1Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<Address>", cA1.getMCType().printType()));
    assertions.add(() -> assertEquals("cA_1", cA1.getName()));
    assertions.add(() -> assertEquals(0, cA1Wrapper.getParams().size()));

    ASTSetValueItem a0 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(c, "a").getExpression())
        .getSetCollectionItem(0));
    Attribute cA2Wrapper = node2Attr.get(a0.getExpression());
    ASTCDAttribute cA2 = cA2Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Address", cA2.getMCType().printType()));
    assertions.add(() -> assertEquals("cA_2", cA2.getName()));
    assertions.add(() -> assertEquals(0, cA2Wrapper.getParams().size()));

    ASTSetValueItem a1 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(c, "a").getExpression())
        .getSetCollectionItem(1));
    Attribute cA3Wrapper = node2Attr.get(a1.getExpression());
    ASTCDAttribute cA3 = cA3Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Address", cA3.getMCType().printType()));
    assertions.add(() -> assertEquals("cA_3", cA3.getName()));
    assertions.add(() -> assertEquals(0, cA3Wrapper.getParams().size()));

    ASTSetEnumeration lp = (ASTSetEnumeration) stat.getArgByName(d, "lp").getExpression();
    Attribute dLp4Wrapper = node2Attr.get(lp);
    ASTCDAttribute dLp4 = dLp4Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<List<Person>>", dLp4.getMCType().printType()));
    assertions.add(() -> assertEquals("dLp_4", dLp4.getName()));
    assertions.add(() -> assertEquals(0, dLp4Wrapper.getParams().size()));

    ASTSetValueItem d0 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(d, "lp").getExpression())
        .getSetCollectionItem(0));
    ASTSetValueItem d1 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(d, "lp").getExpression())
        .getSetCollectionItem(1));
    Attribute dLp5Wrapper = node2Attr.get(d0.getExpression());
    ASTCDAttribute dLp5 = dLp5Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<Person>", dLp5.getMCType().printType()));
    assertions.add(() -> assertEquals("dLp_5", dLp5.getName()));
    assertions.add(() -> assertEquals(0, dLp5Wrapper.getParams().size()));

    ASTSetValueItem d0_1 = ((ASTSetValueItem)
        ((ASTSetEnumeration)d0.getExpression()).getSetCollectionItem(1));
    Attribute dLp6Wrapper = node2Attr.get(d0_1.getExpression());
    ASTCDAttribute dLp6 = dLp6Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Person", dLp6.getMCType().printType()));
    assertions.add(() -> assertEquals("dLp_6", dLp6.getName()));
    assertions.add(() -> assertEquals(0, dLp6Wrapper.getParams().size()));

    Attribute dLp7Wrapper = node2Attr.get(d1.getExpression());
    ASTCDAttribute dLp7 = dLp7Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<Person>", dLp7.getMCType().printType()));
    assertions.add(() -> assertEquals("dLp_7", dLp7.getName()));
    assertions.add(() -> assertEquals(0, dLp7Wrapper.getParams().size()));

    ASTSetValueItem d1_0 = ((ASTSetValueItem)
        ((ASTSetEnumeration)d1.getExpression()).getSetCollectionItem(0));

    Attribute dLp8Wrapper = node2Attr.get(
        ((ASTFieldAccessExpression) ((ASTCallExpression) d1_0.getExpression())
            .getExpression())
            .getExpression());
    ASTCDAttribute dLp8 = dLp8Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<Person>", dLp8.getMCType().printType()));
    assertions.add(() -> assertEquals("dLp_8", dLp8.getName()));
    assertions.add(() -> assertEquals(0, dLp8Wrapper.getParams().size()));

    Attribute eCondition9Wrapper = node2Attr.get(
        ((ASTFieldAccessExpression) e.getCondition()).getExpression());
    ASTCDAttribute eCondition9 = eCondition9Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Person", eCondition9.getMCType().printType()));
    assertions.add(() -> assertEquals("eCondition_9", eCondition9.getName()));
    assertions.add(() -> assertEquals(0, eCondition9Wrapper.getParams().size()));

    Attribute fIterable10Wrapper = node2Attr.get(
        ((ASTGUIShortIterateControl) f.getIterateControl()).getIterable());
    ASTCDAttribute fIterable10 = fIterable10Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("List<Person>", fIterable10.getMCType().printType()));
    assertions.add(() -> assertEquals("fIterable_10", fIterable10.getName()));
    assertions.add(() -> assertEquals(0, fIterable10Wrapper.getParams().size()));

    Attribute fIterable11Wrapper = node2Attr.get(
        ((ASTFieldAccessExpression) ((ASTGUIShortIterateControl) f.getIterateControl())
            .getIterable()).getExpression());
    ASTCDAttribute fIterable11 = fIterable11Wrapper.getCdAttr();
    assertions.add(() -> assertEquals("Person", fIterable11.getMCType().printType()));
    assertions.add(() -> assertEquals("fIterable_11", fIterable11.getName()));
    assertions.add(() -> assertEquals(0, fIterable11Wrapper.getParams().size()));

    Attribute gA13Wrapper = node2Attr.get(
        stat.getArgByName(g, "a").getExpression());
    ASTCDAttribute gA13 = gA13Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, List<Address>>>"),
        TestUtils.removeSpaces(gA13.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("gA_13", gA13.getName()));
    assertions.add(() -> assertEquals(2, gA13Wrapper.getParams().size()));

    ASTSetValueItem ga0 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(g, "a").getExpression())
        .getSetCollectionItem(0));
    Attribute gA14Wrapper = node2Attr.get(ga0.getExpression());
    ASTCDAttribute gA14 = gA14Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, Address>>"),
        TestUtils.removeSpaces(gA14.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("gA_14", gA14.getName()));
    assertions.add(() -> assertEquals(2, gA14Wrapper.getParams().size()));

    ASTSetValueItem ga1 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(g, "a").getExpression())
        .getSetCollectionItem(1));
    Attribute gA15Wrapper = node2Attr.get(ga1.getExpression());
    ASTCDAttribute gA15 = gA15Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, Address>>"),
        TestUtils.removeSpaces(gA15.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("gA_15", gA15.getName()));
    assertions.add(() -> assertEquals(2, gA15Wrapper.getParams().size()));

    Attribute hA16Wrapper = node2Attr.get(
        stat.getArgByName(h, "a").getExpression());
    ASTCDAttribute hA16 = hA16Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, List<Address>>>"),
        TestUtils.removeSpaces(hA16.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("hA_16", hA16.getName()));
    assertions.add(() -> assertEquals(2, hA16Wrapper.getParams().size()));


    ASTSetValueItem ha0 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(h, "a").getExpression())
        .getSetCollectionItem(0));
    Attribute hA17Wrapper = node2Attr.get(ha0.getExpression());
    ASTCDAttribute hA17 = hA17Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, Address>>"),
        TestUtils.removeSpaces(hA17.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("hA_17", hA17.getName()));
    assertions.add(() -> assertEquals(2, hA17Wrapper.getParams().size()));

    ASTSetValueItem ha1 = ((ASTSetValueItem)
        ((ASTSetEnumeration)stat.getArgByName(h, "a").getExpression())
        .getSetCollectionItem(1));
    Attribute hA18Wrapper = node2Attr.get(ha1.getExpression());
    ASTCDAttribute hA18 = hA18Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, Address>>"),
        TestUtils.removeSpaces(hA18.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("hA_18", hA18.getName()));
    assertions.add(() -> assertEquals(2, hA18Wrapper.getParams().size()));

    Attribute iP19Wrapper = node2Attr.get(
        stat.getArgByName(i, "p").getExpression());
    ASTCDAttribute iP19 = iP19Wrapper.getCdAttr();
    assertions.add(() -> assertEquals(
        TestUtils.removeSpaces("Map<Person, Map<Person, Person>>"),
        TestUtils.removeSpaces(iP19.getMCType().printType())
    ));
    assertions.add(() -> assertEquals("iP_19", iP19.getName()));
    assertions.add(() -> assertEquals(2, iP19Wrapper.getParams().size()));

    boolean contains_jV20 = node2Attr.containsKey(
        stat.getArgByName(j, "v").getExpression());
    assertions.add(() -> assertFalse(contains_jV20));

    assertAll(assertions);


  }

}



