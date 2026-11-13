/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.coco;

import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._cocos.GUIDSLASTGUICompilationUnitCoCo;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.symboltable.ISymbol;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.logging.Log;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.util.TagLookUp;

/**
 * Attribute qualified associations ({@code Owner [[attr]] -> (role) Target}) are GemMaps,
 * which GUI models do not support yet: the GUI would treat the role as a list or a single object.
 * Hand-written code can use the map, e.g. {@code owner.getRoleMap()}.
 */
public class GuiNoAttributeQualifiedRoleCoCo implements GUIDSLASTGUICompilationUnitCoCo, CommonExpressionsVisitor2 {

  public static final String ERROR_CODE = "0x00032";

  @Override
  public void check(ASTGUICompilationUnit node) {
    GUIDSLTraverser traverser = GUIDSLMill.traverser();
    traverser.add4CommonExpressions(this);
    node.accept(traverser);
  }

  @Override
  public void visit(ASTFieldAccessExpression node) {
    SymTypeExpression type = GUIGeneratorUtil.getInstance().getType(node);
    if (type.getSourceInfo().getSourceSymbol().isPresent()) {
      ISymbol symbol = type.getSourceInfo().getSourceSymbol().get();
      if (TagLookUp.isAttributeQualified(symbol)) {
        String role = node.getName();
        String getter = "get" + Character.toUpperCase(role.charAt(0)) + role.substring(1) + "Map()";
        Log.error(ERROR_CODE + " The association role '" + role + "' is qualified by an attribute, i.e. a map,"
                + " which GUI models do not support yet. Use it in hand-written code instead, e.g. via "
                + getter + ".",
            node.get_SourcePositionStart(), node.get_SourcePositionEnd());
      }
    }
  }
}
