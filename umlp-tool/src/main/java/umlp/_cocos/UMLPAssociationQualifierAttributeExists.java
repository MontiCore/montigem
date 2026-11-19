/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.symboltable.modifiers.BasicAccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.TypeCheck3;
import de.monticore.types3.util.WithinTypeBasicSymbolsResolver;

import java.util.Optional;

/**
 * The qualifier of an attribute qualified association ({@code Owner [[attr]] -> Value}) names the key of the values:
 * an attribute of the value class or a role of an association navigable from it (declared in the class or a
 * supertype). Its type is checked by {@link UMLPAssociationQualifierByAttributeName}, its cardinality by
 * {@link UMLPAssociationQualifierKeyCardinality}.
 */
public class UMLPAssociationQualifierAttributeExists implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  public static final String ERROR_CODE = "0xCDC69";

  public static final String ERROR_MSG_FORMAT = " Attribute or association role %s of qualified association " +
      "does not exist in class %s.";

  private final WithinTypeBasicSymbolsResolver resolver = new WithinTypeBasicSymbolsResolver();

  @Override
  public void check(ASTCDAssociation assoc) {
    if (UMLPAssociationQualifierKeyCardinality.isAttributeQualified(assoc.getLeft())) {
      check(assoc.getLeft(), assoc.getRight());
    }
    if (UMLPAssociationQualifierKeyCardinality.isAttributeQualified(assoc.getRight())) {
      check(assoc.getRight(), assoc.getLeft());
    }
  }

  protected void check(ASTCDAssocSide qualifiedSide, ASTCDAssocSide valueSide) {
    SymTypeExpression valueType = TypeCheck3.symTypeFromAST(valueSide.getMCQualifiedType());
    String qualifier = qualifiedSide.getCDQualifier().getByAttributeName();
    boolean isAttribute = resolver.resolveVariable(valueType, qualifier, BasicAccessModifier.ALL_INCLUSION, x -> true)
        .isPresent();
    if (!isAttribute && findRole(valueType, qualifier).isEmpty()) {
      logError(qualifiedSide.getCDQualifier().get_SourcePositionStart(), ERROR_CODE, ERROR_MSG_FORMAT, qualifier,
          valueSide.getMCQualifiedType().getMCQualifiedName());
    }
  }

  /**
   * the side named roleName of an association navigable from the type (declared in the type or a supertype),
   * like the generator looks up a qualifier that is no attribute (QualifierKey)
   */
  public static Optional<ASTCDAssocSide> findRole(SymTypeExpression type, String roleName) {
    if (!type.hasTypeInfo() || !(type.getTypeInfo() instanceof CDTypeSymbol)
        || !((CDTypeSymbol) type.getTypeInfo()).isPresentAstNode()) {
      return Optional.empty();
    }
    ASTCDType ast = ((CDTypeSymbol) type.getTypeInfo()).getAstNode();
    return CDSymbolTables.getAssociationsInHierarchy(ast).stream()
        .filter(side -> roleName.equals(side.getName()))
        .findFirst();
  }

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }

  @Override
  public String getErrorMessageFormat() {
    return ERROR_MSG_FORMAT;
  }
}
