/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.types3.TypeCheck3;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.symboltable.modifiers.BasicAccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.util.WithinTypeBasicSymbolsResolver;

import java.util.Optional;


public class UMLPAssociationQualifierByAttributeName implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  //TODO every coco should only check one thing and not two, even if they are similar
  public static final String TYPE_ERROR_CODE = "0xCDC68";
  public static final String NO_ATTR_ERROR_CODE = "0xCDC69";

  public static final String TYPE_ERROR_MSG_FORMAT = " Qualified association with attribute %s " +
      "has incompatible type, qualified associations only support numeric or string type qualifiers.";
  public static final String NO_ATTR_ERROR_MSG_FORMAT = " Attribute %s of qualified association " +
      "does not exist in class %s.";

  private final WithinTypeBasicSymbolsResolver resolver;

  public UMLPAssociationQualifierByAttributeName(){
    this.resolver = new WithinTypeBasicSymbolsResolver();
  }

  @Override
  public void check(ASTCDAssociation assoc) {
    if (assoc.getLeft().isPresentCDQualifier() && assoc.getLeft().getCDQualifier()
        .isPresentByAttributeName()) {
      checkTypeExists(assoc.getLeft(), assoc.getRight());

    }else if (assoc.getRight().isPresentCDQualifier() && assoc.getRight().getCDQualifier()
        .isPresentByAttributeName()) {
      checkTypeExists(assoc.getRight(), assoc.getLeft());
    }
  }

  private void checkTypeExists(ASTCDAssocSide qSide, ASTCDAssocSide attrSide) {
    SymTypeExpression ste = TypeCheck3.symTypeFromAST(attrSide.getMCQualifiedType());
    final String attributeName = qSide.getCDQualifier().getByAttributeName();
    Optional<SymTypeExpression> expr = this.resolver.resolveVariable(ste, attributeName,
        BasicAccessModifier.ALL_INCLUSION, x -> true);
    if (expr.isPresent()) {
      if (!SymTypeRelations.isNumericType(expr.get()) && !SymTypeRelations.isString(expr.get())){
        logError(qSide.getCDQualifier().get_SourcePositionStart(),
                 TYPE_ERROR_CODE,
                 TYPE_ERROR_MSG_FORMAT,
                 attributeName);
      }
    } else {
      logError(qSide.getCDQualifier().get_SourcePositionStart(),
               NO_ATTR_ERROR_CODE,
               NO_ATTR_ERROR_MSG_FORMAT,
               attributeName,
               attrSide.getMCQualifiedType().getMCQualifiedName());
    }
  }

  @Override
  public String getErrorCode() {
    return TYPE_ERROR_CODE;
  }

  @Override
  public String getErrorMessageFormat() {
    return TYPE_ERROR_MSG_FORMAT;
  }
}
