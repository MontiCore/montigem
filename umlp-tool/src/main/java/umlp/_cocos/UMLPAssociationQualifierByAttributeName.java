/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd4code.typescalculator.FullSynthesizeFromCD4Code;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.symboltable.modifiers.BasicAccessModifier;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.util.WithinTypeBasicSymbolsResolver;

import umlp.generator.domain.decorator.GenService;

import java.util.Optional;


public class UMLPAssociationQualifierByAttributeName implements CDAssociationASTCDAssociationCoCo, CoCoTestable {

  //TODO every coco should only check one thing and not two, even if they are similar
  public static final String TYPE_ERROR_CODE = "0xCDC68";
  public static final String NO_ATTR_ERROR_CODE = "0xCDC69";

  public static final String TYPE_ERROR_MSG_FORMAT = " Qualified association with attribute %s " +
      "has incompatible type, qualified associations only support primitive, String or domain class attributes as qualifiers.";
  public static final String NO_ATTR_ERROR_MSG_FORMAT = " Attribute or association role %s of qualified association " +
      "does not exist in class %s.";

  private final FullSynthesizeFromCD4Code synthesizer;
  private final WithinTypeBasicSymbolsResolver resolver;

  public UMLPAssociationQualifierByAttributeName(){
    this.synthesizer = new FullSynthesizeFromCD4Code();
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
    SymTypeExpression ste = this.synthesizer.synthesizeType(attrSide.getMCQualifiedType()).getResult();
    final String attributeName = qSide.getCDQualifier().getByAttributeName();
    Optional<SymTypeExpression> expr = this.resolver.resolveVariable(ste, attributeName,
        BasicAccessModifier.ALL_INCLUSION, x -> true);
    if (expr.isPresent()) {
      // optional and multi-valued attributes are reported by UMLPAssociationQualifierKeyCardinality
      if (!UMLPAssociationQualifierKeyCardinality.isOptionalOrMultiValued(expr.get())
          && !isValueKey(expr.get()) && !isDomainKey(expr.get())){
        logError(qSide.getCDQualifier().get_SourcePositionStart(),
                 TYPE_ERROR_CODE,
                 TYPE_ERROR_MSG_FORMAT,
                 attributeName);
      }
    } else if (findRole(ste, attributeName).isEmpty()) {
      // not a role of an association navigable from the target class either, which would be a domain object key
      logError(qSide.getCDQualifier().get_SourcePositionStart(),
               NO_ATTR_ERROR_CODE,
               NO_ATTR_ERROR_MSG_FORMAT,
               attributeName,
               attrSide.getMCQualifiedType().getMCQualifiedName());
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

  /**
   * primitive (MontiCore typecheck, String included) qualifiers are keys of a GemValueMap
   */
  public static boolean isValueKey(SymTypeExpression type) {
    return type.isPrimitive() || SymTypeRelations.isNumericType(type)
        || SymTypeRelations.isBoolean(type) || SymTypeRelations.isString(type);
  }

  /**
   * domain class qualifiers are keys of a GemEntryMap
   */
  public static boolean isDomainKey(SymTypeExpression type) {
    return type.hasTypeInfo() && GenService.isDomainType(type.getTypeInfo()) && !GenService.isCDEnum(type);
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
