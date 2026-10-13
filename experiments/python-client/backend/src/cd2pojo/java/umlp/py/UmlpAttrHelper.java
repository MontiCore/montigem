package umlp.py;

import de.monticore.cd.codegen.CDGenService;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.mccollectiontypes.types3.MCCollectionSymTypeRelations;
import de.monticore.types3.SymTypeRelations;

public class UmlpAttrHelper {

  public static enum RealType {
    BUILD_IN,
    GEM,
    ENUM,
    UNKNOWN;
  }

  public static enum Multiplicity {
    MANDATORY,
    OPTIONAL,
    SET,
  }


  public static class AttrData {
    private final RealType typeClass;
    private final Multiplicity multiplicity;
    private final boolean ordered;
    private final SymTypeExpression symTypeExpression;

    public AttrData(RealType typeClass, Multiplicity multiplicity, boolean ordered,
                    SymTypeExpression symTypeExpression) {
      this.typeClass = typeClass;
      this.multiplicity = multiplicity;
      this.ordered = ordered;
      this.symTypeExpression = symTypeExpression;
    }

    public RealType getTypeClass() {
      return typeClass;
    }

    public Multiplicity getMultiplicity() {
      return multiplicity;
    }

    public boolean isOrdered() {
      return ordered;
    }

    public SymTypeExpression getSymTypeExpression() {
      return symTypeExpression;
    }
  }

  public static AttrData getFromSymTypeExpr(SymTypeExpression symTypeExpression) {
    if (MCCollectionSymTypeRelations.isOptional(symTypeExpression)) {
      return createFromGeneric(Multiplicity.OPTIONAL, symTypeExpression, false);
    } else if (MCCollectionSymTypeRelations.isSet(symTypeExpression)) {
      return createFromGeneric(Multiplicity.SET, symTypeExpression, false);
    } else if (MCCollectionSymTypeRelations.isList(symTypeExpression)) {
      return createFromGeneric(Multiplicity.SET, symTypeExpression, true);
    } else if (symTypeExpression.isPrimitive() || SymTypeRelations.isCompatible(symTypeExpression,
        getString())) {
      return new AttrData(RealType.BUILD_IN, Multiplicity.MANDATORY, false, symTypeExpression);
    } else if (symTypeExpression.getTypeInfo() instanceof CDTypeSymbol
        && ((CDTypeSymbol) symTypeExpression.getTypeInfo()).isIsEnum()) {
      return new AttrData(RealType.ENUM, Multiplicity.MANDATORY, false, symTypeExpression);
    } else if (isGem(symTypeExpression)) {
      return new AttrData(RealType.GEM, Multiplicity.MANDATORY, false, symTypeExpression);
    }
    return new AttrData(RealType.UNKNOWN, Multiplicity.MANDATORY, false, symTypeExpression);
  }

  protected static AttrData createFromGeneric(Multiplicity multiplicity,
                                              SymTypeExpression symTypeExpression,
                                              boolean ordered) {
    var inner = MCCollectionSymTypeRelations.getCollectionElementType(
        symTypeExpression);

    if (inner.isPrimitive() || SymTypeRelations.isCompatible(inner, getString())) {
      return new AttrData(RealType.BUILD_IN, multiplicity, ordered, inner);
    } else if (isGem(inner)) {
      return new AttrData(RealType.GEM, multiplicity, ordered, inner);
    }
    return new AttrData(RealType.UNKNOWN, multiplicity, ordered, inner);
  }

  protected static boolean isGem(SymTypeExpression symTypeExpression) {
    return true;
  }

  protected static SymTypeExpression _stringSymTypeExpression;
  protected static CDGenService cdGenService = new CDGenService();

  protected static SymTypeExpression getString() {
    if (_stringSymTypeExpression == null) {
      _stringSymTypeExpression = SymTypeExpressionFactory.createStringType();
    }
    return _stringSymTypeExpression;
  }
}
