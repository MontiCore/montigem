package umlp.generator.ocl.javagen.field_access;

import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mccollectiontypes.types3.MCCollectionSymTypeRelations;
import umlp.generator.ocl.TypeUtil;

public class FieldAccessInfo {
  private final FieldAccessKind kind;
  private final SymTypeExpression fieldType;   // element type for collections

  public FieldAccessInfo(
      FieldAccessKind kind,
      SymTypeExpression fieldType
  ) {
    this.kind = kind;
    this.fieldType = fieldType;
  }

  public FieldAccessKind getKind() { return kind; }
  public SymTypeExpression getFieldType() { return fieldType; }

  public static FieldAccessInfo from(
      ASTFieldAccessExpression node,
      SymTypeExpression qualifierType,
      SymTypeExpression expressionType
  ) {
    SymTypeExpression ownerType = resolveOwnerType(node, qualifierType, expressionType);
    SymTypeExpression fieldType = null;

    boolean isCollection =
        TypeUtil.getInstance().isCollectionType(ownerType)
            && ownerType.isGenericType()
            && !ownerType.asGenericType().getArgumentList().isEmpty();

    if (isCollection) {
      fieldType = ownerType.asGenericType().getArgument(0);
    }

    boolean toManyDomain = isCollection && TypeUtil.getInstance().isDomainType(fieldType);
    boolean toOneDomain  = !isCollection && TypeUtil.getInstance().isDomainType(ownerType);

    if (toManyDomain) {
      return new FieldAccessInfo(
          FieldAccessKind.TO_MANY_DOMAIN,
          fieldType
      );
    }

    if (toOneDomain) {
      return new FieldAccessInfo(
          MCCollectionSymTypeRelations.isOptional(ownerType)
              ? FieldAccessKind.TO_OPTIONAL_DOMAIN : FieldAccessKind.TO_ONE_DOMAIN,
          ownerType
      );
    }

    if (isCollection) {
      return new FieldAccessInfo(
          FieldAccessKind.COLLECTION_OF_SIMPLE_VALUE,
          fieldType
      );
    }

    return new FieldAccessInfo(
        FieldAccessKind.SIMPLE_VALUE,
        ownerType
    );
  }

  private static SymTypeExpression resolveOwnerType(
      ASTFieldAccessExpression node,
      SymTypeExpression qualifierType,
      SymTypeExpression expressionType
  ) {
    if (qualifierType.isIntersectionType()) {
      var parts = qualifierType.asIntersectionType().getIntersectedTypeSet();
      if (!parts.isEmpty()
          && parts.stream()
          .map(SymTypeExpression::printFullName)
          .distinct()
          .count() == 1) {

        var owner = parts.iterator().next();
        if (owner.isObjectType()) {
          var varSymOpt = owner.asObjectType()
              .getTypeInfo()
              .getSpannedScope()
              .resolveVariable(node.getName());

          if (varSymOpt.isPresent()) {
            return varSymOpt.get().getType();
          }
        }
      }
    }

    return expressionType;
  }

}
