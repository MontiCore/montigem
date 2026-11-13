/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genhtml;

import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.guidsl._ast.ASTGUIBlockContent;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIComponentStat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class HTMLHelperTest extends DomainGUITest {

  @Test
  public void testHTMLHelper() {
    ASTGUICompilationUnit ast;

    ast = processGUIModel("gui/util/E.gui", "src/test/resources/gui");
    new HTMLHelper(ast);

    assertNoErrors();
  }

  @Test
  public void testGetNgTemplates() {
    ASTGUICompilationUnit ast;
    GUIComponentStat stat = GUIComponentStat.getInstance();

    List<ASTGUIComponent> components;
    List<ASTGUIGuard> guards;
    List<ASTGUIIterate> iterates;

    ast = processGUIModel("gui/genhtml/HTMLHelper.gui", "src/test/resources/gui");
    ASTGUIComponentDeclaration decl = ast.getGUIComponentDeclaration();

    HTMLHelper finder = new HTMLHelper(ast);

    components = stat.getComponents(decl);
    guards = stat.getGuards(decl);
    iterates = stat.getIterates(decl);

    ASTGUIComponent a = stat.getCompById(components, "a");
    ASTGUIIterate b = stat.getIterById(iterates, "b");
    ASTGUIComponent c = stat.getCompById(components, "c");
    ASTGUIGuard d = stat.getGuardById(guards, "d");
    ASTGUIIterate e = stat.getIterById(iterates, "e");
    ASTGUIComponent f = stat.getCompById(components, "f");
    ASTGUIGuard g = stat.getGuardById(guards, "g");
    ASTGUIComponent h = stat.getCompById(components, "h");
    ASTGUIComponent i = stat.getCompById(components, "i");
    ASTGUIComponent j = stat.getCompById(components, "j");
    ASTGUIComponent k = stat.getCompById(components, "k");
    ASTGUIComponent l = stat.getCompById(components, "l");
    ASTGUIComponent m = stat.getCompById(components, "m");
    ASTGUIComponent n = stat.getCompById(components, "n");
    ASTGUIComponent o = stat.getCompById(components, "o");
    ASTGUIComponent p = stat.getCompById(components, "p");
    ASTGUIGuard q = stat.getGuardById(guards, "q");
    ASTGUIComponent r = stat.getCompById(components, "r");
    ASTGUIComponent s = stat.getCompById(components, "s");
    ASTGUIIterate t = stat.getIterById(iterates, "t");
    ASTGUIIterate u = stat.getIterById(iterates, "u");
    ASTGUIComponent v = stat.getCompById(components, "v");
    ASTGUIComponent w = stat.getCompById(components, "w");
    ASTGUIComponent x = stat.getCompById(components, "x");
    ASTGUIGuard y = stat.getGuardById(guards, "y");
    ASTGUIGuard z = stat.getGuardById(guards, "z");
    ASTGUIIterate aa = stat.getIterById(iterates, "aa");
    ASTGUIIterate ab = stat.getIterById(iterates, "ab");
    ASTGUIGuard ac = stat.getGuardById(guards, "ac");
    ASTGUIIterate ad = stat.getIterById(iterates, "ad");
    ASTGUIComponent ae = stat.getCompById(components, "ae");
    ASTGUIIterate af = stat.getIterById(iterates, "af");

    ASTGUIGuard ag = stat.getGuardById(guards, "ag");
    ASTGUIComponent ah = stat.getCompById(components, "ah");
    ASTGUIGuard ai = stat.getGuardById(guards, "ai");

    Set<ASTGUIBlockContent> result;

    List<Executable> assertions = new ArrayList<>();

    // component declaration block

    Set<ASTExpression> comp1 = new HashSet<>();
    Set<ASTGUIGuard> guard1 = new HashSet<>();
    Set<ASTGUIIterate> iter1 = new HashSet<>();
    Set<ASTExpression> exp1 = new HashSet<>();

    result = finder.getNgTemplates(decl.getGUIBlock());

    for (ASTGUIBlockContent content : result) {
      if (content.isPresentExpression() && content.getExpression() instanceof ASTGUIComponent) {
        comp1.add(content.getExpression());
      }
      if (content.isPresentGUIGuard()) {
        guard1.add(content.getGUIGuard());
      }
      if (content.isPresentGUIIterate()) {
        iter1.add(content.getGUIIterate());
      }
      if (content.isPresentExpression() && !(content.getExpression() instanceof ASTGUIComponent)) {
        exp1.add(content.getExpression());
      }
    }

    assertions.add(() -> assertEquals(2, comp1.size()));
    assertions.add(() -> assertEquals(2, guard1.size()));
    assertions.add(() -> assertEquals(2, iter1.size()));
    assertions.add(() -> assertEquals(0, exp1.size()));

    assertions.add(() -> assertTrue(comp1.contains(p)));
    assertions.add(() -> assertTrue(comp1.contains(x)));

    assertions.add(() -> assertTrue(guard1.contains(q)));
    assertions.add(() -> assertTrue(guard1.contains(ai)));

    assertions.add(() -> assertTrue(iter1.contains(ab)));
    assertions.add(() -> assertTrue(iter1.contains(t)));

    // iterate block b

    assertions.add(() -> assertTrue(finder.getNgTemplates(b.getGUIBlock()).isEmpty()));

    // iterate block e

    Set<ASTExpression> comp3 = new HashSet<>();
    Set<ASTGUIGuard> guard3 = new HashSet<>();
    Set<ASTGUIIterate> iter3 = new HashSet<>();
    Set<ASTExpression> exp3 = new HashSet<>();

    result = finder.getNgTemplates(e.getGUIBlock());

    for (ASTGUIBlockContent content : result) {
      if (content.isPresentExpression() && content.getExpression() instanceof ASTGUIComponent) {
        comp3.add(content.getExpression());
      }
      if (content.isPresentGUIGuard()) {
        guard3.add(content.getGUIGuard());
      }
      if (content.isPresentGUIIterate()) {
        iter3.add(content.getGUIIterate());
      }
      if (content.isPresentExpression() && !(content.getExpression() instanceof ASTGUIComponent)) {
        exp3.add(content.getExpression());
      }
    }

    assertions.add(() -> assertEquals(3, comp3.size()));
    assertions.add(() -> assertEquals(0, guard3.size()));
    assertions.add(() -> assertEquals(0, iter3.size()));
    assertions.add(() -> assertEquals(0, exp3.size()));

    assertions.add(() -> assertTrue(comp3.contains(i)));
    assertions.add(() -> assertTrue(comp3.contains(j)));
    assertions.add(() -> assertTrue(comp3.contains(k)));

    // iterate block t

    assertions.add(() -> assertTrue(finder.getNgTemplates(t.getGUIBlock()).isEmpty()));

    // iterate block u

    assertions.add(() -> assertTrue(finder.getNgTemplates(u.getGUIBlock()).isEmpty()));

    // iterate block aa

    assertions.add(() -> assertTrue(finder.getNgTemplates(aa.getGUIBlock()).isEmpty()));

    // iterate block ab

    assertions.add(() -> assertTrue(finder.getNgTemplates(ab.getGUIBlock()).isEmpty()));

    // iterate block ad

    Set<ASTExpression> comp4 = new HashSet<>();
    Set<ASTGUIGuard> guard4 = new HashSet<>();
    Set<ASTGUIIterate> iter4 = new HashSet<>();
    Set<ASTExpression> exp4 = new HashSet<>();

    result = finder.getNgTemplates(ad.getGUIBlock());

    for (ASTGUIBlockContent content : result) {
      if (content.isPresentExpression() && content.getExpression() instanceof ASTGUIComponent) {
        comp4.add(content.getExpression());
      }
      if (content.isPresentGUIGuard()) {
        guard4.add(content.getGUIGuard());
      }
      if (content.isPresentGUIIterate()) {
        iter4.add(content.getGUIIterate());
      }
      if (content.isPresentExpression()) {
        exp4.add(content.getExpression());
      }
    }

    assertions.add(() -> assertEquals(0, comp4.size()));
    assertions.add(() -> assertEquals(0, guard4.size()));
    assertions.add(() -> assertEquals(1, iter4.size()));
    assertions.add(() -> assertEquals(0, exp4.size()));

    assertions.add(() -> assertTrue(iter4.contains(af)));

    // iterate block af

    assertions.add(() -> assertTrue(finder.getNgTemplates(af.getGUIBlock()).isEmpty()));

    assertAll(assertions);
  }

}
