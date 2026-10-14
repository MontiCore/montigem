package umlp.py.utily;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDCardinality;

public class PyGenService {
  private PyGenService() {
    // functional module
  }

  public static boolean isList(ASTCDAssocSide assoc) {
    return assoc.isPresentCDCardinality()
        && (assoc.getCDCardinality().isMult() || assoc.getCDCardinality().isAtLeastOne());
  }

  public static boolean isList(ASTCDCardinality cardinality) {
    return cardinality.isMult() || cardinality.isAtLeastOne();
  }
}
