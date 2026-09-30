package umlp.generator.gui.genjava.prettyprinter;

import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.se_rwth.commons.StringTransformations;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.gui.GUIGeneratorUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ExpressionPrinterConfig {

  protected static ExpressionPrinterConfig instance;

  public Map<ASTExpression, Function<ASTExpression, String>> customConversions = new LinkedHashMap<>();

  public static ExpressionPrinterConfig getInstance() {
    if (instance == null) {
      instance = new ExpressionPrinterConfig();
    }
    return instance;
  }

  private ExpressionPrinterConfig() {
  }

  public static void cleanUp() {
    instance = null;
  }

  public static String attr2Getter(ASTFieldAccessExpression node) {
    if (GUIGeneratorUtil.getInstance().isEnumMember(node)) {
      return node.getName();
    }
    SymTypeExpression ste = GUIGeneratorUtil.getInstance().getType(node);

    if (SymTypeRelations.isBoolean(ste)) {
      return "is" + StringTransformations.capitalize(node.getName());
    } else if (GenService.isList(ste)) {
      return "get" + StringTransformations.capitalize(node.getName())+"List";
    }
    return "get" + StringTransformations.capitalize(node.getName());
  }
}
