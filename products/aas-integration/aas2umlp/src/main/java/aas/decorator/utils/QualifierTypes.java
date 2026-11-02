package aas.decorator.utils;

import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Qualifier;
import org.eclipse.digitaltwin.aas4j.v3.model.QualifierKind;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultQualifier;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;

public abstract class QualifierTypes {

  private static Qualifier _getQualifierC(String value) {
    return new DefaultQualifier.Builder()
        .semanticId(new DefaultReference.Builder()
            .type(ReferenceTypes.EXTERNAL_REFERENCE)
            .keys(new DefaultKey.Builder()
                .type(KeyTypes.GLOBAL_REFERENCE)
                .value("https://admin-shell.io/SubmodelTemplates/Cardinality/1/0")
                .build())
            .build())
      .kind(QualifierKind.CONCEPT_QUALIFIER)
      .type("Cardinality")
      .valueType(DataTypeDefXsd.STRING)
      .value(value)
      .build();
  }

  public static final Qualifier C_ZERO_TO_ONE_QUALIFIER = _getQualifierC("ZeroToOne");

  public static final Qualifier C_ZERO_TO_MANY_QUALIFIER = _getQualifierC("ZeroToMany");

  public static final Qualifier C_ONE_TO_MANY_QUALIFIER = _getQualifierC("OneToMany");

  public static final Qualifier C_ONE_QUALIFIER = _getQualifierC("One");

}
