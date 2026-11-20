/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.genjava.prettyprinter;

import de.monticore.expressions.commonexpressions._ast.ASTStaticFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.prettyprint.IndentPrinter;
import org.junit.jupiter.api.Test;
import umlp.generator.gui.DomainGUITest;
import umlp.generator.gui.GUIComponentStat;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static umlp.generator.gui.TestUtils.removeSpaces;

public class Name2AccessorConverterTest extends DomainGUITest {

  @Test
  public void testAttr2GetterNameExp1() {
    ASTGUIComponentDeclaration componentDecl;
    GUIComponentStat stat = GUIComponentStat.getInstance();

    List<ASTGUIComponent> components;

    Name2AccessorConverter conv = new Name2AccessorConverter(
        new IndentPrinter(), true, new HashSet<>(), new HashSet<>()
    );

    componentDecl = processGUIModel(
        "gui/genjava/Converter.gui",
        "src/test/resources/gui"
    ).getGUIComponentDeclaration();

    components = stat.getComponents(componentDecl);

    ASTGUIComponent comp1 = components.get(0);
    ASTGUIComponent comp2 = components.get(1);
    ASTGUIComponent comp3 = components.get(2);
    ASTGUIComponent comp4 = components.get(3);
    ASTGUIComponent comp5 = components.get(4);
    ASTGUIComponent comp6 = components.get(7);

    String expect1 = removeSpaces("getPerson()");
    String result1 = removeSpaces(conv.attr2Getter((ASTNameExpression)
        comp1.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ));

    String expect2 = removeSpaces("get$per$son()");
    String result2 = removeSpaces(conv.attr2Getter((ASTNameExpression)
        comp2.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ));

    String expect3 = removeSpaces("get_per_son()");
    String result3 = removeSpaces(conv.attr2Getter((ASTNameExpression)
        comp3.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ));

    String expect4 = removeSpaces("isTest1()");
    String result4 = removeSpaces(conv.attr2Getter((ASTNameExpression)
        comp4.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ));

    String expect5 = removeSpaces("isTest2()");
    String result5 = removeSpaces(conv.attr2Getter((ASTNameExpression)
        comp5.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ));

    String expect6 = removeSpaces("MaritalStatus");
    String result6 = removeSpaces(((ASTStaticFieldAccessExpression)
        comp6.getGUINamedArgs().getGUINamedArg(0).getExpression()
    ).getType().printType());
    assertAll(
        () -> assertEquals(expect1, result1),
        () -> assertEquals(expect2, result2),
        () -> assertEquals(expect3, result3),
        () -> assertEquals(expect4, result4),
        () -> assertEquals(expect5, result5),
        () -> assertEquals(expect6, result6)
    );
  }

}
